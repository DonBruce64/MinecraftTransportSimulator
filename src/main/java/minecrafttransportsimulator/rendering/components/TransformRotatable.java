package minecrafttransportsimulator.rendering.components;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Vector3f;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleRotatableModelObject;
import minecrafttransportsimulator.rendering.vehicles.RenderAnimations;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APart;

/**This class represents a rotatable object of a model.  Inputs are the name of the name model,
 * the name of the object, and the  {@link VehicleRotatableModelObject}s that are on the current 
 * part or vehicle that this model is from.
 *
 * @author don_bruce
 */
public class TransformRotatable extends ARenderableTransform{
	private final Vector3f[] rotationPoints;
	private final Vector3f[] rotationAxis;
	private final Float[] rotationMagnitudes;
	private final String[] rotationVariables;
	private final Float[] rotationClampsMin;
	private final Float[] rotationClampsMax;
	private final Boolean[] rotationAbsolutes;
	
	public TransformRotatable(String modelName, String objectName, List<VehicleRotatableModelObject> rotatableModelObjects){
		//Get all rotation points from the passed-in rotatableModelObjects.
		//We put these in lists for now as we don't know how many we will have.
		List<Vector3f> rotationPointsList = new ArrayList<Vector3f>();
		List<Vector3f> rotationAxisList = new ArrayList<Vector3f>();
		List<Float> rotationMagnitudesList = new ArrayList<Float>();
		List<String> rotationVariablesList = new ArrayList<String>();
		List<Float> rotationClampsMinList = new ArrayList<Float>();
		List<Float> rotationClampsMaxList = new ArrayList<Float>();
		List<Boolean> rotationAbsolutesList = new ArrayList<Boolean>();
		for(VehicleRotatableModelObject rotatable : rotatableModelObjects){
			if(rotatable != null && rotatable.partName.equals(objectName)){
				if(rotatable.rotationPoint != null){
					rotationPointsList.add(new Vector3f(rotatable.rotationPoint[0], rotatable.rotationPoint[1], rotatable.rotationPoint[2]));
				}else{
					throw new NullPointerException("ERROR: Attempted to make rotatable object:" + objectName + " from model:" + modelName + ", but no rotationPoint is in the JSON!");
				}
				if(rotatable.rotationAxis != null){
					//For the axis defined in the JSON, the axis is the normalized value of the defined vector, while the 
					//rotation magnitude is the magnitude of that vector.
					Vector3f axisVector = new Vector3f(rotatable.rotationAxis[0], rotatable.rotationAxis[1], rotatable.rotationAxis[2]);
					rotationMagnitudesList.add(axisVector.length());
					axisVector.normalize();
					rotationAxisList.add(axisVector);
				}else{
					throw new NullPointerException("ERROR: Attempted to make rotatable object:" + objectName + " from model:" + modelName + ", but no rotationAxis is in the JSON!");
				}
				if(rotatable.rotationVariable != null){
					rotationVariablesList.add(rotatable.rotationVariable.toLowerCase());
				}else{
					throw new NullPointerException("ERROR: Attempted to make rotatable object:" + objectName + " from model:" + modelName + ", but no rotationVariable is in the JSON!");
				}
				rotationClampsMinList.add(rotatable.rotationClampMin);
				rotationClampsMaxList.add(rotatable.rotationClampMax);
				rotationAbsolutesList.add(rotatable.absoluteValue);
			}
		}
		
		//Covert lists to arrays.  This allows for easier indexing later.
		this.rotationPoints = rotationPointsList.toArray(new Vector3f[rotationPointsList.size()]);
		this.rotationAxis = rotationAxisList.toArray(new Vector3f[rotationAxisList.size()]);
		this.rotationMagnitudes = rotationMagnitudesList.toArray(new Float[rotationMagnitudesList.size()]);
		this.rotationVariables = rotationVariablesList.toArray(new String[rotationVariablesList.size()]);
		this.rotationClampsMin = rotationClampsMinList.toArray(new Float[rotationClampsMinList.size()]);
		this.rotationClampsMax = rotationClampsMaxList.toArray(new Float[rotationClampsMaxList.size()]);
		this.rotationAbsolutes = rotationAbsolutesList.toArray(new Boolean[rotationAbsolutesList.size()]);
	}
	
	/**
	 *  This method rotates this part based on the part's parameters.
	 *  No rendering is performed.  This allows for rotatable parts
	 *  to be used as rotation helper classes in addition to actual
	 *  rotatable renderable parts.
	 */
	public void rotate(EntityVehicleE_Powered vehicle, APart optionalPart, float partialTicks){
		
	}

	@Override
	public void applyTransforms(EntityVehicleE_Powered vehicle, APart optionalPart, float partialTicks){
		//We need to define the rotation out here in case we encounter rotation definitions in sequence.
		//If that is the case, we can skip all but the last definition to save on rotation calls.
		//This also allows for multi-variable clamping.
		double rotation = 0;
		for(byte i=0; i<rotationVariables.length; ++i){
			rotation = RenderAnimations.getVariableValue(rotationVariables[i], rotationMagnitudes[i], (float) rotation, rotationClampsMin[i], rotationClampsMax[i], rotationAbsolutes[i], partialTicks, vehicle, optionalPart);
			//If the next definition is the same point, don't apply rotation yet.
			//We need to get cumulative rotation.
			if(i + 1 < rotationVariables.length && rotationPoints[i].equals(rotationPoints[i + 1])){
				if(rotationAxis[i].equals(rotationAxis[i + 1])){
					continue;
				}
			}else if(rotation != 0){
				GL11.glTranslated(rotationPoints[i].x, rotationPoints[i].y, rotationPoints[i].z);
				GL11.glRotated(rotation, rotationAxis[i].x, rotationAxis[i].y, rotationAxis[i].z);
				GL11.glTranslated(-rotationPoints[i].x, -rotationPoints[i].y, -rotationPoints[i].z);
				rotation = 0;
			}
		}
	}
}
