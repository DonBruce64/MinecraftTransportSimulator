package minecraftflightsimulator.models;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class ModelPlane extends ModelBase{
    protected static final float scale=0.0625F;
    protected ModelRenderer leftAileron;
    protected ModelRenderer rightAileron;
    protected ModelRenderer leftFlap;
    protected ModelRenderer rightFlap;
    protected ModelRenderer leftElevator;
    protected ModelRenderer rightElevator;
    protected ModelRenderer rudder;
    
    public static final ResourceLocation windowTexture = new ResourceLocation("minecraft", "textures/blocks/glass.png");

    public abstract void renderPlane(TextureManager renderEngine, byte textureCode, float aileronAngle, float elevatorAngle, float rudderAngle, float flapAngle);
    
    protected void renderAilerons(float aileronAngle){
    	leftAileron.rotateAngleX = -aileronAngle;
    	rightAileron.rotateAngleX = aileronAngle;
    	leftAileron.render(scale);
    	rightAileron.render(scale);
    }
    
    protected void renderElevators(float elevatorAngle){
    	leftElevator.rotateAngleX=elevatorAngle;
    	rightElevator.rotateAngleX=elevatorAngle;
    	leftElevator.render(scale);
    	rightElevator.render(scale);
    }
    
    protected void renderRudder(float rudderAngle){
    	rudder.rotateAngleY=rudderAngle;
    	rudder.render(scale);
    }
    
    protected void renderFlaps(float flapAngle){
    	leftFlap.rotateAngleX = -flapAngle;
    	rightFlap.rotateAngleX = -flapAngle;
    	leftFlap.render(scale);
    	rightFlap.render(scale);
    }
}