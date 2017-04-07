package minecrafttransportsimulator.rendering.blockmodels;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

public class ModelTrackTie extends ModelBase{
	private static final float scale=0.0625F;
	
	ModelRenderer Tie;
    ModelRenderer Base_1;
    ModelRenderer Base_2;
    ModelRenderer Spike_1;
    ModelRenderer Spike_2;
    ModelRenderer Spike_3;
    ModelRenderer Spike_4;
 
    public ModelTrackTie(){
    	textureWidth = 110;
        textureHeight = 16;
        
          Tie = new ModelRenderer(this, 0, 0);
          Tie.addBox(-24F, -3F, 0F, 48, 3, 6);
          Tie.setRotationPoint(0F, 0F, 0F);
          Tie.setTextureSize(110, 16);
          Tie.mirror = true;
          setRotation(Tie, 0F, 0F, 0F);
          Base_1 = new ModelRenderer(this, 0, 10);
          Base_1.addBox(0F, 0F, 0F, 10, 1, 5);
          Base_1.setRotationPoint(-20F, -3.5F, 0.5F);
          Base_1.setTextureSize(110, 16);
          Base_1.mirror = true;
          setRotation(Base_1, 0F, 0F, 0F);
          Base_2 = new ModelRenderer(this, 0, 10);
          Base_2.addBox(0F, 0F, 0F, 10, 1, 5);
          Base_2.setRotationPoint(10F, -3.5F, 0.5F);
          Base_2.setTextureSize(110, 16);
          Base_2.mirror = true;
          setRotation(Base_2, 0F, 0F, 0F);
          Spike_1 = new ModelRenderer(this, 31, 10);
          Spike_1.addBox(-0.5F, 0F, -0.5F, 1, 1, 1);
          Spike_1.setRotationPoint(-17F, -4.5F, 2.5F);
          Spike_1.setTextureSize(110, 16);
          Spike_1.mirror = true;
          setRotation(Spike_1, 0F, 0.7853982F, 0F);
          Spike_2 = new ModelRenderer(this, 31, 10);
          Spike_2.addBox(-0.5F, 0F, -0.5F, 1, 1, 1);
          Spike_2.setRotationPoint(-13F, -4.5F, 2.5F);
          Spike_2.setTextureSize(110, 16);
          Spike_2.mirror = true;
          setRotation(Spike_2, 0F, 0.7853982F, 0F);
          Spike_3 = new ModelRenderer(this, 31, 10);
          Spike_3.addBox(-0.5F, 0F, -0.5F, 1, 1, 1);
          Spike_3.setRotationPoint(13F, -4.5F, 3.5F);
          Spike_3.setTextureSize(110, 16);
          Spike_3.mirror = true;
          setRotation(Spike_3, 0F, 0.7853982F, 0F);
          Spike_4 = new ModelRenderer(this, 31, 10);
          Spike_4.addBox(-0.5F, 0F, -0.5F, 1, 1, 1);
          Spike_4.setRotationPoint(17F, -4.5F, 3.5F);
          Spike_4.setTextureSize(110, 16);
          Spike_4.mirror = true;
          setRotation(Spike_4, 0F, 0.7853982F, 0F);
    }    
    
    public void render(){
        Tie.render(scale);
        Base_1.render(scale);
        Base_2.render(scale);
        Spike_1.render(scale);
        Spike_2.render(scale);
        Spike_3.render(scale);
        Spike_4.render(scale);
    }
    
    private void setRotation(ModelRenderer model, float x, float y, float z)
    {
      model.rotateAngleX = x;
      model.rotateAngleY = y;
      model.rotateAngleZ = z;
    }
}