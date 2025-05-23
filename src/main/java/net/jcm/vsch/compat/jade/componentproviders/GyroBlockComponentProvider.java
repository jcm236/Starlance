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
			.append(String.format(" %d%%", be.getPercentPower() * 10))
		);
	}

	@Override
	public ResourceLocation getUid() {
		return JadeCompat.GYRO_BLOCK;
	}
}
