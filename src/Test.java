import java.io.File;

import orchestra.playlist.Playlist;
import orchestra.playlist.PlaylistContainer;
import orchestra.playlist.git.PlaygistContainer;
import orchestra.util.Base62;
import orchestra.util.Git;

import org.spearce.jgit.lib.Repository;

import de.felixbruns.jotify.media.Track;

public class Test {
  public static String[] tracks = new String[] {
    "241Oo8Eb4YoaL9mYS6VPHX",
    "2FcWWewZMPQ4xJx1JFOlAM",
    "0mdu7rRY16Kbn3zoqMyRPl",
    "7ycs5VA6Xyf4o3vLe94hYH",
    "4M8M396xWeOTKrZoNTehGd",
    "0O6k4nkMPNlwTWn27HIled",
    "0wry0VT5hvaihRxpxbIk3q",
    "28dguoszXn5JwQa0CIA8ai",
    "7yxyxDs7OCiTN0wu8da5mB"
  };
  
  public static void main(String[] args) throws Exception {
    final Repository repo = new Repository(new File("/Users/liesen/playgists/.git"));
    final Git git = new Git(repo);
    final PlaylistContainer container = PlaygistContainer.open("liesen", git);
    final Playlist pl = container.createPlaylist("Spring Ricco");
    
    for (String s : tracks) {
      Track tr = new Track(Base62.decodeBigInteger(s).toString(16), null, null, null);
      pl.addTrack(tr);
    }
    
    pl.setName("Florence Valentin - Spring Ricco");
  }
}
