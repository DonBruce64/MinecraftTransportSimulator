package minecrafttransportsimulator.mcinterface;

import javax.annotation.Nullable;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityInventoryProvider;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityItemLoader;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

/**Builder for tile entities that contain inventories.  This builder ticks.
 *
 * @author don_bruce
 */
public class BuilderTileEntityInventoryContainer<InventoryTileEntity extends ATileEntityBase<?> & ITileEntityInventoryProvider> extends BuilderTileEntity<InventoryTileEntity> implements IInventory{
	
	public BuilderTileEntityInventoryContainer(){
		super();
	}
	
	@Override
	public void update(){
		super.update();
		if(tileEntity != null){
			tileEntity.update();
			if(tileEntity instanceof TileEntityItemLoader){
				if(((TileEntityItemLoader) tileEntity).isUnloader()){
					EntityInventoryContainer inventory = tileEntity.getInventory();
					if(inventory.getCount() > 0){
						//Pump out items to handler below, if we have one.
						TileEntity teBelow = world.getTileEntity(getPos().down());
						if(teBelow != null && teBelow.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP)){
							IItemHandler itemHandler = teBelow.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
							for(int i=0; i<inventory.getSize(); ++i){
								WrapperItemStack stack = inventory.getStack(i);
								if(!stack.isEmpty()){
									ItemStack testStack = stack.stack.copy();
									testStack.setCount(1);
									for(int j=0; j<itemHandler.getSlots(); ++j){									
										if(itemHandler.insertItem(j, testStack, false).isEmpty()){
											//Handler took item, adjust.
											inventory.removeFromSlot(i, 1);
											return;
										}
									}
								}
							}
						}
					}
				}else{
					EntityInventoryContainer inventory = tileEntity.getInventory();
					if(inventory.getCount() < inventory.getSize()){
						//Grab items from handler above, if we have one.
						TileEntity teAbove = world.getTileEntity(getPos().up());
						if(teAbove != null && teAbove.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.DOWN)){
							IItemHandler itemHandler = teAbove.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.DOWN);
							for(int i=0; i<inventory.getSize(); ++i){
								WrapperItemStack stack = inventory.getStack(i);
								if(stack.isEmpty()){
									for(int j=0; j<itemHandler.getSlots(); ++j){
										ItemStack testStack = itemHandler.extractItem(j, 1, false);
										if(!testStack.isEmpty()){
											//Handler gave item, adjust.
											inventory.setStack(new WrapperItemStack(testStack), j);
											return;
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public String getName(){
		return "item_loader";
	}

	@Override
	public boolean hasCustomName(){
		return false;
	}

	@Override
	public int getSizeInventory(){
		return tileEntity.getInventory().getSize();
	}

	@Override
	public boolean isEmpty(){
		return tileEntity.getInventory().getCount() == 0;
	}

	@Override
	public ItemStack getStackInSlot(int index){
		return tileEntity.getInventory().getStack(index).stack;
	}

	@Override
	public ItemStack decrStackSize(int index, int count){
		tileEntity.getInventory().removeFromSlot(index, count);
		return tileEntity.getInventory().getStack(index).stack;
	}

	@Override
	public ItemStack removeStackFromSlot(int index){
		tileEntity.getInventory().removeFromSlot(index, tileEntity.getInventory().getStack(index).getSize());
		return ItemStack.EMPTY;
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack stack){
		tileEntity.getInventory().setStack(new WrapperItemStack(stack), index);
	}

	@Override
	public int getInventoryStackLimit(){
		return 64;
	}

	@Override
	public boolean isUsableByPlayer(EntityPlayer player){
		return true;
	}

	@Override
	public void openInventory(EntityPlayer player){}

	@Override
	public void closeInventory(EntityPlayer player){}

	@Override
	public boolean isItemValidForSlot(int index, ItemStack stack){
		return true;
	}

	@Override
	public int getField(int id){
		return 0;
	}

	@Override
	public void setField(int id, int value){}

	@Override
	public int getFieldCount(){
		return 0;
	}

	@Override
	public void clear(){}
	
    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing){
    	if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && EnumFacing.DOWN.equals(facing)){
    		return true;
    	}else{
    		return super.hasCapability(capability, facing);
    	}
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing){
    	if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && EnumFacing.DOWN.equals(facing)){
    		return (T) this;
    	}else{
    		return super.getCapability(capability, facing);
    	}
    }
}
