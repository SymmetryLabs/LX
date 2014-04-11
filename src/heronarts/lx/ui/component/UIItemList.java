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
import heronarts.lx.LXKeyEvent;
import heronarts.lx.LXUtils;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.ui.UI;
import heronarts.lx.ui.UIFocus;
import heronarts.lx.ui.UIObject;

import java.util.ArrayList;
import java.util.List;

import processing.core.PConstants;
import processing.core.PGraphics;

/**
 * UI for a list of state items
 */
public class UIItemList extends UIObject implements UIFocus {

  public static interface Item {

    public boolean isSelected();

    public boolean isPending();

    public String getLabel();

    public void onMousePressed();

    public void onMouseReleased();
  }

  public static abstract class AbstractItem implements Item {
    public boolean isPending() {
      return false;
    }

    public void onMousePressed() {
    }

    public void onMouseReleased() {
    }
  }

  private List<Item> items = new ArrayList<Item>();

  public final DiscreteParameter focusIndex = new DiscreteParameter("FOCUS", 1);
  public final DiscreteParameter scrollOffset = new DiscreteParameter("OFFSET",
      1);

  private int itemHeight = 20;
  private int numVisibleItems = 0;

  private boolean hasScroll;
  private float scrollYStart;
  private float scrollYHeight;

  public UIItemList() {
    this(0, 0, 0, 0);
  }

  public UIItemList(float x, float y, float w, float h) {
    super(x, y, w, h);
    this.focusIndex.addListener(new LXParameterListener() {
      public void onParameterChanged(LXParameter parameter) {
        onFocusIndexChanged();
      }
    });
    this.scrollOffset.addListener(new LXParameterListener() {
      public void onParameterChanged(LXParameter parameter) {
        onScrollOffsetChanged();
      }
    });
  }

  public int getFocusIndex() {
    return this.focusIndex.getValuei();
  }

  public UIItemList setFocusIndex(int focusIndex) {
    this.focusIndex.setValue(focusIndex);
    return this;
  }

  private void onFocusIndexChanged() {
    int fi = this.focusIndex.getValuei();
    int so = this.scrollOffset.getValuei();
    if (fi < so) {
      setScrollOffset(fi);
    } else if (fi >= (so + this.numVisibleItems)) {
      setScrollOffset(fi - this.numVisibleItems + 1);
    }
    redraw();
  }

  public UIItemList select() {
    this.keyedItem = this.items.get(getFocusIndex());
    this.keyedItem.onMousePressed();
    this.keyedItem.onMouseReleased();
    redraw();
    return this;
  }

  @Override
  protected void onDraw(UI ui, PGraphics pg) {
    int yp = 0;
    boolean even = true;
    int so = getScrollOffset();
    int fi = getFocusIndex();
    pg.strokeWeight(1);
    for (int i = 0; i < this.numVisibleItems; ++i) {
      if (i + so >= this.items.size()) {
        break;
      }
      int itemIndex = i + so;
      Item item = this.items.get(itemIndex);
      int itemColor;
      int labelColor = ui.WHITE;
      if (item.isSelected()) {
        itemColor = ui.getHighlightColor();
      } else if (item.isPending()) {
        itemColor = ui.getSelectionColor();
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

      if (itemIndex == fi) {
        pg.stroke(item.isSelected() ? 0xff999999 : ui.getFocusColor());
        pg.noFill();
        pg.rect(0, yp, this.width - 1, this.itemHeight - 1);
      }

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
  private Item pressedItem = null;
  private Item keyedItem = null;

  @Override
  public void onKeyPressed(LXKeyEvent keyEvent, char keyChar, int keyCode) {
    int index = getFocusIndex();
    if (keyCode == java.awt.event.KeyEvent.VK_UP) {
      index = Math.max(0, index - 1);
    } else if (keyCode == java.awt.event.KeyEvent.VK_DOWN) {
      index = Math.min(index + 1, this.items.size() - 1);
    } else if ((keyChar == ' ')
        || (keyCode == java.awt.event.KeyEvent.VK_ENTER)) {
      select();
    }
    setFocusIndex(index);
  }

  @Override
  public void onKeyReleased(LXKeyEvent keyEvent, char keyChar, int keyCode) {
    if ((keyChar == ' ') || (keyCode == java.awt.event.KeyEvent.VK_ENTER)) {
      if (this.keyedItem != null) {
        this.keyedItem.onMouseReleased();
        redraw();
      }
    }
  }

  @Override
  public void onMousePressed(float mx, float my) {
    this.pressedItem = null;
    if (this.hasScroll && mx >= this.width - 12) {
      if ((my >= this.scrollYStart)
          && (my < (this.scrollYStart + this.scrollYHeight))) {
        this.scrolling = true;
        this.dAccum = 0;
      }
    } else {
      int index = (int) my / this.itemHeight;
      if (getScrollOffset() + index < this.items.size()) {
        setFocusIndex(getScrollOffset() + index);
        this.pressedItem = this.items.get(getFocusIndex());
        this.pressedItem.onMousePressed();
        redraw();
      }
    }
  }

  @Override
  public void onMouseReleased(float mx, float my) {
    this.scrolling = false;
    if (this.pressedItem != null) {
      this.pressedItem.onMouseReleased();
      redraw();
    }
  }

  private float dAccum = 0;

  @Override
  public void onMouseDragged(float mx, float my, float dx, float dy) {
    if (this.scrolling) {
      this.dAccum += dy;
      float scrollOne = this.height / this.items.size();
      int offset = (int) (this.dAccum / scrollOne);
      if (offset != 0) {
        this.dAccum -= offset * scrollOne;
        moveScrollOffset(offset);
      }
    }
  }

  private float wAccum = 0;

  @Override
  public void onMouseWheel(float mx, float my, float delta) {
    if (!hasFocus()) {
      focus();
    }
    this.wAccum += delta;
    int offset = (int) (this.wAccum / 5);
    if (offset != 0) {
      this.wAccum -= offset * 5;
      moveScrollOffset(offset);
    }
  }

  public int getScrollOffset() {
    return this.scrollOffset.getValuei();
  }

  public UIItemList setScrollOffset(int offset) {
    this.scrollOffset.setValue(LXUtils.constrain(offset, 0, this.items.size()
        - this.numVisibleItems));
    return this;
  }

  private void moveScrollOffset(int delta) {
    setScrollOffset(getScrollOffset() + delta);
  }

  private void onScrollOffsetChanged() {
    int so = this.scrollOffset.getValuei();
    this.scrollYStart = Math.round(so * this.height / this.items.size());
    this.scrollYHeight = Math.round(this.numVisibleItems * this.height
        / this.items.size());
    int fi = this.focusIndex.getValuei();
    if ((fi < so) || (fi > so + this.numVisibleItems - 1)) {
      setFocusIndex(so);
    }
    redraw();
  }

  public UIItemList setItems(List<Item> items) {
    this.items = items;
    this.numVisibleItems = (int) (this.height / this.itemHeight);
    this.hasScroll = this.items.size() > this.numVisibleItems;
    this.focusIndex.setRange(0, this.items.size());
    this.scrollOffset.setRange(0,
        Math.max(0, this.items.size() - this.numVisibleItems) + 1);
    onScrollOffsetChanged();
    redraw();
    return this;
  }
}
