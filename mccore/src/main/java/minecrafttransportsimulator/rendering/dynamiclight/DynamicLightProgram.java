package minecrafttransportsimulator.rendering.dynamiclight;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.mcinterface.IInterfaceDynamicLightingGL;

/**
 * One dynamic light GLSL program, built by the version's {@link IInterfaceDynamicLightingGL} from the shared sources
 * ({@link DynamicLightShaders}) rather than MC's shader system, so the same files work on every version.
 * {@code Position} is bound to attribute 0.  Uniform locations are cached by name.
 */
final class DynamicLightProgram {
    final String name;
    final int id;
    private final IInterfaceDynamicLightingGL gl;
    private final Map<String, Integer> locations = new HashMap<>();

    private DynamicLightProgram(IInterfaceDynamicLightingGL gl, String name, int id) {
        this.gl = gl;
        this.name = name;
        this.id = id;
    }

    /** Compiles and links a program, or returns null (logged) if a stage fails. */
    static DynamicLightProgram create(IInterfaceDynamicLightingGL gl, String vertexName, String fragmentName) {
        String vertexSource = DynamicLightShaders.getSource(vertexName, gl.legacyContext());
        String fragmentSource = DynamicLightShaders.getSource(fragmentName, gl.legacyContext());
        if (vertexSource == null || fragmentSource == null) {
            return null;
        }
        int id = gl.createProgram(vertexSource, fragmentSource, fragmentName);
        return id == 0 ? null : new DynamicLightProgram(gl, fragmentName, id);
    }

    void use() {
        gl.useProgram(id);
    }

    /** The location of {@code uniform}, -1 if the program has none. */
    int location(String uniform) {
        Integer location = locations.get(uniform);
        if (location == null) {
            location = gl.uniformLocation(id, uniform);
            locations.put(uniform, location);
        }
        return location;
    }

    void set(String uniform, int value) {
        int l = location(uniform);
        if (l >= 0) {
            gl.setUniform(l, value);
        }
    }

    void set(String uniform, float value) {
        int l = location(uniform);
        if (l >= 0) {
            gl.setUniform(l, value);
        }
    }

    void set(String uniform, float x, float y) {
        int l = location(uniform);
        if (l >= 0) {
            gl.setUniform(l, x, y);
        }
    }

    void set(String uniform, float x, float y, float z) {
        int l = location(uniform);
        if (l >= 0) {
            gl.setUniform(l, x, y, z);
        }
    }

    void set(String uniform, float x, float y, float z, float w) {
        int l = location(uniform);
        if (l >= 0) {
            gl.setUniform(l, x, y, z, w);
        }
    }

    void setInts(String uniform, int x, int y, int z) {
        int l = location(uniform);
        if (l >= 0) {
            gl.setUniformInts(l, x, y, z);
        }
    }

    /** A whole {@code float[]}, {@code vec2[]} or {@code vec4[]} array uniform ({@code size} 1, 2 or 4) from its first element. */
    void setArray(String uniform, float[] values, int size, int count) {
        int l = location(uniform);
        if (l >= 0 && count > 0) {
            gl.setUniformArray(l, values, size, count);
        }
    }

    /** A {@link DynamicLightMatrix} (16 floats, column-major). */
    void setMatrix(String uniform, float[] matrix) {
        int l = location(uniform);
        if (l >= 0) {
            gl.setUniformMatrix(l, matrix);
        }
    }
}
