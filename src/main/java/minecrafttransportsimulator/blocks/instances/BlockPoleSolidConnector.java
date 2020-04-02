package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.wrappers.WrapperBlockAxial;

public class BlockPoleSolidConnector extends BlockPole{
	
	public BlockPoleSolidConnector(WrapperBlockAxial wrapperReference){
		super(wrapperReference);
	}
	
	@Override
	public boolean canConnectToSolids(){
		return true;
	}
}
