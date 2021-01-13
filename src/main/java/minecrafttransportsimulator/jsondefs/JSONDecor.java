package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONDecor extends AJSONMultiModelProvider<JSONDecor.DecorGeneral>{
	public JSONRendering rendering;

    public class DecorGeneral extends AJSONMultiModelProvider<JSONDecor.DecorGeneral>.General{
    	public String type;
    	@JSONRequired
    	public float width;
    	@JSONRequired
    	public float height;
    	@JSONRequired
    	public float depth;
    	public TextLine[] textLines;
    	@Deprecated
    	public List<JSONText> textObjects;
    	public List<String> itemTypes;
    	public List<String> partTypes;
    	public List<String> items;
    }
    
    @Deprecated
    public class TextLine{
    	public float xPos;
    	public float yPos;
    	public float zPos;
    	public float scale;
    	public String color;
    }
}