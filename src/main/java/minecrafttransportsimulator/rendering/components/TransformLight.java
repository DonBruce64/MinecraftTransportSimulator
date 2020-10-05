package minecrafttransportsimulator.rendering.components;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;

/**This class represents a light object of a model.  Inputs are the name of the name model
* and the name of the light.
*
* @author don_bruce
*/
public class TransformLight extends ATransformRenderable{
	public final LightType type;
	public final boolean isLightupTexture;
	
	private final Color color;
	private final int flashBits;
	private final boolean renderFlare;
	private final boolean renderColor;
	private final boolean renderCover;
	private final boolean renderBeam;
	
	private final Float[][] vertices;
	private final Point3d[] centerPoints;
	private final Float[] size;
	
	public TransformLight(String modelName, String objectName, Float[][] masterVertices){
		super(null);
		this.type = getTypeFromName(objectName);
		//Lights are in the format of "&NAME_XXXXXX_YYYYY_ZZZZ"
		//Where NAME is what switch it goes to.
		//XXXXXX is the color.
		//YYYYY is the blink rate.
		//ZZZZ is the light type.  The first bit renders the flare, the second the color, the third the cover, and the fourth a beam.
		//Beam digit 4 may be omitted to use defaults, but if included will override the default.
		try{
			this.color = Color.decode("0x" + objectName.substring(objectName.indexOf('_') + 1, objectName.indexOf('_') + 7));
			this.flashBits = Integer.decode("0x" + objectName.substring(objectName.indexOf('_', objectName.indexOf('_') + 7) + 1, objectName.lastIndexOf('_')));
			String lightProperties = objectName.substring(objectName.lastIndexOf('_') + 1);
			this.renderFlare = Integer.valueOf(lightProperties.substring(0, 1)) > 0;
			this.renderColor = Integer.valueOf(lightProperties.substring(1, 2)) > 0;
			this.renderCover = Integer.valueOf(lightProperties.substring(2, 3)) > 0;
			this.renderBeam = lightProperties.length() == 4 ? Integer.valueOf(lightProperties.substring(3)) > 0 : type.hasBeam;
		}catch(Exception e){
			throw new NumberFormatException("ERROR: Attempted to parse light information from " + modelName + ":" + objectName + " but faulted.  This is likely due to a naming convention error.");
		}
		
		//If we need to render a flare, cover, or beam, calculate the center points and re-calculate the UV points.
		if(renderFlare || renderCover || renderBeam){
			this.vertices = new Float[masterVertices.length][];
			this.centerPoints = new Point3d[masterVertices.length/6];
			this.size = new Float[masterVertices.length/6];
			for(int i=0; i<centerPoints.length; ++i){
				double minX = 999;
				double maxX = -999;
				double minY = 999;
				double maxY = -999;
				double minZ = 999;
				double maxZ = -999;
				for(byte j=0; j<6; ++j){
					Float[] masterVertex = masterVertices[i*6 + j];
					minX = Math.min(masterVertex[0], minX);
					maxX = Math.max(masterVertex[0], maxX);
					minY = Math.min(masterVertex[1], minY);
					maxY = Math.max(masterVertex[1], maxY);
					minZ = Math.min(masterVertex[2], minZ);
					maxZ = Math.max(masterVertex[2], maxZ);
					
					Float[] newVertex = new Float[masterVertex.length];
					newVertex[0] = masterVertex[0];
					newVertex[1] = masterVertex[1];
					newVertex[2] = masterVertex[2];
					//Adjust UV point here to change this to glass coords.
					switch(j){
						case(0): newVertex[3] = 0.0F; newVertex[4] = 0.0F; break;
						case(1): newVertex[3] = 0.0F; newVertex[4] = 1.0F; break;
						case(2): newVertex[3] = 1.0F; newVertex[4] = 1.0F; break;
						case(3): newVertex[3] = 0.0F; newVertex[4] = 0.0F; break;
						case(4): newVertex[3] = 1.0F; newVertex[4] = 1.0F; break;
						case(5): newVertex[3] = 1.0F; newVertex[4] = 0.0F; break;
					}
					newVertex[5] = masterVertex[5];
					newVertex[6] = masterVertex[6];
					newVertex[7] = masterVertex[7];
					
					this.vertices[(i)*6 + j] = newVertex;
				}
				this.centerPoints[i] = new Point3d(minX + (maxX - minX)/2D, minY + (maxY - minY)/2D, minZ + (maxZ - minZ)/2D);
				this.size[i] = (float) Math.max(Math.max(maxX - minX, maxZ - minZ), maxY - minY)*32F;
			}
		}else{
			this.vertices = masterVertices;
			this.centerPoints = null;
			this.size = null;
		}
		
		//Set the light-up texture status.
		this.isLightupTexture = !renderColor && !renderFlare && !renderCover && !renderBeam;
	}

	@Override
	public double applyTransform(EntityVehicleF_Physics vehicle, APart optionalPart, float partialTicks, double offset){
		//If we are a light-up texture, disable lighting prior to the render call.
		//Lights start dimming due to low power at 8V.
		setLightupTextureState(vehicle.lightsOn.contains(type), (float) Math.min(vehicle.electricPower > 2 ? (vehicle.electricPower-2)/6F : 0, 1));
		return 0;
	}
	
	@Override
	public void doPostRenderLogic(EntityVehicleF_Physics vehicle, APart optionalPart, float partialTicks){
		//We cheat here and render our light bits at this point.
		//It's safe to do this, as we'll already have applied all the other transforms we need, and
		//we'll have rendered the object so we can safely change textures.
		//We won't have to worry about the light-up textures, as those lighting changes will be overidden here.
		boolean lightActuallyOn = vehicle.lightsOn.contains(type) && isFlashingLightOn();
		float sunLight = vehicle.world.getLightBrightness(new Point3i(vehicle.position), false);
		//Lights start dimming due to low power at 8V.
		float electricFactor = (float) Math.min(vehicle.electricPower > 2 ? (vehicle.electricPower-2)/6F : 0, 1);
		//Max brightness occurs when ambient light is 0 and we have at least 8V power.
		float lightBrightness = Math.min((1 - sunLight)*electricFactor, 1);
		render(lightActuallyOn, (float) vehicle.electricPower, electricFactor, lightBrightness, ConfigSystem.configObject.client.vehicleBeams.value);
	}
	
	
	/**
	 *  Renders this light at a specific block-based position.  Full power and brightness is assumed.
	 */
	public void renderOnBlock(IWrapperWorld world, Point3i location, boolean lightActive){
		render(lightActive && isFlashingLightOn(), 12.0F, 1.0F, 1 - world.getLightBrightness(location, false), ConfigSystem.configObject.client.blockBeams.value);
	}
	
	/**
	 *  Renders this light based on the state of the lighting at the passed-in position.  This main call can be used for
	 *  multiple sources of light, not just vehicles.  Rendering is done in all passes, though -1 is a combination of 0 and 1.
	 */
	public void render(boolean lightOn, float electricPower, float electricFactor, float lightBrightness, boolean beamEnabled){
		//Render the texture, color, and cover in pass 0 or -1 as we don't want blending.
		if(MasterLoader.renderInterface.getRenderPass() != 1){
			//Render the color portion of the light if required and we have power.
			//We use electricFactor as color shows up even in daylight.
			if(renderColor && lightOn && electricFactor > 0){
				renderColor(electricFactor);
			}
			
			//Render the cover portion of this light if required.
			//If the light is on, and the vehicle has power, we want to make the cover bright.
			if(renderCover){
				renderCover(lightOn && electricFactor > 0);
			}
		}
		
		//Flag for flare and beam rendering.
		boolean doBlendRenders = lightBrightness > 0 && (ConfigSystem.configObject.client.lightsPass0.value ? MasterLoader.renderInterface.getRenderPass() != 1 : MasterLoader.renderInterface.getRenderPass() != 0); 
		
		//If we need to render a flare, and the light is on, and our brightness is non-zero, do so now.
		//This needs to be done in pass 1 or -1 to do blending.
		if(renderFlare && lightOn && doBlendRenders){
			renderFlare(lightBrightness);
		}
		
		//Render beam if the light is on and the brightness is non-zero.
		//This must be done in pass 1 or -1 to do proper blending.
		if(beamEnabled && renderBeam && lightOn && doBlendRenders){
			renderBeam(Math.min(electricPower > 4 ? 1.0F : 0, lightBrightness));
		}
		
		//Set color back to normal, turn off blending, turn on lighting, and un-bind the light textures.
		//This resets the operations in here for other transforms.
		MasterLoader.renderInterface.setColorState(1.0F, 1.0F, 1.0F, 1.0F);
		MasterLoader.renderInterface.setBlendState(false, false);
		MasterLoader.renderInterface.setLightingState(true);
		MasterLoader.renderInterface.recallTexture();
	}
	
	/**
	 *  Returns true if this light is actually on.  This takes into account the flashing
	 *  bit portion of the light.
	 */
	protected boolean isFlashingLightOn(){
		//Fun with bit shifting!  20 bits make up the light on index here, so align to a 20 tick cycle.
		return ((flashBits >> (20*System.currentTimeMillis()/1000)%20) & 1) > 0;
	}
	
	/**
	 *  Sets the lighting status for light-up texture rendering.  Has no effect if such rendering isn't part of this light.
	 */
	public void setLightupTextureState(boolean lightOn, float electricFactor){
		if(MasterLoader.renderInterface.getRenderPass() != 1 && isLightupTexture){
			MasterLoader.renderInterface.setLightingState(!(lightOn && isFlashingLightOn() && electricFactor > 0));
		}
	}
	
	/**
	 *  Renders the solid color portion of this light, if so configured.
	 *  Parameter is the alpha value for the light.
	 */
	private void renderColor(float alphaValue){
		MasterLoader.renderInterface.bindTexture("mts:textures/rendering/light.png");
		MasterLoader.renderInterface.setLightingState(false);
		MasterLoader.renderInterface.setColorState(color.getRed()/255F, color.getGreen()/255F, color.getBlue()/255F, alphaValue);
		GL11.glBegin(GL11.GL_TRIANGLES);
		for(Float[] vertex : vertices){
			//Add a slight translation and scaling to the light coords based on the normals to make the light
			//a little bit off of the main shape.  Prevents z-fighting.
			GL11.glTexCoord2f(vertex[3], vertex[4]);
			GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
			GL11.glVertex3f(vertex[0]+vertex[5]*0.0001F, vertex[1]+vertex[6]*0.0001F, vertex[2]+vertex[7]*0.0001F);	
		}
		GL11.glEnd();
	}
	
	/**
	 *  Renders the cover of this light, if so configured.  Parameter
	 *  passed-in will disable lighting for the cover if true.
	 */
	private void renderCover(boolean disableLighting){
		MasterLoader.renderInterface.bindTexture("minecraft:textures/blocks/glass.png");
		MasterLoader.renderInterface.setLightingState(!disableLighting);
		MasterLoader.renderInterface.setColorState(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glBegin(GL11.GL_TRIANGLES);
		for(Float[] vertex : vertices){
			//Add a slight translation and scaling to the cover coords based on the normals to make the light
			//a little bit off of the main shape.  Prevents z-fighting.
			GL11.glTexCoord2f(vertex[3], vertex[4]);
			GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
			GL11.glVertex3f(vertex[0]+vertex[5]*0.0003F, vertex[1]+vertex[6]*0.0003F, vertex[2]+vertex[7]*0.0003F);	
		}
		GL11.glEnd();
	}
	
	/**
	 *  Renders the flare portion of this light, if so configured.
	 *  Parameter is the alpha value for the light.  Need to disable
	 *  both lighting and lightmap here to prevent the flare from being dim.
	 */
	private void renderFlare(float alphaValue){
		MasterLoader.renderInterface.bindTexture("mts:textures/rendering/lensflare.png");
		MasterLoader.renderInterface.setLightingState(false);
		MasterLoader.renderInterface.setBlendState(true, ConfigSystem.configObject.client.flareBlending.value);
		MasterLoader.renderInterface.setColorState(color.getRed()/255F, color.getGreen()/255F, color.getBlue()/255F, alphaValue);
		GL11.glBegin(GL11.GL_TRIANGLES);
		for(int i=0; i<centerPoints.length; ++i){
			for(byte j=0; j<6; ++j){
				Float[] vertex = vertices[(i)*6+j];
				//Add a slight translation to the light size to make the flare move off it.
				//Then apply scaling factor to make the flare larger than the light.
				GL11.glTexCoord2f(vertex[3], vertex[4]);
				GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
				GL11.glVertex3d(vertex[0]+vertex[5]*0.0002F + (vertex[0] - centerPoints[i].x)*(2 + size[i]*0.25F), 
						vertex[1]+vertex[6]*0.0002F + (vertex[1] - centerPoints[i].y)*(2 + size[i]*0.25F), 
						vertex[2]+vertex[7]*0.0002F + (vertex[2] - centerPoints[i].z)*(2 + size[i]*0.25F));	
			}
		}
		GL11.glEnd();
	}
	
	/**
	 *  Renders the beam portion of this light, if so configured.
	 *  Parameter is the alpha value for the light.
	 */
	private void renderBeam(float alphaValue){
		MasterLoader.renderInterface.bindTexture("mts:textures/rendering/lightbeam.png");
		MasterLoader.renderInterface.setLightingState(false);
		MasterLoader.renderInterface.setBlendState(true, ConfigSystem.configObject.client.beamBlending.value);
		MasterLoader.renderInterface.setColorState(color.getRed()/255F, color.getGreen()/255F, color.getBlue()/255F, alphaValue);
		
		//As we can have more than one light per definition, we will only render 6 vertices at a time.
		//Use the center point arrays for this; normals are the same for all 6 vertex sets so use whichever.
		for(int i=0; i<centerPoints.length; ++i){
			GL11.glPushMatrix();
			//Translate light to the center of the cone beam.
			GL11.glTranslated(centerPoints[i].x - vertices[i*6][5]*0.15F, centerPoints[i].y - vertices[i*6][6]*0.15F, centerPoints[i].z - vertices[i*6][7]*0.15F);
			//Rotate beam to the normal face.
			GL11.glRotatef((float) Math.toDegrees(Math.atan2(vertices[i*6][6], vertices[i*6][5])), 0, 0, 1);
			GL11.glRotatef((float) Math.toDegrees(Math.acos(vertices[i*6][7])), 0, 1, 0);
			//Now draw the beam
			drawLightCone(size[i]);
			GL11.glPopMatrix();
		}
	}
	
	/**
	 *  Helper method to draw a light cone for the beam rendering.
	 *  Draws two outer and one inner cone.
	 */
	private static void drawLightCone(double radius){
		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		GL11.glTexCoord2f(0, 0);
		GL11.glVertex3d(0, 0, 0);
		for(float theta=(float) (2*Math.PI); theta>=0 - 0.1; theta -= 2F*Math.PI/40F){
			GL11.glTexCoord2f(theta, 1);
			GL11.glVertex3d(radius*Math.cos(theta), radius*Math.sin(theta), radius*3F);
		}
		for(float theta=0; theta < 2*Math.PI + 0.1; theta += 2F*Math.PI/40F){
			GL11.glTexCoord2f(theta, 1);
			GL11.glVertex3d(radius*Math.cos(theta), radius*Math.sin(theta), radius*3F);
		}
		GL11.glEnd();
	}
	
	/**
	 *  Helper method to get the {@link LightType} for this LightPart.
	 *  This allows easier static assignment.
	 */
	private static LightType getTypeFromName(String lightName){
		for(LightType light : LightType.values()){
			//Convert light name to uppercase to match enum name.
			if(lightName.toUpperCase().contains(light.name())){
				return light;
			}
		}
		throw new IllegalArgumentException("ERROR: Attempted to parse light:" + lightName + ", but no lights exist with this name.  Is this light name spelled correctly?");
	}
}
