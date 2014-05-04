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

package heronarts.lx;

public class LXBufferComponent extends LXLayerComponent {

  protected LXBufferComponent(LX lx) {
    super(lx, new ModelBuffer(lx));
  }

  public final int[] getColors() {
    return getBuffer().getArray();
  }

  public final synchronized void copyColors(int[] copy) {
    int[] colors = getBuffer().getArray();
    for (int i = 0; i < colors.length; ++i) {
      copy[i] = colors[i];
    }
  }

  @Override
  public void loop(double deltaMs) {
    if (this.lx.engine.isThreaded()) {
      synchronized(this) {
        super.loop(deltaMs);
      }
    } else {
      super.loop(deltaMs);
    }
  }



}
