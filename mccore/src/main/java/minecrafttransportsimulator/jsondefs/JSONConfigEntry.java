package minecrafttransportsimulator.jsondefs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

/**
 * Config entry.  This is generic and used for most config entries in the JSON.
 * is called.  Default values are assigned in the field declaration, while
 * comments are simply fields of their own.  Note that sub-classes MUST
 * be static to use their default values!
 * <p>
 * An Integer entry gets a range by annotating its field with {@link IntRange}.  The range lives in the
 * annotation, not in the entry: Gson builds every entry it loads from disk without running the
 * constructor, so anything the entry stored itself would be lost.  {@link #bindRanges} copies the
 * annotation onto the loaded entries after each load, into {@code transient} fields that are never written.
 *
 * @author don_bruce
 */
public class JSONConfigEntry<ConfigType> {
    public ConfigType value;
    public String comment;

    /** Bounds from {@link IntRange}; set by {@link #bindRanges}, never serialized. */
    public transient boolean ranged;
    public transient int min;
    public transient int max;

    public JSONConfigEntry(ConfigType defaultValue, String comment) {
        this.value = defaultValue;
        this.comment = comment;
    }

    /**
     * Declares the inclusive range of an Integer entry, on its field.  Shown as a number button in the config GUI;
     * read the value through {@link #getRangedInt()} so a hand-edited file cannot go out of range.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface IntRange {
        int min();

        int max();
    }

    /** Copies the {@link IntRange} of every annotated entry field of {@code settings} onto the entry it holds. */
    public static void bindRanges(Object settings) {
        for (Field field : settings.getClass().getFields()) {
            IntRange range = field.getAnnotation(IntRange.class);
            if (range != null && field.getType().equals(JSONConfigEntry.class)) {
                try {
                    JSONConfigEntry<?> entry = (JSONConfigEntry<?>) field.get(settings);
                    if (entry != null) {
                        entry.ranged = true;
                        entry.min = range.min();
                        entry.max = range.max();
                    }
                } catch (IllegalAccessException e) {
                    //Public fields only, so this cannot happen.
                }
            }
        }
    }

    /** The value of an Integer entry, clamped to its {@link IntRange} if it has one. */
    public int getRangedInt() {
        int intValue = (Integer) value;
        return ranged ? Math.max(min, Math.min(max, intValue)) : intValue;
    }
}
