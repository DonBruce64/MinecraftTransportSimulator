package minecrafttransportsimulator.baseclasses;

import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.sound.ISoundProviderComplex;

/**Interface that allows an object to provide and control a {@link Gun}.
 * This object may only have one gun on it, and should keep a reference
 * to this object for use in these methods if required.
 *
 * @author don_bruce
 */
public interface IGunProvider extends ISoundProviderComplex{
	
	 /**
	 *  Return the rotation of this provider as a Point3d.
	 */
    public Point3d getProviderRotation();
	
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
	public IWrapperEntity getController();
	
	/**
	 *  Returns true if the guns is currently active.
	 *  Controller is passed-in for state-based activity.
	 */
	public boolean isGunActive(IWrapperEntity controller);
	
	/**
	 *  Returns the desired yaw of the gun.
	 *  This should take the controller into account.
	 */
	public double getDesiredYaw(IWrapperEntity controller);
	
	/**
	 *  Returns the desired pitch of the gun.
	 *  This should take the controller into account.
	 */
	public double getDesiredPitch(IWrapperEntity controller);
	
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
