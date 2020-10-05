package mcinterface1122;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.mcinterface.IInterfaceCore;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperTileEntity;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Loader;

@SuppressWarnings("deprecation")
class InterfaceCore implements IInterfaceCore{
	private final List<String> queuedLogs = new ArrayList<String>();
	
	@Override
	public String getGameVersion(){
		return Loader.instance().getMCVersionString().substring("Minecraft ".length());
	}
	
	@Override
	public boolean isModPresent(String modID){
		return Loader.isModLoaded(modID);
	}
	
	@Override
	public String getModName(String modID){
		return Loader.instance().getIndexedModList().get(modID).getName();
	}
	
	@Override
	public String getFluidName(String fluidID){
		return FluidRegistry.getFluid(fluidID) != null ? new FluidStack(FluidRegistry.getFluid(fluidID), 1).getLocalizedName() : "INVALID";
	}

	@Override
	public String translate(String text){
		return  I18n.translateToLocal(text);
	}
	
	@Override
	public void logError(String message){
		if(MasterInterface.logger == null){
			queuedLogs.add(message);
		}else{
			MasterInterface.logger.error(message);
		}
	}
	
	@Override
	public IWrapperNBT createNewTag(){
		return new WrapperNBT(new NBTTagCompound());
	}
	
	@Override
	public IWrapperItemStack getStack(AItemBase item){
		return new WrapperItemStack(new ItemStack(BuilderItem.itemWrapperMap.get(item)));
	}
	
	@Override
	public List<IWrapperItemStack> parseFromJSON(AItemPack<?> item, boolean includeMain, boolean includeSub){
		List<IWrapperItemStack> stackList = new ArrayList<IWrapperItemStack>();
		String currentSubName = "";
		try{
			//Get main materials.
			if(includeMain){
		    	for(String itemText : item.definition.general.materials){
					int itemQty = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
					itemText = itemText.substring(0, itemText.lastIndexOf(':'));
					
					int itemMetadata = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
					itemText = itemText.substring(0, itemText.lastIndexOf(':'));
					stackList.add(new WrapperItemStack(new ItemStack(Item.getByNameOrId(itemText), itemQty, itemMetadata)));
				}
			}
	    	
	    	//Get subType materials, if required.
	    	if(includeSub && item instanceof AItemSubTyped){
		    	for(String itemText : ((AItemSubTyped<?>) item).getExtraMaterials()){
					int itemQty = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
					itemText = itemText.substring(0, itemText.lastIndexOf(':'));
					
					int itemMetadata = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
					itemText = itemText.substring(0, itemText.lastIndexOf(':'));
					stackList.add(new WrapperItemStack(new ItemStack(Item.getByNameOrId(itemText), itemQty, itemMetadata)));
		    	}
	    	}
	    	
	    	//Return all materials.
	    	return stackList;
		}catch(Exception e){
			e.printStackTrace();
			throw new NullPointerException("ERROR: Could not parse crafting ingredients for item: " + item.definition.packID + item.definition.systemName + currentSubName + ".  Report this to the pack author!");
		}
	}
	
	@Override
	public IWrapperTileEntity getFakeTileEntity(String type, IWrapperWorld world, IWrapperNBT data, int inventoryUnits){
		switch(type){
			case("chest") : return new WrapperTileEntity.WrapperEntityChest((WrapperWorld) world, (WrapperNBT) data, inventoryUnits);
			case("furnace") : return new WrapperTileEntity.WrapperEntityFurnace((WrapperWorld) world, (WrapperNBT) data);
			case("brewing_stand") : return new WrapperTileEntity.WrapperEntityBrewingStand((WrapperWorld) world, (WrapperNBT) data);
			default : return null;
		}
	}
	
	/**
     * Called to send queued logs to the logger.  This is required as the logger
     * gets created during pre-init, but logs can be generated during construction.
     */
    public void flushLogQueue(){
    	for(String log : queuedLogs){
    		logError(log);
    	}
    }
}
