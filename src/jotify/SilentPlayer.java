package jotify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.player.PlaybackListener;
import de.felixbruns.jotify.player.Player;

public class SilentPlayer implements Player {
  private static final Logger LOGGER = LoggerFactory.getLogger(SilentPlayer.class);

  public int length() {
    return 0;
  }

  public void pause() {
  }

  public void play(Track track, PlaybackListener listener) {
    LOGGER.warn("Silently playing: {}", track);
    listener.playbackStarted(track);
    listener.playbackFinished(track);
  }

  public void play() {
  }

  public int position() {
    return 0;
  }

  public void stop() {
  }

  public float volume() {
    return 0;
  }

  public void volume(float volume) {
  }
}
