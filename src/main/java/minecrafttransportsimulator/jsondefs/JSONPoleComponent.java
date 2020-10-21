package minecrafttransportsimulator.jsondefs;

import java.util.List;

public class JSONPoleComponent extends AJSONModelProvider<JSONPoleComponent.PoleGeneral>{

    public class PoleGeneral extends AJSONModelProvider<JSONPoleComponent.PoleGeneral>.General{
    	public String type;
    	public float radius;
    	public TextLine[] textLines;
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