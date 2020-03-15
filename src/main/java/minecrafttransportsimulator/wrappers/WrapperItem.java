package minecrafttransportsimulator.wrappers;

import java.util.List;

import javax.annotation.Nullable;

import minecrafttransportsimulator.items.core.AItemBase;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**Wrapper for MC GUI classes.  Constructor takes a type of {@link AItemBase}, but
 * is only visible when calling {@link #createItem(AItemBase)}.  This will automatically
 * construct the wrapper and will return the created instance of the item (not wrapper)
 * for use in the code.  The wrapper instance is cached and saved to be registered
 * in the MC systems.  When interfacing with MC systems use this class, but when
 * doing code in MTS use the item, NOT the wrapper!
 *
 * @author don_bruce
 */
public class WrapperItem extends Item{	
	
	private final AItemBase item;
	
	private WrapperItem(AItemBase item, boolean isStackable){
		super();
		this.item = item;
		setFull3D();
		if(!isStackable){
			this.setMaxStackSize(1);
		}
	}
	
	/**
	 *  This is called by the main MC system to get the displayName for the item.
	 *  Normally this is a translated version of the unlocalized name, but we
	 *  allow for use of the wrapper to decide what name we translate.
	 */
	@Override
	public String getItemStackDisplayName(ItemStack stack){
        return item.getItemName();
	}
	
	/**
	 *  This is called by the main MC system to add tooltip lines to the item.
	 *  The ItemStack is passed-in here as it contains NBT data that may be used
	 *  to change the display of the tooltip.  We convert the NBT into wrapper form
	 *  to prevent excess odd calls and allow for a more raw serialization system.
	 *  Also prevents us from using a MC class with a changing name. 
	 */
	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		item.addTooltipLines(tooltipLines, stack.getTagCompound());
	}
	
	
	//--------------------START OF INSTANCE HELPER METHODS--------------------	


	
	//--------------------START OF STATIC HELPER METHODS--------------------
	/**
	 *  Creates a wrapper for the the passed-in Item, saving the wrapper to be registered later.
	 *  This wrapper instance will interact with all MC code via passthrough of the item's methods.
	 *  Returns the passed-in item for constructor convenience.
	 */
	public static void registerItem(AItemBase item){
		//TODO save item in a list here for registration.
	}
}
