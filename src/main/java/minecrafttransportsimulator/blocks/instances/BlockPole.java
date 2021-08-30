package minecrafttransportsimulator.blocks.instances;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

/**Pole block class.  This class allows for dynamic collision boxes and dynamic
 * placement of components on poles via the Tile Entity.
 *
 * @author don_bruce
 */
public class BlockPole extends ABlockBaseTileEntity{
	private final Map<Axis, BoundingBox> axisBounds = new HashMap<Axis, BoundingBox>();
	
	public BlockPole(){
		super(10.0F, 5.0F);
		double connectorRadius = 0.125D;
		double axialRadius = (0.5D - connectorRadius)/2D;
		double axialCenterPoint = 0.5D - axialRadius;
		axisBounds.put(Axis.NONE, new BoundingBox(new Point3d(), connectorRadius, connectorRadius, connectorRadius));
		axisBounds.put(Axis.UP, new BoundingBox(new Point3d(0, axialCenterPoint, 0), connectorRadius, axialRadius, connectorRadius));
		axisBounds.put(Axis.DOWN, new BoundingBox(new Point3d(0, -axialCenterPoint, 0), connectorRadius, axialRadius, connectorRadius));
		axisBounds.put(Axis.NORTH, new BoundingBox(new Point3d(0, 0, -axialCenterPoint), connectorRadius, connectorRadius, axialRadius));
		axisBounds.put(Axis.SOUTH, new BoundingBox(new Point3d(0, 0, axialCenterPoint), connectorRadius, connectorRadius, axialRadius));
		axisBounds.put(Axis.EAST, new BoundingBox(new Point3d(axialCenterPoint, 0, 0), axialRadius, connectorRadius, connectorRadius));
		axisBounds.put(Axis.WEST, new BoundingBox(new Point3d(-axialCenterPoint, 0, 0), axialRadius, connectorRadius, connectorRadius));
	}
	
	@Override
	public void addCollisionBoxes(WrapperWorld world, Point3d position, List<BoundingBox> collidingBoxes){
		//For every connection or component we have, return a collision box.
		TileEntityPole pole = (TileEntityPole) world.getTileEntity(position);
		if(pole != null){
			for(Axis axis : Axis.values()){
				if(axis.blockBased){
					if(world.getBlock(axis.getOffsetPoint(position)) instanceof BlockPole || world.isBlockSolid(axis.getOffsetPoint(position), axis.getOpposite()) || pole.components.containsKey(axis)){
						collidingBoxes.add(axisBounds.get(axis));
					}
				}else if(axis.equals(Axis.NONE)){
					collidingBoxes.add(axisBounds.get(axis));
				}
			}
		}else{
			super.addCollisionBoxes(world, position, collidingBoxes);
		}
	}
	
	@Override
	public TileEntityPole createTileEntity(WrapperWorld world, Point3d position, WrapperPlayer placingPlayer, WrapperNBT data){
		return new TileEntityPole(world, position, placingPlayer, data);
	}
}
