package orchestra.playlist.git;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import orchestra.playlist.Playlist;
import orchestra.playlist.PlaylistListener;
import de.felixbruns.jotify.media.Track;

public class Playgist extends Playlist {
  public static final String METADATA_PREFIX = "> ";

  private static final String COLLABORATIVE_PROPERTY_NAME = "collaborative";
  private static final String NAME_PROPERTY_NAME = "name";

  private final Map<String, String> metadata;

  /** Path to the file on disk. */
  private final File absolutePath;
  
  private final URI identifier;

  private final List<PlaylistListener> listeners;

  private final List<Track> tracks;

  private boolean dirty;

  private Playgist(File path, List<Track> tracks, Map<String, String> metadata) {
    super(metadata.get(NAME_PROPERTY_NAME), "<Unknown author>");
    
    super.setCollaborative(metadata.containsKey(COLLABORATIVE_PROPERTY_NAME)
        && metadata.get(COLLABORATIVE_PROPERTY_NAME).equalsIgnoreCase("true"));
    
    this.metadata = metadata;
    this.absolutePath = path;
    this.identifier = URI.create("orchestra:playlist:" + path.getName());
    this.tracks = new LinkedList<Track>(tracks);
    this.listeners = new LinkedList<PlaylistListener>();
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
        parseMetadataLine(line, metadata);
      } else {
        tracks.add(new Track(line, null, null, null));
      }
    }

    return new Playgist(file.getAbsoluteFile(), tracks, metadata);
  }

  private static void parseMetadataLine(String line, Map<String, String> metadata)
      throws IOException {
    Properties props = new Properties();
    props.load(new StringReader(line.substring(METADATA_PREFIX.length())));

    for (String key : props.stringPropertyNames()) {
      metadata.put(key, props.getProperty(key));
    }
  }

  @Override
  public URI getIdentifier() {
    return identifier;
  }

  /**
   * @return the path to the file on disk.
   */
  public File getPath() {
    return absolutePath;
  }

  // TODO(liesen): remove this asap and use getName etc. instead
  public Map<String, String> getMetadata() {
    return Collections.unmodifiableMap(metadata);
  }

  @Override
  public Playlist setName(String name) {
    if (name == null) {
      throw new IllegalArgumentException("New name can not be null");
    }
    
    String currentName = getName();

    if (currentName == null || !currentName.equals(name)) {
      metadata.put(NAME_PROPERTY_NAME, name);
      super.setName(name);
      notifyListeners();
    }

    return this;
  }

  @Override
  public Playlist setCollaborative(boolean collaborative) {
    if (!isCollaborative() && collaborative) {
      metadata.put(COLLABORATIVE_PROPERTY_NAME, "true");
      super.setCollaborative(true);
      notifyListeners();
    } else if (isCollaborative() && !collaborative) {
      metadata.put(COLLABORATIVE_PROPERTY_NAME, "false");
      super.setCollaborative(false);
      notifyListeners();
    }
    
    return this;
  }

  public boolean isDirty() {
    return dirty;
  }

  public Playlist setDirty(boolean dirty) {
    this.dirty = dirty;
    return this;
  }

  @Override
  public Playlist addTrack(int index, Track track) {
    tracks.add(index, track);
    notifyListeners();
    return this;
  }

  @Override
  public Playlist addTrack(Track track) {
    tracks.add(track);
    notifyListeners();
    return this;
  }

  @Override
  public Playlist addTracks(List<Track> tracks) {
    tracks.addAll(tracks); // Batch
    notifyListeners();
    return this;
  }

  @Override
  public List<Track> getTracks() {
    return Collections.unmodifiableList(tracks);
  }

  @Override
  public Playlist removeTrack(Track track) {
    tracks.remove(track);
    notifyListeners();
    return this;
  }

  @Override
  public Playlist removeTracks(List<Track> tracks) {
    this.tracks.removeAll(tracks);
    notifyListeners();
    return this;
  }

  @Override
  public Playlist setTracks(List<Track> tracks) {
    this.tracks.clear();
    this.tracks.addAll(tracks);
    notifyListeners();
    return this;
  }

  public void addListener(PlaylistListener listener) {
    listeners.add(listener);
  }

  public void removeListener(PlaylistListener listener) {
    listeners.remove(listener);
  }

  /**
   * Notifies interested parties that this playlist has changed.
   */
  private void notifyListeners() {
    for (final PlaylistListener listener : listeners) {
      listener.playlistChanged(this);
    }
  }
}
