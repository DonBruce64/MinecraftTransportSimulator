package minecrafttransportsimulator.rendering.components;

/**Implement this interface on anything that needs to spawn particles or FX, and do the spawning there.
*  This prevents client-side code from being in the main method block, and allows for faster particle
*  and sound spawning due to this firing every frame rather than every tick.
*
* @author don_bruce
*/
public interface IParticleProvider{
	public abstract void spawnParticles();
}
