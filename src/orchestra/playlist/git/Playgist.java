package orchestra.playlist.git;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import orchestra.playlist.Playlist;
import orchestra.playlist.PlaylistListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spearce.jgit.lib.TreeEntry;

import de.felixbruns.jotify.media.Track;

public class Playgist implements Playlist, Iterable<Track> {
  private static final Logger log = LoggerFactory.getLogger(Playgist.class);

  public static final String METADATA_PREFIX = "> ";

  private static final String COLLABORATIVE_PROPERTY_NAME = "collaborative";
  private static final String NAME_PROPERTY_NAME = "name";

  private final Map<String, String> metadata;

  /** Entry in the git tree. */
  private TreeEntry treeEntry;

  /** Path to the file on disk. */
  private final File absolutePath;

  private List<PlaylistListener> listeners;

  private final List<Track> tracks;

  private boolean dirty;

  private Playgist(File path, List<Track> tracks, Map<String, String> metadata) {
    this.metadata = metadata;
    this.absolutePath = path;
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
  public static Playgist open(File workDir, File entry) throws FileNotFoundException, IOException {
    File file = new File(workDir, entry.getPath());
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
    log.info("Reading property line: {}", line);

    for (String key : props.stringPropertyNames()) {
      metadata.put(key, props.getProperty(key));
    }
  }

  public String getId() {
    return absolutePath.getName();
  }

  /** Returns the location (on disk) for this gist. */
  public TreeEntry getTreeEntry() {
    return treeEntry;
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

  public String getName() {
    return metadata.get(NAME_PROPERTY_NAME);
  }

  public Playlist setName(String name) {
    String currentName = getName();

    if (currentName == null || !currentName.equals(name)) {
      metadata.put(NAME_PROPERTY_NAME, name);
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

  public Playlist setCollaborative(boolean collaborative) {
    if (!isCollaborative() && collaborative) {
      metadata.put(COLLABORATIVE_PROPERTY_NAME, "true");
      notifyListeners();
    }

    return this;
  }

  public String getAuthor() {
    throw new UnsupportedOperationException();
  }

  public Playlist setAuthor(String owner) {
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

  public Playlist addTrack(int index, Track track) {
    tracks.add(index, track);
    notifyListeners();
    return this;
  }

  public Playlist addTrack(Track track) {
    tracks.add(track);
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

  public Iterator<Track> iterator() {
    return getTracks().iterator();
  }

  public Playlist removeTrack(Track track) {
    tracks.remove(track);
    notifyListeners();
    return this;
  }

  public Playlist removeTracks(List<Track> tracks) {
    tracks.removeAll(tracks);
    notifyListeners();
    return this;
  }

  public Playlist setTracks(List<Track> tracks) {
    tracks.clear();
    tracks.addAll(tracks);
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
