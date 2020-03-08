package minecrafttransportsimulator.rendering.vehicles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleTranslatableModelObject;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APart;
import net.minecraft.util.math.Vec3d;

/**This class represents a translatable part on a vehicle.  Inputs are the name of the name part,
 * all vertices that make up the part, and a list of all {@link VehicleTranslatableModelObject}s that
 * are on the current vehicle.  This allows us to link one of the items in the list to this part.
 *
 * @author don_bruce
 */
public final class RenderVehicle_TranslatablePart{
	public final String name;
	
	private final String uniqueModelName;
	private final Float[][] vertices;
	private final Vec3d[] translationAxis;
	private final Float[] translationMagnitudes;
	private final String[] translationVariables;
	private final Float[] translationClampsMin;
	private final Float[] translationClampsMax;
	
	private static final Map<String, Integer> partDisplayLists = new HashMap<String, Integer>();
	
	public RenderVehicle_TranslatablePart(String name, Float[][] vertices, String modelName, List<VehicleTranslatableModelObject> translatableModelObjects){
		this.name = name;
		this.uniqueModelName = modelName + "_" + name;
		this.vertices = vertices;
		
		//Get all translation points from the passed-in translatableModelObjects.
		//We put these in lists for now as we don't know how many we will have.
		List<Vec3d> translationAxisList = new ArrayList<Vec3d>();
		List<Float> translationMagnitudesList = new ArrayList<Float>();
		List<String> translationVariablesList = new ArrayList<String>();
		List<Float> translationClampsMinList = new ArrayList<Float>();
		List<Float> translationClampsMaxList = new ArrayList<Float>();
		for(VehicleTranslatableModelObject translatable : translatableModelObjects){
			if(translatable != null && translatable.partName.equals(this.name)){
				if(translatable.translationAxis != null){
					//For the axis defined in the JSON, the axis is the normalized value of the defined vector, while the 
					//rotation magnitude is the magnitude of that vector.
					translationAxisList.add(new Vec3d(translatable.translationAxis[0], translatable.translationAxis[1], translatable.translationAxis[2]).normalize());
					translationMagnitudesList.add((float) new Vec3d(translatable.translationAxis[0], translatable.translationAxis[1], translatable.translationAxis[2]).lengthVector());
				}else{
					throw new NullPointerException("ERROR: Translatable part definition:" + this.name + " is missing a translationAxis in the vehicle JSON!");
				}
				if(translatable.translationVariable != null){
					translationVariablesList.add(translatable.translationVariable.toLowerCase());
				}else{
					throw new NullPointerException("ERROR: Translatable part definition:" + this.name + " is missing a translationVariable in the vehicle JSON!");
				}
				translationClampsMinList.add(translatable.translationClampMin);
				translationClampsMaxList.add(translatable.translationClampMax);
			}
		}
		
		//Covert lists to arrays.  This allows for easier indexing later.
		this.translationAxis = translationAxisList.toArray(new Vec3d[translationAxisList.size()]);
		this.translationMagnitudes = translationMagnitudesList.toArray(new Float[translationMagnitudesList.size()]);
		this.translationVariables = translationVariablesList.toArray(new String[translationVariablesList.size()]);
		this.translationClampsMin = translationClampsMinList.toArray(new Float[translationClampsMinList.size()]);
		this.translationClampsMax = translationClampsMaxList.toArray(new Float[translationClampsMaxList.size()]);
	}
	
	/**
	 *  This method translates this part based on the part's parameters.
	 *  No rendering is performed.  This allows for translatable parts
	 *  to be used as translation helper classes in addition to actual
	 *  translatable renderable parts.
	 */
	public void translate(EntityVehicleE_Powered vehicle, APart<? extends EntityVehicleE_Powered> optionalPart, float partialTicks){
		for(byte i=0; i<translationVariables.length; ++i){
			double translation = RenderAnimations.getVariableValue(translationVariables[i], translationMagnitudes[i], 0F, translationClampsMin[i], translationClampsMax[i], partialTicks, vehicle, null);
			if(translation != 0){
				GL11.glTranslated(translation*translationAxis[i].x, translation*translationAxis[i].y, translation*translationAxis[i].z);
			}
		}
	}
	
	/**
	 *  This method renders this part based on the part's parameters.
	 *  This uses a displayList for efficiency.  This list uses the
	 *  part's name, as well as the name of the vehicle or part model
	 *  the part came from.  This is to prevent the issue of same-named
	 *  parts on two different models conflicting.
	 */
	public void render(EntityVehicleE_Powered vehicle, APart<? extends EntityVehicleE_Powered> optionalPart, float partialTicks){
		//Translate prior to rendering.
		translate(vehicle, optionalPart, partialTicks);
		
		//Now render, caching the displayList if needed.
		if(!partDisplayLists.containsKey(uniqueModelName)){
			int displayListIndex = GL11.glGenLists(1);
			GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
			GL11.glBegin(GL11.GL_TRIANGLES);
			for(Float[] vertex : vertices){
				GL11.glTexCoord2f(vertex[3], vertex[4]);
				GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
				GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
			}
			GL11.glEnd();
			GL11.glEndList();
			partDisplayLists.put(uniqueModelName, displayListIndex);
		}
		GL11.glCallList(partDisplayLists.get(uniqueModelName));
	}
	
	/**Used to clear out the rendering caches in dev mode to allow the re-loading of models.**/
	public void clearCaches(){
		if(partDisplayLists.containsKey(uniqueModelName)){
			GL11.glDeleteLists(partDisplayLists.get(uniqueModelName), 1);
		}
		partDisplayLists.remove(uniqueModelName);
	}
}
