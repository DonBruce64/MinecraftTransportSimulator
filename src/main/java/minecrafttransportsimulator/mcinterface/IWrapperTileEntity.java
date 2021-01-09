package minecrafttransportsimulator.mcinterface;

/**Wrapper for the MC Tile Entity class (called BlockEntity in later MC versions cause
 * the people who maintain the mappings like to make life difficult through constant
 * re-naming of things).  This class, unlike {@link IBuilderTileEntity}, is responsible
 * for interfacing with MC Tile Entities.  Because of this, some methods are different
 * between the two classes.
 * <br><br>
 * Note that while this wrapper has an update method, it does not necessarily mean the
 * TE that is wrapped will be ticked (updated).  Some TEs don't have ticking functionality,
 * so if the update method is called on one of those then the method call will simply do nothing.
 *
 * @author don_bruce
 */
public interface IWrapperTileEntity{
	
	/**
	 *  Updates the TE, if the TE supports it.
	 */
	public void update();
	
	/**
	 *  Gets the inventory for this TE, or null if it doesn't have one.
	 */
	public WrapperInventory getInventory();
	
	/**
	 *  Loads the TE data from the passed-in source.
	 */
	public void load(WrapperNBT data);
	
	/**
	 *  Saves the TE to the passed-in NBT Wrapper tag.
	 */
	public void save(WrapperNBT data);
}
