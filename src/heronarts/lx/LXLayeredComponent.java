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

package heronarts.lx;

import heronarts.lx.color.LXColor;
import heronarts.lx.color.LXPalette;
import heronarts.lx.model.LXFixture;
import heronarts.lx.model.LXPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for system components that have a color buffer and run in the
 * engine, with common attributes such as parameters, modulators, and layers.
 * For instance, patterns, transitions, and effects are all LXLayeredComponents.
 * Subclasses do their work mainly by implementing onLoop() to write into the
 * color buffer.
 *
 * LXLayeredComponents have a HybridBuffer, which manages a pair of 8-bit and
 * 16-bit color buffers (see HybridBuffer for details).  Subclasses marked with
 * LXLayeredComponent.Uses16 should internally write only to the 16-bit buffer
 * (whose contents will be automatically converted to 8-bit colors as needed);
 * subclasses not marked with Uses16 should write only to the 8-bit buffer
 * (whose contents will be automatically converted to 16-bit colors as needed).
 *
 * LXLayeredComponent subclasses can be marked as LXLayeredComponent.Buffered
 * (which means they own their own buffers), or not (which means they operate
 * on external buffers passed in via setBuffer() or setBuffer16()).
 *
 * For subclasses marked Buffered:
 *     Internal API:
 *         The implementation of onLoop() should write colors into the
 *         appropriate array (colors16 for a Uses16 class, or colors otherwise).
 *     External API:
 *         getBuffer() returns the 8-bit colors (converting from 16 bits if needed).
 *         getBuffer16() returns the 16-bit colors (converting from 8 bits if needed).
 *         setBuffer() and setBuffer16() are illegal to call.
 *
 * For subclasses not marked Buffered:
 *     Internal API:
 *         The implementation of onLoop() should read and write the contents
 *         of the appropriate array (colors16 for a Uses16 class, colors otherwise).
 *     External API:
 *         getBuffer() and getBuffer16() are the same as above.
 *         setBuffer() takes an 8-bit LXBuffer and makes it the place where
 *             onLoop()'s results will appear (in a Uses16 class, its contents
 *             will be converted to 16 bits before onLoop() and then back to
 *             8 bits after; in a non-Uses16 class no conversion is needed).
 *         setBuffer16() takes a 16-bit LXBuffer16 and makes it the place where
 *             onLoop()'s results will appear (in a non-Uses16 class, its
 *             contents will be converted to 8 bits before onLoop() and then
 *             back to 16 bits after; in a Uses16 class no conversion is needed).
 */
public abstract class LXLayeredComponent extends LXModelComponent implements LXLoopTask {
  /** Marker interface for subclasses that operate on the 16-bit color buffer. */
  public interface Uses16 {}

  /** Marker interface for subclasses that want to own their own buffers. */
  public interface Buffered {}

  public final Timer timer = constructTimer();
  protected final LX lx;

  /** The hybrid buffer contains the 8-bit and 16-bit color buffers. */
  private HybridBuffer hybridBuffer = null;

  // colors and colors16 are aliases for the 8-bit and 16-bit color buffer arrays,
  // for use in subclass implementations of onLoop() and run().
  protected int[] colors = null;
  protected long[] colors16 = null;

  private final List<LXLayer> mutableLayers = new ArrayList<LXLayer>();
  protected final List<LXLayer> layers = Collections.unmodifiableList(mutableLayers);

  protected final LXPalette palette;

  protected LXLayeredComponent(LX lx) {
    super(lx);
    this.lx = lx;
    palette = lx.palette;
    hybridBuffer = new HybridBuffer(lx);
    resetArrayAliases();
  }

  protected LXLayeredComponent(LX lx, LXDeviceComponent component) {
    this(lx);
    setBuffer(component);
  }

  protected LXLayeredComponent(LX lx, LXBuffer externalIntBuffer) {
    this(lx);
    setBuffer(externalIntBuffer);
  }

  protected LXLayeredComponent(LX lx, LXBuffer16 externalLongBuffer) {
    this(lx);
    setBuffer16(externalLongBuffer);
  }

  /** Gets the 8-bit color buffer (performing conversions if necessary). */
  protected LXBuffer getBuffer() {
    return hybridBuffer.getBuffer();
  }

  /** Gets the 16-bit color buffer (performing conversions if necessary). */
  protected LXBuffer16 getBuffer16() {
    return hybridBuffer.getBuffer16();
  }

  /** Gets the 8-bit color buffer's array (performing conversions if necessary). */
  public int[] getColors() {
    return hybridBuffer.getBuffer().getArray();
  }

  /** Gets the 16-bit color buffer's array (performing conversions if necessary). */
  public long[] getColors16() {
    return hybridBuffer.getBuffer16().getArray16();
  }

  /**
   * Sets up the internal colors/colors16 fields as convenient aliases for the
   * 8-bit/16-bit color buffer arrays, for use in subclass implementations of
   * onLoop() and run().  Just one of colors/colors16 is set and the other is null.
   */
  protected void resetArrayAliases() {
    colors = (this instanceof Uses16) ? null : getColors();
    colors16 = (this instanceof Uses16) ? getColors16() : null;
  }

  /** Sets the buffer of another component as the buffer to read from and write to. */
  protected LXLayeredComponent setBuffer(LXLayeredComponent component) {
    if (component instanceof Uses16) {
      setBuffer16(component.getBuffer16());
    } else {
      setBuffer(component.getBuffer());
    }
    resetArrayAliases();
    return this;
  }

  /** Sets an external 8-bit color buffer as the buffer to read from and write to. */
  protected LXLayeredComponent setBuffer(LXBuffer externalBuffer) {
    assertNotBuffered();
    hybridBuffer.setBuffer(externalBuffer);
    hybridBuffer.setBuffer16(null);
    resetArrayAliases();
    return this;
  }

  /** Sets an external 16-bit color buffer as the buffer to read from and write to. */
  protected LXLayeredComponent setBuffer16(LXBuffer16 externalBuffer16) {
    assertNotBuffered();
    hybridBuffer.setBuffer16(externalBuffer16);
    hybridBuffer.setBuffer(null);
    resetArrayAliases();
    return this;
  }

  protected void markBufferModified() {
    if (this instanceof Uses16) {
      hybridBuffer.markBuffer16Modified();
    } else {
      hybridBuffer.markBufferModified();
    }
  }

  private void assertNotBuffered() {
    if (this instanceof Buffered) {
      throw new UnsupportedOperationException("Cannot set an external buffer in a Buffered LXLayeredComponent");
    }
  }

  @Override
  public void loop(double deltaMs) {
    long loopStart = System.nanoTime();

    // To ensure that colors/colors16 are set correctly even if a subclass
    // happens to reassign them, we reset them here on every call to loop().
    resetArrayAliases();

    super.loop(deltaMs);
    onLoop(deltaMs);

    for (LXLayer layer : this.mutableLayers) {
      layer.setBuffer(this);

      // TODO(mcslee): is this best here or should it be in addLayer?
      layer.setModel(this.model);

      layer.loop(deltaMs);
    }
    afterLayers(deltaMs);

    if (!(this instanceof Buffered)) {
      // The buffers are external; we need to make the output from onLoop() and
      // afterLayers() visible in the external buffers, converting if needed.
      hybridBuffer.sync();
    }

    this.timer.loopNanos = System.nanoTime() - loopStart;
  }

  protected /* abstract */ void onLoop(double deltaMs) {
      // Implementations should call markBufferModified() if they modify the color buffer.
  }

  protected /* abstract */ void afterLayers(double deltaMs) {
      // Implementations should call markBufferModified() if they modify the color buffer.
  }

  protected final LXLayer addLayer(LXLayer layer) {
    if (this.mutableLayers.contains(layer)) {
      throw new IllegalStateException("Cannot add same layer twice: " + this + " " + layer);
    }
    layer.setParent(this);
    this.mutableLayers.add(layer);
    return layer;
  }

  protected final LXLayer removeLayer(LXLayer layer) {
    this.mutableLayers.remove(layer);
    layer.dispose();
    return layer;
  }

  public final List<LXLayer> getLayers() {
    return this.layers;
  }

  @Override
  public void dispose() {
    for (LXLayer layer : this.mutableLayers) {
      layer.dispose();
    }
    this.mutableLayers.clear();
    super.dispose();
  }

  // NOTE(ping): Most of the utility routines below are rarely used, and rarely or never chained.
  // We won't reimplement them all for 16-bit color, just setColor and setColors.

  /** Sets the 16-bit color of a single point. */
  protected void setColor16(int i, long c) {
    colors16[i] = c;
  }

  /** Sets the 16-bit color of all points. */
  protected void setColors16(int c) {
    for (int i = 0; i < colors16.length; i++) {
      colors16[i] = c;
    }
  }

  /** Sets the 16-bit color of all points in a fixture. */
  protected void setColor16(LXFixture f, long c) {
    for (LXPoint p : f.getPoints()) {
      colors16[p.index] = c;
    }
  }


  /**
   * Sets the color of point i
   *
   * @param i Point index
   * @param c color
   * @return this
   */
  protected final LXLayeredComponent setColor(int i, int c) {
    this.colors[i] = c;
    return this;
  }

  /**
   * Blend the color at index i with its existing value
   *
   * @param i Index
   * @param c New color
   * @param blendMode blending mode
   *
   * @return this
   */
  protected final LXLayeredComponent blendColor(int i, int c, LXColor.Blend blendMode) {
    this.colors[i] = LXColor.blend(this.colors[i], c, blendMode);
    return this;
  }

  protected final LXLayeredComponent blendColor(LXFixture f, int c, LXColor.Blend blendMode) {
    for (LXPoint p : f.getPoints()) {
      this.colors[p.index] = LXColor.blend(this.colors[p.index], c, blendMode);
    }
    return this;
  }

  /**
   * Adds to the color of point i, using blendColor with ADD
   *
   * @param i Point index
   * @param c color
   * @return this
   */
  protected final LXLayeredComponent addColor(int i, int c) {
    this.colors[i] = LXColor.add(this.colors[i], c);
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
  protected final LXLayeredComponent addColor(int x, int y, int c) {
    return addColor(x + y * this.lx.width, c);
  }

  /**
   * Adds the color to the fixture
   *
   * @param f Fixture
   * @param c New color
   * @return this
   */
  protected final LXLayeredComponent addColor(LXFixture f, int c) {
    for (LXPoint p : f.getPoints()) {
      this.colors[p.index] = LXColor.add(this.colors[p.index], c);
    }
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
  protected final LXLayeredComponent setColor(int x, int y, int c) {
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
  protected final LXLayeredComponent setColors(int c) {
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
  protected final LXLayeredComponent setColor(LXFixture f, int c) {
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
  protected final LXLayeredComponent clearColors() {
    return setColors(0);
  }

}
