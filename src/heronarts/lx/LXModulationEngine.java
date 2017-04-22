/**
 * Copyright 2017- Mark C. Slee, Heron Arts LLC
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
 * ##library.name##
 * ##library.sentence##
 * ##library.url##
 *
 * @author      ##author##
 * @modified    ##date##
 * @version     ##library.prettyVersion## (##library.version##)
 */

package heronarts.lx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import heronarts.lx.modulator.LXModulator;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.LXCompoundModulation;
import heronarts.lx.parameter.LXTriggerModulation;

public class LXModulationEngine extends LXModulatorComponent implements LXOscComponent {

  private final LX lx;

  public interface Listener {
    public void modulatorAdded(LXModulationEngine engine, LXModulator modulator);
    public void modulatorRemoved(LXModulationEngine engine, LXModulator modulator);

    public void modulationAdded(LXModulationEngine engine, LXCompoundModulation modulation);
    public void modulationRemoved(LXModulationEngine engine, LXCompoundModulation modulation);

    public void triggerAdded(LXModulationEngine engine, LXTriggerModulation modulation);
    public void triggerRemoved(LXModulationEngine engine, LXTriggerModulation modulation);
  }

  private final List<Listener> listeners = new ArrayList<Listener>();

  private final List<LXCompoundModulation> internalModulations = new ArrayList<LXCompoundModulation>();
  public final List<LXCompoundModulation> modulations = Collections.unmodifiableList(this.internalModulations);

  private final List<LXTriggerModulation> internalTriggers = new ArrayList<LXTriggerModulation>();
  public final List<LXTriggerModulation> triggers = Collections.unmodifiableList(this.internalTriggers);

  LXModulationEngine(LX lx) {
    super(lx);
    this.lx = lx;
  }

  public String getOscAddress() {
    return "/lx/modulation";
  }

  public LXModulationEngine addListener(Listener listener) {
    this.listeners.add(listener);
    return this;
  }

  public LXModulationEngine removeListener(Listener listener) {
    this.listeners.remove(listener);
    return this;
  }

  public LXModulationEngine addModulation(LXCompoundModulation modulation) {
    if (this.internalModulations.contains(modulation)) {
      throw new IllegalStateException("Cannot add same modulation twice");
    }
    ((LXComponent) modulation).setParent(this);
    this.internalModulations.add(modulation);
    for (Listener listener : this.listeners) {
      listener.modulationAdded(this, modulation);
    }
    return this;
  }

  public LXModulationEngine removeModulation(LXCompoundModulation modulation) {
    this.internalModulations.remove(modulation);
    for (Listener listener : this.listeners) {
      listener.modulationRemoved(this, modulation);
    }
    modulation.dispose();
    return this;
  }

  public LXModulationEngine addTrigger(LXTriggerModulation trigger) {
    if (this.internalTriggers.contains(trigger)) {
      throw new IllegalStateException("Cannot add same trigger twice");
    }
    ((LXComponent) trigger).setParent(this);
    this.internalTriggers.add(trigger);
    for (Listener listener : this.listeners) {
      listener.triggerAdded(this, trigger);
    }
    return this;
  }

  public LXModulationEngine removeTrigger(LXTriggerModulation trigger) {
    this.internalTriggers.remove(trigger);
    for (Listener listener : this.listeners) {
      listener.triggerRemoved(this, trigger);
    }
    trigger.dispose();
    return this;
  }

  public LXModulationEngine removeModulations(LXComponent component) {
    Iterator<LXCompoundModulation> iterator = this.internalModulations.iterator();
    while (iterator.hasNext()) {
      LXCompoundModulation modulation = iterator.next();
      if (modulation.source == component || modulation.source.getComponent() == component || modulation.target.getComponent() == component) {
        iterator.remove();
        for (Listener listener : this.listeners) {
          listener.modulationRemoved(this, modulation);
        }
        modulation.dispose();
      }
    }
    Iterator<LXTriggerModulation> triggerIterator = this.internalTriggers.iterator();
    while (triggerIterator.hasNext()) {
      LXTriggerModulation trigger = triggerIterator.next();
      if (trigger.source.getComponent() == component || trigger.target.getComponent() == component) {
        triggerIterator.remove();
        for (Listener listener : this.listeners) {
          listener.triggerRemoved(this, trigger);
        }
        trigger.dispose();
      }
    }
    return this;
  }

  @Override
  public LXModulator addModulator(LXModulator modulator) {
    super.addModulator(modulator);
    for (Listener listener : this.listeners) {
      listener.modulatorAdded(this, modulator);
    }
    return modulator;
  }

  @Override
  public LXModulator removeModulator(LXModulator modulator) {
    super.removeModulator(modulator);
    for (Listener listener : this.listeners) {
      listener.modulatorRemoved(this, modulator);
    }
    return modulator;
  }

  @Override
  public void dispose() {
    for (LXCompoundModulation modulation : this.internalModulations) {
      modulation.dispose();
    }
    this.internalModulations.clear();
    super.dispose();
  }

  @Override
  public String getLabel() {
    return "Mod";
  }

  private static final String KEY_MODULATORS = "modulators";
  private static final String KEY_MODULATIONS = "modulations";
  private static final String KEY_TRIGGERS = "triggers";

  @Override
  public void save(LX lx, JsonObject obj) {
    obj.add(KEY_MODULATORS, LXSerializable.Utils.toArray(lx, this.modulators));
    obj.add(KEY_MODULATIONS, LXSerializable.Utils.toArray(lx, this.modulations));
    obj.add(KEY_TRIGGERS, LXSerializable.Utils.toArray(lx, this.triggers));
  }

  @Override
  public void load(LX lx, JsonObject obj) {
    // Remove everything first
    for (int i = this.modulators.size() - 1; i >= 0; --i) {
      removeModulator(this.modulators.get(i));
    }
    for (int i = this.modulations.size() - 1; i >= 0; --i) {
      removeModulation(this.modulations.get(i));
    }
    for (int i = this.triggers.size() - 1; i >= 0; --i) {
      removeTrigger(this.triggers.get(i));
    }

    if (obj.has(KEY_MODULATORS)) {
      JsonArray modulatorArr = obj.getAsJsonArray(KEY_MODULATORS);
      for (JsonElement modulatorElement : modulatorArr) {
        JsonObject modulatorObj = modulatorElement.getAsJsonObject();
        String modulatorClass = modulatorObj.get(KEY_CLASS).getAsString();
        LXModulator modulator = this.lx.instantiateModulator(modulatorClass);
        if (modulator == null) {
          System.err.println("Could not instantiate modulator: " + modulatorClass);
        } else {
          addModulator(modulator);
          modulator.load(lx, modulatorObj);
        }
      }
    }
    if (obj.has(KEY_MODULATIONS)) {
      JsonArray modulationArr = obj.getAsJsonArray(KEY_MODULATIONS);
      for (JsonElement modulationElement : modulationArr) {
        JsonObject modulationObj = modulationElement.getAsJsonObject();
        LXCompoundModulation modulation = new LXCompoundModulation(this.lx, modulationObj);
        addModulation(modulation);
        modulation.load(lx, modulationObj);
      }
    }
    if (obj.has(KEY_TRIGGERS)) {
      JsonArray triggerArr = obj.getAsJsonArray(KEY_TRIGGERS);
      for (JsonElement triggerElement : triggerArr) {
        JsonObject triggerObj = triggerElement.getAsJsonObject();
        LXTriggerModulation trigger = new LXTriggerModulation(this.lx, triggerObj);
        addTrigger(trigger);
        trigger.load(lx, triggerObj);
      }
    }
  }

}
