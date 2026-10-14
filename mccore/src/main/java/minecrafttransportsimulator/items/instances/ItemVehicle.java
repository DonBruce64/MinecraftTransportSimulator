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

    /**
     * Spawns this vehicle at the requested block position.  Timed deployment packets call this
     * only after the server has validated the full hold duration.
     */
    public void deployVehicle(AWrapperWorld world, IWrapperPlayer player, Point3D position) {
        if (!world.isClient()) {
            IWrapperItemStack deploymentStack = player.getHeldStack().copy().split(1);
            deployVehicle(world, player, position, deploymentStack, player.getYaw(), true, !player.isCreative());
        }
    }

    /**
     * Spawns this vehicle from the supplied stack and yaw.  Unlike the legacy overload, this
     * method never needs to read the player's held stack when {@code consumeHeldStack} is false.
     * Callers that reserve a stack before an autonomous deployment should pass false here.
     */
    public EntityVehicleF_Physics deployVehicle(AWrapperWorld world, IWrapperPlayer player, Point3D position, IWrapperItemStack deploymentStack, double placementYaw, boolean consumeHeldStack) {
        return deployVehicle(world, player, position, deploymentStack, placementYaw, consumeHeldStack, player != null && !player.isCreative());
    }

    /**
     * Spawns this vehicle from an explicit placement context.  The refund flag is separate from
     * stack consumption so autonomous deployments can preserve it after the player disconnects.
     */
    public EntityVehicleF_Physics deployVehicle(AWrapperWorld world, IWrapperPlayer player, Point3D position, IWrapperItemStack deploymentStack, double placementYaw, boolean consumeHeldStack, boolean refundOnPlacementFailure) {
        if (world.isClient() || deploymentStack == null || deploymentStack.isEmpty() || deploymentStack.getItem() != this) {
            return null;
        }

        //Consume only for legacy/hold deployment.  Autonomous callers own their reserved stack.
        if (consumeHeldStack) {
            if (player == null) {
                return null;
            }
            if (!player.isCreative()) {
                IWrapperItemStack heldStack = player.getInventory().getStack(player.getHotbarIndex());
                if (!heldStack.isCompleteMatch(deploymentStack) || !player.getInventory().removeFromSlot(player.getHotbarIndex(), 1)) {
                    return null;
                }
            }
        }

        //First construct the class.
        //This takes into account all saved data in the stack, so the vehicle will re-load its data from it
        //as if it has been saved in the world rather than into an item.  If there's no data,
        //then we just make a blank, new instance.
        IWrapperNBT data = deploymentStack.getData();
        EntityVehicleF_Physics vehicle = new EntityVehicleF_Physics(world, player, this, data);

        //Set position to the spot that was clicked by the player.
        //Add a -90 rotation offset so the vehicle is facing perpendicular.
        //Remove motion to prevent it if it was previously stored.
        //Makes placement easier and is less likely for players to get stuck.
        //Then spawn the vehicle into the world once these properties are set.
        vehicle.position.set(position).add(0.5, 1, 0.5);
        vehicle.prevPosition.set(position);
        vehicle.orientation.setToAngles(new Point3D(0, placementYaw + 90, 0));
        vehicle.prevOrientation.set(vehicle.orientation);
        vehicle.motion.set(0, 0, 0);
        vehicle.prevMotion.set(vehicle.motion);

        vehicle.setPlacementContext(deploymentStack, refundOnPlacementFailure);
        vehicle.world.spawnEntity(vehicle);

        //Now add the parts.  These have to be done after spawning the vehicle so they have the right tick order and position.
        try {
            vehicle.addPartsPostAddition(player, data);
        } catch (RuntimeException | Error e) {
            vehicle.remove();
            throw e;
        }
        return vehicle;
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
