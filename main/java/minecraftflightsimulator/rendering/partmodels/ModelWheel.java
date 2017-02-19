package minecraftflightsimulator.rendering.partmodels;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

public class ModelWheel extends ModelBase{	
    private final float scale=0.0625F;
    private ModelRenderer smallInnerWheel;
    private ModelRenderer smallOuterWheel;
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
    	
    	largeInnerWheel = new ModelRenderer(this, 0, 0);
    	largeInnerWheel.setTextureSize(textureWidth, textureHeight);
    	largeInnerWheel.setRotationPoint(0F, 5F, 0F);
    	largeInnerWheel.addBox(-2.5F, -2.5F, -2.5F, 5, 5, 5);
    	
    	largeOuterWheel = new ModelRenderer(this, 0, 0);
    	largeOuterWheel.setTextureSize(textureWidth, textureHeight);
    	largeOuterWheel.setRotationPoint(0F, 5F, 0F);
    	largeOuterWheel.addBox(-2F, -2.5F, -5.5F, 4, 5, 1);
    	largeOuterWheel.addBox(-2F, -4.5F, -4.5F, 4, 9, 2);
    	largeOuterWheel.addBox(-2F, -5.5F, -2.5F, 4, 11, 5);
    	largeOuterWheel.addBox(-2F, -4.5F, 2.5F, 4, 9, 2);
    	largeOuterWheel.addBox(-2F, -2.5F, 4.5F, 4, 5, 1);
    }    
    
    public void renderSmallInnerWheel(float wheelRotation){
		smallInnerWheel.rotateAngleX = wheelRotation;
		smallInnerWheel.render(scale);
    }
    
    public void renderSmallOuterWheel(float wheelRotation){
		smallOuterWheel.rotateAngleX = wheelRotation;
    	smallOuterWheel.render(scale);
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