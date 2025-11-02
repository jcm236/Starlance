package net.jcm.vsch.compat.create.ponder.scenes;

import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.OverlayInstructions;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.jcm.vsch.items.VSCHItems;
import net.lointain.cosmos.init.CosmosModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ThrusterScenes {
	
	public static void thrusters(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("thrusters", "Thrusters");
		scene.configureBasePlate(1, 0, 5);
		// scene.scaleSceneView(.85f);
		scene.removeShadow();
		// scene.setSceneOffsetY(-1.5f);

		BlockPos middleThrusterLever = util.grid().at(2, 2, 2);
		BlockPos leftThrusterLever = util.grid().at(3, 2, 2);
		BlockPos rightThrusterLever = util.grid().at(1, 2, 2);

		Selection leftThruster = util.select().fromTo(3, 1, 1, 3, 2, 2);
		Selection middleThruster = util.select().fromTo(2, 1, 1, 2, 2, 2);
		Selection rightThruster = util.select().fromTo(1, 1, 1, 1, 2, 2);

		Selection baseplate = util.select().fromTo(0, 0, 0, 4, 0, 4);

		scene.world().showSection(baseplate, Direction.UP);
		ElementLink<WorldSectionElement> middleThrusterContraption = scene.world().showIndependentSection(middleThruster, Direction.DOWN);

		scene.idle(40);

		scene.overlay()
			.showText(80)
			.colored(PonderPalette.WHITE)
			.text("Thrusters need to be powered with redstone to thrust")
			.pointAt(util.vector().centerOf(middleThrusterLever))
			.attachKeyFrame()
			.placeNearTarget();

		scene.idle(80);

		scene.idle(20);

		scene.addKeyframe();

		scene.overlay()
			.showControls(util.vector().topOf(middleThrusterLever), Pointing.DOWN, 30)
			.rightClick();

		scene.idle(30);


		scene.world().moveSection(middleThrusterContraption, util.vector().of(0, 0, 20), 30);

		scene.effects().emitParticles(util.vector().of(2.5, 1.5, 3.5), scene.effects().simpleParticleEmitter(CosmosModParticleTypes.THRUSTED.get(), new Vec3(0, 0, -10)), 2, 10);

		scene.idle(60);

		ElementLink<WorldSectionElement> leftThrusterContraption = scene.world().showIndependentSection(leftThruster, Direction.DOWN);
		ElementLink<WorldSectionElement> rightThrusterContraption = scene.world().showIndependentSection(rightThruster, Direction.DOWN);

		scene.idle(40);

		scene.overlay()
			.showText(65)
			.colored(PonderPalette.WHITE)
			.text("Redstone level can control strength")
			.pointAt(util.vector().centerOf(leftThrusterLever))
			.attachKeyFrame()
			.placeNearTarget();

		scene.idle(65);

		scene.idle(20);

		scene.overlay()
			.showControls(util.vector().topOf(leftThrusterLever), Pointing.DOWN, 30)
			.rightClick();

		scene.overlay()
			.showControls(util.vector().topOf(rightThrusterLever), Pointing.DOWN, 30)
			.rightClick();

		scene.idle(20);

		scene.addKeyframe();

		scene.world().modifyBlockEntityNBT(
			util.select().position(leftThrusterLever),
			AnalogLeverBlockEntity.class,
			nbt -> nbt.putInt("State", 1)
		);

        scene.world().toggleRedstonePower(util.select().fromTo(rightThrusterLever, rightThrusterLever));


        scene.idle(10);

		scene.idle(10);

		scene.world().moveSection(leftThrusterContraption, util.vector().of(0, 0, 20), 70);
		scene.world().moveSection(rightThrusterContraption, util.vector().of(0, 0, 20), 30);

		scene.effects().emitParticles(util.vector().of(3.5, 1.5, 3.5), scene.effects().simpleParticleEmitter(CosmosModParticleTypes.THRUSTED.get(), new Vec3(0, 0, -10)), 2, 10);
		scene.effects().emitParticles(util.vector().of(1.5, 1.5, 3.5), scene.effects().simpleParticleEmitter(CosmosModParticleTypes.THRUSTED.get(), new Vec3(0, 0, -10)), 2, 10);

		scene.idle(10);

		scene.effects().emitParticles(util.vector().of(3.5, 1.5, 6.5), scene.effects().simpleParticleEmitter(CosmosModParticleTypes.THRUSTED.get(), new Vec3(0, 0, -10)), 2, 10);

		scene.idle(15);

		scene.effects().emitParticles(util.vector().of(3.5, 1.5, 9), scene.effects().simpleParticleEmitter(CosmosModParticleTypes.THRUSTED.get(), new Vec3(0, 0, -10)), 2, 10);

		scene.idle(50);

		scene.markAsFinished();
	}

	public static void modes(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("thruster_modes", "Thruster Modes");
		scene.configureBasePlate(1, 0, 5);
		scene.removeShadow();

		Selection basePlate = util.select().fromTo(0, 0, 0, 4, 0, 4);
		Selection ship = util.select().fromTo(3, 1, 1, 1, 2, 2);

		BlockPos thruster = util.grid().at(3, 1, 1);
		BlockPos lever = util.grid().at(3, 2, 2);

		scene.world().showSection(basePlate, Direction.UP);
		scene.idle(5);
		ElementLink<WorldSectionElement> shipLink = scene.world().showIndependentSection(ship, Direction.DOWN);

		scene.idle(10);

		scene.overlay().showText(60)
			.text("By default, thrusters are in POSITION mode")
			.pointAt(util.vector().centerOf(thruster))
			.attachKeyFrame();

		scene.idle(60+20);

		scene.overlay()
			.showText(60)
			.text("This means they will apply force based on where they are")
			.pointAt(util.vector().centerOf(thruster))
			.attachKeyFrame();

		scene.idle(30);

		scene.overlay().showOutline(PonderPalette.RED, 1, util.select().position(thruster), 60);

		scene.idle(5);

		drawArrow(scene.overlay(), util.vector().centerOf(thruster), util.vector().centerOf(3, 1, 5), PonderPalette.RED, 55);

		scene.idle(55+10);

		scene.world().configureCenterOfRotation(shipLink, new Vec3(1, 1, 4));
		scene.world().rotateSection(shipLink, 0, -60, 0, 20);
		scene.world().moveSection(shipLink, new Vec3(-2, 0, 9), 20);

		scene.effects().emitParticles(
			util.vector().centerOf(3, 1, 2),
			scene.effects().simpleParticleEmitter(
				CosmosModParticleTypes.THRUSTED.get(),
				new Vec3(0, 0, -10)
			),
			10,
			7
		);

		scene.idle(7);

		scene.effects().emitParticles(
			util.vector().centerOf(3, 1, 5),
			scene.effects().simpleParticleEmitter(
				CosmosModParticleTypes.THRUSTED.get(),
				new Vec3(3, 0, -7)
			),
			10,
			5
		);

		scene.idle(5);

		scene.effects().emitParticles(
			util.vector().centerOf(3, 1, 8),
			scene.effects().simpleParticleEmitter(
				CosmosModParticleTypes.THRUSTED.get(),
				new Vec3(5, 0, -5)
			),
			10,
			5
		);

		scene.idle(5+3+10);

		scene.world().hideIndependentSection(shipLink, Direction.DOWN);
		shipLink = scene.world().showIndependentSection(ship, Direction.DOWN);

		scene.idle(10);

		scene.overlay().showText(60)
			.text("However, this can cause unwanted rotation")
			.pointAt(util.vector().centerOf(2, 1, 2));

		scene.idle(60 + 20);

		scene.addKeyframe();

		scene.overlay()
			.showControls(
				util.vector().topOf(thruster),
				Pointing.DOWN, 30
			)
			.rightClick()
			.withItem(new ItemStack(VSCHItems.WRENCH.get()));

		scene.idle(30 + 10);

		scene.overlay().showText(60)
			.text("Using a wrench, a thruster can be changed to GLOBAL mode")
			.pointAt(util.vector().centerOf(thruster));

		scene.idle(60 + 20);

		scene.overlay().showText(60)
			.text("In GLOBAL mode it applies force without position")
			.pointAt(util.vector().centerOf(thruster))
			.attachKeyFrame();

		scene.idle(30);

		scene.overlay().showOutline(PonderPalette.GREEN, 1, ship.substract(util.select().layer(2)), 60);

		scene.idle(5);

		drawArrow(scene.overlay(), util.vector().centerOf(2, 1, 2), util.vector().centerOf(2, 1, 5), PonderPalette.GREEN, 55);

		scene.idle(55+10);

		scene.world().moveSection(shipLink, new Vec3(0, 0, 10), 20);

		scene.effects().emitParticles(
			util.vector().centerOf(3, 1, 2),
			scene.effects().simpleParticleEmitter(
				CosmosModParticleTypes.THRUSTED.get(),
				new Vec3(0, 0, -10)
			),
			10,
			10
		);

		scene.idle(10);

		scene.effects().emitParticles(
			util.vector().centerOf(3, 1, 7),
			scene.effects().simpleParticleEmitter(
				CosmosModParticleTypes.THRUSTED.get(),
				new Vec3(0, 0, -10)
			),
			10,
			10
		);

		scene.idle(10+10);

		scene.world().hideIndependentSection(shipLink, Direction.DOWN);
		shipLink = scene.world().showIndependentSection(ship, Direction.DOWN);

		scene.idle(10);

		scene.overlay().showText(60)
			.text("This prevents unwanted rotation")
			.pointAt(util.vector().centerOf(2,1,2))
			.attachKeyFrame();

		scene.idle(60 + 20);

		scene.markAsFinished();
	}

	private static void drawArrow(OverlayInstructions overlay, Vec3 start, Vec3 end, PonderPalette color, int duration) {
		//Draw main line
		overlay.showLine(color, start, end, duration);

		float headLength = 0.5F;

		// ----- ChatGPT math ----- //

		// Calculate direction vector
		Vector3f direction = new Vector3f(end.toVector3f()).sub(start.toVector3f()).normalize();

		// Choose a perpendicular vector
		Vector3f up = new Vector3f(0, 1, 0); // Default perpendicular vector
		if (Math.abs(direction.dot(up)) > 0.99) {
				up.set(1, 0, 0); // Fallback to another perpendicular vector if direction aligns with up
		}

		Vector3f perpendicular = new Vector3f(direction).cross(up).normalize().mul(headLength);

		// Adjust for 45-degree arrowhead angles
		Vector3f arrowLeft = new Vector3f(end.toVector3f())
			.add(new Vector3f(perpendicular).sub(direction).normalize().mul(headLength));
		Vector3f arrowRight = new Vector3f(end.toVector3f())
			.add(new Vector3f(perpendicular).negate().sub(direction).normalize().mul(headLength));

		// ---------- //

		overlay.showLine(color, end, new Vec3(arrowLeft), duration);

		// We have to go B -> A instead of A -> B because multiple lines can't share the same starting position
		// (For some VERY ANNOYING, STUPID, UNDOCUMENTED reason)
		overlay.showLine(color, new Vec3(arrowRight), end, duration);

	}
}
