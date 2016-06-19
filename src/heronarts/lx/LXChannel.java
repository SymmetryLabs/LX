/**
 * Copyright 2013- Mark C. Slee, Heron Arts LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package heronarts.lx;

import heronarts.lx.color.LXPalette;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.model.LXModel;
import heronarts.lx.parameter.BasicParameter;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.transition.DissolveTransition;
import heronarts.lx.transition.LXTransition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;

import com.googlecode.concurentlocks.ReadWriteUpdateLock;
import com.googlecode.concurentlocks.ReentrantReadWriteUpdateLock;

/**
 * A channel is a single component of the engine that has a set of patterns from
 * which it plays and rotates. It also has a fader to control how this channel
 * is blended with the channels before it.
 */
public class LXChannel extends LXComponent {

  /**
   * Listener interface for objects which want to be notified when the internal
   * channel state is modified.
   */
  public interface Listener {

    public void effectAdded(LXChannel channel, LXEffect effect);

    public void effectRemoved(LXChannel channel, LXEffect effect);

    public void patternAdded(LXChannel channel, LXPattern pattern);

    public void patternRemoved(LXChannel channel, LXPattern pattern);

    public void patternWillChange(LXChannel channel, LXPattern pattern, LXPattern nextPattern);

    public void patternDidChange(LXChannel channel, LXPattern pattern);

    public void faderTransitionDidChange(LXChannel channel, LXTransition faderTransition);
  }

  /**
   * Utility class to extend in cases where only some methods need overriding.
   */
  public abstract static class AbstractListener implements Listener {

    @Override
    public void effectAdded(LXChannel channel, LXEffect effect) {
    }

    @Override
    public void effectRemoved(LXChannel channel, LXEffect effect) {
    }

    @Override
    public void patternAdded(LXChannel channel, LXPattern pattern) {
    }

    @Override
    public void patternRemoved(LXChannel channel, LXPattern pattern) {
    }

    @Override
    public void patternWillChange(LXChannel channel, LXPattern pattern,
        LXPattern nextPattern) {
    }

    @Override
    public void patternDidChange(LXChannel channel, LXPattern pattern) {
    }

    @Override
    public void faderTransitionDidChange(LXChannel channel,
        LXTransition faderTransition) {
    }
  }

  /**
   * The index of this channel in the engine.
   */
  private int index;

  private final LX lx;

  /**
   * Whether this channel is enabled.
   */
  public final BooleanParameter enabled = new BooleanParameter("ON", true);

  /**
   * Whether this channel should listen to MIDI events
   */
  public final BooleanParameter midiEnabled = new BooleanParameter("MIDI", false);

  /**
   * Whether auto pattern transition is enabled on this channel
   */
  public final BooleanParameter autoTransitionEnabled = new BooleanParameter("AUTO", false);

  private final List<LXPattern> patterns = new ArrayList<LXPattern>();
  private final List<LXPattern> unmodifiablePatterns = Collections.unmodifiableList(patterns);

  private final List<LXEffect> effects = new ArrayList<LXEffect>();
  private final List<LXEffect> unmodifiableEffects = Collections.unmodifiableList(effects);

  private final ModelBuffer buffer;
  private int[] colors;

  private int activePatternIndex = 0;
  private int nextPatternIndex = 0;

  private int autoTransitionThreshold = 60000;

  private LXTransition faderTransition = null;
  private final BasicParameter fader = new BasicParameter("FADER", 0);

  private LXTransition transition = null;
  private long transitionMillis = 0;

  private final List<Listener> listeners = new ArrayList<Listener>();

  private final ReadWriteUpdateLock channelModificationLock = new ReentrantReadWriteUpdateLock();

  LXChannel(LX lx, int index, LXPattern[] patterns) {
    super(lx);
    this.lx = lx;
    this.index = index;
    this.buffer = new ModelBuffer(lx);
    this.faderTransition = new DissolveTransition(lx);
    this.transitionMillis = System.currentTimeMillis();
    _updatePatterns(patterns);
    this.colors = this.getActivePattern().getColors();

    addParameter(this.enabled);
    addParameter(this.midiEnabled);
    addParameter(this.autoTransitionEnabled);

    getActivePattern().onActive();
  }

  @Override
  protected void onModelChanged(LXModel model) {
    for (LXPattern pattern : this.patterns) {
      pattern.setModel(model);
    }
  }

  @Override
  protected void onPaletteChanged(LXPalette palette) {
    for (LXPattern pattern : this.patterns) {
      pattern.setPalette(palette);
    }
  }

  public final void addListener(Listener listener) {
    Lock l = this.channelModificationLock.writeLock();
    l.lock();
    try {
      this.listeners.add(listener);
    } finally {
      l.unlock();
    }
  }

  public final void removeListener(Listener listener) {
    Lock l = this.channelModificationLock.writeLock();
    l.lock();
    try {
      this.listeners.remove(listener);
    } finally {
      l.unlock();
    }
  }

  final LXChannel setIndex(int index) {
    Lock l = this.channelModificationLock.writeLock();
    l.lock();
    try {
      this.index = index;
    } finally {
      l.unlock();
    }
    return this;
  }

  public final int getIndex() {
    Lock l = this.acquireReadLock();
    try {
      return this.index;
    } finally {
      unlock(l);
    }
  }

  public final BasicParameter getFader() {
    return this.fader;
  }

  public final LXTransition getFaderTransition() {
    Lock l = this.acquireReadLock();
    try {
      return this.faderTransition;
    } finally {
      unlock(l);
    }
  }

  public final LXChannel setFaderTransition(LXTransition transition) {
    Lock l = this.channelModificationLock.writeLock();
    l.lock();
    try {
      if (this.faderTransition != transition) {
        this.faderTransition = transition;
        for (Listener listener : this.listeners) {
          listener.faderTransitionDidChange(this, this.faderTransition);
        }
      }
    } finally {
      l.unlock();
    }
    return this;
  }

  public final LXChannel addEffect(LXEffect effect) {
    Lock l = this.channelModificationLock.writeLock();
    l.lock();
    try {
      this.effects.add(effect);
      for (Listener listener : this.listeners) {
        listener.effectAdded(this, effect);
      }
    } finally {
      l.unlock();
    }
    return this;
  }

  public final LXChannel removeEffect(LXEffect effect) {
    Lock l = this.channelModificationLock.writeLock();
    l.lock();
    try {
      this.effects.remove(effect);
      for (Listener listener : this.listeners) {
        listener.effectRemoved(this, effect);
      }
    } finally {
      l.unlock();
    }
    return this;
  }

  public final List<LXEffect> getEffects() {
    Lock l = this.acquireReadLock();
    try {
      return this.unmodifiableEffects;
    } finally {
      unlock(l);
    }
  }

  public final List<LXPattern> getPatterns() {
    Lock l = this.acquireReadLock();
    try {
      return this.unmodifiablePatterns;
    } finally {
      unlock(l);
    }
  }

  public final LXPattern getPattern(String className) {
    Lock l = this.acquireReadLock();
    try {
      for (LXPattern pattern : this.unmodifiablePatterns) {
        if (pattern.getClass().getName().equals(className)) {
          return pattern;
        }
      }
    } finally {
      unlock(l);
    }
    return null;
  }

  public final LXChannel setPatterns(LXPattern[] patterns) {
    Lock l = this.channelModificationLock.writeLock();
    l.lock();
    try {
      getActivePattern().onInactive();
      _updatePatterns(patterns);
      this.activePatternIndex = this.nextPatternIndex = 0;
      this.transition = null;
      getActivePattern().onActive();
    } finally {
      l.unlock();
    }
    return this;
  }

  public final LXChannel addPattern(LXPattern pattern) {
    Lock l = this.channelModificationLock.writeLock();
    l.lock();
    try {
      pattern.setChannel(this);
      ((LXComponent)pattern).setModel(this.model);
      ((LXComponent)pattern).setPalette(this.palette);
      this.patterns.add(pattern);
      for (Listener listener : this.listeners) {
        listener.patternAdded(this, pattern);
      }
    } finally {
      l.unlock();
    }
    return this;
  }

  public final LXChannel removePattern(LXPattern pattern) {
    Lock l = this.channelModificationLock.writeLock();
    l.lock();
    try {
      if (this.patterns.size() <= 1) {
        throw new UnsupportedOperationException("LXChannel must have at least one pattern");
      }
      int index = this.patterns.indexOf(pattern);
      if (index >= 0) {
        this.patterns.remove(index);
        pattern.setChannel(null);
        if (this.activePatternIndex >= index) {
          --this.activePatternIndex;
          if (this.activePatternIndex < 0) {
            this.activePatternIndex = this.patterns.size() - 1;
          }
        }
        if (this.nextPatternIndex >= index) {
          --this.nextPatternIndex;
          if (this.nextPatternIndex < 0) {
            this.nextPatternIndex = this.patterns.size() - 1;
          }
        }
        for (Listener listener : this.listeners) {
          listener.patternRemoved(this, pattern);
        }
      }
    } finally {
      l.unlock();
    }
    return this;
  }

  private void _updatePatterns(LXPattern[] patterns) {
    if (patterns == null) {
      throw new IllegalArgumentException("May not set null pattern array");
    }
    if (patterns.length == 0) {
      throw new IllegalArgumentException("LXChannel must have at least one pattern");
    }
    for (LXPattern pattern : this.patterns) {
      pattern.setChannel(null);
      for (Listener listener : this.listeners) {
        listener.patternRemoved(this, pattern);
      }
    }
    this.patterns.clear();
    for (LXPattern pattern : patterns) {
      if (pattern == null) {
        throw new IllegalArgumentException("Pattern array may not include null elements");
      }
      addPattern(pattern);
    }
  }

  public final int getActivePatternIndex() {
    Lock l = this.acquireReadLock();
    try {
      return this.activePatternIndex;
    } finally {
      unlock(l);
    }
  }

  public final LXPattern getActivePattern() {
    Lock l = this.acquireReadLock();
    try {
      return this.patterns.get(this.activePatternIndex);
    } finally {
      unlock(l);
    }
  }

  public final int getNextPatternIndex() {
    Lock l = this.acquireReadLock();
    try {
      return this.nextPatternIndex;
    } finally {
      unlock(l);
    }
  }

  public final LXPattern getNextPattern() {
    Lock l = this.acquireReadLock();
    try {
      return this.patterns.get(this.nextPatternIndex);
    } finally {
      unlock(l);
    }
  }

  protected final LXTransition getActiveTransition() {
    Lock l = this.acquireReadLock();
    try {
      return this.transition;
    } finally {
      unlock(l);
    }
  }

  public final LXChannel goPrev() {
    Lock l = this.channelModificationLock.writeLock();
    l.lock();
    try {
      if (this.transition != null) {
        return this;
      }
      this.nextPatternIndex = this.activePatternIndex - 1;
      if (this.nextPatternIndex < 0) {
        this.nextPatternIndex = this.patterns.size() - 1;
      }
      startTransition();
    } finally {
      l.unlock();
    }
    return this;
  }

  public final LXChannel goNext() {
    Lock l = this.channelModificationLock.writeLock();
    l.lock();
    try {
      if (this.transition != null) {
        return this;
      }
      this.nextPatternIndex = this.activePatternIndex;
      do {
        this.nextPatternIndex = (this.nextPatternIndex + 1)
            % this.patterns.size();
      } while ((this.nextPatternIndex != this.activePatternIndex)
          && !getNextPattern().isEligible());
      if (this.nextPatternIndex != this.activePatternIndex) {
        startTransition();
      }
    } finally {
      l.unlock();
    }
    return this;
  }

  public final LXChannel goPattern(LXPattern pattern) {
    Lock l = this.channelModificationLock.writeLock();
    l.lock();
    try {
      int pi = 0;
      for (LXPattern p : this.patterns) {
        if (p == pattern) {
          return goIndex(pi);
        }
        ++pi;
      }
    } finally {
      l.unlock();
    }
    return this;
  }

  public final LXChannel goIndex(int i) {
    Lock l = this.channelModificationLock.writeLock();
    l.lock();
    try {
      if (this.transition != null) {
        return this;
      }
      if (i < 0 || i >= this.patterns.size()) {
        return this;
      }
      this.nextPatternIndex = i;
      startTransition();
    } finally {
      l.unlock();
    }
    return this;
  }

  public LXChannel disableAutoTransition() {
    Lock l = this.channelModificationLock.writeLock();
    l.lock();
    try {
      this.autoTransitionEnabled.setValue(false);
    } finally {
      l.unlock();
    }
    return this;
  }

  public LXChannel enableAutoTransition(int autoTransitionThreshold) {
    Lock l = this.channelModificationLock.writeLock();
    l.lock();
    try {
      this.autoTransitionThreshold = autoTransitionThreshold;
      if (!this.autoTransitionEnabled.isOn()) {
        this.autoTransitionEnabled.setValue(true);
        if (this.transition == null) {
          this.transitionMillis = System.currentTimeMillis();
        }
      }
    } finally {
      l.unlock();
    }
    return this;
  }

  public int getAutoTransitionThreshold() {
    Lock l = this.acquireReadLock();
    try {
      return this.autoTransitionThreshold;
    } finally {
      unlock(l);
    }
  }

  public boolean isAutoTransitionEnabled() {
    Lock l = this.acquireReadLock();
    try {
      return this.autoTransitionEnabled.isOn();
    } finally {
      unlock(l);
    }
  }

  private void startTransition() {
    Lock l = this.channelModificationLock.writeLock();
    l.lock();
    try {
      LXPattern activePattern = getActivePattern();
      LXPattern nextPattern = getNextPattern();
      if (activePattern == nextPattern) {
        return;
      }
      nextPattern.onActive();
      for (Listener listener : this.listeners) {
        listener.patternWillChange(this, activePattern, nextPattern);
      }
      this.transition = nextPattern.getTransition();
      if (this.transition == null) {
        finishTransition();
      } else {
        nextPattern.onTransitionStart();
        this.transition.blend(activePattern.getColors(), nextPattern.getColors(), 0);
        this.transitionMillis = System.currentTimeMillis();
      }
    } finally {
      l.unlock();
    }
  }

  private void finishTransition() {
    Lock l = this.channelModificationLock.writeLock();
    l.lock();
    try {
      getActivePattern().onInactive();
      this.activePatternIndex = this.nextPatternIndex;
      LXPattern activePattern = getActivePattern();
      if (this.transition != null) {
        activePattern.onTransitionEnd();
      }
      this.transition = null;
      this.transitionMillis = System.currentTimeMillis();
      for (Listener listener : listeners) {
        listener.patternDidChange(this, activePattern);
      }
    } finally {
      l.unlock();
    }
  }

  @Override
  public void loop(double deltaMs) {
    Lock l = this.channelModificationLock.updateLock();
    l.lock();
    try {
      long loopStart = System.nanoTime();

      // Run modulators and components
      super.loop(deltaMs);

      // Run active pattern
      LXPattern activePattern = getActivePattern();
      activePattern.loop(deltaMs);

      // Run transition if applicable
      if (this.transition != null) {
        int transitionMs = (int) (this.lx.engine.nowMillis - this.transitionMillis);
        if (transitionMs >= this.transition.getDuration()) {
          finishTransition();
        } else {
          getNextPattern().loop(deltaMs);
          this.transition.loop(deltaMs);
          this.transition.blend(
            getActivePattern().getColors(),
            getNextPattern().getColors(),
            transitionMs / this.transition.getDuration()
          );
        }
      } else {
        if (this.autoTransitionEnabled.isOn() &&
            (this.lx.engine.nowMillis - this.transitionMillis > this.autoTransitionThreshold)) {
          goNext();
        }
      }

      int[] colors = (this.transition != null) ? this.transition.getColors() : getActivePattern().getColors();

      if (this.effects.size() > 0) {
        int[] array = this.buffer.getArray();
        for (int i = 0; i < colors.length; ++i) {
          array[i] = colors[i];
        }
        colors = array;
        for (LXEffect effect : this.effects) {
          ((LXLayeredComponent)effect).setBuffer(this.buffer);
          effect.loop(deltaMs);
        }
      }

      this.colors = colors;

      this.timer.loopNanos = System.nanoTime() - loopStart;
    } finally {
      l.unlock();
    }
  }

  public int[] getColors() {
    Lock l = this.acquireReadLock();
    try {
      return this.colors;
    } finally {
      unlock(l);
    }
  }

  public void copyColors(int[] copy) {
    Lock l = this.acquireReadLock();
    try {
      for (int i = 0; i < this.colors.length; ++i) {
        copy[i] = this.colors[i];
      }
    } finally {
      unlock(l);
    }
  }

  private Lock acquireReadLock() {
    Lock l = this.channelModificationLock.readLock();
    try {
      l.lock();
    } catch (IllegalStateException e) {
      // Current thread already holding update lock.
      // Safe to skip getting read lock
      return null;
    }
    return l;
  }

  private void unlock(Lock l) {
    if (l != null) {
      l.unlock();
    }
  }

}
