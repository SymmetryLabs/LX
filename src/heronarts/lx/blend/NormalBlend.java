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

public class NormalBlend extends LXBlend {

  public NormalBlend(LX lx) {
    super(lx);
  }

  @Override
  public void blend(PolyBuffer dst, PolyBuffer src,
                    double alpha, PolyBuffer output, PolyBuffer.Space space) {
    switch (space) {
      case RGB8:
        lerp((int[]) dst.getArray(space), (int[]) src.getArray(space),
            alpha, (int[]) output.getArray(space));
        output.markModified(space);
        break;
      case RGB16:
        lerp16((long[]) dst.getArray(space), (long[]) src.getArray(space),
            alpha, (long[]) output.getArray(space));
        output.markModified(space);
        break;
    }
  }

  public void blend(int[] dst, int[] src, double alpha, int[] output) {
    lerp(dst, src, alpha, output);
  }

  public void blend16(long[] dst, long[] src, double alpha, long[] output) {
    lerp16(dst, src, alpha, output);
  }

  public static void lerp(int[] dst, int[] src, double alpha, int[] output) {
    int alphaAdjust = (int) (alpha * 0x100);
    for (int i = 0; i < src.length; ++i) {
      int a = (((src[i] >>> ALPHA_SHIFT) * alphaAdjust) >> 8) & 0xff;

      int srcAlpha = a + (a >= 0x7F ? 1 : 0);
      int dstAlpha = 0x100 - srcAlpha;

      output[i] = min((dst[i] >>> ALPHA_SHIFT) + a, 0xff) << ALPHA_SHIFT |
          ((dst[i] & RB_MASK) * dstAlpha + (src[i] & RB_MASK) * srcAlpha) >>> 8 & RB_MASK |
          ((dst[i] & G_MASK) * dstAlpha + (src[i] & G_MASK) * srcAlpha) >>> 8 & G_MASK;
    }
  }

  public static void lerp16(long[] dst, long[] src, double alpha, long[] output) {
    long alphaAdjust = (int) (alpha * 0x10000);
    for (int i = 0; i < src.length; ++i) {
      long a = (((src[i] >>> ALPHA_SHIFT16) * alphaAdjust) >> 16) & 0xffff;

      long srcAlpha = a + (a >= 0x7fff ? 1 : 0);
      long dstAlpha = 0x10000 - srcAlpha;

      output[i] = min((dst[i] >>> ALPHA_SHIFT16) + a, 0xffff) << ALPHA_SHIFT16 |
          ((dst[i] & RB_MASK16) * dstAlpha + (src[i] & RB_MASK16) * srcAlpha) >>> 16 & RB_MASK16 |
          ((dst[i] & G_MASK16) * dstAlpha + (src[i] & G_MASK16) * srcAlpha) >>> 16 & G_MASK16;
    }
  }
}
