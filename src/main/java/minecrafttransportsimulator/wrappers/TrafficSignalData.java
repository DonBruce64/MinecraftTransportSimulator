package minecrafttransportsimulator.wrappers;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.math.BlockPos;

import java.awt.*;

public class TrafficSignalData {
    private BlockPos blockPos;
    private boolean isEnabled = false;
    private Color color = Color.RED;
    private boolean shouldFlash = false;

    public TrafficSignalData(BlockPos blockPos, boolean isEnabled, Color color, boolean shouldFlash) {
        this.blockPos = blockPos;
        this.isEnabled = isEnabled;
        this.color = color;
        this.shouldFlash = shouldFlash;
    }

    public TrafficSignalData(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public TrafficSignalData setBlockPos(BlockPos blockPos) {
        this.blockPos = blockPos;
        return this;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public TrafficSignalData setEnabled(boolean enabled) {
        isEnabled = enabled;
        return this;
    }

    public Color getColor() {
        if (color == null) color = Color.RED;
        return color;
    }

    public String getColorName() {
        return getColor() == Color.YELLOW ? "YELLOW" : getColor() == Color.GREEN ? "GREEN" : "RED";
    }

    public TrafficSignalData setColor(Color color) {
        this.color = color;
        return this;
    }


    public TrafficSignalData setColor(String colorName) {
        this.color = colorName.equalsIgnoreCase("yellow") ? Color.YELLOW : colorName.equalsIgnoreCase("green") ? Color.GREEN : Color.RED;
        return this;
    }

    public boolean isShouldFlash() {
        return shouldFlash;
    }

    public TrafficSignalData setShouldFlash(boolean shouldFlash) {
        this.shouldFlash = shouldFlash;
        return this;
    }
}
