package orchestra.playlist.git;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import orchestra.playlist.Playlist;
import orchestra.playlist.PlaylistListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.felixbruns.jotify.media.Track;

public class Playgist implements Playlist {
  private static final Logger log = LoggerFactory.getLogger(Playgist.class);
  
  public static final String METADATA_PREFIX = "> ";
  
  private static final String COLLABORATIVE_PROPERTY_NAME = "collaborative";
  private static final String NAME_PROPERTY_NAME = "name";
  
  private final Map<String, String> metadata;

  /** Path to playlist file on disk. */
  public final File path;
  
  private PlaylistListener listener;

  private final LinkedList<Track> tracks;

  private boolean dirty;

  private Playgist(File path, List<Track> tracks) {
    this(path, tracks, new TreeMap<String, String>(), null);
  }

  private Playgist(File path, List<Track> tracks, Map<String, String> metadata, PlaylistListener listener) {
    this.metadata = metadata;
    this.path = path;
    this.tracks = new LinkedList<Track>(tracks);
    this.listener = null;
  }

  /**
   * Opens a playlist file. Reads tracks and metadata.
   * 
   * @param filename
   * @return
   * @throws FileNotFoundException
   * @throws IOException
   */
  public static Playgist open(File file) throws FileNotFoundException, IOException {
    BufferedReader reader = new BufferedReader(new FileReader(file));
    Map<String, String> metadata = new TreeMap<String, String>();
    List<Track> tracks = new LinkedList<Track>();

    for (String line; (line = reader.readLine()) != null;) {
      if (line.startsWith(METADATA_PREFIX)) {
        parseMetadata(line, metadata);
      } else {
        tracks.add(new Track(line, null, null, null));
      }
    }

    return new Playgist(file, tracks, metadata, null);
  }

  private static void parseMetadata(String line, Map<String, String> metadata) throws IOException {
    Properties props = new Properties();
    props.load(new StringReader(line.substring(METADATA_PREFIX.length())));
    log.info("Reading property line: {}", line);
    
    for (String key : props.stringPropertyNames()) {
      log.info("Reading property: {}", key);
      metadata.put(key, props.getProperty(key));
    }
  }
  
  public Playlist setName(String name) {
    String currentName = getName();
    
    if (currentName == null || !currentName.equals(name)) {
      metadata.put(NAME_PROPERTY_NAME, name);
      notifyListeners();
    }
    
    return this;
  }
  
  public String getName() {
    return metadata.get(NAME_PROPERTY_NAME);
  }
  
  public Playlist setCollaborative(boolean collaborative) {
    if (!isCollaborative() && collaborative) {
      metadata.put(COLLABORATIVE_PROPERTY_NAME, "true");
      notifyListeners();
    }
    
    return this;
  }
  
  public boolean isCollaborative() {
    if (!metadata.containsKey(COLLABORATIVE_PROPERTY_NAME)) {
      return false;
    }
    
    return metadata.get(COLLABORATIVE_PROPERTY_NAME).equalsIgnoreCase("true");
  }

  public Playlist addTrack(int index, Track track) {
    tracks.add(index, track);
    notifyListeners();
    return this;
  }
  
  public Playlist addTracks(List<Track> tracks) {
    tracks.addAll(tracks); // Batch
    notifyListeners();
    return this;
  }

  public List<Track> getTracks() {
    return Collections.unmodifiableList(tracks);
  }

  public Playlist removeTrack(Track track) {
    tracks.remove(track);
    notifyListeners();
    return this;
  }
  
  /**
   * Tries to write the playlist to disk. If it fails then the playlist is set
   * to a dirty state.
   */
  private void notifyListeners() {
    if (listener != null) {
      listener.hasChanged(this);
    }
  }

  public String getId() {
    return path.getName();
  }

  /** Returns the location (on disk) for this gist. */
  protected File getPath() {
    return path;
  }

  public Map<String, String> getMetadata() {
    return Collections.unmodifiableMap(metadata);
  }

  public Playlist addTrack(Track track) {
    tracks.add(track);
    notifyListeners();
    return this;
  }

  public String getOwner() {
    throw new UnsupportedOperationException();
  }

  public int getRevision() {
    throw new UnsupportedOperationException();
  }

  public boolean isDirty() {
    return dirty;
  }

  public Playlist setDirty(boolean dirty) {
    this.dirty = dirty;
    return this;
  }

  public Playlist setOwner(String owner) {
    throw new UnsupportedOperationException();
  }

  public void setListener(PlaylistListener listener) {
    this.listener = listener;
  }
}
