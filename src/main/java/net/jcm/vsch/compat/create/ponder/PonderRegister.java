package net.jcm.vsch.compat.create.ponder;

import net.createmod.ponder.foundation.PonderIndex;

public class PonderRegister {
    public static void add() {
        PonderIndex.addPlugin(new VSCHPonderPlugin());
    }
}
