/**
 * Copyright 2016- Mark C. Slee, Heron Arts LLC
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

package heronarts.lx.audio;

import com.google.gson.JsonObject;

import heronarts.lx.LX;
import heronarts.lx.LXModulatorComponent;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXParameter;

public class LXAudioEngine extends LXModulatorComponent {

  public BooleanParameter enabled = new BooleanParameter("Enabled", false);

  /**
   * Audio input object
   */
  public final LXAudioInput input = new LXAudioInput();

  private BandGate beatDetect = null;

  public final GraphicMeter meter = new GraphicMeter("EQ", this.input);

  public LXAudioEngine(LX lx) {
    super(lx);
    addModulator(this.meter);
    addParameter(this.enabled);
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    if (p == this.enabled) {
      if (this.enabled.isOn()) {
        this.input.open();
        this.input.start();
      } else {
        this.input.stop();
      }
      this.meter.running.setValue(this.enabled.isOn());
    }
  }

  /**
   * Retrieves the audio input object at default sample rate of 44.1kHz
   *
   * @return Audio input object
   */
  public final LXAudioInput getInput() {
    return this.input;
  }

  /**
   * Gets the global beat detection object, creating if necessary
   *
   * @return Beat detection object
   */
  public final BandGate beatDetect() {
    if (this.beatDetect == null) {
      GraphicMeter eq = new GraphicMeter(this.input, 4);
      eq.attack.setValue(10);
      eq.release.setValue(250);
      eq.range.setValue(14);
      eq.gain.setValue(16);
      startModulator(eq);

      this.beatDetect = new BandGate("BEAT", eq).setBands(1, 4);
      this.beatDetect.floor.setValue(0.9);
      this.beatDetect.threshold.setValue(0.75);
      this.beatDetect.decay.setValue(480);
      startModulator(this.beatDetect);
    }
    return this.beatDetect;
  }

  @Override
  public void dispose() {
    this.input.close();
    super.dispose();
  }

  @Override
  public String getLabel() {
    return "Audio";
  }

  private static final String KEY_METER = "meter";

  @Override
  public void save(JsonObject obj) {
    JsonObject meterObj = new JsonObject();
    this.meter.save(meterObj);
    obj.add(KEY_METER, meterObj);
    super.save(obj);
  }

  @Override
  public void load(JsonObject obj) {
    if (obj.has(KEY_METER)) {
      this.meter.load(obj.getAsJsonObject(KEY_METER));
    }
    super.load(obj);
  }

}
