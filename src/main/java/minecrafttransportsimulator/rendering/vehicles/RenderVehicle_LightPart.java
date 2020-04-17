package minecrafttransportsimulator.rendering.vehicles;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered.LightType;
import minecrafttransportsimulator.wrappers.WrapperRender;
import minecrafttransportsimulator.wrappers.WrapperWorld;
import net.minecraft.world.EnumSkyBlock;

/**This class represents a lighted part on a vehicle.  Inputs are the name of the lighted parts,
 * and all vertices that make up the part.
 *
 * @author don_bruce
 */
public final class RenderVehicle_LightPart{
	public final String name;
	public final LightType type;
	public final boolean isLightupTexture;
	
	private final Color color;
	private final int flashBits;
	private final boolean renderFlare;
	private final boolean renderColor;
	private final boolean renderCover;
	
	private final Float[][] vertices;
	private final Point3d[] centerPoints;
	private final Float[] size;
	
	//This can't be final as we'll do a double-display-list if we try to do this on construction.
	private int displayListIndex = -1;
	
	public RenderVehicle_LightPart(String name, Float[][] masterVertices){
		this.name = name;
		this.type = getTypeFromName(name);
		//Lights are in the format of "&NAME_XXXXXX_YYYYY_ZZZ"
		//Where NAME is what switch it goes to.
		//XXXXXX is the color.
		//YYYYY is the blink rate.
		//ZZZ is the light type.  The first bit renders the flare, the second the color, and the third the cover.
		try{
			this.color = Color.decode("0x" + name.substring(name.indexOf('_') + 1, name.indexOf('_') + 7));
			this.flashBits = Integer.decode("0x" + name.substring(name.indexOf('_', name.indexOf('_') + 7) + 1, name.lastIndexOf('_')));
			this.renderFlare = Integer.valueOf(name.substring(name.length() - 3, name.length() - 2)) > 0;
			this.renderColor = Integer.valueOf(name.substring(name.length() - 2, name.length() - 1)) > 0;
			this.renderCover = Integer.valueOf(name.substring(name.length() - 1)) > 0;
		}catch(Exception e){
			throw new NumberFormatException("ERROR: Attempted to parse light information from: " + this.name + " but faulted.  This is likely due to a naming convention error.");
		}
		
		
		//If we need to render a flare, cover, or beam, calculate the center points and re-calculate the UV points.
		if(renderFlare || renderCover || type.hasBeam){
			this.vertices = new Float[masterVertices.length][];
			this.centerPoints = new Point3d[masterVertices.length/6];
			this.size = new Float[masterVertices.length/6];
			for(short i=0; i<centerPoints.length; ++i){
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
		this.isLightupTexture = !renderColor && !renderFlare && !renderCover && !type.hasBeam;
	}
	
	/**
	 *  Renders this light for this vehicle.  This falls down to the method below for actual rendering.  Segmented
	 *  to allow for other things to use vehicle lighting code, such as blocks.
	 */
	public void renderOnVehicle(EntityVehicleE_Powered vehicle, boolean wasRenderedPrior, String textureDomain, String textureLocation){
		boolean lightActuallyOn = vehicle.lightsOn.contains(type) && isFlashingLightOn();
		float sunLight = vehicle.world.getSunBrightness(0)*(vehicle.world.getLightFor(EnumSkyBlock.SKY, vehicle.getPosition()) - vehicle.world.getSkylightSubtracted())/15F;
		//Lights start dimming due to low power at 8V.
		float electricFactor = (float) Math.min(vehicle.electricPower > 2 ? (vehicle.electricPower-2)/6F : 0, 1);
		//Max brightness occurs when ambient light is 0 and we have at least 8V power.
		float lightBrightness = Math.min((1 - sunLight)*electricFactor, 1);
		render(lightActuallyOn, wasRenderedPrior, (float) vehicle.electricPower, electricFactor, lightBrightness, textureDomain, textureLocation);
	}
	
	/**
	 *  Renders this light at a specific block-based position.  Full power and brightness is assumed.
	 */
	public void renderOnBlock(WrapperWorld world, Point3i location, boolean lightActive, String textureDomain, String textureLocation){
		render(lightActive && isFlashingLightOn(), WrapperRender.getRenderPass() == -1, 12.0F, 1.0F, 1 - world.getLightBrightness(location, false), textureDomain, textureLocation);
	}
	
	/**
	 *  Renders this light based on the state of the lighting at the passed-in position.  This main call can be used for
	 *  multiple sources of light, not just vehicles.  Rendering is done in all passes.
	 */
	public void render(boolean lightOn, boolean wasRenderedPrior, float electricPower, float electricFactor, float lightBrightness, String textureDomain, String textureLocation){
		//Render the texture, color, and cover in pass 0 or -1 as we don't want blending.
		if(WrapperRender.getRenderPass() != 1 && !wasRenderedPrior){
			//Render the texture if we are a light-up texture light.
			//Otherwise, don't render the texture here as it'll be in the main vehicle DisplayList.
			if(isLightupTexture){
				renderTexture(lightOn && electricFactor > 0, textureDomain, textureLocation);
			}
			
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
		boolean doBlendRenders = lightBrightness > 0 && (ConfigSystem.configObject.client.lightsPass0.value ? WrapperRender.getRenderPass() != 1 : WrapperRender.getRenderPass() != 0) && !wasRenderedPrior; 
		
		//If we need to render a flare, and the light is on, and our brightness is non-zero, do so now.
		//This needs to be done in pass 1 or -1 to do blending.
		if(renderFlare && lightOn && doBlendRenders){
			renderFlare(lightBrightness);
		}
		
		//Render beam if the light is on and the brightness is non-zero.
		//This must be done in pass 1 or -1 to do proper blending.
		if(type.hasBeam && lightOn && doBlendRenders){
			renderBeam(Math.min(electricPower > 4 ? 1.0F : 0, lightBrightness));
		}
		
		//Set color, lighting and blending state back to normal.
		WrapperRender.resetStates();
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
	 *  Renders the textured portion of this light.  All that really needs to be done here
	 *  is disabling lighting to make the texture be bright if we have enough electricity to do so.
	 */
	private void renderTexture(boolean disableLighting, String textureDomain, String textureLocation){
		WrapperRender.bindTexture(textureDomain, textureLocation);
		WrapperRender.setWorldLightingState(!disableLighting);
		WrapperRender.setColorState(1.0F, 1.0F, 1.0F, 1.0F);
		
		//If we don't have a DisplayList, create one now.
		if(displayListIndex == -1){
			displayListIndex = GL11.glGenLists(1);
			GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
			GL11.glBegin(GL11.GL_TRIANGLES);
			for(Float[] vertex : vertices){
				GL11.glTexCoord2f(vertex[3], vertex[4]);
				GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
				GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);	
			}
			GL11.glEnd();
			GL11.glEndList();
		}
		GL11.glCallList(displayListIndex);
	}
	
	/**
	 *  Renders the solid color portion of this light, if so configured.
	 *  Parameter is the alpha value for the light.
	 */
	private void renderColor(float alphaValue){
		WrapperRender.bindTexture(MTS.MODID, "textures/rendering/light.png");
		WrapperRender.setLightingState(false);
		WrapperRender.setColorState(color.getRed()/255F, color.getGreen()/255F, color.getBlue()/255F, alphaValue);
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
		WrapperRender.bindTexture("minecraft", "textures/blocks/glass.png");
		WrapperRender.setLightingState(!disableLighting);
		WrapperRender.setColorState(1.0F, 1.0F, 1.0F, 1.0F);
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
		WrapperRender.bindTexture(MTS.MODID, "textures/rendering/lensflare.png");
		WrapperRender.setLightingState(false);
		WrapperRender.setBlendState(true, ConfigSystem.configObject.client.flareBlending.value);
		WrapperRender.setColorState(color.getRed()/255F, color.getGreen()/255F, color.getBlue()/255F, alphaValue);
		GL11.glBegin(GL11.GL_TRIANGLES);
		for(byte i=0; i<centerPoints.length; ++i){
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
		WrapperRender.bindTexture(MTS.MODID, "textures/rendering/lightbeam.png");
		WrapperRender.setLightingState(false);
		WrapperRender.setBlendState(true, ConfigSystem.configObject.client.beamBlending.value);
		WrapperRender.setColorState(color.getRed()/255F, color.getGreen()/255F, color.getBlue()/255F, alphaValue);
		
		//As we can have more than one light per definition, we will only render 6 vertices at a time.
		//Use the center point arrays for this; normals are the same for all 6 vertex sets so use whichever.
		for(byte i=0; i<centerPoints.length; ++i){
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
