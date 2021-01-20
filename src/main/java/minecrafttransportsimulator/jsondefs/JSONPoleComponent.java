package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONPoleComponent extends AJSONMultiModelProvider<JSONPoleComponent.PoleGeneral>{
	@JSONAutoGenerate
	@JSONDescription("Optional rendering properties for this pole component.")
	public JSONRendering rendering;
	
    public class PoleGeneral extends AJSONMultiModelProvider<JSONPoleComponent.PoleGeneral>.General{
    	@JSONRequired
    	public String type;
	@JSONDescription("This parameter tells MTS how much to offset components put on this pole.\nThis is because some poles may be larger than others, and making it so models always render at the same point would lead to clipping on large poles and floating on small ones. \nFor all cases, you should set this to the offset from the center where all components should attach to your pole.")
    	public float radius;
	@Deprecated
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
