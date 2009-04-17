package orchestra;

import java.io.IOException;

import orchestra.playlist.JotifyPlaylist;
import orchestra.playlist.Playlist;
import orchestra.playlist.PlaylistContainer;
import orchestra.playlist.git.PlaygistContainer;
import orchestra.util.Git;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.felixbruns.jotify.Jotify;
import de.felixbruns.jotify.media.Playlists;

public class Maestro extends Jotify {
  private static final Logger LOGGER = LoggerFactory.getLogger(Maestro.class);

  private PlaylistContainer playlists;

  /**
   * 
   */
  private Maestro(PlaylistContainer container) {
    super();
    playlists = container;
  }

  public static Maestro newInstance(String username, Git git) throws Exception {
    PlaylistContainer playlists = PlaygistContainer.open(username, git);
    return new Maestro(playlists);
  }

  public PlaylistContainer getPlaylistContainer() throws IOException {
    return playlists;
  }

  @Override
  public Playlists playlists() {
    Playlists pls = super.playlists();

    for (Playlist pl : playlists) {
      // Add new playlists first since Jotify stops updating playlists if one
      // update fails
      pls.getPlaylists().add(0, new JotifyPlaylist(pl));
    }

    return pls;
  }

  @Override
  public de.felixbruns.jotify.media.Playlist playlist(String id, boolean useCache) {
    LOGGER.info("Fetching playlist: {}", id);

    // TODO(liesen): This is kind of not the way we'd want to distinguish
    // between types of playlists
    if (id.length() == 40) {
      Playlist playgist = playlists.getPlaylist(id);

      if (playgist != null) {
        return new JotifyPlaylist(playgist);
      }

      return null; // Otherwise Jotify fails
    }

    return super.playlist(id, useCache);
  }
}
