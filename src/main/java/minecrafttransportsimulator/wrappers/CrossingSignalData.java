package minecrafttransportsimulator.wrappers;

import net.minecraft.util.math.BlockPos;

public class CrossingSignalData {
    private BlockPos blockPos;
    private boolean isEnabled = false;
    private boolean showWalk = false;
    private boolean shouldFlash = false;

    public CrossingSignalData(BlockPos blockPos, boolean isEnabled, boolean showWalk, boolean shouldFlash) {
        this.blockPos = blockPos;
        this.isEnabled = isEnabled;
        this.showWalk = showWalk;
        this.shouldFlash = shouldFlash;
    }

    public CrossingSignalData(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public CrossingSignalData setBlockPos(BlockPos blockPos) {
        this.blockPos = blockPos;
        return this;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public CrossingSignalData setEnabled(boolean enabled) {
        isEnabled = enabled;
        return this;
    }

    public boolean isShowWalk() {
        return showWalk;
    }

    public CrossingSignalData setShowWalk(boolean showWalk) {
        this.showWalk = showWalk;
        return this;
    }

    public boolean isShouldFlash() {
        return shouldFlash;
    }

    public CrossingSignalData setShouldFlash(boolean shouldFlash) {
        this.shouldFlash = shouldFlash;
        return this;
    }
}
