package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.guis.instances.GUIPartBench;
import minecrafttransportsimulator.items.instances.ItemPaintGun;
import minecrafttransportsimulator.mcinterface.InterfaceGUI;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockDecor extends ABlockBaseDecor<TileEntityDecor>{
	
    public BlockDecor(){
    	super();
	}
    
    @Override
	public boolean onClicked(WrapperWorld world, Point3i point, Axis axis, WrapperPlayer player){
		if(world.isClient()){
			TileEntityDecor decor = (TileEntityDecor) world.getTileEntity(point);
			if(player.getHeldItem() instanceof ItemPaintGun){
				//Let the paint gun open the GUI.  To do this, we return false to allow item interaction.
				return false;
			}else if(decor.definition.general.itemTypes != null){
				InterfaceGUI.openGUI(new GUIPartBench(decor, player));
			}
		}
		return true;
	}
    
    @Override
	public TileEntityDecor createTileEntity(WrapperWorld world, Point3i position, WrapperNBT data){
		return new TileEntityDecor(world, position, data);
	}

	@Override
	public Class<TileEntityDecor> getTileEntityClass(){
		return TileEntityDecor.class;
	}
}
