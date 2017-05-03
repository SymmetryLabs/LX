/**
 * Copyright 2013- Mark C. Slee, Heron Arts LLC
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package heronarts.lx;

import heronarts.lx.blend.LXBlend;
import heronarts.lx.clip.LXClip;
import heronarts.lx.midi.LXMidiEngine;
import heronarts.lx.midi.LXShortMessage;
import heronarts.lx.model.LXModel;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.BooleanParameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A channel is a single component of the engine that has a set of patterns from
 * which it plays and rotates. It also has a fader to control how this channel
 * is blended with the channels before it.
 */
public class LXChannel extends LXBus {

  public class Timer extends LXModulatorComponent.Timer {
    public long blendNanos;
  }

  @Override
  protected LXModulatorComponent.Timer constructTimer() {
    return new Timer();
  }

  /**
   * Listener interface for objects which want to be notified when the internal
   * channel state is modified.
   */
  public interface Listener extends LXBus.Listener {
    public void indexChanged(LXChannel channel);
    public void patternAdded(LXChannel channel, LXPattern pattern);
    public void patternRemoved(LXChannel channel, LXPattern pattern);
    public void patternMoved(LXChannel channel, LXPattern pattern);
    public void patternWillChange(LXChannel channel, LXPattern pattern, LXPattern nextPattern);
    public void patternDidChange(LXChannel channel, LXPattern pattern);
  }

  public interface MidiListener {
    public void midiReceived(LXChannel channel);
  }

  public interface ClipListener {
    public void clipAdded(LXChannel channel, LXClip clip);
    public void clipRemoved(LXChannel channel, LXClip clip);
  }

  /**
   * Utility class to extend in cases where only some methods need overriding.
   */
  public abstract static class AbstractListener implements Listener {

    @Override
    public void indexChanged(LXChannel channel) {
    }

    @Override
    public void effectAdded(LXBus channel, LXEffect effect) {
    }

    @Override
    public void effectRemoved(LXBus channel, LXEffect effect) {
    }

    @Override
    public void effectMoved(LXBus channel, LXEffect effect) {
    }

    @Override
    public void patternAdded(LXChannel channel, LXPattern pattern) {
    }

    @Override
    public void patternRemoved(LXChannel channel, LXPattern pattern) {
    }

    @Override
    public void patternMoved(LXChannel channel, LXPattern pattern) {
    }

    @Override
    public void patternWillChange(LXChannel channel, LXPattern pattern,
        LXPattern nextPattern) {
    }

    @Override
    public void patternDidChange(LXChannel channel, LXPattern pattern) {
    }
  }

  private final List<Listener> listeners = new ArrayList<Listener>();
  private final List<ClipListener> clipListeners = new ArrayList<ClipListener>();
  private final List<MidiListener> midiListeners = new ArrayList<MidiListener>();

  public enum CrossfadeGroup {
    BYPASS,
    A,
    B
  };

  /**
   * The index of this channel in the engine.
   */
  private int index;

  /**
   * Which pattern is focused in the channel
   */
  public final DiscreteParameter focusedPattern;

  /**
   * Whether this channel is enabled.
   */
  public final BooleanParameter enabled =
    new BooleanParameter("On", true)
    .setDescription("Sets whether this channel is on or off");

  /**
   * Crossfade group this channel belongs to
   */
  public final EnumParameter<CrossfadeGroup> crossfadeGroup =
    new EnumParameter<CrossfadeGroup>("Group", CrossfadeGroup.BYPASS)
    .setDescription("Assigns this channel to crossfader group A or B");

  /**
   * Arms the channel for clip recording.
   */
  public final BooleanParameter arm =
    new BooleanParameter("Arm")
    .setDescription("Arms the channel for clip recording");

  /**
   * Whether this channel should listen to MIDI events
   */
  public final BooleanParameter midiMonitor =
    new BooleanParameter("MIDI", false)
    .setDescription("Enables or disables monitoring of MIDI input on this channel");

  /**
   * Which channel MIDI messages this channel observes
   */
  public final EnumParameter<LXMidiEngine.Channel> midiChannel =
    new EnumParameter<LXMidiEngine.Channel>("MIDI Channel", LXMidiEngine.Channel.OMNI)
    .setDescription("Determines which MIDI channel is responded to");

  /**
   * Whether this channel should show in the cue UI.
   */
  public final BooleanParameter cueActive =
    new BooleanParameter("Cue", false)
    .setDescription("Toggles the channel CUE state, determining whether it is shown in the preview window");

  /**
   * Whether auto pattern transition is enabled on this channel
   */
  public final BooleanParameter autoCycleEnabled =
    new BooleanParameter("Auto-Cycle", false)
    .setDescription("When enabled, this channel will automatically cycle between its patterns");

  /**
   * Time in milliseconds after which transition thru the pattern set is automatically initiated.
   */
  public final BoundedParameter autoCycleTimeSecs = (BoundedParameter)
    new BoundedParameter("Cycle Time", 60, .1, 60*60*4)
    .setDescription("Sets the number of seconds after which the channel cycles to the next pattern")
    .setUnits(LXParameter.Units.SECONDS);

  public final BoundedParameter transitionTimeSecs = (BoundedParameter)
    new BoundedParameter("Transition Time", 5, .1, 180)
    .setDescription("Sets the duration of blending transitions between patterns")
    .setUnits(LXParameter.Units.SECONDS);

  public final BooleanParameter transitionEnabled =
    new BooleanParameter("Transitions", false)
    .setDescription("When enabled, transitions between patterns use a blend");

  public final DiscreteParameter transitionBlendMode;

  public final CompoundParameter fader =
    new CompoundParameter("Fader", 0)
    .setDescription("Sets the alpha level of the output of this channel");

  public final DiscreteParameter blendMode;

  private final List<LXPattern> internalPatterns = new ArrayList<LXPattern>();

  public final List<LXPattern> patterns = Collections.unmodifiableList(internalPatterns);

  private final List<LXClip> mutableClips = new ArrayList<LXClip>();

  public final List<LXClip> clips = Collections.unmodifiableList(this.mutableClips);

  /**
   * This is a local buffer used for transition blending on this channel
   */
  private final ModelBuffer blendBuffer;

  private int[] colors;

  private double autoCycleProgress = 0;
  private double transitionProgress = 0;
  private int activePatternIndex = 0;
  private int nextPatternIndex = 0;

  private LXBlend transition = null;
  private long transitionMillis = 0;

  LXChannel(LX lx, int index, LXPattern[] patterns) {
    super(lx, "Channel-" + (index+1));
    this.index = index;
    this.label.setDescription("The name of this channel");
    this.blendBuffer = new ModelBuffer(lx);

    this.focusedPattern =
      new DiscreteParameter("Focused Pattern", 0, patterns.length)
      .setDescription("Which pattern has focus in the UI");

    this.blendMode = new DiscreteParameter("Blend", lx.engine.channelBlends)
      .setDescription("Specifies the blending function used for the channel fader");

    this.transitionBlendMode = new DiscreteParameter("Transition Blend", lx.engine.crossfaderBlends)
      .setDescription("Specifies the blending function used for transitions between patterns on the channel");

    this.transitionMillis = lx.engine.nowMillis;
    _updatePatterns(patterns);
    this.colors = this.getActivePattern().getColors();

    addParameter("enabled", this.enabled);
    addParameter("arm", this.arm);
    addParameter("cue", this.cueActive);
    addParameter("midiMonitor", this.midiMonitor);
    addParameter("midiChannel", this.midiChannel);
    addParameter("autoCycleEnabled", this.autoCycleEnabled);
    addParameter("autoCycleTimeSecs", this.autoCycleTimeSecs);
    addParameter("fader", this.fader);
    addParameter("crossfadeGroup", this.crossfadeGroup);
    addParameter("blendMode", this.blendMode);
    addParameter("transitionEnabled", this.transitionEnabled);
    addParameter("transitionTimeSecs", this.transitionTimeSecs);
    addParameter("transitionBlendMode", this.transitionBlendMode);
  }

  public String getOscAddress() {
    return "/lx/channel/" + (this.index+1);
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    if (p == this.autoCycleEnabled) {
      if (this.transition == null) {
        this.transitionMillis = this.lx.engine.nowMillis;
      }
    } else if (p == this.cueActive) {
      if (this.cueActive.isOn()) {
        this.lx.engine.cueA.setValue(false);
        this.lx.engine.cueB.setValue(false);
      }
    }
  }

  @Override
  protected void onModelChanged(LXModel model) {
    super.onModelChanged(model);
    for (LXPattern pattern : this.internalPatterns) {
      pattern.setModel(model);
    }
  }

  public final void addListener(Listener listener) {
    super.addListener(listener);
    this.listeners.add(listener);
  }

  public final void removeListener(Listener listener) {
    super.removeListener(listener);
    this.listeners.remove(listener);
  }

  public LXChannel addClipListener(ClipListener listener) {
    this.clipListeners.add(listener);
    return this;
  }

  public LXChannel removeClipListener(ClipListener listener) {
    this.clipListeners.remove(listener);
    return this;
  }

  public LXChannel addMidiListener(MidiListener listener) {
    this.midiListeners.add(listener);
    return this;
  }

  public LXChannel removeMidiListener(MidiListener listener) {
    this.midiListeners.remove(listener);
    return this;
  }

  public void midiMessage(LXShortMessage message) {
    for (MidiListener listener : this.midiListeners) {
      listener.midiReceived(this);
    }
  }

  final LXChannel setIndex(int index) {
    if (this.index != index) {
      this.index = index;
      for (LXBus.Listener listener : this.listeners) {
        ((LXChannel.Listener)listener).indexChanged(this);
      }
    }
    return this;
  }

  public final int getIndex() {
    return this.index;
  }

  public final List<LXPattern> getPatterns() {
    return this.patterns;
  }

  public final LXPattern getPattern(int index) {
    return this.internalPatterns.get(index);
  }

  public final LXPattern getPattern(String label) {
    for (LXPattern pattern : this.patterns) {
      if (pattern.getLabel().equals(label)) {
        return pattern;
      }
    }
    return null;
  }

  public final LXPattern getPatternByClassName(String className) {
    for (LXPattern pattern : this.patterns) {
      if (pattern.getClass().getName().equals(className)) {
        return pattern;
      }
    }
    return null;
  }

  public final LXChannel setPatterns(LXPattern[] patterns) {
    if (this.transition != null) {
      finishTransition();
    } else {
      getActivePattern().onInactive();
    }
    _updatePatterns(patterns);
    this.activePatternIndex = this.nextPatternIndex = 0;
    this.transition = null;
    getActivePattern().onActive();
    return this;
  }

  public final LXChannel addPattern(LXPattern pattern) {
    pattern.setChannel(this);
    pattern.setModel(this.model);
    pattern.setIndex(this.internalPatterns.size());
    this.internalPatterns.add(pattern);
    this.focusedPattern.setRange(this.internalPatterns.size());
    for (Listener listener : this.listeners) {
      listener.patternAdded(this, pattern);
    }
    if (this.internalPatterns.size() == 1) {
      this.focusedPattern.bang();
    }
    return this;
  }

  public final LXChannel removePattern(LXPattern pattern) {
    return removePattern(pattern, true);
  }

  private final LXChannel removePattern(LXPattern pattern, boolean checkLast) {
    if (checkLast && (this.internalPatterns.size() <= 1)) {
      throw new UnsupportedOperationException("LXChannel must have at least one pattern");
    }
    int index = this.internalPatterns.indexOf(pattern);
    if (index >= 0) {
      boolean wasActive = (this.activePatternIndex == index);
      int focusedPatternIndex = this.focusedPattern.getValuei();
      if ((this.transition != null) && (
          (this.activePatternIndex == index) ||
          (this.nextPatternIndex == index)
         )) {
        finishTransition();
      }
      this.internalPatterns.remove(index);
      for (int i = index; i < this.internalPatterns.size(); ++i) {
        this.internalPatterns.get(i).setIndex(i);
      }
      if (this.activePatternIndex > index) {
        --this.activePatternIndex;
      } else if (this.activePatternIndex >= this.internalPatterns.size()) {
        this.activePatternIndex = this.internalPatterns.size() - 1;
      }
      if (this.nextPatternIndex > index) {
        --this.nextPatternIndex;
      } else if (this.nextPatternIndex >= this.internalPatterns.size()) {
        this.nextPatternIndex = this.internalPatterns.size() - 1;
      }
      if (focusedPatternIndex > index) {
        --focusedPatternIndex;
      } else if (focusedPatternIndex >= this.internalPatterns.size()) {
        focusedPatternIndex = this.internalPatterns.size() - 1;
      }
      if (this.activePatternIndex < 0) {
        this.activePatternIndex = 0;
        this.nextPatternIndex = 0;
      }
      if (focusedPatternIndex >= 0) {
        if (this.focusedPattern.getValuei() != focusedPatternIndex) {
          this.focusedPattern.setValue(focusedPatternIndex);
        } else {
          this.focusedPattern.bang();
        }
      }
      this.focusedPattern.setRange(Math.max(1, this.internalPatterns.size()));
      for (Listener listener : this.listeners) {
        listener.patternRemoved(this, pattern);
      }
      if (wasActive && (this.internalPatterns.size() > 0)) {
        LXPattern newActive = getActivePattern();
        newActive.onActive();
        for (Listener listener : this.listeners) {
          listener.patternDidChange(this, newActive);
        }
      }
      pattern.dispose();
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
    for (LXPattern pattern : this.internalPatterns) {
      pattern.dispose();
    }
    this.internalPatterns.clear();
    for (LXPattern pattern : patterns) {
      if (pattern == null) {
        throw new IllegalArgumentException("Pattern array may not include null elements");
      }
      addPattern(pattern);
    }
  }

  public LXChannel movePattern(LXPattern pattern, int index) {
    LXPattern focusedPattern = getFocusedPattern();
    LXPattern activePattern = getActivePattern();
    LXPattern nextPattern = getNextPattern();
    this.internalPatterns.remove(pattern);
    this.internalPatterns.add(index, pattern);
    int i = 0;
    for (LXPattern p : this.internalPatterns) {
       p.setIndex(i++);
    }
    this.activePatternIndex = activePattern.getIndex();
    this.nextPatternIndex = nextPattern.getIndex();
    for (Listener listener : this.listeners) {
      listener.patternMoved(this, pattern);
    }
    if (pattern == focusedPattern) {
      this.focusedPattern.setValue(pattern.getIndex());
    }
    return this;
  }

  public final int getActivePatternIndex() {
    return this.activePatternIndex;
  }

  public final LXPattern getFocusedPattern() {
    return this.internalPatterns.get(this.focusedPattern.getValuei());
  }

  public final LXPattern getActivePattern() {
    return this.internalPatterns.get(this.activePatternIndex);
  }

  public final int getNextPatternIndex() {
    return this.nextPatternIndex;
  }

  public final LXPattern getNextPattern() {
    return this.internalPatterns.get(this.nextPatternIndex);
  }

  public final LXBus goPrev() {
    if (this.transition != null) {
      return this;
    }
    this.nextPatternIndex = this.activePatternIndex - 1;
    if (this.nextPatternIndex < 0) {
      this.nextPatternIndex = this.internalPatterns.size() - 1;
    }
    startTransition();
    return this;
  }

  public final LXBus goNext() {
    if (this.transition != null) {
      return this;
    }
    this.nextPatternIndex = this.activePatternIndex;
    do {
      this.nextPatternIndex = (this.nextPatternIndex + 1)
          % this.internalPatterns.size();
    } while ((this.nextPatternIndex != this.activePatternIndex)
        && !getNextPattern().isAutoCycleEligible());
    if (this.nextPatternIndex != this.activePatternIndex) {
      startTransition();
    }
    return this;
  }

  public final LXBus goPattern(LXPattern pattern) {
    int pi = 0;
    for (LXPattern p : this.internalPatterns) {
      if (p == pattern) {
        return goIndex(pi);
      }
      ++pi;
    }
    return this;
  }

  public final LXBus goIndex(int i) {
    if (i < 0 || i >= this.internalPatterns.size()) {
      return this;
    }
    if (this.transition != null) {
      finishTransition();
    }
    this.nextPatternIndex = i;
    startTransition();
    return this;
  }

  public LXClip getClip(int index) {
    if (index < this.clips.size()) {
      return this.clips.get(index);
    }
    return null;
  }

  public LXClip addClip() {
    return addClip(this.mutableClips.size());
  }

  public LXClip addClip(int index) {
    while (this.mutableClips.size() <= index) {
      this.mutableClips.add(null);
    }
    LXClip clip = new LXClip(this.lx, this, index);
    clip.label.setValue("Clip-" + (index+1));
    this.mutableClips.set(index, clip);
    for (ClipListener listener : this.clipListeners) {
      listener.clipAdded(this, clip);
    }
    return clip;
  }

  public LXClip removeClip(LXClip clip) {
    int index = this.mutableClips.indexOf(clip);
    if (index < 0) {
      throw new IllegalArgumentException("Clip is not owned by channel: " + clip + " " + this);
    }
    return removeClip(index);
  }

  public LXClip removeClip(int index) {
    LXClip clip = this.mutableClips.get(index);
    this.mutableClips.set(index, null);
    for (ClipListener listener : this.clipListeners) {
      listener.clipRemoved(this, clip);
    }
    return clip;
  }

  public LXBus disableAutoTransition() {
    this.autoCycleEnabled.setValue(false);
    return this;
  }

  /**
   * Enable automatic transition from pattern to pattern on this channel
   *
   * @param autoTransitionThresholdTransition time in seconds
   * @return
   */
  public LXBus enableAutoTransition(double autoTransitionThreshold) {
    this.autoCycleTimeSecs.setValue(autoTransitionThreshold);
    this.autoCycleEnabled.setValue(true);
    return this;
  }

  /**
   * Return progress towards making a cycle
   *
   * @return amount of progress towards the next cycle
   */
  public double getAutoCycleProgress() {
    return this.autoCycleProgress;
  }

  /**
   * Return progress through a transition
   *
   * @return amount of progress thru current transition
   */
  public double getTransitionProgress() {
    return this.transitionProgress;
  }

  private void startTransition() {
    LXPattern activePattern = getActivePattern();
    LXPattern nextPattern = getNextPattern();
    if (activePattern == nextPattern) {
      return;
    }
    nextPattern.onActive();
    for (Listener listener : this.listeners) {
      listener.patternWillChange(this, activePattern, nextPattern);
    }
    if (this.transitionEnabled.isOn()) {
      this.transition = lx.engine.crossfaderBlends[this.transitionBlendMode.getValuei()];
      nextPattern.onTransitionStart();
      this.transitionMillis = this.lx.engine.nowMillis;
    } else {
      finishTransition();
    }
  }

  private void finishTransition() {
    getActivePattern().onInactive();
    this.activePatternIndex = this.nextPatternIndex;
    LXPattern activePattern = getActivePattern();
    if (this.transition != null) {
      activePattern.onTransitionEnd();
    }
    this.transition = null;
    this.transitionMillis = this.lx.engine.nowMillis;
    for (Listener listener : listeners) {
      listener.patternDidChange(this, activePattern);
    }
  }

  @Override
  public void loop(double deltaMs) {
    long loopStart = System.nanoTime();

    // Run modulators and components
    super.loop(deltaMs);

    // Run the active clip...
    // TODO(mcslee): don't loop, keep tabs of which is active now
    for (LXClip clip : this.mutableClips) {
      if (clip != null) {
        clip.loop(deltaMs);
      }
    }

    // Check for transition completion
    if (this.transition != null) {
      double transitionMs = this.lx.engine.nowMillis - this.transitionMillis;
      double transitionDone = 1000 * this.transitionTimeSecs.getValue();
      if (transitionMs >= transitionDone) {
        finishTransition();
      }
    }

    // Auto-cycle if appropriate
    if (this.transition == null) {
      this.autoCycleProgress = (this.lx.engine.nowMillis - this.transitionMillis) / (1000 * this.autoCycleTimeSecs.getValue());
      if (this.autoCycleProgress >= 1) {
        this.autoCycleProgress = 1;
        if (this.autoCycleEnabled.isOn()) {
          goNext();
        }
      }
    }

    // Run active pattern
    LXPattern activePattern = getActivePattern();
    activePattern.loop(deltaMs);
    int[] colors = activePattern.getColors();

    // Run transition!
    if (this.transition != null) {
      this.autoCycleProgress = 1.;
      this.transitionProgress = (this.lx.engine.nowMillis - this.transitionMillis) / (1000 * this.transitionTimeSecs.getValue());
      getNextPattern().loop(deltaMs);;
      // TODO(mcslee): this is incorrect. the blend objects are shared, so the same one may be run on multiple
      // channels. either they need to be per-channel instances, or they are not loopable with modulators etc.
      this.transition.loop(deltaMs);
      colors = this.blendBuffer.getArray();
      if (this.transitionProgress < .5) {
        double alpha = Math.min(1, this.transitionProgress*2.);
        this.transition.blend(
          getActivePattern().getColors(),
          getNextPattern().getColors(),
          alpha,
          colors
        );
      } else {
        double alpha = Math.max(0, (1-this.transitionProgress)*2.);
        this.transition.blend(
          getNextPattern().getColors(),
          getActivePattern().getColors(),
          alpha,
          colors
        );
      }
    } else {
      this.transitionProgress = 0;
    }

    // Apply effects
    if (this.internalEffects.size() > 0) {
      int[] array = this.blendBuffer.getArray();
      if (colors != array) {
        System.arraycopy(colors, 0, array, 0, colors.length);
      }
      colors = array;
      for (LXEffect effect : this.internalEffects) {
        effect.setBuffer(this.blendBuffer);
        effect.loop(deltaMs);
      }
    }

    this.colors = colors;
    this.timer.loopNanos = System.nanoTime() - loopStart;
  }

  int[] getColors() {
    return this.colors;
  }

  @Override
  public void dispose() {
    for (LXPattern pattern : this.internalPatterns) {
      pattern.dispose();
    }
    this.internalPatterns.clear();
    super.dispose();
  }

  private static final String KEY_PATTERNS = "patterns";
  private static final String KEY_PATTERN_INDEX = "patternIndex";

  @Override
  public void save(LX lx, JsonObject obj) {
    super.save(lx, obj);
    obj.addProperty(KEY_PATTERN_INDEX, this.activePatternIndex);
    obj.add(KEY_PATTERNS, LXSerializable.Utils.toArray(lx, this.internalPatterns));
  }

  @Override
  public void load(LX lx, JsonObject obj) {
    super.load(lx, obj);
    // Remove patterns
    for (int i = this.internalPatterns.size() - 1; i >= 0; --i) {
      removePattern(this.internalPatterns.get(i), false);
    }

    // Add patterns
    JsonArray patternsArray = obj.getAsJsonArray(KEY_PATTERNS);
    for (JsonElement patternElement : patternsArray) {
      JsonObject patternObj = (JsonObject) patternElement;
      LXPattern pattern = this.lx.instantiatePattern(patternObj.get(KEY_CLASS).getAsString());
      pattern.load(lx, patternObj);
      addPattern(pattern);
    }

    // Set the active index instantly, do not transition!
    if (obj.has(KEY_PATTERN_INDEX)) {
      int patternIndex = obj.get(KEY_PATTERN_INDEX).getAsInt();
      if (this.activePatternIndex != patternIndex) {
        getActivePattern().onInactive();
        this.activePatternIndex = this.nextPatternIndex = patternIndex;
        LXPattern activePattern = getActivePattern();
        activePattern.onActive();
        for (Listener listener : listeners) {
          listener.patternDidChange(this, activePattern);
        }
      }
    }
  }

}
