package minecrafttransportsimulator.entities.components;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.instances.EntityRadio;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.sound.SoundInstance;

/**Base class for entities that exist in the world. In addition to the normal functions
 * of having a lookup ID, this class also has position/velocity information.  This can be
 * modified to move the entity around.  As the entity exists in the world, it can be used
 * to play sounds, though it cannot provide them of its own accord.
 * 
 * @author don_bruce
 */
public abstract class AEntityB_Existing extends AEntityA_Base{
	protected static final Point3D ZERO_FOR_CONSTRUCTOR = new Point3D();
	
	public final Point3D position;
	public final Point3D prevPosition;
	public final Point3D motion;
	public final Point3D prevMotion;
	
	public final Point3D angles;
	public final Point3D prevAngles;
	public final Point3D rotation;
	
	public final BoundingBox boundingBox;
	
	/*The rotational component of orientation for this entity.*/
	public final RotationMatrix orientation;
	public final RotationMatrix prevOrientation;
	public final Point3D axialOrientation;
	
	public double airDensity;
	public double velocity;
	/**The player that placed this entity.  Only valid on the server where placement occurs. Client-side will always be null.**/
	public final WrapperPlayer placingPlayer;
	
	//Internal sound variables.
	public final EntityRadio radio;
	public List<SoundInstance> sounds = new ArrayList<SoundInstance>();
	
	/**Constructor for synced entities**/
	public AEntityB_Existing(WrapperWorld world, WrapperPlayer placingPlayer, WrapperNBT data){
		super(world, data);
		this.position = data.getPoint3d("position");
		this.prevPosition = position.copy();
		this.motion = data.getPoint3d("motion");
		this.prevMotion = motion.copy();
		this.angles = data.getPoint3d("angles");
		this.prevAngles = angles.copy();
		this.rotation = data.getPoint3d("rotation");
		this.orientation = new RotationMatrix().setAngleRotation(angles);
		this.prevOrientation = new RotationMatrix().set(orientation);
		this.axialOrientation = new Point3D(0, 0, 1).rotate(orientation);
		this.placingPlayer = placingPlayer;
		this.boundingBox = new BoundingBox(shouldLinkBoundsToPosition() ? this.position : new Point3D(this.position), 0.5, 0.5, 0.5);
		if(hasRadio()){
			this.radio = new EntityRadio(this, data.getDataOrNew("radio"));
			world.addEntity(radio);
		}else{
			this.radio = null;
		}
	}
	
	/**Constructor for un-synced entities.  Allows for specification of position/motion/angles.**/
	public AEntityB_Existing(WrapperWorld world, Point3D position, Point3D motion, Point3D angles){
		super(world, null);
		this.position = position.copy();
		this.prevPosition = position.copy();
		this.motion = motion.copy();
		this.prevMotion = motion.copy();
		this.angles = angles.copy();
		this.prevAngles = angles.copy();
		this.rotation = new Point3D();
		this.orientation = new RotationMatrix().setAngleRotation(angles);
		this.prevOrientation = new RotationMatrix().set(orientation);
		this.axialOrientation = new Point3D(0, 0, 1).rotate(orientation);
		this.placingPlayer = null;
		this.boundingBox = new BoundingBox(shouldLinkBoundsToPosition() ? this.position : new Point3D(this.position), 0.5, 0.5, 0.5);
		this.radio = null;
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			world.beginProfiling("EntityB_Level", true);
			if(world.isClient()){
				updateSounds(0);
			}
			if(changesPosition()){
				prevPosition.set(position);
				prevMotion.set(motion);
				prevOrientation.set(orientation);
				//FIXME this is only here as a hack to get this to work with existing rendering.  See if we can remove angles after we are done.
				orientation.setAngleRotation(angles);
				prevAngles.set(angles);			
				
				axialOrientation.set(0, 0, 1).rotate(orientation);
				airDensity = 1.225*Math.pow(2, -position.y/(500D*world.getMaxHeight()/256D));
				velocity = motion.length();
			}
			world.endProfiling();
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public void remove(){
		super.remove();
		if(world.isClient()){
			if(radio != null){
				radio.stop();
			}
			for(SoundInstance sound : sounds){
				sound.stopSound = true;
			}
		}
	}
	
	/**
	 * Called to destroy this entity.  While removal will still allow the entity to be re-created
	 * into the world on the next loading of the world or the chunk it is is, destruction is the
	 * permanent removal of this entity from the world.  Think breaking blocks or crashing vehicles.
	 * The passed-in bounding box may be considered the location of destruction.  Used in cases where
	 * an entity has multiple bounding boxes and the destruction is location-specific.
	 */
	public void destroy(BoundingBox box){
		//Do normal removal operations.
		remove();
	}
	
	/**
	 *  This method returns true if this entity needs to be chunkloaded.  This will prevent it from
	 *  being unloaded server-side.  Client-side entities will still unload as clients unload their
	 *  own chunks.
	 */
	public boolean needsChunkloading(){
		return false;
	}
	
	/**
	 *  Returning false here will prevent this entity's positional data from being saved during saving
	 *  operations.  Normally you want this, but if your entity dynamically calculates its position based
	 *  on other data, such as another entity, then you may not care for this data and can return false.
	 *  This will save on disk space and networking if you have a lot of entities.
	 */
	public boolean shouldSavePosition(){
		return true;
	}
	
	/**
	 *  This method returns true if this entity should link its bounding box to its position.  This will
	 *  result in the box always being centered on the entity.  This is normally true, but may be made
	 *  false for entities that need their collision offset from their position.  This will require manual bounding
	 *  box syncing, though the initial location of the box will be the position of the entity.
	 */
	public boolean shouldLinkBoundsToPosition(){
		return true;
	}
	
	/**
	 *  This method returns true if this entity can change position (or if positional data is important to it).
	 *  This is normally true, but some entities may not ever move, and so there are some calls we can skip.
	 */
	public boolean changesPosition(){
		return true;
	}
	
	/**
	 *  Sets the interpolated orientation into the passed-in Matrix4d.
	 *  The position is not interpolated with this as {@link #orientation}
	 *  only contains the rotational elements of this entity. 
	 */
	public void getInterpolatedOrientation(RotationMatrix store, double partialTicks){
		store.interploate(prevOrientation, orientation, partialTicks);
	}
	
	/**
	 *  Returns true if this entity can collide with the passed-in entity.  Normally this is false, but there
	 *  are times where entities should affect collision.
	 */
	public boolean canCollideWith(AEntityB_Existing entityToCollide){
		return false;
	}
	
	/**
	 * Called when checking if this entity can be interacted with.
	 * If it does interactions it should do them and then return true.
	 * This is only called on the server: client modifications will be done via packets.
	 */
	public boolean interact(WrapperPlayer player){
		return false;
	}
	
	/**
	 *  This method returns how much light this entity is providing.  Used to send lighting status to various
	 *  systems for rendering in the world to provide actual light rather than rendered light.
	 */
	public float getLightProvided(){
    	return 0.0F;
	}
    
    /**
   	 *  Returns true if this entity should render light beams.  This is entity-specific in the config,
   	 *  so the method is abstract here.
   	 */
    public boolean shouldRenderBeams(){
    	return false;
    }
    
    /**
	 *  Returns true if this entity has a radio.  Radios are updated to sync with the entity and
	 *  will save on them as applicable.
	 */
	public boolean hasRadio(){
		return false;
	}
    
    /**
	 *  This method should start/stop any sounds, and change any existing sound properties when called.
	 *  Called at the start of every update tick to update sounds, and on partial tick frames.  You can
	 *  tell if the method is being called on a partial tick if the partial ticks parameter is non-zero.
	 *  Use this to ensure you don't query slow-activating sounds every frame.
	 */
    public void updateSounds(float partialTicks){
    	//Update radio of we have one and we're on the main update.
    	if(radio != null && partialTicks == 0){
			radio.update();
		}
    }
	
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		if(shouldSavePosition()){
			data.setPoint3d("position", position);
			data.setPoint3d("motion", motion);
			data.setPoint3d("angles", angles);
			data.setPoint3d("rotation", rotation);
		}
		if(radio != null){
			data.setData("radio", radio.save(new WrapperNBT()));
		}
		return data;
	}
}
