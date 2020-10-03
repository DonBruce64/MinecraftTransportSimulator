package minecrafttransportsimulator.blocks.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.guis.instances.GUIPartBench;
import minecrafttransportsimulator.guis.instances.GUIRadio;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;

public class BlockDecor extends ABlockBase implements IBlockTileEntity<TileEntityDecor>{
	
    public BlockDecor(){
    	super(10.0F, 5.0F);
	}
    
    @Override
    public void addCollisionBoxes(IWrapperWorld world, Point3i location, List<BoundingBox> collidingBoxes){
    	//Get collision box from decor.
    	TileEntityDecor decor = (TileEntityDecor) world.getTileEntity(location);
    	if(decor != null){
    		byte rotationIndex = (byte) (getRotation(world, location)/90F);
    		if(decor.boundingBoxes[rotationIndex] != null){
    			collidingBoxes.add(decor.boundingBoxes[rotationIndex]);
    		}
    	}else{
			super.addCollisionBoxes(world, location, collidingBoxes);
		}
	}
    
    @Override
	public boolean onClicked(IWrapperWorld world, Point3i point, Axis axis, IWrapperPlayer player){
		if(world.isClient()){
			TileEntityDecor decor = (TileEntityDecor) world.getTileEntity(point);
			if(decor.definition.general.itemTypes != null){
				MasterLoader.guiInterface.openGUI(new GUIPartBench(decor, player));
			}else if(decor.definition.general.type != null && decor.definition.general.type.equals("radio")){
				MasterLoader.guiInterface.openGUI(new GUIRadio(decor));
			}
		}
		return true;
	}
    
    @Override
	public TileEntityDecor createTileEntity(IWrapperWorld world, Point3i position, IWrapperNBT data){
		return new TileEntityDecor(world, position, data);
	}

	@Override
	public Class<TileEntityDecor> getTileEntityClass(){
		return TileEntityDecor.class;
	}
}
