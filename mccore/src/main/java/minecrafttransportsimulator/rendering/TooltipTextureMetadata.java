package minecrafttransportsimulator.rendering;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * Minecraft-style GUI scaling metadata for a raw tooltip texture.  The metadata is read from the
 * texture's companion {@code .png.mcmeta} file and is intentionally version-independent so legacy
 * clients can render modern nine-slice GUI textures identically to newer clients.
 */
public final class TooltipTextureMetadata {
    private static final Map<String, TooltipTextureMetadata> CACHE = new ConcurrentHashMap<>();

    public final String resourceLocation;
    public final ScalingType scalingType;
    public final int textureWidth;
    public final int textureHeight;
    public final int borderLeft;
    public final int borderTop;
    public final int borderRight;
    public final int borderBottom;
    public final boolean stretchInner;

    private TooltipTextureMetadata(String resourceLocation, ScalingType scalingType, int textureWidth, int textureHeight, int borderLeft, int borderTop, int borderRight, int borderBottom, boolean stretchInner) {
        this.resourceLocation = resourceLocation;
        this.scalingType = scalingType;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.borderLeft = borderLeft;
        this.borderTop = borderTop;
        this.borderRight = borderRight;
        this.borderBottom = borderBottom;
        this.stretchInner = stretchInner;
    }

    /** Returns cached scaling metadata for the specified item-pack texture path. */
    public static TooltipTextureMetadata get(String packID, String path) {
        return get(packID, path, (metadataLocation) -> {
            String[] locationParts = metadataLocation.split(":", 2);
            return InterfaceManager.coreInterface.getPackResource("/assets/" + locationParts[0] + "/" + locationParts[1]);
        });
    }

    /**
     * Returns cached scaling metadata using the supplied client resource provider.  Minecraft's
     * resource manager must be used here on modern versions because pack assets may live in a
     * separate mod module that the core mod's classloader cannot see.
     */
    public static TooltipTextureMetadata get(String packID, String path, MetadataStreamProvider metadataProvider) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        String resourceLocation = normalizeResourceLocation(packID, path);
        return CACHE.computeIfAbsent(resourceLocation, (location) -> load(location, metadataProvider));
    }

    /** Emits the rectangles required to render this texture with its configured GUI scaling. */
    public void render(int x, int y, int width, int height, SliceConsumer consumer) {
        if (width <= 0 || height <= 0) {
            return;
        }
        switch (scalingType) {
            case TILE:
                tile(x, y, width, height, 0, 0, textureWidth, textureHeight, consumer);
                break;
            case NINE_SLICE:
                renderNineSlice(x, y, width, height, consumer);
                break;
            case STRETCH:
            default:
                consumer.draw(x, y, width, height, 0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
                break;
        }
    }

    /**
     * Renders a tooltip layer around its content rectangle.  Nine-slice border values are also the
     * destination insets, matching Minecraft's GUI-sprite behavior; non-nine-slice textures retain
     * the traditional four-pixel tooltip padding.
     */
    public void renderTooltip(int contentX, int contentY, int contentWidth, int contentHeight, int defaultPadding, SliceConsumer consumer) {
        int left = scalingType == ScalingType.NINE_SLICE ? borderLeft : defaultPadding;
        int top = scalingType == ScalingType.NINE_SLICE ? borderTop : defaultPadding;
        int right = scalingType == ScalingType.NINE_SLICE ? borderRight : defaultPadding;
        int bottom = scalingType == ScalingType.NINE_SLICE ? borderBottom : defaultPadding;
        render(contentX - left, contentY - top, contentWidth + left + right, contentHeight + top + bottom, consumer);
    }

    private void renderNineSlice(int x, int y, int width, int height, SliceConsumer consumer) {
        int left = Math.min(borderLeft, width / 2);
        int right = Math.min(borderRight, width / 2);
        int top = Math.min(borderTop, height / 2);
        int bottom = Math.min(borderBottom, height / 2);
        int innerWidth = width - left - right;
        int innerHeight = height - top - bottom;
        int sourceInnerWidth = textureWidth - left - right;
        int sourceInnerHeight = textureHeight - top - bottom;

        drawSlice(x, y, left, top, 0, 0, left, top, consumer);
        drawSlice(x + width - right, y, right, top, textureWidth - right, 0, right, top, consumer);
        drawSlice(x, y + height - bottom, left, bottom, 0, textureHeight - bottom, left, bottom, consumer);
        drawSlice(x + width - right, y + height - bottom, right, bottom, textureWidth - right, textureHeight - bottom, right, bottom, consumer);

        drawInner(x + left, y, innerWidth, top, left, 0, sourceInnerWidth, top, consumer);
        drawInner(x + left, y + height - bottom, innerWidth, bottom, left, textureHeight - bottom, sourceInnerWidth, bottom, consumer);
        drawInner(x, y + top, left, innerHeight, 0, top, left, sourceInnerHeight, consumer);
        drawInner(x + width - right, y + top, right, innerHeight, textureWidth - right, top, right, sourceInnerHeight, consumer);
        drawInner(x + left, y + top, innerWidth, innerHeight, left, top, sourceInnerWidth, sourceInnerHeight, consumer);
    }

    private void drawInner(int x, int y, int width, int height, int u, int v, int sourceWidth, int sourceHeight, SliceConsumer consumer) {
        if (stretchInner) {
            drawSlice(x, y, width, height, u, v, sourceWidth, sourceHeight, consumer);
        } else {
            tile(x, y, width, height, u, v, sourceWidth, sourceHeight, consumer);
        }
    }

    private void tile(int x, int y, int width, int height, int u, int v, int sourceWidth, int sourceHeight, SliceConsumer consumer) {
        if (width <= 0 || height <= 0 || sourceWidth <= 0 || sourceHeight <= 0) {
            return;
        }
        for (int xOffset = 0; xOffset < width; xOffset += sourceWidth) {
            int drawWidth = Math.min(sourceWidth, width - xOffset);
            for (int yOffset = 0; yOffset < height; yOffset += sourceHeight) {
                int drawHeight = Math.min(sourceHeight, height - yOffset);
                consumer.draw(x + xOffset, y + yOffset, drawWidth, drawHeight, u, v, drawWidth, drawHeight, textureWidth, textureHeight);
            }
        }
    }

    private void drawSlice(int x, int y, int width, int height, int u, int v, int sourceWidth, int sourceHeight, SliceConsumer consumer) {
        if (width > 0 && height > 0 && sourceWidth > 0 && sourceHeight > 0) {
            consumer.draw(x, y, width, height, u, v, sourceWidth, sourceHeight, textureWidth, textureHeight);
        }
    }

    private static TooltipTextureMetadata load(String resourceLocation, MetadataStreamProvider metadataProvider) {
        try (InputStream stream = metadataProvider.open(resourceLocation + ".mcmeta")) {
            if (stream == null) {
                return stretch(resourceLocation);
            }
            JsonObject root = new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject gui = root.has("gui") && root.get("gui").isJsonObject() ? root.getAsJsonObject("gui") : null;
            JsonObject scaling = gui != null && gui.has("scaling") && gui.get("scaling").isJsonObject() ? gui.getAsJsonObject("scaling") : null;
            if (scaling == null) {
                return stretch(resourceLocation);
            }

            String type = getString(scaling, "type", "stretch");
            if ("nine_slice".equals(type)) {
                int width = getPositiveInt(scaling, "width", 1);
                int height = getPositiveInt(scaling, "height", 1);
                int left;
                int top;
                int right;
                int bottom;
                JsonElement borderElement = scaling.get("border");
                if (borderElement != null && borderElement.isJsonObject()) {
                    JsonObject border = borderElement.getAsJsonObject();
                    left = getNonNegativeInt(border, "left", 0);
                    top = getNonNegativeInt(border, "top", 0);
                    right = getNonNegativeInt(border, "right", 0);
                    bottom = getNonNegativeInt(border, "bottom", 0);
                } else {
                    int border = borderElement != null ? Math.max(0, borderElement.getAsInt()) : 0;
                    left = top = right = bottom = border;
                }
                if (left + right >= width || top + bottom >= height) {
                    InterfaceManager.coreInterface.logError("Invalid nine-slice tooltip metadata for " + resourceLocation + ": border leaves no center slice. Falling back to stretch scaling.");
                    return stretch(resourceLocation);
                }
                boolean stretchInner = scaling.has("stretch_inner") && scaling.get("stretch_inner").getAsBoolean();
                return new TooltipTextureMetadata(resourceLocation, ScalingType.NINE_SLICE, width, height, left, top, right, bottom, stretchInner);
            } else if ("tile".equals(type)) {
                int width = getPositiveInt(scaling, "width", 1);
                int height = getPositiveInt(scaling, "height", 1);
                return new TooltipTextureMetadata(resourceLocation, ScalingType.TILE, width, height, 0, 0, 0, 0, false);
            } else {
                return stretch(resourceLocation);
            }
        } catch (Exception exception) {
            InterfaceManager.coreInterface.logError("Could not read tooltip texture metadata for " + resourceLocation + ": " + exception.getMessage() + ". Falling back to stretch scaling.");
            return stretch(resourceLocation);
        }
    }

    private static TooltipTextureMetadata stretch(String resourceLocation) {
        return new TooltipTextureMetadata(resourceLocation, ScalingType.STRETCH, 1, 1, 0, 0, 0, 0, true);
    }

    private static String normalizeResourceLocation(String packID, String path) {
        String normalizedPath = path.trim().replace('\\', '/');
        if (normalizedPath.startsWith("/assets/")) {
            normalizedPath = normalizedPath.substring("/assets/".length());
            int separator = normalizedPath.indexOf('/');
            if (separator > 0) {
                return normalizedPath.substring(0, separator) + ":" + normalizedPath.substring(separator + 1);
            }
        }
        while (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        return normalizedPath.contains(":") ? normalizedPath : packID + ":" + normalizedPath;
    }

    private static String getString(JsonObject object, String key, String defaultValue) {
        return object.has(key) ? object.get(key).getAsString() : defaultValue;
    }

    private static int getPositiveInt(JsonObject object, String key, int defaultValue) {
        return object.has(key) ? Math.max(1, object.get(key).getAsInt()) : defaultValue;
    }

    private static int getNonNegativeInt(JsonObject object, String key, int defaultValue) {
        return object.has(key) ? Math.max(0, object.get(key).getAsInt()) : defaultValue;
    }

    public enum ScalingType {
        STRETCH,
        TILE,
        NINE_SLICE
    }

    @FunctionalInterface
    public interface SliceConsumer {
        void draw(int x, int y, int width, int height, int u, int v, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight);
    }

    @FunctionalInterface
    public interface MetadataStreamProvider {
        InputStream open(String metadataResourceLocation) throws Exception;
    }
}
