package minecrafttransportsimulator.systems.summoning;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.instances.PacketVehiclePhoneAction.PhoneAction;
import minecrafttransportsimulator.systems.LanguageSystem;
import minecrafttransportsimulator.systems.summoning.VehiclePhone.PhoneEntry;

/**
 * Runs vehicle phone calls on the server.  One job per vehicle; calling a vehicle that's already on its
 * way restarts its call.
 */
public final class VehicleSummonManager {
    private static final Map<UUID, VehicleSummonJob> jobs = new ConcurrentHashMap<>();

    private VehicleSummonManager() {
    }

    /**Handles a button press from the phone GUI.  Server only.**/
    public static void handleAction(AWrapperWorld world, IWrapperPlayer player, PhoneAction action, UUID vehicleID) {
        //Only act on vehicles registered on the phone the player is actually holding.
        if (!VehiclePhone.isPhone(player.getHeldItem())) {
            return;
        }
        IWrapperItemStack stack = player.getHeldStack();
        List<PhoneEntry> entries = VehiclePhone.getEntries(stack);
        PhoneEntry entry = entries.stream().filter(e -> e.vehicleID.equals(vehicleID)).findFirst().orElse(null);
        if (entry == null) {
            return;
        }

        VehicleSummonJob existing = jobs.remove(vehicleID);
        if (existing != null) {
            existing.cancel();
        }
        switch (action) {
            case SUMMON: {
                jobs.put(vehicleID, new VehicleSummonJob(world, player, vehicleID, entry.name));
                break;
            }
            case STOP: {
                VehiclePhone.message(player, existing != null ? LanguageSystem.INTERACT_PHONE_CANCELLED : LanguageSystem.INTERACT_PHONE_NOTCALLED, entry.name);
                break;
            }
            case REMOVE: {
                entries.remove(entry);
                VehiclePhone.setEntries(stack, entries);
                VehiclePhone.message(player, LanguageSystem.INTERACT_PHONE_REMOVED, entry.name);
                break;
            }
        }
    }

    /**Called at the start of every server tick for each world.**/
    public static void tickWorld(AWrapperWorld world) {
        Iterator<VehicleSummonJob> iterator = jobs.values().iterator();
        while (iterator.hasNext()) {
            VehicleSummonJob job = iterator.next();
            if (job.world == world && job.tick()) {
                iterator.remove();
            }
        }
        VehicleLocationTracker.tick(world);
    }

    /**Called every vehicle tick, on both sides, just before the vehicle is moved.**/
    public static void onVehicleTick(EntityVehicleF_Physics vehicle) {
        if (!vehicle.world.isClient()) {
            VehicleLocationTracker.onVehicleTick(vehicle);
            if (vehicle.ticksExisted % 20 == 0 && vehicle.getOrCreateVariable(VehicleSummonFlight.VAR_ACTIVE).isActive && !jobs.containsKey(vehicle.uniqueUUID)) {
                //Flight left on from a call that no longer exists, like one cut off by a restart.
                VehicleSummonFlight.stop(vehicle);
            }
        }
        VehicleSummonFlight.apply(vehicle);
    }

    /**Called when a server world unloads.  Leaves any vehicles on their way safe, and saves where vehicles are.**/
    public static void onWorldUnload(AWrapperWorld world) {
        Iterator<VehicleSummonJob> iterator = jobs.values().iterator();
        while (iterator.hasNext()) {
            VehicleSummonJob job = iterator.next();
            if (job.world == world) {
                job.cancel();
                iterator.remove();
            }
        }
        VehicleLocationTracker.onWorldUnload(world);
    }
}
