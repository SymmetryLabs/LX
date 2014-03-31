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

package heronarts.lx.parameter;

/**
 * A FixedParameter is an immutable parameter. It will throw a RuntimeException
 * if setValue() is attempted. Useful for anonymous placeholder values in places
 * that expect to use LXParameters.
 */
public class FixedParameter implements LXParameter {

  private final double value;

  public FixedParameter(double value) {
    this.value = value;
  }

  @Override
  public LXParameter reset() {
    return this;
  }

  @Override
  public LXParameter setValue(double value) {
    throw new RuntimeException("Cannot invoke setValue on a FixedParameter");
  }

  @Override
  public double getValue() {
    return this.value;
  }

  @Override
  public float getValuef() {
    return (float) this.value;
  }

  @Override
  public String getLabel() {
    return null;
  }

}
