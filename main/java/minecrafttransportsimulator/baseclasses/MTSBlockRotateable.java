package minecrafttransportsimulator.baseclasses;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
	
public abstract class MTSBlockRotateable extends BlockContainer{
	
    public MTSBlockRotateable(Material material){
		super(material);
		this.fullBlock = false;
	}

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase entity, ItemStack stack){
        super.onBlockPlacedBy(world, pos, state, entity, stack);
        float yaw = entity.rotationYaw;
        while(yaw < 0){
            yaw += 360;
        }
        if(this.canRotateDiagonal()){
        	((MTSTileEntity) world.getTileEntity(pos)).rotation = Math.round(yaw%360/45) == 8 ? 0 : (byte) Math.round(yaw%360/45);
        }else{
        	((MTSTileEntity) world.getTileEntity(pos)).rotation = (byte) (Math.round(yaw%360/90) == 8 ? 0 : Math.round(yaw%360/90)*2);
        }
    }

	@Override
	public abstract MTSTileEntity createNewTileEntity(World worldIn, int meta);
    
    protected abstract boolean canRotateDiagonal();
    
    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state){
        return false;
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state){
        return false;
    }
}
