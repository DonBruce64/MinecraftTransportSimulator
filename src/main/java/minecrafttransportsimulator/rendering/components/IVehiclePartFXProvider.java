package minecrafttransportsimulator.rendering.components;

/**Implement this interface on any parts that spawns particles or FX, and do the spawning there.
*  This will be called after rendering the part, so it's assured to only be on the client.
*
* @author don_bruce
*/
public interface IVehiclePartFXProvider{
	public abstract void spawnParticles();
}
