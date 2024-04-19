package mcinterface1165.mixin.client;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mcinterface1165.InterfaceLoader;
import mcinterface1165.InterfaceSound;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.packloading.PackResourceLoader;
import minecrafttransportsimulator.packloading.PackResourceLoader.ResourceType;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.resources.IResource;
import net.minecraft.resources.SimpleReloadableResourceManager;
import net.minecraft.resources.data.IMetadataSectionSerializer;
import net.minecraft.util.ResourceLocation;

@Mixin(SimpleReloadableResourceManager.class)
public abstract class SimpleReloadableResourceManagerMixin {

    /**
     * Kill off any sounds and models.  Their cached indexes will get fouled here if we don't.
     */
    @Inject(method = "clear", at = @At(value = "TAIL"))
    public void inject_clear(CallbackInfo ci) {
        //SimpleReloadableResourceManager manager = (SimpleReloadableResourceManager) ((Object) this);
        //InterfaceEventsModelLoader.packPacks.forEach(pack -> manager.add(pack));

        //Stop all sounds, since sound slots will have changed.
        InterfaceSound.stopAllSounds();

        //Clear all model caches, since OpenGL indexes will have changed.
        AWrapperWorld world = InterfaceManager.clientInterface.getClientWorld();
        if (world != null) {
        	for(AEntityD_Definable<?> entity : world.getEntitiesExtendingType(AEntityD_Definable.class)) {
        		entity.resetModelsAndAnimations();
        	}
        }
    }

    @Inject(method = "hasResource", at = @At(value = "HEAD"))
    public void inject_hasResource(ResourceLocation location, CallbackInfoReturnable<Boolean> ci) {
        if (InterfaceLoader.packIDs.contains(location.getNamespace())) {
            //IV thing, check if we are supposed to override the path.
            //If so, return true since we know we will have the thing, or auto-create it.
            ci.setReturnValue(!location.getPath().contains("blockstates") && !location.getPath().contains("armatures") && !location.getPath().contains("mcmeta") && !location.getPath().contains("recipes") && !location.getPath().contains("advancements") && ((location.getPath().endsWith(".json") && !location.getPath().equals("sounds.json")) || location.getPath().endsWith(".png")));
        }
    }

    @Inject(method = "getResource", at = @At(value = "HEAD"), cancellable = true)
    public void inject_getResource(ResourceLocation location, CallbackInfoReturnable<IResource> ci) throws IOException {
        if (InterfaceLoader.packIDs.contains(location.getNamespace()) && !location.getPath().contains("recipes") && !location.getPath().contains("advancements")) {
            //We are a IV thing, either return our updated location, or generate the thing we need.
            //Create stream return variable and get raw data.
            final InputStream stream;
            String domain = location.getNamespace();
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
                    stream = InterfaceManager.coreInterface.getPackResource("/assets/" + domain + "/" + rawPackInfo);
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
                        InputStream testStream = InterfaceManager.coreInterface.getPackResource(resourcePath);
                        if (testStream == null) {
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
                        } else {
                            stream = testStream;
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
                        InputStream testStream = InterfaceManager.coreInterface.getPackResource(streamLocation);

                        if (testStream == null) {
                            if (isItemPNG) {
                                //We might not have this file, but we also might have a JSON-defined item here.
                                //Try the JSON standards before throwing an error.
                                String streamJSONLocation = "/assets/" + packID + "/" + rawPackInfo;
                                stream = InterfaceManager.coreInterface.getPackResource(streamJSONLocation);
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
                        } else {
                            stream = testStream;
                        }
                    } else {
                        //No pack item for this texture.  Must be an internal texture for other things.
                        //In this case, we just get the stream exact location.
                        String streamLocation = "/assets/" + domain + "/" + rawPackInfo;
                        stream = InterfaceManager.coreInterface.getPackResource(streamLocation);
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
            ci.setReturnValue(new IResource() {
                @Override
                public void close() throws IOException {
                    //Don't need to close anything.
                }

                @Override
                public ResourceLocation getLocation() {
                    return location;
                }

                @Override
                public InputStream getInputStream() {
                    return stream;
                }

                @Override
                public <T> T getMetadata(IMetadataSectionSerializer<T> pSerializer) {
                    return null;
                }

                @Override
                public String getSourceName() {
                    return domain + "_pack";
                }
            });
        }
    }
}
