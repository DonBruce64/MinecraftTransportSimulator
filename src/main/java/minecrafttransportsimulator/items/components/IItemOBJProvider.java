package minecrafttransportsimulator.items.components;

/**Interface for items that have an OBJ model linked to them.  These items
 * will display said model in the crafting bench rather than the standard item texture.
 *
 * @author don_bruce
 */
public interface IItemOBJProvider{
	
	/**
	 *  Returns the location of the OBJ model for this item.
	 */
	public String getModelLocation();
	
	/**
	 *  Returns the location of the texture for this item.
	 */
	public String getTextureLocation();
}
