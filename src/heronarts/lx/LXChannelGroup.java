package heronarts.lx;

import java.util.Arrays;
import java.util.List;

public class LXChannelGroup {

  private final List<LXChannel> channels;

  public LXChannelGroup(LXChannel... channels) {
    this(Arrays.asList(channels));
  }

  public LXChannelGroup(List<LXChannel> channels) {
    this.channels = channels;
  }

  public List<LXChannel> getChannels() {
    return this.channels;
  }

}
