package minecrafttransportsimulator.baseclasses;

import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.rendering.components.IParticleProvider;

/**Interface that allows an object to provide and control a {@link Gun}.
 * This object may only have one gun on it, and should keep a reference
 * to this object for use in these methods if required.
 *
 * @author don_bruce
 */
public interface IGunProvider extends IParticleProvider{
	
	 /**
	 *  Rotates the passed-in point to match the provider's orientation.
	 *  This is done for points relative to the gun's provider to get
	 *  proper orientation prior to firing, so it should take into account
	 *  the gun's state in the world.
	 */
    public void orientToProvider(Point3d point);
	
    /**
	 *  This is called when the gun needs to reload.  Do so here
	 *  via the {@link Gun#tryToReload(ItemPart)}.  Calling that
	 *  method, if successful, will result in {@link #handleReload(ItemPart)}
	 *  being called, so keep this in mind.
	 */
	public void reloadGunBullets();
	
	/**
	 *  Returns the controller for the gun.
	 *  The returned value may or may not be the provider itself.
	 */
	public WrapperEntity getController();
	
	/**
	 *  Returns true if the guns is currently active.
	 *  Controller is passed-in for state-based activity.
	 */
	public boolean isGunActive(WrapperEntity controller);
	
	/**
	 *  Returns the desired yaw of the gun as defined by the controller.
	 *  Usually this is just where the controller is looking.  If no controller
	 *  is present, then this method will not be called.
	 */
	public double getDesiredYaw(WrapperEntity controller);
	
	/**
	 *  Returns the desired pitch of the gun as defined by the controller.
	 *  Usually this is just where the controller is looking.  If no controller
	 *  is present, then this method will not be called.
	 */
	public double getDesiredPitch(WrapperEntity controller);
	
	/**
	 *  Returns the gun number for the gun.
	 *  This is used to determine gun firing delay timing.
	 */
	public int getGunNumber();
	
	/**
	 *  Returns the total number of guns on this provider.
	 *  This is used to determine gun firing delay timing.
	 */
	public int getTotalGuns();
}
