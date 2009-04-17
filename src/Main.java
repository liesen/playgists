import java.io.File;
import java.util.LinkedList;
import java.util.List;

import orchestra.Maestro;
import orchestra.util.Git;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spearce.jgit.lib.Repository;

import de.felixbruns.jotify.gui.JotifyApplication;
import de.felixbruns.jotify.gui.listeners.JotifyBroadcast;
import de.felixbruns.jotify.gui.listeners.PlaylistListener;
import de.felixbruns.jotify.media.Playlist;


public class Main {
  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws Exception {
    final Repository repo = new Repository(new File("/Users/liesen/playgists/.git"));
    final Git git = new Git(repo);
    final Maestro maestro = Maestro.newInstance("liesen", git);
    
    final JotifyBroadcast broadcaster = JotifyBroadcast.getInstance(); // Love
    // static!
    LOGGER.info("Broadcast instance: {}", broadcaster);

    PlaylistListener playlistListener = new LoggingPlaylistListener();
    broadcaster.addPlaylistListener(playlistListener);

    JotifyApplication app = new JotifyApplication(maestro);
    app.initialize();
  }

  static class LoggingPlaylistListener implements PlaylistListener {
    private int numPlaylists;
    
    /**
     * @param broadcaster
     */
    public LoggingPlaylistListener() {
    }

    public void playlistAdded(Playlist playlist) {
      LOGGER.info("Adding playlist '{}' (count: {})", playlist.getName(), ++numPlaylists);
    }

    public void playlistRemoved(Playlist playlist) {
    }

    public void playlistSelected(Playlist playlist) {
    }

    public void playlistUpdated(Playlist playlist) {
      LOGGER.info("Playlist '{}' updated", playlist.getName());
    }
  }
}
