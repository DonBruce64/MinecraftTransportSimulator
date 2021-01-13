package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONItem extends AJSONItem<JSONItem.ItemGeneral>{
	@JSONRequired(dependentField="type", dependentValues={"booklet"}, subField="general")
	public JSONBooklet booklet;
	@JSONRequired(dependentField="type", dependentValues={"food"}, subField="general")
	public JSONFood food;

    public class ItemGeneral extends AJSONItem<JSONItem.ItemGeneral>.General{
    	public String type;
    }
    
    public class JSONBooklet{
    	public int textureWidth;
    	public int textureHeight;
    	public boolean disableTOC;
    	public String coverTexture;
    	public JSONText[] titleText;
    	public BookletPage[] pages;
    	
    	public class BookletPage{
        	public String pageTexture;
        	public String title;
        	public JSONText[] pageText;
        }
    }
    
    public class JSONFood{
    	public boolean isDrink;
    	public int timeToEat;
    	public int hungerAmount;
    	public float saturationAmount;
    	public List<JSONPotionEffect> effects;
    }
}