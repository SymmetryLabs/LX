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

package heronarts.lx.effect;

import heronarts.lx.LX;
import heronarts.lx.LXEffect;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;

public class DesaturationEffect extends LXEffect {

  private final CompoundParameter attack =
    new CompoundParameter("Attack", 100, 0, 1000)
    .setDescription("Sets the attack time of the desaturation");

  private final CompoundParameter decay =
    new CompoundParameter("Decay", 100, 0, 1000)
    .setDescription("Sets the decay time of the desaturation");

  private final CompoundParameter amount =
    new CompoundParameter("Amount", 1.)
    .setDescription("Sets the amount of desaturation to apply");


  public DesaturationEffect(LX lx) {
    super(lx);
    addParameter("amount", this.amount);
    addParameter("attack", this.attack);
    addParameter("decay", this.decay);
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    if (p == this.attack) {
      this.enabledDampingAttack.setValue(p.getValue());
    } else if (p == this.decay) {
      this.enabledDampingRelease.setValue(p.getValue());
    }
  }

  @Override
  protected void run(double deltaMs, double amount) {
    double d = amount * this.amount.getValue();
    if (d > 0) {
      d = 1-d;
      for (int i = 0; i < colors.length; ++i) {
        this.colors[i] = LXColor.hsb(
          LXColor.h(this.colors[i]),
          Math.max(0, LXColor.s(colors[i]) * d),
          LXColor.b(colors[i])
        );
      }
    }
  }

}
