package net.jcm.vsch.event;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.jcm.vsch.ship.VSCHForceInducedShips;
import net.minecraft.util.Mth;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.text.NumberFormat;
import java.util.Map;

public class PhysTick {

    public static final double PERMEABILITY = 0.05d;
    public static final double MAX_FORCE = 100000000.0d;
    public static final double MAX_LENGTH = 100.0d;


    public static void apply(Map<String, Long2ObjectMap<PhysShipImpl>> dimensionToShipIdToPhysShip) {
        double deltaTime = 1.0 / (VSGameUtilsKt.getVsPipeline(ValkyrienSkiesMod.getCurrentServer()).computePhysTps());

        dimensionToShipIdToPhysShip.forEach((dim, idToPhysShip) -> {

            QueryableShipData<ServerShip> allShips = VSGameUtilsKt.getShipObjectWorld(ValkyrienSkiesMod.getCurrentServer()).getAllShips();

            idToPhysShip.forEach((id1, physShip1) -> {
                ServerShip ship1 = allShips.getById(id1);

                if (ship1 == null) return;

                VSCHForceInducedShips attach1 = ship1.getAttachment(VSCHForceInducedShips.class);

                if (attach1 == null) return;

                idToPhysShip.forEach((id2, physShip2) -> {

                    if (physShip1.getPoseVel().getPos().sub(physShip2.getPoseVel().getPos(), new Vector3d()).lengthSquared() > MAX_LENGTH) return;

                    ServerShip ship2 = allShips.getById(id2);

                    if (ship2 == null || id1.equals(id2)) return;

                    VSCHForceInducedShips attach2 = ship2.getAttachment(VSCHForceInducedShips.class);

                    if (attach2 == null) return;

                    attach1.magnets.forEach((bPos1, data1) -> {

                        Vector3d acc1 = new Vector3d();
                        Vector3d acc2 = new Vector3d();

                        Vector3d shipPos1 = VectorConversionsMCKt.toJOMLD(bPos1).add(.5,.5,.5);

                        Vector3d shipPos1N = shipPos1.fma(0.45, data1.direction, new Vector3d());
                        Vector3d shipPos1S = shipPos1.fma(-0.45, data1.direction, new Vector3d());

                        Vector3d pos1N = physShip1.getTransform().getShipToWorld().transformPosition(shipPos1N, new Vector3d());
                        Vector3d pos1S = physShip1.getTransform().getShipToWorld().transformPosition(shipPos1S, new Vector3d());

                        attach2.magnets.forEach((bPos2, data2) -> {


                            Vector3d shipPos2 = VectorConversionsMCKt.toJOMLD(bPos2).add(.5,.5,.5);


                            Vector3d pos2N = shipPos2.fma(0.45, data2.direction, new Vector3d());
                            Vector3d pos2S = shipPos2.fma(-0.45, data2.direction, new Vector3d());

                            double totalForce = data1.force * data2.force * 10000;

                            physShip2.getTransform().getShipToWorld().transformPosition(pos2N);
                            physShip2.getTransform().getShipToWorld().transformPosition(pos2S);

                            acc1.add(calculateGilbertForce(pos1N, data1.force, pos2N, data2.force, true).mul(totalForce));
                            acc1.add(calculateGilbertForce(pos1N, data1.force, pos2S, data2.force, false).mul(totalForce));

                            acc2.add(calculateGilbertForce(pos1S, data1.force, pos2N, data2.force, false).mul(totalForce));
                            acc2.add(calculateGilbertForce(pos1S, data1.force, pos2S, data2.force, true).mul(totalForce));


                        });

                        if (acc1.lengthSquared() > MAX_FORCE * MAX_FORCE) acc1.normalize(MAX_FORCE);
                        if (acc2.lengthSquared() > MAX_FORCE * MAX_FORCE) acc2.normalize(MAX_FORCE);

                        physShip1.applyInvariantForceToPos(acc1, shipPos1N.sub(physShip1.getTransform().getPositionInShip(), new Vector3d()));
                        physShip1.applyInvariantForceToPos(acc2, shipPos1S.sub(physShip1.getTransform().getPositionInShip(), new Vector3d()));

                    });
                });
            });
        });
    }

    private static Vector3d calculateGilbertForce(Vector3d pos1, double force1, Vector3d pos2, double force2, boolean samePole) {
        Vector3d r = pos2.sub(pos1, new Vector3d());
        double dist = r.length();
        double part0 = PERMEABILITY * force1 * force2;
        double part1 = 4 * Math.PI * dist;

        double f = (part0 / part1);

        if (samePole)
            f = -f;

        r.normalize(f);

        System.out.println(r.toString(NumberFormat.getInstance()));

        return r;
    }
}
