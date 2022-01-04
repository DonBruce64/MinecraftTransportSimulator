package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController.SignalGroup;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;

/**Traffic signal component for poles.  This doesn't tick, as the state of the light
 * is by default having the unlinked light on until changed by a {@link TileEntitySignalController}.
 * 
 * @author don_bruce
 */
public class TileEntityPole_TrafficSignal extends ATileEntityPole_Component{
	public TileEntitySignalController linkedController;
	
	public TileEntityPole_TrafficSignal(TileEntityPole core, WrapperPlayer placingPlayer,  Axis axis, WrapperNBT data){
		super(core, placingPlayer, axis, data);
	}
	
	@Override
	public boolean update(){
		if(super.update() && linkedController != null){
			variablesOn.clear();
			if(linkedController.isValid && linkedController.controlledSignals.contains(this)){
				for(SignalGroup group : linkedController.signalGroups.get(axis)){
					if(group.currentLight.lowercaseName != null){
						variablesOn.add(group.currentLight.lowercaseName);
					}
				}
			}else{
				linkedController = null;
			}
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public double getRawVariableValue(String variable, float partialTicks){
		switch(variable){
			case("linked"): return linkedController != null ? 1 : 0;
		}
		
		return super.getRawVariableValue(variable, partialTicks);
	}
}
