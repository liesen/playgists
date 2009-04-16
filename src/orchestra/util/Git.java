package orchestra.util;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectWriter;
import org.spearce.jgit.lib.PersonIdent;
import org.spearce.jgit.lib.RefUpdate;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;

/**
 * Wrapper around JGit's {@link Repository} class to provide a simple interface
 * to common tasks.
 * 
 * <p>
 * Code influenced by http://github.com/myabc/nbgit/blob/f81740730fb05fc469d7f634c65aace081220a0c/src/org/nbgit/util/GitCommand.java}
 */
public class Git {
  private static final Logger LOG = LoggerFactory.getLogger(Git.class);

  /** git repository. */
  private final Repository repo;

  /**
   * @param repo
   */
  public Git(Repository repo) {
    this.repo = repo;
  }

  public Repository getRepository() {
    return repo;
  }

  /**
   * Given a file within the git repository; return the repository-relative part
   * of its path.
   * 
   * @param file
   * @return
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
      GitIndex.Entry indexEntry = index.add(repo.getWorkDir(), file.getAbsoluteFile());
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
