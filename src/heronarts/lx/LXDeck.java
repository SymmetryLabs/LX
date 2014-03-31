/**
 * ##library.name##
 * ##library.sentence##
 * ##library.url##
 *
 * Copyright ##copyright## ##author##
 * All Rights Reserved
 * 
 * @author      ##author##
 * @modified    ##date##
 * @version     ##library.prettyVersion## (##library.version##)
 */

package heronarts.lx;

import heronarts.lx.parameter.BasicParameter;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.transition.DissolveTransition;
import heronarts.lx.transition.LXTransition;

import java.util.ArrayList;
import java.util.List;

/**
 * A deck is a single component of the engine that has a set of patterns from
 * which it plays and rotates. It also has a fader to control how this deck is
 * blended with the decks before it.
 */
public class LXDeck {

  /**
   * Listener interface for objects which want to be notified when the internal
   * deck state is modified.
   */
  public interface Listener {
    public void patternWillChange(LXDeck deck, LXPattern pattern,
        LXPattern nextPattern);

    public void patternDidChange(LXDeck deck, LXPattern pattern);

    public void faderTransitionDidChange(LXDeck deck,
        LXTransition faderTransition);
  }

  /**
   * Utility class to extend in cases where only some methods need overriding.
   */
  public abstract static class AbstractListener implements Listener {
    public void patternWillChange(LXDeck deck, LXPattern pattern,
        LXPattern nextPattern) {
    }

    public void patternDidChange(LXDeck deck, LXPattern pattern) {
    }

    public void faderTransitionDidChange(LXDeck deck,
        LXTransition faderTransition) {
    }
  }

  public class Timer {
    public long runNanos = 0;
  }

  public final Timer timer = new Timer();

  private final LX lx;

  /**
   * The index of this deck in the engine.
   */
  public final int index;

  private LXPattern[] patterns;
  private int activePatternIndex = 0;
  private int nextPatternIndex = 0;

  private boolean autoTransitionEnabled = false;
  private int autoTransitionThreshold = 0;

  LXTransition faderTransition = null;
  final BasicParameter fader = new BasicParameter("FADER", 0);

  private LXTransition transition = null;
  private long transitionMillis = 0;

  private final List<Listener> listeners = new ArrayList<Listener>();

  LXDeck(LX lx, int index, LXPattern[] patterns) {
    this.lx = lx;
    this.index = index;
    this.faderTransition = new DissolveTransition(lx);
    this.transitionMillis = System.currentTimeMillis();
    _updatePatterns(patterns);
  }

  public final void addListener(Listener listener) {
    synchronized (listeners) {
      listeners.add(listener);
    }
  }

  public final void removeListener(Listener listener) {
    synchronized (listeners) {
      listeners.remove(listener);
    }
  }

  private final void notifyPatternWillChange(LXPattern pattern,
      LXPattern nextPattern) {
    synchronized (listeners) {
      for (Listener listener : listeners) {
        listener.patternWillChange(this, pattern, nextPattern);
      }
    }
  }

  private final void notifyPatternDidChange(LXPattern pattern) {
    synchronized (listeners) {
      for (Listener listener : listeners) {
        listener.patternDidChange(this, pattern);
      }
    }
  }

  private final void notifyFaderTransitionDidChange(LXTransition transition) {
    synchronized (listeners) {
      for (Listener listener : listeners) {
        listener.faderTransitionDidChange(this, transition);
      }
    }
  }

  public final BasicParameter getFader() {
    return this.fader;
  }

  public synchronized final LXPattern[] getPatterns() {
    return this.patterns;
  }

  public synchronized final LXTransition getFaderTransition() {
    return this.faderTransition;
  }

  public synchronized final LXDeck setFaderTransition(LXTransition transition) {
    if (this.faderTransition != transition) {
      this.faderTransition = transition;
      notifyFaderTransitionDidChange(transition);
    }
    return this;
  }

  public synchronized final LXDeck setPatterns(LXPattern[] patterns) {
    getActivePattern().onInactive();
    _updatePatterns(patterns);
    this.activePatternIndex = this.nextPatternIndex = 0;
    this.transition = null;
    getActivePattern().onActive();
    return this;
  }

  private void _updatePatterns(LXPattern[] patterns) {
    for (LXPattern p : patterns) {
      if (p.getDeck() != this) {
        p.setDeck(this);
      }
    }
    this.patterns = patterns;
  }

  public synchronized final int getActivePatternIndex() {
    return this.activePatternIndex;
  }

  public synchronized final LXPattern getActivePattern() {
    return this.patterns[this.activePatternIndex];
  }

  public synchronized final int getNextPatternIndex() {
    return this.nextPatternIndex;
  }

  public synchronized final LXPattern getNextPattern() {
    return this.patterns[this.nextPatternIndex];
  }

  protected synchronized final LXTransition getActiveTransition() {
    return this.transition;
  }

  public synchronized final LXDeck goPrev() {
    if (this.transition != null) {
      return this;
    }
    this.nextPatternIndex = this.activePatternIndex - 1;
    if (this.nextPatternIndex < 0) {
      this.nextPatternIndex = this.patterns.length - 1;
    }
    startTransition();
    return this;
  }

  public synchronized final LXDeck goNext() {
    if (this.transition != null) {
      return this;
    }
    this.nextPatternIndex = this.activePatternIndex;
    do {
      this.nextPatternIndex = (this.nextPatternIndex + 1)
          % this.patterns.length;
    } while ((this.nextPatternIndex != this.activePatternIndex)
        && !getNextPattern().isEligible());
    if (this.nextPatternIndex != this.activePatternIndex) {
      startTransition();
    }
    return this;
  }

  public synchronized final LXDeck goPattern(LXPattern pattern) {
    for (int i = 0; i < this.patterns.length; ++i) {
      if (this.patterns[i] == pattern) {
        return goIndex(i);
      }
    }
    return this;
  }

  public synchronized final LXDeck goIndex(int i) {
    if (this.transition != null) {
      return this;
    }
    if (i < 0 || i >= this.patterns.length) {
      return this;
    }
    this.nextPatternIndex = i;
    startTransition();
    return this;
  }

  protected synchronized LXDeck disableAutoTransition() {
    this.autoTransitionEnabled = false;
    return this;
  }

  protected synchronized LXDeck enableAutoTransition(int autoTransitionThreshold) {
    this.autoTransitionEnabled = true;
    this.autoTransitionThreshold = autoTransitionThreshold;
    if (this.transition == null) {
      this.transitionMillis = System.currentTimeMillis();
    }
    return this;
  }

  protected synchronized boolean isAutoTransitionEnabled() {
    return this.autoTransitionEnabled;
  }

  private synchronized void startTransition() {
    if (getActivePattern() == getNextPattern()) {
      return;
    }
    getNextPattern().onActive();
    notifyPatternWillChange(getActivePattern(), getNextPattern());
    this.transition = getNextPattern().getTransition();
    if (this.transition == null) {
      finishTransition();
    } else {
      getNextPattern().onTransitionStart();
      this.transition.blend(getActivePattern().getColors(), getNextPattern()
          .getColors(), 0, 0);
      this.transitionMillis = System.currentTimeMillis();
    }
  }

  private synchronized void finishTransition() {
    getActivePattern().onInactive();
    this.activePatternIndex = this.nextPatternIndex;
    if (this.transition != null) {
      getActivePattern().onTransitionEnd();
    }
    this.transition = null;
    this.transitionMillis = System.currentTimeMillis();
    notifyPatternDidChange(getActivePattern());
  }

  synchronized void run(long nowMillis, double deltaMs) {
    long runStart = System.nanoTime();

    // Run active pattern
    getActivePattern().go(deltaMs);

    // Run transition if applicable
    if (this.transition != null) {
      int transitionMs = (int) (nowMillis - this.transitionMillis);
      if (transitionMs >= this.transition.getDuration()) {
        finishTransition();
      } else {
        getNextPattern().go(deltaMs);
        this.transition.blend(getActivePattern().getColors(), getNextPattern()
            .getColors(),
            (double) transitionMs / this.transition.getDuration(), deltaMs);
      }
    } else {
      if (this.autoTransitionEnabled
          && (nowMillis - this.transitionMillis > this.autoTransitionThreshold)) {
        goNext();
      }
    }

    this.timer.runNanos = System.nanoTime() - runStart;
  }

  public synchronized void copyBuffer(int[] buffer) {
    int[] colors = getColors();
    for (int i = 0; i < colors.length; ++i) {
      buffer[i] = colors[i];
    }
  }

  public synchronized int[] getColors() {
    return (this.transition != null) ? this.transition.getColors() : this
        .getActivePattern().getColors();
  }
}
