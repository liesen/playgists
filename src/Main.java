import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import orchestra.Maestro;
import orchestra.util.Git;
import orchestra.util.TrackIdComparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spearce.jgit.lib.Repository;

import de.felixbruns.jotify.Jotify;
import de.felixbruns.jotify.gui.JotifyApplication;
import de.felixbruns.jotify.gui.listeners.JotifyBroadcast;
import de.felixbruns.jotify.gui.listeners.PlaylistListener;
import de.felixbruns.jotify.gui.util.JotifyPool;
import de.felixbruns.jotify.media.Playlist;
import de.felixbruns.jotify.media.Result;
import de.felixbruns.jotify.media.Track;



public class Main {
  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws Exception {
    final Repository repo = new Repository(new File("/Users/liesen/playgists/.git"));
    final Git git = new Git(repo);
    final Maestro maestro = Maestro.newInstance("liesen", git);

    final JotifyBroadcast broadcaster = JotifyBroadcast.getInstance(); // Love
    // static!
    LOGGER.info("Broadcast instance: {}", broadcaster);

    PlaylistListener playlistListener = new SneakyPlaylistListener(broadcaster, maestro);
    broadcaster.addPlaylistListener(playlistListener);

    JotifyApplication app = new JotifyApplication();
    app.initialize();
  }

  static class SneakyPlaylistListener implements PlaylistListener {
    private final JotifyBroadcast broadcaster;
    private int numPlaylists;
    private final Maestro maestro;
    private List<Playlist> newPlaylists;
    private volatile boolean update = false;

    /**
     * @param broadcaster
     */
    public SneakyPlaylistListener(JotifyBroadcast broadcaster, Maestro maestro) {
      this.broadcaster = broadcaster;
      this.maestro = maestro;
      newPlaylists = new LinkedList<Playlist>(maestro.playlists().getPlaylists());
    }

    public void playlistAdded(Playlist playlist) {
      if (newPlaylists.size() > 0) {
        LOGGER.info("Aadding new playlist");

        Playlist newPlaylist = newPlaylists.remove(0);
        broadcaster.firePlaylistAdded(newPlaylist); // Will cause the next
        // playlist to be added
        update = true;
      }

      LOGGER.info("Adding playlist '{}' (count: {})", playlist.getName(), ++numPlaylists);
    }

    public void playlistRemoved(Playlist playlist) {
    }

    public void playlistSelected(Playlist playlist) {
    }

    public void playlistUpdated(Playlist playlist) {
      LOGGER.info("Playlist '{}' updated", playlist.getName());

      if (update) {
        newPlaylists = maestro.playlists().getPlaylists();
        
        for (final Playlist newPlaylist : newPlaylists) {
          // We don't get track resolving for free

          // Thank god for singletons! X(
          try {
            final Jotify jotify = JotifyPool.getInstance();
            LOGGER.info("Updating the track list using {}", jotify);
            final Result result = jotify.browse(playlist.getTracks());
            newPlaylist.setTracks(result.getTracks());
          } catch (Exception e) {
            LOGGER.info("Could not get hold of the Jotify instance", e);
          }
        }
        
        update = false;
        
        for (final Playlist newPlaylist : newPlaylists) {
          broadcaster.firePlaylistUpdated(newPlaylist);
        }
      }
    }
  }
}
