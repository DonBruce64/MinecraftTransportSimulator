package minecrafttransportsimulator.wrappers;

import net.minecraft.util.math.BlockPos;

import java.awt.*;

public class CrossingSignalData {
    private BlockPos blockPos;
    public boolean isEnabled = false;
    public boolean showWalk = false;
    public boolean shouldFlash = false;

    public CrossingSignalData(BlockPos blockPos, boolean isEnabled, boolean showWalk, boolean shouldFlash) {
        this.blockPos = blockPos;
        this.isEnabled = isEnabled;
        this.showWalk = showWalk;
        this.shouldFlash = shouldFlash;
    }

    public CrossingSignalData() {}

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public void setBlockPos(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public boolean isShowWalk() {
        return showWalk;
    }

    public void setShowWalk(boolean showWalk) {
        this.showWalk = showWalk;
    }

    public boolean isShouldFlash() {
        return shouldFlash;
    }

    public void setShouldFlash(boolean shouldFlash) {
        this.shouldFlash = shouldFlash;
    }
}
