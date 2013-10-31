package heronarts.lx.control;

/**
 * Parameter type with a discrete set of possible integer values from [0, range-1].
 */
public class DiscreteParameter extends LXListenableParameter {

	private final int range;
	
	public DiscreteParameter(String label, int range) {
		this(label, range, 0);
	}
	
	public DiscreteParameter(String label, int range, int value) {
		super(label, value);
		this.range = range;
	}
			
	@Override
	protected double updateValue(double value) {
		return ((int)value % this.range);
	}

}
