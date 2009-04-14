package orchestra.playlist.git;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Map;

import orchestra.playlist.Playlist;
import orchestra.playlist.PlaylistContainer;
import orchestra.util.Git;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.felixbruns.jotify.media.Track;

public class PlaygistContainer extends PlaylistContainer {
  private static final Logger log = LoggerFactory.getLogger(PlaygistContainer.class);

  private final MessageDigest messageDigest;

  private static final FileFilter PLAYGIST_FILE_FILTER = new FileFilter() {
    private static final int FILENAME_LENGTH = 40; // SHA-1 "hash" length

    public boolean accept(File pathname) {
      return pathname.isFile() && pathname.canRead()
          && pathname.getName().length() == FILENAME_LENGTH;
    }
  };

  private final Git git;

  private PlaygistContainer(String owner, Git git, MessageDigest digest) {
    super(owner);
    this.git = git;
    this.messageDigest = digest;
  }

  public static PlaygistContainer open(String owner, Git git) throws Exception {
    MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
    PlaygistContainer container = new PlaygistContainer(owner, git, sha1);
    addPlaygistsRecursively(container, git.getRepositoryPath(), PLAYGIST_FILE_FILTER);
    return container;
  }

  private static void addPlaygistsRecursively(PlaygistContainer container, File pathname,
      FileFilter filter) {
    if (pathname.isDirectory()) {
      for (File f : pathname.listFiles()) {
        if (!f.getName().startsWith(".") && f.isDirectory()) {
          addPlaygistsRecursively(container, f, filter);
        } else if (filter.accept(f)) {
          try {
            Playgist gist = Playgist.open(f);
            container.addPlaylist(gist);
            log.info("Adding {}", gist.getId());
          } catch (Exception e) {
            log.warn("Failed to open gist", e);
          }
        }
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
    git.add(new File(getOwner(), gist.getId()));
    git.commit("added playlist: " + gist.getName());
    addPlaylist(gist);
  }

  /**
   * Returns the path to this container's owner.
   * 
   * @return
   */
  private File getOwnerRoot() {
    return new File(git.getRepositoryPath(), getOwner());
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
