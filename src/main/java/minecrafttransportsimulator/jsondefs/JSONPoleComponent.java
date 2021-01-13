package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONPoleComponent extends AJSONMultiModelProvider<JSONPoleComponent.PoleGeneral>{
	public JSONRendering rendering;
	
    public class PoleGeneral extends AJSONMultiModelProvider<JSONPoleComponent.PoleGeneral>.General{
    	@JSONRequired
    	public String type;
    	public float radius;
    	public TextLine[] textLines;
    	@Deprecated
    	public List<JSONText> textObjects;
    }
    
    @Deprecated
    public class TextLine{
    	public int characters;
    	public float xPos;
    	public float yPos;
    	public float zPos;
    	public float scale;
    	public String color;
    }
}