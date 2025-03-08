package net.jcm.vsch.api.laser;

import net.jcm.vsch.api.laser.LaserContext;

import java.util.List;

public interface ILaserSyncedSource {
	/**
	 * getEmittingLasers returns a {@link List} that represents directly emitted lasers from this source.
	 *
	 * @return fired/redirected lasers from this source.
	 */
	List<LaserContext> getEmittingLasers();

	/**
	 * onLaserFired will be invoked by {@link LaserUtil} after a laser is fired directly or from queue.
	 * The source then can render the laser in client.
	 *
	 * @param laser The laser which just processed.
	 *
	 * @see LaserUtil.queueLaser
	 * @see LaserUtil.fireLaser
	 * @see LaserUtil.fireRedirectedLaser
	 */
	void onLaserFired(LaserContext laser);
}
