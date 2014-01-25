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

package heronarts.lx.ui;

import processing.core.PGraphics;
import processing.core.PConstants;

/**
 * A UIContext is a container that owns a graphics buffer. This buffer is persistent
 * across frames and is only redrawn as necessary. It is simply bitmapped onto the
 * UI that is a part of.
 */
public class UIContext extends UIContainer {
		
	/**
	 * The UI that this belongs to.
	 */
	protected final UI ui;
	
	/**
	 * Graphics context for this container.
	 */
	private final PGraphics pg;
	
	/**
	 * Previous mouse x position
	 */
	private float px;
	
	/**
	 * Previous mouse y position
	 */
	private float py;
	
	/**
	 * Whether this context is currently taking mouse drag events.
	 */
	private boolean dragging = false;
	
	/**
	 * Constructs a new UIContext
	 * 
	 * @param ui the UI to place it in
	 * @param x x-position
	 * @param y y-position
	 * @param w width
	 * @param h height
	 */
	public UIContext(UI ui, float x, float y, float w, float h) {
		super(x, y, w, h);
		this.ui = ui;
		this.pg = ui.applet.createGraphics((int)this.width, (int)this.height, PConstants.JAVA2D);
		this.pg.smooth();
	}

	final void draw(UI ui) {
		if (!this.visible) {
			return;
		}
		if (this.needsRedraw || this.childNeedsRedraw) {
			this.pg.beginDraw();
			this.draw(this.pg);
			this.pg.endDraw();
		}
		ui.applet.image(this.pg, this.x, this.y);
	}

	boolean mousePressed(float mx, float my) {
		if (!this.visible) {
			return false;
		}
		if (contains(mx, my)) {
			this.dragging = true;
			this.px = mx;
			this.py = my;
			onMousePressed(mx - this.x, my - this.y);
			return true;
		}
		return false;
	}

	boolean mouseReleased(float mx, float my) {
		if (!this.visible) {
			return false;
		}
		this.dragging = false;
		onMouseReleased(mx - this.x, my - this.y);
		return true;
	}

	boolean mouseDragged(float mx, float my) {
		if (!this.visible) {
			return false;
		}
		if (this.dragging) {
			float dx = mx - this.px;
			float dy = my - this.py;
			onMouseDragged(mx - this.x, my - this.y, dx, dy);
			this.px = mx;
			this.py = my;
			return true;
		}
		return false;
	}

	boolean mouseWheel(float mx, float my, float delta) {
		if (!this.visible) {
			return false;
		}
		if (contains(mx, my)) {
			onMouseWheel(mx, my, delta);
			return true;
		}
		return false;
	}
}
