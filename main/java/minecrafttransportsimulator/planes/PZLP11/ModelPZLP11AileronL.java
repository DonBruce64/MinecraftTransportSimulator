package minecrafttransportsimulator.planes.PZLP11;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

public class ModelPZLP11AileronL extends ModelBase{
	private static final float scale=0.0625F;
	
    ModelRenderer An3;
    ModelRenderer An1;
    ModelRenderer An5;
    ModelRenderer An2;
    ModelRenderer Ep2;
    ModelRenderer An4;
    ModelRenderer An8;
    ModelRenderer An6;
    ModelRenderer An7;
    ModelRenderer An9;
    ModelRenderer An10;
    ModelRenderer Ep1;
    
    public ModelPZLP11AileronL(){
    	textureWidth = 64;
        textureHeight = 32;
        
          An3 = new ModelRenderer(this, 0, 16);
          An3.addBox(0F, 0F, 0F, 30, 2, 0);
          An3.setRotationPoint(-15F, -2F, 0F);
          An3.setTextureSize(64, 32);
          An3.mirror = true;
          setRotation(An3, 0F, 0F, 0F);
          An1 = new ModelRenderer(this, -2, 18);
          An1.addBox(0F, 0F, 0F, 30, 0, 2);
          An1.setRotationPoint(-15F, 0F, 0F);
          An1.setTextureSize(64, 32);
          An1.mirror = true;
          setRotation(An1, 0F, 0F, 0F);
          An5 = new ModelRenderer(this, 0, 6);
          An5.addBox(0F, 0F, 0F, 0, 2, 6);
          An5.setRotationPoint(-15F, -2F, 0F);
          An5.setTextureSize(64, 32);
          An5.mirror = true;
          setRotation(An5, 0F, 0F, 0F);
          An2 = new ModelRenderer(this, -2, 18);
          An2.addBox(0F, 0F, 0F, 30, 0, 2);
          An2.setRotationPoint(-15F, -2F, 0F);
          An2.setTextureSize(64, 32);
          An2.mirror = true;
          setRotation(An2, 0F, 0F, 0F);
          Ep2 = new ModelRenderer(this, 0, 2);
          Ep2.addBox(0F, 0F, 0F, 15, 1, 1);
          Ep2.setRotationPoint(-15F, -1F, 6F);
          Ep2.setTextureSize(64, 32);
          Ep2.mirror = true;
          setRotation(Ep2, 0F, 0F, 0F);
          An4 = new ModelRenderer(this, 0, 12);
          An4.addBox(0F, 0F, 0F, 0, 2, 2);
          An4.setRotationPoint(15F, -2F, 0F);
          An4.setTextureSize(64, 32);
          An4.mirror = true;
          setRotation(An4, 0F, 0F, 0F);
          An8 = new ModelRenderer(this, -4, 8);
          An8.addBox(0F, 0F, 0F, 15, 0, 4);
          An8.setRotationPoint(-15F, 0F, 2F);
          An8.setTextureSize(64, 32);
          An8.mirror = true;
          setRotation(An8, 0F, 0F, 0F);
          An6 = new ModelRenderer(this, -4, 8);
          An6.addBox(0F, 0F, 0F, 15, 0, 4);
          An6.setRotationPoint(-15F, -2F, 2F);
          An6.setTextureSize(64, 32);
          An6.mirror = true;
          setRotation(An6, 0F, 0F, 0F);
          An7 = new ModelRenderer(this, 0, 4);
          An7.addBox(0F, 0F, 0F, 15, 1, 0);
          An7.setRotationPoint(-15F, -2F, 6F);
          An7.setTextureSize(64, 32);
          An7.mirror = true;
          setRotation(An7, 0F, 0F, 0F);
          An9 = new ModelRenderer(this, 0, 5);
          An9.addBox(0F, 0F, 0F, 15, 1, 0);
          An9.setRotationPoint(0F, -2F, 2F);
          An9.setTextureSize(64, 32);
          An9.mirror = true;
          setRotation(An9, 0F, 0F, 0F);
          An10 = new ModelRenderer(this, 0, 2);
          An10.addBox(0F, 0F, 0F, 0, 2, 4);
          An10.setRotationPoint(0F, -2F, 2F);
          An10.setTextureSize(64, 32);
          An10.mirror = true;
          setRotation(An10, 0F, 0F, 0F);
          Ep1 = new ModelRenderer(this, 0, 0);
          Ep1.addBox(0F, 0F, 0F, 15, 1, 1);
          Ep1.setRotationPoint(0F, -1F, 2F);
          Ep1.setTextureSize(64, 32);
          Ep1.mirror = true;
          setRotation(Ep1, 0F, 0F, 0F);
    }
    
    public void render(){
    	An3.render(scale);
        An1.render(scale);
        An5.render(scale);
        An2.render(scale);
        Ep2.render(scale);
        An4.render(scale);
        An8.render(scale);
        An6.render(scale);
        An7.render(scale);
        An9.render(scale);
        An10.render(scale);
        Ep1.render(scale);
    }
    
    private void setRotation(ModelRenderer model, float x, float y, float z){
      model.rotateAngleX = x;
      model.rotateAngleY = y;
      model.rotateAngleZ = z;
    }
}
