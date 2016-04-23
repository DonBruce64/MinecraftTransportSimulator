package minecraftflightsimulator.models;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ModelPropeller extends ModelBase{
    private final float scale=0.0625F;
    private ModelRenderer shortBlade;
    private ModelRenderer mediumBlade;
    private ModelRenderer longBlade;
    private ModelRenderer shaft;
 
    public ModelPropeller(){
    	this.textureWidth = 16;
    	this.textureHeight = 16;
    	shortBlade = new ModelRenderer(this, 0, 0);
    	shortBlade.setTextureSize(textureWidth, textureHeight);
    	shortBlade.setRotationPoint(0F, 8F, 0F);
    	shortBlade.addBox(-1F, 0F, -1F, 2, 12, 2);

    	mediumBlade = new ModelRenderer(this, 0, 0);
    	mediumBlade.setTextureSize(textureWidth, textureHeight);
    	mediumBlade.setRotationPoint(0F, 8F, 0F);
    	mediumBlade.addBox(-1F, 0F, -1F, 2, 18, 2);
    	
    	longBlade = new ModelRenderer(this, 0, 0);
    	longBlade.setTextureSize(textureWidth, textureHeight);
    	longBlade.setRotationPoint(0F, 8F, 0F);
    	longBlade.addBox(-1F, 0F, -1F, 2, 24, 2);
    	
    	shaft = new ModelRenderer(this, 0, 0);
    	shaft.setTextureSize(textureWidth, textureHeight);
    	shaft.setRotationPoint(0F, 8F, 0F);
    	shaft.addBox(-1.5F, -1.5F, -10F, 3, 3, 10);
    }    
    
    public void renderPropellor(int numberBlades, int diameter, float propellerRotation){
    	shaft.rotateAngleZ = propellerRotation;
    	shaft.render(scale);
    	for(int i=0; i<numberBlades; ++i){
    		if(diameter >= 100){
    			longBlade.rotateAngleZ = propellerRotation + i*6.283185312F/numberBlades;
    			longBlade.render(scale);
    		}else if(diameter >= 85){
    			mediumBlade.rotateAngleZ = propellerRotation + i*6.283185312F/numberBlades;
    			mediumBlade.render(scale);
    		}else{
    			shortBlade.rotateAngleZ = propellerRotation + i*6.283185312F/numberBlades;
    			shortBlade.render(scale);
    		}
    	}
    }
}