package minecrafttransportsimulator.systems.summoning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    /**
     * Most time all calls together may spend searching for spots and routes each server tick, in
     * nanoseconds.  However many players are calling vehicles, this is all the time they get.
     */
    private static final long SEARCH_TIME_PER_TICK = 5_000_000;
    private static final Map<UUID, VehicleSummonJob> jobs = new ConcurrentHashMap<>();
    /**Worlds ticked so far this server tick, and the search time left in it.**/
    private static final Set<AWrapperWorld> worldsTicked = new HashSet<>();
    private static long searchTimeLeft = SEARCH_TIME_PER_TICK;
    /**Which call goes first each tick, rotating so none of them always gets what's left over.**/
    private static int firstJob;

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
        //Every world ticks once a server tick, so seeing one again means a new server tick has started.
        if (!worldsTicked.add(world)) {
            worldsTicked.clear();
            worldsTicked.add(world);
            searchTimeLeft = SEARCH_TIME_PER_TICK;
            ++firstJob;
        }

        //Share the search time left evenly between this world's calls.  Time one doesn't use goes to the rest.
        List<VehicleSummonJob> worldJobs = new ArrayList<>();
        for (VehicleSummonJob job : jobs.values()) {
            if (job.world == world) {
                worldJobs.add(job);
            }
        }
        for (int i = 0; i < worldJobs.size(); ++i) {
            VehicleSummonJob job = worldJobs.get((firstJob + i) % worldJobs.size());
            long startTime = System.nanoTime();
            boolean done = job.tick(Math.max(0, searchTimeLeft / (worldJobs.size() - i)));
            searchTimeLeft -= System.nanoTime() - startTime;
            if (done) {
                jobs.remove(job.vehicleID, job);
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
        worldsTicked.remove(world);
    }
}
