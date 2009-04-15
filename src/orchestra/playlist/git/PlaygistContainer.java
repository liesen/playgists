package orchestra.playlist.git;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Map;

import orchestra.playlist.Playlist;
import orchestra.playlist.PlaylistContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.Repository;

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

  public static PlaygistContainer open(String owner, Repository repo) throws Exception {
    MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
    PlaygistContainer container = new PlaygistContainer(owner, repo, sha1);
    container.readPlaylists();
    return container;
  }
  
  private void readPlaylists() throws IOException {
    GitIndex index = repo.getIndex();
    
    for (GitIndex.Entry entry : index.getMembers()) {
      try {
        Playgist gist = Playgist.open(new File(repo.getWorkDir(), entry.getName()));
        addPlaylist(gist);
        log.info("Added playlist: {}", gist.getName());
      } catch (IOException e) {
        log.info("Failed to open gist: {}", e.getMessage());
      }
    }
  }

  @Override
  public Playlist newPlaylist(String name) throws Exception {
    File absolutePath = new File(getOwnerRoot(), getNextHash());
    createFile(absolutePath);
    log.info("Created new playlist at {}", absolutePath);
    Playgist gist = Playgist.open(absolutePath);
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
      // I want NIO.2
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
    addPlaylist(gist);
  }

  /**
   * Returns the path to this container's owner.
   * 
   * @return
   */
  private File getOwnerRoot() {
    return new File(repo.getWorkDir(), getOwner());
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
    super.hasChanged(playlist);

    if (playlist instanceof Playgist) {
      tryWriteFile((Playgist) playlist);
    }
  }

  /**
   * Tries to write the playlist to disk. If it fails then the playlist is set
   * to a dirty state.
   */
  private void tryWriteFile(Playgist gist) {
    try {
      writeFile(gist);
      gist.setDirty(false);
    } catch (IOException e) {
      gist.setDirty(true);
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
  }
}
