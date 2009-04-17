package orchestra.util;

import java.util.Comparator;

import de.felixbruns.jotify.media.Track;

public class TrackIdComparator implements Comparator<Track> {
  public int compare(Track o1, Track o2) {
    return o1.getId().compareTo(o2.getId());
  }
}
