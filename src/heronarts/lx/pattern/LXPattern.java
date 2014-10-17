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

package heronarts.lx.pattern;

import heronarts.lx.LX;
import heronarts.lx.LXBufferedComponent;
import heronarts.lx.LXChannel;
import heronarts.lx.LXTime;
import heronarts.lx.transition.LXTransition;

/**
 * A pattern is the core object that the animation engine uses to generate
 * colors for all the points. It is
 */
public abstract class LXPattern extends LXBufferedComponent {

  /**
   * Reference to the channel this pattern belongs to.
   */
  private LXChannel channel = null;

  /**
   * Transition used when this pattern becomes active.
   */
  protected LXTransition transition = null;

  private int intervalBegin = -1;

  private int intervalEnd = -1;

  private boolean eligible = true;

  public final Timer timer = new Timer();

  public class Timer {
    public long runNanos = 0;
  }

  protected LXPattern(LX lx) {
    super(lx);
  }

  /**
   * Gets the name of this pattern
   *
   * @return Name of this pattern
   */
  @Override
  public String getName() {
    String simple = getClass().getSimpleName();
    int index = simple.indexOf("Pattern");
    if (index >= 0) {
      simple = simple.substring(0, index);
    }
    return simple;
  }

  /**
   * Gets the channel that this pattern is loaded in. May be null if the pattern is
   * not yet loaded onto any channel.
   *
   * @return Channel pattern is loaded onto
   */
  public final LXChannel getChannel() {
    return this.channel;
  }

  /**
   * Called by the engine when pattern is loaded onto a channel. This may only be
   * called once, by the engine. Do not call directly.
   *
   * @param channel Channel pattern is loaded onto
   * @return this
   */
  public final LXPattern setChannel(LXChannel channel) {
    if ((channel != null) && (this.channel != null)) {
      throw new UnsupportedOperationException("LXPattern instance can only be added to one LXChannel at a time.");
    }
    this.channel = channel;
    return this;
  }

  /**
   * Set an interval during which this pattern is allowed to run. Begin and end
   * times are specified in minutes of the daytime. So midnight corresponds to
   * the value of 0, 360 would be 6:00am, 1080 would be 18:00 (or 6:00pm)
   *
   * @param begin Interval start time
   * @param end Interval end time
   * @return this
   */
  public LXPattern setInterval(int begin, int end) {
    this.intervalBegin = begin;
    this.intervalEnd = end;
    return this;
  }

  /**
   * Clears a timer interval set to this pattern.
   *
   * @return this
   */
  public LXPattern clearInterval() {
    this.intervalBegin = this.intervalEnd = -1;
    return this;
  }

  /**
   * Tests whether there is an interval for this pattern.
   *
   * @return true if there is an interval
   */
  public final boolean hasInterval() {
    return (this.intervalBegin >= 0) && (this.intervalEnd >= 0);
  }

  /**
   * Tests whether this pattern is in an eligible interval.
   *
   * @return true if the pattern has an interval, and is currently in it.
   */
  public final boolean isInInterval() {
    if (!this.hasInterval()) {
      return false;
    }
    int now = LXTime.hour() * 60 + LXTime.minute();
    if (this.intervalBegin < this.intervalEnd) {
      // Normal daytime interval
      return (now >= this.intervalBegin) && (now < this.intervalEnd);
    } else {
      // Wrapping around midnight
      return (now >= this.intervalBegin) || (now < this.intervalEnd);
    }
  }

  /**
   * Sets whether this pattern is eligible for automatic selection.
   *
   * @param eligible
   * @return this
   */
  public final LXPattern setEligible(boolean eligible) {
    this.eligible = eligible;
    return this;
  }

  /**
   * Toggles the eligibility state of this pattern.
   *
   * @return this
   */
  public final LXPattern toggleEligible() {
    this.setEligible(!this.eligible);
    return this;
  }

  /**
   * Determines whether this pattern is eligible to be run at the moment. A
   * pattern is eligible if its eligibility flag has not been set to false, and
   * if it either has no interval, or is currently in its interval.
   *
   * @return True if pattern is eligible to run now
   */
  public final boolean isEligible() {
    return this.eligible && (!this.hasInterval() || this.isInInterval());
  }

  /**
   * Sets the transition to be used when this pattern becomes active.
   *
   * @param transition
   * @return this
   */
  public final LXPattern setTransition(LXTransition transition) {
    this.transition = transition;
    return this;
  }

  /**
   * Gets the transition to be used when this pattern becomes active.
   *
   * @return transition
   */
  public final LXTransition getTransition() {
    return transition;
  }

  @Override
  protected final void onLoop(double deltaMs) {
    long runStart = System.nanoTime();
    this.run(deltaMs);
    this.timer.runNanos = System.nanoTime() - runStart;
  }

  /**
   * Main pattern loop function. Invoked in a render loop. Subclasses must
   * implement this function.
   *
   * @param deltaMs Number of milliseconds elapsed since last invocation
   */
  protected abstract void run(double deltaMs);

  /**
   * Subclasses may override this method. It will be invoked when the pattern is
   * about to become active. Patterns may take care of any initialization needed
   * or reset parameters if desired.
   */
  public/* abstract */void onActive() {
  }

  /**
   * Subclasses may override this method. It will be invoked when the pattern is
   * no longer active. Resources may be freed if desired.
   */
  public/* abstract */void onInactive() {
  }

  /**
   * Subclasses may override this method. It will be invoked if a transition
   * into this pattern is taking place. This will be called after onActive. This
   * is not invoked on an already-running pattern. It is only called on the new
   * pattern.
   */
  public/* abstract */void onTransitionStart() {
  }

  /**
   * Subclasses may override this method. It will be invoked when the transition
   * into this pattern is complete.
   */
  public/* abstract */void onTransitionEnd() {
  }

}
