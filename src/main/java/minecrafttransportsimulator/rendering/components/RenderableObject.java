package minecrafttransportsimulator.rendering.components;

import java.nio.FloatBuffer;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.mcinterface.InterfaceRender;

/**Class designed to represent a renderable object.  Said object has at minimum some
 * geometry, though this can be a cached set of vertices or a hard-coded saved set.
 * It may also have a texture, though this texture may be a single solid white sheet
 * for shader-compatible solid rendering.  In this case, the color will be specified,
 * and should be used to change the color prior to rendering.  Various other properties
 * exist for lighting/blending.  In all cases, similar renderable objects should be
 * grouped together and batch-rendered for efficiency.  The primary grouping should be
 * texture, as this prevents re-binds.  Secondary should be color.  Lighting will usually
 * dictate when the object can render rather than in what order (solid vs translucent pass).
 * To assist with this, the equals() method checks texture and color and, if they are identical,
 * returns true.  This allows for said objects to be used as map-keys for easier grouping.
 * Note that this does NOT include the actual vertex data in this equality check.
 * 
 *  For said vertex data, the data order is as follows:
 *  <ul>
 *  <li>The nX-coordinate of the normal for the vertex, in the x-dimension.
 *  <li>The nY-coordinate of the normal for the vertex, in the y-dimension.
 *  <li>The nZ-coordinate of the normal for the vertex, in the z-dimension.
 *  <li>The u-coordinate of the UV-mapping for the vertex.
 *  <li>The v-coordinate of the UV-mapping for the vertex.
 *  <li>The x-coordinate of a vertex on the model.
 *  <li>The y-coordinate of a vertex on the model.
 *  <li>The z-coordinate of a vertex on the model.
 *  </ul>
 *  
 *  Note that this object can render lines as well as tris.  For lines, {@link #lineWidth} should
 *  be set to a non-zero number.  If this is the case, then the buffer will be interpreted as line
 *  data and line rendering will occur.   the data format is as follows:
 * <ul>
 *  <li>The x-coordinate of the first point on the line.
 *  <li>The y-coordinate of the first point on the line.
 *  <li>The z-coordinate of the first point on the line.
 *  <li>The x-coordinate of the second point on the line.
 *  <li>The y-coordinate of the second point on the line.
 *  <li>The z-coordinate of the second point on the line.
 *  </ul>
 *
 * @author don_bruce
 */
public class RenderableObject{
	
	public final String name;
	public String texture;
	public final ColorRGB color;
	public FloatBuffer vertices;
	public final boolean cacheVertices;
	
	public boolean isTranslucent;
	public int cachedVertexIndex = -1;
	public BlendState blend = BlendState.SOLID;
	public float alpha = 1.0F;
	public float lineWidth = 0.0F;
	public boolean disableLighting;
	public boolean enableBrightBlending;
	
	public RenderableObject(String name, String texture, ColorRGB color, FloatBuffer vertices, boolean cacheVertices){
		this.name = name;
		this.texture = texture;
		this.color = color;
		this.cacheVertices = cacheVertices;
		this.isTranslucent = name.toLowerCase().contains(AModelParser.TRANSLUCENT_OBJECT_NAME);
		this.vertices = vertices;
	}
	
	@Override
	public boolean equals(Object object){
		if(object instanceof RenderableObject){
			RenderableObject otherProperties = (RenderableObject) object;
			return this.texture.equals(otherProperties.texture) && this.color.equals(otherProperties.color);
		}else{
			return false;
		}
	}
	
	/**Renders the vertices from this object.  If they were cached, it renders them as such and destroys
	 * the reference to the static vertices object.  This is to free up the FloatBuffer for re-use.  We
	 * would normally set it to null during construction, but it is realized that having this for post-processing
	 * after model parsing is ideal, so it's not destroyed until render.
	 */
	public void render(){
		InterfaceRender.renderVertices(this);
	}
	
	/**Normalizes the UVs in this object.  This is done to re-map them to the 0->1 texture space
	 * for overridden textures such as lights and windows.
	 */
	public void normalizeUVs(){
		int verticesInObject = vertices.capacity()/8;
		for(int i=0; i<verticesInObject; ++i){
			if(verticesInObject > 3 && i%6 >= 3){
				//Second-half of a quad.
				switch(i%6){
					case(3): vertices.put(i*8+3, 0.0F); vertices.put(i*8+4, 0.0F); break;
					case(4): vertices.put(i*8+3, 1.0F); vertices.put(i*8+4, 1.0F); break;
					case(5): vertices.put(i*8+3, 1.0F); vertices.put(i*8+4, 0.0F); break;
				}
			}else{
				//Normal tri or first half of quad using tri mapping.
				switch(i%6){
					case(0): vertices.put(i*8+3, 0.0F); vertices.put(i*8+4, 0.0F); break;
					case(1): vertices.put(i*8+3, 0.0F); vertices.put(i*8+4, 1.0F); break;
					case(2): vertices.put(i*8+3, 1.0F); vertices.put(i*8+4, 1.0F); break;
					
					case(3): vertices.put(i*8+3, 1.0F); vertices.put(i*8+4, 1.0F); break;
					case(4): vertices.put(i*8+3, 1.0F); vertices.put(i*8+4, 0.0F); break;
					case(5): vertices.put(i*8+3, 0.0F); vertices.put(i*8+4, 0.0F); break;
				}
			}
		}
	}
	
	/**Destroys this object, resetting all references in it for use in other areas.*/
	public void destroy(){
		vertices = null;
		InterfaceRender.deleteVertices(this);
	}
	
	public enum BlendState{
		SOLID,
		TRANSLUCENT,
		BRIGHT_BLENDED;
	}
}
