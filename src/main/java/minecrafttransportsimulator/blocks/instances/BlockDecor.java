package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.guis.instances.GUIPartBench;
import minecrafttransportsimulator.guis.instances.GUITextEditor;
import minecrafttransportsimulator.items.instances.ItemPaintGun;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockDecor extends ABlockBaseDecor<TileEntityDecor>{
	
    public BlockDecor(){
    	super();
	}
    
    @Override
	public boolean onClicked(WrapperWorld world, Point3d position, Axis axis, WrapperPlayer player){
    	TileEntityDecor decor = (TileEntityDecor) world.getTileEntity(position);
		if(player.getHeldItem() instanceof ItemPaintGun){
			//Don't do decor actions if we are holding a paint gun.
			return false;
		}else if(decor.definition.decor.itemTypes != null || decor.definition.decor.items != null){
			if(world.isClient()){
				InterfaceGUI.openGUI(new GUIPartBench(decor, player));
			}
			return true;
		}else if(!decor.text.isEmpty()){
			if(world.isClient()){
				InterfaceGUI.openGUI(new GUITextEditor(decor));
			}
			return true;
		}
		return false;
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
