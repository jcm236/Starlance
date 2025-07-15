package net.jcm.vsch;

import net.jcm.vsch.api.pipe.capability.INodePortProvider;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class VSCHCapabilities {
	private VSCHCapabilities() {}

	public static final Capability<INodePortProvider> PORT_PROVIDER = CapabilityManager.get(new CapabilityToken<>(){});
}
