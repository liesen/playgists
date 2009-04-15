package orchestra.playlist;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Base implementation of an playlist container.
 * 
 */
public class PlaylistContainer implements Iterable<Playlist>, PlaylistListener {
  private final Map<String, Playlist> playlists;
  
  private final String owner;
  
  public PlaylistContainer(String owner) {
    this(owner, new HashMap<String, Playlist>());
  }

  /**
   * @param playlists
   */
  public PlaylistContainer(String owner, Map<String, Playlist> playlists) {
    this.owner = owner;
    this.playlists = playlists;
  }

  public String getOwner() {
    return owner;
  }
  
  public int size() {
    return playlists.size();
  }
  
  public PlaylistContainer addPlaylist(Playlist playlist) {
    playlists.put(playlist.getId(), playlist);
    return this;
  }
  
  public PlaylistContainer removePlaylist(Playlist playlist) {
    playlists.remove(playlist.getId());
    return this;
  }
  
  public Playlist createPlaylist(String name) throws Exception {
    throw new NotImplementedException();
  }
  
  public Playlist getPlaylist(String id) {
    return playlists.get(id);
  }

  public Collection<Playlist> getPlaylists() {
    return Collections.unmodifiableCollection(playlists.values());
  }

  public Iterator<Playlist> iterator() {
    return playlists.values().iterator();
  }

  public void hasChanged(Playlist playlist) {
    
  }
}
