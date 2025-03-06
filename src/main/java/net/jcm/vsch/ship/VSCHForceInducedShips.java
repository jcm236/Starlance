package net.jcm.vsch.ship;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Pair;
import net.jcm.vsch.util.LevelBlockPos;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.*;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import net.jcm.vsch.config.VSCHConfig;
import net.jcm.vsch.util.VSCHUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

@SuppressWarnings("deprecation")
public class VSCHForceInducedShips implements ShipForcesInducer {

	/**
	 * Don't mess with this unless you know what your doing. I'm making it public for all the people that do know what their doing.
	 * Instead, look at {@link #addThruster(BlockPos, ThrusterData)} or {@link #removeThruster(BlockPos)} or {@link #getThrusterAtPos(BlockPos)}
	 */
	public Map<BlockPos, ThrusterData> thrusters = new ConcurrentHashMap<>();

	/**
	 * Don't mess with this unless you know what your doing. I'm making it public for all the people that do know what their doing.
	 * Instead, look at {@link #addDragger(BlockPos, DraggerData)} or {@link #removeDragger(BlockPos)} or {@link #getDraggerAtPos(BlockPos)}
	 */
	public Map<BlockPos, DraggerData> draggers = new ConcurrentHashMap<>();

	public Map<BlockPos, MagnetData> magnets = new ConcurrentHashMap<>();
	public static Map<LevelBlockPos, MagnetData> worldMagnets = new ConcurrentHashMap<>();

	private String dimensionId = null;

	public VSCHForceInducedShips() {}

	public static final float PERMEABILITY = 0.05f;
	public static final float MAX_FORCE = 10000.0f;

	public VSCHForceInducedShips(String dimensionId) {
		this.dimensionId = dimensionId;
	}

	@Override
	public void applyForces(@NotNull PhysShip physicShip) {
		PhysShipImpl physShip = (PhysShipImpl) physicShip;


		double deltaTime = 1.0 / (VSGameUtilsKt.getVsPipeline(ValkyrienSkiesMod.getCurrentServer()).computePhysTps());

//		((ShipObjectServer)VSGameUtilsKt.getShipObjectWorld(ValkyrienSkiesMod.getCurrentServer()).getAllShips().getById(physShip.getId())).
		// Apply thrusters force
		thrusters.forEach((pos, data) -> {
			// Get current thrust from thruster
			float throttle = data.throttle;
			if (throttle == 0.0f) {
				return;
			}

			// Transform force direction from ship relative to world relative
			Vector3d tForce = physShip.getTransform().getShipToWorld().transformDirection(data.dir, new Vector3d());
			tForce.mul(throttle);


			Vector3dc linearVelocity = physShip.getPoseVel().getVel();

			if (VSCHConfig.LIMIT_SPEED.get()) {

				int maxSpeed = VSCHConfig.MAX_SPEED.get().intValue();

				if (Math.abs(linearVelocity.length()) >= maxSpeed) {

					double dotProduct = tForce.dot(linearVelocity);

					if (dotProduct > 0) {

						if (data.mode == ThrusterData.ThrusterMode.GLOBAL) {

							applyScaledForce(physShip, linearVelocity, tForce, maxSpeed);

						} else {
							// POSITION should be the only other value

							Vector3d tPos = VectorConversionsMCKt.toJOMLD(pos)
									.add(0.5, 0.5, 0.5, new Vector3d())
									.sub(physShip.getTransform().getPositionInShip());


							Vector3d parallel = new Vector3d(tPos).mul(tForce.dot(tPos) / tForce.dot(tForce));

							Vector3d perpendicular = new Vector3d(tForce).sub(parallel);

							// rotate the ship
							physShip.applyInvariantForceToPos(perpendicular, tPos);

							// apply global force, since the force is perfectly lined up with the centre of gravity
							applyScaledForce(physShip, linearVelocity, parallel, maxSpeed);

						}
						return;
					}
				}
			}

			// Switch between applying force at position and just applying the force
			if (data.mode == ThrusterData.ThrusterMode.POSITION) {
				Vector3d tPos = VectorConversionsMCKt.toJOMLD(pos)
						.add(0.5, 0.5, 0.5, new Vector3d())
						.sub(physShip.getTransform().getPositionInShip());

				physShip.applyInvariantForceToPos(tForce, tPos);

				//ThrusterData.ThrusterMode.GLOBAL should be the only other value:
			} else {
				// Apply the force at no specific position
				physShip.applyInvariantForce(tForce);
			}
		});

		// Apply draggers force
		draggers.forEach((pos, data) -> {
			Vector3dc linearVelocity = physShip.getPoseVel().getVel();
			Vector3dc angularVelocity = physShip.getPoseVel().getOmega();

			if (!data.on) {
				return;
			}


			Vector3d acceleration = linearVelocity.negate(new Vector3d());
			Vector3d force = acceleration.mul(physShip.getInertia().getShipMass());

			force = VSCHUtils.clampVector(force, VSCHConfig.MAX_DRAG.get().intValue());

			Vector3d rotAcceleration = angularVelocity.negate(new Vector3d());
			Vector3d rotForce = rotAcceleration.mul(physShip.getInertia().getShipMass());

			rotForce = VSCHUtils.clampVector(rotForce, VSCHConfig.MAX_DRAG.get().intValue());

			physShip.applyInvariantForce(force);
			physShip.applyInvariantTorque(rotForce);
		});


		QueryableShipData<ServerShip> allShips = VSGameUtilsKt.getShipObjectWorld(ValkyrienSkiesMod.getCurrentServer()).getAllShips();
		ServerShip shipOn = allShips.getById(physShip.getId());

		if (shipOn != null) {
//			Map<Vector3d, Pair<Vector3d, Double>> allMagnets = new HashMap<>();
//
//			new HashMap<>(worldMagnets).forEach((levelPos, data) -> {
//				if (Objects.equals(levelPos.level, shipOn.getChunkClaimDimension())) allMagnets.put(VectorConversionsMCKt.toJOMLD(levelPos), new Pair<>(data.direction, data.force));
//			});
//
//			allShips.getIntersecting(VectorConversionsMCKt.toJOML(VectorConversionsMCKt.toMinecraft(shipOn.getWorldAABB()).inflate(25.0))).forEach(ship -> {
//				if (!ship.getChunkClaimDimension().equals(shipOn.getChunkClaimDimension()) || ship == shipOn) return;
//				new HashMap<>(getOrCreate(ship).magnets).forEach((pos, data) -> {
//					allMagnets.put(ship.getTransform().getShipToWorld().transformPosition(VectorConversionsMCKt.toJOMLD(pos).add(.5,.5,.5)), new Pair<>(ship.getTransform().getShipToWorldRotation().transform(data.direction, new Vector3d()), data.force));
//				});
//			});
//
//
//			new HashMap<>(magnets).forEach((bPos, data) -> {
//				Vector3d pos0 = physShip.getTransform().getShipToWorld().transformPosition(VectorConversionsMCKt.toJOMLD(bPos).add(.5,.5,.5), new Vector3d());
//
//				Vector3d dir0 = physShip.getTransform().getShipToWorldRotation().transform(data.direction, new Vector3d());
//
//				double force0 = data.force;
//
//				Vector3d accF1 = new Vector3d();
//				Vector3d accF2 = new Vector3d();
//				System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//				allMagnets.forEach((pos1, pair) -> {
//					if (pos0.equals(pos1)) return;
//					Vector3d force1 = calculateGilbertForce(pos0.fma(0.5, dir0), dir0, force0, pos1.fma(0.5, pair.getFirst()), pair.getFirst(), pair.getSecond()).negate();
//					Vector3d force2 = calculateGilbertForce(pos0.fma(-0.5, dir0), dir0, force0, pos1.fma(0.5, pair.getFirst()), pair.getFirst(), pair.getSecond());
//					Vector3d force3 = calculateGilbertForce(pos0.fma(0.5, dir0), dir0, force0, pos1.fma(-0.5, pair.getFirst()), pair.getFirst(), pair.getSecond());
//					Vector3d force4 = calculateGilbertForce(pos0.fma(-0.5, dir0), dir0, force0, pos1.fma(-0.5, pair.getFirst()), pair.getFirst(), pair.getSecond()).negate();
//
//					double force = force0 * pair.getSecond();
//
//					accF1.add(force1.mul(force));
//					accF2.add(force2.mul(force));
//					accF1.add(force3.mul(force));
//					accF2.add(force4.mul(force));
//
//					System.out.println();
//					System.out.println(accF1.toString(NumberFormat.getInstance()));
//					System.out.println(accF2.toString(NumberFormat.getInstance()));
//					System.out.println();
//				});
//
//				Vector3d posShip0 = VectorConversionsMCKt.toJOMLD(bPos).add(.5,.5,.5).fma(0.5, data.direction).sub(physShip.getTransform().getPositionInShip());
//				Vector3d posShip1 = VectorConversionsMCKt.toJOMLD(bPos).add(-.5,.5,.5).fma(0.5, data.direction).sub(physShip.getTransform().getPositionInShip());
//
//				if (accF1.lengthSquared() > MAX_FORCE * MAX_FORCE) accF1.normalize(MAX_FORCE);
//				if (accF2.lengthSquared() > MAX_FORCE * MAX_FORCE) accF1.normalize(MAX_FORCE);
//
////				System.out.println();
////				System.out.println(accF1.toString(NumberFormat.getInstance()));
////				System.out.println(accF2.toString(NumberFormat.getInstance()));
////				System.out.println(posShip0.toString(NumberFormat.getInstance()));
////				System.out.println(posShip1.toString(NumberFormat.getInstance()));
//
//				physShip.applyInvariantForceToPos(accF1, posShip0);
//				physShip.applyInvariantForceToPos(accF2, posShip1);
//
//			});
		}
	}


	private Vector3d calculateGilbertForce(Vector3d m1, Vector3d dir1, double force1, Vector3d m2, Vector3d dir2, double force2) {

		Vector3d r = m2.sub(m1, new Vector3d());
		double dist = r.length();
		double part0 = PERMEABILITY * force1 * force2;
		double part1 = 4 * Math.PI * dist;

		double f = (part0 / part1);
		System.out.println();
		System.out.println(f);
		System.out.println();

		return r.normalize(new Vector3d()).mul(f);
	}

	private static void applyScaledForce(PhysShipImpl physShip, Vector3dc linearVelocity, Vector3d tForce, int maxSpeed) {
		assert ValkyrienSkiesMod.getCurrentServer() != null;
		double deltaTime = 1.0 / (VSGameUtilsKt.getVsPipeline(ValkyrienSkiesMod.getCurrentServer()).computePhysTps());
		double mass = physShip.getInertia().getShipMass();

		//Invert the parallel projection of tForce onto linearVelocity and scales it so that the resulting speed is exactly
		// equal to length of linearVelocity, but still in the direction the ship would have been going without the speed limit
		Vector3d targetVelocity = (new Vector3d(linearVelocity).add(new Vector3d(tForce).mul(deltaTime / mass)).normalize(maxSpeed)).sub(linearVelocity);

		// Apply the force at no specific position
		physShip.applyInvariantForce(targetVelocity.mul(mass / deltaTime));
	}

	// ----- Thrusters ----- //

	public void addThruster(BlockPos pos, ThrusterData data) {
		thrusters.put(pos, data);
	}


	public void removeThruster(BlockPos pos) {
		thrusters.remove(pos);
	}

	@Nullable
	public ThrusterData getThrusterAtPos(BlockPos pos) {
		return thrusters.get(pos);
	}

	// ----- Draggers ----- //

	public void addDragger(BlockPos pos, DraggerData data) {
		draggers.put(pos, data);
	}

	public void removeDragger(BlockPos pos) {
		draggers.remove(pos);
	}

	public void addMagnet(BlockPos pos, MagnetData data) {
		magnets.put(pos, data);
	}

	public void removeMagnet(BlockPos pos) {
		magnets.remove(pos);
	}

	@Nullable
	public DraggerData getDraggerAtPos(BlockPos pos) {
		return draggers.get(pos);
	}

	// ----- Force induced ships ----- //

	public static VSCHForceInducedShips getOrCreate(ServerShip ship, String dimensionId) {
		VSCHForceInducedShips attachment = ship.getAttachment(VSCHForceInducedShips.class);
		if (attachment == null) {
			attachment = new VSCHForceInducedShips(dimensionId);
			ship.saveAttachment(VSCHForceInducedShips.class, attachment);
		}
		return attachment;
	}

	public static VSCHForceInducedShips getOrCreate(ServerShip ship) {
		return getOrCreate(ship, ship.getChunkClaimDimension());
	}

	public static VSCHForceInducedShips get(Level level, BlockPos pos) {
		ServerLevel serverLevel = (ServerLevel) level;
		// Don't ask, I don't know
		ServerShip ship = VSGameUtilsKt.getShipObjectManagingPos(serverLevel, pos);
		if (ship == null) {
			ship = VSGameUtilsKt.getShipManagingPos(serverLevel, pos);
		}
		// Seems counter-intutive at first. But basically, it returns null if it wasn't a ship. Otherwise, it gets the attachment OR creates and then gets it
		return ship != null ? getOrCreate(ship) : null;
	}
}
