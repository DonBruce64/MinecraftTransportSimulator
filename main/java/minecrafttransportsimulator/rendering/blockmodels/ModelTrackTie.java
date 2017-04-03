package minecrafttransportsimulator.rendering.blockmodels;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

public class ModelTrackTie extends ModelBase{
	private static final float scale=0.0625F;
	
    ModelRenderer Tie;
    ModelRenderer Base_1;
    ModelRenderer Base_2;
    ModelRenderer Base_3;
    ModelRenderer Base_4;
    ModelRenderer Nail_1;
    ModelRenderer Nail_2;
    ModelRenderer Nail_3;
    ModelRenderer Nail_4;
    ModelRenderer Nail_5;
    ModelRenderer Nail_6;
    ModelRenderer Spike_1;
    ModelRenderer Spike_2;
    ModelRenderer Spike_3;
    ModelRenderer Spike_4;
 
    public ModelTrackTie(){
    	textureWidth = 110;
        textureHeight = 20;
        
          Tie = new ModelRenderer(this, 0, 0);
          Tie.addBox(-24F, -3F, 0F, 48, 3, 6);
          Tie.setRotationPoint(0F, 0F, 0F);
          Tie.setTextureSize(64, 32);
          Tie.mirror = true;
          setRotation(Tie, 0F, 0F, 0F);
          Base_1 = new ModelRenderer(this, 0, 10);
          Base_1.addBox(0F, 0F, 0F, 4, 1, 5);
          Base_1.setRotationPoint(-21F, -3.5F, 0.5F);
          Base_1.setTextureSize(64, 32);
          Base_1.mirror = true;
          setRotation(Base_1, 0F, 0F, 0F);
          Base_2 = new ModelRenderer(this, 0, 10);
          Base_2.addBox(0F, 0F, 0F, 4, 1, 5);
          Base_2.setRotationPoint(-13F, -3.5F, 0.5F);
          Base_2.setTextureSize(64, 32);
          Base_2.mirror = true;
          setRotation(Base_2, 0F, 0F, 0F);
          Base_3 = new ModelRenderer(this, 0, 10);
          Base_3.addBox(0F, 0F, 0F, 4, 1, 5);
          Base_3.setRotationPoint(17F, -3.5F, 0.5F);
          Base_3.setTextureSize(64, 32);
          Base_3.mirror = true;
          setRotation(Base_3, 0F, 0F, 0F);
          Base_4 = new ModelRenderer(this, 0, 10);
          Base_4.addBox(0F, 0F, 0F, 4, 1, 5);
          Base_4.setRotationPoint(9F, -3.5F, 0.5F);
          Base_4.setTextureSize(64, 32);
          Base_4.mirror = true;
          setRotation(Base_4, 0F, 0F, 0F);
          Nail_1 = new ModelRenderer(this, 19, 10);
          Nail_1.addBox(-0.5F, 0F, -0.5F, 1, 1, 1);
          Nail_1.setRotationPoint(-20F, -4F, 3.5F);
          Nail_1.setTextureSize(64, 32);
          Nail_1.mirror = true;
          setRotation(Nail_1, 0F, 0.7853982F, 0F);
          Nail_2 = new ModelRenderer(this, 19, 10);
          Nail_2.addBox(-0.5F, 0F, -0.5F, 1, 1, 1);
          Nail_2.setRotationPoint(20F, -4F, 2.5F);
          Nail_2.setTextureSize(64, 32);
          Nail_2.mirror = true;
          setRotation(Nail_2, 0F, 0.7853982F, 0F);
          Nail_3 = new ModelRenderer(this, 19, 10);
          Nail_3.addBox(-0.5F, 0F, -0.5F, 1, 1, 1);
          Nail_3.setRotationPoint(-10F, -4F, 1.5F);
          Nail_3.setTextureSize(64, 32);
          Nail_3.mirror = true;
          setRotation(Nail_3, 0F, 0.7853982F, 0F);
          Nail_4 = new ModelRenderer(this, 19, 10);
          Nail_4.addBox(-0.5F, 0F, -0.5F, 1, 1, 1);
          Nail_4.setRotationPoint(-10F, -4F, 4.5F);
          Nail_4.setTextureSize(64, 32);
          Nail_4.mirror = true;
          setRotation(Nail_4, 0F, 0.7853982F, 0F);
          Nail_5 = new ModelRenderer(this, 19, 10);
          Nail_5.addBox(-0.5F, 0F, -0.5F, 1, 1, 1);
          Nail_5.setRotationPoint(10F, -4F, 1.5F);
          Nail_5.setTextureSize(64, 32);
          Nail_5.mirror = true;
          setRotation(Nail_5, 0F, 0.7853982F, 0F);
          Nail_6 = new ModelRenderer(this, 19, 10);
          Nail_6.addBox(-0.5F, 0F, -0.5F, 1, 1, 1);
          Nail_6.setRotationPoint(10F, -4F, 4.5F);
          Nail_6.setTextureSize(64, 32);
          Nail_6.mirror = true;
          setRotation(Nail_6, 0F, 0.7853982F, 0F);
          Spike_1 = new ModelRenderer(this, 19, 14);
          Spike_1.addBox(0F, 0F, 0F, 1, 1, 1);
          Spike_1.setRotationPoint(-18F, -4F, 1.5F);
          Spike_1.setTextureSize(64, 32);
          Spike_1.mirror = true;
          setRotation(Spike_1, 0F, 0F, 0F);
          Spike_2 = new ModelRenderer(this, 19, 14);
          Spike_2.addBox(0F, 0F, 0F, 1, 1, 1);
          Spike_2.setRotationPoint(-13F, -4F, 1.5F);
          Spike_2.setTextureSize(64, 32);
          Spike_2.mirror = true;
          setRotation(Spike_2, 0F, 0F, 0F);
          Spike_3 = new ModelRenderer(this, 19, 14);
          Spike_3.addBox(0F, 0F, 0F, 1, 1, 1);
          Spike_3.setRotationPoint(12F, -4F, 3.5F);
          Spike_3.setTextureSize(64, 32);
          Spike_3.mirror = true;
          setRotation(Spike_3, 0F, 0F, 0F);
          Spike_4 = new ModelRenderer(this, 19, 14);
          Spike_4.addBox(0F, 0F, 0F, 1, 1, 1);
          Spike_4.setRotationPoint(17F, -4F, 3.5F);
          Spike_4.setTextureSize(64, 32);
          Spike_4.mirror = true;
          setRotation(Spike_4, 0F, 0F, 0F);
    }    
    
    public void render(){
    	Tie.render(scale);
        Base_1.render(scale);
        Base_2.render(scale);
        Base_3.render(scale);
        Base_4.render(scale);
        Nail_1.render(scale);
        Nail_2.render(scale);
        Nail_3.render(scale);
        Nail_4.render(scale);
        Nail_5.render(scale);
        Nail_6.render(scale);
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