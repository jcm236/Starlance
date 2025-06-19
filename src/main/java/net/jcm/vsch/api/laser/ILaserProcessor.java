package net.jcm.vsch.api.laser;

import java.util.function.Consumer;

public interface ILaserProcessor {
	/**
	 * @return {@code false} if the laser will not be transmit further. {@code true} otherwise.
	 */
	default boolean isEndPoint() {
		return false;
	}

	/**
	 * Get the maximum laser strength the processor can handle.
	 * Only has effect when {@link isEndPoint} returns {@code false}
	 *
	 * @return the maximum laser strength the processor can handle
	 */
	int getMaxLaserStrength();

	/**
	 * invoked when a laser hits this processor
	 *
	 * @param ctx The laser that hits the processor
	 *
	 * @see LaserContext#fire
	 */
	void onLaserHit(LaserContext ctx);

	/**
	 * Wrap a {@link LaserContext} consumer as an endpoint laser processor.
	 */
	static ILaserProcessor fromEndPoint(Consumer<LaserContext> processor) {
		return new EndPointProcessor(processor);
	}
}

final class EndPointProcessor implements ILaserProcessor {
	private final Consumer<LaserContext> processor;

	EndPointProcessor(final Consumer<LaserContext> processor) {
		this.processor = processor;
	}

	@Override
	public boolean isEndPoint() {
		return true;
	}

	@Override
	public int getMaxLaserStrength() {
		return 0;
	}

	@Override
	public void onLaserHit(LaserContext ctx) {
		this.processor.accept(ctx);
	}
}
