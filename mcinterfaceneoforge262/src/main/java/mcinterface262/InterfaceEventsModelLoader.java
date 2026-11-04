package mcinterface262;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.network.chat.Component;

/**
 * Interface for handling events pertaining to loading models into MC.  This used to handle mainly item models, but
 * now it just re-directs texture calls for the main core mod to allow them to work in development with the referenced
 * core library files that MC doesn't see normally.
 *
 * @author don_bruce
 */
public class InterfaceEventsModelLoader {
    public static PackResourcePack packPack = new PackResourcePack();

    /**
     * True when running from classes rather than the built mod JAR.  In dev runs the core
     * assets live in a separate JAR that the game's resource manager can't see, so they
     * must be served by {@link PackResourcePack} instead.
     */
    public static final boolean runningDevIDE = isRunningDevIDE();

    private static boolean isRunningDevIDE() {
        try {
            return new java.io.File(InterfaceLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI()).isDirectory();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Called to init the custom model loader.  Should be done before any other things.
     * This allows injecting our custom resource manager into MC's systems to have it use it.
     * We do this by registering it as a reload listener, as on a resource reload (and boot) MC will purge the list
     * of packs and will re-query from disk.  But we aren't on disk, and so we will need to be
     * ready when that call comes and will re-add ourselves.
     */
    public static void init() {
        packPack.domains.addAll(PackParser.getAllPackIDs());
    }

    /**
     * Custom ResourcePack class for auto-generating item JSONs.
     */
    public static class PackResourcePack implements PackResources {
        private final Set<String> domains;
        private final Set<String> fakeDomains;

        private PackResourcePack() {
            super();
            domains = new HashSet<>();
            fakeDomains = new HashSet<>();
            fakeDomains.add(InterfaceLoader.MODID);
        }

        @Override
        public IoSupplier<InputStream> getResource(PackType type, Identifier location) {
            String path = location.getPath();
            //Pack items need a 26.2 item model definition (items/<name>.json) that references
            //the model the pack ships at mts:item/<name>.  Generate it on demand.
            if (type == PackType.CLIENT_RESOURCES && location.getNamespace().equals(InterfaceLoader.MODID) && path.startsWith("items/") && path.endsWith(".json")) {
                String itemName = path.substring("items/".length(), path.length() - ".json".length());
                if (isPackItem(itemName)) {
                    return () -> new ByteArrayInputStream(generateItemDefinition(itemName).getBytes(StandardCharsets.UTF_8));
                }
                return null;
            }
            String packID = getPackID(path);
            //Core resources (textures in the mccore jar) are only in the resource path in dev runs.
            //In production they are unpacked into the mod JAR and the normal mod resource pack finds them.
            boolean packResource = domains.contains(location.getNamespace()) || domains.contains(packID);
            boolean coreResource = runningDevIDE && (location.getNamespace().equals(InterfaceLoader.MODID) || packID.equals(InterfaceLoader.MODID));
            if ((packResource || coreResource) && path.endsWith(".png")) {
                //Create stream return variable and get raw data.
                String domain = !location.getNamespace().equals(InterfaceLoader.MODID) ? location.getNamespace() : packID;
                String rawPackInfo = path;
                String streamLocation = "/assets/" + domain + "/" + rawPackInfo;
                final InputStream stream = InterfaceManager.coreInterface.getPackResource(streamLocation);
                if (stream == null) {
                    if (!streamLocation.contains("/assets/mts/textures/mcfont") && ConfigSystem.settings.general.devMode.value) {
                        InterfaceManager.coreInterface.logError("Couldn't find requested PNG: " + streamLocation);
                    }
                    return null;
                }
                //Return whichever stream we found.
                return () -> stream;
            } else {
                return null;
            }
        }

        @Override
        public Set<String> getNamespaces(PackType pType) {
            return fakeDomains;
        }

        @Override
        public <T> T getMetadataSection(MetadataSectionType<T> pDeserializer) throws IOException {
            return null;
        }

        @Override
        public String packId() {
            return InterfaceLoader.MODID + "_packs";
        }

        @Override
        public PackLocationInfo location() {
            return new PackLocationInfo(packId(), Component.literal(packId()), PackSource.DEFAULT, java.util.Optional.empty());
        }

        @Override
        public void close() {
        }

        @Override
        public IoSupplier<InputStream> getRootResource(String... pElements) {
            String pFileName = String.join("/", pElements);
            if (!pFileName.contains("/") && !pFileName.contains("\\")) {
                return this.getResource(PackType.CLIENT_RESOURCES, Identifier.parse(pFileName));
            } else {
                throw new IllegalArgumentException("Root resources can only be filenames, not paths (no / allowed!)");
            }
        }

        @Override
        public void listResources(PackType pType, String pNamespace, String pPath, PackResources.ResourceOutput pResourceOutput) {
            //List the generated item model definitions for all pack items.  The models themselves
            //are shipped by the packs at mts:item/<name>.
            if (pType == PackType.CLIENT_RESOURCES && pNamespace.equals(InterfaceLoader.MODID) && (pPath.isEmpty() || pPath.equals("items"))) {
                for (AItemBase item : BuilderItem.itemMap.keySet()) {
                    if (item instanceof AItemPack && !((AItemPack<?>) item).definition.packID.equals(InterfaceLoader.MODID)) {
                        String itemName = item.getRegistrationName();
                        Identifier location = Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "items/" + itemName + ".json");
                        pResourceOutput.accept(location, () -> new ByteArrayInputStream(generateItemDefinition(itemName).getBytes(StandardCharsets.UTF_8)));
                    }
                }
            }
        }

        /**Returns true if the passed-in name is a registered item from an external pack.*/
        private static boolean isPackItem(String itemName) {
            for (AItemBase item : BuilderItem.itemMap.keySet()) {
                if (item instanceof AItemPack && !((AItemPack<?>) item).definition.packID.equals(InterfaceLoader.MODID) && item.getRegistrationName().equals(itemName)) {
                    return true;
                }
            }
            return false;
        }

        /**Generates the 26.2 item model definition for a pack item.*/
        private static String generateItemDefinition(String itemName) {
            return "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"" + InterfaceLoader.MODID + ":item/" + itemName + "\"}}";
        }

        private static String getPackID(String path) {
            int distanceToFirstDot = path.indexOf(".");
            int distanceToSlashBefore = path.lastIndexOf("/", distanceToFirstDot);
            if (distanceToSlashBefore != -1) {
                String packID = path.substring(distanceToSlashBefore + 1, distanceToFirstDot);
                if (PackParser.getAllPackIDs().contains(packID)) {
                    return packID;
                }
            }
            //Not an actual pack resource, must be from core.
            return InterfaceLoader.MODID;
        }
    }
}
