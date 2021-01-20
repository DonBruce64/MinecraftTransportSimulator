package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONItem extends AJSONItem<JSONItem.ItemGeneral>{
	@JSONRequired(dependentField="type", dependentValues={"booklet"}, subField="general")
	public JSONBooklet booklet;
	@JSONRequired(dependentField="type", dependentValues={"food"}, subField="general")
	public JSONFood food;

    public class ItemGeneral extends AJSONItem<JSONItem.ItemGeneral>.General{
	@JSONDescription("This parameter is optional.  If included, the item will be created with specific functionality.  The following special types are currently supported: \"booklet\" and \"food\".")
    	public String type;
    }
    
    public class JSONBooklet{
	@JSONDescription("How wide of a texture, in px, this booklet uses.  Used for ALL pages")
    	public int textureWidth;
    	@JSONDescription("How high of a texture, in px, this booklet uses.  Used for ALL pages")
    	public int textureHeight;
	@JSONDescription("If present and true, the Table of Contents will not be created.")
    	public boolean disableTOC;
    	@JSONDescription("The name of the texture for the cover.  Should be prefixed by your modID and MUST be a power of 2.")
    	public String coverTexture;
    	@JSONDescription("A list of Text objects that make up the text for the title.")
    	public JSONText[] titleText;
    	@JSONDescription("A list of Page objects that make up the pages of this booklet.")
    	public BookletPage[] pages;
    	
    	public class BookletPage{
		@JSONDescription("The name of the texture for this page.  Each page may use a different texture, if desired.\nHowever, all textures MUST be the same resolution as defined by textureWidth and textureHeight")
        	public String pageTexture;
		@JSONDescription("The title for this page that will be used to make the Table of Contents.  May be omitted if the Table of Contents is disabled.")
        	public String title;
		@JSONDescription("An array of text objects that make up the text for the page. These follow the same format as textObjects.\nThe only difference between these and vehicles is the z-coord should always be 0, and the defaultText parameter is what will be rendered in the booklet.")
        	public JSONText[] pageText;
        }
    }
    
    public class JSONFood{
    	@JSONDescription("The animation to play while consuming this item.  If true, the drinking animation is played.  If false, the eating animation is played.")
    	public boolean isDrink;
    	@JSONDescription("How long, in ticks, it takes to eat this food item.")
    	public int timeToEat;
 	@JSONDescription("How much hunger this food item fills.  Must be a whole number.")
    	public int hungerAmount;
	@JSONDescription("How much saturation this food has.  May be a decimal.")
    	public float saturationAmount;
	@JSONDescription("A optional list of effects that this food item provides.")
    	public List<JSONPotionEffect> effects;
    }
}
