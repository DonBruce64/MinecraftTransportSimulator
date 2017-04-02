package minecrafttransportsimulator.rendering.blockmodels;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

public class ModelPropellerBench extends ModelBase{
	private static final float scale=0.0625F;
	
    ModelRenderer Ctm1;
    ModelRenderer Ctm2;
    ModelRenderer Ctm3;
    ModelRenderer Ctm4;
    ModelRenderer Ctm5;
    ModelRenderer Ctm6;
    ModelRenderer Ctm7;
    ModelRenderer Ctm8;
  
  public ModelPropellerBench(){
    textureWidth = 128;
    textureHeight = 128;
    
      Ctm1 = new ModelRenderer(this, 0, 36);
      Ctm1.addBox(0F, 0F, 0F, 32, 3, 14);
      Ctm1.setRotationPoint(-8F, -9F, 3F);
      Ctm1.setTextureSize(128, 128);
      Ctm1.mirror = true;
      setRotation(Ctm1, 0F, 0F, 0F);
      Ctm2 = new ModelRenderer(this, 0, 79);
      Ctm2.addBox(0F, 0F, 0F, 8, 6, 29);
      Ctm2.setRotationPoint(4F, -6F, 0F);
      Ctm2.setTextureSize(128, 128);
      Ctm2.mirror = true;
      setRotation(Ctm2, 0F, 0F, 0F);
      Ctm3 = new ModelRenderer(this, 0, 0);
      Ctm3.addBox(0F, 0F, 0F, 16, 7, 29);
      Ctm3.setRotationPoint(0F, 0F, 0F);
      Ctm3.setTextureSize(128, 128);
      Ctm3.mirror = true;
      setRotation(Ctm3, 0F, 0F, 0F);
      Ctm4 = new ModelRenderer(this, 0, 53);
      Ctm4.addBox(0F, 0F, 0F, 14, 11, 8);
      Ctm4.setRotationPoint(1F, -17F, 20F);
      Ctm4.setTextureSize(128, 128);
      Ctm4.mirror = true;
      setRotation(Ctm4, 0F, 0F, 0F);
      Ctm5 = new ModelRenderer(this, 24, 53);
      Ctm5.addBox(0F, 0F, 0F, 8, 5, 21);
      Ctm5.setRotationPoint(4F, -22F, 5F);
      Ctm5.setTextureSize(128, 128);
      Ctm5.mirror = true;
      setRotation(Ctm5, 0F, 0F, 0F);
      Ctm6 = new ModelRenderer(this, 21, 18);
      Ctm6.addBox(0F, 0F, 0F, 2, 3, 2);
      Ctm6.setRotationPoint(7F, -17F, 13F);
      Ctm6.setTextureSize(128, 128);
      Ctm6.mirror = true;
      setRotation(Ctm6, 0F, 0F, 0F);
      Ctm7 = new ModelRenderer(this, 25, 23);
      Ctm7.addBox(0F, 0F, 0F, 1, 5, 1);
      Ctm7.setRotationPoint(7.5F, -17F, 6F);
      Ctm7.setTextureSize(128, 128);
      Ctm7.mirror = true;
      setRotation(Ctm7, 0F, 0F, 0F);
      Ctm8 = new ModelRenderer(this, 24, 0);
      Ctm8.addBox(-0.5F, -1F, -0.5F, 1, 2, 1);
      Ctm8.setRotationPoint(8F, -13F, 14F);
      Ctm8.setTextureSize(128, 128);
      Ctm8.mirror = true;
      setRotation(Ctm8, 0F, 0F, 0F);
  }
  
  public void renderBase(){
    Ctm2.render(scale);
    Ctm3.render(scale);
    Ctm4.render(scale);
    Ctm5.render(scale);
    Ctm7.render(scale);
  }
  
  public void renderTable(){
    Ctm1.render(scale);
  }
  
  public void renderBody(){
    Ctm6.render(scale);
  }
  
  public void renderBit(float rotation){
	Ctm8.rotateAngleY = rotation;
    Ctm8.render(scale);
  }
  
  private void setRotation(ModelRenderer model, float x, float y, float z){
    model.rotateAngleX = x;
    model.rotateAngleY = y;
    model.rotateAngleZ = z;
  }
}
