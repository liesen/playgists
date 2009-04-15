import java.io.File;

import orchestra.playlist.Playlist;
import orchestra.playlist.PlaylistContainer;
import orchestra.playlist.git.PlaygistContainer;

import org.spearce.jgit.lib.Repository;

import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.util.Hex;

public class Test {
  public static String[] S = new String[] {
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
    PlaylistContainer container = PlaygistContainer.open("liesen", new Repository(new File("/Users/liesen/playgists/.git")));
    Playlist pl = container.createPlaylist("Spring Ricco");
    
    for (String s : S) {
      Track tr = new Track(Hex.URIToId(s), null, null, null);
      pl.addTrack(tr);
    }
    
    pl.setName("Florence Valentin - Spring Ricco");
  }
}
