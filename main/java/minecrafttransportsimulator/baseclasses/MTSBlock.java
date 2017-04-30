package minecrafttransportsimulator.baseclasses;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class MTSBlock extends Block{

	public MTSBlock(Material material, float hardness, float resistance){
		super(material);
		this.setHardness(hardness);
		this.setResistance(resistance);
		this.setDefaultBlockBounds();
	}
		
	protected abstract boolean isBlock3D();
		
	protected abstract void setDefaultBlockBounds();
	
	protected abstract void setBlockBoundsFromMetadata(int metadata);
	
	protected int getBlockMetadata(World world, int x, int y, int z){
		return world.getBlockMetadata(x, y, z);
	}
	
	public void setBlockMetadata(World world, int x, int y, int z, int metadata){
		world.setBlockMetadataWithNotify(x, y, z, metadata, 3);
	}

	@Override
	public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB box, List list, Entity entity){
		this.setBlockBoundsFromMetadata(this.getBlockMetadata(world, x, y, z));
		super.addCollisionBoxesToList(world, x, y, z, box, list, entity);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z){
		this.setBlockBoundsFromMetadata(this.getBlockMetadata(world, x, y, z));
		return super.getSelectedBoundingBoxFromPool(world, x, y, z);
	}
	
	@Override
    public int getRenderType(){
        return isBlock3D() ? -1 : super.getRenderType();
    }
	
	@Override
    public boolean renderAsNormalBlock(){
        return !isBlock3D();
    }
	
	@Override
	public boolean isOpaqueCube(){
		return !isBlock3D();
	}
}
