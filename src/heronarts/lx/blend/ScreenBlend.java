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

public class ScreenBlend extends LXBlend {

  public ScreenBlend(LX lx) {
    super(lx);
  }

  public void blend(PolyBuffer dst, PolyBuffer src,
                    double alpha, PolyBuffer output, PolyBuffer.Space space) {
    switch (space) {
      case RGB8:
        screen((int[]) dst.getArray(space), (int[]) src.getArray(space),
            alpha, (int[]) output.getArray(space));
        output.markModified(space);
        break;
      case RGB16:
        screen16((long[]) dst.getArray(space), (long[]) src.getArray(space),
            alpha, (long[]) output.getArray(space));
        output.markModified(space);
        break;
    }
  }

  @Override
  public void blend(int[] dst, int[] src, double alpha, int[] output) {
    screen(dst, src, alpha, output);
  }

  public void blend16(long[] dst, long[] src, double alpha, long[] output) {
    screen16(dst, src, alpha, output);
  }

  public static void screen(int[] dst, int[] src, double alpha, int[] output) {
    int alphaAdjust = (int) (alpha * 0x100);
    for (int i = 0; i < src.length; ++i) {
      int a = (((src[i] >>> ALPHA_SHIFT) * alphaAdjust) >> 8) & 0xff;

      int srcAlpha = a + (a >= 0x7f ? 1 : 0);
      int dstAlpha = 0x100 - srcAlpha;

      int dstRb = dst[i] & RB_MASK;
      int dstGn = dst[i] & G_MASK;
      int srcGn = src[i] & G_MASK;
      int dstR = (dst[i] & R_MASK) >> R_SHIFT;
      int dstB = dst[i] & B_MASK;

      int rbSub = (
          (src[i] & R_MASK) * (dstR + 1) |
          (src[i] & B_MASK) * (dstB + 1)
        ) >>> 8 & RB_MASK;
      int gnSub = srcGn * (dstGn + 0x100) >> 16 & G_MASK;

      output[i] = min((dst[i] >>> ALPHA_SHIFT) + a, 0xff) << ALPHA_SHIFT |
        (dstRb * dstAlpha + (dstRb + (src[i] & RB_MASK) - rbSub) * srcAlpha) >>> 8 & RB_MASK |
        (dstGn * dstAlpha + (dstGn + srcGn - gnSub) * srcAlpha) >>> 8 & G_MASK;
    }
  }

  public static void screen16(long[] dst, long[] src, double alpha, long[] output) {
    int alphaAdjust = (int) (alpha * 0x10000);
    for (int i = 0; i < src.length; ++i) {
      long a = (((src[i] >>> ALPHA_SHIFT16) * alphaAdjust) >> 16) & 0xffff;

      long srcAlpha = a + (a >= 0x7fff ? 1 : 0);
      long dstAlpha = 0x10000 - srcAlpha;

      long dstRb = dst[i] & RB_MASK16;
      long dstGn = dst[i] & G_MASK16;
      long srcGn = src[i] & G_MASK16;
      long dstR = (dst[i] & R_MASK16) >> (R_SHIFT*2);
      long dstB = dst[i] & B_MASK16;

      long rbSub = (
          (src[i] & R_MASK16) * (dstR + 1) |
          (src[i] & B_MASK16) * (dstB + 1)
        ) >>> 16 & RB_MASK16;
      long gnSub = srcGn * (dstGn + 0x10000) >> 32 & G_MASK16;

      output[i] = min((dst[i] >>> ALPHA_SHIFT16) + a, 0xffff) << ALPHA_SHIFT16 |
        (dstRb * dstAlpha + (dstRb + (src[i] & RB_MASK16) - rbSub) * srcAlpha) >>> 16 & RB_MASK16 |
        (dstGn * dstAlpha + (dstGn + srcGn - gnSub) * srcAlpha) >>> 16 & G_MASK16;
    }
  }
}
