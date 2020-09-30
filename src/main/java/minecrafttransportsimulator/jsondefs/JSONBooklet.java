package minecrafttransportsimulator.jsondefs;

public class JSONBooklet extends AJSONItem<JSONBooklet.BookletGeneral>{
	
    public class BookletGeneral extends AJSONItem<JSONBooklet.BookletGeneral>.General{
    	public int textureWidth;
    	public int textureHeight;
    	public boolean disableTOC;
    	public String coverTexture;
    	public JSONText[] titleText;
    	public BookletPage[] pages;
    }
    
    public class BookletPage{
    	public String pageTexture;
    	public String title;
    	public JSONText[] pageText;
    }
}