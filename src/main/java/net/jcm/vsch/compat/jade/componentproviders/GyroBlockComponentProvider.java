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
package net.jcm.vsch.compat.jade.componentproviders;

import net.jcm.vsch.blocks.entity.GyroBlockEntity;
import net.jcm.vsch.compat.jade.JadeCompat;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public final class GyroBlockComponentProvider implements IBlockComponentProvider {
	public static final GyroBlockComponentProvider INSTANCE = new GyroBlockComponentProvider();

	private GyroBlockComponentProvider() {}

	@Override
	public void appendTooltip(
			ITooltip tooltip,
			BlockAccessor accessor,
			IPluginConfig config
	) {
		if (!(accessor.getBlockEntity() instanceof GyroBlockEntity be)) {
			return;
		}
		tooltip.add(Component.translatable("vsch.message.strength")
			.append(String.format("%d%%", be.getPercentPower()))
		);
	}

	@Override
	public ResourceLocation getUid() {
		return JadeCompat.GYRO_BLOCK;
	}
}
