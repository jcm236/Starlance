package net.jcm.vsch.ship.thruster;

import net.jcm.vsch.config.VSCHServerConfig;
import net.jcm.vsch.ship.IVSCHForceApplier;

import net.minecraft.core.BlockPos;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.api.world.PhysLevel;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

public class ThrusterForceApplier implements IVSCHForceApplier {
    private final ThrusterData data;

    public ThrusterData getData() {
        return this.data;
    }

    public ThrusterForceApplier(ThrusterData data){
        this.data = data;
    }

    @Override
    public void applyForces(BlockPos pos, PhysShip ship, PhysLevel physLevel) {
        // Get current thrust from thruster
        double throttle = data.throttle;
        if (throttle == 0) {
            return;
        }

        // Transform force direction from ship relative to world relative
        ShipTransform transform = ship.getTransform();
        // TODO: investigate if VS 2.5 still scaling down velocity
        Vector3d tForce = transform.getShipToWorld().transformDirection(data.dir.div(transform.getShipToWorldScaling(), new Vector3d()));
        tForce.mul(throttle);

        Vector3dc linearVelocity = ship.getVelocity();

        if (VSCHServerConfig.LIMIT_SPEED.get()) {

            int maxSpeed = VSCHServerConfig.MAX_SPEED.get().intValue();

            if (Math.abs(linearVelocity.length()) >= maxSpeed) {

                double dotProduct = tForce.dot(linearVelocity);

                if (dotProduct > 0) {

                    if (data.mode == ThrusterData.ThrusterMode.GLOBAL) {

                        applyScaledForce(ship, linearVelocity, tForce, maxSpeed);

                    } else {
                        // POSITION should be the only other value

                        Vector3d tPos = VectorConversionsMCKt.toJOMLD(pos)
                                .add(0.5, 0.5, 0.5, new Vector3d())
                                .sub(transform.getPositionInShip());


                        Vector3d parallel = new Vector3d(tPos).mul(tForce.dot(tPos) / tForce.dot(tForce));

                        Vector3d perpendicular = new Vector3d(tForce).sub(parallel);

                        // rotate the ship
                        ship.applyInvariantForceToPos(perpendicular, tPos);

                        // apply global force, since the force is perfectly lined up with the centre of gravity
                        applyScaledForce(ship, linearVelocity, parallel, maxSpeed);

                    }
                    return;
                }
            }
        }

        // Switch between applying force at position and just applying the force
        if (data.mode == ThrusterData.ThrusterMode.POSITION) {
            Vector3d tPos = VectorConversionsMCKt.toJOMLD(pos)
                    .add(0.5, 0.5, 0.5, new Vector3d())
                    .sub(transform.getPositionInShip());

            ship.applyInvariantForceToPos(tForce, tPos);

            //ThrusterData.ThrusterMode.GLOBAL should be the only other value:
        } else {
            // Apply the force at no specific position
            ship.applyInvariantForce(tForce);
        }
    }

    private static void applyScaledForce(PhysShip ship, Vector3dc linearVelocity, Vector3d tForce, int maxSpeed) {
        assert ValkyrienSkiesMod.getCurrentServer() != null;
        double deltaTime = 1.0 / (20 * 3);
        double mass = ship.getMass();

        //Invert the parallel projection of tForce onto linearVelocity and scales it so that the resulting speed is exactly
        // equal to length of linearVelocity, but still in the direction the ship would have been going without the speed limit
        Vector3d targetVelocity = (new Vector3d(linearVelocity).add(new Vector3d(tForce).mul(deltaTime / mass)).normalize(maxSpeed)).sub(linearVelocity);

        // Apply the force at no specific position
        ship.applyInvariantForce(targetVelocity.mul(mass / deltaTime));
    }
}
