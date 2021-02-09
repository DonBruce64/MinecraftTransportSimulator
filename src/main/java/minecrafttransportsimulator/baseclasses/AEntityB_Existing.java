package minecrafttransportsimulator.baseclasses;

import java.util.List;

import minecrafttransportsimulator.mcinterface.WrapperEntity;
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
	
	/**Wrapper reference for interfacing with entity-wrapper systems.  MAY be null if the entity isn't wrapped by
	 * the game because it's not an actual entity as the game defines it.  As such, this should only be used by
	 * game-interface (mcinterface) code, NOT by the actual code.**/
	public final WrapperEntity wrapper;
	public final Point3d position;
	public final Point3d prevPosition;
	public final Point3d motion;
	public final Point3d prevMotion;
	public final Point3d angles;
	public final Point3d prevAngles;
	public final Point3d rotation;
	public final Point3d prevRotation;
	
	public AEntityB_Existing(WrapperWorld world, WrapperEntity wrapper, WrapperNBT data){
		super(world, data);
		this.wrapper = wrapper;
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
	 *  This method returns true if this entity is lit up.  Used to send lighting status to various
	 *  systems for rendering.  Note that this does NOT imply that this entity is bright enough to make
	 *  its surroundings lit up.  Rather, this simply means there is a light on this entity somewhere.
	 */
	public boolean isLitUp(){
		//FIXME make this true for vehicles and decors.
    	return false;
	}
	
	/**
   	 *  Returns how much power the lights on the entity have.
   	 *  1 is full power, 0 is no power.  Note that this does not directly
   	 *  correspond to rendering of the lights due to different light sections
   	 *  rendering differently at different power levels.
   	 */
    public float getLightPower(){
    	//FIXME make this true for vehicles and decors.
    	return 1.0F;
    }
    
    /**
   	 *  Returns true if this entity should render light beams.  This is entity-specific in the config,
   	 *  so the method is abstract here.
   	 */
    public boolean shouldRenderBeams(){
    	//FIXME make this true for vehicles and decors.
    	return false;
    }
    
    /**
	 *  Called by the audio system to query this entity to update its sounds.
	 *  The entity should start/stop any sounds, and change their properties,
	 *  when this method is called.  All existing sounds this entity is playing
	 *  are passed-in to allow the entity to know if we need to start them or not.
	 */
    public void updateSounds(List<SoundInstance> sounds){}
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		data.setPoint3d("position", position);
		data.setPoint3d("motion", motion);
		data.setPoint3d("angles", angles);
		data.setPoint3d("rotation", rotation);
	}
}
