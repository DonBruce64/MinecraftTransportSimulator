package mcinterface261;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.packloading.PackResourceLoader;
import minecrafttransportsimulator.packloading.PackResourceLoader.ResourceType;
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
            String rawPackInfo = location.getPath();
            String domain = !location.getNamespace().equals(InterfaceLoader.MODID) ? location.getNamespace() : getPackID(rawPackInfo);

            //Handle MC 1.21.4+ item definition JSON: items/<packid>.<sysname>.json
            //These point MC at the model JSON so it knows how to render the item.
            if (rawPackInfo.startsWith("items/") && rawPackInfo.endsWith(".json")) {
                String nameWithoutPrefix = rawPackInfo.substring("items/".length(), rawPackInfo.length() - ".json".length());
                //Name is either "<packid>.<sysname>" or just "<sysname>" if namespace is already the packid.
                String packID;
                String systemName;
                int dotIndex = nameWithoutPrefix.indexOf('.');
                if (dotIndex != -1) {
                    packID = nameWithoutPrefix.substring(0, dotIndex);
                    systemName = nameWithoutPrefix.substring(dotIndex + 1);
                } else {
                    packID = domain;
                    systemName = nameWithoutPrefix;
                }
                if (PackParser.getAllPackIDs().contains(packID) && PackParser.getItem(packID, systemName) != null) {
                    //Synthesize the item definition JSON that points to our model.
                    //Pack mods ship their model JSONs under the legacy "mts" namespace at
                    //assets/mts/models/item/<packID>.<sysname>.json, so reference them via mts:item/...
                    //Format: {"model":{"type":"minecraft:model","model":"mts:item/<packid>.<sysname>"}}
                    String modelPath = InterfaceLoader.MODID + ":item/" + packID + "." + systemName;
                    String fakeJSON = "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"" + modelPath + "\"}}";
                    final byte[] bytes = fakeJSON.getBytes(StandardCharsets.UTF_8);
                    return () -> new ByteArrayInputStream(bytes);
                }
                return null;
            }

            //Handle model JSON: models/item/<packid>.<sysname>.json
            //These define the actual item model geometry and texture.
            if (rawPackInfo.startsWith("models/item/") && rawPackInfo.endsWith(".json")) {
                String nameWithoutPrefix = rawPackInfo.substring("models/item/".length(), rawPackInfo.length() - ".json".length());
                int dotIndex = nameWithoutPrefix.indexOf('.');
                if (dotIndex != -1) {
                    String packID = nameWithoutPrefix.substring(0, dotIndex);
                    String systemName = nameWithoutPrefix.substring(dotIndex + 1);
                    if (PackParser.getAllPackIDs().contains(packID)) {
                        AItemPack<?> packItem = PackParser.getItem(packID, systemName);
                        if (packItem != null) {
                            //Try to load a hand-authored item JSON first (classification-specific path, e.g. assets/<packID>/models/item/parts/<systemName>.json).
                            String resourcePath = PackResourceLoader.getPackResource(packItem.definition, ResourceType.ITEM_JSON, systemName);
                            InputStream initialStream = InterfaceManager.coreInterface.getPackResource(resourcePath);
                            
                            //If not found, try the legacy path (assets/mts/models/item/<packID>.<sysName>.json)
                            //which is where 1.20.1/1.21.1 packs actually put their model JSON files.
                            if (initialStream == null) {
                                String legacyPath = "/assets/" + InterfaceLoader.MODID + "/models/item/" + packID + "." + systemName + ".json";
                                initialStream = InterfaceManager.coreInterface.getPackResource(legacyPath);
                            }
                            
                            if (initialStream != null) {
                                String modelJSON;
                                try {
                                    modelJSON = new String(initialStream.readAllBytes(), StandardCharsets.UTF_8);
                                } catch (Exception e) {
                                    return null;
                                }
                                modelJSON = normalizeItemModelTextureReferences(modelJSON, packID, packItem);
                                //Add particle field if missing to suppress Minecraft warnings
                                if (!modelJSON.contains("\"particle\"")) {
                                    //Find the textures closing brace and add particle before it
                                    int texturesEnd = modelJSON.indexOf("}", modelJSON.indexOf("\"textures\""));
                                    if (texturesEnd != -1) {
                                        //Extract layer0 texture to use as particle
                                        int layer0Start = modelJSON.indexOf("\"layer0\"");
                                        if (layer0Start != -1) {
                                            int layer0ValueStart = modelJSON.indexOf("\"", layer0Start + 10);
                                            if (layer0ValueStart != -1) {
                                                int layer0ValueEnd = modelJSON.indexOf("\"", layer0ValueStart + 1);
                                                if (layer0ValueEnd != -1) {
                                                    String layer0Value = modelJSON.substring(layer0ValueStart, layer0ValueEnd + 1);
                                                    modelJSON = modelJSON.substring(0, texturesEnd) + ",\"particle\":" + layer0Value + modelJSON.substring(texturesEnd);
                                                }
                                            }
                                        }
                                    }
                                }
                                final byte[] bytes = modelJSON.getBytes(StandardCharsets.UTF_8);
                                return () -> new ByteArrayInputStream(bytes);
                            }
                            //Auto-generate: get the item texture path and synthesize the JSON.
                            String itemTexturePath = PackResourceLoader.getPackResource(packItem.definition, ResourceType.ITEM_PNG, getItemResourceName(packItem));
                            //Strip "/assets/<packID>/" prefix (implied by JSON domain) and ".png" suffix.
                            itemTexturePath = itemTexturePath.substring(("/assets/" + packID + "/").length());
                            if (itemTexturePath.startsWith("textures/")) {
                                itemTexturePath = itemTexturePath.substring("textures/".length());
                            }
                            itemTexturePath = itemTexturePath.substring(0, itemTexturePath.length() - ".png".length());
                            //Prefix with packID namespace so MC sends the PNG request to the right loader.
                            itemTexturePath = packID + ":" + itemTexturePath;
                            String fakeJSON = "{\"parent\":\"mts:item/basic\",\"textures\":{\"layer0\":\"" + itemTexturePath + "\"},\"particle\":\"" + itemTexturePath + "\"}";
                            final byte[] bytes = fakeJSON.getBytes(StandardCharsets.UTF_8);
                            return () -> new ByteArrayInputStream(bytes);
                        }
                    }
                }
                return null;
            }

            //Handle PNG redirects for pack item textures.
            if ((PackParser.getAllPackIDs().contains(location.getNamespace()) || PackParser.getAllPackIDs().contains(getPackID(rawPackInfo))) && rawPackInfo.endsWith(".png")) {
                String streamLocation = "/assets/" + domain + "/" + rawPackInfo;
                InputStream stream = InterfaceManager.coreInterface.getPackResource(streamLocation);
                if (stream == null) {
                    String requestedName = rawPackInfo.substring(rawPackInfo.lastIndexOf("/") + 1, rawPackInfo.length() - ".png".length());
                    AItemPack<?> packItem = getPackItemByTextureName(domain, requestedName);
                    if (packItem != null) {
                        streamLocation = PackResourceLoader.getPackResource(packItem.definition, ResourceType.ITEM_PNG, getItemResourceName(packItem));
                        stream = InterfaceManager.coreInterface.getPackResource(streamLocation);
                    }
                    if (stream == null && !streamLocation.contains("/assets/mts/textures/mcfont") && ConfigSystem.settings.general.devMode.value) {
                        InterfaceManager.coreInterface.logError("Couldn't find requested PNG: " + streamLocation);
                    }
                    if (stream == null) {
                        return null;
                    }
                }
                final InputStream finalStream = stream;
                return () -> finalStream;
            }

            if (location.getNamespace().equals(InterfaceLoader.MODID) && rawPackInfo.startsWith("textures/") && rawPackInfo.endsWith(".png")) {
                String requestedName = rawPackInfo.substring(rawPackInfo.lastIndexOf("/") + 1, rawPackInfo.length() - ".png".length());
                String packID = getPackID(requestedName + ".png");
                AItemPack<?> packItem = getPackItemByTextureName(packID, requestedName);
                if (packItem != null) {
                    String streamLocation = PackResourceLoader.getPackResource(packItem.definition, ResourceType.ITEM_PNG, getItemResourceName(packItem));
                    InputStream stream = InterfaceManager.coreInterface.getPackResource(streamLocation);
                    if (stream != null) {
                        final InputStream finalStream = stream;
                        return () -> finalStream;
                    }
                }
            }

            return null;
        }

        @Override
        public Set<String> getNamespaces(PackType pType) {
            //Return mts namespace plus all pack namespaces so MC routes PNG requests here.
            Set<String> all = new HashSet<>(fakeDomains);
            all.addAll(domains);
            return all;
        }

        @Override
        public <T> T getMetadataSection(MetadataSectionType<T> pDeserializer) {
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
            //Minecraft 1.21+ uses listResources to discover all JSON files. We must report our synthetic files here,
            //otherwise Minecraft's asset loader will never discover them and will never call getResource().
            if (pType == PackType.CLIENT_RESOURCES && pNamespace.equals(InterfaceLoader.MODID)) {
                if (pPath.equals("items")) {
                    for (String packID : PackParser.getAllPackIDs()) {
                        for (AItemPack<?> item : PackParser.getAllItemsForPack(packID, false)) {
                            if (item.autoGenerate()) {
                                String regName = item.getRegistrationName();
                                Identifier loc = Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "items/" + regName + ".json");
                                IoSupplier<InputStream> supplier = getResource(pType, loc);
                                if (supplier != null) {
                                    pResourceOutput.accept(loc, supplier);
                                }
                            }
                        }
                    }
                } else if (pPath.equals("models/item") || pPath.equals("models")) {
                    for (String packID : PackParser.getAllPackIDs()) {
                        for (AItemPack<?> item : PackParser.getAllItemsForPack(packID, false)) {
                            if (item.autoGenerate()) {
                                String regName = item.getRegistrationName();
                                Identifier loc = Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "models/item/" + regName + ".json");
                                IoSupplier<InputStream> supplier = getResource(pType, loc);
                                if (supplier != null) {
                                    pResourceOutput.accept(loc, supplier);
                                }
                            }
                        }
                    }
                }
            }
            if (pType == PackType.CLIENT_RESOURCES && domains.contains(pNamespace) && (pPath.equals("textures") || pPath.equals("textures/item") || pPath.equals("textures/items") || pPath.equals("textures/parts"))) {
                for (AItemPack<?> item : PackParser.getAllItemsForPack(pNamespace, false)) {
                    if (item.autoGenerate()) {
                        String itemTexturePath = PackResourceLoader.getPackResource(item.definition, ResourceType.ITEM_PNG, getItemResourceName(item));
                        itemTexturePath = itemTexturePath.substring(("/assets/" + pNamespace + "/").length());
                        Identifier loc = Identifier.fromNamespaceAndPath(pNamespace, itemTexturePath);
                        IoSupplier<InputStream> supplier = getResource(pType, loc);
                        if (supplier != null) {
                            pResourceOutput.accept(loc, supplier);
                        }
                        Identifier legacyLoc = Identifier.fromNamespaceAndPath(pNamespace, "textures/item/" + getItemResourceName(item) + ".png");
                        IoSupplier<InputStream> legacySupplier = getResource(pType, legacyLoc);
                        if (legacySupplier != null) {
                            pResourceOutput.accept(legacyLoc, legacySupplier);
                        }
                        Identifier partsAliasLoc = Identifier.fromNamespaceAndPath(pNamespace, "textures/parts/" + getItemResourceName(item) + ".png");
                        IoSupplier<InputStream> partsAliasSupplier = getResource(pType, partsAliasLoc);
                        if (partsAliasSupplier != null) {
                            pResourceOutput.accept(partsAliasLoc, partsAliasSupplier);
                        }
                    }
                }
            }
        }

        private static String getItemResourceName(AItemPack<?> item) {
            String registrationName = item.getRegistrationName();
            String packPrefix = item.definition.packID + ".";
            return registrationName.startsWith(packPrefix) ? registrationName.substring(packPrefix.length()) : item.definition.systemName;
        }

        private static AItemPack<?> getPackItemByTextureName(String packID, String textureName) {
            AItemPack<?> directItem = PackParser.getItem(packID, textureName);
            if (directItem != null) {
                return directItem;
            }
            for (AItemPack<?> item : PackParser.getAllItemsForPack(packID, false)) {
                if (getItemResourceName(item).equals(textureName)) {
                    return item;
                }
            }
            return null;
        }

        private static String normalizeItemModelTextureReferences(String modelJSON, String packID, AItemPack<?> item) {
            String resourceName = getItemResourceName(item);
            String normalizedTexture = packID + ":item/" + resourceName;
            modelJSON = modelJSON.replace(packID + ":items/items/" + resourceName, normalizedTexture);
            modelJSON = modelJSON.replace(packID + ":items/parts/" + resourceName, normalizedTexture);
            modelJSON = modelJSON.replace(packID + ":items/vehicles/" + resourceName, normalizedTexture);
            modelJSON = modelJSON.replace(packID + ":parts/" + resourceName, normalizedTexture);
            modelJSON = modelJSON.replace("minecraft:" + resourceName, normalizedTexture);
            return modelJSON;
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
