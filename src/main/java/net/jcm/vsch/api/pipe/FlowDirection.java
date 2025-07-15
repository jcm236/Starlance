package net.jcm.vsch.api.pipe;

public enum FlowDirection {
	NONE(false, false),
	BOTH(true, true),
	IN(true, false),
	OUT(false, true);

	private final boolean in;
	private final boolean out;

	private FlowDirection(final boolean in, final boolean out) {
		this.in = in;
		this.out = out;
	}

	public boolean canFlowIn() {
		return this.in;
	}

	public boolean canFlowOut() {
		return this.out;
	}
}
