package net.jcm.vsch.compat.create.ponder.scenes;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.lointain.cosmos.init.CosmosModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class RocketAssemblerScene {
	public static void inducer(SceneBuilder scene, SceneBuildingUtil util) {
        // Setup
		scene.title("rocket_assembler", "Rocket Assembler");
		scene.configureBasePlate(0, 0, 5);
		scene.removeShadow();

        // Reference
		BlockPos button = util.grid().at(2, 3, 3);
		BlockPos lever = util.grid().at(2, 2, 1);

		BlockPos assem_1 = util.grid().at(2, 2, 3);
        BlockPos assem_2 = util.grid().at(2, 1, 1);

        BlockPos errant_block = util.grid().at(2, 1, 3);

        Selection rocket = util.select().fromTo(3, 1, 2, 1, 3, 2).add(util.select().position(lever));
        Selection tower = util.select().fromTo(2, 1, 4, 2, 2, 4);

        // Start scene
        scene.showBasePlate();

        scene.idle(20);

        scene.world().showSection(tower, Direction.DOWN);
        scene.world().showSection(util.select().position(assem_1), Direction.DOWN);
        scene.world().showSection(util.select().position(button), Direction.DOWN);

        scene.idle(20);

        ElementLink<WorldSectionElement> rocketElement = scene.world().showIndependentSection(rocket, Direction.DOWN);

        scene.idle(20);

        scene.overlay()
                .showText(65)
                .colored(PonderPalette.WHITE)
                .text("Rocket Assemblers can give a section of the world physics")
                .pointAt(util.vector().blockSurface(assem_1, Direction.WEST))
                .attachKeyFrame();
        scene.idle(65+20);

        scene.overlay()
                .showText(50)
                .colored(PonderPalette.WHITE)
                .text("When powered by redstone, it will look for blocks on its green face")
                .pointAt(util.vector().blockSurface(button, Direction.DOWN))
                .attachKeyFrame();
        scene.idle(50+10);

        scene.overlay().showOutline(PonderPalette.GREEN, "select_1", util.select().position(assem_1.north()), 10);
        scene.idle(10);

        scene.overlay().showOutline(PonderPalette.GREEN, "select_2", rocket.copy().substract(util.select().position(lever)), 10);
        scene.idle(10);

        scene.overlay().showOutline(PonderPalette.GREEN, "select_3", util.select().fromTo(1, 1, 1, 3, 3, 2), 20);
        scene.idle(25);

        scene.overlay()
                .showText(40)
                .colored(PonderPalette.WHITE)
                .text("These blocks will be assembled into a 'ship'")
                .pointAt(util.vector().centerOf(2, 2, 2))
                .attachKeyFrame();
        scene.idle(40+10);

        scene.overlay()
                .showText(30)
                .colored(PonderPalette.WHITE)
                .text("And will have physics")
                .pointAt(util.vector().centerOf(2, 2, 2));
        scene.idle(30+10);

        scene.overlay()
                .showControls(util.vector().blockSurface(button, Direction.DOWN), Pointing.DOWN, 20)
                .rightClick();
        scene.idle(10);
        scene.world().toggleRedstonePower(util.select().position(button));
        scene.idle(10);
        scene.overlay().showOutline(PonderPalette.GREEN, "select_4", util.select().fromTo(1, 1, 1, 3, 3, 2), 10);
        scene.idle(10);
        scene.world().toggleRedstonePower(util.select().position(button));
        scene.idle(30);

        scene.overlay()
                .showControls(util.vector().topOf(lever), Pointing.DOWN, 10)
                .rightClick();

        scene.world().toggleRedstonePower(util.select().position(lever));
        scene.idle(15);

        scene.world().moveSection(rocketElement, new Vec3(0, 10, 0), 20);
        scene.idle(20);

        scene.markAsFinished();
	}
}
