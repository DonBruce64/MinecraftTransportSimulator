package minecraftflightsimulator.planes.Trimotor;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

public class ModelTrimotor extends ModelBase{
	private static final float scale=0.0625F;
	
	//Regular parts
    private ModelRenderer frontFuselage;
    private ModelRenderer topFuselage1;
    private ModelRenderer topFuselage2;
    private ModelRenderer topFuselage3;
    private ModelRenderer topFuselage4;
    private ModelRenderer leftRearFuselage;
    private ModelRenderer rightRearFuselage;
    private ModelRenderer tailFuselage;
    private ModelRenderer tail;
    private ModelRenderer rearWheelStrut;
    private ModelRenderer leftNose;
    private ModelRenderer rightNose;
    private ModelRenderer cockpitWindowFrameLeft;
    private ModelRenderer cockpitWindowFrameRight;
    private ModelRenderer interiorPartitions;
    
    //Parts with rotated texture
    private ModelRenderer bottomFuselage;
    private ModelRenderer bottomRearFuselage;
    private ModelRenderer bottomTailFuselage;
    private ModelRenderer topFuselage5;
    private ModelRenderer topFuselage6;
    private ModelRenderer wing;
    private ModelRenderer stabilizer;
    
    //Outer structures
    private ModelRenderer leftGondola;
    private ModelRenderer leftGondolaSupport1;
    private ModelRenderer leftGondolaSupport2;
    private ModelRenderer leftGondolaSupport3;
    private ModelRenderer leftGondolaSupport4;
    private ModelRenderer leftGondolaSupport5;
    private ModelRenderer leftGondolaSupport6;
    private ModelRenderer leftWheelSupport1;
    private ModelRenderer leftWheelSupport2;
    private ModelRenderer leftStabilizerSupport;
    private ModelRenderer leftWheelMount;
    private ModelRenderer rightGondola;
    private ModelRenderer rightGondolaSupport1;
    private ModelRenderer rightGondolaSupport2;
    private ModelRenderer rightGondolaSupport3;
    private ModelRenderer rightGondolaSupport4;
    private ModelRenderer rightGondolaSupport5;
    private ModelRenderer rightGondolaSupport6;
    private ModelRenderer rightWheelSupport1;
    private ModelRenderer rightWheelSupport2;
    private ModelRenderer rightStabilizerSupport;
    private ModelRenderer rightWheelMount;
    
    //Control surfaces
    private ModelRenderer leftAileron;
    private ModelRenderer rightAileron;
    private ModelRenderer leftElevator;
    private ModelRenderer rightElevator;
    private ModelRenderer rudder;

    //Colored parts
    private ModelRenderer leftNoseFlashing;
    private ModelRenderer rightNoseFlashing;
    private ModelRenderer bottomNose;
    private ModelRenderer leftFrontCowling;
    private ModelRenderer rightFrontCowling;    
    private ModelRenderer frontCowling; 
    private ModelRenderer wingEdges;
    private ModelRenderer elevatorEdges;
    private ModelRenderer windowFrames;
 
    public ModelTrimotor(){
    	this.textureHeight = 8;
    	this.textureWidth = 8;

      	frontFuselage = new ModelRenderer(this);
        frontFuselage.setRotationPoint(0F, 0F, 0F);        
        frontFuselage.addBox(-20F, -16F, -96, 2, 16, 96);
        frontFuselage.addBox(-20F, 8F, -96, 2, 8, 96);
        frontFuselage.addBox(18F, -16F, -96, 2, 16, 96);
        frontFuselage.addBox(18F, 8F, -96, 2, 8, 96);
        
    	leftNose = new ModelRenderer(this);
    	leftNose.setRotationPoint(20F, 0F, 0F);
    	leftNose.addBox(-2F, 18F, 0, 2, 2, 22);
    	leftNose.addBox(-2F, -12F, 0, 2, 24, 12);
    	leftNose.addBox(-2F, -10F, 12, 2, 20, 10);
    	leftNose.rotateAngleY = (float) Math.toRadians(-12);
        
    	rightNose = new ModelRenderer(this);
    	rightNose.setRotationPoint(-20F, 0F, 0F);
    	rightNose.addBox(0F, 18F, 0, 2, 2, 22);
    	rightNose.addBox(0F, -12F, 0, 2, 24, 12);
    	rightNose.addBox(0F, -10F, 12, 2, 20, 10);
    	rightNose.rotateAngleY = (float) Math.toRadians(12);
    	
    	leftNoseFlashing = new ModelRenderer(this);
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
        
    	rightNoseFlashing = new ModelRenderer(this);
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
    	
    	bottomNose = new ModelRenderer(this);
    	bottomNose.setRotationPoint(0F, -16F, 0F);
    	bottomNose.addBox(-18F, 0F, 0, 36, 4, 6);
    	bottomNose.addBox(-17F, 2F, 6, 34, 2, 6);
    	bottomNose.addBox(-16F, 2F, 12, 32, 4, 4);
    	bottomNose.addBox(-15F, 4F, 16, 30, 2, 5);
    	
        leftFrontCowling = new ModelRenderer(this);
        leftFrontCowling.setRotationPoint(15.5F, 0F, 21.5F);
        leftFrontCowling.addBox(-2F, -11F, 0F, 2, 20, 8);
        leftFrontCowling.addBox(-2F, -9F, 8F, 2, 16, 8);
        leftFrontCowling.rotateAngleY = (float) Math.toRadians(-20);
        
        rightFrontCowling = new ModelRenderer(this);
        rightFrontCowling.setRotationPoint(-15.5F, 0F, 21.5F);
        rightFrontCowling.addBox(0F, -11F, 0F, 2, 20, 8);
        rightFrontCowling.addBox(0F, -9F, 8F, 2, 16, 8);
        rightFrontCowling.rotateAngleY = (float) Math.toRadians(20);
        
        frontCowling = new ModelRenderer(this);
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
        
        cockpitWindowFrameLeft = new ModelRenderer(this);
        cockpitWindowFrameLeft.setRotationPoint(15.5F, 8.9F, 21.4F);
        cockpitWindowFrameLeft.addBox(-2F, 8F, 2F, 2, 2, 13);
        cockpitWindowFrameLeft.addBox(-2F, 0F, 0F, 2, 10, 2);
        cockpitWindowFrameLeft.addBox(-2F, 0F, 2F, 2, 2, 13);
        cockpitWindowFrameLeft.addBox(-2F, 0F, 15F, 2, 10, 2);
        cockpitWindowFrameLeft.rotateAngleY = (float) Math.toRadians(-66);
    	
    	cockpitWindowFrameRight = new ModelRenderer(this);
        cockpitWindowFrameRight.setRotationPoint(-15.5F, 8.9F, 21.4F);
        cockpitWindowFrameRight.addBox(0F, 8F, 2F, 2, 2, 13);
        cockpitWindowFrameRight.addBox(0F, 0F, 0F, 2, 10, 2);
        cockpitWindowFrameRight.addBox(0F, 0F, 2F, 2, 2, 13);
        cockpitWindowFrameRight.addBox(0F, 0F, 15F, 2, 10, 2);
        cockpitWindowFrameRight.rotateAngleY = (float) Math.toRadians(66);
        
        windowFrames = new ModelRenderer(this);
        windowFrames.setRotationPoint(0F, 0F, 0F);
        windowFrames.addBox(-20F, 0F, -96, 2, 1, 96);
        windowFrames.addBox(-20F, 7F, -96, 2, 1, 96);
        windowFrames.addBox(18F, 0F, -96, 2, 1, 96);
        windowFrames.addBox(18F, 7F, -96, 2, 1, 96);
        for(int i=0; i>=-96; i-=16){
        	if(i==0 || i==-95){
        		i=i+1;
        		windowFrames.addBox(-20F, 1F, i-2, 2, 6, 1);
            	windowFrames.addBox(18F, 1F, i-2, 2, 6, 1);
        	}else{
        		windowFrames.addBox(-20F, 1F, i-2, 2, 6, 2);
        		windowFrames.addBox(18F, 1F, i-2, 2, 6, 2);
        	}
        }
        
        bottomFuselage = new ModelRenderer(this);
        bottomFuselage.setRotationPoint(0F, 0F, 0F);
        bottomFuselage.addBox(-18F, -16F, -98, 36, 2, 98);
    	
    	topFuselage1 = new ModelRenderer(this);
    	topFuselage1.setRotationPoint(20F, 16F, 0F);
    	topFuselage1.addBox(-2F, 0F, -96, 2, 12, 96);
    	topFuselage1.rotateAngleZ = (float) Math.toRadians(45);
    	
    	topFuselage2 = new ModelRenderer(this);
    	topFuselage2.setRotationPoint(-20F, 16F, 0F);
    	topFuselage2.addBox(0F, 0F, -96, 2, 12, 96);
    	topFuselage2.rotateAngleZ = (float) Math.toRadians(-45);
    	
    	topFuselage3 = new ModelRenderer(this);
    	topFuselage3.setRotationPoint(20F, 16F, -96F);
    	topFuselage3.addBox(-2F, 0F, -80F, 2, 12, 80);
    	topFuselage3.rotateAngleZ = (float) Math.toRadians(45);
    	topFuselage3.rotateAngleY = (float) Math.toRadians(5.65);
    	topFuselage3.rotateAngleX = (float) Math.toRadians(5.65);
    	
    	topFuselage4 = new ModelRenderer(this);
    	topFuselage4.setRotationPoint(-20F, 16F, -96F);
    	topFuselage4.addBox(0F, 0F, -80F, 2, 12, 80);
    	topFuselage4.rotateAngleZ = (float) Math.toRadians(-45);
    	topFuselage4.rotateAngleY = (float) Math.toRadians(-5.65);
    	topFuselage4.rotateAngleX = (float) Math.toRadians(5.65);
        
        topFuselage5 = new ModelRenderer(this);
        topFuselage5.setRotationPoint(0F, 0F, 0F);
        topFuselage5.addBox(-11.5F, 22.5F, -96, 23, 2, 96);
        for(int i=0; i<11; ++i){
        	topFuselage5.addBox(-10.5F + i, 22.5F, -102-i*7 - i/6, 21-2*i, 2, 7 + (i%6==0 && i>0 ? i/6 : 0));
        }
        
        topFuselage6 = new ModelRenderer(this);
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

        leftRearFuselage = new ModelRenderer(this);
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
        
        rightRearFuselage = new ModelRenderer(this);
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
    	
    	bottomRearFuselage = new ModelRenderer(this);
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
    	
    	tailFuselage = new ModelRenderer(this);
    	tailFuselage.setRotationPoint(0F, 20F, -174F);
    	for(int i=0; i<5; ++i){
    		tailFuselage.addBox(-7F+i, -18F+i, -2F-2*i, 14-2*i, 15, 2+i*2);
    	}
    	tailFuselage.addBox(-2F, -13F, -12F, 4, 13, 2);
    	
    	bottomTailFuselage = new ModelRenderer(this);
    	bottomTailFuselage.setRotationPoint(0F, 1.99F, -176F);
    	for(int i=0; i<6; ++i){
    		bottomTailFuselage.addBox(-7F+i, i, 0F-2*i, 14-2*i, 0, 2+i*2);
    	}
    	
        interiorPartitions = new ModelRenderer(this);
        interiorPartitions.setRotationPoint(0F, -14F, 0F);
        interiorPartitions.addBox(-14F, 4F, 20.2F, 28, 19, 1);
        interiorPartitions.addBox(-18F, 0F, -0.99F, 12, 30, 1);
        interiorPartitions.addBox(6F, 0F, -0.99F, 12, 30, 1);
        interiorPartitions.addBox(-18F, 30F, -1.99F, 36, 2, 2);
        interiorPartitions.addBox(-8F, 0F, -0.01F, 14, 7, 0);
        interiorPartitions.addBox(-18F, 7F, 0F, 36, 0, 5);
        interiorPartitions.addBox(-17F, 7F, 5F, 34, 0, 5);
        interiorPartitions.addBox(-16F, 7F, 10F, 32, 0, 5);
        interiorPartitions.addBox(-15F, 7F, 15F, 30, 0, 6);
        for(byte i=0; i<=7; ++i){
        	interiorPartitions.addBox(-18F, i, -17F, 14 - i, 1, 1);
        	interiorPartitions.addBox(4F + i, i, -17F, 14 - i, 1, 1);
        }
        for(byte i=0; i<7; ++i){
        	interiorPartitions.addBox(-18F, 8F + 4*i, -17F, 6 - i, 4, 1);
        	interiorPartitions.addBox(12F + i, 8F + 4*i, -17F, 6 - i, 4, 1);
        }
        interiorPartitions.addBox(-18F, 0F, -98F, 36, 30, 0);
    	
        rearWheelStrut = new ModelRenderer(this);
    	rearWheelStrut.setRotationPoint(0F, 20F, -184F);
    	rearWheelStrut.addBox(-1, -12, -2, 2, 12, 2);
    	rearWheelStrut.rotateAngleX = (float) Math.toRadians(25F);
    	
    	wing = new ModelRenderer(this);
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
    	wing.addBox(-50F, 0F, 2F, 100, 2, 70);
    	wing.addBox(-18F, 0F, 72F, 36, 0, 26);
    	wing.rotateAngleY = (float) Math.toRadians(180);
    	
        wingEdges = new ModelRenderer(this);
        wingEdges.setRotationPoint(0, 0, 0);
        wingEdges.addBox(-178F, 24F, -52F, 2, 1, 50);
        wingEdges.addBox(176F, 24F, -52F, 2, 1, 50);
        wingEdges.addBox(-50F, 20F, -186F, 2, 2, 12);
        wingEdges.addBox(48F, 20F, -186F, 2, 2, 12);
    	
    	leftGondola = new ModelRenderer(this);
        leftGondola.setRotationPoint(46, -2, -10);
        for(byte i=9; i>2; --i){
        	leftGondola.addBox(i, i, 4*i - 36, -2*i, -2*i, 4);
        }
        
        leftGondolaSupport1 = new ModelRenderer(this);
        leftGondolaSupport1.setRotationPoint(44, 1, -28);
        leftGondolaSupport1.addBox(-1F, 0F, -1F, 2, 22, 2);
        leftGondolaSupport1.rotateAngleZ = (float) Math.toRadians(40);
        
        leftGondolaSupport2 = new ModelRenderer(this);
        leftGondolaSupport2.setRotationPoint(48, 1, -28);
        leftGondolaSupport2.addBox(-1F, 0F, -1F, 2, 15, 2);
        
        leftGondolaSupport3 = new ModelRenderer(this);
        leftGondolaSupport3.setRotationPoint(47, 16, -12);
        leftGondolaSupport3.addBox(0F, -12F, -1F, 2, 12, 2);
        leftGondolaSupport3.rotateAngleZ = (float) Math.toRadians(20);
        
        leftGondolaSupport4 = new ModelRenderer(this);
        leftGondolaSupport4.setRotationPoint(48, 16, -27);
        leftGondolaSupport4.addBox(-1F, -18F, 0F, 2, 18, 2); 
        leftGondolaSupport4.rotateAngleX = (float) Math.toRadians(-50);
        leftGondolaSupport4.rotateAngleY = (float) Math.toRadians(14);
        
        leftGondolaSupport5 = new ModelRenderer(this);
        leftGondolaSupport5.setRotationPoint(47, 16, -12);
        leftGondolaSupport5.addBox(-2F, -12F, -1F, 2, 12, 2);
        leftGondolaSupport5.rotateAngleZ = (float) Math.toRadians(-20);
        
        leftGondolaSupport6 = new ModelRenderer(this);
        leftGondolaSupport6.setRotationPoint(44, 1, -12);
        leftGondolaSupport6.addBox(-1F, 0F, -1F, 2, 22, 2);
        leftGondolaSupport6.rotateAngleZ = (float) Math.toRadians(40);
        
        leftWheelSupport1 = new ModelRenderer(this);
        leftWheelSupport1.setRotationPoint(42.5F, -29, -11);
        leftWheelSupport1.addBox(-28F, 0, -1, 28, 2, 2);
        leftWheelSupport1.rotateAngleZ = (float) Math.toRadians(-32);
        
        leftWheelSupport2 = new ModelRenderer(this);
        leftWheelSupport2.setRotationPoint(42F, -29, -11);
        leftWheelSupport2.addBox(-48F, 0, -1, 48, 2, 2);
        leftWheelSupport2.rotateAngleZ = (float) Math.toRadians(-32);
        leftWheelSupport2.rotateAngleY = (float) Math.toRadians(-55);
        leftWheelSupport2.rotateAngleX = (float) Math.toRadians(20);
        
        leftWheelMount = new ModelRenderer(this);
    	leftWheelMount.setRotationPoint(46, -24, -11);
    	leftWheelMount.addBox(-1F, 0, -1, 2, 14, 2);
    	leftWheelMount.addBox(-3.5F, -2, -6, 7, 2, 12);
    	leftWheelMount.addBox(2.5F, -6, -4, 1, 4, 8);
    	leftWheelMount.addBox(-3.5F, -6, -4, 1, 4, 8);
    	leftWheelMount.addBox(2.5F, -10, -2, 1, 4, 4);
    	leftWheelMount.addBox(-3.5F, -10, -2, 1, 4, 4);
        
        rightGondola = new ModelRenderer(this);
        rightGondola.setRotationPoint(-46, -2, -10);
        for(byte i=9; i>2; --i){
        	rightGondola.addBox(i, i, 4*i - 36, -2*i, -2*i, 4);
        }

        rightGondolaSupport1 = new ModelRenderer(this);
        rightGondolaSupport1.setRotationPoint(-44, 1, -28);
        rightGondolaSupport1.addBox(-1F, 0F, -1F, 2, 22, 2);
        rightGondolaSupport1.rotateAngleZ = (float) Math.toRadians(-40);
        
        rightGondolaSupport2 = new ModelRenderer(this);
        rightGondolaSupport2.setRotationPoint(-48, 1, -28);
        rightGondolaSupport2.addBox(-1F, 0F, -1F, 2, 15, 2);
        
        rightGondolaSupport3 = new ModelRenderer(this);
        rightGondolaSupport3.setRotationPoint(-47, 16, -12);
        rightGondolaSupport3.addBox(-2F, -12F, -1F, 2, 12, 2);
        rightGondolaSupport3.rotateAngleZ = (float) Math.toRadians(-20);
        
        rightGondolaSupport4 = new ModelRenderer(this);
        rightGondolaSupport4.setRotationPoint(-48, 16, -27);
        rightGondolaSupport4.addBox(-1F, -18F, 0F, 2, 18, 2); 
        rightGondolaSupport4.rotateAngleX = (float) Math.toRadians(-50);
        rightGondolaSupport4.rotateAngleY = (float) Math.toRadians(-14);
        
        rightGondolaSupport5 = new ModelRenderer(this);
        rightGondolaSupport5.setRotationPoint(-47, 16, -12);
        rightGondolaSupport5.addBox(0F, -12F, -1F, 2, 12, 2);
        rightGondolaSupport5.rotateAngleZ = (float) Math.toRadians(20);
        
        rightGondolaSupport6 = new ModelRenderer(this);
        rightGondolaSupport6.setRotationPoint(-44, 1, -12);
        rightGondolaSupport6.addBox(-1F, 0F, -1F, 2, 22, 2);
        rightGondolaSupport6.rotateAngleZ = (float) Math.toRadians(-40);
        
        rightWheelSupport1 = new ModelRenderer(this);
        rightWheelSupport1.setRotationPoint(-42.5F, -29, -11);
        rightWheelSupport1.addBox(0F, 0, -1, 28, 2, 2);
        rightWheelSupport1.rotateAngleZ = (float) Math.toRadians(32);
        
        rightWheelSupport2 = new ModelRenderer(this);
        rightWheelSupport2.setRotationPoint(-42F, -29, -11);
        rightWheelSupport2.addBox(0F, 0, -1, 48, 2, 2);
        rightWheelSupport2.rotateAngleZ = (float) Math.toRadians(32);
        rightWheelSupport2.rotateAngleY = (float) Math.toRadians(55);
        rightWheelSupport2.rotateAngleX = (float) Math.toRadians(20);
        
        rightWheelMount = new ModelRenderer(this);
    	rightWheelMount.setRotationPoint(-46, -24, -11);
    	rightWheelMount.addBox(-1F, 0, -1, 2, 14, 2);
    	rightWheelMount.addBox(-3.5F, -2, -6, 7, 2, 12);
    	rightWheelMount.addBox(2.5F, -6, -4, 1, 4, 8);
    	rightWheelMount.addBox(-3.5F, -6, -4, 1, 4, 8);
    	rightWheelMount.addBox(2.5F, -10, -2, 1, 4, 4);
    	rightWheelMount.addBox(-3.5F, -10, -2, 1, 4, 4);
    	
       	stabilizer = new ModelRenderer(this);
    	stabilizer.setRotationPoint(0F, 20F, -174F);
    	stabilizer.addBox(-48F, 0F, -12F, 96, 2, 12);
    	
        leftStabilizerSupport = new ModelRenderer(this);
        leftStabilizerSupport.setRotationPoint(26F, 20F, -181.99F);
        leftStabilizerSupport.addBox(-2F, -27F, 0F, 2, 27, 2);
        leftStabilizerSupport.rotateAngleZ = (float) Math.toRadians(-56);
        
        rightStabilizerSupport = new ModelRenderer(this);
        rightStabilizerSupport.setRotationPoint(-26F, 20F, -181.99F);
        rightStabilizerSupport.addBox(0F, -27F, 0F, 2, 27, 2);
        rightStabilizerSupport.rotateAngleZ = (float) Math.toRadians(56);
    	
    	tail = new ModelRenderer(this);
    	tail.setRotationPoint(0F, 21F, -186F);
    	for(int i=1; i<15; ++i){
    		tail.addBox(-1F, i, 0F, 2, 1, 20-i);
    	}

    	leftAileron = new ModelRenderer(this);
    	leftAileron.setRotationPoint(0F, 24.5F, -44F);
    	leftAileron.addBox(136F, -0.5F, -8F, 32, 1, 8);
    	
    	rightAileron = new ModelRenderer(this);
    	rightAileron.setRotationPoint(0F, 24.5F, -44F);
    	rightAileron.addBox(-168F, -0.5F, -8F, 32, 1, 8);
    	
    	rudder = new ModelRenderer(this);
    	rudder.setRotationPoint(0F, 19F, -186F);
    	rudder.addBox(-0.5F, 17F, 0F, 1, 5, 6);
    	rudder.addBox(-0.5F, 0F, -12F, 1, 22, 12);
    	rudder.addBox(-0.5F, -1F, -12F, 1, 1, 11);
    	rudder.addBox(-0.5F, -2F, -12F, 1, 1, 10);
    	rudder.addBox(-0.5F, -4F, -12F, 1, 2, 8);
    	rudder.addBox(-0.5F, -5F, -12F, 1, 1, 6);
    	rudder.addBox(-0.5F, -6F, -12F, 1, 1, 4);
    	
    	leftElevator = new ModelRenderer(this);
    	leftElevator.setRotationPoint(0F, 20.5F, -186F);
    	leftElevator.addBox(4F, 0F, -10F, 44, 1, 10);
    	
    	rightElevator = new ModelRenderer(this);
    	rightElevator.setRotationPoint(0F, 20.5F, -186F);
    	rightElevator.addBox(-48F, 0F, -10F, 44, 1, 10);
    	
        elevatorEdges = new ModelRenderer(this);
        elevatorEdges.setRotationPoint(0, 21, -186);
        elevatorEdges.addBox(-50F, -0.5F, -10F, 2, 1, 10);
        elevatorEdges.addBox(48F, -0.5F, -10F, 2, 1, 10);
    }    
    	
    public void renderRegularParts(float aileronAngle, float elevatorAngle, float rudderAngle){
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
        interiorPartitions.render(scale);
        tail.render(scale);
        renderRudder(rudderAngle);
        
        leftGondola.render(scale);
        leftWheelSupport1.render(scale);
        leftWheelSupport2.render(scale);
        
        rightGondola.render(scale);
        rightWheelSupport1.render(scale);
        rightWheelSupport2.render(scale);
    }

    public void renderRotatedTextureParts(float aileronAngle, float elevatorAngle, float rudderAngle){
        bottomFuselage.render(scale);
        bottomRearFuselage.render(scale);
        bottomTailFuselage.render(scale);
        topFuselage5.render(scale);
        topFuselage6.render(scale);
        wing.render(scale);
        stabilizer.render(scale);
        rearWheelStrut.render(scale);
        renderAilerons(aileronAngle);
        renderElevators(elevatorAngle);
        
        leftWheelMount.render(scale);
        leftGondolaSupport1.render(scale);
        leftGondolaSupport2.render(scale);
        leftGondolaSupport3.render(scale);
        leftGondolaSupport4.render(scale);
        leftGondolaSupport5.render(scale);
        leftGondolaSupport6.render(scale);
        leftStabilizerSupport.render(scale);
        
        rightWheelMount.render(scale);
        rightGondolaSupport1.render(scale);
        rightGondolaSupport2.render(scale);
        rightGondolaSupport3.render(scale);
        rightGondolaSupport4.render(scale);
        rightGondolaSupport5.render(scale);
        rightGondolaSupport6.render(scale);
        rightStabilizerSupport.render(scale);
    }

    public void renderColoredParts(float aileronAngle, float elevatorAngle, float rudderAngle){        
        leftNoseFlashing.render(scale);
        rightNoseFlashing.render(scale);
        bottomNose.render(scale);
        leftFrontCowling.render(scale);
        rightFrontCowling.render(scale);
        frontCowling.render(scale);
        wingEdges.render(scale);
        windowFrames.render(scale);
        elevatorEdges.rotateAngleX=elevatorAngle; 
        elevatorEdges.render(scale);
    }
    
	private void renderAilerons(float aileronAngle){
    	leftAileron.rotateAngleX = -aileronAngle;
    	rightAileron.rotateAngleX = aileronAngle;
    	leftAileron.render(scale);
    	rightAileron.render(scale);
    }
    
	private void renderElevators(float elevatorAngle){
    	leftElevator.rotateAngleX=elevatorAngle;
    	rightElevator.rotateAngleX=elevatorAngle;
    	leftElevator.render(scale);
    	rightElevator.render(scale);
    }
    
	private void renderRudder(float rudderAngle){
    	rudder.rotateAngleY=rudderAngle;
    	rudder.render(scale);
    }
}
