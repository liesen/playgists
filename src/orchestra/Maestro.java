package orchestra;

import java.io.File;
import java.io.IOException;

import orchestra.playlist.PlaylistContainer;
import orchestra.playlist.git.PlaygistContainer;
import orchestra.util.Git;
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
  
  public static Maestro newInstance(String username) throws Exception {
    final String playlistRootPath = "/tmp/orchestra";
    Git git = new Git(new File(playlistRootPath), true);
    PlaylistContainer playlists = PlaygistContainer.open(username, git);
    return new Maestro(playlists);
  }
  
  public PlaylistContainer getPlaylistContainer() throws IOException {
    return playlists;
  }
}
