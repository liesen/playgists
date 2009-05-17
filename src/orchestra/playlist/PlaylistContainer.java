package orchestra.playlist;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Base implementation of an playlist container.
 * 
 */
public class PlaylistContainer implements Iterable<Playlist>, PlaylistListener {
  private final Map<URI, Playlist> playlists;
  
  private final String author;
  
  public PlaylistContainer(String author) {
    this(author, Collections.<Playlist>emptyList());
  }
  
  public PlaylistContainer(String author, List<Playlist> playlists) {
    this.author = author;
    this.playlists = new HashMap<URI, Playlist>();
    
    for (final Playlist playlist : playlists) {
      addPlaylist(playlist);
    }
  }

  public String getAuthor() {
    return author;
  }
  
  /**
   * Returns the number of playlists.
   * 
   * @return
   */
  public int size() {
    return playlists.size();
  }
  
  public PlaylistContainer addPlaylist(Playlist playlist) {
    playlists.put(playlist.getIdentifier(), playlist);
    return this;
  }
  
  public PlaylistContainer removePlaylist(Playlist playlist) {
    playlists.remove(playlist.getIdentifier());
    return this;
  }
  
  /**
   * Creates a new playlist.
   * 
   * @param name name of the playlist
   * @return
   * @throws Exception
   */
  public Playlist createPlaylist(String name) throws Exception {
    throw new UnsupportedOperationException();
  }
  
  public Playlist getPlaylist(URI identifier) {
    return playlists.get(identifier);
  }

  public Collection<Playlist> getPlaylists() {
    return Collections.unmodifiableCollection(playlists.values());
  }

  public Iterator<Playlist> iterator() {
    return playlists.values().iterator();
  }

  public void playlistChanged(Playlist playlist) {
  }
}
