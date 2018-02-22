package minecrafttransportsimulator.rendering.partmodels;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

public class ModelWheel extends ModelBase{	
    private final float scale=0.0625F;
    private ModelRenderer smallInnerWheel;
    private ModelRenderer smallOuterWheel;
    private ModelRenderer mediumInnerWheel;
    private ModelRenderer mediumOuterWheel;
    private ModelRenderer largeInnerWheel;
    private ModelRenderer largeOuterWheel;
 
    public ModelWheel(){
    	this.textureWidth = 16;
    	this.textureHeight = 16;
    	smallInnerWheel = new ModelRenderer(this, 0, 0);
    	smallInnerWheel.setTextureSize(textureWidth, textureHeight);
    	smallInnerWheel.setRotationPoint(0F, 3F, 0F);
    	smallInnerWheel.addBox(-2F, -1.5F, -1.5F, 4, 3, 3);
    	
    	smallOuterWheel = new ModelRenderer(this, 0, 0);
    	smallOuterWheel.setTextureSize(textureWidth, textureHeight);
    	smallOuterWheel.setRotationPoint(0F, 3F, 0F);
    	smallOuterWheel.addBox(-1.5F, -2.5F, -3.5F, 3, 5, 1);
    	smallOuterWheel.addBox(-1.5F, -2.5F, 2.5F, 3, 5, 1);
    	smallOuterWheel.addBox(-1.5F, -3.5F, -2.5F, 3, 7, 5);
    	
    	mediumInnerWheel = new ModelRenderer(this, 0, 0);
    	mediumInnerWheel.setTextureSize(textureWidth, textureHeight);
    	mediumInnerWheel.setRotationPoint(0F, 5F, 0F);
    	mediumInnerWheel.addBox(-2.5F, -2.5F, -2.5F, 5, 5, 5);
    	
    	mediumOuterWheel = new ModelRenderer(this, 0, 0);
    	mediumOuterWheel.setTextureSize(textureWidth, textureHeight);
    	mediumOuterWheel.setRotationPoint(0F, 5F, 0F);
    	mediumOuterWheel.addBox(-2F, -2.5F, -5.5F, 4, 5, 1);
    	mediumOuterWheel.addBox(-2F, -4.5F, -4.5F, 4, 9, 2);
    	mediumOuterWheel.addBox(-2F, -5.5F, -2.5F, 4, 11, 5);
    	mediumOuterWheel.addBox(-2F, -4.5F, 2.5F, 4, 9, 2);
    	mediumOuterWheel.addBox(-2F, -2.5F, 4.5F, 4, 5, 1);
    	
    	largeInnerWheel = new ModelRenderer(this, 0, 0);
    	largeInnerWheel.setTextureSize(textureWidth, textureHeight);
    	largeInnerWheel.setRotationPoint(0F, 7F, 0F);
    	largeInnerWheel.addBox(-3F, -2.5F, -3.5F, 6, 5, 1);
    	largeInnerWheel.addBox(-3F, -3.5F, -2.5F, 6, 7, 5);
    	largeInnerWheel.addBox(-3F, -2.5F, 2.5F, 6, 5, 1);
    	
    	largeOuterWheel = new ModelRenderer(this, 0, 0);
    	largeOuterWheel.setTextureSize(textureWidth, textureHeight);
    	largeOuterWheel.setRotationPoint(0F, 7F, 0F);
    	largeOuterWheel.addBox(-2.5F, -2.5F, -7.5F, 5, 5, 1);
    	largeOuterWheel.addBox(-2.5F, -4.5F, -6.5F, 5, 9, 2);
    	largeOuterWheel.addBox(-2.5F, -6.5F, -4.5F, 5, 13, 2);
    	largeOuterWheel.addBox(-2.5F, -7.5F, -2.5F, 5, 15, 5);
    	largeOuterWheel.addBox(-2.5F, -6.5F, 2.5F, 5, 13, 2);
    	largeOuterWheel.addBox(-2.5F, -4.5F, 4.5F, 5, 9, 2);
    	largeOuterWheel.addBox(-2.5F, -2.5F, 6.5F, 5, 5, 1);
    }    
    
    public void renderSmallInnerWheel(float wheelRotation){
		smallInnerWheel.rotateAngleX = wheelRotation;
		smallInnerWheel.render(scale);
    }
    
    public void renderSmallOuterWheel(float wheelRotation){
		smallOuterWheel.rotateAngleX = wheelRotation;
    	smallOuterWheel.render(scale);
    }
    
    public void renderMediumInnerWheel(float wheelRotation){
    	mediumInnerWheel.rotateAngleX = wheelRotation;
		mediumInnerWheel.render(scale);
    }
    
    public void renderMediumOuterWheel(float wheelRotation){
    	mediumOuterWheel.rotateAngleX = wheelRotation;
		mediumOuterWheel.render(scale);
    }
    
    public void renderLargeInnerWheel(float wheelRotation){
		largeInnerWheel.rotateAngleX = wheelRotation;
		largeInnerWheel.render(scale);
    }
    
    public void renderLargeOuterWheel(float wheelRotation){
		largeOuterWheel.rotateAngleX = wheelRotation;
		largeOuterWheel.render(scale);
    }
}