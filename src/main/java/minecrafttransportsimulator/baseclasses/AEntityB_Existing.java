package minecrafttransportsimulator.baseclasses;

import java.util.List;

import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.sound.InterfaceSound;
import minecrafttransportsimulator.sound.SoundInstance;

/**Base class for entities that exist in the world. In addition to the normal functions
 * of having a lookup ID, this class also has position/velocity information.  This can be
 * modified to move the entity around.  As the entity exists in the world, it can be used
 * to play sounds, though it cannot provide them of its own accord.
 * 
 * @author don_bruce
 */
public abstract class AEntityB_Existing extends AEntityA_Base{
	
	public final Point3d position;
	public final Point3d prevPosition;
	public final Point3d motion;
	public final Point3d prevMotion;
	public final Point3d angles;
	public final Point3d prevAngles;
	public final Point3d rotation;
	public final Point3d prevRotation;
	public double airDensity;
	
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
		
		//Start sounds.
		if(world.isClient()){
			InterfaceSound.startSounds(this);
		}
	}
	
	@Override
	public void update(){
		super.update();
		prevPosition.setTo(position);
		prevMotion.setTo(motion);
		prevAngles.setTo(angles);
		prevRotation.setTo(rotation);
		airDensity = 1.225*Math.pow(2, -position.y/(500D*world.getMaxHeight()/256D));
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
	 *  on other data, such as an entity on another entity, then you may not care for this data and can
	 *  return false.  This will save on disk space and networking if you have a lot of entities.
	 */
	public boolean shouldSavePosition(){
		return true;
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
	 *  Called by the audio system to query this entity to update its sounds.
	 *  The entity should start/stop any sounds, and change their properties,
	 *  when this method is called.  All existing sounds this entity is playing
	 *  are passed-in to allow the entity to know if we need to start them or not.
	 */
    public void updateSounds(List<SoundInstance> sounds){
    	if(!isValid){
    		for(SoundInstance sound : sounds){
    			sound.stop();
    		}
    	}
    }
    
    /**
   	 *  Spawns particles for this entity.  This is called after every render frame, so
   	 *  watch your methods to prevent spam.  Note that this method is not called if the
   	 *  game is paused, as particles are assumed to only be spawned during normal entity
   	 *  updates.
   	 */
    public void spawnParticles(){}
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		if(shouldSavePosition()){
			data.setPoint3d("position", position);
			data.setPoint3d("motion", motion);
			data.setPoint3d("angles", angles);
			data.setPoint3d("rotation", rotation);
		}
	}
}
