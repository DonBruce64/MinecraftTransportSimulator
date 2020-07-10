package minecrafttransportsimulator.vehicles.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mcinterface.BuilderEntity;
import mcinterface.WrapperEntity;
import mcinterface.WrapperNBT;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3d;

/**Base entity class.  This class contains the most basic code for entities,
 * as well as some basic variables and methods for movement and save/load operations.
 * The constructor in this class is called both on servers and clients.  At this time,
 * the data to create this entity will be present, unlike MC where NBT is held from clients.
 * Because of this, no checks are required in the code to ensure server-client syncing has
 * occurred, as this entity class will not be constructed until such handshakes are performed.
 * Do note that this means that for a brief period on the client-side, this class will not exist,
 * despite there being an active builder created.
 * <br><br>
 * Also note that this entity is never "loads" saved data.  Rather, it is created from it.
 * This means that there is no method call to load properties from data. Instead, data required
 * for loading will be passed-in to the constructor.  This data should be used to create the entity
 * in its loaded state.  For saving, which can happen multiple times in the entity's lifetime,
 * {@link #save(WrapperNBT)} is called.  All data required in the constructor should be saved here.
 * 
 * 
 * @author don_bruce
 */
public abstract class AEntityBase{
	private static int idCounter = 0;
	public static Map<Integer, AEntityBase> createdEntities = new HashMap<Integer, AEntityBase>();
	
	public final BuilderEntity builder;
	public final int uniqueID;
	public final WrapperWorld world;
	public final Point3d position;
	public final Point3d prevPosition;
	public final Point3d motion;
	public final Point3d prevMotion;
	public final Point3d angles;
	public final Point3d prevAngles;
	public final Point3d rotation;
	public final Point3d prevRotation;
	
	public boolean isValid = true;
	public List<Point3d> riders = new ArrayList<Point3d>();
	public List<Point3d> riderOffsets = new ArrayList<Point3d>();
	
	public AEntityBase(BuilderEntity builder, WrapperWorld world, WrapperNBT data){
		this.builder = builder;
		this.uniqueID = world.isClient() ? data.getInteger("uniqueID") : idCounter++;
		this.world = world;
		this.position = data.getPoint3d("position");
		this.prevPosition = position.copy();
		this.motion = data.getPoint3d("motion");
		this.prevMotion = motion.copy();
		this.angles = data.getPoint3d("angles");
		this.prevAngles = angles.copy();
		this.rotation = data.getPoint3d("rotation");
		this.prevRotation = rotation.copy();
		
		for(int i=0; i<data.getInteger("totalRiderOffsets"); ++i){
			Point3d riderOffset = data.getPoint3d("riderPosition_" + i);
			Double[] seatPosition = new Double[3];
			seatPosition[0] = tagCompound.getDouble("Seat" + String.valueOf(riderSeatPositions.size()) + "0");
			seatPosition[1] = tagCompound.getDouble("Seat" + String.valueOf(riderSeatPositions.size()) + "1");
			seatPosition[2] = tagCompound.getDouble("Seat" + String.valueOf(riderSeatPositions.size()) + "2");
			riderSeatPositions.add(seatPosition);
		}
		
		//FIXME remove dead entities from this list.
		createdEntities.put(uniqueID, this);
	}
	
	 /**
	 * Called to update this entity.  Value of previous variables are set here by default, but
	 * extra functionality can and should be added in sub-classes.
	 */
	public void update(){
		prevPosition.setTo(position);
		prevMotion.setTo(motion);
		prevAngles.setTo(angles);
		prevRotation.setTo(rotation);
	}
	
	/**
	 *  Called when the Entity needs to be saved to disk.  The passed-in wrapper
	 *  should be written to at this point with any data needing to be saved.
	 */
	public void save(WrapperNBT data){
		data.setInteger("uniqueID", uniqueID);
		data.setPoint3d("position", position);
		data.setPoint3d("motion", motion);
		data.setPoint3d("angles", angles);
		data.setPoint3d("rotation", rotation);
	}
}
