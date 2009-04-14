package orchestra.playlist;

import java.util.List;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import de.felixbruns.jotify.media.Track;


public class JotifyPlaylist extends de.felixbruns.jotify.media.Playlist implements Playlist {
  private boolean collaborative;
  private boolean dirty;
  
  /**
   * 
   */
  protected JotifyPlaylist() {
    super();
  }

  /**
   * @param id
   * @param name
   * @param author
   */
  protected JotifyPlaylist(String id, String name, String author) {
    super(id, name, author);
  }

  public Playlist addTrack(int index, Track track) {
    getTracks().add(index, track);
    return this;
  }

  public Playlist addTrack(Track track) {
    getTracks().add(track);
    return this;
  }

  public Playlist addTracks(List<Track> tracks) {
    getTracks().addAll(tracks);
    return this;
  }

  public String getOwner() {
    return getAuthor();
  }

  public int getRevision() {
    return 0;
  }

  public boolean isCollaborative() {
    return collaborative;
  }

  public boolean isDirty() {
    return dirty;
  }

  public Playlist removeTrack(Track track) {
    getTracks().remove(track);
    return this;
  }

  public Playlist setCollaborative(boolean collaborative) {
    this.collaborative = collaborative;
    return this;
  }

  public Playlist setDirty(boolean dirty) {
    this.dirty = dirty;
    return this;
  }

  public Playlist setName(String name) {
    throw new NotImplementedException();
  }

  public Playlist setOwner(String owner) {
    throw new NotImplementedException();
  }
}
