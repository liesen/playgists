package orchestra.playlist;

import java.util.List;

import de.felixbruns.jotify.media.Track;

public interface Playlist {

  public String getId();

  public String getName();

  public Playlist setName(String name);

  public String getOwner();

  public Playlist setOwner(String owner);

  public int getRevision();

  public boolean isCollaborative();

  public Playlist setCollaborative(boolean collaborative);

  public boolean isDirty();

  public Playlist setDirty(boolean dirty);

  public Playlist addTrack(int index, Track track);

  public Playlist addTrack(Track track);

  public Playlist addTracks(List<Track> tracks);

  public Playlist removeTrack(Track track);

  public List<Track> getTracks();
}
