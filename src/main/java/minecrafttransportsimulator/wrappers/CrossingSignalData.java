package minecrafttransportsimulator.wrappers;

import java.awt.*;

public class CrossingSignalData {
    public boolean isEnabled = false;
    public boolean showWalk = false;
    public boolean shouldFlash = false;

    public CrossingSignalData(boolean isEnabled, boolean showWalk, boolean shouldFlash) {
        this.isEnabled = isEnabled;
        this.showWalk = showWalk;
        this.shouldFlash = shouldFlash;
    }

    public CrossingSignalData() {}

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
