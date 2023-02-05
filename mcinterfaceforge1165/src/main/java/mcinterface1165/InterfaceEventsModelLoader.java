package mcinterface1165;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.packloading.PackResourceLoader;
import minecrafttransportsimulator.packloading.PackResourceLoader.ResourceType;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IFutureReloadListener;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.IResourcePack;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.resources.SimpleReloadableResourceManager;
import net.minecraft.resources.data.IMetadataSectionSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;

/**
 * Interface for handling events pertaining to loading models into MC.  These events are mainly for item models,
 * though events for Entity and Tile Entity model rendering classes are also included here as they are registered
 * like item models.
 *
 * @author don_bruce
 */
public class InterfaceEventsModelLoader {

    public static void init(ModelRegistryEvent event) {
        //Get the list of default resource packs here to inject a custom parser for auto-generating JSONS.
        //FAR easier than trying to use the bloody bakery system.
        //FIXME this probably wor't work for the packs, we test.
        try {
            //Now that we've created all the pack loaders, reload the resource manager to add them to the systems.
            ((SimpleReloadableResourceManager) Minecraft.getInstance().getResourceManager()).add(new PackResourcePack(InterfaceLoader.MODID));
        } catch (Exception e) {
            InterfaceManager.coreInterface.logError("Could not get default pack list. Item icons will be disabled.");
            e.printStackTrace();
        }
    }

    /**
     * Custom ResourcePack class for auto-generating item JSONs.
     */
    private static class PackResourcePack implements IResourcePack, IFutureReloadListener {
        private final String domain;
        private final Set<String> domains;

        private PackResourcePack(String domain) {
            super();
            this.domain = domain;
            domains = new HashSet<>();
            domains.add(domain);
        }

        @Override
        public InputStream getResource(ResourcePackType type, ResourceLocation location) throws IOException {
            System.out.println(location);

            //Create stream return variable and get raw data.
            InputStream stream;
            String rawPackInfo = location.getPath();

            //If we are for an item JSON, try to find that JSON, or generate one automatically.
            //If we are for an item PNG, just load the PNG as-is.  If we don't find it, then just let MC purple checker it.
            //Note that the internal mts_packs loader does not do PNG loading, as it re-directs the PNG files to the pack's loaders.
            if (rawPackInfo.endsWith(".json")) {
                //Strip the suffix from the packInfo, and then test to see if it's an internal
                //JSON reference from an item JSON, or if it's the primary JSON for the item being loaded..
                String strippedSuffix = rawPackInfo.substring(0, rawPackInfo.lastIndexOf("."));
                if (!strippedSuffix.contains(".")) {
                    //JSON reference.  Get the specified file.
                    stream = getClass().getResourceAsStream("/assets/" + domain + "/" + rawPackInfo);
                    if (stream == null) {
                        if (ConfigSystem.settings.general.devMode.value) {
                            InterfaceManager.coreInterface.logError("Could not find JSON-specified file: " + rawPackInfo);
                        }
                        throw new FileNotFoundException(rawPackInfo);
                    }
                } else {
                    String resourcePath = "";
                    String itemTexturePath = "";

                    //Strip off the auto-generated prefix.
                    String combinedPackInfo;
                    combinedPackInfo = strippedSuffix.substring("models/item/".length());

                    //Get the pack information, and try to load the resource.
                    try {
                        String packID = combinedPackInfo.substring(0, combinedPackInfo.indexOf("."));
                        String systemName = combinedPackInfo.substring(combinedPackInfo.indexOf(".") + 1);
                        AItemPack<?> packItem = PackParser.getItem(packID, systemName);
                        resourcePath = PackResourceLoader.getPackResource(packItem.definition, ResourceType.ITEM_JSON, systemName);

                        //Try to load the item JSON, or create it if it doesn't exist.
                        stream = getClass().getResourceAsStream(resourcePath);
                        if (stream == null) {
                            //Get the actual texture path.
                            itemTexturePath = PackResourceLoader.getPackResource(packItem.definition, ResourceType.ITEM_PNG, systemName);

                            //Remove the "/assets/packID/" portion as it's implied with JSON.
                            itemTexturePath = itemTexturePath.substring(("/assets/" + packID + "/").length());

                            //Remove the .png suffix as it's also implied.
                            itemTexturePath = itemTexturePath.substring(0, itemTexturePath.length() - ".png".length());

                            //Need to add packID domain to this to comply with JSON domains.
                            //If we don't, the PNG won't get sent to the right loader.
                            itemTexturePath = packID + ":" + itemTexturePath;

                            //Generate fake JSON and return as stream to MC loader.
                            String fakeJSON = "{\"parent\":\"mts:item/basic\",\"textures\":{\"layer0\": \"" + itemTexturePath + "\"}}";
                            stream = new ByteArrayInputStream(fakeJSON.getBytes(StandardCharsets.UTF_8));
                        }
                    } catch (Exception e) {
                        if (ConfigSystem.settings.general.devMode.value) {
                            InterfaceManager.coreInterface.logError("Could not parse out item JSON from: " + rawPackInfo + "  Looked for JSON at:" + resourcePath + (itemTexturePath.isEmpty() ? (", with fallback at:" + itemTexturePath) : ", but could not find it."));
                        }
                        throw new FileNotFoundException(rawPackInfo);
                    }
                }
            } else {
                try {
                    //First check if this is for an item or a model.
                    boolean isItemPNG = rawPackInfo.contains("/items/") || rawPackInfo.contains("_item");

                    //Strip off the auto-generated prefix and suffix data.
                    String combinedPackInfo = rawPackInfo;
                    combinedPackInfo = combinedPackInfo.substring("textures/".length(), combinedPackInfo.length() - ".png".length());

                    //Get the pack information.
                    //If we are ending in _item, it means we are getting a JSON for a modular-pack's item PNG.
                    //Need to remove this suffix to get the correct systemName to look-up in the systems.
                    String packID = domain;
                    String systemName = combinedPackInfo.substring(combinedPackInfo.lastIndexOf('/') + 1);
                    if (systemName.endsWith("_item")) {
                        systemName = systemName.substring(0, systemName.length() - "_item".length());
                    }
                    AItemPack<?> packItem = PackParser.getItem(packID, systemName);

                    if (packItem != null) {
                        //Get the actual resource path for this resource and return its stream.
                        String streamLocation = PackResourceLoader.getPackResource(packItem.definition, isItemPNG ? ResourceType.ITEM_PNG : ResourceType.PNG, systemName);
                        stream = getClass().getResourceAsStream(streamLocation);

                        if (stream == null) {
                            if (isItemPNG) {
                                //We might not have this file, but we also might have a JSON-defined item here.
                                //Try the JSON standards before throwing an error.
                                String streamJSONLocation = "/assets/" + packID + "/" + rawPackInfo;
                                stream = getClass().getResourceAsStream(streamJSONLocation);
                                if (stream == null) {
                                    if (ConfigSystem.settings.general.devMode.value) {
                                        if (streamLocation != null) {
                                            InterfaceManager.coreInterface.logError("Could not find item PNG at specified location: " + streamLocation + "  Or potential JSON location: " + streamJSONLocation);
                                        } else {
                                            InterfaceManager.coreInterface.logError("Could not find JSON PNG: " + streamJSONLocation);
                                        }
                                    }
                                    throw new FileNotFoundException(rawPackInfo);
                                }
                            } else {
                                if (ConfigSystem.settings.general.devMode.value) {
                                    InterfaceManager.coreInterface.logError("Could not find OBJ PNG: " + streamLocation);
                                }
                                throw new FileNotFoundException(rawPackInfo);
                            }
                        }
                    } else {
                        //No pack item for this texture.  Must be an internal texture for other things.
                        //In this case, we just get the stream exact location.
                        String streamLocation = "/assets/" + domain + "/" + rawPackInfo;
                        stream = getClass().getResourceAsStream(streamLocation);
                        if (stream == null) {
                            if (ConfigSystem.settings.general.devMode.value) {
                                InterfaceManager.coreInterface.logError("Couldn't find...whatever this is: " + streamLocation);
                            }
                            throw new FileNotFoundException(rawPackInfo);
                        }
                    }
                } catch (Exception e) {
                    if (e instanceof FileNotFoundException) {
                        throw e;
                    } else {
                        if (ConfigSystem.settings.general.devMode.value) {
                            InterfaceManager.coreInterface.logError("Could not parse which item PNG to get from: " + rawPackInfo);
                        }
                        throw new FileNotFoundException(rawPackInfo);
                    }
                }

            }

            //Return whichever stream we found.
            return stream;
        }

        @Override
        public boolean hasResource(ResourcePackType type, ResourceLocation location) {
            return domains.contains(location.getNamespace()) && !location.getPath().contains("blockstates") && !location.getPath().contains("armatures") && !location.getPath().contains("mcmeta") && ((location.getPath().endsWith(".json") && !location.getPath().equals("sounds.json")) || location.getPath().endsWith(".png"));
        }

        @Override
        public Set<String> getNamespaces(ResourcePackType pType) {
            return domains;
        }

        @Override
        public <T> T getMetadataSection(IMetadataSectionSerializer<T> pDeserializer) {
            return null;
        }

        @Override
        public String getName() {
            return domain + "_packs";
        }

        @Override
        public void close() {
        }

        @Override
        public InputStream getRootResource(String pFileName) throws IOException {
            if (!pFileName.contains("/") && !pFileName.contains("\\")) {
                return this.getResource(ResourcePackType.CLIENT_RESOURCES, new ResourceLocation(domain, pFileName));
            } else {
                throw new IllegalArgumentException("Root resources can only be filenames, not paths (no / allowed!)");
            }
        }

        @Override
        public Collection<ResourceLocation> getResources(ResourcePackType pType, String pNamespace, String pPath, int pMaxDepth, Predicate<String> pFilter) {
            //We shouldn't never need this, our resources are on-demand.
            return new ArrayList<ResourceLocation>();
        }

        @Override
        public CompletableFuture<Void> reload(IStage pStage, IResourceManager pResourceManager, IProfiler pPreparationsProfiler, IProfiler pReloadProfiler, Executor pBackgroundExecutor, Executor pGameExecutor) {
            //Re-add us to the main resource pack list, otherwise we go poof!
            //FIXME this might not play nice with the future-tasks....
            ((SimpleReloadableResourceManager) Minecraft.getInstance().getResourceManager()).add(this);
            return CompletableFuture.completedFuture(null);
        }
    }
}
