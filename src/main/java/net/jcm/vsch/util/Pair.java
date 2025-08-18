package net.jcm.vsch.util;

public record Pair<T, U>(T left, U right) {
	public record RefInt<T>(T left, int right) {}

	public record RefLong<T>(T left, long right) {}

	public record RefFloat<T>(T left, float right) {}

	public record RefDouble<T>(T left, double right) {}
}
