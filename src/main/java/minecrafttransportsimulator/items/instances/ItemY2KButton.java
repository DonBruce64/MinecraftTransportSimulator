package minecrafttransportsimulator.items.instances;

import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlAnalog;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlDigital;
import minecrafttransportsimulator.packets.instances.PacketPartEngine;
import minecrafttransportsimulator.packets.instances.PacketPartEngine.Signal;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.rendering.components.LightType;

public class ItemY2KButton extends AItemBase{
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, WrapperNBT data){
		for(byte i=1; i<=4; ++i){
			tooltipLines.add(InterfaceCore.translate("info.item.y2kbutton.line" + String.valueOf(i)));
		}
	}
	
	@Override
	public boolean onUsed(WrapperWorld world, WrapperPlayer player){
		if(!world.isClient() && player.isOP()){
			for(AEntityA_Base entity : AEntityA_Base.getEntities(world)){
				if(entity instanceof EntityVehicleF_Physics){
					EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) entity;
					vehicle.throttle = 0;
					InterfacePacket.sendToAllClients(new PacketVehicleControlAnalog(vehicle, PacketVehicleControlAnalog.Controls.THROTTLE, (short) 0, (byte) 0));
					vehicle.parkingBrakeOn = true;
					InterfacePacket.sendToAllClients(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.P_BRAKE, true));
					for(PartEngine engine : vehicle.engines.values()){
						engine.setMagnetoStatus(false);
						InterfacePacket.sendToAllClients(new PacketPartEngine(engine, Signal.MAGNETO_OFF));
					}
					Iterator<String> variableIterator = vehicle.variablesOn.iterator();
					while(variableIterator.hasNext()){
						String variableName = variableIterator.next();
						for(LightType light : LightType.values()){
							if(light.lowercaseName.equals(variableName)){
								InterfacePacket.sendToAllClients(new PacketEntityVariableToggle(vehicle, variableName));
								variableIterator.remove();
								break;
							}
						}
					}
				}
			}
		}
        return true;
    }
}
