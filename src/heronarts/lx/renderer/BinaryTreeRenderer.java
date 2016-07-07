package heronarts.lx.renderer;

import java.util.ArrayList;
import java.util.List;

import heronarts.lx.LXChannel;

public class BinaryTreeRenderer extends LXAbstractRenderer {

  private final List<List<LXRendererBlending>> blendings = new ArrayList<List<LXRendererBlending>>();

  public void setBlending(int tier, int index, LXRendererBlending blending) {
    while (this.blendings.size() <= tier) {
      this.blendings.add(new ArrayList<LXRendererBlending>());
    }
    List<LXRendererBlending> blendingsRow = this.blendings.get(tier);
    while (blendingsRow.size() <= index) {
      blendingsRow.add(null);
    }
    blendingsRow.set(index, blending);
  }

  public LXRendererBlending getBlending(int tier, int index) {
    if (tier < this.blendings.size()) {
      List<LXRendererBlending> blendingsRow = this.blendings.get(tier);
      if (index < blendingsRow.size()) {
        return blendingsRow.get(index);
      }
    }
    return null;
  }

  @Override
  public int[] blend(final int[] colorBuffer, List<LXChannel> channels) {
    int[] result = colorBuffer;

    LXChannel channel1 = null;
    LXChannel channel2 = null;

    int i = 0;
    for (LXChannel channel : channels) {
      if (channel.enabled.isOn()) {
        if (channel1 == null) channel1 = channel;
        else if (channel2 == null) channel2 = channel;
      }

      if (channel1 != null && channel2 != null) {
        LXRendererBlending blending = getBlending(0, i++);
        if (blending == null) blending = channel.getRendererBlending();

        if (blending != null) {
          result = blending.blend(channel1.getColors(), channel2.getColors(), this.lastDeltaMs);
        }

        channel1 = null;
        channel2 = null;
      }
    }

    for (int tier = 0; tier < blendings.size()-1; tier++) {
      List<LXRendererBlending> blendingRow = blendings.get(tier);

      LXRendererBlending blending1 = null;
      LXRendererBlending blending2 = null;
      i = 0;
      for (LXRendererBlending blend : blendingRow) {
        if (blending1 == null) blending1 = blend;
        else if (blending2 == null) blending2 = blend;

        if (blending1 != null && blending2 != null) {
          LXRendererBlending blending = getBlending(tier + 1, i++);

          if (blending != null) {
            result = blending.blend(blending1.getColors(), blending2.getColors(), this.lastDeltaMs);
          }

          blending1 = null;
          blending2 = null;
        }
      }
    }

    return result;
  }

}
