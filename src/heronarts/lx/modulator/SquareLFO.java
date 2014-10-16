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
 * Simple square wave LFO. Not damped. Oscillates between a low and high value.
 */
public class SquareLFO extends LXRangeModulator {

  public SquareLFO(double startValue, double endValue, double periodMs) {
    this(new FixedParameter(startValue), new FixedParameter(endValue),
        new FixedParameter(periodMs));
  }

  public SquareLFO(LXParameter startValue, double endValue, double periodMs) {
    this(startValue, new FixedParameter(endValue), new FixedParameter(periodMs));
  }

  public SquareLFO(double startValue, LXParameter endValue, double periodMs) {
    this(new FixedParameter(startValue), endValue, new FixedParameter(periodMs));
  }

  public SquareLFO(double startValue, double endValue, LXParameter periodMs) {
    this(new FixedParameter(startValue), new FixedParameter(endValue), periodMs);
  }

  public SquareLFO(LXParameter startValue, LXParameter endValue, double periodMs) {
    this(startValue, endValue, new FixedParameter(periodMs));
  }

  public SquareLFO(LXParameter startValue, double endValue, LXParameter periodMs) {
    this(startValue, new FixedParameter(endValue), periodMs);
  }

  public SquareLFO(double startValue, LXParameter endValue, LXParameter periodMs) {
    this(new FixedParameter(startValue), endValue, periodMs);
  }

  public SquareLFO(LXParameter startValue, LXParameter endValue,
      LXParameter periodMs) {
    this("SQUARE", startValue, endValue, periodMs);
  }

  public SquareLFO(String label, double startValue, double endValue,
      double periodMs) {
    this(label, new FixedParameter(startValue), new FixedParameter(endValue),
        new FixedParameter(periodMs));
  }

  public SquareLFO(String label, LXParameter startValue, double endValue,
      double periodMs) {
    this(label, startValue, new FixedParameter(endValue), new FixedParameter(
        periodMs));
  }

  public SquareLFO(String label, double startValue, LXParameter endValue,
      double periodMs) {
    this(label, new FixedParameter(startValue), endValue, new FixedParameter(
        periodMs));
  }

  public SquareLFO(String label, double startValue, double endValue,
      LXParameter periodMs) {
    this(label, new FixedParameter(startValue), new FixedParameter(endValue),
        periodMs);
  }

  public SquareLFO(String label, LXParameter startValue, LXParameter endValue,
      double periodMs) {
    this(label, startValue, endValue, new FixedParameter(periodMs));
  }

  public SquareLFO(String label, LXParameter startValue, double endValue,
      LXParameter periodMs) {
    this(label, startValue, new FixedParameter(endValue), periodMs);
  }

  public SquareLFO(String label, double startValue, LXParameter endValue,
      LXParameter periodMs) {
    this(label, new FixedParameter(startValue), endValue, periodMs);
  }

  public SquareLFO(String label, LXParameter startValue, LXParameter endValue,
      LXParameter periodMs) {
    super(label, startValue, endValue, periodMs);
  }

  @Override
  protected double computeNormalizedValue(double deltaMs, double basis) {
    return (basis < 0.5) ? 0 : 1;
  }

  @Override
  protected double computeNormalizedBasis(double basis, double normalizedValue) {
    return (normalizedValue == 0) ? 0 : 0.5;
  }

}
