package mcinterface1211;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.mcinterface.IInterfaceCore;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;

class InterfaceCore implements IInterfaceCore {
    protected static final Map<String, List<BuilderItem>> taggedItems = new HashMap<>();

    /**
     * Cache that remembers which lookup strategy succeeded for a given resource path.
     * This avoids repeated failed classloader lookups (up to 5 per call) on the render thread.
     * Values: 0=not cached, 1-5=strategy index that worked, -1=resource not found.
     */
    private static final ConcurrentHashMap<String, Integer> resourceLookupCache = new ConcurrentHashMap<>();

    @Override
    public boolean isGameFlattened() {
        return true;
    }

    @Override
    public boolean isModPresent(String modID) {
        return ModList.get().isLoaded(modID);
    }

    @Override
    public boolean isFluidValid(String fluidID) {
        for (ResourceLocation location : BuiltInRegistries.FLUID.keySet()) {
            if (location.getPath().equals(fluidID)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getModName(String modID) {
        return ModList.get().getModContainerById(modID).get().getModInfo().getDisplayName();
    }
    
    @Override
    public InputStream getPackResource(String resource) {
        int assetsIndexEnd = resource.indexOf("assets/") + "assets/".length();
        int modIDEnd = resource.indexOf("/", assetsIndexEnd + 1);
        String modID = resource.substring(assetsIndexEnd, modIDEnd);
        //Strip leading slash for classloader lookup (ClassLoader.getResourceAsStream doesn't use leading slash).
        String cleanResource = resource.startsWith("/") ? resource.substring(1) : resource;
        String absoluteResource = resource.startsWith("/") ? resource : "/" + resource;

        //Check if we already know which lookup strategy works for this resource.
        Integer cachedStrategy = resourceLookupCache.get(resource);
        if (cachedStrategy != null) {
            if (cachedStrategy == -1) {
                return null; //Known missing resource.
            }
            InputStream stream = getResourceByStrategy(cachedStrategy, cleanResource, absoluteResource, resource);
            if (stream != null) {
                return stream;
            }
            //Cache was stale (shouldn't happen), fall through to full lookup.
        }

        //Full lookup — try each strategy and cache the one that works.
        //In NeoForge 1.21.1, mods are loaded via JPMS ModuleLayer. ModContainer no longer has getMod().
        //Use InterfaceLoader's classloader which sees all fat-jar'd resources (including pack assets).
        //This works because all pack resources are embedded in the same JAR as InterfaceLoader.
        if (ModList.get().isLoaded(modID)) {
            InputStream stream = InterfaceLoader.class.getClassLoader().getResourceAsStream(cleanResource);
            if (stream != null) {
                resourceLookupCache.put(resource, 1);
                return stream;
            }
            //Also try with leading slash via Class.getResourceAsStream (absolute path lookup).
            stream = InterfaceLoader.class.getResourceAsStream(absoluteResource);
            if (stream != null) {
                resourceLookupCache.put(resource, 2);
                return stream;
            }
            if (modID.equals(InterfaceLoader.MODID)) {
                //For dev builds, the core files aren't in the main jar yet and are in their own compiled one.
                //This requires us to check a class of that jar vs the mod jar for the resource.
                stream = InterfaceManager.class.getResourceAsStream(resource);
                if (stream != null) {
                    resourceLookupCache.put(resource, 3);
                    return stream;
                }
                stream = InterfaceManager.class.getClassLoader().getResourceAsStream(cleanResource);
                if (stream != null) {
                    resourceLookupCache.put(resource, 4);
                    return stream;
                }
            }
        }
        //Mod not found by ID, try the InterfaceLoader's own classloader (covers fat-jar'd pack resources).
        InputStream stream = InterfaceLoader.class.getClassLoader().getResourceAsStream(cleanResource);
        if (stream != null) {
            resourceLookupCache.put(resource, 5);
            return stream;
        }
        //Try to get a Minecraft texture, we use the classloader of the block class, since it's common to servers and clients.
        stream = Blocks.AIR.getClass().getResourceAsStream(resource);
        if (stream != null) {
            resourceLookupCache.put(resource, 6);
            return stream;
        }
        resourceLookupCache.put(resource, -1);
        return null;
    }

    /**Fast path: directly call the classloader strategy that previously succeeded for this resource.*/
    private static InputStream getResourceByStrategy(int strategy, String cleanResource, String absoluteResource, String originalResource) {
        switch (strategy) {
            case 1: return InterfaceLoader.class.getClassLoader().getResourceAsStream(cleanResource);
            case 2: return InterfaceLoader.class.getResourceAsStream(absoluteResource);
            case 3: return InterfaceManager.class.getResourceAsStream(originalResource);
            case 4: return InterfaceManager.class.getClassLoader().getResourceAsStream(cleanResource);
            case 5: return InterfaceLoader.class.getClassLoader().getResourceAsStream(cleanResource);
            case 6: return Blocks.AIR.getClass().getResourceAsStream(originalResource);
            default: return null;
        }
    }

    @Override
    public void logError(String message) {
        InterfaceLoader.LOGGER.error("MTSERROR: " + message);
    }

    @Override
    public IWrapperNBT getNewNBTWrapper() {
        return new WrapperNBT();
    }

    @Override
    public IWrapperItemStack getAutoGeneratedStack(AItemBase item, IWrapperNBT data) {
        WrapperItemStack newStack = new WrapperItemStack(new ItemStack(BuilderItem.itemMap.get(item)));
        newStack.setData(data);
        return newStack;
    }

    @Override
    public IWrapperItemStack getStackForProperties(String name, int meta, int qty) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(name));
        if (item != null && item != Items.AIR) {
            return new WrapperItemStack(new ItemStack(item, qty));
        } else {
            return new WrapperItemStack(ItemStack.EMPTY.copy());
        }
    }

    @Override
    public String getStackItemName(IWrapperItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(((WrapperItemStack) stack).stack.getItem()).toString();
    }

    @Override
    public boolean isOredictMatch(IWrapperItemStack stackA, IWrapperItemStack stackB) {
        return !((WrapperItemStack) stackA).stack.isEmpty() && ((WrapperItemStack) stackA).stack.is(((WrapperItemStack) stackB).stack.getItem());
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<IWrapperItemStack> getOredictMaterials(String oreName, int stackSize) {
        //Convert to lowercase in case we are camelCase from oreDict systems.
        //Also do a bunch of stupid stream crap, cause hashmaps are clearly not made to lookup things...
        String lowerCaseOre = oreName.toLowerCase(Locale.ROOT);
        List<IWrapperItemStack> stacks = new ArrayList<>();
        Stream<TagKey<Item>> tagStream = BuiltInRegistries.ITEM.getTagNames().filter(tagKey -> tagKey.location().getPath().equals(lowerCaseOre));
        tagStream.forEach(tagKey -> {
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tagKey)) {
                stacks.add(new WrapperItemStack(new ItemStack(holder.value(), stackSize)));
            }
        });
        //Couldn't find normal OreDict, check our internal stuff.
        if (stacks.isEmpty()) {
            List<BuilderItem> items = taggedItems.get(lowerCaseOre);
            if (items != null) {
                items.forEach(item -> stacks.add(new WrapperItemStack(new ItemStack(item, stackSize))));
            }
        }

        return stacks;
    }
}
