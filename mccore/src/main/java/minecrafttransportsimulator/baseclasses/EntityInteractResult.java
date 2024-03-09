package minecrafttransportsimulator.baseclasses;

import minecrafttransportsimulator.entities.components.AEntityE_Interactable;

/**
 * Helper class for interact return data.
 */
public class EntityInteractResult {
    public final AEntityE_Interactable<?> entity;
    public final BoundingBox box;
    public final Point3D position;

    public EntityInteractResult(AEntityE_Interactable<?> entity, BoundingBox box, Point3D point) {
        this.entity = entity;
        this.box = box;
        this.position = point;
    }
}