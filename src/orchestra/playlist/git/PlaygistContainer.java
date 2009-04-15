package orchestra.playlist.git;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Map;

import orchestra.playlist.Playlist;
import orchestra.playlist.PlaylistContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.FileTreeEntry;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectWriter;
import org.spearce.jgit.lib.PersonIdent;
import org.spearce.jgit.lib.RefUpdate;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.SymlinkTreeEntry;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;
import org.spearce.jgit.lib.TreeVisitor;

import de.felixbruns.jotify.media.Track;

public class PlaygistContainer extends PlaylistContainer {
  private static final Logger log = LoggerFactory.getLogger(PlaygistContainer.class);

  private final MessageDigest messageDigest;

  private final Repository repo;

  private PlaygistContainer(String owner, Repository repo, MessageDigest digest) {
    super(owner);
    this.repo = repo;
    this.messageDigest = digest;
  }

  /**
   * Opens and reads all playlists in a playgist repository.
   * 
   * @param owner
   * @param repo
   * @return
   * @throws IOException
   * @throws GeneralSecurityException
   */
  public static PlaygistContainer open(String owner, Repository repo) throws IOException,
      GeneralSecurityException {
    MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
    PlaygistContainer container = new PlaygistContainer(owner, repo, sha1);
    container.readPlaylists();
    return container;
  }

  /**
   * Reads the Git tree and populates the container with playlists.
   * 
   * @throws IOException
   */
  private void readPlaylists() throws IOException {
    Tree head = repo.mapTree(Constants.HEAD);

    if (head != null) {
      head.accept(new TreeVisitor() {
        public void visitFile(FileTreeEntry f) throws IOException {
          try {
            Playgist gist = Playgist.open(repo.getWorkDir(), new File(f.getFullName()));
            addPlaygist(gist);
            log.info("Added playlist: {}", gist.getTreeEntry().getName());
          } catch (IOException e) {
            log.info("Failed to open gist: {}", e.getMessage());
          }
        }

        public void endVisitTree(Tree t) throws IOException {
        }

        public void startVisitTree(Tree t) throws IOException {
        }

        public void visitSymlink(SymlinkTreeEntry s) throws IOException {
        }
      });
    }
  }

  @Override
  public Playlist createPlaylist(String name) throws Exception {
    String hash = getNextHash();
    String repoRelativePath = new File(getOwner(), hash).getPath();
    File absolutePath = new File(repo.getWorkDir(), repoRelativePath);

    if (!absolutePath.exists()) {
      createFile(absolutePath);
    }

    // GitIndex index = repo.getIndex();
    // GitIndex.Entry indexEntry = index.add(repo.getWorkDir(), absolutePath);
    // indexEntry.setAssumeValid(false);
    // index.write();
    Tree tree = repo.mapTree(Constants.HEAD);

    if (tree == null) {
      tree = new Tree(repo);
    }

    FileTreeEntry entry = tree.addFile(repoRelativePath);
    entry.setId(ObjectId.zeroId());
    new ObjectWriter(repo).writeTree(tree);

    log.info("Created new playlist at {}", absolutePath);
    Playgist gist = Playgist.open(repo.getWorkDir(), new File(getOwner(), hash));
    gist.setName(name);
    addPlaygist(gist);
    return gist;
  }

  // Tries harder than plain Java to create the file, /absoluteFile/.
  private void createFile(File absolutePath) throws IOException {
    log.info("Attempting to create {}", absolutePath);
    try {
      if (absolutePath.createNewFile()) {
        return;
      }
    } catch (IOException e) {
    }

    if (!absolutePath.getParentFile().mkdirs()) {
      throw new IOException("Failed to create directories for " + absolutePath);
    }

    if (!absolutePath.createNewFile()) {
      throw new IOException("Failed to create file: " + absolutePath);
    }
  }

  /**
   * Adds the playlist to the git repository.
   * 
   * @param gist
   * @throws Exception
   */
  private void addPlaygist(Playgist gist) throws IOException {
    gist.setListener(this);
    addPlaylist(gist);
  }

  /**
   * @return the path to this container's owner
   */
  private File getOwnerRoot() {
    return new File(repo.getWorkDir(), getOwner());
  }

  private PersonIdent getOwnerIdent() {
    return new PersonIdent(repo);
  }

  /**
   * @return
   * @throws Exception
   */
  private String getNextHash() throws Exception {
    String filename = String.format("%s-%d", getOwner(), size());
    byte[] digest = messageDigest.digest(filename.getBytes(Charset.forName("UTF-8")));
    String hash = new BigInteger(1, digest).toString(16);
    return hash;
  }

  @Override
  public void hasChanged(Playlist playlist) {
    if (playlist == null) {
      log.info("null playlist hasChanged");
      return;
    }

    if (playlist instanceof Playgist) {
      Playgist gist = (Playgist) playlist;

      try {
        ObjectId objectId = tryWriteFile(gist);
        Tree tree = repo.mapTree(Constants.HEAD);

        if (tree == null) {
          log.info("really weird");
          tree = new Tree(repo);
        }

        Commit commit = new Commit(repo);
        RefUpdate updateRef = repo.updateRef(Constants.HEAD);

        if (updateRef.getOldObjectId() != null) {
          commit.setParentIds(new ObjectId[] {updateRef.getOldObjectId()});
        }

        commit.setTreeId(objectId);
        commit.setAuthor(getOwnerIdent());
        commit.setCommitter(new PersonIdent("orchestra", "johanliesen@gmail.com"));
        commit.setMessage("playlist change"); // TODO(liesen): better commit
                                              // messages
        commit.commit();

        updateRef.setNewObjectId(commit.getCommitId());
        updateRef.setRefLogMessage(commit.getMessage(), false);

        RefUpdate.Result result = updateRef.update();

        log.info("Updated, result: {}", result); // TODO(liesen): error handling
        playlist.setDirty(false);
      } catch (IOException e) {
        log.warn("Failed to commit", e);
        playlist.setDirty(true);
      }
    }
  }

  private void writeTree(Tree tree) throws IOException {
    if (tree.getId() == null) {
      for (TreeEntry member : tree.members()) {
        if (member.isModified() && member instanceof Tree) {
          writeTree((Tree) member);
        }
      }

      ObjectWriter writer = new ObjectWriter(tree.getRepository());
      tree.setId(writer.writeTree(tree));
    }
  }

  /**
   * Tries to write the playlist to disk. If it fails then the playlist is set
   * to a dirty state.
   */
  private ObjectId tryWriteFile(Playgist gist) {
    try {
      return writeFile(gist);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return ObjectId.zeroId();
  }

  /**
   * Writes the playlist to disk.
   * 
   * @throws IOException
   */
  private ObjectId writeFile(Playgist gist) throws IOException {
    File path = gist.getPath();
    log.info("Writing file {}", path);

    if (!path.exists()) {
      path.createNewFile();
    }

    log.info("Writing file {}", path);
    BufferedWriter out =
        new BufferedWriter(Channels.newWriter(new FileOutputStream(path).getChannel(), "UTF-8"));

    // Write metadata
    Map<String, String> metadata = gist.getMetadata();

    for (String prop : metadata.keySet()) {
      out.write(String.format("%s%s = %s", Playgist.METADATA_PREFIX, prop, metadata.get(prop)));
      out.newLine();
    }

    // Write tracks
    for (Track track : gist.getTracks()) {
      out.write(track.getId());
      out.newLine();
    }

    out.flush();
    out.close();

    return new ObjectWriter(repo).writeBlob(path);
  }
}
