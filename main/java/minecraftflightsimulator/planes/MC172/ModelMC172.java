package minecraftflightsimulator.planes.MC172;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ModelMC172 extends ModelBase{
	private static final float scale=0.0625F;
	
    private ModelRenderer frontFuselage;
    private ModelRenderer rearFuselage;
    private ModelRenderer leftRearFuselage;
    private ModelRenderer rightRearFuselage;
    private ModelRenderer bottomFuselage;
    private ModelRenderer noseLeft;
    private ModelRenderer noseRight;
    private ModelRenderer cowling;
    private ModelRenderer leftStrut;
    private ModelRenderer rightStrut;
    private ModelRenderer centerStrut;
    private ModelRenderer wing;
    private ModelRenderer leftWingSupport;
    private ModelRenderer rightWingSupport;
    private ModelRenderer tail;
    
    private ModelRenderer leftAileron;
    private ModelRenderer rightAileron;
    private ModelRenderer leftFlap;
    private ModelRenderer rightFlap;
    private ModelRenderer leftElevator;
    private ModelRenderer rightElevator;
    private ModelRenderer rudder;
 
    public ModelMC172(){
    	this.textureHeight = 16;
    	this.textureWidth = 16;
    	
      	frontFuselage = new ModelRenderer(this, 0, 0);
        frontFuselage.setTextureSize(textureWidth, textureHeight);
        frontFuselage.setRotationPoint(0F, -6F, 0F);
        frontFuselage.addBox(12F, 0F, -24F, 4, 16, 40);
        frontFuselage.addBox(-16F, 0F, -24F, 4, 16, 40);
        frontFuselage.addBox(-12F, 0.5F, 11.5F, 24, 14, 3);
        frontFuselage.addBox(-8F, 0F, 33.5F, 16, 15, 1);
        
        rearFuselage = new ModelRenderer(this, 0, 0);
        rearFuselage.setTextureSize(textureWidth, textureHeight);
        rearFuselage.setRotationPoint(0F, 8.1F, -24F);
        rearFuselage.addBox(-12F, 0F, -8F, 24, 2, 8);
        rearFuselage.addBox(-10F, 0F, -16F, 20, 2, 8);
        rearFuselage.addBox(-8F, 0F, -24F, 16, 2, 8);
        rearFuselage.addBox(-6F, 0F, -32F, 12, 2, 8);
        rearFuselage.addBox(-4F, 0F, -40F, 8, 2, 8);
        
        leftRearFuselage = new ModelRenderer(this, 0, 0);
    	leftRearFuselage.setTextureSize(textureWidth, textureHeight);
    	leftRearFuselage.setRotationPoint(16F, -6F, -24F);
    	leftRearFuselage.addBox(0F, 0F, 0F, 4, 16, 56);
    	leftRearFuselage.rotateAngleY=(float) Math.toRadians(195);
        
        rightRearFuselage = new ModelRenderer(this, 0, 0);
        rightRearFuselage.setTextureSize(textureWidth, textureHeight);
        rightRearFuselage.setRotationPoint(-16F, -6F, -24F);
        rightRearFuselage.addBox(-4F, 0F, 0F, 4, 16, 56);
        rightRearFuselage.rotateAngleY=(float) Math.toRadians(165);
        
    	bottomFuselage = new ModelRenderer(this, 0, 0);
    	bottomFuselage.setTextureSize(textureWidth, textureHeight);
    	bottomFuselage.setRotationPoint(0F, -5.95F, 0F);
    	bottomFuselage.addBox(-9F, 0F, 26F, 18, 2, 8);
    	bottomFuselage.addBox(-12F, 0F, 18F, 24, 2, 8);
    	bottomFuselage.addBox(-15F, 0F, -24F, 30, 2, 42);    	
    	bottomFuselage.addBox(-12F, 0F, -32F, 24, 2, 8);
    	bottomFuselage.addBox(-10F, 0F, -40F, 20, 2, 8);
    	bottomFuselage.addBox(-8F, 0F, -48F, 16, 2, 8);
    	bottomFuselage.addBox(-6F, 0F, -56F, 12, 2, 8);
    	bottomFuselage.addBox(-4F, 0F, -64F, 8, 2, 8);
    	bottomFuselage.addBox(-2F, 0F, -72F, 4, 2, 8);
        
        noseLeft = new ModelRenderer(this, 0, 0);
        noseLeft.setTextureSize(textureWidth, textureHeight);
        noseLeft.setRotationPoint(16F, -6F, 16F);
        noseLeft.addBox(-4F, 0F, 0F, 4, 16, 20);
        noseLeft.rotateAngleY=(float) Math.toRadians(-15);
        
        noseRight = new ModelRenderer(this, 0, 0);
        noseRight.setTextureSize(textureWidth, textureHeight);
        noseRight.setRotationPoint(-16F, -6F, 16F);
        noseRight.addBox(0F, 0F, 0F, 4, 16, 20);
        noseRight.rotateAngleY=(float) Math.toRadians(15);
        
        leftStrut = new ModelRenderer(this, 0, 0);
        leftStrut.setTextureSize(textureWidth, textureHeight);
        leftStrut.setRotationPoint(15F, -3F, -1.5F);
        leftStrut.addBox(0, 0, 0, 3, 15, 3);
        leftStrut.rotateAngleZ = (float) Math.toRadians(-130);
        leftStrut.render(scale);
        
        rightStrut = new ModelRenderer(this, 0, 0);
        rightStrut.setTextureSize(textureWidth, textureHeight);
        rightStrut.setRotationPoint(-14.5F, -5F, -1.5F);
        rightStrut.addBox(0, 0, 0, 3, 15, 3);
        rightStrut.rotateAngleZ = (float) Math.toRadians(130);
        rightStrut.render(scale);
        
        centerStrut = new ModelRenderer(this, 0, 0);
        centerStrut.setTextureSize(textureWidth, textureHeight);
        centerStrut.setRotationPoint(0, -6F, 25F);
        centerStrut.addBox(-1F, 0, 0, 2, 8, 2);
        centerStrut.rotateAngleX = (float) Math.toRadians(-200);
        
    	wing = new ModelRenderer(this, 0, 0);
    	wing.setTextureSize(textureWidth, textureHeight);
    	wing.setRotationPoint(0F, 26F, 0F);
    	wing.addBox(-72F, 0F, 0F, 144, 2, 14);
    	wing.addBox(-16F, 0F, -8F, 32, 2, 8);
    	wing.addBox(-72F, 0F, -8F, 8, 2, 8);
    	wing.addBox(64F, 0F, -8F, 8, 2, 8);
    	wing.addBox(-16F, -16F, -8F, 4, 16, 4);
    	wing.addBox(-16F, -16F, 10F, 4, 16, 4);
    	wing.addBox(12F, -16F, -8F, 4, 16, 4);
    	wing.addBox(12F, -16F, 10F, 4, 16, 4);
    	
    	leftWingSupport = new ModelRenderer(this, 0, 0);
    	leftWingSupport.setTextureSize(textureWidth, textureHeight);
    	leftWingSupport.setRotationPoint(0F, 28F, -6F);
    	leftWingSupport.addBox(12.5F, -36F, 0F, 3, 36, 4);
    	leftWingSupport.rotateAngleX=(float) Math.toRadians(60);
    	leftWingSupport.rotateAngleY=(float) Math.toRadians(10);
    	
    	rightWingSupport = new ModelRenderer(this, 0, 0);
    	rightWingSupport.setTextureSize(textureWidth, textureHeight);
    	rightWingSupport.setRotationPoint(0F, 28F, -6F);
    	rightWingSupport.addBox(-15.5F, -36F, 0F, 3, 36, 4);
    	rightWingSupport.rotateAngleX=(float) Math.toRadians(60);
    	rightWingSupport.rotateAngleY=(float) Math.toRadians(-10);
    	
    	tail = new ModelRenderer(this, 0, 0);
    	tail.setTextureSize(textureWidth, textureHeight);
    	tail.setRotationPoint(0F, 8F, -68F);
    	tail.addBox(-32F, -1F, 0F, 64, 2, 8);
    	tail.addBox(-2F, 0F, 0F, 4, 24, 8);
    	
    	leftAileron = new ModelRenderer(this, 0, 0);
    	leftAileron.setTextureSize(textureWidth, textureHeight);
    	leftAileron.setRotationPoint(0F, 26F, 0F);
    	leftAileron.addBox(40F, 0F, -8F, 24, 2, 8);
    	
    	rightAileron = new ModelRenderer(this, 0, 0);
    	rightAileron.setTextureSize(textureWidth, textureHeight);
    	rightAileron.setRotationPoint(0F, 26F, 0F);
    	rightAileron.addBox(-64F, 0F, -8F, 24, 2, 8);
    	
    	rudder = new ModelRenderer(this, 0, 0);
    	rudder.setTextureSize(textureWidth, textureHeight);
    	rudder.setRotationPoint(0F, 8F, -68F);
    	rudder.addBox(-1F, 0F, -8F, 2, 24, 8);
    	
    	leftElevator = new ModelRenderer(this, 0, 0);
    	leftElevator.setTextureSize(textureWidth, textureHeight);
    	leftElevator.setRotationPoint(0F, 8F, -68F);
    	leftElevator.addBox(0F, -1F, -8F, 32, 2, 8);
    	
    	rightElevator = new ModelRenderer(this, 0, 0);
    	rightElevator.setTextureSize(textureWidth, textureHeight);
    	rightElevator.setRotationPoint(0F, 8F, -68F);
    	rightElevator.addBox(-32F, -1F, -8F, 32, 2, 8);
    	
    	rightFlap = new ModelRenderer(this, 0, 0);
    	rightFlap.setTextureSize(textureWidth, textureHeight);
    	rightFlap.setRotationPoint(0F, 23.9F, 0F);
    	rightFlap.addBox(-40F, 2F, -8F, 24, 2, 10);

    	leftFlap = new ModelRenderer(this, 0, 0);
    	leftFlap.setTextureSize(textureWidth, textureHeight);
    	leftFlap.setRotationPoint(0F, 23.9F, 0F);
    	leftFlap.addBox(16F, 2F, -8F, 24, 2, 10);

    	cowling = new ModelRenderer(this, 0, 0);
    	cowling.setTextureSize(textureWidth, textureHeight);
        cowling.setRotationPoint(0F, 8.1F, 12F);
        cowling.setTextureOffset(0, 0);
        cowling.addBox(-15.5F, 0F, 0F, 31, 2, 8);
        cowling.setTextureOffset(0, -8);
        cowling.addBox(-15F, 0F, 8F, 30, 2, 2);
        cowling.setTextureOffset(0, -10);
        cowling.addBox(-14.5F, 0F, 10F, 29, 2, 2);
        cowling.setTextureOffset(0, -12);
        cowling.addBox(-14F, 0F, 12F, 28, 2, 2);
        cowling.setTextureOffset(0, -14);
        cowling.addBox(-13.5F, 0F, 14F, 27, 2, 2);
        cowling.setTextureOffset(0, -16);
        cowling.addBox(-13F, 0F, 16F, 26, 2, 2);
        cowling.setTextureOffset(0, -18);
        cowling.addBox(-12.5F, 0F, 18F, 25, 2, 2);
        cowling.setTextureOffset(0, -20);
        cowling.addBox(-12F, 0F, 20F, 24, 2, 3);
    }    
    
    public void renderPlane(){
    	frontFuselage.render(scale);
        bottomFuselage.render(scale);
        rearFuselage.render(scale);
        leftRearFuselage.render(scale);
        rightRearFuselage.render(scale); 
        noseLeft.render(scale);
        noseRight.render(scale);
        cowling.render(scale);
        leftStrut.render(scale);
        rightStrut.render(scale);
        wing.render(scale);
        leftWingSupport.render(scale);
        rightWingSupport.render(scale);
        tail.render(scale);
        centerStrut.render(scale);
    }
    
	public void renderAilerons(float aileronAngle){
    	leftAileron.rotateAngleX = -aileronAngle;
    	rightAileron.rotateAngleX = aileronAngle;
    	leftAileron.render(scale);
    	rightAileron.render(scale);
    }
    
	public void renderElevators(float elevatorAngle){
    	leftElevator.rotateAngleX=elevatorAngle;
    	rightElevator.rotateAngleX=elevatorAngle;
    	leftElevator.render(scale);
    	rightElevator.render(scale);
    }
    
	public void renderRudder(float rudderAngle){
    	rudder.rotateAngleY=rudderAngle;
    	rudder.render(scale);
    }
    
	public void renderFlaps(float flapAngle){
    	leftFlap.rotateAngleX = -flapAngle;
    	rightFlap.rotateAngleX = -flapAngle;
    	leftFlap.render(scale);
    	rightFlap.render(scale);
    }
}