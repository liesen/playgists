package orchestra.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spearce.jgit.errors.TransportException;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectWriter;
import org.spearce.jgit.lib.PersonIdent;
import org.spearce.jgit.lib.ProgressMonitor;
import org.spearce.jgit.lib.RefUpdate;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.RepositoryConfig;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;
import org.spearce.jgit.transport.PushResult;
import org.spearce.jgit.transport.RefSpec;
import org.spearce.jgit.transport.RemoteRefUpdate;
import org.spearce.jgit.transport.Transport;


/**
 * Wrapper around JGit's {@link Repository} class to provide a simple interface
 * to common git tasks such as add, commit and push.
 * 
 * <p>
 * Code influenced by <a href="http://github.com/myabc/nbgit/blob/f81740730fb05fc469d7f634c65aace081220a0c/src/org/nbgit/util/GitCommand.java"
 * >GitCommand.java</a>.
 */
public class Git {
  private static final Logger LOG = LoggerFactory.getLogger(Git.class);

  // This should go in org.spearce.jgit.lib.Constants, no?
  private static final String ORIGIN = "origin";

  /** git repository. */
  private final Repository repo;

  /**
   * @param repo
   */
  public Git(Repository repo) {
    this.repo = repo;
  }

  /**
   * @return the git repository
   */
  public Repository getRepository() {
    return repo;
  }

  /**
   * Given a file within the git repository; return the repository-relative part
   * of its path.
   * 
   * @param file file inside the git working directory
   * @return relative path to file within the git working directory
   * @throws IllegalArgumentException if the file is not inside the repository's
   *         working directory
   */
  public File makeRepositoryRelative(File file) throws IllegalArgumentException {
    if (!file.isAbsolute() && new File(repo.getWorkDir(), file.getPath()).exists()) {
      return file;
    }

    final String repoWorkingPath = repo.getWorkDir().getPath();

    if (file.getPath().startsWith(repoWorkingPath)) {
      return new File(file.getPath().substring(repoWorkingPath.length() + 1));
    }

    throw new IllegalArgumentException(file + " not inside the repository.");
  }

  /**
   * Gets or creates the HEAD tree.
   * 
   * @return
   * @throws IOException
   */
  private Tree getHeadTree() throws IOException {
    Tree tree = repo.mapTree(Constants.HEAD);

    if (tree == null) {
      return new Tree(repo);
    }

    return tree;
  }

  /**
   * Adds a set of files to the index (but not to a tree).
   * 
   * @param files
   * @throws IOException
   */
  public void add(File... files) throws IOException {
    final GitIndex index = repo.getIndex();

    for (File file : files) {
      GitIndex.Entry indexEntry = index.add(repo.getWorkDir(), file);
      indexEntry.setAssumeValid(false);
    }

    index.write();
  }

  /**
   * Commits a set of files to HEAD by first updating both the index and then
   * the tree.
   * 
   * @param message
   * @param files
   * @return
   * @throws IOException
   */
  public RefUpdate.Result commit(String message, File... files) throws IOException {
    final Tree tree = getHeadTree();
    final GitIndex index = repo.getIndex();
    boolean writeIndex = false;

    for (File file : files) {
      final String repoRelativePath = makeRepositoryRelative(file).getPath();

      // Update index
      final GitIndex.Entry indexEntry = index.getEntry(repoRelativePath);

      if (indexEntry.update(file)) {
        writeIndex = true;
      }

      // Update file in the tree
      TreeEntry treeEntry = tree.findBlobMember(repoRelativePath);

      // Add file to tree if it's new
      if (treeEntry == null) {
        treeEntry = tree.addFile(repoRelativePath);
      }

      treeEntry.setId(indexEntry.getObjectId());
    }

    if (writeIndex) {
      index.write();
    }

    writeTree(tree); // XXX(liesen): GitIndex.writeTree?

    final Commit commit = new Commit(repo);
    final RefUpdate updateRef = repo.updateRef(Constants.HEAD);

    if (updateRef.getOldObjectId() != null) {
      commit.setParentIds(new ObjectId[] {updateRef.getOldObjectId()});
    }

    commit.setAuthor(new PersonIdent(repo));
    commit.setCommitter(new PersonIdent(repo));
    commit.setMessage(message.replaceAll("\r", "\n"));
    commit.setTree(tree);
    commit.commit();

    updateRef.setNewObjectId(commit.getCommitId());
    updateRef.setRefLogMessage(commit.getMessage(), false);
    return updateRef.update();
  }

  public List<PushResult> push(final ProgressMonitor monitor) throws IOException {
    final List<PushResult> results = new LinkedList<PushResult>();

    try {
      // TODO(liesen): can there be more than one origin?
      final List<Transport> transports = Transport.openAll(repo, ORIGIN);

      for (final Transport tx : transports) {
        try {
          results.add(push(tx, monitor));
        } finally {
          tx.close();
        }
      }
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }

    return results;
  }

  /**
   * Performs a push operation without a progress monitor.
   * 
   * @return
   * @throws IOException
   */
  public List<PushResult> push() throws IOException {
    return push(new NoProgressMonitor());
  }

  /**
   * Does a push to [origin].
   * 
   * @param monitor
   * @return
   * @throws IOException
   */
  public PushResult pushToOrigin(final ProgressMonitor monitor) throws IOException {
    try {
      final Transport tx = Transport.open(repo, ORIGIN);

      try {
        return push(tx, monitor);
      } finally {
        tx.close();
      }
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }
  }

  /**
   * Pushes via a given {@link Transport} using {@link #defaultRefSpecs()} as
   * specification.
   * 
   * @param tx
   * @param monitor
   * @return
   * @throws IOException
   * @throws TransportException
   */
  private PushResult push(final Transport tx, final ProgressMonitor monitor) throws IOException,
      TransportException {
    final Collection<RefSpec> specs = defaultRefSpecs();
    final Collection<RemoteRefUpdate> updates = tx.findRemoteRefUpdatesFor(specs);

    for (final RemoteRefUpdate update : updates) {
      LOG.info("Pushing {} ({}) to {}", new Object[] {update.getNewObjectId(),
          repo.mapCommit(update.getNewObjectId()).getMessage(), tx.getURI()});
    }

    return tx.push(monitor, updates);
  }

  public boolean hasRemoteOrigin() {
    return repo.getConfig().getSubsections(RepositoryConfig.REMOTE_SECTION).contains("[origin]");
  }

  /**
   * Find the specification on how refs in the repository should be copied into
   * the remote (?) repository.
   * 
   * @return
   */
  public String getRemoteFetchSpec() {
    return repo.getConfig().getString(RepositoryConfig.REMOTE_SECTION, ORIGIN, "fetch");
  }

  /**
   * Returns the default {@link RefSpec}'s (that tell how local refs should be
   * copied to remote repositories). Tries to first figure out what the
   * configuration says.
   * 
   * @return
   */
  private Collection<RefSpec> defaultRefSpecs() {
    String remoteFetchSpec = getRemoteFetchSpec();

    if (remoteFetchSpec == null) {
      return Collections.emptySet();
    }

    return Collections.singleton(new RefSpec(remoteFetchSpec));
  }

  /**
   * Recursively writes a tree and its subtrees.
   * 
   * @param tree
   * @throws IOException
   */
  private static void writeTree(Tree tree) throws IOException {
    if (tree.getId() == null) {
      for (TreeEntry treeEntry : tree.members()) {
        if (treeEntry.isModified() && treeEntry instanceof Tree) {
          writeTree((Tree) treeEntry);
        }
      }

      tree.setId(new ObjectWriter(tree.getRepository()).writeTree(tree));
    }
  }
}
