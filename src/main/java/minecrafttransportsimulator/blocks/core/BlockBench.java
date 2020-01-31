package minecrafttransportsimulator.blocks.core;

import java.util.Arrays;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockBench extends ABlockRotatable{
	private final List<String> validPartTypes;
	private final Class<? extends AJSONItem> validJsonClass;
	public final RenderType renderType;

	
	public BlockBench(Class<? extends AJSONItem> validJsonClass, String... validPartTypes){
		super();
		this.setCreativeTab(MTSRegistry.coreTab);
		this.validPartTypes = Arrays.asList(validPartTypes);
		this.validJsonClass = validJsonClass;
		this.renderType = validJsonClass.equals(JSONVehicle.class) ? RenderType.SPINNING3D_EXTENDED : (validJsonClass.equals(JSONPart.class) || validJsonClass.equals(JSONDecor.class) ? RenderType.SPINNING3D : RenderType.SIMPLE2D);
	}
	
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ){
		if(Math.sqrt(player.getDistanceSq(pos)) < 5){
			if(world.isRemote){
				MTS.proxy.openGUI(this, player);
			}
		}
		return true;
	}
	
	public boolean isJSONValid(AJSONItem definition){
		if(definition.getClass().equals(validJsonClass)){
			return validJsonClass.equals(JSONPart.class) ? validPartTypes.contains(((JSONPart) definition).general.type) : true;
		}
		return false;
	}
	
	//Need to override this to make the decor bench render its glass texture correctly.
	@Override
	public BlockRenderLayer getBlockLayer(){
		return BlockRenderLayer.CUTOUT;
	}
	
	public static enum RenderType{
		SIMPLE2D(false, false),
		SPINNING3D(false, true),
		SPINNING3D_EXTENDED(true, true);
		
		public final boolean isForVehicles;
		public final boolean isFor3DModels;
			
		private RenderType(boolean isForVehicles, boolean isFor3DModels){
			this.isForVehicles = isForVehicles;
			this.isFor3DModels = isFor3DModels;
		}
	}
}
