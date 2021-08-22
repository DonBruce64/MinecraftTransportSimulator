package minecrafttransportsimulator.baseclasses;

/**Basic color class.  Stores a color as three floats representing RGBA values from 0 to 1.
 * These are in the domain of 0-255 for standard RGB, but stored as floats as OpenGL
 * uses those internally.  You may, however, create this from a standard 0-255 constructor.
 * This class is used instead of the AWT class as it lets us pack color into the JSON, which
 * is loaded on servers that don't have windows or colors installed.
 *
 * @author don_bruce
 */
public class ColorRGB{
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
	
	public final float red;
	public final float green;
	public final float blue;
	public final int rgbInt;
	
	public ColorRGB(){
		this(1.0F, 1.0F, 1.0F);
	}
	
	public ColorRGB(String hexRGB){
		//Sanitize this input to not require a hex prefix.
		this(Integer.decode("#" + hexRGB.substring(hexRGB.length() - 6)));
	}
	
	public ColorRGB(int packedRGB){
		this((packedRGB >> 16) & 255, (packedRGB >> 8) & 255, packedRGB & 255);
	}
	
	public ColorRGB(int red, int green, int blue){
		this(red/255F, green/255F, blue/255F);
	}
	
	public ColorRGB(float red, float green, float blue){
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.rgbInt = ((int) (red*255) << 16) | ((int) (green*255) << 8) | (int) (blue*255);
	}
	
	@Override
	public boolean equals(Object object){
		if(object instanceof ColorRGB){
			ColorRGB otherColor = (ColorRGB) object;
			return red == otherColor.red && blue == otherColor.blue && green == otherColor.green;
		}else{
			return false;
		}
	}
	
	@Override
	public String toString(){
		return "[" + red + ", " + green + ", " + blue + "]";
	}
}
