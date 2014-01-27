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

package heronarts.lx.ui.component;

import heronarts.lx.LX;
import heronarts.lx.LXUtils;
import heronarts.lx.ui.UI;
import heronarts.lx.ui.UIObject;

import java.util.ArrayList;
import java.util.List;

import processing.core.PConstants;
import processing.core.PFont;
import processing.core.PGraphics;

public class UIScrollList extends UIObject {

	public static interface ScrollItem {
		public boolean isSelected();

		public boolean isPending();

		public String getLabel();

		public void onMousePressed();

		public void onMouseReleased();
	}

	public static abstract class AbstractScrollItem implements ScrollItem {
		public boolean isPending() {
			return false;
		}

		public void select() {
		}

		public void onMousePressed() {
		}

		public void onMouseReleased() {
		}
	}

	private List<ScrollItem> items = new ArrayList<ScrollItem>();

	private int itemHeight = 20;
	private int scrollOffset = 0;
	private int numVisibleItems = 0;

	private boolean hasScroll;
	private float scrollYStart;
	private float scrollYHeight;

	public UIScrollList(float x, float y, float w, float h) {
		super(x, y, w, h);
	}

	protected void onDraw(UI ui, PGraphics pg) {
		int yp = 0;
		boolean even = true;
		for (int i = 0; i < this.numVisibleItems; ++i) {
			if (i + this.scrollOffset >= this.items.size()) {
				break;
			}
			ScrollItem item = this.items.get(i + this.scrollOffset);
			int itemColor;
			int labelColor = ui.WHITE;
			if (item.isSelected()) {
				itemColor = ui.getHighlightColor();
			} else if (item.isPending()) {
				itemColor = ui.getActiveColor();
			} else {
				labelColor = ui.BLACK;
				itemColor = 0xff707070;
			}
			float factor = even ? .92f : 1.08f;
			itemColor = LX.scaleBrightness(itemColor, factor);

			pg.noStroke();
			pg.fill(itemColor);
			pg.rect(0, yp, this.width, this.itemHeight);
			pg.fill(labelColor);
			pg.textFont(ui.getItemFont());
			pg.textAlign(PConstants.LEFT, PConstants.TOP);
			pg.text(item.getLabel(), 6, yp + 4);

			yp += this.itemHeight;
			even = !even;
		}
		if (this.hasScroll) {
			pg.noStroke();
			pg.fill(0x26ffffff);
			pg.rect(this.width - 12, 0, 12, this.height);
			pg.fill(0xff333333);
			pg.rect(this.width - 12, this.scrollYStart, 12, this.scrollYHeight);
		}

	}

	private boolean scrolling = false;
	private ScrollItem pressedItem = null;

	public void onMousePressed(float mx, float my) {
		this.pressedItem = null;
		if (this.hasScroll && mx >= this.width - 12) {
			if (my >= this.scrollYStart
					&& my < (this.scrollYStart + this.scrollYHeight)) {
				this.scrolling = true;
				this.dAccum = 0;
			}
		} else {
			int index = (int) my / this.itemHeight;
			if (this.scrollOffset + index < this.items.size()) {
				this.pressedItem = this.items.get(this.scrollOffset + index);
				this.pressedItem.onMousePressed();
				redraw();
			}
		}
	}

	public void onMouseReleased(float mx, float my) {
		this.scrolling = false;
		if (this.pressedItem != null) {
			this.pressedItem.onMouseReleased();
			redraw();
		}
	}

	private float dAccum = 0;

	public void onMouseDragged(float mx, float my, float dx, float dy) {
		if (this.scrolling) {
			this.dAccum += dy;
			float scrollOne = this.height / this.items.size();
			int offset = (int) (this.dAccum / scrollOne);
			if (offset != 0) {
				this.dAccum -= offset * scrollOne;
				setScrollOffset(this.scrollOffset + offset);
			}
		}
	}

	private float wAccum = 0;

	public void onMouseWheel(float mx, float my, float delta) {
		this.wAccum += delta;
		int offset = (int) (this.wAccum / 5);
		if (offset != 0) {
			this.wAccum -= offset * 5;
			setScrollOffset(this.scrollOffset + offset);
		}
	}

	public void setScrollOffset(int offset) {
		this.scrollOffset = LXUtils.constrain(offset, 0,
				Math.max(0, this.items.size() - this.numVisibleItems));
		this.scrollYStart = Math.round(this.scrollOffset * this.height
				/ this.items.size());
		this.scrollYHeight = Math.round(this.numVisibleItems * this.height
				/ this.items.size());
		redraw();
	}

	public UIScrollList setItems(List<ScrollItem> items) {
		this.items = items;
		this.numVisibleItems = (int) (this.height / this.itemHeight);
		this.hasScroll = this.items.size() > this.numVisibleItems;
		setScrollOffset(0);
		redraw();
		return this;
	}
}
	