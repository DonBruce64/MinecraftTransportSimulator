package minecrafttransportsimulator.rendering.vehicles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleRotatableModelObject;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APart;
import net.minecraft.util.math.Vec3d;

/**This class represents a rotatable part on a vehicle.  Inputs are the name of the name part,
 * all vertices that make up the part, and a list of all {@link VehicleRotatableModelObject}s that
 * are on the current vehicle.  This allows us to link one of the items in the list to this part.
 *
 * @author don_bruce
 */
public final class RenderVehicle_RotatablePart{
	public final String name;
	
	private final String uniqueModelName;
	private final Float[][] vertices;
	private final Vec3d[] rotationPoints;
	private final Vec3d[] rotationAxis;
	private final Float[] rotationMagnitudes;
	private final String[] rotationVariables;
	
	private static final Map<String, Integer> partDisplayLists = new HashMap<String, Integer>();
	
	public RenderVehicle_RotatablePart(String name, Float[][] vertices, String modelName, List<VehicleRotatableModelObject> rotatableModelObjects){
		this.name = name;
		this.uniqueModelName = modelName + "_" + name;
		this.vertices = vertices;
		
		//Get all rotation points from the passed-in rotatableModelObjects.
		//We put these in lists for now as we don't know how many we will have.
		List<Vec3d> rotationPointsList = new ArrayList<Vec3d>();
		List<Vec3d> rotationAxisList = new ArrayList<Vec3d>();
		List<Float> rotationMagnitudesList = new ArrayList<Float>();
		List<String> rotationVariablesList = new ArrayList<String>();
		for(VehicleRotatableModelObject rotatable : rotatableModelObjects){
			if(rotatable != null && rotatable.partName.equals(this.name)){
				if(rotatable.rotationPoint != null){
					rotationPointsList.add(new Vec3d(rotatable.rotationPoint[0], rotatable.rotationPoint[1], rotatable.rotationPoint[2]));
				}else{
					throw new NullPointerException("ERROR: Rotatable part definition:" + this.name + " is missing a rotationPoint in the vehicle JSON!");
				}
				if(rotatable.rotationAxis != null){
					//For the axis defined in the JSON, the axis is the normalized value of the defined vector, while the 
					//rotation magnitude is the magnitude of that vector.
					rotationAxisList.add(new Vec3d(rotatable.rotationAxis[0], rotatable.rotationAxis[1], rotatable.rotationAxis[2]).normalize());
					rotationMagnitudesList.add((float) new Vec3d(rotatable.rotationAxis[0], rotatable.rotationAxis[1], rotatable.rotationAxis[2]).lengthVector());
				}else{
					throw new NullPointerException("ERROR: Rotatable part definition:" + this.name + " is missing a rotationAxis in the vehicle JSON!");
				}
				if(rotatable.rotationVariable != null){
					rotationVariablesList.add(rotatable.rotationVariable.toLowerCase());
				}else{
					throw new NullPointerException("ERROR: Rotatable part definition:" + this.name + " is missing a rotationVariable in the vehicle JSON!");
				}
			}
		}
		
		//Covert lists to arrays.  This allows for easier indexing later.
		this.rotationPoints = rotationPointsList.toArray(new Vec3d[rotationPointsList.size()]);
		this.rotationAxis = rotationAxisList.toArray(new Vec3d[rotationAxisList.size()]);
		this.rotationMagnitudes = rotationMagnitudesList.toArray(new Float[rotationMagnitudesList.size()]);
		this.rotationVariables = rotationVariablesList.toArray(new String[rotationVariablesList.size()]);
	}
	
	/**
	 *  This method rotates this part based on the part's parameters.
	 *  No rendering is performed.  This allows for rotatable parts
	 *  to be used as rotation helper classes in addition to actual
	 *  rotatable renderable parts.
	 */
	public void rotate(EntityVehicleE_Powered vehicle, APart<? extends EntityVehicleE_Powered> optionalPart, float partialTicks){
		for(byte i=0; i<rotationVariables.length; ++i){
			double rotation = RenderAnimations.getVariableValue(rotationVariables[i], partialTicks, vehicle, optionalPart);
			if(rotation != 0){
				GL11.glTranslated(rotationPoints[i].x, rotationPoints[i].y, rotationPoints[i].z);
				GL11.glRotated(rotation*rotationMagnitudes[i], rotationAxis[i].x, rotationAxis[i].y, rotationAxis[i].z);
				GL11.glTranslated(-rotationPoints[i].x, -rotationPoints[i].y, -rotationPoints[i].z);
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
		//Rotate prior to rendering.
		rotate(vehicle, optionalPart, partialTicks);
		
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
	
	/**
	 *  This method creates an {@link RenderVehicle_TreadRoller} class from this RotatablePart.
	 *  Used for vehicle treads in the auto configuration.
	 */
	public RenderVehicle_TreadRoller createTreadRoller(){
		double minY = 999;
		double maxY = -999;
		double minZ = 999;
		double maxZ = -999;
		for(Float[] point : vertices){
			minY = Math.min(minY, point[1]);
			maxY = Math.max(maxY, point[1]);
			minZ = Math.min(minZ, point[2]);
			maxZ = Math.max(maxZ, point[2]);
		}
		return new RenderVehicle_TreadRoller(this, minY, maxY, minZ, maxZ);
	}
}
