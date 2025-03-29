package net.jcm.vsch.api.laser;

import java.util.function.Consumer;

public interface ILaserProcessor {
	default boolean isEndPoint() {
		return false;
	}

	int getMaxLaserStrength();

	void onLaserHit(LaserContext ctx);

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
