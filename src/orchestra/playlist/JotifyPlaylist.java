package orchestra.playlist;

import java.util.Iterator;
import java.util.List;

import de.felixbruns.jotify.media.Track;


/**
 * Wrapper for a Jotify playlist. Let's us do crazy stuff behind the scenes.
 * 
 */
public class JotifyPlaylist extends de.felixbruns.jotify.media.Playlist {
  private final Playlist playlist;

  /**
   * @param playlist
   */
  public JotifyPlaylist(Playlist playlist) {
    this.playlist = playlist;
  }

  @Override
  public String getAuthor() {
    return playlist.getAuthor();
  }

  @Override
  public long getChecksum() {
    return 0;
  }

  @Override
  public String getId() {
    return playlist.getIdentifier();
  }

  @Override
  public String getName() {
    return playlist.getName();
  }

  @Override
  public long getRevision() {
    return playlist.getRevision();
  }

  @Override
  public List<Track> getTracks() {
    return playlist.getTracks();
  }

  @Override
  public boolean hasTracks() {
    return playlist.getTracks().size() > 0;
  }

  @Override
  public boolean isCollaborative() {
    return playlist.isCollaborative();
  }

  @Override
  public Iterator<Track> iterator() {
    return playlist.getTracks().iterator();
  }

  @Override
  public void setAuthor(String author) {
    playlist.setAuthor(author);
  }

  @Override
  public void setChecksum(long checksum) {
    // throw new UnsupportedOperationException();
  }

  @Override
  public void setCollaborative(boolean collaborative) {
    playlist.setCollaborative(collaborative);
  }

  @Override
  public void setId(String id) {
    // What? NO!
    throw new UnsupportedOperationException();
  }

  @Override
  public void setName(String name) {
    playlist.setName(name);
  }

  @Override
  public void setRevision(long revision) {
    // throw new UnsupportedOperationException();
  }

  @Override
  public void setTracks(List<Track> tracks) {
    playlist.setTracks(tracks);
  }
  
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    
    if (!(o instanceof Playlist)) {
      return false;
    }
    
    return getId().equalsIgnoreCase(((Playlist) o).getIdentifier());
  }
}
