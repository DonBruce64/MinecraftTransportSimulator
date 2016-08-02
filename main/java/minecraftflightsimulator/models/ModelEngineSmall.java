package minecraftflightsimulator.models;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

public class ModelEngineSmall extends ModelBase{
    private final float scale=0.0625F;
    ModelRenderer Se1;
    ModelRenderer Se2;
    ModelRenderer Se3;
    ModelRenderer Se4;
    ModelRenderer Se5;
    ModelRenderer Se6;
    ModelRenderer Se7;
    ModelRenderer Se8;
    ModelRenderer Se9;
    ModelRenderer Se10;
    ModelRenderer Se11;
    ModelRenderer Se12;
    ModelRenderer Se13;
    ModelRenderer Se14;
 
    public ModelEngineSmall(){
    	textureWidth = 64;
        textureHeight = 33;
        
          Se1 = new ModelRenderer(this, 0, 0);
          Se1.addBox(-3.5F, -4.5F, -9F, 7, 7, 18);
          Se1.setRotationPoint(0F, 1F, 0F);
          Se1.setTextureSize(64, 33);
          Se1.mirror = true;
          setRotation(Se1, 0F, 0F, 0F);
          Se2 = new ModelRenderer(this, 34, 25);
          Se2.addBox(-4F, 0F, 0F, 4, 4, 4);
          Se2.setRotationPoint(-3F, -1F, -4F);
          Se2.setTextureSize(64, 33);
          Se2.mirror = true;
          setRotation(Se2, 0F, 0F, 0F);
          Se3 = new ModelRenderer(this, 34, 25);
          Se3.addBox(-4F, 0F, 0F, 4, 4, 4);
          Se3.setRotationPoint(-3F, -1F, 4F);
          Se3.setTextureSize(64, 33);
          Se3.mirror = true;
          setRotation(Se3, 0F, 0F, 0F);
          Se4 = new ModelRenderer(this, 0, 0);
          Se4.addBox(0F, 0F, 0F, 4, 4, 4);
          Se4.setRotationPoint(3F, -1F, 4F);
          Se4.setTextureSize(64, 33);
          Se4.mirror = true;
          setRotation(Se4, 0F, 0F, 0F);
          Se5 = new ModelRenderer(this, 50, 23);
          Se5.addBox(-4F, -2.5F, -2.5F, 2, 5, 5);
          Se5.setRotationPoint(-5F, 0F, 6F);
          Se5.setTextureSize(64, 33);
          Se5.mirror = true;
          setRotation(Se5, 0F, 0F, 0F);
          Se6 = new ModelRenderer(this, 0, 0);
          Se6.addBox(0F, 0F, 0F, 4, 4, 4);
          Se6.setRotationPoint(3F, -1F, -4F);
          Se6.setTextureSize(64, 33);
          Se6.mirror = true;
          setRotation(Se6, 0F, 0F, 0F);
          Se7 = new ModelRenderer(this, 50, 23);
          Se7.addBox(-4F, -2.5F, -2.5F, 2, 5, 5);
          Se7.setRotationPoint(-5F, 0F, -2F);
          Se7.setTextureSize(64, 33);
          Se7.mirror = true;
          setRotation(Se7, 0F, 0F, 0F);
          Se8 = new ModelRenderer(this, 1, 8);
          Se8.addBox(0F, -2.5F, -2.5F, 2, 5, 5);
          Se8.setRotationPoint(7F, 0F, 6F);
          Se8.setTextureSize(64, 33);
          Se8.mirror = true;
          setRotation(Se8, 0F, 0F, 0F);
          Se9 = new ModelRenderer(this, 1, 8);
          Se9.addBox(0F, -2.5F, -2.5F, 2, 5, 5);
          Se9.setRotationPoint(7F, 0F, -2F);
          Se9.setTextureSize(64, 33);
          Se9.mirror = true;
          setRotation(Se9, 0F, 0F, 0F);
          Se10 = new ModelRenderer(this, 32, 16);
          Se10.addBox(0F, 0F, -0.5F, 5, 1, 1);
          Se10.setRotationPoint(-8F, -2F, 6F);
          Se10.setTextureSize(64, 33);
          Se10.mirror = true;
          setRotation(Se10, 0F, 0F, -0.2617994F);
          Se11 = new ModelRenderer(this, 32, 16);
          Se11.addBox(0F, 0F, -0.5F, 5, 1, 1);
          Se11.setRotationPoint(-8F, -2F, -2F);
          Se11.setTextureSize(64, 33);
          Se11.mirror = true;
          setRotation(Se11, 0F, 0F, -0.2617994F);
          Se12 = new ModelRenderer(this, 32, 16);
          Se12.addBox(-5F, 0F, -0.5F, 5, 1, 1);
          Se12.setRotationPoint(8F, -2F, 6F);
          Se12.setTextureSize(64, 33);
          Se12.mirror = true;
          setRotation(Se12, 0F, 0F, 0.2617994F);
          Se13 = new ModelRenderer(this, 32, 16);
          Se13.addBox(-5F, 0F, -0.5F, 5, 1, 1);
          Se13.setRotationPoint(8F, -2F, -2F);
          Se13.setTextureSize(64, 33);
          Se13.mirror = true;
          setRotation(Se13, 0F, 0F, 0.2617994F);
          Se14 = new ModelRenderer(this, 32, 0);
          Se14.addBox(-2F, 0F, 0F, 4, 2, 11);
          Se14.setRotationPoint(0F, 3F, -5F);
          Se14.setTextureSize(64, 33);
          Se14.mirror = true;
          setRotation(Se14, 0F, 0F, 0F);
    }    
    
    public void render(){
    	Se1.render(scale);
        Se2.render(scale);
        Se3.render(scale);
        Se4.render(scale);
        Se5.render(scale);
        Se6.render(scale);
        Se7.render(scale);
        Se8.render(scale);
        Se9.render(scale);
        Se10.render(scale);
        Se11.render(scale);
        Se12.render(scale);
        Se13.render(scale);
        Se14.render(scale);
    }
    
    private void setRotation(ModelRenderer model, float x, float y, float z){
      model.rotateAngleX = x;
      model.rotateAngleY = y;
      model.rotateAngleZ = z;
    }
}