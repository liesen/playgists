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
import orchestra.util.Git;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.FileTreeEntry;
import org.spearce.jgit.lib.PersonIdent;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.SymlinkTreeEntry;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeVisitor;

import de.felixbruns.jotify.media.Track;

/**
 * A "playlist of playlists" for git-backed "gists".
 * 
 */
public class PlaygistContainer extends PlaylistContainer {
  private static final Logger log = LoggerFactory.getLogger(PlaygistContainer.class);

  private final MessageDigest messageDigest;

  private final Git git;

  private PlaygistContainer(String owner, Git git, MessageDigest digest) {
    super(owner);
    this.git = git;
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
  public static PlaygistContainer open(String owner, Git git) throws IOException,
      GeneralSecurityException {
    MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
    PlaygistContainer container = new PlaygistContainer(owner, git, sha1);
    container.readPlaylists();
    return container;
  }

  /**
   * Reads the Git tree and populates the container with playlists.
   * 
   * @throws IOException
   */
  private void readPlaylists() throws IOException {
    final Repository repo = git.getRepository();
    final Tree head = repo.mapTree(Constants.HEAD);

    if (head != null) {
      head.accept(new TreeVisitor() {
        public void visitFile(FileTreeEntry f) throws IOException {
          try {
            Playgist gist = Playgist.open(repo.getWorkDir(), new File(f.getFullName()));
            log.info("Added playlist: {}", gist.getName());
            addPlaygist(gist);
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
    File absolutePath = new File(git.getRepository().getWorkDir(), repoRelativePath);

    if (absolutePath.exists()) {
      throw new IllegalStateException("Playlist file already exists: " + absolutePath.getPath());
    }

    createFile(absolutePath);
    git.add(absolutePath);

    log.info("Created new playlist at {}", absolutePath);
    Playgist gist = Playgist.open(git.getRepository().getWorkDir(), new File(getOwner(), hash));
    gist.setName(name);
    addPlaygist(gist);
    return gist;
  }

  // Tries harder than plain Java to create the file, /absoluteFile/.
  private void createFile(File absolutePath) throws IOException {
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
    return new File(git.getRepository().getWorkDir(), getOwner());
  }

  /**
   * @return {@link PersonIdent} of the repository owner.
   */
  private PersonIdent getOwnerIdent() {
    return new PersonIdent(git.getRepository());
  }

  /**
   * @return
   * @throws Exception
   */
  private String getNextHash() throws Exception {
    // TODO(liesen): this is a insufficient method for generating a playlist id
    String filename = String.format("%s-%d", getOwner(), size());
    byte[] digest = messageDigest.digest(filename.getBytes(Charset.forName("UTF-8")));
    String hash = String.format("%040x", new BigInteger(1, digest));
    return hash;
  }

  @Override
  public void playlistChanged(Playlist playlist) {
    if (playlist == null) {
      return;
    }

    if (playlist instanceof Playgist) {
      Playgist gist = (Playgist) playlist;
      tryWriteFile(gist);

      try {
        git.commit("Playlist update", gist.getPath());
        playlist.setDirty(false);
      } catch (IOException e) {
        log.warn("Failed to commit", e);
        playlist.setDirty(true);
      }
    }
  }

  /**
   * Tries to write the playlist to disk. If it fails then the playlist is set
   * to a dirty state.
   */
  private void tryWriteFile(Playgist gist) {
    try {
      writeFile(gist);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Writes the playlist to disk.
   * 
   * @throws IOException
   */
  private void writeFile(Playgist gist) throws IOException {
    File path = gist.getPath();

    if (!path.exists()) {
      path.createNewFile();
    }

    BufferedWriter out =
        new BufferedWriter(Channels.newWriter(new FileOutputStream(path).getChannel(), "UTF-8"));

    // Write metadata
    Map<String, String> metadata = gist.getMetadata();

    for (String prop : metadata.keySet()) {
      // TODO(liesen): user some lib for storing metadata
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
  }
}
