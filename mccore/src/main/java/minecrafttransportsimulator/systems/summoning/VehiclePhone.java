package minecrafttransportsimulator.systems.summoning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.jsondefs.AJSONInteractableEntity;
import minecrafttransportsimulator.jsondefs.AJSONInteractableEntity.JSONCustomKeybind;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.LanguageSystem;
import minecrafttransportsimulator.systems.LanguageSystem.LanguageEntry;

/**
 * Helpers for vehicle phone items.  Vehicles are registered on a phone by sneak-clicking them,
 * and are stored on the phone's stack, so whoever holds the phone can call them.
 */
public final class VehiclePhone {
    /**
     * Vehicle constant that makes an aircraft fly in when called, even if it doesn't expose
     * {@link #VAR_HOVER} and {@link #VAR_STAY_ON}.  Aircraft that can't fly in are teleported to the ground instead.
     **/
    public static final String CONSTANT_AIRCRAFT_ALLOWED = "phone_summon_aircraft";
    /**Variables an aircraft needs to fly in, hover over the player and land.**/
    static final String VAR_HOVER = "hover";
    static final String VAR_STAY_ON = "stay_on";
    static final String VAR_GEAR = "gear_setpoint";
    /**Vehicle variable for how far away it appears before driving in, in blocks.  0 uses the default.**/
    public static final String VAR_SUMMON_DISTANCE = "phone_summon_distance";
    static final int MAX_VEHICLES = 8;
    private static final String DATA_VEHICLES = "phoneVehicles";
    private static final String ENTRY_SEPARATOR = ";";

    private VehiclePhone() {
    }

    public static boolean isPhone(AItemBase item) {
        return item instanceof ItemItem && ((ItemItem) item).definition.item.type == ItemComponentType.PHONE;
    }

    /**Registers the vehicle on the phone the player is holding.  Server only.**/
    public static void registerVehicle(EntityVehicleF_Physics vehicle, IWrapperPlayer player) {
        if (!canControl(player, vehicle)) {
            message(player, LanguageSystem.INTERACT_PHONE_NEEDKEY);
            return;
        }
        IWrapperItemStack stack = player.getHeldStack();
        List<PhoneEntry> entries = getEntries(stack);
        String name = getName(vehicle);
        entries.removeIf(entry -> entry.vehicleID.equals(vehicle.uniqueUUID));
        if (entries.size() >= MAX_VEHICLES) {
            message(player, LanguageSystem.INTERACT_PHONE_FULL);
            return;
        }
        entries.add(new PhoneEntry(vehicle.uniqueUUID, name));
        setEntries(stack, entries);
        VehicleLocationTracker.update(vehicle);
        message(player, LanguageSystem.INTERACT_PHONE_REGISTERED, name);
    }

    /**
     * True if the player may register and call this vehicle: it has no key, or they're
     * carrying its key, or they're an operator.
     */
    static boolean canControl(IWrapperPlayer player, EntityVehicleF_Physics vehicle) {
        if (vehicle.keyUUID == null || player.isOP()) {
            return true;
        }
        for (int i = 0; i < player.getInventory().getSize(); ++i) {
            IWrapperNBT data = player.getInventory().getStack(i).getData();
            if (data != null && vehicle.keyUUID.equals(data.getUUID(ItemItem.KEY_UUID_TAG))) {
                return true;
            }
        }
        return false;
    }

    /**
     * True if this aircraft can fly itself over to the player: it has the
     * {@link #CONSTANT_AIRCRAFT_ALLOWED} constant, or it or one of its parts exposes both
     * {@link #VAR_HOVER} and {@link #VAR_STAY_ON} as custom variables or keybinds.
     */
    static boolean canFlyIn(EntityVehicleF_Physics vehicle) {
        if (vehicle.getOrCreateVariable(CONSTANT_AIRCRAFT_ALLOWED).isActive) {
            return true;
        }
        Set<String> variables = new HashSet<>();
        addControlVariables(vehicle.definition, variables);
        for (APart part : vehicle.allParts) {
            addControlVariables(part.definition, variables);
        }
        return variables.contains(VAR_HOVER) && variables.contains(VAR_STAY_ON);
    }

    private static void addControlVariables(AJSONInteractableEntity definition, Set<String> variables) {
        if (definition.rendering != null && definition.rendering.customVariables != null) {
            variables.addAll(definition.rendering.customVariables);
        }
        if (definition.customKeybinds != null) {
            for (JSONCustomKeybind keybind : definition.customKeybinds) {
                if (keybind.action != null && keybind.action.variable != null) {
                    variables.add(keybind.action.variable);
                }
            }
        }
    }

    static String getName(EntityVehicleF_Physics vehicle) {
        return vehicle.subDefinition.name != null ? vehicle.subDefinition.name : vehicle.definition.general.name;
    }

    public static List<PhoneEntry> getEntries(IWrapperItemStack stack) {
        List<PhoneEntry> entries = new ArrayList<>();
        IWrapperNBT data = stack.getData();
        if (data != null) {
            for (String value : data.getStrings(DATA_VEHICLES)) {
                int split = value.indexOf(ENTRY_SEPARATOR);
                if (split > 0) {
                    try {
                        entries.add(new PhoneEntry(UUID.fromString(value.substring(0, split)), value.substring(split + 1)));
                    } catch (IllegalArgumentException e) {
                        //Corrupt entry, drop it.
                    }
                }
            }
        }
        return entries;
    }

    static void setEntries(IWrapperItemStack stack, List<PhoneEntry> entries) {
        IWrapperNBT data = stack.getData();
        if (data == null) {
            data = InterfaceManager.coreInterface.getNewNBTWrapper();
        }
        List<String> values = new ArrayList<>();
        entries.forEach(entry -> values.add(entry.vehicleID + ENTRY_SEPARATOR + entry.name));
        data.setStrings(DATA_VEHICLES, values);
        stack.setData(data);
    }

    static void message(IWrapperPlayer player, LanguageEntry language, Object... args) {
        if (player != null) {
            player.sendPacket(new PacketPlayerChatMessage(player, language, args));
        }
    }

    /**A vehicle registered on a phone.**/
    public static final class PhoneEntry {
        public final UUID vehicleID;
        public final String name;

        PhoneEntry(UUID vehicleID, String name) {
            this.vehicleID = vehicleID;
            this.name = name;
        }
    }
}
