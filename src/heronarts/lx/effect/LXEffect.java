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

package heronarts.lx.effect;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXLayer;
import heronarts.lx.modulator.LXModulator;

import java.util.ArrayList;

/**
 * Class to represent an effect that may be applied to the color
 * array. Effects may be stateless or stateful, though typically they
 * operate on a single frame. Only the current frame is provided at
 * runtime.
 */
public abstract class LXEffect extends LXLayer {
	
	protected final LX lx;
	private final boolean momentary;
	protected boolean enabled = false;

	protected LXEffect(LX lx) {
		this(lx, false);
	}
	
	protected LXEffect(LX lx, boolean momentary) {
		this.lx = lx;
		this.momentary = momentary;
	}

	/**
	 * @return whether the effect is currently enabled
	 */
	public final boolean isEnabled() {
		return this.enabled;
	}
	
	/**
	 * @return Whether this is a momentary effect or not
	 */
	public final boolean isMomentary() {
		return this.momentary;
	}
	
	/**
	 * Toggles the effect.
	 * 
	 * @return this
	 */
	public final LXEffect toggle() {
		if (this.enabled) {
			this.disable();
		} else {
			this.enable();
		}
		return this;
	}
	
	/**
	 * Enables the effect.
	 * 
	 * @return this
	 */
	public final LXEffect enable() {
		if (!this.enabled) {
			this.enabled = true;
			this.onEnable();
		}
		return this;
	}
	
	/**
	 * Disables the effect.
	 */
	public final LXEffect disable() {
		if (this.enabled) {
			this.enabled = false;
			this.onDisable();
		}
		return this;
	}
	
	/**
	 * This is to trigger special one-shot effects. If the effect is enabled,
	 * then it is disabled. Otherwise, it's enabled state is never changed and
	 * it simply has its onTrigger method invoked. 
	 */
	public final void trigger() {
		if (this.enabled) {
			this.disable();
		} else {
			this.onTrigger();
		}
	}	
	
	protected /* abstract */ void onEnable() {}
	protected /* abstract */ void onDisable() {}
	protected /* abstract */ void onTrigger() {}
		
	/**
	 * Applies this effect to the current frame
	 * 
	 * @param deltaMs Milliseconds since last frame
	 * @param colors Array of this frame's colors 
	 */
	public final void run(double deltaMs, int[] colors) {
		for (LXModulator m : this.modulators) {
			m.run(deltaMs);
		}
		this.apply(colors);
		for (LXLayer layer : this.layers) {
			layer.run(deltaMs, colors);
		}
	}
	
	/**
	 * Implementation of the effect. Subclasses need to override this
	 * to implement their functionality.
	 * 
	 * @param colors
	 */
	protected abstract void apply(int[] colors);
}
