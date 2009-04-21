package orchestra.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;

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
import org.spearce.jgit.transport.RemoteConfig;
import org.spearce.jgit.transport.RemoteRefUpdate;
import org.spearce.jgit.transport.Transport;
import org.spearce.jgit.transport.URIish;


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

  private static final Collection<RefSpec> MASTER_REF_SPEC =
      Collections.singleton(new RefSpec("master"));

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

    // Create and persist the commit
    final Commit commit = new Commit(repo);
    final RefUpdate updateRef = repo.updateRef(Constants.HEAD);

    if (updateRef.getOldObjectId() != null) {
      commit.setParentIds(new ObjectId[] {updateRef.getOldObjectId()});
    }

    commit.setAuthor(new PersonIdent(repo));
    commit.setCommitter(new PersonIdent(repo));
    commit.setMessage(message);
    commit.setTree(tree);
    commit.commit();

    updateRef.setNewObjectId(commit.getCommitId());
    updateRef.setRefLogMessage(commit.getMessage(), false);
    return updateRef.update();
  }

  /**
   * Pushes to the "master" refspec to origin.
   * 
   * @param monitor
   * @return the result of the push operation
   * @throws IOException
   */
  public PushResult pushOriginMaster(final ProgressMonitor monitor) throws IOException {
    try {
      final Transport tx = Transport.open(repo, ORIGIN);

      try {
        return pushSafe(tx, MASTER_REF_SPEC, monitor);
      } finally {
        tx.close();
      }
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }
  }

  /**
   * Pushes a collection of refspecs to a remote location via a given
   * {@link Transport}.
   * 
   * @param tx
   * @param specs
   * @param monitor
   * @return
   * @throws IOException
   * @throws TransportException
   */
  private PushResult push(final Transport tx, final Collection<RefSpec> specs,
      final ProgressMonitor monitor) throws IOException, TransportException {
    final Collection<RemoteRefUpdate> updates = tx.findRemoteRefUpdatesFor(specs);

    for (final RemoteRefUpdate update : updates) {
      LOG.info("Pushing {} ({}) to {}", new Object[] {update.getNewObjectId(),
          repo.mapCommit(update.getNewObjectId()).getMessage(), tx.getURI()});
    }

    return tx.push(monitor, updates);
  }

  /**
   * A fuzzier version of {@link #push(Transport, Collection, ProgressMonitor)} that doesn't
   * bail when a transfer is canceled. If a transfer is aborted, the method
   * returns an empty {@link PushResult}.
   * 
   * @param tx
   * @param specs
   * @param monitor
   * @return
   * @throws IOException
   */
  private PushResult pushSafe(final Transport tx, Collection<RefSpec> specs,
      final ProgressMonitor monitor) throws IOException {
    try {
      return push(tx, specs, monitor);
    } catch (final TransportException e) {
      LOG.warn("Error when pushing", e);
      return createEmptyPushResult(tx.getURI());
    }
  }

  /**
   * @return <code>true</code> if the remote section of the git configuration
   *         contains an origin sub-section
   */
  public boolean hasRemoteOrigin() {
    return repo.getConfig().getSubsections(RepositoryConfig.REMOTE_SECTION).contains("[origin]");
  }

  /**
   * Find the specification on how refs in the repository should be copied into
   * the remote (?) repository.
   * 
   * @return
   */
  public Collection<RefSpec> defaultRemoteFetchSpecs() {
    try {
      return new RemoteConfig(repo.getConfig(), "origin").getFetchRefSpecs();
    } catch (URISyntaxException e) {
      LOG.warn(e.getMessage(), e);
      return Collections.emptyList();
    }
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

  /**
   * Creates an "empty" {@link PushResult} object used instead of an exception
   * to signal an error when pushing to the repository.
   * 
   * @param uri
   * @return
   */
  private static final PushResult createEmptyPushResult(final URIish uri) {
    return new PushResult() {
      @Override
      public URIish getURI() {
        return uri;
      }
    };
  }
}
