package minecrafttransportsimulator.jsondefs;

public class JSONSign extends AJSONItem<JSONSign.SignGeneral>{

    public class SignGeneral extends AJSONItem.General{
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