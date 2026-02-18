/**
 * Copyright (C) 2025  the authors of Starlance
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 **/
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
