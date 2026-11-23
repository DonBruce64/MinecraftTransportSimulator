package mcinterface1211;

import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class FlashbackCompatUI {

    private static final String VEHICLE_LIST_ID = "iv_vehicle_list";

    private static Object vehicleListOpen;
    private static Object vehicleSearchField;

    // Vehicle search
    private static String lastSearch = "";
    private static long lastSearchUpdate = 0;

    private static List<EntityVehicleF_Physics> searchedVehicles = new ArrayList<>();
    private static Map<UUID, List<String>> riders = new HashMap<>();

    private static void saveFlashbackConfig() {
        try {
            Object config = Class.forName("com.moulberry.flashback.Flashback")
                    .getMethod("getConfig")
                    .invoke(null);
            config.getClass().getMethod("delayedSaveToDefaultFolder").invoke(config);
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings("unchecked")
    private static Set<String> openedWindows() {
        try {
            Object config = Class.forName("com.moulberry.flashback.Flashback")
                    .getMethod("getConfig")
                    .invoke(null);
            Object internal = config.getClass().getField("internal").get(config);
            return (Set<String>) internal.getClass().getField("openedWindows").get(internal);
        } catch (Throwable e) {
            return null;
        }
    }

    public static void renderVisualsWorld() {
        if (FlashbackImGui.ready() && FlashbackImGui.checkbox(I18n.get("mts.flashback.visuals.world.render_iv"), FlashbackCompat.renderVehicles)) {
            FlashbackCompat.renderVehicles = !FlashbackCompat.renderVehicles;
        }
    }

    public static void renderMenuItemBeforePlayerList() {
        if (!FlashbackImGui.ready() || !FlashbackImGui.menuItem(I18n.get("mts.flashback.vehicle_list") + "##IVVehicleList")) {
            return;
        }
        Set<String> opened = openedWindows();
        if (opened != null) {
            if (!opened.remove(VEHICLE_LIST_ID)) {
                opened.add(VEHICLE_LIST_ID);
            }
            saveFlashbackConfig();
        }
    }

    public static void renderSelectedVehicleUI(Entity entity) {
        if (entity instanceof BuilderEntityExisting
                && ((BuilderEntityExisting) entity).entity instanceof EntityVehicleF_Physics
                && FlashbackImGui.ready()) {
            UUID vehicleUUID = ((BuilderEntityExisting) entity).entity.uniqueUUID;

            FlashbackImGui.separator();
            FlashbackImGui.text(I18n.get("mts.flashback.mod_name"));
            EntityVehicleF_Physics selectedVehicle = (EntityVehicleF_Physics) ((BuilderEntityExisting) entity).entity;
            FlashbackImGui.text("Model: " + vehicleTitle(selectedVehicle));
            FlashbackImGui.text("Definition: " + vehicleName(selectedVehicle));
            FlashbackImGui.text("Vehicle UUID: " + vehicleUUID.toString());
            if (FlashbackImGui.button(I18n.get("mts.flashback.copy_uuid_vehicle"))) {
                Minecraft.getInstance().keyboardHandler.setClipboard(vehicleUUID.toString());
            }
            boolean isMuted = FlashbackCompat.MUTED.contains(vehicleUUID);
            if (FlashbackImGui.button(I18n.get(isMuted ? "mts.flashback.unmute_vehicle" : "mts.flashback.mute_vehicle"))) {
                if (isMuted) {
                    FlashbackCompat.MUTED.remove(vehicleUUID);
                } else {
                    FlashbackCompat.MUTED.add(vehicleUUID);
                }
            }
        }
    }

    private static String vehicleName(EntityVehicleF_Physics vehicle) {
        String name = vehicle.definition.packID + ":" + vehicle.definition.systemName;
        String sub = vehicle.subDefinition != null ? vehicle.subDefinition.subName : null;
        return sub != null ? name + " [" + sub + "]" : name;
    }

    private static String vehicleTitle(EntityVehicleF_Physics vehicle) {
        return vehicle.subDefinition.name;
    }

    private static Map<UUID, List<String>> collectRiders(Minecraft mc) {
        Map<UUID, List<String>> riders = new HashMap<>();
        if (mc.level == null) {
            return riders;
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof BuilderEntityLinkedSeat) {
                Object seatPart = ((BuilderEntityLinkedSeat) entity).entity;
                if (seatPart instanceof APart && ((APart) seatPart).vehicleOn != null) {
                    for (Entity passanger : entity.getPassengers()) {
                        riders.computeIfAbsent(
                                ((APart) seatPart).vehicleOn.uniqueUUID, uuid -> new ArrayList<>())
                                .add(passanger.getDisplayName().getString());
                    }
                }
            }
        }
        return riders;
    }

    private static void updateVehicleList() {
        searchedVehicles.clear();

        Minecraft mc = Minecraft.getInstance();
        final Vec3 cam = mc.cameraEntity.position();

        AWrapperWorld world = InterfaceManager.clientInterface.getClientWorld();
        riders = collectRiders(Minecraft.getInstance());
        for (EntityVehicleF_Physics vehicle : world.getEntitiesOfType(EntityVehicleF_Physics.class)) {
            if (!vehicle.isValid) {
                return;
            }
            if (!lastSearch.isEmpty()) {
                UUID builderUUID = FlashbackCompat.BUILDERS_UUID.get(vehicle.uniqueUUID);
                List<String> ridersInVehicle = riders.get(vehicle.uniqueUUID);
                String searchString = (
                        vehicleTitle(vehicle)
                        + " "
                        + vehicleName(vehicle)
                        + " "
                        + vehicle.uniqueUUID
                        + " "
                        + (builderUUID != null ? builderUUID.toString() : "")
                        + " "
                        + (ridersInVehicle != null ? String.join(" ", ridersInVehicle) : "")
                ).toLowerCase(Locale.ROOT);
                if (!searchString.contains(lastSearch)) {
                    continue;
                }
            }
            searchedVehicles.add(vehicle);
        }

        searchedVehicles.sort(Comparator.comparingDouble(entity ->
                cam.distanceToSqr(entity.position.x, entity.position.y, entity.position.z)));
    }

    public static void renderVehicleList() {
        Set<String> openedWindows = openedWindows();
        if (!openedWindows.contains(VEHICLE_LIST_ID) || !FlashbackCompat.isReplayClient() || !FlashbackImGui.ready()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        final Vec3 cam = mc.cameraEntity.position();

        if (vehicleListOpen == null) {
            vehicleListOpen = FlashbackImGui.newImBoolean(true);
            vehicleSearchField = FlashbackImGui.newImString();
        }
        FlashbackImGui.imBooleanSet(vehicleListOpen, true);

        FlashbackImGui.setNextWindowSizeConstraints(650, 420, 5000, 5000);

        if (FlashbackImGui.begin(I18n.get("mts.flashback.vehicle_list") + "###IVVehicleList", vehicleListOpen)) {
            FlashbackImGui.inputTextWithHint("###ivvehiclesearch", I18n.get("mts.flashback.search_name_or_uuid"), vehicleSearchField);

            long currentTime = System.currentTimeMillis();
            if (lastSearch  == null || currentTime > lastSearchUpdate + 3000 || currentTime < lastSearchUpdate) {
                lastSearch = FlashbackImGui.imStringGet(vehicleSearchField).trim().toLowerCase(Locale.ROOT);
                lastSearchUpdate = currentTime;
                updateVehicleList();
            }


            FlashbackImGui.separator();

            if (searchedVehicles.isEmpty()) {
                FlashbackImGui.text(lastSearch.isEmpty() ? I18n.get("mts.flashback.no_vehicles_loaded") : I18n.get("mts.flashback.no_vehicles_match"));
            } else {
                int shown = 0;
                for (int i = 0; i < searchedVehicles.size(); i++) {
                    if (++shown > 200) {
                        break;
                    }

                    FlashbackImGui.pushID(i);
                    EntityVehicleF_Physics vehicle = searchedVehicles.get(i);

                    UUID builderUUID = FlashbackCompat.BUILDERS_UUID.get(vehicle.uniqueUUID);
                    int distance = (int) Math.sqrt(cam.distanceToSqr(vehicle.position.x, vehicle.position.y, vehicle.position.z));
                    FlashbackImGui.text(vehicleTitle(vehicle) + " (" + vehicleName(vehicle) + ") " + " (" + distance + "m)");
                    FlashbackImGui.text("V: " + vehicle.uniqueUUID + (builderUUID != null ? " [E: " + builderUUID + "]" : ""));
                    List<String> ridersInVehicle = riders.get(vehicle.uniqueUUID);
                    if (ridersInVehicle != null) {
                        FlashbackImGui.text(I18n.get("mts.flashback.passengers") + ": " + String.join(", ", ridersInVehicle));
                    }

                    if (FlashbackImGui.button(I18n.get("mts.flashback.look_at"))) {
                        mc.cameraEntity.lookAt(
                                EntityAnchorArgument.Anchor.EYES,
                                new Vec3(vehicle.position.x, vehicle.position.y, vehicle.position.z)
                        );
                    }
                    FlashbackImGui.sameLine();
                    if (FlashbackImGui.button(I18n.get("flashback.tp"))) {
                        mc.getConnection().sendCommand("teleport " + builderUUID);
                        lastSearchUpdate = currentTime;
                    }
                    FlashbackImGui.sameLine();
                    if (FlashbackCompat.isVehicleMarkedHidden(vehicle)) {
                        if (FlashbackImGui.button(I18n.get("flashback.show"))) {
                            FlashbackCompat.markHidden(vehicle.uniqueUUID, false);
                            lastSearchUpdate = currentTime;
                        }
                    } else if (FlashbackImGui.button(I18n.get("flashback.hide"))) {
                        FlashbackCompat.markHidden(vehicle.uniqueUUID, true);
                        lastSearchUpdate = currentTime;
                    }
                    FlashbackImGui.sameLine();
                    if (FlashbackImGui.button(I18n.get("mts.flashback.copy_uuid_vehicle"))) {
                        mc.keyboardHandler.setClipboard(vehicle.uniqueUUID.toString());
                        lastSearchUpdate = currentTime;
                    }
                    FlashbackImGui.sameLine();
                    if (builderUUID != null && FlashbackImGui.button(I18n.get("mts.flashback.copy_uuid_entity"))) {
                        mc.keyboardHandler.setClipboard(builderUUID.toString());
                        lastSearchUpdate = currentTime;
                    }

                    FlashbackImGui.popID();
                }
            }

            FlashbackImGui.separator();
        }

        FlashbackImGui.end();
        if (!FlashbackImGui.imBooleanGet(vehicleListOpen)) {
            openedWindows.remove(VEHICLE_LIST_ID);
            saveFlashbackConfig();
        }
    }

}
