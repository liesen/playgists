package orchestra;

import java.io.IOException;

import orchestra.playlist.PlaylistContainer;
import orchestra.playlist.git.PlaygistContainer;

import org.spearce.jgit.lib.Repository;

import de.felixbruns.jotify.Jotify;

public class Maestro extends Jotify {
  private PlaylistContainer playlists;

  /**
   * 
   */
  private Maestro(PlaylistContainer container) {
    super();
    playlists = container;
  }
  
  public static Maestro newInstance(String username, Repository repo) throws Exception {
    PlaylistContainer playlists = PlaygistContainer.open(username, repo);
    return new Maestro(playlists);
  }
  
  public PlaylistContainer getPlaylistContainer() throws IOException {
    return playlists;
  }
}
