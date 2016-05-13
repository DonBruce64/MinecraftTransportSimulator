package minecraftflightsimulator.models;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ModelSkid extends ModelBase{
	private static final float scale=0.0625F;
	
    ModelRenderer Shape1;
    ModelRenderer Shape2;
    ModelRenderer Shape3;
 
    public ModelSkid(){
    	textureWidth = 12;
        textureHeight = 12;
        
          Shape1 = new ModelRenderer(this, 4, 0);
          Shape1.addBox(0F, 0F, 0F, 1, 4, 1);
          Shape1.setRotationPoint(0F, 0F, 0F);
          Shape1.setTextureSize(12, 12);
          Shape1.mirror = true;
          setRotation(Shape1, 0F, 0F, 0F);
          Shape2 = new ModelRenderer(this, 0, 0);
          Shape2.addBox(0F, 0F, 0F, 1, 6, 1);
          Shape2.setRotationPoint(0F, 0F, -4F);
          Shape2.setTextureSize(12, 12);
          Shape2.mirror = true;
          setRotation(Shape2, 0.7853982F, 0F, 0F);
          Shape3 = new ModelRenderer(this, 1, 5);
          Shape3.addBox(0F, 0.3F, 0F, 2, 1, 3);
          Shape3.setRotationPoint(-0.5F, 3F, -1F);
          Shape3.setTextureSize(12, 12);
          Shape3.mirror = true;
          setRotation(Shape3, 0F, 0F, 0F);
    }    
    
    public void render(){
      Shape1.render(scale);
      Shape2.render(scale);
      Shape3.render(scale);
    }
    
    private void setRotation(ModelRenderer model, float x, float y, float z){
      model.rotateAngleX = x;
      model.rotateAngleY = y;
      model.rotateAngleZ = z;
    }
}