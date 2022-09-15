package minecrafttransportsimulator.baseclasses;

/**
 * Basic color class.  Stores a color as three floats representing RGBA values from 0 to 1.
 * These are in the domain of 0-255 for standard RGB, but stored as floats as OpenGL
 * uses those internally.  You may, however, create this from a standard 0-255 constructor.
 * This class is used instead of the AWT class as it lets us pack color into the JSON, which
 * is loaded on servers that don't have windows or colors installed.
 *
 * @author don_bruce
 */
public class ColorRGB {
    public static final ColorRGB WHITE = new ColorRGB(255, 255, 255);
    public static final ColorRGB BLACK = new ColorRGB(0, 0, 0);

    public static final ColorRGB LIGHT_GRAY = new ColorRGB(192, 192, 192);
    public static final ColorRGB GRAY = new ColorRGB(128, 128, 128);
    public static final ColorRGB DARK_GRAY = new ColorRGB(64, 64, 64);

    public static final ColorRGB RED = new ColorRGB(255, 0, 0);
    public static final ColorRGB GREEN = new ColorRGB(0, 255, 0);
    public static final ColorRGB BLUE = new ColorRGB(0, 0, 255);

    public static final ColorRGB CYAN = new ColorRGB(0, 255, 255);
    public static final ColorRGB MAGENTA = new ColorRGB(255, 0, 255);
    public static final ColorRGB YELLOW = new ColorRGB(255, 255, 0);

    public static final ColorRGB PINK = new ColorRGB(255, 175, 175);
    public static final ColorRGB ORANGE = new ColorRGB(255, 200, 0);

    public float red;
    public float green;
    public float blue;
    public int rgbInt;
    public int[] hsv;

    public ColorRGB() {
        this(1.0F, 1.0F, 1.0F, false);
    }

    public ColorRGB(String hexRGB) {
        //Sanitize this input to not require a hex prefix.
        this(Integer.decode("#" + hexRGB.substring(hexRGB.length() - 6)));
    }

    public ColorRGB(int packedRGB) {
        this((packedRGB >> 16) & 255, (packedRGB >> 8) & 255, packedRGB & 255);
    }

    public ColorRGB(int red, int green, int blue) {
        this(red / 255F, green / 255F, blue / 255F, false);
    }

    public ColorRGB(float red, float green, float blue, boolean isHSV) {
        float finalRed;
        float finalGreen;
        float finalBlue;

        if (isHSV) {
            this.hsv = new int[]{(int) red, (int) green, (int) blue};
            float[] RGB = HSVtoRGB(red % 360, green / 100, blue / 100);
            finalRed = RGB[0];
            finalGreen = RGB[1];
            finalBlue = RGB[2];
        } else {
            finalRed = red;
            finalGreen = green;
            finalBlue = blue;
            this.hsv = RGBtoHSV(red, green, blue);
        }

        this.red = finalRed;
        this.green = finalGreen;
        this.blue = finalBlue;
        this.rgbInt = ((int) (red * 255) << 16) | ((int) (green * 255) << 8) | (int) (blue * 255);
    }

    public static float[] HSVtoRGB(float hue, float saturation, float value) {
        float c = value * saturation;
        float x = c * (1 - Math.abs((hue / 60) % 2 - 1));
        float m = value - c;

        if (hue < 60) {
            return new float[]{c + m, x + m, m};
        } else if (hue < 120) {
            return new float[]{x + m, c + m, m};
        } else if (hue < 180) {
            return new float[]{m, c + m, x + m};
        } else if (hue < 240) {
            return new float[]{m, x + m, c + m};
        } else if (hue < 300) {
            return new float[]{x + m, m, c + m};
        } else {
            return new float[]{c + m, m, x + m};
        }
    }

    public static int[] RGBtoHSV(float red, float green, float blue) {
        red /= 255;
        green /= 255;
        blue /= 255;

        float cMax = Math.max(red, Math.max(green, blue));
        float cMin = Math.min(red, Math.min(green, blue));
        float diff = cMax - cMin;

        int hue;
        int saturation;

        if (cMax == cMin) {
            hue = 0;
        } else if (cMax == red) {
            hue = (int) (60 * ((green - blue) / diff) + 360) % 360;
        } else if (cMax == green) {
            hue = (int) (60 * ((blue - red) / diff) + 120) % 360;
        } else {
            hue = (int) (60 * ((red - green) / diff) + 240) % 360;
        }

        if (cMax == 0) {
            saturation = 0;
        } else {
            saturation = (int) (diff / cMax) * 100;
        }

        int value = (int) cMax * 100;

        return new int[]{hue, saturation, value};

    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ColorRGB) {
            ColorRGB otherColor = (ColorRGB) object;
            return red == otherColor.red && blue == otherColor.blue && green == otherColor.green;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "[" + red + ", " + green + ", " + blue + "]";
    }

    /**
     * Sets the color to the passed-in color.
     * This is done to keep this object instance for reference while changing it.
     * You MUST use this method to change the color values, otherwise it will
     * leave this object in an inconsistent state!
     */
    public void setTo(ColorRGB color) {
        this.red = color.red;
        this.green = color.green;
        this.blue = color.blue;
        this.rgbInt = color.rgbInt;
        this.hsv = color.hsv;
    }
}
