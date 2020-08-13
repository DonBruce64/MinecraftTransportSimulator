package minecrafttransportsimulator.blocks.instances;

import java.util.List;

import mcinterface.WrapperNBT;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;

public class BlockDecor extends ABlockBase implements IBlockTileEntity<TileEntityDecor>{
	
    public BlockDecor(){
    	super(10.0F, 5.0F);
	}
    
    @Override
    public void addCollisionBoxes(WrapperWorld world, Point3i location, List<BoundingBox> collidingBoxes){
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
	public TileEntityDecor createTileEntity(WrapperWorld world, Point3i position, WrapperNBT data) {
		return new TileEntityDecor(world, position, data);
	}

	@Override
	public Class<TileEntityDecor> getTileEntityClass(){
		return TileEntityDecor.class;
	}
}
