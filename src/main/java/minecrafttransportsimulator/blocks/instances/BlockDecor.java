package minecrafttransportsimulator.blocks.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.wrappers.WrapperBlock;
import minecrafttransportsimulator.wrappers.WrapperTileEntity;
import minecrafttransportsimulator.wrappers.WrapperWorld;

public class BlockDecor extends ABlockBase implements WrapperTileEntity.IProvider{
	public final JSONDecor definition;
	private final BoundingBox[] boundingBoxes = new BoundingBox[4];
	
    public BlockDecor(WrapperBlock wrapperReference, JSONDecor definition){
    	super(wrapperReference, 10.0F, 5.0F);
		this.definition = definition;
		//Add a bounding box for each rotation.
		boundingBoxes[0] = new BoundingBox(0, 0, 0, definition.general.width, definition.general.height, definition.general.depth);
		boundingBoxes[1] = boundingBoxes[0].getRotated90();
		boundingBoxes[2] = boundingBoxes[0];
		boundingBoxes[3] = boundingBoxes[1];
	}
    
    
    @Override
    public void addCollisionBoxes(WrapperWorld world, Point3i point, List<BoundingBox> collidingBoxes){
    	byte rotationIndex = (byte) (getRotation(world, point)/90F);
		collidingBoxes.add(boundingBoxes[rotationIndex]);
	}
    
    @Override
    public boolean renderBlockModel(){
		//No block models for decor.
    	return false;
	}
    
    @Override
	public TileEntityDecor createTileEntity(){
		return new TileEntityDecor();
	}
}
