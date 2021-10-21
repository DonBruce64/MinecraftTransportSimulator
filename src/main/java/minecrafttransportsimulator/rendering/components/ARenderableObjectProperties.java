package minecrafttransportsimulator.rendering.components;

import java.nio.FloatBuffer;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.InterfaceRender;

/**Class designed to represent a renderable object.  Said object has at minimum some
 * geometry, though this can be a cached set of vertices or a hard-coded saved set.
 * It will also have a texture, though this texture may be a single solid white sheet
 * for shader-compatible solid rendering.  In this case, the color will be specified,
 * and should be used to change the color prior to rendering.  Various other properties
 * exist for lighting/blending.  In all cases, similar renderable objects should be
 * grouped together and batch-rendered for efficiency.  The primary grouping should be
 * texture, as this prevents re-binds.  Secondary will be color.  Lighting will usually
 * dictate when the object can render rather than in what order (solid vs translucent pass).
 * To assist with this, the equals() method checks texture and color and, if they are identical,
 * returns true.  This allows for said objects to be used as map-keys for easier grouping.
 * Note that this does NOT include the actual vertex data in this equality check.
 * 
 *
 * @author don_bruce
 */
public abstract class ARenderableObjectProperties{
	
	public final String texture;
	public final ColorRGB color;
	public float alpha;
	public final FloatBuffer vertices;
	public final int cachedVertexIndex;
	
	public ARenderableObjectProperties(String texture, ColorRGB color, float alpha, FloatBuffer vertices, boolean cacheVertices){
		this.texture = texture;
		this.color = color;
		this.alpha = alpha;
		if(cacheVertices){
			this.vertices = null;
			this.cachedVertexIndex = InterfaceRender.cacheVertices(vertices);
		}else{
			this.vertices = vertices;
			this.cachedVertexIndex = -1;
		}
	}
	
	@Override
	public boolean equals(Object object){
		if(object instanceof ARenderableObjectProperties){
			ARenderableObjectProperties otherProperties = (ARenderableObjectProperties) object;
			return this.texture.equals(otherProperties.texture) && this.color.equals(otherProperties.color);
		}else{
			return false;
		}
	}
	
	/**
	 *  Returns the blend state for this object.  This is abstract as this can change
	 *  during run-time if an object goes from normal to bright blending.
	 */
	public abstract BlendState getBlendState();
	
	/**
	 *  Returns the position where this object should render.
	 *  This is applied prior to rotation, so ensure this takes
	 *  rotation into account.
	 */
	public abstract Point3d getPosition();
	
	/**
	 *  Returns the orientation of this object for rendering.
	 */
	public abstract Point3d getRotation();
	
	public enum BlendState{
		SOLID,
		TRANSLUCENT,
		BRIGHT_BLENDED;
	}
}
