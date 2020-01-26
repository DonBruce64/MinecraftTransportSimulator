package minecrafttransportsimulator.jsondefs;

public class JSONSign extends AJSONItem<JSONSign.SignGeneral>{

    public class SignGeneral extends AJSONItem.General{
    	public String font;
    	public TextLines[] textLines;
    }
    
    public class TextLines{
    	public byte characters;
    	public float xPos;
    	public float yPos;
    	public float scale;
    	public String color;
    }
}