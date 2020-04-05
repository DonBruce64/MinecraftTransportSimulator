package minecrafttransportsimulator.blocks.components;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.wrappers.WrapperBlockAxial;
import minecrafttransportsimulator.wrappers.WrapperWorld;

/**Axial block class.  Like the base class, but has axial connection functionality from
 * {@link WrapperBlockAxial}.  Most everything will be handled by the wrapper, as
 * this class assumes that the only reason you're doing this is to toggle flags in
 * the JSON.
 *
 * @author don_bruce
 */
public abstract class ABlockAxial extends ABlockBase{
	protected final Map<Axis, BoundingBox> bounds = new HashMap<Axis, BoundingBox>();
	protected final Map<Axis, Boolean> states = new HashMap<Axis, Boolean>();
	
	public ABlockAxial(float hardness, float blastResistance, double connectorRadius){
		super(hardness, blastResistance);
		double axialRadius = (0.5D - connectorRadius)/2D;
		double axialCenterPoint = 0.5D - axialRadius;
		bounds.put(Axis.NONE, new BoundingBox(0, 0, 0, connectorRadius, connectorRadius, connectorRadius));
		bounds.put(Axis.UP, new BoundingBox(0, axialCenterPoint, 0, connectorRadius, axialRadius, connectorRadius));
		bounds.put(Axis.DOWN, new BoundingBox(0, -axialCenterPoint, 0, connectorRadius, axialRadius, connectorRadius));
		bounds.put(Axis.NORTH, new BoundingBox(0, 0, -axialCenterPoint, connectorRadius, connectorRadius, axialRadius));
		bounds.put(Axis.SOUTH, new BoundingBox(0, 0, axialCenterPoint, connectorRadius, connectorRadius, axialRadius));
		bounds.put(Axis.EAST, new BoundingBox(axialCenterPoint, 0, 0, axialRadius, connectorRadius, connectorRadius));
		bounds.put(Axis.WEST, new BoundingBox(-axialCenterPoint, 0, 0, axialRadius, connectorRadius, connectorRadius));
	}
	
	@Override
	public void addCollisionBoxes(WrapperWorld world, Point3i point, List<BoundingBox> collidingBoxes){
		//Need to add multiple collision boxes here.
		//First update the axis states based on what MC has.
		WrapperBlockAxial.updateAxisStates(world, point, states);
		
		//Now add the collision boxes.
		for(Entry<Axis, Boolean> statePair : states.entrySet()){
			if(statePair.getValue()){
				collidingBoxes.add(bounds.get(statePair.getKey()));
			}
		}
	}

	/**
	 *  Returns true if the front side of this axial block can connect to anything.
	 *  This should return false for blocks like signs where you don't want anything
	 *  to connect to the front of the block.
	 */
	public boolean canConnectOnFront(){
		return true;
	}
	
	/**
	 *  Returns true if this block can connect to solids.  If false, it will only
	 *  connect to other blocks of the same class or super-class (instanceof).
	 */
	public boolean canConnectToSolids(){
		return false;
	}
	
	public static enum Axis{
		NONE,
		UP,
		DOWN,
		NORTH,
		SOUTH,
		EAST,
		WEST;
	}
}
