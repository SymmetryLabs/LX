package heronarts.lx.renderer;

import java.util.List;

import heronarts.lx.LXChannel;

public class LeftToRightRenderer extends LXAbstractRenderer {

  @Override
  public int[] blend(int[] colorBuffer, List<LXChannel> channels) {
    for (LXChannel channel : channels) {
      if (channel.enabled.isOn()) {

        // This optimization assumed that all transitions do
        // nothing at 0 and completely take over at 1. That's
        // not always the case. Leaving this here for reference.

        // if (channel.getFader().getValue() == 0) {
        // // No blending on this channel, leave colors as they were
        // } else if (channel.getFader().getValue() >= 1) {
        // // Fully faded in, just use this channel
        // bufferColors = channel.getColors();
        // } else {

        // Apply the fader to this channel
        channel.getRendererBlending().getTransition().loop(this.lastDeltaMs);
        channel.getRendererBlending().getTransition().blend(
          colorBuffer,
          channel.getColors(),
          channel.getRendererBlending().getAmount().getValue()
        );
        colorBuffer = channel.getRendererBlending().getTransition().getColors();
      }
    }
    return colorBuffer;
  }

}
