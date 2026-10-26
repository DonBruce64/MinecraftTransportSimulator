package minecrafttransportsimulator.items.instances;

import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.components.IItemEntityProvider;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.instances.PacketVehicleDeployment;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ControlSystem;

public class ItemVehicle extends AItemSubTyped<JSONVehicle> implements IItemEntityProvider {

    public ItemVehicle(JSONVehicle definition, JSONSubDefinition subDefinition, String sourcePackID) {
        super(definition, subDefinition, sourcePackID);
    }

    @Override
    public boolean onBlockClicked(AWrapperWorld world, IWrapperPlayer player, Point3D position, Axis axis) {
        if (definition.motorized.deployTime > 0) {
            if (world.isClient()) {
                ControlSystem.startVehicleDeployment(player, this, position, axis);
            } else {
                PacketVehicleDeployment.grantVehicleDeployment(player, this, position, axis);
            }
            return true;
        }

        deployVehicle(world, player, position);
        return true;
    }

    /**Spawns the vehicle after the server has validated the required hold duration.*/
    public void deployVehicle(AWrapperWorld world, IWrapperPlayer player, Point3D position) {
        if (world.isClient()) {
            return;
        }
        IWrapperItemStack heldStack = player.getHeldStack();
        if (heldStack.isEmpty() || heldStack.getItem() != this) {
            return;
        }
        IWrapperItemStack deploymentStack = heldStack.copy().split(1);
        if (!player.isCreative() && !player.getInventory().removeFromSlot(player.getHotbarIndex(), 1)) {
            return;
        }
        IWrapperNBT data = deploymentStack.getData();
        EntityVehicleF_Physics vehicle = new EntityVehicleF_Physics(world, player, this, data);
        vehicle.position.set(position).add(0.5, 1, 0.5);
        vehicle.prevPosition.set(position);
        vehicle.orientation.setToAngles(new Point3D(0, player.getYaw() + 90, 0));
        vehicle.prevOrientation.set(vehicle.orientation);
        vehicle.motion.set(0, 0, 0);
        vehicle.prevMotion.set(vehicle.motion);
        if (!player.isCreative()) {
            vehicle.setPlacementStack(deploymentStack);
        }
        vehicle.world.spawnEntity(vehicle);
        try {
            vehicle.addPartsPostAddition(player, data);
        } catch (RuntimeException | Error e) {
            vehicle.remove();
            throw e;
        }
    }

    @Override
    public void repair(IWrapperNBT data) {
        super.repair(data);
        data.setDouble("electricPower", 12);
    }

    @Override
    public void populateRepairMaterials(List<String> materials, int recipeIndex) {
        super.populateRepairMaterials(materials, recipeIndex);
        if (!ConfigSystem.settings.general.defaultVehicleRepairMaterials.value.isEmpty()) {
            materials.addAll(ConfigSystem.settings.general.defaultVehicleRepairMaterials.value);
        }
    }

    @Override
    public void registerEntities(Map<String, IItemEntityFactory> entityMap) {
        entityMap.put(EntityVehicleF_Physics.class.getSimpleName(), (world, data) -> new EntityVehicleF_Physics(world, null, data.getPackItem(), data));
    }
}
