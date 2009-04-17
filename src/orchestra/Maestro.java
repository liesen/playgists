package orchestra;

import java.io.IOException;

import orchestra.playlist.JotifyPlaylist;
import orchestra.playlist.Playlist;
import orchestra.playlist.PlaylistContainer;
import orchestra.playlist.git.PlaygistContainer;
import orchestra.util.Git;
import de.felixbruns.jotify.Jotify;
import de.felixbruns.jotify.media.Playlists;

public class Maestro extends Jotify {
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
    Playlists pls = new Playlists();
    
    for (Playlist pl : playlists) {
      pls.getPlaylists().add(new JotifyPlaylist(pl));
    }
    
    return pls;
  }
}
