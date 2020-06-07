package minecrafttransportsimulator.rendering.components;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Vector3f;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleTranslatableModelObject;
import minecrafttransportsimulator.rendering.vehicles.RenderAnimations;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APart;

/**This class represents a translatable object of a model.  Inputs are the name of the name model,
 * the name of the object, and the  {@link VehicleTranslatableModelObject}s that are on the current 
 * part or vehicle that this model is from.
 *
 * @author don_bruce
 */
public class TransformTranslatable extends ARenderableTransform{
	private final Vector3f[] translationAxis;
	private final Float[] translationMagnitudes;
	private final String[] translationVariables;
	private final Float[] translationClampsMin;
	private final Float[] translationClampsMax;
	private final Boolean[] translationAbsolutes;
	
	public TransformTranslatable(String modelName, String objectName, List<VehicleTranslatableModelObject> translatableModelObjects){
		//Get all translation points from the passed-in translatableModelObjects.
		//We put these in lists for now as we don't know how many we will have.
		List<Vector3f> translationAxisList = new ArrayList<Vector3f>();
		List<Float> translationMagnitudesList = new ArrayList<Float>();
		List<String> translationVariablesList = new ArrayList<String>();
		List<Float> translationClampsMinList = new ArrayList<Float>();
		List<Float> translationClampsMaxList = new ArrayList<Float>();
		List<Boolean> translationAbsolutesList = new ArrayList<Boolean>();
		for(VehicleTranslatableModelObject translatable : translatableModelObjects){
			if(translatable != null && translatable.partName.equals(objectName)){
				if(translatable.translationAxis != null){
					//For the axis defined in the JSON, the axis is the normalized value of the defined vector, while the 
					//rotation magnitude is the magnitude of that vector.
					Vector3f axisVector = new Vector3f(translatable.translationAxis[0], translatable.translationAxis[1], translatable.translationAxis[2]);
					translationMagnitudesList.add(axisVector.length());
					axisVector.normalize();
					translationAxisList.add(axisVector);
				}else{
					throw new NullPointerException("ERROR: Attempted to make translatable object:" + objectName + " from model:" + modelName + ", but no translationAxis is in the JSON!");
				}
				if(translatable.translationVariable != null){
					translationVariablesList.add(translatable.translationVariable.toLowerCase());
				}else{
					throw new NullPointerException("ERROR: Attempted to make translatable object:" + objectName + " from model:" + modelName + ", but no translationVariable is in the JSON!");
				}
				translationClampsMinList.add(translatable.translationClampMin);
				translationClampsMaxList.add(translatable.translationClampMax);
				translationAbsolutesList.add(translatable.absoluteValue);
			}
		}
		
		//Covert lists to arrays.  This allows for easier indexing later.
		this.translationAxis = translationAxisList.toArray(new Vector3f[translationAxisList.size()]);
		this.translationMagnitudes = translationMagnitudesList.toArray(new Float[translationMagnitudesList.size()]);
		this.translationVariables = translationVariablesList.toArray(new String[translationVariablesList.size()]);
		this.translationClampsMin = translationClampsMinList.toArray(new Float[translationClampsMinList.size()]);
		this.translationClampsMax = translationClampsMaxList.toArray(new Float[translationClampsMaxList.size()]);
		this.translationAbsolutes = translationAbsolutesList.toArray(new Boolean[translationAbsolutesList.size()]);
	}

	@Override
	public void applyTransforms(EntityVehicleE_Powered vehicle, APart optionalPart, float partialTicks){
		for(byte i=0; i<translationVariables.length; ++i){
			double translation = RenderAnimations.getVariableValue(translationVariables[i], translationMagnitudes[i], 0F, translationClampsMin[i], translationClampsMax[i], translationAbsolutes[i], partialTicks, vehicle, null);
			if(translation != 0){
				GL11.glTranslated(translation*translationAxis[i].x, translation*translationAxis[i].y, translation*translationAxis[i].z);
			}
		}
	}
}
