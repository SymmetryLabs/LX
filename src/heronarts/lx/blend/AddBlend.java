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

package heronarts.lx.blend;

import heronarts.lx.LX;
import heronarts.lx.PolyBuffer;

import static heronarts.lx.PolyBuffer.Space.RGB16;
import static heronarts.lx.PolyBuffer.Space.RGB8;

public class AddBlend extends LXBlend {

  public AddBlend(LX lx) {
    super(lx);
  }

  public void blend(PolyBuffer base, PolyBuffer overlay,
                    double alpha, PolyBuffer dest, PolyBuffer.Space space) {
    if (space == RGB8) {
      blend((int[]) base.getArray(RGB8), (int[]) overlay.getArray(RGB8),
          alpha, (int[]) dest.getArray(RGB8));
      dest.markModified(RGB8);
    } else {
      blend16((long[]) base.getArray(RGB16), (long[]) overlay.getArray(RGB16),
          alpha, (long[]) dest.getArray(RGB16));
      dest.markModified(RGB16);
    }
  }

  @Override
  public void blend(int[] dst, int[] src, double alpha, int[] output) {
    int alphaAdjust = (int) (alpha * 0x100);
    for (int i = 0; i < src.length; ++i) {
      int a = (((src[i] >>> ALPHA_SHIFT) * alphaAdjust) >> 8) & 0xff;

      int srcAlpha = a + (a >= 0x7f ? 1 : 0);

      int rb = (dst[i] & RB_MASK) + ((src[i] & RB_MASK) * srcAlpha >>> 8 & RB_MASK);
      int gn = (dst[i] & G_MASK) + ((src[i] & G_MASK) * srcAlpha >>> 8);

      output[i] = min((dst[i] >>> ALPHA_SHIFT) + a, 0xff) << ALPHA_SHIFT |
          min(rb & 0xffff0000, R_MASK) |
          min(gn & 0x00ffff00, G_MASK) |
          min(rb & 0x0000ffff, B_MASK);
    }
  }

  public void blend16(long[] base, long[] overlay, double alpha, long[] dest) {
    int alphaAdjust = (int) (alpha * 0x10000);
    for (int i = 0; i < overlay.length; ++i) {
      long a = (((overlay[i] >>> ALPHA_SHIFT16) * alphaAdjust) >> 16) & 0xffff;

      long srcAlpha = a + (a >= 0x7fff ? 1 : 0);

      long rb = (base[i] & RB_MASK16) + ((overlay[i] & RB_MASK16) * srcAlpha >>> 16 & RB_MASK16);
      long gn = (base[i] & G_MASK16) + ((overlay[i] & G_MASK16) * srcAlpha >>> 16);

      dest[i] = min((base[i] >>> ALPHA_SHIFT16) + a, 0xffff) << ALPHA_SHIFT16 |
          min(rb & 0x0001ffff00000000L, R_MASK16) |
          min(gn & 0x00000001ffff0000L, G_MASK16) |
          min(rb & 0x000000000001ffffL, B_MASK16);
    }
  }
}
