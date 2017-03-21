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

package heronarts.lx.modulator;

import heronarts.lx.LXComponent;
import heronarts.lx.LXRunnable;
import heronarts.lx.parameter.LXParameter;

/**
 * A Modulator is an abstraction for a variable with a value that varies over
 * time, such as an envelope or a low frequency oscillator. Some modulators run
 * continuously, others may halt after they reach a certain value.
 */
public abstract class LXModulator extends LXRunnable implements LXParameter {

  /**
   * The current computed value of this modulator.
   */
  private double value = 0;

  private final String label;

  private LXComponent component;

  private String path;

  /**
   * Quick helper to get half of PI.
   */
  public static final double HALF_PI = Math.PI / 2.;

  /**
   * Quick helper to get two times PI.
   */
  public static final double TWO_PI = Math.PI * 2.;

  private static int idCounter = 1;

  /**
   * Utility default constructor
   *
   * @param label Label
   */
  protected LXModulator(String label) {
    this.label = (label == null) ? (getClass().getSimpleName()+"-"+idCounter++) : label;
  }

  @Override
  public LXParameter setComponent(LXComponent component, String path) {
    if (component == null || path == null) {
      throw new IllegalArgumentException("May not set null component or path");
    }
    if (this.component != null || this.path != null) {
      throw new IllegalStateException("Component already set on this modulator: " + this);
    }
    this.component = component;
    this.path = path;
    return this;
  }

  @Override
  public LXComponent getComponent() {
    return this.component;
  }

  @Override
  public String getPath() {
    return this.path;
  }

  @Override
  public final String getLabel() {
    return this.label;
  }

    /**
   * Retrieves the current value of the modulator in full precision
   *
   * @return Current value of the modulator
   */
  public final double getValue() {
    return this.value;
  }

  /**
   * Retrieves the current value of the modulator in floating point precision.
   *
   * @return Current value of the modulator, cast to float
   */
  public final float getValuef() {
    return (float) this.getValue();
  }

  /**
   * Set the modulator to a certain value in its cycle.
   *
   * @param value The value to apply
   * @return This modulator, for method chaining
   */
  public final LXModulator setValue(double value) {
    this.value = value;
    this.onSetValue(value);
    return this;
  }

  /**
   * Subclasses may override when actions are necessary on value change.
   *
   * @param value New value
   */
  protected/* abstract */void onSetValue(double value) {
  }

  /**
   * Helper for subclasses to update value in situations where it needs to be
   * recomputed. This cannot be overriden, and subclasses may assume that it
   * ONLY updates the internal value without triggering any other
   * recomputations.
   *
   * @param value Value for modulator
   * @return this, for method chaining
   */
  protected final LXModulator updateValue(double value) {
    this.value = value;
    return this;
  }

  /**
   * Applies updates to the modulator for the specified number of milliseconds.
   * This method is invoked by the core engine.
   *
   * @param deltaMs Milliseconds to advance by
   */
  @Override
  protected final void run(double deltaMs) {
    this.value = this.computeValue(deltaMs);
  }

  /**
   * Implementation method to advance the modulator's internal state. Subclasses
   * must provide and update value appropriately.
   *
   * @param deltaMs Number of milliseconds to advance by
   * @return Computed value
   */
  protected abstract double computeValue(double deltaMs);

}
