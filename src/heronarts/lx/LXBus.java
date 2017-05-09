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

import heronarts.lx.clip.LXClip;
import heronarts.lx.model.LXModel;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.BooleanParameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Abstract representation of a channel, which could be a normal channel with patterns
 * or the master channel.
 */
public abstract class LXBus extends LXModelComponent implements LXOscComponent {

  /**
   * Listener interface for objects which want to be notified when the internal
   * channel state is modified.
   */
  public interface Listener {
    public void effectAdded(LXBus channel, LXEffect effect);
    public void effectRemoved(LXBus channel, LXEffect effect);
    public void effectMoved(LXBus channel, LXEffect effect);
  }

  public interface ClipListener {
    public void clipAdded(LXBus bus, LXClip clip);
    public void clipRemoved(LXBus bus, LXClip clip);
  }


  /**
   * Arms the channel for clip recording.
   */
  public final BooleanParameter arm =
    new BooleanParameter("Arm")
    .setDescription("Arms the channel for clip recording");

  protected final LX lx;

  protected final List<LXEffect> mutableEffects = new ArrayList<LXEffect>();
  public final List<LXEffect> effects = Collections.unmodifiableList(mutableEffects);

  private final List<LXClip> mutableClips = new ArrayList<LXClip>();
  public final List<LXClip> clips = Collections.unmodifiableList(this.mutableClips);

  private final List<Listener> listeners = new ArrayList<Listener>();
  private final List<ClipListener> clipListeners = new ArrayList<ClipListener>();

  LXBus(LX lx) {
    this(lx, null);
  }

  LXBus(LX lx, String label) {
    super(lx, label);
    this.lx = lx;
    addParameter("arm", this.arm);
  }

  @Override
  protected void onModelChanged(LXModel model) {
    for (LXEffect effect : this.mutableEffects) {
      effect.setModel(model);
    }
  }

  public final void addListener(Listener listener) {
    this.listeners.add(listener);
  }

  public final void removeListener(Listener listener) {
    this.listeners.remove(listener);
  }

  public LXBus addClipListener(ClipListener listener) {
    this.clipListeners.add(listener);
    return this;
  }

  public LXBus removeClipListener(ClipListener listener) {
    this.clipListeners.remove(listener);
    return this;
  }

  public final LXBus addEffect(LXEffect effect) {
    this.mutableEffects.add(effect);
    effect.setBus(this);
    effect.setIndex(this.mutableEffects.size() - 1);
    for (Listener listener : this.listeners) {
      listener.effectAdded(this, effect);
    }
    return this;
  }

  public final LXBus removeEffect(LXEffect effect) {
    int index = this.mutableEffects.indexOf(effect);
    if (index >= 0) {
      effect.setIndex(-1);
      this.mutableEffects.remove(index);
      while (index < this.mutableEffects.size()) {
        this.mutableEffects.get(index).setIndex(index);
        ++index;
      }
      for (Listener listener : this.listeners) {
        listener.effectRemoved(this, effect);
      }
      effect.dispose();
    }
    return this;
  }

  public LXBus moveEffect(LXEffect effect, int index) {
    this.mutableEffects.remove(effect);
    this.mutableEffects.add(index, effect);
    int i = 0;
    for (LXEffect e : this.mutableEffects) {
       e.setIndex(i++);
    }
    for (Listener listener : this.listeners) {
      listener.effectMoved(this, effect);
    }
    return this;
  }

  public final List<LXEffect> getEffects() {
    return this.effects;
  }

  public LXEffect getEffect(int i) {
    return this.effects.get(i);
  }

  public LXEffect getEffect(String label) {
    for (LXEffect effect : this.effects) {
      if (effect.getLabel().equals(label)) {
        return effect;
      }
    }
    return null;
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
    LXClip clip = constructClip(index);
    clip.label.setValue("Clip-" + (index+1));
    this.mutableClips.set(index, clip);
    for (ClipListener listener : this.clipListeners) {
      listener.clipAdded(this, clip);
    }
    return clip;
  }

  protected abstract LXClip constructClip(int index);

  public void removeClip(LXClip clip) {
    int index = this.mutableClips.indexOf(clip);
    if (index < 0) {
      throw new IllegalArgumentException("Clip is not owned by channel: " + clip + " " + this);
    }
    removeClip(index);
  }

  public void removeClip(int index) {
    LXClip clip = this.mutableClips.get(index);
    this.mutableClips.set(index, null);
    for (ClipListener listener : this.clipListeners) {
      listener.clipRemoved(this, clip);
    }
    clip.dispose();
  }

  @Override
  public void loop(double deltaMs) {
    long loopStart = System.nanoTime();

    // Run modulators and components
    super.loop(deltaMs);

    // Run the active clip...
    // TODO(mcslee): keep tabs of which is active?
    for (LXClip clip : this.clips) {
      if (clip != null) {
        clip.loop(deltaMs);
      }
    }

    this.timer.loopNanos = System.nanoTime() - loopStart;
  }

  @Override
  public void dispose() {
    for (LXEffect effect : this.mutableEffects) {
      effect.dispose();
    }
    this.mutableEffects.clear();
    super.dispose();
  }

  private static final String KEY_EFFECTS = "effects";
  private static final String KEY_CLIPS = "clips";

  @Override
  public void save(LX lx, JsonObject obj) {
    super.save(lx, obj);;
    obj.add(KEY_EFFECTS, LXSerializable.Utils.toArray(lx, this.mutableEffects));
    JsonArray clipsArr = new JsonArray();
    for (LXClip clip : this.clips) {
      if (clip != null) {
        clipsArr.add(LXSerializable.Utils.toObject(lx, clip));
      }
    }
    obj.add(KEY_CLIPS, clipsArr);
  }

  @Override
  public void load(LX lx, JsonObject obj) {
    // Remove clips
    for (LXClip clip : this.clips) {
      if (clip != null) {
        removeClip(clip);
      }
    }
    // Remove effects
    for (int i = this.mutableEffects.size() - 1; i >= 0; --i) {
      removeEffect(this.mutableEffects.get(i));
    }
    // Add the effects
    JsonArray effectsArray = obj.getAsJsonArray(KEY_EFFECTS);
    for (JsonElement effectElement : effectsArray) {
      JsonObject effectObj = (JsonObject) effectElement;
      LXEffect effect = this.lx.instantiateEffect(effectObj.get("class").getAsString());
      effect.load(lx, effectObj);
      addEffect(effect);
    }
    // Add the new clips
    if (obj.has(KEY_CLIPS)) {
      JsonArray clipsArr = obj.get(KEY_CLIPS).getAsJsonArray();
      for (JsonElement clipElem : clipsArr) {
        JsonObject clipObj = clipElem.getAsJsonObject();
        int clipIndex = clipObj.get(LXClip.KEY_INDEX).getAsInt();
        LXClip clip = addClip(clipIndex);
        clip.load(lx, clipObj);
      }
    }

    super.load(lx, obj);
  }

}
