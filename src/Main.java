import orchestra.Maestro;
import orchestra.playlist.Playlist;
import orchestra.playlist.PlaylistContainer;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.util.Hex;


public class Main {
  public static void main(String[] args) throws Exception {
    Maestro maestro = Maestro.newInstance("liesen");
    maestro.login("liesen", "m0n0l1th");
    
    // Get the playlist container again for clarity
    PlaylistContainer container = maestro.getPlaylistContainer();
    
    for (Playlist playlist : container) {
      System.out.printf("%s (%s) [%s]%n", playlist.getName(), playlist.getId(), playlist.getClass().getName());
    }
    
    Playlist playlist = container.newPlaylist("Spring Ricco");
    playlist.addTrack(new Track(Hex.URIToId("241Oo8Eb4YoaL9mYS6VPHX"), null, null, null));
    
    
  }
}
