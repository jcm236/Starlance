package net.jcm.vsch.api.pipe.capability;

import net.jcm.vsch.api.pipe.FlowDirection;

public interface NodePort {
	/**
	 * @return pressure on the port, cannot be less than {@code 0}
	 */
	double getPressure();

	/**
	 * @return {@link FlowDirection} relative to the block 
	 */
	FlowDirection getFlowDirection();
}
