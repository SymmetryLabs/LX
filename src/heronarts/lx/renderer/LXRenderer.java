package heronarts.lx.renderer;

import java.util.List;

import heronarts.lx.LXChannel;
import heronarts.lx.LXLoopTask;

public interface LXRenderer extends LXLoopTask {

  public int[] blend(int[] colorBuffer, List<LXChannel> channels);

}
