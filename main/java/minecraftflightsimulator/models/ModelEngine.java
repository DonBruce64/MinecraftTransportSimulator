package minecraftflightsimulator.models;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ModelEngine extends ModelBase{
    private final float scale=0.0625F;
    private ModelRenderer engineSmall;
    private ModelRenderer engineLarge;
 
    public ModelEngine(){
    	this.textureWidth = 16;
    	this.textureHeight = 16;
    	engineSmall = new ModelRenderer(this, 0, 0);
    	engineSmall.setTextureSize(textureWidth, textureHeight);
        engineSmall.setRotationPoint(0F, 2F, 0F);
        engineSmall.addBox(-4.5F, 0F, -9F, 9, 9, 18);
        
        engineLarge = new ModelRenderer(this, 0, 0);
        engineLarge.setTextureSize(textureWidth, textureHeight);
        engineLarge.setRotationPoint(0F, 2F, 0F);
        engineLarge.addBox(-4.5F, 0F, -9F, 9, 9, 22);
        engineLarge.addBox(-2.5F, -3F, -7F, 5, 3, 18);
        engineLarge.addBox(-2.5F, 9F, -7F, 5, 3, 18);
        engineLarge.addBox(-7.5F, 2F, -7F, 3, 5, 18);
        engineLarge.addBox(4.5F, 2F, -7F, 3, 5, 18);
    }    
    
    public void renderSmallEngine(){
    	engineSmall.render(scale);
    }
    
    public void renderLargeEngine(){
    	engineLarge.render(scale);
    }
}