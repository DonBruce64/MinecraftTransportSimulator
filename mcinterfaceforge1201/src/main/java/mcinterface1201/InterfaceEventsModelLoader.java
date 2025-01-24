package mcinterface1201;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;

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
        public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
            //First check if we should even process this resource.
            if ((domains.contains(location.getNamespace()) || domains.contains(getPackID(location.getPath()))) && location.getPath().endsWith(".png")) {
                //Create stream return variable and get raw data.
                InputStream stream;
                String domain = !location.getNamespace().equals(InterfaceManager.coreModID) ? location.getNamespace() : getPackID(location.getPath());
                String rawPackInfo = location.getPath();
                String streamLocation = "/assets/" + domain + "/" + rawPackInfo;
                stream = InterfaceManager.coreInterface.getPackResource(streamLocation);
                if (stream == null && !streamLocation.contains("/assets/mts/textures/mcfont")) {
                    if (ConfigSystem.settings.general.devMode.value) {
                        InterfaceManager.coreInterface.logError("Couldn't find requested PNG: " + streamLocation);
                    }
                }
                //Return whichever stream we found.
                final InputStream streamForSupplier = stream;
                return () -> streamForSupplier;
            } else {
                return null;
            }
        }

        @Override
        public Set<String> getNamespaces(PackType pType) {
            return fakeDomains;
        }

        @Override
        public <T> T getMetadataSection(MetadataSectionSerializer<T> pDeserializer) {
            return null;
        }

        @Override
        public String packId() {
            return InterfaceManager.coreModID + "_packs";
        }

        @Override
        public void close() {
        }

        @Override
        public IoSupplier<InputStream> getRootResource(String... pElements) {
            String pFileName = String.join("/", pElements);
            if (!pFileName.contains("/") && !pFileName.contains("\\")) {
                return this.getResource(PackType.CLIENT_RESOURCES, new ResourceLocation(pFileName));
            } else {
                throw new IllegalArgumentException("Root resources can only be filenames, not paths (no / allowed!)");
            }
        }

        @Override
        public void listResources(PackType pType, String pNamespace, String pPath, PackResources.ResourceOutput pResourceOutput) {
            //Don't list resources.  Ours are on-demand and we don't handle the cached items/models.
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
            return InterfaceManager.coreModID;
        }
    }
}
