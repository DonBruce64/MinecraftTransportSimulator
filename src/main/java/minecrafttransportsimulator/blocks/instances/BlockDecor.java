package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.guis.instances.GUIPartBench;
import minecrafttransportsimulator.guis.instances.GUITextEditor;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.items.instances.ItemItem.ItemComponentType;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;

public class BlockDecor extends ABlockBaseDecor<TileEntityDecor>{
	
    public BlockDecor(){
    	super();
	}
    
    @Override
	public boolean onClicked(WrapperWorld world, Point3d position, Axis axis, WrapperPlayer player){
    	TileEntityDecor decor = (TileEntityDecor) world.getTileEntity(position);
    	AItemBase heldItem = player.getHeldItem();
		if(heldItem instanceof ItemItem && ((ItemItem) heldItem).definition.item.equals(ItemComponentType.PAINT_GUN)){
			//Don't do decor actions if we are holding a paint gun.
			return false;
		}else if(decor.definition.decor.crafting != null){
			if(world.isClient() && player.equals(InterfaceClient.getClientPlayer())){
				InterfaceGUI.openGUI(new GUIPartBench(decor.definition.decor.crafting));
			}
		}else if(!decor.text.isEmpty()){
			if(world.isClient()){
				InterfaceGUI.openGUI(new GUITextEditor(decor));
			}
		}
		if(!world.isClient()){
			decor.variablesOn.add("clicked");
			if(decor.variablesOn.contains("activated")){
				decor.variablesOn.remove("activated");
			}else{
				decor.variablesOn.add("activated");
			}
			InterfacePacket.sendToAllClients(new PacketEntityVariableToggle(decor, "clicked"));
			InterfacePacket.sendToAllClients(new PacketEntityVariableToggle(decor, "activated"));
		}
		return true;
	}
    
    @Override
	public TileEntityDecor createTileEntity(WrapperWorld world, Point3d position, WrapperNBT data){
		return new TileEntityDecor(world, position, data);
	}

	@Override
	public Class<TileEntityDecor> getTileEntityClass(){
		return TileEntityDecor.class;
	}
}
