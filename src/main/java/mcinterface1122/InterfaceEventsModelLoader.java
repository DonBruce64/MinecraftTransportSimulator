package mcinterface1122;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.packloading.PackResourceLoader;
import minecrafttransportsimulator.packloading.PackResourceLoader.ResourceType;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.resource.VanillaResourceType;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Interface for handling events pertaining to loading models into MC.  These events are mainly for item models,
 * though events for Entity and Tile Entity model rendering classes are also included here as they are registered
 * like item models.
 *
 * @author don_bruce
 */
@EventBusSubscriber(Side.CLIENT)
public class InterfaceEventsModelLoader {

    /**
     * Event that's called to register models.  We register our render wrapper
     * classes here, as well as all item JSONs.
     */
    @SuppressWarnings({"unchecked"})
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        //Register the global entity rendering class.
        RenderingRegistry.registerEntityRenderingHandler(BuilderEntityRenderForwarder.class, manager -> new Render<BuilderEntityRenderForwarder>(manager) {
            @Override
            protected ResourceLocation getEntityTexture(BuilderEntityRenderForwarder builder) {
                return null;
            }

            @Override
            public boolean shouldRender(BuilderEntityRenderForwarder builder, ICamera camera, double camX, double camY, double camZ) {
                //Always render the forwarder, no matter where the camera is.
                return true;
            }

            @Override
            public void doRender(BuilderEntityRenderForwarder builder, double x, double y, double z, float entityYaw, float partialTicks) {
                //Get all entities in the world, and render them manually for this one builder.
                //Only do this if the player the builder is following is the client player.
                WrapperWorld world = WrapperWorld.getWrapperFor(builder.world);
                if (Minecraft.getMinecraft().player.equals(builder.playerFollowing) && builder.shouldRenderEntity(partialTicks)) {
                    ConcurrentLinkedQueue<AEntityC_Renderable> allEntities = world.renderableEntities;
                    if (allEntities != null) {
                        boolean blendingEnabled = MinecraftForgeClient.getRenderPass() == 1;

                        //Use smooth shading for model rendering.
                        GL11.glShadeModel(GL11.GL_SMOOTH);
                        //Disable alpha testing on blended pass as it discards transparent fragments.
                        if (blendingEnabled) {
                            GlStateManager.disableAlpha();
                        }
                        //Enable normal re-scaling for model rendering.
                        //This prevents bad lighting.
                        GlStateManager.enableRescaleNormal();

                        //Start master profiling section.
                        for (AEntityC_Renderable entity : allEntities) {
                            world.beginProfiling("MTSRendering", true);
                            entity.render(blendingEnabled, partialTicks);
                            world.endProfiling();
                        }

                        //Reset states.
                        GL11.glShadeModel(GL11.GL_FLAT);
                        if (blendingEnabled) {
                            GlStateManager.enableAlpha();
                        }
                        GlStateManager.disableRescaleNormal();
                    }
                }
            }
        });

        //Get the list of default resource packs here to inject a custom parser for auto-generating JSONS.
        //FAR easier than trying to use the bloody bakery system.
        //Normally we'd add our pack to the current loader, but this gets wiped out during reloads and unless we add our pack to the main list, it won't stick.
        //To do this, we use reflection to get the field from the main MC class that holds the master list to add our custom ones.
        //((SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).reloadResourcePack(new PackResourcePack(MasterLoader.MODID + "_packs"));
        List<IResourcePack> defaultPacks = null;
        for (Field field : Minecraft.class.getDeclaredFields()) {
            if (field.getName().equals("defaultResourcePacks") || field.getName().equals("field_110449_ao")) {
                try {
                    if (!field.isAccessible()) {
                        field.setAccessible(true);
                    }

                    defaultPacks = (List<IResourcePack>) field.get(Minecraft.getMinecraft());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        //Check to make sure we have the pack list before continuing.
        if (defaultPacks == null) {
            InterfaceManager.coreInterface.logError("Could not get default pack list. Item icons will be disabled.");
            return;
        }

        //Now that we have the custom resource pack location, add our built-in loader.
        //This one auto-generates item JSONs.
        defaultPacks.add(new PackResourcePack(InterfaceManager.coreModID + "_packs"));

        //Now register items for the packs.
        //When we register a pack item from an external pack, we'll need to make a resource loader for it.
        //This is done to allow MC/Forge to play nice with item textures.
        for (AItemBase item : BuilderItem.itemMap.keySet()) {
            if (item instanceof AItemPack) {
                AItemPack<?> packItem = (AItemPack<?>) item;
                if (!PackResourcePack.createdLoaders.containsKey(packItem.definition.packID)) {
                    defaultPacks.add(new PackResourcePack(packItem.definition.packID));
                }
                ModelLoader.setCustomModelResourceLocation(BuilderItem.itemMap.get(packItem), 0, new ModelResourceLocation(InterfaceManager.coreModID + "_packs:" + packItem.getRegistrationName(), "inventory"));
            }
        }

        //Now that we've created all the pack loaders, reload the resource manager to add them to the systems.
        FMLClientHandler.instance().refreshResources(VanillaResourceType.MODELS);
    }

    /**
     * Custom ResourcePack class for auto-generating item JSONs.
     */
    private static class PackResourcePack implements IResourcePack {
        private static final Map<String, PackResourcePack> createdLoaders = new HashMap<>();
        private final String domain;
        private final Set<String> domains;

        private PackResourcePack(String domain) {
            this.domain = domain;
            domains = new HashSet<>();
            domains.add(domain);
            createdLoaders.put(domain, this);
        }

        @Override
        public InputStream getInputStream(ResourceLocation location) throws IOException {
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
        public boolean resourceExists(ResourceLocation location) {
            return domains.contains(location.getNamespace()) && !location.getPath().contains("blockstates") && !location.getPath().contains("armatures") && !location.getPath().contains("mcmeta") && ((location.getPath().endsWith(".json") && !location.getPath().equals("sounds.json")) || location.getPath().endsWith(".png"));
        }

        @Override
        public Set<String> getResourceDomains() {
            return domains;
        }

        @Override
        public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer metadataSerializer, String metadataSectionName) {
            return null;
        }

        @Override
        public BufferedImage getPackImage() {
            return null;
        }

        @Override
        public String getPackName() {
            return "Internal:" + domain;
        }
    }
}
