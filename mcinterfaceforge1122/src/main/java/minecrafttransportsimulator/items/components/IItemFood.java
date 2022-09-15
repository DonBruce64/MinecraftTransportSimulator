package minecrafttransportsimulator.items.components;

import java.util.List;

import minecrafttransportsimulator.jsondefs.JSONPotionEffect;

/**
 * Interface that allows for this item to be considered food and eaten.
 *
 * @author don_bruce
 */
public interface IItemFood {

    /**
     * Returns the number of ticks it takes to eat this item.  If this item cannot be eaten, return 0.
     * If 0 is returned here, then no other methods should be called in this interface as it's assumed
     * this food item isn't ready for eating.
     */
    int getTimeToEat();

    /**
     * Returns true if this food item is a drink rather than a physical food.
     */
    boolean isDrink();

    /**
     * Returns the amount of hunger this item refills when eaten.
     */
    int getHungerAmount();

    /**
     * Returns the saturation of this food when eaten.
     */
    float getSaturationAmount();

    /**
     * Returns a list of effects this food item has, or null if there are no effects.
     */
    List<JSONPotionEffect> getEffects();
}
