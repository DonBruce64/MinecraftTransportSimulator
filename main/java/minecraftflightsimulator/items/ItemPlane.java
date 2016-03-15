package minecraftflightsimulator.items;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.EntityCore;
import minecraftflightsimulator.entities.EntityPlane;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class ItemPlane extends Item{
	
	public ItemPlane(){
		super();
		this.setCreativeTab(MFS.tabMFS);
	}
	
	public boolean canSpawnPlane(World world, EntityPlane plane){
		float[][] coreLocations = plane.getCoreLocations();
		EntityCore[] spawnedCores = new EntityCore[coreLocations.length];
		for(int i=0; i<coreLocations.length; ++i){
			EntityCore core = new EntityCore(world, plane, plane.UUID, coreLocations[i][0], coreLocations[i][1], coreLocations[i][2]);
			world.spawnEntityInWorld(core);
			spawnedCores[i] = core;
			if(!core.worldObj.getCollidingBoundingBoxes(core, core.boundingBox).isEmpty()){
				for(int j=0; j<=i; ++j){
					spawnedCores[j].setDead();
				}
				plane.setDead();
				return false;
			}
		}
		world.spawnEntityInWorld(plane);
		return true;
	}
	
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister p_94581_1_){}
}
