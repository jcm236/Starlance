package net.jcm.vsch.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.Objects;

public class LevelBlockPos extends BlockPos {
    public final String level;

    public LevelBlockPos(int pX, int pY, int pZ, @NotNull String level) {
        super(pX, pY, pZ);
        this.level = level;
    }

    public LevelBlockPos(Vec3i vec3i, @NotNull String level) {
        super(vec3i);
        this.level = level;
    }

    public LevelBlockPos(int pX, int pY, int pZ, Level level) {
        this(pX, pY, pZ, VSGameUtilsKt.getDimensionId(level));
    }

    public LevelBlockPos(Vec3i vec3i, Level level) {
        this(vec3i, VSGameUtilsKt.getDimensionId(level));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LevelBlockPos blockPos)) return false;
        return Objects.equals(blockPos.level, level) && super.equals(obj);
    }
}
