package minecrafttransportsimulator.baseclasses;

import minecrafttransportsimulator.minecrafthelpers.BlockHelper;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
	
public abstract class MTSBlockTileEntity extends MTSBlock implements ITileEntityProvider{
	
    public MTSBlockTileEntity(Material material, float hardness, float resistance){
		super(material, hardness, resistance);
	}

    @Override
	public void breakBlock(World world, int x, int y, int z, Block block, int metadata){
        super.breakBlock(world, x, y, z, block, metadata);
        world.removeTileEntity(x, y, z);
    }
	
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack stack){
		super.onBlockPlacedBy(world, x, y, z, entity, stack);
		float yaw = entity.rotationYaw;
		while(yaw < 0){
			yaw += 360;
		}
		((MTSTileEntity) BlockHelper.getTileEntityFromCoords(world, x, y, z)).rotation = Math.round(yaw%360/45) == 8 ? 0 : (byte) Math.round(yaw%360/45);
	}
	
	@Override
	public TileEntity createNewTileEntity(World world, int metadata){
		return getTileEntity();
	}
	
	public abstract MTSTileEntity getTileEntity();
}
