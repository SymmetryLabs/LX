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

/**
 * Helper class of useful utilities, many just mirror Processing built-ins but
 * reduce the awkwardness of calling through applet in the library code.
 */
public class LXUtils {

  /**
   * Only used statically, need not be instantiated.
   */
  private LXUtils() {
  }

  public static double constrain(double value, double min, double max) {
    return value < min ? min : (value > max ? max : value);
  }

  public static float constrainf(float value, float min, float max) {
    return value < min ? min : (value > max ? max : value);
  }

  public static int constrain(int value, int min, int max) {
    return value < min ? min : (value > max ? max : value);
  }

  public static double random(double min, double max) {
    return min + Math.random() * (max - min);
  }

  public static double distance(double x1, double y1, double x2, double y2) {
    return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
  }

  public static double lerp(double v1, double v2, double amt) {
    return v1 + (v2 - v1) * amt;
  }

  public static float lerpf(float v1, float v2, float amt) {
    return v1 + (v2 - v1) * amt;
  }

  public static double tri(double t) {
    t = t - Math.floor(t);
    if (t < 0.25) {
      return t * 4;
    } else if (t < 0.75) {
      return 1 - 4 * (t - 0.25);
    } else {
      return -1 + 4 * (t - 0.75);
    }
  }

  public static float trif(float t) {
    return (float) LXUtils.tri(t);
  }

  public static double avg(double v1, double v2) {
    return (v1 + v2) / 2.;
  }

  public static float avgf(float v1, float v2) {
    return (v1 + v2) / 2.f;
  }

  /**
   * Computes the distance between v1 and v2 with a wrap-around at the modulus.
   * Both v1 and v2 must be in the range [0, modulus]. For example, if v1=1, v2=11,
   * and modulus = 12, then the distance is 2, not 10.
   *
   * @param v1 First value
   * @param v2 Second value
   * @param modulus Modulus to wrap around
   * @return
   */
  public static double wrapdist(double v1, double v2, double modulus) {
    if (v1 < v2) {
      return Math.min(v2 - v1, v1 + modulus - v2);
    } else {
      return Math.min(v1 - v2, v2 + modulus - v1);
    }
  }

  public static float wrapdistf(float v1, float v2, float modulus) {
    if (v1 < v2) {
      return Math.min(v2 - v1, v1 + modulus - v2);
    } else {
      return Math.min(v1 - v2, v2 + modulus - v1);
    }
  }

  // Using a cutoff of 0.0031308 as recommended by https://en.wikipedia.org/wiki/SRGB
  // creates a discontinuity at the cutoff point of up to 3e-08.  Using 0.003130668442501
  // ensures that the discontinuity is less than 1e-16.
  protected static double SRGB_TRANSFER_CUTOFF = 0.003130668442501;

  /**
   * Converts an sRGB colour channel value (v, ranging from 0 to 1)
   * to a linear intensity (also ranging from 0 to 1).
   *
   * Tuned to guarantee that:
   *     f(0.0) gives 0.0 exactly
   *     f(1.0) gives 1.0 exactly
   *     0.0 <= f(v) <= 1.0 for all double-precision values 0.0 <= v <= 1.0
   *     f(v) > f(w) for all double-precision values of v > w
   */
  public static double srgb_value_to_intensity(double v) {
    // See https://en.wikipedia.org/wiki/SRGB#The_reverse_transformation
    return v <= (12.92 * SRGB_TRANSFER_CUTOFF) ? v / 12.92 :
        Math.pow((v + 0.055) / 1.055, 2.4);
  }

  /**
   * Converts an sRGB colour channel value (v, ranging from 0 to 1)
   * to a linear intensity (also ranging from 0 to 1).
   *
   * Tuned to guarantee that:
   *     f(0.0) gives 0.0 exactly
   *     f(1.0) gives 1.0 exactly
   *     0.0 <= f(i) <= 1.0 for all double-precision values 0.0 <= i <= 1.0
   *     f(i) > f(j) - 1e16 for all double-precision values of i > j
   */
  public static double srgb_intensity_to_value(double i) {
    // See https://en.wikipedia.org/wiki/SRGB#The_forward_transformation_(CIE_XYZ_to_sRGB)
    // Due to floating-point error, 1.055 - 0.055 does not yield 1.0;
    // subtracting 0.05499999999999999 instead of 0.055 ensures that
    // srgb_intensity_to_value(1.0) returns exactly 1.0.
    return i <= SRGB_TRANSFER_CUTOFF ? 12.92 * i :
        1.055 * Math.pow(i, 1.0/2.4) - 0.05499999999999999;
  }

  /**
   * Converts a CIELAB perceived lightness value (L, ranging from 0 to 1)
   * to a CIEXYZ linear luminance value (Y, also ranging from 0 to 1).
   */
  public static double cie_lightness_to_luminance(double l) {
    // See https://en.wikipedia.org/wiki/CIELAB_color_space#Reverse_transformation
    // The values of t and delta have been scaled up by 29 to avoid
    // floating-point error.  This formulation is designed to yield
    // exactly 0.0 and 1.0 for inputs of 0.0 and 1.0, and to make it
    // easy to see that both parts have the same value and same first
    // derivative at the crossover point where t = 6 (l = 0.08).
    double t = l * 25 + 4;  // t ranges from 4 to 29
    return (t > 6 ? t * t * t : 3*(t - 4) * 6 * 6) / (29 * 29 * 29);
  }

  public static class LookupTable {

    public interface Function {
      static Function SIN = new Function() {
        public float compute(int i, int tableSize) {
          return (float) Math.sin(i * LX.TWO_PI / tableSize);
        }
      };

      static Function COS = new Function() {
        public float compute(int i, int tableSize) {
          return (float) Math.cos(i * LX.TWO_PI / tableSize);
        }
      };

      static Function TAN = new Function() {
        public float compute(int i, int tableSize) {
          return (float) Math.tan(i * LX.TWO_PI / tableSize);
        }
      };

      public float compute(int i, int tableSize);
    }

    public static class Sin extends LookupTable {
      public Sin(int tableSize) {
        super(tableSize, Function.SIN);
      }

      public float sin(float radians) {
        int index = (int) Math.round(Math.abs(radians) / LX.TWO_PI * this.tableSize);
        float val = this.values[index % this.values.length];
        return (radians > 0) ? val : -val;
      }
    }

    public static class Cos extends LookupTable {
      public Cos(int tableSize) {
        super(tableSize, Function.COS);
      }

      public float cos(float radians) {
        int index = (int) Math.round(Math.abs(radians) / LX.TWO_PI * this.tableSize);
        return this.values[index % this.values.length];
      }
    }

    public static class Tan extends LookupTable {
      public Tan(int tableSize) {
        super(tableSize, Function.TAN);
      }

      public float tan(float radians) {
        int index = (int) Math.round(Math.abs(radians) / LX.TWO_PI * this.tableSize);
        float val = this.values[index % this.values.length];
        return (radians > 0) ? val : -val;
      }
    }

    protected final int tableSize;
    protected float[] values;

    public LookupTable(int tableSize, Function function) {
      this.tableSize = tableSize;
      this.values = new float[tableSize + 1];
      for (int i = 0; i <= tableSize; ++i) {
        this.values[i] = function.compute(i, tableSize);
      }
    }

    /**
     * Looks up the value in the table
     * @param basis
     * @return
     */
    public float get(float basis) {
      return this.values[Math.round(basis * this.tableSize)];
    }

    public float get(double basis) {
      return this.values[(int) Math.round(basis * this.tableSize)];
    }
  }
}
