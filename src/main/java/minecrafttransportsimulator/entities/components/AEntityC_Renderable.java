package minecrafttransportsimulator.entities.components;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.rendering.components.ARenderEntity;

/**Base class for entities that are rendered in the world in 3D.
 * This level adds various rendering methods and functions for said rendering. 
 * 
 * @author don_bruce
 */
public abstract class AEntityC_Renderable extends AEntityB_Existing{
	
	/**The scale of this entity.  Is not used to move bounding boxes.  More for rendering, but does some physics.*/
	public float scale = 1.0F;
	
	/**The mirrored state of this entity.  Only used for rendering to flip the model across the x-axis.*/
	public boolean mirrored;
	
	/**Constructor for synced entities**/
	public AEntityC_Renderable(WrapperWorld world, WrapperPlayer placingPlayer, WrapperNBT data){
		super(world, placingPlayer, data);
	}
	
	/**Constructor for un-synced entities.  Allows for specification of position/motion/angles.**/
	public AEntityC_Renderable(WrapperWorld world, Point3d position, Point3d motion, Point3d angles){
		super(world, position, motion, angles);
	}
    
    /**
	 *  Gets the renderer for this entity.  No actual rendering should be done in this method, 
	 *  as doing so could result in classes being imported during object instantiation on the server 
	 *  for graphics libraries that do not exist.  Instead, generate a class that does this and call it.
	 *  This method is assured to be only called on clients, so you can just do the construction of the
	 *  renderer in this method and pass it back as the return.
	 */
	public abstract <RendererInstance extends ARenderEntity<AnimationEntity>, AnimationEntity extends AEntityC_Renderable> RendererInstance getRenderer();
}
