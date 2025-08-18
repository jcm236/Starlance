package net.jcm.vsch.api.entity;

public interface ISpecialTeleportLogicEntity {
	void starlance$beforeTeleport();

	void starlance$afterTeleport(ISpecialTeleportLogicEntity old);
}
