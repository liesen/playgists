package orchestra.playlist;

import java.util.List;

import de.felixbruns.jotify.media.Track;

public interface Playlist {
  public String getId();

  public String getName();

  public Playlist setName(String name);

  public String getAuthor();

  public Playlist setAuthor(String owner);

  public int getRevision();

  public boolean isCollaborative();

  public Playlist setCollaborative(boolean collaborative);

  public boolean isDirty();

  public Playlist setDirty(boolean dirty);

  public Playlist addTrack(int index, Track track);

  public Playlist addTrack(Track track);

  public Playlist addTracks(List<Track> tracks);

  public Playlist setTracks(List<Track> tracks);

  public Playlist removeTrack(Track track);

  public Playlist removeTracks(List<Track> tracks);

  public List<Track> getTracks();
}
