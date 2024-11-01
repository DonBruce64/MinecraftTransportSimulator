package mcinterface1182;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.BoundingBoxHitResult;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup.CollisionType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * This class is essentially a collective list of BoundingBoxes.  It intercepts all AABB
 * calls and does checks for each BoundingBox that's in the passed-in list.
 * Mostly used for entities that need complex collision mapping, because MC don't let you have more
 * than one AABB per entity, but somehow you can have more than one for something as small as a block?
 *
 * @author don_bruce
 */
public class WrapperAABBCollective extends AABB {
    private final AEntityE_Interactable<?> interactable;
    private final boolean collision;
    private final Set<BoundingBox> boxes = new HashSet<>();

    public WrapperAABBCollective(AEntityE_Interactable<?> interactable, boolean collision) {
        super(interactable.encompassingBox.globalCenter.x - interactable.encompassingBox.widthRadius, interactable.encompassingBox.globalCenter.y - interactable.encompassingBox.heightRadius, interactable.encompassingBox.globalCenter.z - interactable.encompassingBox.depthRadius, interactable.encompassingBox.globalCenter.x + interactable.encompassingBox.widthRadius, interactable.encompassingBox.globalCenter.y + interactable.encompassingBox.heightRadius, interactable.encompassingBox.globalCenter.z + interactable.encompassingBox.depthRadius);
        this.interactable = interactable;
        this.collision = collision;
    }

    public synchronized Set<BoundingBox> getBoxes() {
        if (boxes.isEmpty()) {
            (interactable instanceof AEntityF_Multipart ? ((AEntityF_Multipart<?>) interactable).allCollisionBoxes : interactable.collisionBoxes).forEach(box -> {
                if (collision) {
                    if (box.collisionTypes.contains(CollisionType.ENTITY)) {
                        boxes.add(box);
                    }
                } else {
                    if (box.collisionTypes.contains(CollisionType.ATTACK) || box.collisionTypes.contains(CollisionType.CLICK)) {
                        boxes.add(box);
                    }
                }
            });
        }
        return boxes;
    }

    @Override
    public WrapperAABBCollective inflate(double value) {
        return this;
    }

    @Override
    public boolean intersects(double otherMinX, double otherMinY, double otherMinZ, double otherMaxX, double otherMaxY, double otherMaxZ) {
        //CHeck super first, as that's the encompassing box.
        if (super.intersects(otherMinX, otherMinY, otherMinZ, otherMaxX, otherMaxY, otherMaxZ)) {
            for (BoundingBox testBox : getBoxes()) {
                if (otherMaxX > testBox.globalCenter.x - testBox.widthRadius && otherMinX < testBox.globalCenter.x + testBox.widthRadius && otherMaxY > testBox.globalCenter.y - testBox.heightRadius && otherMinY < testBox.globalCenter.y + testBox.heightRadius && otherMaxZ > testBox.globalCenter.z - testBox.depthRadius && otherMinZ < testBox.globalCenter.z + testBox.depthRadius) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean contains(Vec3 vec) {
        return this.intersects(vec.x, vec.y, vec.z, vec.x, vec.y, vec.z);
    }

    @Override
    public Optional<Vec3> clip(Vec3 vecA, Vec3 vecB) {
        //Check all the bounding boxes for collision to see if we hit one of them.
        Point3D start = new Point3D(vecA.x, vecA.y, vecA.z);
        Point3D end = new Point3D(vecB.x, vecB.y, vecB.z);
        BoundingBoxHitResult intersection = null;
        for (BoundingBox testBox : getBoxes()) {
            BoundingBoxHitResult testIntersection = testBox.getIntersection(start, end);
            if (testIntersection != null) {
                if (intersection == null || start.isFirstCloserThanSecond(testIntersection.position, intersection.position)) {
                    intersection = testIntersection;
                }
            }
        }
        if (intersection != null) {
            return Optional.of(new Vec3(intersection.position.x, intersection.position.y, intersection.position.z));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Helper method that's akin to MC's older collision methods in 1.12.2, just here rather than
     * in a VoxelShape.
     */
    public Vec3 getCollision(Vec3 movement, AABB testBox) {
        double x = movement.x != 0 ? calculateXOffset(testBox, movement.x) : 0;
        double y = movement.y != 0 ? calculateYOffset(testBox, movement.y) : 0;
        double z = movement.z != 0 ? calculateZOffset(testBox, movement.z) : 0;
        return new Vec3(x, y, z);
    }

    private double calculateXOffset(AABB box, double offset) {
        for (BoundingBox testBox : getBoxes()) {
            if (box.maxY > testBox.globalCenter.y - testBox.heightRadius && box.minY < testBox.globalCenter.y + testBox.heightRadius && box.maxZ > testBox.globalCenter.z - testBox.depthRadius && box.minZ < testBox.globalCenter.z + testBox.depthRadius) {
                if (offset > 0.0D) {
                    //Positive offset, box.maxX <= this.minX.
                    double collisionDepth = testBox.globalCenter.x - testBox.widthRadius - box.maxX;
                    if (collisionDepth >= 0 && collisionDepth < offset) {
                        offset = collisionDepth;
                    }
                } else if (offset < 0.0D) {
                    //Negative offset, box.minX >= this.maxX.
                    double collisionDepth = testBox.globalCenter.x + testBox.widthRadius - box.minX;
                    if (collisionDepth <= 0 && collisionDepth > offset) {
                        offset = collisionDepth;
                    }
                }
            }
        }
        return offset;
    }
    
    private double calculateYOffset(AABB box, double offset) {
        for (BoundingBox testBox : getBoxes()) {
            if (box.maxX > testBox.globalCenter.x - testBox.widthRadius && box.minX < testBox.globalCenter.x + testBox.widthRadius && box.maxZ > testBox.globalCenter.z - testBox.depthRadius && box.minZ < testBox.globalCenter.z + testBox.depthRadius) {
                if (offset > 0.0D) {
                    //Positive offset, box.maxX <= this.minX.
                    double collisionDepth = testBox.globalCenter.y - testBox.heightRadius - box.maxY;
                    if (collisionDepth >= 0 && collisionDepth < offset) {
                        offset = collisionDepth;
                    }
                } else if (offset < 0.0D) {
                    //Negative offset, box.minX >= this.maxX.
                    double collisionDepth = testBox.globalCenter.y + testBox.heightRadius - box.minY;
                    if (collisionDepth <= 0 && collisionDepth > offset) {
                        offset = collisionDepth;
                    }
                }
            }
        }
        return offset;
    }
    
    private double calculateZOffset(AABB box, double offset) {
        for (BoundingBox testBox : getBoxes()) {
            if (box.maxX > testBox.globalCenter.x - testBox.widthRadius && box.minX < testBox.globalCenter.x + testBox.widthRadius && box.maxY > testBox.globalCenter.y - testBox.heightRadius && box.minY < testBox.globalCenter.y + testBox.heightRadius) {
                if (offset > 0.0D) {
                    //Positive offset, box.maxX <= this.minX.
                    double collisionDepth = testBox.globalCenter.z - testBox.depthRadius - box.maxZ;
                    if (collisionDepth >= 0 && collisionDepth < offset) {
                        offset = collisionDepth;
                    }
                } else if (offset < 0.0D) {
                    //Negative offset, box.minX >= this.maxX.
                    double collisionDepth = testBox.globalCenter.z + testBox.depthRadius - box.minZ;
                    if (collisionDepth <= 0 && collisionDepth > offset) {
                        offset = collisionDepth;
                    }
                }
            }
        }
        return offset;
    }
}
