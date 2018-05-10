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
import heronarts.lx.blend.NormalBlend;
import heronarts.lx.blend.ScreenBlend;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LXColor16;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.PolyBuffer;

public class BlurEffect extends LXEffect {

  public final CompoundParameter amount =
    new CompoundParameter("Amount", 0)
    .setDescription("Sets the amount of blur to apply");

  private final PolyBuffer blurBuffer;

  public BlurEffect(LX lx) {
    super(lx);
    this.blurBuffer = new PolyBuffer(lx);
    resetBuffers();
    addParameter("amount", this.amount);
  }

  private void resetBuffers() {
    int[] intArray = (int[]) blurBuffer.getArray(PolyBuffer.Space.RGB8);
    for (int i = 0; i < intArray.length; ++i) {
      intArray[i] = LXColor.BLACK;
    }

    long[] longArray = (long[]) blurBuffer.getArray(PolyBuffer.Space.RGB16);
    for (int i = 0; i < longArray.length; ++i) {
      longArray[i] = LXColor16.BLACK;
    }
  }

  @Override
  protected void onEnable() {
    resetBuffers();
  }

  @Override
  public void run(double deltaMs, double enabledAmount, PolyBuffer.Space space) {
    float blurf = (float) (enabledAmount * this.amount.getValuef());
    if (blurf > 0) {
      blurf = 1 - (1 - blurf) * (1 - blurf) * (1 - blurf);

      if (space == PolyBuffer.Space.RGB8) {
        int[] intColors = (int[]) getArray(space);
        int[] intBlurArray = (int[]) blurBuffer.getArray(space);

        // Screen blend the colors onto the blur array
        ScreenBlend.screen(intBlurArray, intColors, 1, intBlurArray);
  
        // Lerp onto the colors based upon amount
        NormalBlend.lerp(intColors, intBlurArray, blurf, intColors);
  
        // Copy colors into blur array for next frame
        System.arraycopy(intColors, 0, intBlurArray, 0, intColors.length);
      }
      else if (space == PolyBuffer.Space.RGB16) {
        long[] longColors = (long[]) getArray(space);
        long[] longBlurArray = (long[]) blurBuffer.getArray(space);

        ScreenBlend.screen16(longBlurArray, longColors, 1, longBlurArray);
        NormalBlend.lerp16(longColors, longBlurArray, blurf, longColors);
        System.arraycopy(longColors, 0, longBlurArray, 0, longColors.length);
      }
    }
  }
}
