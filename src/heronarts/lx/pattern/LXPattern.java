/**
 * ##library.name##
 * ##library.sentence##
 * ##library.url##
 *
 * Copyright ##copyright## ##author##
 * All Rights Reserved
 * 
 * @author      ##author##
 * @modified    ##date##
 * @version     ##library.prettyVersion## (##library.version##)
 */

package heronarts.lx.pattern;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXDeck;
import heronarts.lx.LXLayer;
import heronarts.lx.model.LXFixture;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.transition.LXTransition;

import java.util.ArrayList;
import java.util.List;

import processing.core.PConstants;

/**
 * A pattern is the core object that the animation engine uses to generate colors
 * for all the points. It is   
 */
public abstract class LXPattern extends LXComponent {

	/**
	 * Reference to the LX instance
	 */
	protected final LX lx;

	/**
	 * Buffer of point colors for this pattern 
	 */
	protected final int[] colors;

	/**
	 * Reference to the deck this pattern belongs to.
	 */
	private LXDeck deck = null;
	
	/**
	 * Transition used when this pattern becomes active.
	 */
	protected LXTransition transition = null;
	
	private int intervalBegin = -1;
	
	private int intervalEnd = -1;
	
	private boolean eligible = true;
	
	public final Timer timer = new Timer();
	
	public class Timer {
		public long goNanos = 0;
	}
	
	protected LXPattern(LX lx) {
		this.lx = lx;
		this.colors = new int[lx.total];
	}
	
	/**
	 * Gets the deck that this pattern is loaded in. May be null if the pattern
	 * is not yet loaded onto any deck.
	 * 
	 * @return Deck pattern is loaded onto
	 */
	public final LXDeck getDeck() {
		return this.deck;
	}
	
	/**
	 * Called by the engine when pattern is loaded onto a deck. This may only be
	 * called once, by the engine. Do not call directly.
	 * 
	 * @param deck Deck pattern is loaded onto
	 * @return this
	 */
	public final LXPattern setDeck(LXDeck deck) {
		if (this.deck != null) {
			throw new RuntimeException("LXPattern instance can only be added to LXDeck once.");
		}
		this.deck = deck;
		return this;
	}
	
	/**
	 * Set an interval during which this pattern is allowed to run. Begin and end times
	 * are specified in minutes of the daytime. So midnight corresponds to the value
	 * of 0, 360 would be 6:00am, 1080 would be 18:00 (or 6:00pm)
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
		int now = this.lx.applet.hour()*60 + this.lx.applet.minute();
		if (this.intervalBegin < this.intervalEnd) {
			// Normal daytime interval
			return (now >= this.intervalBegin) && (now < this.intervalEnd);
		} else {
			// Wrapping around midnight
			return (now >= this.intervalBegin) || (now < this.intervalEnd);
		}
	}
	
	/**
	 * Sets whether this pattern is eligible for selection.
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
	 * Determines whether this pattern is eligible to be run at the moment. A pattern
	 * is eligible if its eligibility flag has not been set to false, and if it either
	 * has no interval, or is currently in its interval.
	 * 
	 * @return
	 */
	public final boolean isEligible() {
		return
			this.eligible &&
			(!this.hasInterval() || this.isInInterval());
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
	
	/**
	 * Sets the color of point i
	 * 
	 * @param i Point index
	 * @param c color
	 * @return this
	 */
	protected final LXPattern setColor(int i, int c) {
		this.colors[i] = c;
		return this;
	}

	/**
	 * Adds to the color of point i, using blendColor with ADD
	 * 
	 * @param i Point index
	 * @param c color
	 * @return this
	 */
	protected final LXPattern addColor(int i, int c) {
		this.colors[i] = this.lx.applet.blendColor(this.colors[i], c, PConstants.ADD);
		return this;
	}
	
	/**
	 * Adds to the color of point (x,y) in a default GridModel, using blendColor
	 * 
	 * @param x x-index
	 * @param y y-index
	 * @param c color
	 * @return this
	 */
	protected final LXPattern addColor(int x, int y, int c) {
		this.addColor(x + y * this.lx.width, c);
		return this;
	}
	
	/**
	 * Sets the color of point (x,y) in a default GridModel
	 * 
	 * @param x x-index
	 * @param y y-index
	 * @param c color
	 * @return this
	 */
	protected final LXPattern setColor(int x, int y, int c) {
		this.colors[x + y * this.lx.width] = c;
		return this;
	}

	/**
	 * Gets the color at point (x,y) in a GridModel
	 * 
	 * @param x x-index
	 * @param y y-index
	 * @return Color value
	 */
	protected final int getColor(int x, int y) {
		return this.colors[x + y * this.lx.width];
	}
	
	/**
	 * Sets all points to one color
	 * 
	 * @param c Color
	 * @return this
	 */
	protected final LXPattern setColors(int c) {
		for (int i = 0; i < colors.length; ++i) {
			this.colors[i] = c;
		}
		return this;
	}
	
	/**
	 * Sets the color of all points in a fixture
	 * 
	 * @param f Fixture
	 * @param c color
	 * @return this
	 */
	protected final LXPattern setColor(LXFixture f, int c) {
		for (LXPoint p : f.getPoints()) {
			this.colors[p.index] = c;
		}
		return this;
	}

	/**
	 * Clears all colors
	 * 
	 * @return this
	 */
	protected final LXPattern clearColors() {
		this.setColors(0);
		return this;
	}

	/**
	 * Gets the color buffer
	 * 
	 * @return color buffer for this pattern
	 */
	public final int[] getColors() {
		return this.colors;
	}

	/**
	 * Invoked by the engine when the pattern is running.
	 * 
	 * @param deltaMs
	 */
	public final void go(double deltaMs) {
		long goStart = System.nanoTime();
		for (LXModulator m : this.modulators) {
			m.run(deltaMs);
		}
		this.run(deltaMs);
		for (LXLayer layer : this.layers) {
			for (LXModulator m : layer.getModulators()) {
				m.run(deltaMs);
			}
			layer.run(deltaMs, this.colors);
		}
		this.timer.goNanos = System.nanoTime() - goStart;
	}
	
	/**
	 * Main pattern loop function. Invoked in a render loop. Subclasses must implement
	 * this function.
	 * 
	 * @param deltaMs Number of milliseconds elapsed since last invocation
	 */
	protected abstract void run(double deltaMs);

	/**
	 * Invoked by the engine when pattern will become active. May not be
	 * overridden. Use onActive() instead.
	 */
	public final void willBecomeActive() {
		this.onActive();
	}
	
	/**
	 * Invoked by the engine when pattern is no longer active. May not be
	 * overridden. Use onInactive() instead.
	 */
	public final void didResignActive() {
		this.onInactive();
	}
	
	/**
	 * Subclasses may override this method. It will be invoked when the pattern is
	 * about to become active. Patterns may take care of any initialization needed
	 * or reset parameters if desired.
	 */
	protected /* abstract */ void onActive() {}
	
	/**
	 * Subclasses may override this method. It will be invoked when the pattern is
	 * no longer active. Resources may be freed if desired.
	 */
	protected /* abstract */ void onInactive() {}
	
	/**
	 * Subclasses may override this method. It will be invoked if a transition into
	 * this pattern is taking place. This will be called after onActive. This is not
	 * invoked on an already-running pattern. It is only called on the new pattern.
	 */
	public /* abstract */ void onTransitionStart() {}
	
	/**
	 * Subclasses may override this method. It will be invoked when the transition into
	 * this pattern is complete.
	 */
	public /* abstract */ void onTransitionEnd() {}

}

