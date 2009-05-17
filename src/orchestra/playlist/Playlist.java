package orchestra.playlist;

import java.net.URI;
import java.util.Iterator;
import java.util.List;

import de.felixbruns.jotify.media.Track;


/**
 * Base implementation of a playlist.
 * 
 */
public abstract class Playlist implements Iterable<Track> {
  /** Playlist display name */
  private String name;

  /** Author */
  private String author;

  /** Indicates if the playlist is collaborative */
  private boolean collaborative;
  
  /**
   * 
   */
  protected Playlist() {
  }

  /**
   * @param name
   * @param author
   */
  public Playlist(String name, String author) {
    this(name, author, false);
  }

  /**
   * @param name
   * @param author
   * @param collaborative
   */
  public Playlist(String name, String author, boolean collaborative) {
    this.name = name;
    this.author = author;
    this.collaborative = collaborative;
  }

  /**
   * Returns a unique identifier for the playlist.
   * 
   * @return a playlist identifier
   */
  public abstract URI getIdentifier();

  /**
   * @return the display name of the playlist
   */
  public String getName() {
    return name;
  }

  public Playlist setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * @return author of the playlist
   */
  public String getAuthor() {
    return author;
  }

  public Playlist setAuthor(String author) {
    this.author = author;
    return this;
  }

  /**
   * Returns the revision of the playlist. Optional operation: throws
   * {@link UnsupportedOperationException} if not implemented.
   * 
   * @return
   */
  public long getRevision() {
    throw new UnsupportedOperationException();
  }

  public boolean isCollaborative() {
    return collaborative;
  }

  public Playlist setCollaborative(boolean collaborative) {
    this.collaborative = collaborative;
    return this;
  }

  /**
   * Adds a track at a specific index.
   * 
   * @param index
   * @param track
   * @return
   */
  public abstract Playlist addTrack(int index, Track track);

  public Playlist addTrack(Track track) {
    return addTrack(getTracks().size(), track);
  }

  public Playlist addTracks(int index, List<Track> tracks) {
    for (Track track : tracks) {
      addTrack(index++, track);
    }

    return this;
  }

  public Playlist addTracks(List<Track> tracks) {
    return addTracks(getTracks().size(), tracks);
  }

  /**
   * Replaces all tracks with a list of new tracks.
   * 
   * @param tracks
   * @return
   */
  public Playlist setTracks(List<Track> tracks) {
    removeTracks(getTracks());
    return addTracks(tracks);
  }

  /**
   * Removes a track from the playlist.
   * 
   * @param track
   * @return
   */
  // Make sure not to implement this using setTracks because of recursion
  public abstract Playlist removeTrack(Track track);

  public Playlist removeTracks(List<Track> tracks) {
    for (Track track : tracks) {
      removeTrack(track);
    }

    return this;
  }

  /**
   * Returns all tracks that make up the playlist.
   * 
   * @return tracks in the playlist
   */
  public abstract List<Track> getTracks();

  public Iterator<Track> iterator() {
    return getTracks().iterator();
  }
}
