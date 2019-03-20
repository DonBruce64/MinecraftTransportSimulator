package minecrafttransportsimulator.blocks.core;

import java.util.Random;

import javax.annotation.Nullable;

import minecrafttransportsimulator.items.blocks.ItemBlockRotatable;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;


public class BlockRotatable extends Block{
	public final EnumFacing orientation;
	public final ItemBlockRotatable item;
	
	public BlockRotatable(EnumFacing orientation, ItemBlockRotatable item){
		super(Material.ROCK);
		this.orientation = orientation;
		this.item = item;
		this.fullBlock = false;
		this.setHardness(5.0F);
		this.setResistance(10.0F);
	}
	
	@Override
	@Nullable
    public Item getItemDropped(IBlockState state, Random rand, int fortune){
        return item;
    }
	
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
