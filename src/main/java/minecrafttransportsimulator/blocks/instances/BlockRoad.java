package minecrafttransportsimulator.blocks.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad.RoadComponent;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemRoad;
import minecrafttransportsimulator.items.instances.ItemRoadComponent;
import minecrafttransportsimulator.items.instances.ItemWrench;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packets.instances.PacketTileEntityRoadChange;

public class BlockRoad extends ABlockBase implements IBlockTileEntity<TileEntityRoad>{
	
    public BlockRoad(){
    	super(10.0F, 5.0F);
	}
    
    @Override
	public boolean onClicked(IWrapperWorld world, Point3i location, Axis axis, IWrapperPlayer player){
		//Fire a packet to interact with this road.  Will either add, remove, or allow editing of the road.
		//Only fire packet if player is holding a road component that's not an actual road.
		TileEntityRoad road = (TileEntityRoad) world.getTileEntity(location);
		if(road != null){
			IWrapperItemStack heldStack = player.getHeldStack();
			AItemBase heldItem = heldStack.getItem();
			boolean isPlayerHoldingWrench = heldItem instanceof ItemWrench;
			boolean isPlayerHoldingComponent = heldItem instanceof ItemRoadComponent && !(heldItem instanceof ItemRoad);
			RoadComponent componentType = null;
			if(world.isClient()){
				if(isPlayerHoldingWrench){
					MasterLoader.networkInterface.sendToServer(new PacketTileEntityRoadChange(road, componentType, null));
				}else if(isPlayerHoldingComponent){
					MasterLoader.networkInterface.sendToServer(new PacketTileEntityRoadChange(road, componentType, (ItemRoadComponent) heldItem));	
				}else{
					return false;
				}
				return true;
			}
		}
		return false;
	}
    
    @Override
    public void addCollisionBoxes(IWrapperWorld world, Point3i location, List<BoundingBox> collidingBoxes){
    	//Get collision box height from definition.
    	TileEntityRoad road = (TileEntityRoad) world.getTileEntity(location);
    	if(road != null){
    		collidingBoxes.add(road.boundingBox);
    	}else{
			super.addCollisionBoxes(world, location, collidingBoxes);
		}
	}
    
    @Override
	public void onPlaced(IWrapperWorld world, Point3i location, IWrapperPlayer player){
    	//If there's no NBT data, this is a new road and needs to have its initial component added.
		IWrapperNBT data = player.getHeldStack().getData();
		if(data.getString("packID").isEmpty()){
			TileEntityRoad road = (TileEntityRoad) world.getTileEntity(location);
			road.components.put(RoadComponent.CORE, TileEntityRoad.createComponent(((ItemRoadComponent) player.getHeldItem())));
		}
	}
    
    @Override
	public TileEntityRoad createTileEntity(IWrapperWorld world, Point3i position, IWrapperNBT data){
		return new TileEntityRoad(world, position, data);
	}

	@Override
	public Class<TileEntityRoad> getTileEntityClass(){
		return TileEntityRoad.class;
	}
}
