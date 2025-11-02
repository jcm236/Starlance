package net.jcm.vsch.blocks.rocketassembler;

public enum AssembleResult {
	SUCCESS("success", AssembleLED.GREEN),
	WORKING("working", AssembleLED.BLACK),

	ASSEMBLING_SELF("assembling_self", AssembleLED.RED),
	CHUNK_UNLOADED("chunk_unloaded", AssembleLED.YELLOW),
	NO_BLOCK("no_block", AssembleLED.RED),
	NO_ENERGY("no_energy", AssembleLED.YELLOW),
	OTHER_ASSEMBLING("other_assembling", AssembleLED.YELLOW),
	SIZE_OVERFLOW("size_overflow", AssembleLED.RED),
	TOO_MANY_BLOCKS("too_many_blocks", AssembleLED.RED),
	UNABLE_ASSEMBLE("unable_assemble", AssembleLED.RED);

	private final String msgId;
	private final AssembleLED led;

	private AssembleResult(final String msgId, final AssembleLED led) {
		this.msgId = msgId;
		this.led = led;
	}

	public boolean isSuccess() {
		return this == SUCCESS;
	}

	public boolean isWorking() {
		return this == WORKING;
	}

	public String getMessageId() {
		return this.msgId;
	}

	public AssembleLED getLED() {
		return this.led;
	}
}
