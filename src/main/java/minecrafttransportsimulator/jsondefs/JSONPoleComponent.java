package minecrafttransportsimulator.jsondefs;

public class JSONPoleComponent extends AJSONCraftable<JSONPoleComponent.PoleGeneral>{

    public class PoleGeneral extends AJSONCraftable<JSONPoleComponent.PoleGeneral>.General{
    	public String type;
    	public TextLine[] textLines;
    }
    
    public class TextLine{
    	public byte characters;
    	public float xPos;
    	public float yPos;
    	public float scale;
    	public String color;
    }
}