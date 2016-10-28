package heronarts.lx;

import java.util.Arrays;
import java.util.List;

public class LXChannelGroup extends LXComponent {

  private final LX lx;

  private final List<LXChannel> channels;

  public LXChannelGroup(LX lx, LXChannel... channels) {
    this(lx, Arrays.asList(channels));
  }

  public LXChannelGroup(LX lx, List<LXChannel> channels) {
    super(lx);
    this.lx = lx;
    this.channels = channels;
  }

  public List<LXChannel> getChannels() {
    return this.channels;
  }

}
