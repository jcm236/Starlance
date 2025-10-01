package net.jcm.vsch.config;

public enum ShipLandingMode {
	/**
	 * Always use the player menu for ships landing on a planet.
	 * A ship without a player nearby will not land, and may freeze in place until a player arrives
	 * and lands it.
	 */
	PLAYER_MENU(true, false),
	/**
	 * When a ship is near a planet, the saved launch position will be used for landing.
	 * If no history is present, the ship will land at the origin.
	 */
	HISTORY(false, true),
	/**
	 * When a ship is near a planet with a player, {@link #PLAYER_MENU} will be used.
	 * When a ship is near a planet and there is no player nearby, {@link #HISTORY} will be used.
	 */
	AUTO_HISTORY(true, true);

	private final boolean openMenu;
	private final boolean useHistory;

	ShipLandingMode(final boolean openMenu, final boolean useHistory) {
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
