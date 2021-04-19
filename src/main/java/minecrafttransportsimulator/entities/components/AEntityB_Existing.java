package minecrafttransportsimulator.entities.components;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.sound.Radio;
import minecrafttransportsimulator.sound.SoundInstance;

/**Base class for entities that exist in the world. In addition to the normal functions
 * of having a lookup ID, this class also has position/velocity information.  This can be
 * modified to move the entity around.  As the entity exists in the world, it can be used
 * to play sounds, though it cannot provide them of its own accord.
 * 
 * @author don_bruce
 */
public abstract class AEntityB_Existing extends AEntityA_Base{
	protected static final Point3d ZERO_FOR_CONSTRUCTOR = new Point3d();
	
	public final Point3d position;
	public final Point3d prevPosition;
	public final Point3d motion;
	public final Point3d prevMotion;
	public final Point3d angles;
	public final Point3d prevAngles;
	public final Point3d rotation;
	public final Point3d prevRotation;
	public BoundingBox boundingBox;
	public double airDensity;
	public double velocity;
	
	//Internal sound variables.
	public final Radio radio;
	public List<SoundInstance> sounds = new ArrayList<SoundInstance>();
	
	/**Constructor for synced entities**/
	public AEntityB_Existing(WrapperWorld world, WrapperNBT data){
		super(world, data);
		this.position = data.getPoint3d("position");
		this.prevPosition = position.copy();
		this.motion = data.getPoint3d("motion");
		this.prevMotion = motion.copy();
		this.angles = data.getPoint3d("angles");
		this.prevAngles = angles.copy();
		this.rotation = data.getPoint3d("rotation");
		this.prevRotation = rotation.copy();
		this.boundingBox = new BoundingBox(new Point3d(), position, 0.5, 0.5, 0.5, false, false, false, 0);
		this.radio = hasRadio() ? new Radio(this, data.getDataOrNew("radio")) : null;
	}
	
	/**Constructor for un-synced entities.  Allows for specification of position/motion/angles.**/
	public AEntityB_Existing(WrapperWorld world, Point3d position, Point3d motion, Point3d angles){
		super(world, null);
		this.position = position.copy();
		this.prevPosition = position.copy();
		this.motion = motion.copy();
		this.prevMotion = motion.copy();
		this.angles = angles.copy();
		this.prevAngles = angles.copy();
		this.rotation = new Point3d();
		this.prevRotation = rotation.copy();
		this.boundingBox = new BoundingBox(new Point3d(), position, 0.5, 0.5, 0.5, false, false, false, 0);
		this.radio = null;
	}
	
	@Override
	public void update(){
		super.update();
		if(world.isClient()){
			updateSounds();
		}
		prevPosition.setTo(position);
		prevMotion.setTo(motion);
		prevAngles.setTo(angles);
		prevRotation.setTo(rotation);
		airDensity = 1.225*Math.pow(2, -position.y/(500D*world.getMaxHeight()/256D));
		velocity = motion.length();
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
	 *  Returns true if this entity can be collided with.  Normally this is false, but there
	 *  are times where entities should affect collision.
	 */
	public boolean canBeCollidedWith(){
		return false;
	}
	
	/**
	 *  This method returns how much light this entity is providing.  Used to send lighting status to various
	 *  systems for rendering in the world to provide actual light rather than rendered light.
	 *  This is different than {@link #getLightPower()}, which is for internally-rendered lights.
	 */
	public float getLightProvided(){
    	return 0.0F;
	}
	
	/**
   	 *  Returns how much power the lights on the entity have.
   	 *  1 is full power, 0 is no power.  Note that this does not directly
   	 *  correspond to rendering of the lights due to different light sections
   	 *  rendering differently at different power levels.
   	 */
    public float getLightPower(){
    	return 1.0F;
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
	 *  Called at the start of every update tick to update sounds, as this catches the updates from the prior tick
	 *  and allows for use of a simplified super() call in updates while at the same time keeping all sound code isolated.
	 */
    public void updateSounds(){
    	//Update radio of we have one.
    	if(radio != null){
			radio.update();
		}
    }
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		if(shouldSavePosition()){
			data.setPoint3d("position", position);
			data.setPoint3d("motion", motion);
			data.setPoint3d("angles", angles);
			data.setPoint3d("rotation", rotation);
		}
		if(radio != null){
			WrapperNBT radioData = new WrapperNBT();
			radio.save(radioData);
			data.setData("radio", radioData);
		}
	}
}
