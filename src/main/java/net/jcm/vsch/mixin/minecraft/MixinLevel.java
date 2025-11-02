package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.ILevelAccessor;
import net.jcm.vsch.util.wapi.LevelData;

import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Level.class)
public abstract class MixinLevel implements ILevelAccessor {
	@Unique
	private LevelData levelData = null;

	@Override
	public LevelData starlance$getLevelData() {
		if (this.levelData == null) {
			this.levelData = LevelData.get0((Level)((Object)(this)));
		}
		return this.levelData;
	}

	@Override
	public void starlance$clearLevelData() {
		this.levelData = null;
	}
}
