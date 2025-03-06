package net.jcm.vsch.compat.create.ponder;

import com.simibubi.create.foundation.ponder.PonderRegistrationHelper;
import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.compat.create.ponder.scenes.DragInducerScene;
import net.jcm.vsch.compat.create.ponder.scenes.ThrusterScenes;

public class VSCHPonderRegistry {
    private static final PonderRegistrationHelper HELPER = new PonderRegistrationHelper(VSCHMod.MODID);

    public static void register() {

        HELPER.forComponents(
                        VSCHRegistrateBlocks.THRUSTER_BLOCK,
                        VSCHRegistrateBlocks.AIR_THRUSTER_BLOCK,
                        VSCHRegistrateBlocks.POWERFUL_THRUSTER_BLOCK
                )
                .addStoryBoard("thrusters", ThrusterScenes::thrusters)
                .addStoryBoard("thruster_modes", ThrusterScenes::modes);

        HELPER.forComponents(
                VSCHRegistrateBlocks.DRAG_INDUCER_BLOCK
        ).addStoryBoard("drag_inducer", DragInducerScene::inducer);


    }

}
