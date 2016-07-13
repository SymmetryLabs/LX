package heronarts.lx.renderer;

import heronarts.lx.LX;
import heronarts.lx.parameter.BasicParameter;
import heronarts.lx.transition.DissolveTransition;
import heronarts.lx.transition.LXTransition;
import heronarts.lx.utils.LXVar;

public class LXRendererBlending {

  private final LXVar<LXTransition> transition = new LXVar<LXTransition>();
  private final BasicParameter blendAmount = new BasicParameter("BLEND", 0);

  public LXRendererBlending(LX lx) {
    this.transition.set(new DissolveTransition(lx));
  }

  public BasicParameter getAmount() {
    return this.blendAmount;
  }

  public LXTransition getTransition() {
    return this.transition.get();
  }

  public LXVar<LXTransition> getTransitionObservable() {
    return this.transition;
  }

  public void setTransition(LXTransition transition) {
    this.transition.set(transition);
  }

  public int[] blend(int[] colorBuffer, int[] colors, double deltaMs) {
    this.transition.get().loop(deltaMs);
    this.transition.get().blend(
      colorBuffer,
      colors,
      blendAmount.getValue()
    );
    return this.transition.get().getColors();
  }

  public int[] getColors() {
    return this.transition.get().getColors();
  }

}
