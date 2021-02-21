package minecrafttransportsimulator.blocks.components;

import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public abstract class ABlockBaseDecor<DecorClass extends TileEntityDecor> extends ABlockBase implements IBlockTileEntity<DecorClass>{
	
    public ABlockBaseDecor(){
    	super(10.0F, 5.0F);
	}
    
    @Override
    public void addCollisionBoxes(WrapperWorld world, Point3d position, List<BoundingBox> collidingBoxes){
    	//Get collision box from decor.
    	TileEntityDecor decor = (TileEntityDecor) world.getTileEntity(position);
    	if(decor != null){
    		byte rotationIndex = (byte) (getRotation(world, position)/90F);
    		if(decor.boundingBoxes[rotationIndex] != null){
    			collidingBoxes.add(decor.boundingBoxes[rotationIndex]);
    		}
    	}else{
			super.addCollisionBoxes(world, position, collidingBoxes);
		}
	}
}
