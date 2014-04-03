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

import heronarts.lx.LXKeyEvent;
import heronarts.lx.LXUtils;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.ui.UI;
import heronarts.lx.ui.UIFocus;
import heronarts.lx.ui.UIObject;
import processing.core.PConstants;
import processing.core.PGraphics;

public class UIIntegerBox extends UIObject implements UIFocus {

  private int minValue = 0;
  private int maxValue = PConstants.MAX_INT;
  private int value = 0;
  private DiscreteParameter parameter = null;

  private final LXParameterListener parameterListener = new LXParameterListener() {
    public void onParameterChanged(LXParameter p) {
      setValue(parameter.getValuei());
    }
  };

  public UIIntegerBox() {
    this(0, 0, 0, 0);
  }

  public UIIntegerBox(float x, float y, float w, float h) {
    super(x, y, w, h);
    setBorderColor(0xff666666);
    setBackgroundColor(0xff222222);
  }

  public UIIntegerBox setParameter(final DiscreteParameter parameter) {
    if (this.parameter != null) {
      this.parameter.removeListener(this.parameterListener);
    }
    this.parameter = parameter;
    if (parameter != null) {
      this.minValue = parameter.getMinValue();
      this.maxValue = parameter.getMaxValue();
      this.value = parameter.getValuei();
      this.parameter.addListener(this.parameterListener);
    }
    return this;
  }

  public UIIntegerBox setRange(int minValue, int maxValue) {
    this.minValue = minValue;
    this.maxValue = maxValue;
    setValue(LXUtils.constrain(this.value, minValue, maxValue));
    return this;
  }

  public int getValue() {
    return this.value;
  }

  public UIIntegerBox setValue(int value) {
    if (this.value != value) {
      int range = (this.maxValue - this.minValue + 1);
      while (value < this.minValue) {
        value += range;
      }
      this.value = this.minValue + (value - this.minValue) % range;
      if (this.parameter != null) {
        this.parameter.setValue(this.value);
      }
      this.onValueChange(this.value);
      redraw();
    }
    return this;
  }

  @Override
  protected void onDraw(UI ui, PGraphics pg) {
    pg.textAlign(PConstants.CENTER, PConstants.CENTER);
    pg.textFont(ui.getItemFont());
    pg.fill(ui.getTextColor());
    pg.text("" + this.value, this.width / 2, this.height / 2);
  }

  protected void onValueChange(int value) {
  }

  float dAccum = 0;

  @Override
  protected void onMousePressed(float mx, float my) {
    this.dAccum = 0;
  }

  @Override
  protected void onMouseDragged(float mx, float my, float dx, float dy) {
    this.dAccum -= dy;
    int offset = (int) (this.dAccum / 5);
    this.dAccum = this.dAccum - (offset * 5);
    setValue(this.value + offset);
  }

  @Override
  public void onKeyPressed(LXKeyEvent keyEvent, char keyChar, int keyCode) {
    int times = 1;
    if (this.parameter != null) {
      if (keyEvent.isShiftDown()) {
        times = Math.max(1, this.parameter.getRange() / 10);
      }
    }
    if ((keyCode == java.awt.event.KeyEvent.VK_LEFT)
        || (keyCode == java.awt.event.KeyEvent.VK_DOWN)) {
      setValue(getValue() - times);
    } else if ((keyCode == java.awt.event.KeyEvent.VK_RIGHT)
        || (keyCode == java.awt.event.KeyEvent.VK_UP)) {
      setValue(getValue() + times);
    }
  }
}