package minecrafttransportsimulator.jsondefs;

public class PackBookletObject{
	public BookletGeneralConfig general;

    public class BookletGeneralConfig{
    	public String name;
    	public String[] materials;
    	public int textureWidth;
    	public int textureHeight;
    	public boolean disableTOC;
    	public String coverTexture;
    	public BookletText[] titleText;
    	public BookletPage[] pages;
    }
    
    public class BookletPage{
    	public String pageTexture;
    	public String title;
    	public BookletText[] pageText;
    }
    
    public class BookletText{
    	public String text;
    	public String color;
    	public float scale;
    	public int offsetX;
    	public int offsetY;
    	public int wrapWidth;
    	public boolean centered;
    }
}