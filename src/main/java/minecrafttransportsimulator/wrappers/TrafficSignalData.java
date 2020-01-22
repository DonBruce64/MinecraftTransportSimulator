package minecrafttransportsimulator.wrappers;

import java.awt.*;

public class TrafficSignalData {
    public Color color = Color.RED;
    public boolean shouldFlash = true;

    public TrafficSignalData(Color color, boolean shouldFlash) {
        this.color = color;
        this.shouldFlash = shouldFlash;
    }

    public TrafficSignalData() {}

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public boolean isShouldFlash() {
        return shouldFlash;
    }

    public void setShouldFlash(boolean shouldFlash) {
        this.shouldFlash = shouldFlash;
    }
}
