package minecrafttransportsimulator.guis.components;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.rendering.components.AModelParser;

/**Custom #D model render class.  This allows for rendering a parsed model into a GUI.
 * Mainly used to render vehicles, though can be used for other models if desired.
 * This class keeps a list of all parsed models for caching, as this allows for faster
 * switching as we don't need to parse the models each time we view them.  These lists
 * are cleared when the GUI containing this component is un-loaded.  Note that the
 * model and texture associated with this component may change while this component
 * is still active.  This is to allows us to use one component to render changing
 * models, say in a crafting bench for instance.  The same reasoning applies
 * for why the position is not static (though it is required at construction).
 *
 * @author don_bruce
 */
public class GUIComponent3DModel{
	/**Parsed vertex indexes.  Keyed by model name.*/
	private static final Map<String, Integer> modelParsedVertexLists = new HashMap<String, Integer>();
	private static final Map<String, Float> modelScalingFactors = new HashMap<String, Float>();
	
	public final float scaleFactor;
	public final boolean isometric;
	public final boolean staticScaling;
	
	public int x;
	public int y;
	public boolean spin;
	public float scale;
	public String modelLocation;
	public String textureLocation;
	
	public boolean visible = true;
	    	
	public GUIComponent3DModel(int x, int y, float scaleFactor, boolean isometric, boolean spin, boolean staticScaling){
		this.x = x;
		this.y = y;
		this.scaleFactor = scaleFactor;
		this.isometric = isometric;
		this.spin = spin;
		this.staticScaling = staticScaling;
	}
	
	/**
	 *  Renders the model that this component defines.
	 */
    public void renderModel(){
    	if(visible){
			if(modelLocation != null){
				if(!modelParsedVertexLists.containsKey(modelLocation)){
					Map<String, FloatBuffer> parsedModel = AModelParser.parseModel(modelLocation);
					//Remove any windows and "commented" objects from the model.  We don't want to render those.
					parsedModel.keySet().removeIf(objectName -> objectName.toLowerCase().contains("window") || objectName.startsWith("#"));
					
					//Get the min/max vertex values for the model so we know how much to scale it.
					//Also get how many vertices are in the model total for the final buffer.
					float minX = 999;
					float maxX = -999;
					float minY = 999;
					float maxY = -999;
					float minZ = 999;
					float maxZ = -999;
					int totalVertices = 0;
					for(FloatBuffer vertices : parsedModel.values()){
						totalVertices += vertices.capacity();
						for(int i=0; i<vertices.capacity(); i+=8){
							float xCoord = vertices.get(i+5);
							float yCoord = vertices.get(i+6);
							float zCoord = vertices.get(i+7);
							minX = Math.min(minX, xCoord);
							maxX = Math.max(maxX, xCoord);
							minY = Math.min(minY, yCoord);
							maxY = Math.max(maxY, yCoord);
							minZ = Math.min(minZ, zCoord);
							maxZ = Math.max(maxZ, zCoord);
						}
					}
					float globalMax = Math.max(Math.max(maxX - minX, maxY - minY), maxZ - minZ);
					modelScalingFactors.put(modelLocation, globalMax > 1.5 ? 1.5F/globalMax : 1.0F);
					
					//Cache the model now that we know how big it is.
					FloatBuffer totalModel = FloatBuffer.allocate(totalVertices);
					for(FloatBuffer vertices : parsedModel.values()){
						totalModel.put(vertices);
					}
					totalModel.flip();
					modelParsedVertexLists.put(modelLocation, InterfaceRender.cacheVertices(totalModel));
				}
				GL11.glPushMatrix();
				//Translate to position and rotate to isometric view if required.
				GL11.glTranslatef(x, y, 100);
				GL11.glRotatef(180, 1, 0, 0);
				if(isometric){
					GL11.glRotatef(45, 0, 1, 0);
					GL11.glRotatef(-35.264F, 1, 0, 1);
				}
				
				//If set to rotate, do so now based on time.
				if(spin){
					GL11.glRotatef((36*System.currentTimeMillis()/1000)%360, 0, 1, 0);
				}

				//Scale based on our scaling factor and render.
				if(!staticScaling){
					scale = modelScalingFactors.get(modelLocation);
				}
				GL11.glScalef(scale*scaleFactor, scale*scaleFactor, -scale*scaleFactor);
				InterfaceRender.renderVertices(modelParsedVertexLists.get(modelLocation));
				GL11.glPopMatrix();
			}
		}
    }
    
    /**
	 *  Clear the caches.  Call this when closing the GUI this component is a part of to free up RAM.
	 */
    public static void clearModelCaches(){
    	modelParsedVertexLists.values().forEach(vertexID -> InterfaceRender.deleteVertices(vertexID));
    	modelParsedVertexLists.clear();
    }
}
