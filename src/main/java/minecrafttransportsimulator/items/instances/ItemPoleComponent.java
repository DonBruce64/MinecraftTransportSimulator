package minecrafttransportsimulator.items.instances;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.instances.BlockPole;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.components.IItemBlock;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;

public class ItemPoleComponent extends AItemSubTyped<JSONPoleComponent> implements IItemBlock{
	
	public ItemPoleComponent(JSONPoleComponent definition, String subName){
		super(definition, subName);
	}
	
	public boolean onBlockClicked(IWrapperWorld world, IWrapperPlayer player, Point3i point, Axis axis){
		if(definition.general.type.equals("core")){
			return ((IItemBlock) this).placeBlock(world, player, point, axis);
		}else{
			return false;
		}
	}
	
	@Override
	public Class<? extends ABlockBase> getBlockClass(){
		return BlockPole.class;
	}
}
