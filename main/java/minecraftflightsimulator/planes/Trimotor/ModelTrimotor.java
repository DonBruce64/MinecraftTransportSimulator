package minecraftflightsimulator.planes.Trimotor;

import minecraftflightsimulator.models.ModelPlane;
import net.minecraft.client.model.ModelRenderer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ModelTrimotor extends ModelPlane{	
    private ModelRenderer frontFuselage;
    private ModelRenderer bottomFuselage;
    private ModelRenderer topFuselage1;
    private ModelRenderer topFuselage2;
    private ModelRenderer topFuselage3;
    private ModelRenderer topFuselage4;
    private ModelRenderer topFuselage5;
    private ModelRenderer topFuselage6;
    private ModelRenderer leftRearFuselage;
    private ModelRenderer rightRearFuselage;
    private ModelRenderer bottomRearFuselage;
    private ModelRenderer tailFuselage;
    private ModelRenderer bottomTailFuselage;
    private ModelRenderer leftNose;
    private ModelRenderer rightNose;
    private ModelRenderer leftNoseFlashing;
    private ModelRenderer rightNoseFlashing;
    private ModelRenderer bottomNose;
    private ModelRenderer leftFrontCowling;
    private ModelRenderer rightFrontCowling;    
    private ModelRenderer frontCowling;
    private ModelRenderer cockpitWindowFrameLeft;
    private ModelRenderer cockpitWindowFrameRight;
    private ModelRenderer windowFrames;
    
    private ModelRenderer wing;
    private ModelRenderer stabilizer;
    private ModelRenderer tail;    
 
    public ModelTrimotor(){
    	this.textureHeight = 8;
    	this.textureWidth = 8;
    	
    	//64 high
    	//352 wide
    	//240 long
    	//each wheel and engie is 3 m or 48 from center
    	//fuselage is 36 wide, or 18 fromcenter on side
    	
    	

      	frontFuselage = new ModelRenderer(this, 0, 0);
        frontFuselage.setTextureSize(textureWidth, textureHeight);
        frontFuselage.setRotationPoint(0F, 0F, 0F);
        frontFuselage.addBox(-11F*16, -0.5F, -0.5F, 22*16, 1, 1);
        frontFuselage.addBox(-0.5F, -2F*16, -0.5F, 1, 4*16, 1);
        frontFuselage.addBox(-0.5F, -0.5F, -12F*16, 1, 1, 15*16);
        
        frontFuselage.addBox(-20F, -16F, -96, 2, 16, 96);
        frontFuselage.addBox(-20F, 8F, -96, 2, 8, 96);
        frontFuselage.addBox(18F, -16F, -96, 2, 16, 96);
        frontFuselage.addBox(18F, 8F, -96, 2, 8, 96);
        
    	leftNose = new ModelRenderer(this, 0, 0);
    	leftNose.setTextureSize(textureWidth, textureHeight);
    	leftNose.setRotationPoint(20F, 0F, 0F);
    	leftNose.addBox(-2F, 18F, 0, 2, 2, 22);
    	leftNose.addBox(-2F, -12F, 0, 2, 24, 12);
    	leftNose.addBox(-2F, -10F, 12, 2, 20, 10);
    	leftNose.rotateAngleY = (float) Math.toRadians(-12);
        
    	rightNose = new ModelRenderer(this, 0, 0);
    	rightNose.setTextureSize(textureWidth, textureHeight);
    	rightNose.setRotationPoint(-20F, 0F, 0F);
    	rightNose.addBox(0F, 18F, 0, 2, 2, 22);
    	rightNose.addBox(0F, -12F, 0, 2, 24, 12);
    	rightNose.addBox(0F, -10F, 12, 2, 20, 10);
    	rightNose.rotateAngleY = (float) Math.toRadians(12);
    	
    	leftNoseFlashing = new ModelRenderer(this, 0, 0);
    	leftNoseFlashing.setTextureSize(textureWidth, textureHeight);
    	leftNoseFlashing.setRotationPoint(20F, 0F, 0F);
    	leftNoseFlashing.addBox(-2F, 13F, 0, 2, 4, 1);
    	leftNoseFlashing.addBox(-2F, 12F, 0, 2, 1, 12);
    	leftNoseFlashing.addBox(-2F, 17F, 0, 2, 1, 22);
    	leftNoseFlashing.addBox(-2F, 12F, 0, 2, 1, 12);
    	leftNoseFlashing.addBox(-2F, 10F, 12, 2, 7, 1);
    	leftNoseFlashing.addBox(-2F, 10F, 13, 2, 1, 8);
    	leftNoseFlashing.addBox(-2F, 10F, 21, 2, 7, 1);
    	
    	leftNoseFlashing.addBox(-2F, -16F, 0, 2, 4, 6);
    	leftNoseFlashing.addBox(-2F, -14F, 6, 2, 2, 6);
    	leftNoseFlashing.addBox(-2F, -14F, 12, 2, 4, 5);
    	leftNoseFlashing.addBox(-2F, -12F, 17, 2, 2, 5);
    	leftNoseFlashing.rotateAngleY = (float) Math.toRadians(-12);
        
    	rightNoseFlashing = new ModelRenderer(this, 0, 0);
    	rightNoseFlashing.setTextureSize(textureWidth, textureHeight);
    	rightNoseFlashing.setRotationPoint(-20F, 0F, 0F);
    	rightNoseFlashing.addBox(0F, 13F, 0, 2, 4, 1);
    	rightNoseFlashing.addBox(0F, 12F, 0, 2, 1, 12);
    	rightNoseFlashing.addBox(0F, 17F, 0, 2, 1, 22);
    	rightNoseFlashing.addBox(0F, 12F, 0, 2, 1, 12);
    	rightNoseFlashing.addBox(0F, 10F, 12, 2, 7, 1);
    	rightNoseFlashing.addBox(0F, 10F, 13, 2, 1, 8);
    	rightNoseFlashing.addBox(0F, 10F, 21, 2, 7, 1);
    	
    	rightNoseFlashing.addBox(0F, -16F, 0, 2, 4, 6);
    	rightNoseFlashing.addBox(0F, -14F, 6, 2, 2, 6);
    	rightNoseFlashing.addBox(0F, -14F, 12, 2, 4, 5);
    	rightNoseFlashing.addBox(0F, -12F, 17, 2, 2, 5);
    	rightNoseFlashing.rotateAngleY = (float) Math.toRadians(12);
    	
    	bottomNose = new ModelRenderer(this, 0, 0);
    	bottomNose.setTextureSize(textureWidth, textureHeight);
    	bottomNose.setRotationPoint(0F, -16F, 0F);
    	bottomNose.addBox(-18F, 0F, 0, 36, 4, 6);
    	bottomNose.addBox(-17F, 2F, 6, 34, 2, 6);
    	bottomNose.addBox(-16F, 2F, 12, 32, 4, 4);
    	bottomNose.addBox(-15F, 4F, 16, 30, 2, 5);
    	
        leftFrontCowling = new ModelRenderer(this, 0, 0);
        leftFrontCowling.setTextureSize(textureWidth, textureHeight);
        leftFrontCowling.setRotationPoint(15.5F, 0F, 21.5F);
        leftFrontCowling.addBox(-2F, -11F, 0F, 2, 20, 8);
        leftFrontCowling.addBox(-2F, -9F, 8F, 2, 16, 8);
        leftFrontCowling.rotateAngleY = (float) Math.toRadians(-20);
        
        rightFrontCowling = new ModelRenderer(this, 0, 0);
        rightFrontCowling.setTextureSize(textureWidth, textureHeight);
        rightFrontCowling.setRotationPoint(-15.5F, 0F, 21.5F);
        rightFrontCowling.addBox(0F, -11F, 0F, 2, 20, 8);
        rightFrontCowling.addBox(0F, -9F, 8F, 2, 16, 8);
        rightFrontCowling.rotateAngleY = (float) Math.toRadians(20);
        
        frontCowling = new ModelRenderer(this, 0, 0);
        frontCowling.setTextureSize(textureWidth, textureHeight);
        frontCowling.setRotationPoint(0F, 0F, 21.25F);
        frontCowling.addBox(-14F, 7.1F, 0F, 28, 2, 4);
        frontCowling.addBox(-12F, 7.1F, 4F, 24, 2, 4);
        frontCowling.addBox(-11F, 5.1F, 8F, 22, 2, 4);
        frontCowling.addBox(-10F, 5.1F, 12F, 20, 2, 4);
        
        frontCowling.addBox(-14F, -11.1F, 0F, 28, 2, 4);
        frontCowling.addBox(-12F, -11.1F, 4F, 24, 2, 4);
        frontCowling.addBox(-11F, -9.1F, 8F, 22, 2, 4);
        frontCowling.addBox(-10F, -9.1F, 12F, 20, 2, 4);
        frontCowling.addBox(-10F, -9F, 14F, 20, 16, 2);
        
        cockpitWindowFrameLeft = new ModelRenderer(this, 0, 0);
        cockpitWindowFrameLeft.setTextureSize(textureWidth, textureHeight);
        cockpitWindowFrameLeft.setRotationPoint(15.5F, 8.9F, 21.4F);
        cockpitWindowFrameLeft.addBox(-2F, 8F, 2F, 2, 2, 13);
        cockpitWindowFrameLeft.addBox(-2F, 0F, 0F, 2, 10, 2);
        cockpitWindowFrameLeft.addBox(-2F, 0F, 2F, 2, 2, 13);
        cockpitWindowFrameLeft.addBox(-2F, 0F, 15F, 2, 10, 2);
        cockpitWindowFrameLeft.rotateAngleY = (float) Math.toRadians(-66);
    	
    	cockpitWindowFrameRight = new ModelRenderer(this, 0, 0);
        cockpitWindowFrameRight.setTextureSize(textureWidth, textureHeight);
        cockpitWindowFrameRight.setRotationPoint(-15.5F, 8.9F, 21.4F);
        cockpitWindowFrameRight.addBox(0F, 8F, 2F, 2, 2, 13);
        cockpitWindowFrameRight.addBox(0F, 0F, 0F, 2, 10, 2);
        cockpitWindowFrameRight.addBox(0F, 0F, 2F, 2, 2, 13);
        cockpitWindowFrameRight.addBox(0F, 0F, 15F, 2, 10, 2);
        cockpitWindowFrameRight.rotateAngleY = (float) Math.toRadians(66);
        
        windowFrames = new ModelRenderer(this, 0, 0);
        windowFrames.setTextureSize(textureWidth, textureHeight);
        windowFrames.setRotationPoint(0F, 0F, 0F);
        windowFrames.addBox(-20F, 0F, -96, 2, 1, 96);
        windowFrames.addBox(-20F, 7F, -96, 2, 1, 96);
        windowFrames.addBox(18F, 0F, -96, 2, 1, 96);
        windowFrames.addBox(18F, 7F, -96, 2, 1, 96);
        for(int i=0; i>=-96; i=i-16){
        	if(i==0 || i==-95){
        		i=i+1;
        		windowFrames.addBox(-20F, 1F, i-2, 2, 6, 1);
            	windowFrames.addBox(18F, 1F, i-2, 2, 6, 1);
        	}else{
        		windowFrames.addBox(-20F, 1F, i-2, 2, 6, 2);
        		windowFrames.addBox(18F, 1F, i-2, 2, 6, 2);
        	}
        }
        
        bottomFuselage = new ModelRenderer(this, 0, 0);
        bottomFuselage.setTextureSize(textureWidth, textureHeight);
        bottomFuselage.setRotationPoint(0F, 0F, 0F);
        bottomFuselage.addBox(-18F, -16F, -98, 36, 2, 98);
    	
    	topFuselage1 = new ModelRenderer(this, 0, 0);
    	topFuselage1.setTextureSize(textureWidth, textureHeight);
    	topFuselage1.setRotationPoint(20F, 16F, 0F);
    	topFuselage1.addBox(-2F, 0F, -96, 2, 12, 96);
    	topFuselage1.rotateAngleZ = (float) Math.toRadians(45);
    	
    	topFuselage2 = new ModelRenderer(this, 0, 0);
    	topFuselage2.setTextureSize(textureWidth, textureHeight);
    	topFuselage2.setRotationPoint(-20F, 16F, 0F);
    	topFuselage2.addBox(0F, 0F, -96, 2, 12, 96);
    	topFuselage2.rotateAngleZ = (float) Math.toRadians(-45);
    	
    	topFuselage3 = new ModelRenderer(this, 0, 0);
    	topFuselage3.setTextureSize(textureWidth, textureHeight);
    	topFuselage3.setRotationPoint(20F, 16F, -96F);
    	topFuselage3.addBox(-2F, 0F, -80F, 2, 12, 80);
    	topFuselage3.rotateAngleZ = (float) Math.toRadians(45);
    	topFuselage3.rotateAngleY = (float) Math.toRadians(5.65);
    	topFuselage3.rotateAngleX = (float) Math.toRadians(5.65);
    	
    	topFuselage4 = new ModelRenderer(this, 0, 0);
    	topFuselage4.setTextureSize(textureWidth, textureHeight);
    	topFuselage4.setRotationPoint(-20F, 16F, -96F);
    	topFuselage4.addBox(0F, 0F, -80F, 2, 12, 80);
    	topFuselage4.rotateAngleZ = (float) Math.toRadians(-45);
    	topFuselage4.rotateAngleY = (float) Math.toRadians(-5.65);
    	topFuselage4.rotateAngleX = (float) Math.toRadians(5.65);
        
        topFuselage5 = new ModelRenderer(this, 0, 0);
        topFuselage5.setTextureSize(textureWidth, textureHeight);
        topFuselage5.setRotationPoint(0F, 0F, 0F);
        topFuselage5.addBox(-11.5F, 22.5F, -96, 23, 2, 96);
        for(int i=0; i<11; ++i){
        	topFuselage5.addBox(-10.5F + i, 22.5F, -102-i*7 - i/6, 21-2*i, 2, 7 + (i%6==0 && i>0 ? i/6 : 0));
        }
        
        topFuselage6 = new ModelRenderer(this, 0, 0);
    	topFuselage6.setTextureSize(textureWidth, textureHeight);
    	topFuselage6.setRotationPoint(0F, 18.1F, 0F);
    	topFuselage6.addBox(-18F, 0F, 0F, 36, 2, 7);
    	topFuselage6.addBox(-17F, 0F, 7F, 34, 2, 5);
    	topFuselage6.addBox(-16F, 2F, 0F, 32, 2, 7);
    	topFuselage6.addBox(-15F, 2F, 7F, 30, 2, 5);
    	topFuselage6.addBox(-14F, 4F, -5F, 28, 2, 12);
    	topFuselage6.addBox(-13F, 4F, 5F, 26, 1, 7);
    	topFuselage6.addBox(-16F, 0F, 12F, 8, 2, 5);
    	topFuselage6.addBox(-15F, 0F, 17F, 7, 2, 4);
    	topFuselage6.addBox(-14F, 2F, 12F, 6, 2, 5);
    	topFuselage6.addBox(-13F, 2F, 17F, 5, 1, 4);
    	topFuselage6.addBox(8F, 0F, 12F, 8, 2, 5);
    	topFuselage6.addBox(8F, 0F, 17F, 7, 2, 4);
    	topFuselage6.addBox(8F, 2F, 12F, 6, 2, 5);
    	topFuselage6.addBox(8F, 2F, 17F, 5, 1, 4);
    	topFuselage6.addBox(-1F, 0F, 12F, 2, 4, 5);
    	topFuselage6.addBox(-1F, 0F, 17F, 2, 3, 4);
    	topFuselage6.addBox(-8F, 0F, 20F, 16, 3, 1);
    	topFuselage6.addBox(-1F, 0F, 21F, 2, 2, 6);

        leftRearFuselage = new ModelRenderer(this, 0, 0);
        leftRearFuselage.setTextureSize(textureWidth, textureHeight);
        leftRearFuselage.setRotationPoint(20F, -16F, -96F);
        leftRearFuselage.addBox(-2F, 18F, -80F, 2, 14, 80);
        leftRearFuselage.addBox(-2F, 16F, -72F, 2, 2, 72);
        leftRearFuselage.addBox(-2F, 14F, -64F, 2, 2, 64);
        leftRearFuselage.addBox(-2F, 12F, -56F, 2, 2, 56);
        leftRearFuselage.addBox(-2F, 10F, -48F, 2, 2, 48);
        leftRearFuselage.addBox(-2F, 8F, -40F, 2, 2, 40);
        leftRearFuselage.addBox(-2F, 6F, -32F, 2, 2, 32);
        leftRearFuselage.addBox(-2F, 4F, -24F, 2, 2, 24);
        leftRearFuselage.addBox(-2F, 2F, -16F, 2, 2, 16);
        leftRearFuselage.addBox(-2F, 0F, -8F, 2, 2, 8);
        leftRearFuselage.rotateAngleY=(float) Math.toRadians(8);
        
        rightRearFuselage = new ModelRenderer(this, 0, 0);
        rightRearFuselage.setTextureSize(textureWidth, textureHeight);
        rightRearFuselage.setRotationPoint(-20F, -16F, -96F);
        rightRearFuselage.addBox(0F, 18F, -80F, 2, 14, 80);
        rightRearFuselage.addBox(0F, 16F, -72F, 2, 2, 72);
        rightRearFuselage.addBox(0F, 14F, -64F, 2, 2, 64);
        rightRearFuselage.addBox(0F, 12F, -56F, 2, 2, 56);
        rightRearFuselage.addBox(0F, 10F, -48F, 2, 2, 48);
        rightRearFuselage.addBox(0F, 8F, -40F, 2, 2, 40);
        rightRearFuselage.addBox(0F, 6F, -32F, 2, 2, 32);
        rightRearFuselage.addBox(0F, 4F, -24F, 2, 2, 24);
        rightRearFuselage.addBox(0F, 2F, -16F, 2, 2, 16);
        rightRearFuselage.addBox(0F, 0F, -8F, 2, 2, 8);
        rightRearFuselage.rotateAngleY=(float) Math.toRadians(-8);
    	
    	bottomRearFuselage = new ModelRenderer(this, 0, 0);
    	bottomRearFuselage.setTextureSize(textureWidth, textureHeight);
    	bottomRearFuselage.setRotationPoint(0F, 2F, -175F);
    	bottomRearFuselage.addBox(-8.5F, 0F, 0F, 17, 2, 8);
    	bottomRearFuselage.addBox(-9.5F, 0F, 8F, 19, 2, 8);
    	bottomRearFuselage.addBox(-10.5F, 0F, 16F, 21, 2, 8);
    	bottomRearFuselage.addBox(-11.5F, 0F, 24F, 23, 2, 8);
    	bottomRearFuselage.addBox(-12.5F, 0F, 32F, 25, 2, 8);
    	bottomRearFuselage.addBox(-13.5F, 0F, 40F, 27, 2, 8);
    	bottomRearFuselage.addBox(-14.5F, 0F, 48F, 29, 2, 8);
    	bottomRearFuselage.addBox(-15.5F, 0F, 56F, 31, 2, 8);
    	bottomRearFuselage.addBox(-16.5F, 0F, 64F, 33, 2, 8);
    	bottomRearFuselage.addBox(-17.5F, 0F, 72F, 35, 2, 8);
    	bottomRearFuselage.rotateAngleX = (float) Math.toRadians(13.25);
    	
    	tailFuselage = new ModelRenderer(this, 0, 0);
    	tailFuselage.setTextureSize(textureWidth, textureHeight);
    	tailFuselage.setRotationPoint(0F, 20F, -174F);
    	for(int i=0; i<5; ++i){
    		tailFuselage.addBox(-7F+i, -18F+i, -2F-2*i, 14-2*i, 15, 2+i*2);
    	}
    	tailFuselage.addBox(-2F, -13F, -12F, 4, 13, 2);
    	
    	bottomTailFuselage = new ModelRenderer(this, 0, 0);
    	bottomTailFuselage.setTextureSize(textureWidth, textureHeight);
    	bottomTailFuselage.setRotationPoint(0F, 1.99F, -174F);
    	for(int i=0; i<6; ++i){
    		bottomTailFuselage.addBox(-7F+i, i, -2F-2*i, 14-2*i, 0, 4+i*2);
    	}
    	
    	wing = new ModelRenderer(this, 0, 0);
    	wing.setTextureSize(textureWidth, textureHeight);
    	wing.setRotationPoint(0F, 16F, 0F);
    	wing.addBox(-176F, 8F, 2F, 8, 1, 50);
    	wing.addBox(-168F, 8F, 2F, 32, 1, 42);
    	wing.addBox(-136F, 8F, 2F, 136, 1, 50);
    	wing.addBox(0F, 8F, 2F, 136, 1, 50);
    	wing.addBox(136, 8F, 2F, 32, 1, 42);
    	wing.addBox(168, 8F, 2F, 8, 1, 50);
    	wing.addBox(-132F, 7F, 0F, 264, 1, 56);
    	wing.addBox(-110F, 6F, 0F, 220, 1, 60);
    	wing.addBox(-88F, 4F, 0F, 176, 2, 64);
    	wing.addBox(-66F, 2F, 0F, 132, 2, 68);    	
    	wing.addBox(-48F, 0F, 2F, 96, 2, 70);
    	wing.rotateAngleY = (float) Math.toRadians(180);
    	
       	stabilizer = new ModelRenderer(this, 0, 0);
    	stabilizer.setTextureSize(textureWidth, textureHeight);
    	stabilizer.setRotationPoint(0F, 20F, -174F);
    	stabilizer.addBox(-48F, 0F, -12F, 96, 2, 12);
    	
    	tail = new ModelRenderer(this, 0, 0);
    	tail.setTextureSize(textureWidth, textureHeight);
    	tail.setRotationPoint(0F, 21F, -186F);
    	for(int i=1; i<15; ++i){
    		tail.addBox(-1F, i, 0F, 2, 1, 20-i);
    	}

    	leftAileron = new ModelRenderer(this, 0, 0);
    	leftAileron.setTextureSize(textureWidth, textureHeight);
    	leftAileron.setRotationPoint(0F, 24.5F, -44F);
    	leftAileron.addBox(136F, -0.5F, -8F, 32, 1, 8);
    	
    	rightAileron = new ModelRenderer(this, 0, 0);
    	rightAileron.setTextureSize(textureWidth, textureHeight);
    	rightAileron.setRotationPoint(0F, 24.5F, -44F);
    	rightAileron.addBox(-168F, -0.5F, -8F, 32, 1, 8);
    	
    	rudder = new ModelRenderer(this, 0, 0);
    	rudder.setTextureSize(textureWidth, textureHeight);
    	rudder.setRotationPoint(0F, 19F, -186F);
    	rudder.addBox(-0.5F, 17F, 0F, 1, 5, 6);
    	rudder.addBox(-0.5F, 0F, -12F, 1, 22, 12);
    	rudder.addBox(-0.5F, -1F, -12F, 1, 1, 11);
    	rudder.addBox(-0.5F, -2F, -12F, 1, 1, 10);
    	rudder.addBox(-0.5F, -4F, -12F, 1, 2, 8);
    	rudder.addBox(-0.5F, -5F, -12F, 1, 1, 6);
    	rudder.addBox(-0.5F, -6F, -12F, 1, 1, 4);
    	
    	leftElevator = new ModelRenderer(this, 0, 0);
    	leftElevator.setTextureSize(textureWidth, textureHeight);
    	leftElevator.setRotationPoint(0F, 20.5F, -186F);
    	leftElevator.addBox(4F, 0F, -10F, 44, 1, 10);
    	
    	rightElevator = new ModelRenderer(this, 0, 0);
    	rightElevator.setTextureSize(textureWidth, textureHeight);
    	rightElevator.setRotationPoint(0F, 20.5F, -186F);
    	rightElevator.addBox(-48F, 0F, -10F, 44, 1, 10);
    }    
    
    @Override
    public void renderPlane(byte textureCode, float aileronAngle, float elevatorAngle, float rudderAngle, float flapAngle){}
    	
    public void renderFirstStage(byte textureCode, float aileronAngle, float elevatorAngle, float rudderAngle, float flapAngle){
        //windows 12x6 
    	frontFuselage.render(scale);
        leftRearFuselage.render(scale);
        rightRearFuselage.render(scale);
        tailFuselage.render(scale);
        topFuselage1.render(scale);
        topFuselage2.render(scale);
        topFuselage3.render(scale);
        topFuselage4.render(scale);
        leftNose.render(scale);
        rightNose.render(scale);
        cockpitWindowFrameLeft.render(scale);
        cockpitWindowFrameRight.render(scale);
        tail.render(scale);
        renderRudder(rudderAngle);
    }

    public void renderSecondStage(byte textureCode, float aileronAngle, float elevatorAngle, float rudderAngle, float flapAngle){
        bottomFuselage.render(scale);
        bottomRearFuselage.render(scale);
        bottomTailFuselage.render(scale);
        topFuselage5.render(scale);
        topFuselage6.render(scale);
        wing.render(scale);
        stabilizer.render(scale);
        renderAilerons(aileronAngle);
        renderElevators(elevatorAngle);
    }

    public void renderThirdStage(byte textureCode, float aileronAngle, float elevatorAngle, float rudderAngle, float flapAngle){        
        leftNoseFlashing.render(scale);
        rightNoseFlashing.render(scale);
        bottomNose.render(scale);
        leftFrontCowling.render(scale);
        rightFrontCowling.render(scale);
        frontCowling.render(scale);
        windowFrames.render(scale);
    }        
}