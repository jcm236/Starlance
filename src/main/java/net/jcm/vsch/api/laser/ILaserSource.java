package net.jcm.vsch.api.laser;

import net.jcm.vsch.api.laser.LaserContext;

import java.util.List;

public interface ILaserSource {
	/**
	 * getEmittingLasers returns a {@link List} that represents the fired/redirected lasers from this source.
	 *
	 * @return fired/redirected lasers from this source.
	 */
	List<LaserContext> getEmittingLasers();

	/**
	 * onLaserFired will be invoked by {@link LaserUtil} after a laser is fired directly or from queue
	 *
	 * @param laser The laser being fired.
	 */
	void onLaserFired(LaserContext laser);
}
