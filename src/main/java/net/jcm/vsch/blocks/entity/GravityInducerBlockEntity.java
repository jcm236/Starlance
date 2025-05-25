package net.jcm.vsch.blocks.entity;

import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;
import net.jcm.vsch.config.VSCHConfig;
import net.jcm.vsch.items.VSCHItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.List;

public class GravityInducerBlockEntity extends BlockEntity implements ParticleBlockEntity {
	private static final double MIN_FORCE = 0.01;
	private static final ItemStack MAGNET_BOOTS = new ItemStack(VSCHItems.MAGNET_BOOT.get());
	public GravityInducerBlockEntity(BlockPos pos, BlockState blockState) {
		super(VSCHBlockEntities.GRAVITY_INDUCER_BLOCK_ENTITY.get(), pos, blockState);
	}

	public double getAttractDistance() {
		return VSCHConfig.GRAVITY_DISTANCE.get().doubleValue();
	}

	public double getMaxForce() {
		return VSCHConfig.GRAVITY_MAX_FORCE.get().doubleValue();
	}


	@Override
	public void tickForce(ServerLevel level, BlockPos pos, BlockState state) {
		if(level.getBestNeighborSignal(pos) == 0) {
			return;
		}

		LoadedServerShip ship = VSGameUtilsKt.getShipObjectManagingPos(level, pos);
		if (ship == null) {
			return;
		}

		List<Entity> entities = level.getEntities(null, VectorConversionsMCKt.toMinecraft(ship.getWorldAABB()));
		for (Entity entity : entities) {
			if (entity.noPhysics) {
				continue;
			}

			if (entity instanceof ServerPlayer) {
				continue;
			}
			
			calculateAndApplyGravity(entity,level,getAttractDistance(),getMaxForce());
		}
	}

	@Override
	public void tickParticles(Level level, BlockPos pos, BlockState state) {
		ClientShip ship = VSGameUtilsKt.getShipObjectManagingPos((ClientLevel) level, pos);
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;

		if(level.getBestNeighborSignal(pos) == 0){
			return;
		}

		if((ship == null) || player == null){
			return;
		}

		if(player.getAbilities().flying){
			return;
		}
		if(player.getInventory().armor.get(0).is(VSCHItems.MAGNET_BOOT.get())){
			return;
		}

		AABB shipAABB = VectorConversionsMCKt.toMinecraft(ship.getWorldAABB());
		Vec3 playerPos = player.position();

		if(!(shipAABB.contains(playerPos))){
			return;
		}

		calculateAndApplyGravity(player,level,getAttractDistance(),getMaxForce());
	}

	public static void calculateAndApplyGravity(Entity entity, Level level, double maxDistance, double maxForce){
//		double maxDistance = getAttractDistance();

		Vec3 direction = new Vec3(0, -1, 0); // TODO: maybe we can change the direction to match the ship that player stands on?
		Vec3 startPos = entity.position(); // Starting position (entity's position)
		Vec3 endPos = startPos.add(direction.scale(maxDistance));

		HitResult hitResult = level.clip(new ClipContext(
				startPos,
				endPos,
				ClipContext.Block.COLLIDER, // Raycast considers block collision shapes, maybe we don't want this?
				ClipContext.Fluid.NONE,     // Ignore fluids
				entity
		));

		if(hitResult.getType() != HitResult.Type.BLOCK){
			return;
		}

		double distance = startPos.distanceToSqr(hitResult.getLocation());
		double scaledForce = Math.min(maxDistance * maxDistance / distance * MIN_FORCE, maxForce);

		Vec3 force = direction.scale(scaledForce);
		System.out.println(entity);
		System.out.println(force);
		entity.push(force.x, force.y, force.z);
	}
}
