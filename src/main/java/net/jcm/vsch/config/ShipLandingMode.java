package net.jcm.vsch.config;

public enum ShipLandingMode {
	/**
	 * Always use the player menu to make ship land on a planet.
	 * A ship without player nearby will not land and may get freezed in the space untill a player appear
	 * and lands it.
	 */
	PLAYER_MENU(true, false),
	/**
	 * When a ship is near a planet, the history launch position will be used for the ship to land.
	 * If no history is present, the ship will land at the origin.
	 */
	HISTORY(false, true),
	/**
	 * When a ship is near a planet with a player, {@link PLAYER_MENU will be used}
	 * When a ship is near a planet and there is no player nearby, {@link HISTORY will be used}
	 */
	AUTO_HISTORY(true, true);

	private final boolean openMenu;
	private final boolean useHistory;

	private ShipLandingMode(final boolean openMenu, final boolean useHistory) {
		this.openMenu	= openMenu;
		this.useHistory = useHistory;
	}

	public boolean canOpenMenu() {
		return this.openMenu;
	}

	public boolean canUseHistory() {
		return this.useHistory;
	}
}
