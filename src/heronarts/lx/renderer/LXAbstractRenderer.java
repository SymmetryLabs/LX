package heronarts.lx.renderer;

import java.util.List;

import heronarts.lx.LXChannel;

public abstract class LXAbstractRenderer implements LXRenderer {

  protected double lastDeltaMs;

  @Override
  public void loop(double deltaMs) {
    this.lastDeltaMs = deltaMs;
  }

  @Override
  public abstract int[] blend(int[] colorBuffer, List<LXChannel> channels);

}
