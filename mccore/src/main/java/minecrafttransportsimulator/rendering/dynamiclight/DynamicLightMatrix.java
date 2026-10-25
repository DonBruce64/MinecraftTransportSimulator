package minecrafttransportsimulator.rendering.dynamiclight;

/**
 * 4x4 float matrices as 16 floats, column-major (the layout {@code glUniformMatrix4fv} takes untransposed), for the
 * dynamic light renderer: mccore has no JOML.  {@code m[c * 4 + r]} is column {@code c}, row {@code r}; the methods
 * follow JOML's conventions (GL clip depth -1..1, {@link #translate} multiplies on the right).
 */
public final class DynamicLightMatrix {
    private DynamicLightMatrix() {
    }

    public static float[] identity(float[] m) {
        for (int i = 0; i < 16; i++) {
            m[i] = (i % 5) == 0 ? 1F : 0F;
        }
        return m;
    }

    /** {@code out = a * b}; {@code out} may be {@code a} or {@code b}. */
    public static float[] mul(float[] a, float[] b, float[] out) {
        float[] r = out == a || out == b ? new float[16] : out;
        for (int c = 0; c < 4; c++) {
            for (int row = 0; row < 4; row++) {
                r[c * 4 + row] = a[row] * b[c * 4] + a[4 + row] * b[c * 4 + 1] + a[8 + row] * b[c * 4 + 2] + a[12 + row] * b[c * 4 + 3];
            }
        }
        if (r != out) {
            System.arraycopy(r, 0, out, 0, 16);
        }
        return out;
    }

    /** {@code out = m^-1} (left as is when {@code m} is singular); {@code out} may be {@code m}. */
    public static float[] invert(float[] m, float[] out) {
        float a = m[0] * m[5] - m[1] * m[4];
        float b = m[0] * m[6] - m[2] * m[4];
        float c = m[0] * m[7] - m[3] * m[4];
        float d = m[1] * m[6] - m[2] * m[5];
        float e = m[1] * m[7] - m[3] * m[5];
        float f = m[2] * m[7] - m[3] * m[6];
        float g = m[8] * m[13] - m[9] * m[12];
        float h = m[8] * m[14] - m[10] * m[12];
        float i = m[8] * m[15] - m[11] * m[12];
        float j = m[9] * m[14] - m[10] * m[13];
        float k = m[9] * m[15] - m[11] * m[13];
        float l = m[10] * m[15] - m[11] * m[14];
        float det = a * l - b * k + c * j + d * i - e * h + f * g;
        if (det == 0F) {
            if (out != m) {
                System.arraycopy(m, 0, out, 0, 16);
            }
            return out;
        }
        det = 1F / det;
        float[] r = new float[16];
        r[0] = (m[5] * l - m[6] * k + m[7] * j) * det;
        r[1] = (-m[1] * l + m[2] * k - m[3] * j) * det;
        r[2] = (m[13] * f - m[14] * e + m[15] * d) * det;
        r[3] = (-m[9] * f + m[10] * e - m[11] * d) * det;
        r[4] = (-m[4] * l + m[6] * i - m[7] * h) * det;
        r[5] = (m[0] * l - m[2] * i + m[3] * h) * det;
        r[6] = (-m[12] * f + m[14] * c - m[15] * b) * det;
        r[7] = (m[8] * f - m[10] * c + m[11] * b) * det;
        r[8] = (m[4] * k - m[5] * i + m[7] * g) * det;
        r[9] = (-m[0] * k + m[1] * i - m[3] * g) * det;
        r[10] = (m[12] * e - m[13] * c + m[15] * a) * det;
        r[11] = (-m[8] * e + m[9] * c - m[11] * a) * det;
        r[12] = (-m[4] * j + m[5] * h - m[6] * g) * det;
        r[13] = (m[0] * j - m[1] * h + m[2] * g) * det;
        r[14] = (-m[12] * d + m[13] * b - m[14] * a) * det;
        r[15] = (m[8] * d - m[9] * b + m[10] * a) * det;
        System.arraycopy(r, 0, out, 0, 16);
        return out;
    }

    /** A perspective projection, {@code fovY} radians tall. */
    public static float[] perspective(float fovY, float aspect, float near, float far, float[] out) {
        float h = (float) Math.tan(fovY * 0.5F);
        for (int i = 0; i < 16; i++) {
            out[i] = 0F;
        }
        out[0] = 1F / (h * aspect);
        out[5] = 1F / h;
        out[10] = (far + near) / (near - far);
        out[11] = -1F;
        out[14] = 2F * far * near / (near - far);
        return out;
    }

    /** A view from the origin along {@code d} with {@code up} up (JOML {@code lookAt} from the origin). */
    public static float[] lookAlong(float dx, float dy, float dz, float upX, float upY, float upZ, float[] out) {
        //JOML's dir is eye - centre: the view looks down -z.
        float zx = -dx;
        float zy = -dy;
        float zz = -dz;
        float zl = (float) (1.0 / Math.sqrt(zx * zx + zy * zy + zz * zz));
        zx *= zl;
        zy *= zl;
        zz *= zl;
        float xx = upY * zz - upZ * zy;
        float xy = upZ * zx - upX * zz;
        float xz = upX * zy - upY * zx;
        float xl = (float) (1.0 / Math.sqrt(xx * xx + xy * xy + xz * xz));
        xx *= xl;
        xy *= xl;
        xz *= xl;
        float yx = zy * xz - zz * xy;
        float yy = zz * xx - zx * xz;
        float yz = zx * xy - zy * xx;
        out[0] = xx;
        out[1] = yx;
        out[2] = zx;
        out[3] = 0F;
        out[4] = xy;
        out[5] = yy;
        out[6] = zy;
        out[7] = 0F;
        out[8] = xz;
        out[9] = yz;
        out[10] = zz;
        out[11] = 0F;
        out[12] = 0F;
        out[13] = 0F;
        out[14] = 0F;
        out[15] = 1F;
        return out;
    }

    /** {@code m = m * translation(x, y, z)}. */
    public static float[] translate(float[] m, float x, float y, float z) {
        for (int row = 0; row < 4; row++) {
            m[12 + row] += m[row] * x + m[4 + row] * y + m[8 + row] * z;
        }
        return m;
    }

    /** {@code m * (x, y, z, w)} into {@code out[0..3]}. */
    public static float[] transform(float[] m, float x, float y, float z, float w, float[] out) {
        float rx = m[0] * x + m[4] * y + m[8] * z + m[12] * w;
        float ry = m[1] * x + m[5] * y + m[9] * z + m[13] * w;
        float rz = m[2] * x + m[6] * y + m[10] * z + m[14] * w;
        float rw = m[3] * x + m[7] * y + m[11] * z + m[15] * w;
        out[0] = rx;
        out[1] = ry;
        out[2] = rz;
        out[3] = rw;
        return out;
    }

    /** The largest of the lengths of {@code m}'s first three columns: its largest scale. */
    public static float maxScale(float[] m) {
        float x = (float) Math.sqrt(m[0] * m[0] + m[1] * m[1] + m[2] * m[2]);
        float y = (float) Math.sqrt(m[4] * m[4] + m[5] * m[5] + m[6] * m[6]);
        float z = (float) Math.sqrt(m[8] * m[8] + m[9] * m[9] + m[10] * m[10]);
        return Math.max(x, Math.max(y, z));
    }
}
