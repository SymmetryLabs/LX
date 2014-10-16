/**
 * Copyright 2013- Mark C. Slee, Heron Arts LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package heronarts.lx.modulator;

import heronarts.lx.parameter.FixedParameter;
import heronarts.lx.parameter.LXParameter;

/**
 * A quadratic envelope moves from one value to another along a quadratic curve.
 */
public class QuadraticEnvelope extends LXRangeModulator {

  /**
   * Different modes of quadratic easing.
   */
  public enum Ease {
    /**
     * The quadratic curve accelerates towards the final value
     */
    IN,

    /**
     * The quadratic curve decelerates towards the final value
     */
    OUT,

    /**
     * The curve slops on both the start and end values
     */
    BOTH
  };

  private Ease ease = Ease.IN;

  public QuadraticEnvelope(double startValue, double endValue, double periodMs) {
    this(new FixedParameter(startValue), new FixedParameter(endValue),
        new FixedParameter(periodMs));
  }

  public QuadraticEnvelope(LXParameter startValue, double endValue,
      double periodMs) {
    this(startValue, new FixedParameter(endValue), new FixedParameter(periodMs));
  }

  public QuadraticEnvelope(double startValue, LXParameter endValue,
      double periodMs) {
    this(new FixedParameter(startValue), endValue, new FixedParameter(periodMs));
  }

  public QuadraticEnvelope(double startValue, double endValue,
      LXParameter periodMs) {
    this(new FixedParameter(startValue), new FixedParameter(endValue), periodMs);
  }

  public QuadraticEnvelope(LXParameter startValue, LXParameter endValue,
      double periodMs) {
    this(startValue, endValue, new FixedParameter(periodMs));
  }

  public QuadraticEnvelope(LXParameter startValue, double endValue,
      LXParameter periodMs) {
    this(startValue, new FixedParameter(endValue), periodMs);
  }

  public QuadraticEnvelope(double startValue, LXParameter endValue,
      LXParameter periodMs) {
    this(new FixedParameter(startValue), endValue, periodMs);
  }

  public QuadraticEnvelope(LXParameter startValue, LXParameter endValue,
      LXParameter periodMs) {
    this("QENV", startValue, endValue, periodMs);
  }

  public QuadraticEnvelope(String label, double startValue, double endValue,
      double periodMs) {
    this(label, new FixedParameter(startValue), new FixedParameter(endValue),
        new FixedParameter(periodMs));
  }

  public QuadraticEnvelope(String label, LXParameter startValue,
      double endValue, double periodMs) {
    this(label, startValue, new FixedParameter(endValue), new FixedParameter(
        periodMs));
  }

  public QuadraticEnvelope(String label, double startValue,
      LXParameter endValue, double periodMs) {
    this(label, new FixedParameter(startValue), endValue, new FixedParameter(
        periodMs));
  }

  public QuadraticEnvelope(String label, double startValue, double endValue,
      LXParameter periodMs) {
    this(label, new FixedParameter(startValue), new FixedParameter(endValue),
        periodMs);
  }

  public QuadraticEnvelope(String label, LXParameter startValue,
      LXParameter endValue, double periodMs) {
    this(label, startValue, endValue, new FixedParameter(periodMs));
  }

  public QuadraticEnvelope(String label, LXParameter startValue,
      double endValue, LXParameter periodMs) {
    this(label, startValue, new FixedParameter(endValue), periodMs);
  }

  public QuadraticEnvelope(String label, double startValue,
      LXParameter endValue, LXParameter periodMs) {
    this(label, new FixedParameter(startValue), endValue, periodMs);
  }

  public QuadraticEnvelope(String label, LXParameter startValue,
      LXParameter endValue, LXParameter periodMs) {
    super(label, startValue, endValue, periodMs);
    setLooping(false);
  }

  /**
   * Sets the easing type
   * 
   * @param ease easing type
   * @return this
   */
  public QuadraticEnvelope setEase(Ease ease) {
    this.ease = ease;
    return this;
  }

  @Override
  protected double computeNormalizedValue(double deltaMs, double basis) {
    switch (this.ease) {
    case IN:
      return basis * basis;
    case OUT:
      return 1 - (1 - basis) * (1 - basis);
    case BOTH:
      if (basis < 0.5) {
        return (basis * 2) * (basis * 2) / 2.;
      } else {
        final double biv = 1 - (basis - 0.5) * 2.;
        return 0.5 + (1 - biv * biv) / 2.;
      }
    }
    return 0;
  }

  @Override
  protected double computeNormalizedBasis(double basis, double normalizedValue) {
    switch (this.ease) {
    case IN:
      return Math.sqrt(normalizedValue);
    case OUT:
      return 1 - Math.sqrt(1 - normalizedValue);
    case BOTH:
      if (normalizedValue < 0.5) {
        normalizedValue = normalizedValue * 2;
        return Math.sqrt(normalizedValue) / 2.;
      } else {
        normalizedValue = (normalizedValue - 0.5) * 2;
        return 0.5 + (1 - Math.sqrt(1 - normalizedValue)) / 2.;
      }
    }
    return 0;
  }

}
