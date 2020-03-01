package minecrafttransportsimulator.rendering.vehicles;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered.LightType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.client.MinecraftForgeClient;

/**This class represents a lighted part on a vehicle.  Inputs are the name of the lighted parts,
 * and all vertices that make up the part.
 *
 * @author don_bruce
 */
public final class RenderVehicle_LightPart{
	private static final ResourceLocation vanillaGlassTexture = new ResourceLocation("minecraft", "textures/blocks/glass.png");
	private static final ResourceLocation lensFlareTexture = new ResourceLocation(MTS.MODID, "textures/rendering/lensflare.png");
	private static final ResourceLocation lightTexture = new ResourceLocation(MTS.MODID, "textures/rendering/light.png");
	private static final ResourceLocation lightBeamTexture = new ResourceLocation(MTS.MODID, "textures/rendering/lightbeam.png");
	
	public final String name;
	public final LightType type;
	
	private final Color color;
	private final int flashBits;
	private final boolean renderFlare;
	private final boolean renderColor;
	private final boolean renderCover;
	
	private final Float[][] vertices;
	private final Vec3d[] centerPoints;
	private final Float[] size;
	
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
		
		this.vertices = new Float[masterVertices.length][];
		this.centerPoints = new Vec3d[masterVertices.length/6];
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
					case(3): newVertex[3] = 1.0F; newVertex[4] = 1.0F; break;
					case(4): newVertex[3] = 1.0F; newVertex[4] = 0.0F; break;
					case(5): newVertex[3] = 0.0F; newVertex[4] = 0.0F; break;
				}
				newVertex[5] = masterVertex[5];
				newVertex[6] = masterVertex[6];
				newVertex[7] = masterVertex[7];
				
				this.vertices[((short) i)*6 + j] = newVertex;
			}
			this.centerPoints[i] = new Vec3d(minX + (maxX - minX)/2D, minY + (maxY - minY)/2D, minZ + (maxZ - minZ)/2D);
			this.size[i] = (float) Math.max(Math.max(maxX - minX, maxZ - minZ), maxY - minY)*32F;
		}
	}
	
	/**
	 *  Renders this light based on the state of the vehicle and whether or not it was rendered in a prior pass.
	 *  This method needs to know the brightness of the sun and blocks to calculate the brightness of the lights.
	 *  It calculates this based on the environment of the passed-in vehicle.  It also uses the vehicle's electric
	 *  power level to determine brightness.  Rendering is done in all passes.
	 */
	public void render(EntityVehicleE_Powered vehicle, boolean wasRenderedPrior){
		boolean lightActuallyOn = isLightActuallyOn(vehicle);
		float sunLight = vehicle.world.getSunBrightness(0)*vehicle.world.getLightBrightness(vehicle.getPosition());
		float blockLight = vehicle.world.getLightFromNeighborsFor(EnumSkyBlock.BLOCK, vehicle.getPosition())/15F;
		//Lights start dimming due to low power at 8V.
		float electricFactor = (float) Math.min(vehicle.electricPower > 2 ? (vehicle.electricPower-2)/6F : 0, 1);
		//Max brightness occurs when ambient light is 0 and we have at least 8V power.
		float lightBrightness = (float) Math.min((1 - Math.max(sunLight, blockLight))*electricFactor, 1);

		//Render the color and cover in pass 0 or -1 as we don't want blending.
		if(MinecraftForgeClient.getRenderPass() != 1 && !wasRenderedPrior){
			
			//Render the color portion of the light if required and we have power.
			//We use electricFactor as color shows up even in daylight.
			if(renderColor && lightActuallyOn && electricFactor > 0){
				renderColor(electricFactor);
			}
			
			//Render the cover portion of this light if required.
			//If the light is on, and the vehicle has power, we want to make the cover bright.
			if(renderCover){
				renderCover(lightActuallyOn && electricFactor > 0);
			}
		}
		
		//If we need to render a flare, and the light is on, and our brightness is non-zero, do so now..
		//This needs to be done in pass 1 or -1 to do blending.
		if(renderFlare && lightActuallyOn && lightBrightness > 0 && MinecraftForgeClient.getRenderPass() != 0 && !wasRenderedPrior){
			renderFlare(lightBrightness);
		}
		
		//Render beam if the light is on and the brightness is non-zero.
		//This must be done in pass -1 to do proper blending.
		if(lightActuallyOn && MinecraftForgeClient.getRenderPass() == -1){
			renderBeam(Math.min(vehicle.electricPower > 4 ? 1.0F : 0, lightBrightness/2F));
		}
	}
	
	/**
	 *  Returns true if this light is actually on.  This takes into account the flashing
	 *  bit portion of the light as well as if the light is set to be on in the vehicle.
	 */
	private boolean isLightActuallyOn(EntityVehicleE_Powered vehicle){
		//Fun with bit shifting!  20 bits make up the light on index here, so align to a 20 tick cycle.
		return vehicle.isLightOn(type) ? ((flashBits >> vehicle.world.getTotalWorldTime()%20) & 1) > 0 : false;
	}
	
	/**
	 *  Renders the solid color portion of this light, if so configured.
	 *  Parameter is the alpha value for the light.
	 */
	private void renderColor(float alphaValue){
		Minecraft.getMinecraft().getTextureManager().bindTexture(lightTexture);
		Minecraft.getMinecraft().entityRenderer.disableLightmap();
		GL11.glColor4f(color.getRed()/255F, color.getGreen()/255F, color.getBlue()/255F, alphaValue);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glBegin(GL11.GL_TRIANGLES);
		for(Float[] vertex : vertices){
			//Add a slight translation and scaling to the light coords based on the normals to make the light
			//a little bit off of the main shape.  Prevents z-fighting.
			GL11.glTexCoord2f(vertex[3], vertex[4]);
			GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
			GL11.glVertex3f(vertex[0]+vertex[5]*0.0001F, vertex[1]+vertex[6]*0.0001F, vertex[2]+vertex[7]*0.0001F);	
		}
		GL11.glEnd();
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glColor4f(1, 1, 1, 1);
		Minecraft.getMinecraft().entityRenderer.enableLightmap();
	}
	
	/**
	 *  Renders the cover of this light, if so configured.  Parameter
	 *  passed-in will disable lighting for the cover if true.
	 */
	private void renderCover(boolean disableLighting){
		Minecraft.getMinecraft().getTextureManager().bindTexture(vanillaGlassTexture);
		if(disableLighting){
			GL11.glDisable(GL11.GL_LIGHTING);
			Minecraft.getMinecraft().entityRenderer.disableLightmap();
		}
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glBegin(GL11.GL_TRIANGLES);
		for(Float[] vertex : vertices){
			//Add a slight translation and scaling to the cover coords based on the normals to make the light
			//a little bit off of the main shape.  Prevents z-fighting.
			GL11.glTexCoord2f(vertex[3], vertex[4]);
			GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
			GL11.glVertex3f(vertex[0]+vertex[5]*0.0003F, vertex[1]+vertex[6]*0.0003F, vertex[2]+vertex[7]*0.0003F);	
		}
		GL11.glEnd();
		if(disableLighting){
			GL11.glEnable(GL11.GL_LIGHTING);
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
		}
	}
	
	/**
	 *  Renders the flare portion of this light, if so configured.
	 *  Parameter is the alpha value for the light.  Need to disable
	 *  both lighting and lightmap here to prevent the flare from being dim.
	 */
	private void renderFlare(float alphaValue){
		Minecraft.getMinecraft().getTextureManager().bindTexture(lensFlareTexture);
		Minecraft.getMinecraft().entityRenderer.disableLightmap();
		GL11.glColor4f(color.getRed()/255F, color.getGreen()/255F, color.getBlue()/255F, alphaValue);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glBegin(GL11.GL_TRIANGLES);
		for(byte i=0; i<centerPoints.length; ++i){
			for(byte j=0; j<6; ++j){
				Float[] vertex = vertices[((short) i)*6+j];
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
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glColor4f(1, 1, 1, 1);
		Minecraft.getMinecraft().entityRenderer.enableLightmap();
	}
	
	/**
	 *  Renders the beam portion of this light, if so configured.
	 *  Parameter is the alpha value for the light.
	 */
	private void renderBeam(float alphaValue){
		if(type.hasBeam){
			Minecraft.getMinecraft().entityRenderer.disableLightmap();
			Minecraft.getMinecraft().getTextureManager().bindTexture(lightBeamTexture);
			GL11.glPushMatrix();
	    	GL11.glDisable(GL11.GL_LIGHTING);
	    	GL11.glEnable(GL11.GL_BLEND);
	    	GL11.glColor4f(1, 1, 1, alphaValue);
	    	//Allows making things brighter by using alpha blending.
	    	GL11.glDepthMask(false);
	    	GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_ALPHA);
			
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
				GL11.glDepthMask(false);
				for(byte j=0; j<=2; ++j){
		    		drawLightCone(size[i], false);
		    	}
				drawLightCone(size[i], true);
				GL11.glPopMatrix();
			}
	    	GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	    	GL11.glDepthMask(true);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glPopMatrix();
			GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glColor4f(1, 1, 1, 1);
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
		}
	}
	
	/**
	 *  Helper method to draw a light cone for the beam rendering.
	 */
	private static void drawLightCone(double radius, boolean reverse){
		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		GL11.glTexCoord2f(0, 0);
		GL11.glVertex3d(0, 0, 0);
		if(reverse){
			for(float theta=0; theta < 2*Math.PI + 0.1; theta += 2F*Math.PI/40F){
				GL11.glTexCoord2f(theta, 1);
				GL11.glVertex3d(radius*Math.cos(theta), radius*Math.sin(theta), radius*3F);
			}
		}else{
			for(float theta=(float) (2*Math.PI); theta>=0 - 0.1; theta -= 2F*Math.PI/40F){
				GL11.glTexCoord2f(theta, 1);
				GL11.glVertex3d(radius*Math.cos(theta), radius*Math.sin(theta), radius*3F);
			}
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
