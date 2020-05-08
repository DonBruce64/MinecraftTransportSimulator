package minecrafttransportsimulator.guis.components;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.systems.OBJParserSystem;

/**Custom OBJ render class.  This allows for rendering an OBJ model into a GUI.
 * Mainly used to render vehicles, though can be used for any OBJ model.
 * This class keeps a list of all parsed models as DisplayLists for caching, which
 * are cleared when the GUI containing this component is un-loaded.  Note that the
 * model and texture associated with this component may change while this component
 * is still active.  This is to allows us to use one component to render changing
 * OBJ models, say in a crafting bench for instance.  The same reasoning applies
 * for why the position is not static (though it is required at construction).
 *
 * @author don_bruce
 */
public class GUIComponentOBJModel{
	/**Display list GL integers.  Keyed by model name.*/
	private static final Map<String, Integer> modelDisplayLists = new HashMap<String, Integer>();
	private static final Map<String, Float> modelScalingFactors = new HashMap<String, Float>();
	
	public final float scaleFactor;
	public final boolean isometric;
	public final boolean staticScaling;
	
	public int x;
	public int y;
	public boolean spin;
	public float scale;
	public String modelDomain;
	public String modelLocation;
	public String textureDomain;
	public String textureLocation;
	
	public boolean visible = true;
	    	
	public GUIComponentOBJModel(int x, int y, float scaleFactor, boolean isometric, boolean spin, boolean staticScaling){
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
			if(modelDomain != null){
				String modelFile = modelDomain + ":" + modelLocation;
				if(!modelDisplayLists.containsKey(modelFile)){
					parseModel(modelDomain, modelLocation);
				}
				GL11.glPushMatrix();
				//Translate to position and rotate to isometric view if required.
				GL11.glTranslatef(x, y, 100);
				GL11.glRotatef(180, 0, 0, 1);
				if(isometric){
					GL11.glRotatef(45, 0, 1, 0);
					GL11.glRotatef(35.264F, 1, 0, 1);
				}
				
				//If set to rotate, do so now based on time.
				if(spin){
					GL11.glRotatef((36*System.currentTimeMillis()/1000)%360, 0, 1, 0);
				}

				//Scale based on our scaling factor and render.
				if(!staticScaling){
					scale = modelScalingFactors.get(modelFile);
				}
				GL11.glScalef(scale*scaleFactor, scale*scaleFactor, scale*scaleFactor);
				GL11.glCallList(modelDisplayLists.get(modelFile));
				GL11.glPopMatrix();
			}
		}
    }
    
    /**
	 *  Parses the OBJ model.  This also gets centering information for rendering.
	 */
    private static void parseModel(String modelDomain, String modelLocation){
		float minX = 999;
		float maxX = -999;
		float minY = 999;
		float maxY = -999;
		float minZ = 999;
		float maxZ = -999;
		Map<String, Float[][]> parsedModel = OBJParserSystem.parseOBJModel(modelDomain, modelLocation);
		int displayListIndex = GL11.glGenLists(1);
		GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
		GL11.glBegin(GL11.GL_TRIANGLES);
		for(Entry<String, Float[][]> entry : parsedModel.entrySet()){
			for(Float[] vertex : entry.getValue()){
				GL11.glTexCoord2f(vertex[3], vertex[4]);
				GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
				GL11.glVertex3f(-vertex[0], vertex[1], vertex[2]);
				minX = Math.min(minX, vertex[0]);
				maxX = Math.max(maxX, vertex[0]);
				minY = Math.min(minY, vertex[1]);
				maxY = Math.max(maxY, vertex[1]);
				minZ = Math.min(minZ, vertex[2]);
				maxZ = Math.max(maxZ, vertex[2]);
			}
		}
		float globalMax = Math.max(Math.max(maxX - minX, maxY - minY), maxZ - minZ);
		modelScalingFactors.put(modelDomain + ":" + modelLocation, globalMax > 1.5 ? 1.5F/globalMax : 1.0F);
		GL11.glEnd();
		GL11.glEndList();
		modelDisplayLists.put(modelDomain + ":" + modelLocation, displayListIndex);
	}
    
    /**
	 *  Clear the DisplayList caches.  Call this when closing the GUI this component is a part of to free up RAM.
	 */
    public static void clearDisplayListCaches(){
    	for(int displayListID : modelDisplayLists.values()){
			GL11.glDeleteLists(displayListID, 1);
		}
    	modelDisplayLists.clear();
    }
}
