package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityFluidTankProvider;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityTickable;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFuelPumpConnection;

public class TileEntityFuelPump extends TileEntityDecor implements ITileEntityTickable, ITileEntityFluidTankProvider{
	public EntityVehicleF_Physics connectedVehicle;
    private final EntityFluidTank tank;
    public final EntityInventoryContainer fuelItems;
    public final List<Integer> fuelAmounts = new ArrayList<Integer>();
    public int fuelPurchasedRemaining;
    public boolean isCreative;
	public String placingPlayerID;

    public TileEntityFuelPump(WrapperWorld world, Point3d position, WrapperNBT data){
    	super(world, position, data);
    	this.tank = new EntityFluidTank(world, data.getDataOrNew("tank"), 15000){
    		@Override
    		public double fill(String fluid, double maxAmount, boolean doFill){
    			if(!isCreative){
					//We are a pump with a set cost, ensure we have purchased fuel.
    				//Make sure to add a small amount to ensure that the pump displays the name of the fluid in it.
    				if(fuelPurchasedRemaining == 0 && connectedVehicle == null && getFluidLevel() == 0){
    					maxAmount = 1;
					}else if(maxAmount > fuelPurchasedRemaining){
						maxAmount = fuelPurchasedRemaining;
					}
					double amountFilled = super.fill(fluid, maxAmount, doFill);
					if(doFill && fuelPurchasedRemaining > 0){
						fuelPurchasedRemaining -= amountFilled;
					}
					return amountFilled;
				}
    			return super.fill(fluid, maxAmount, doFill);
    		}
    	};
    	this.fuelItems = new EntityInventoryContainer(world, data.getDataOrNew("inventory"), 10);
    	for(int i=0; i<fuelItems.getSize(); ++i){
    		this.fuelAmounts.add(data.getInteger("fuelAmount" + i));
    	}
    	this.fuelPurchasedRemaining = data.getInteger("fuelPurchasedRemaining");
    	this.placingPlayerID = data.getString("placingPlayerID");
    }
	
	@Override
	public boolean update(){
		if(super.update()){
			//Update creative status.
			isCreative = true;
			for(int i=0; i<fuelItems.getSize(); ++i){
				if(!fuelItems.getStack(i).isEmpty()){
					isCreative = false;
				}
			}
			
			//Update text lines to the current tank status if required.
			//Only do this on clients, as servers don't render any text.
			if(world.isClient() && definition.rendering != null && definition.rendering.textObjects != null){
				text.clear();
				String fluidName = tank.getFluidLevel() > 0 ? InterfaceCore.getFluidName(tank.getFluid()).toUpperCase() : "";
				String fluidLevel = InterfaceCore.translate("tile.fuelpump.level") + String.format("%04.1f", tank.getFluidLevel()/1000F) + "b";
				String fluidDispensed = InterfaceCore.translate("tile.fuelpump.dispensed") + String.format("%04.1f", tank.getAmountDispensed()/1000F) + "b";
				for(int i=0; i<definition.rendering.textObjects.size(); ++i){
					switch(i%3){
						case(0) : text.put(definition.rendering.textObjects.get(i), fluidName); break;
						case(1) : text.put(definition.rendering.textObjects.get(i),fluidLevel); break;
						case(2) : text.put(definition.rendering.textObjects.get(i),fluidDispensed); break;
					}
				}
			}
			
			//Do fuel checks.  Fuel checks only occur on servers.  Clients get packets for state changes.
			if(connectedVehicle != null && !world.isClient()){
				//Don't fuel vehicles that don't exist.
				if(!connectedVehicle.isValid){
					connectedVehicle.beingFueled = false;
					connectedVehicle = null;
					return false;
				}
				
				//Check distance to make sure the vehicle hasn't moved away.
				if(connectedVehicle.position.distanceTo(position) > 16){
					InterfacePacket.sendToAllClients(new PacketTileEntityFuelPumpConnection(this, false));
					for(WrapperEntity entity : world.getEntitiesWithin(new BoundingBox(position, 25, 25, 25))){
						if(entity instanceof WrapperPlayer){
							((WrapperPlayer) entity).sendPacket(new PacketPlayerChatMessage((WrapperPlayer) entity, "interact.fuelpump.toofar"));
						}
					}
					connectedVehicle.beingFueled = false;
					connectedVehicle = null;
					return false;
				}
				//If we have room for fuel, try to add it to the vehicle.
				if(tank.getFluidLevel() > 0){
					double amountToFill = connectedVehicle.fuelTank.fill(tank.getFluid(), 10, false);
					if(amountToFill > 0){
						double amountToDrain = tank.drain(tank.getFluid(), amountToFill, false);
						connectedVehicle.fuelTank.fill(tank.getFluid(), amountToDrain, true);
						tank.drain(tank.getFluid(), amountToDrain, true);
					}else{
						//No more room in the vehicle.  Disconnect.
						InterfacePacket.sendToAllClients(new PacketTileEntityFuelPumpConnection(this, false));
						connectedVehicle.beingFueled = false;
						connectedVehicle = null;
						for(WrapperEntity entity : world.getEntitiesWithin(new BoundingBox(position, 16, 16, 16))){
							if(entity instanceof WrapperPlayer){
								((WrapperPlayer) entity).sendPacket(new PacketPlayerChatMessage((WrapperPlayer) entity, "interact.fuelpump.complete"));
							}
						}
					}
				}else{
					//No more fuel.  Disconnect vehicle.
					InterfacePacket.sendToAllClients(new PacketTileEntityFuelPumpConnection(this, false));
					connectedVehicle.beingFueled = false;
					connectedVehicle = null;
					for(WrapperEntity entity : world.getEntitiesWithin(new BoundingBox(position, 16, 16, 16))){
						if(entity instanceof WrapperPlayer){
							((WrapperPlayer) entity).sendPacket(new PacketPlayerChatMessage((WrapperPlayer) entity, "interact.fuelpump.empty"));
						}
					}
				}
			}
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public EntityFluidTank getTank(){
		return tank;
	}
	
	@Override
	public boolean canConnectOnAxis(Axis axis){
		return axis.equals(Axis.DOWN);
	}
	
	@Override
	public double getRawVariableValue(String variable, float partialTicks){
		switch(variable){
			case("fuelpump_active"): return connectedVehicle != null ? 1 : 0;	
			case("fuelpump_stored"): return getTank().getFluidLevel();
			case("fuelpump_dispensed"): return getTank().getAmountDispensed();
			case("fuelpump_free"): return isCreative ? 1 : 0;
			case("fuelpump_purchased"): return fuelPurchasedRemaining;
		}
		
		return super.getRawVariableValue(variable, partialTicks);
	}
	
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		data.setData("tank", tank.save(new WrapperNBT()));
		data.setData("inventory", fuelItems.save(new WrapperNBT()));
		for(int i=0; i<fuelItems.getSize(); ++i){
    		data.setInteger("fuelAmount" + i, fuelAmounts.get(i));
    	}
		data.setInteger("fuelPurchasedRemaining", fuelPurchasedRemaining);
		data.setString("placingPlayerID", placingPlayerID);
		return data;
	}
}
