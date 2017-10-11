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
 * @author Mark C. Slee <mark@heronarts.com>
 */

package heronarts.lx.parameter;

import com.google.gson.JsonObject;

import heronarts.lx.LX;

public class LXTriggerModulation extends LXParameterModulation {

  public final BooleanParameter source;
  public final BooleanParameter target;

  private final boolean sourceMomentary;
  private final boolean targetMomentary;

  public LXTriggerModulation(BooleanParameter source, BooleanParameter target) {
    super(source, target);
    this.source = source;
    this.target = target;
    this.sourceMomentary = (source.getMode() == BooleanParameter.Mode.MOMENTARY);
    this.targetMomentary = (target.getMode() == BooleanParameter.Mode.MOMENTARY);
    this.source.addListener(this);
  }

  public LXTriggerModulation(LX lx, JsonObject obj) {
    this(
      (BooleanParameter) getParameter(lx, obj.getAsJsonObject(KEY_SOURCE)),
      (BooleanParameter) getParameter(lx, obj.getAsJsonObject(KEY_TARGET))
    );
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    super.onParameterChanged(p);
    if (this.enabled.isOn()) {
      if (p == this.source) {
        if (this.sourceMomentary) {
          if (this.source.isOn()) {
            if (this.targetMomentary) {
              this.target.setValue(true);
            } else {
              this.target.toggle();
            }
          }
        } else {
          this.target.setValue(this.source.getValue());
        }
      }
    }
  }

  @Override
  public void dispose() {
    this.source.removeListener(this);
    super.dispose();
  }
}
