package orchestra;

import java.net.URI;

import orchestra.playlist.JotifyPlaylist;
import orchestra.playlist.Playlist;
import orchestra.playlist.git.PlaygistContainer;
import orchestra.util.Git;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.felixbruns.jotify.JotifyPool;
import de.felixbruns.jotify.media.PlaylistContainer;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.player.PlaybackListener;

public class Maestro extends JotifyPool {
  private static final Logger LOGGER = LoggerFactory.getLogger(Maestro.class);

  private PlaygistContainer playgists;
  
  private boolean canPlayMusic = true;

  /**
   * 
   */
  private Maestro(PlaygistContainer container) {
    super(4);
    playgists = container;
  }

  public static Maestro newInstance(String username, Git git) throws Exception {
    PlaygistContainer playlists = PlaygistContainer.open(username, git);
    LOGGER.info("Using playgist container: {}", playlists.getPlaylists());
    
    return new Maestro(playlists);
  }
  
  @Override
  public void play(Track track, PlaybackListener listener) {
    if (canPlayMusic) {
      // Catch all exceptions when trying to play music -- somewhere an NPE is thrown :( 
      try {
        super.play(track, listener);
        return;
      } catch (Exception e) {
        canPlayMusic = false;
        LOGGER.warn("Could not play " + track, e);
      }
    } else {
      LOGGER.debug("Can not play music");
    }
  }

  @Override
  public de.felixbruns.jotify.media.Playlist playlist(String id) {
    LOGGER.info("Fetching playlist: {}", id);

    try {
      URI identifier = URI.create(id); 
      
      if (identifier.getScheme().equals("orchestra")) {
        Playlist playgist = playgists.getPlaylist(identifier);

        if (playgist != null) {
          return new JotifyPlaylist(playgist);
        }

        return null; // Otherwise Jotify fails
      }
    } catch (IllegalArgumentException e) {
      LOGGER.info("Failed creating a URI from the playlist ID", e);
    }

    return super.playlist(id);
  }

  @Override
  public PlaylistContainer playlists() {
    LOGGER.info("Fetching playlists from Spotify");
    PlaylistContainer playlists = super.playlists();
    LOGGER.info("Received {} playlists", playlists.getPlaylists().size());

    for (Playlist pl : playgists) {
      // Add new playlists first since Jotify stops updating playlists if one
      // update fails
      playlists.getPlaylists().add(0, new JotifyPlaylist(pl));
      LOGGER.info("Added {}", pl.getName());
    }
    
    return playlists;
  }
}
