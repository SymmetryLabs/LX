package heronarts.lx.parameter;

/**
 * A listener interface to be notified of changes to the parameter value
 * before and after they happen.
 */
public interface LXParameterListenerExtended extends LXParameterListener {

  /**
   * Invoked when the value of a parameter is about to change.
   *
   * @param parameter The parameter that will change its value
   */
  public void onParameterWillChange(LXParameter parameter);

}
