package net.jcm.vsch.compat.jade.componentproviders;

import net.jcm.vsch.blocks.thruster.AbstractThrusterBlockEntity;
import net.jcm.vsch.compat.jade.JadeCompat;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public final class ThrusterBlockComponentProvider implements IBlockComponentProvider {
	public static final ThrusterBlockComponentProvider INSTANCE = new ThrusterBlockComponentProvider();

	private ThrusterBlockComponentProvider() {}

	@Override
	public void appendTooltip(
			ITooltip tooltip,
			BlockAccessor accessor,
			IPluginConfig config
	) {
		if (!(accessor.getBlockEntity() instanceof AbstractThrusterBlockEntity be)) {
			return;
		}
		tooltip.add(Component.translatable("vsch.message.mode")
			.append(Component.translatable("vsch." + be.getThrusterMode().toString().toLowerCase()))
		);
	}

	@Override
	public ResourceLocation getUid() {
		return JadeCompat.THRUSTER_BLOCK;
	}
}
