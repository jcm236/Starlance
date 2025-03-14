package net.jcm.vsch.compat.create.ponder.scenes;

import com.simibubi.create.foundation.ponder.*;
import com.simibubi.create.foundation.ponder.element.InputWindowElement;
import com.simibubi.create.foundation.ponder.element.WorldSectionElement;
import com.simibubi.create.foundation.ponder.instruction.EmitParticlesInstruction;
import com.simibubi.create.foundation.utility.Pointing;
import net.lointain.cosmos.init.CosmosModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class DragInducerScene {
	public static void inducer(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("drag_inducer", "Drag inducer");
		scene.configureBasePlate(0, 0, 5);
		//scene.showBasePlate();
		scene.removeShadow();

		BlockPos button = util.grid.at(2, 2, 2);
		BlockPos lever = util.grid.at(3, 2, 2);
		BlockPos drag_inducer = util.grid.at(3, 1, 2);

		Selection rocketNoInducer = util.select.fromTo(1, 1, 2, 2, 2, 2);
		Selection rocketWithInducer = util.select.fromTo(1, 1, 2, 3, 2, 2);
		Selection baseplateSelect = util.select.fromTo(0, 0, 0, 4, 0, 4);

		ElementLink<WorldSectionElement> rocket = scene.world.showIndependentSection(rocketNoInducer, Direction.DOWN);
		ElementLink<WorldSectionElement> baseplate = scene.world.showIndependentSection(baseplateSelect, Direction.UP);

		scene.idle(20);

		scene.overlay
			.showText(70)
			.colored(PonderPalette.WHITE)
			.text("Sometimes, it can be hard to slow down in space")
			.pointAt(util.vector.centerOf(util.grid.at(2, 1, 2)));

		scene.idle(70);

		scene.overlay.showControls(new InputWindowElement(util.vector.topOf(button), Pointing.DOWN)
						.rightClick()
				, 30);

		scene.idle(20);

		scene.world.toggleRedstonePower(util.select.fromTo(button, button));

		scene.idle(10);

		scene.world.toggleRedstonePower(util.select.fromTo(button, button));

		scene.world.moveSection(baseplate, util.vector.of(-20, 0, 0), 50);
		scene.effects.emitParticles(util.vector.of(1.5, 1.5, 2.5), EmitParticlesInstruction.Emitter.simple(CosmosModParticleTypes.THRUSTED.get(), new Vec3(-10, 0, 0)), 2, 10);

		scene.idle(50);

		scene.world.hideIndependentSection(rocket, Direction.UP, 0);

		scene.world.moveSection(baseplate, util.vector.of(20, 0, 0), 0);

		rocket = scene.world.showIndependentSection(rocketWithInducer, Direction.DOWN);

		scene.idle(10);

		scene.overlay
			.showText(70)
			.colored(PonderPalette.WHITE)
			.text("This is where the drag inducer comes in")
			.pointAt(util.vector.centerOf(drag_inducer))
			.placeNearTarget()
			.attachKeyFrame();

		scene.idle(70);

		scene.idle(10);

		scene.overlay.showControls(new InputWindowElement(util.vector.topOf(lever), Pointing.DOWN)
				.rightClick()
			, 30);

		scene.idle(20);

		scene.world.toggleRedstonePower(util.select.fromTo(lever, lever));

		scene.overlay.showControls(
			new InputWindowElement(util.vector.topOf(button), Pointing.DOWN)
				.rightClick(),
			30
		);

		scene.idle(20);

		scene.world.toggleRedstonePower(util.select.fromTo(button, button));

		scene.idle(10);

		scene.world.toggleRedstonePower(util.select.fromTo(button, button));

		scene.effects.emitParticles(util.vector.of(1.5, 1.5, 2.5), EmitParticlesInstruction.Emitter.simple(CosmosModParticleTypes.THRUSTED.get(), new Vec3(-10, 0, 0)), 2, 10);


		for (int i = 0; i < 20; i++) {
			scene.world.moveSection(baseplate, util.vector.of(-0.5, 0, 0), i / 2);
			scene.idle(i / 2);
		}

		scene.idle(20);

		scene.world.hideIndependentSection(rocket, Direction.UP, 0);

		scene.world.moveSection(baseplate, util.vector.of(10, 0, 0), 0);

		rocket = scene.world.showIndependentSection(rocketWithInducer, Direction.DOWN);

		scene.idle(20);

		scene.overlay
			.showText(80)
			.colored(PonderPalette.WHITE)
			.text("When powered with redstone, the drag inducer will try to slow you down in all directions")
			.pointAt(util.vector.centerOf(drag_inducer))
			.placeNearTarget()
			.attachKeyFrame();

		scene.idle(80);

		scene.idle(10);

		scene.markAsFinished();
	}
}
