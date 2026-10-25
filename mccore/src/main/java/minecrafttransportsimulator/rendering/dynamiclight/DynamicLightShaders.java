package minecrafttransportsimulator.rendering.dynamiclight;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import minecrafttransportsimulator.mcinterface.IInterfaceDynamicLightingGL;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * Loads the shared dynamic light GLSL from {@code /assets/mts/shaders/dynamiclight/}.  The files carry no
 * {@code #version} line and write their stage interface through macros ({@code ATTRIBUTE}, {@code VARYING},
 * {@code fragColor}): the loader puts the header of the calling version's dialect in front, GLSL 1.50, or GLSL 1.20
 * with {@code EXT_gpu_shader4} on a GL 2.1 context ({@link IInterfaceDynamicLightingGL#legacyContext}), which renames the 1.50
 * texture calls to that extension's.  So every interface module compiles the same sources with its own raw GL calls,
 * and the sources keep to what both dialects have (no integer {@code min}/{@code clamp}, no {@code mix} by a
 * {@code bvec}, array constructors with their size).  A line {@code #include <file>} is replaced by that file of the
 * folder ({@code voxel.glsl}, the block grid both the light pass and the shadow trace walk).
 */
public final class DynamicLightShaders {
    public static final String FOLDER = "/assets/mts/shaders/dynamiclight/";
    public static final String FULLSCREEN = "fullscreen.vsh";
    public static final String LIGHT_PASS = "light_pass.fsh";
    public static final String COMPOSITE = "light_composite.fsh";
    public static final String BEAM = "lamp_beam.fsh";
    public static final String CORONA = "lamp_corona.fsh";
    public static final String TRACE = "lamp_trace.fsh";
    public static final String COPY = "lamp_copy.fsh";
    public static final String BODY_VERTEX = "lamp_body.vsh";
    public static final String BODY_FRAGMENT = "lamp_body.fsh";
    private static final String INCLUDE = "#include ";

    private static final String MODERN_VERTEX = "#version 150\n#define ATTRIBUTE in\n#define VARYING out\n";
    private static final String MODERN_FRAGMENT = "#version 150\n#define VARYING in\nout vec4 fragColor;\n";
    private static final String LEGACY = "#version 120\n#extension GL_EXT_gpu_shader4 : require\n#define texelFetch texelFetch2D\n#define textureSize textureSize2D\n#define texture texture2D\n#define VARYING varying\n";
    private static final String LEGACY_VERTEX = LEGACY + "#define ATTRIBUTE attribute\n";
    private static final String LEGACY_FRAGMENT = LEGACY + "#define fragColor gl_FragColor\n";

    private DynamicLightShaders() {
    }

    /**
     * Returns the source of {@code name} (a {@code .vsh} or {@code .fsh}) behind the header of its stage in the
     * dialect {@code legacy} names, or null if the file cannot be read.
     */
    public static String getSource(String name, boolean legacy) {
        boolean vertex = name.endsWith(".vsh");
        String header = legacy ? (vertex ? LEGACY_VERTEX : LEGACY_FRAGMENT) : (vertex ? MODERN_VERTEX : MODERN_FRAGMENT);
        String source = read(name);
        if (source == null) {
            return null;
        }
        StringBuilder out = new StringBuilder(header);
        for (String line : source.split("\n", -1)) {
            if (line.startsWith(INCLUDE)) {
                String included = read(line.substring(INCLUDE.length()).trim());
                if (included == null) {
                    return null;
                }
                out.append(included).append('\n');
            } else {
                out.append(line).append('\n');
            }
        }
        return out.toString();
    }

    /** The text of file {@code name} of the folder, or null (logged) if it cannot be read. */
    private static String read(String name) {
        String path = FOLDER + name;
        try (InputStream stream = open(path)) {
            if (stream == null) {
                InterfaceManager.coreInterface.logError("Could not find dynamic light shader " + path);
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            InterfaceManager.coreInterface.logError("Could not read dynamic light shader " + path + ": " + e.getMessage());
            return null;
        }
    }

    private static InputStream open(String path) {
        InputStream stream = DynamicLightShaders.class.getResourceAsStream(path);
        return stream != null ? stream : InterfaceManager.coreInterface.getPackResource(path);
    }
}
