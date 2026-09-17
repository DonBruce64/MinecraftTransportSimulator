package minecrafttransportsimulator.blocks.tileentities.instances;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TextureOverlaySwitchbox;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.instances.BlockCollision;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.RoadClickData;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLane;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLaneConnection;
import minecrafttransportsimulator.items.instances.ItemRoadComponent;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent.JSONLaneSector;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent.JSONRoadCollisionArea;
import minecrafttransportsimulator.jsondefs.JSONTextureOverlay;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketTileEntityRoadCollisionUpdate;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.rendering.AModelParser;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableModelObject;
import minecrafttransportsimulator.rendering.RenderableVertices;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.LanguageSystem;

/**
 * Road tile entity.  Contains the definition so we know how
 * to render this in the TESR call, as well as stores the "fake"
 * block positions of blocks that make up this road segment.
 * Optionally stores the next and prior road in the segment, if one exists.
 * This can be used to create smooth road segments.
 * Note that while the number of connection points is finite, there may be multiple
 * curves connected to and from each point.  However, each road segment can only have
 * one curve from any one lane connection.  Therefore, to make junctions you will have
 * multiple segments connected to the same point, but each of those segments will only be
 * connected to that junction and nowhere else.
 * Also note that all points and positions are relative.  This is done for simpler
 * math operations.  Should the world-based point be needed, simply offset any relative
 * point by the position of this TE.
 *
 * @author don_bruce
 */
public class TileEntityRoad extends ATileEntityBase<JSONRoadComponent> {
    //Static variables based on core definition.
    public BezierCurve dynamicCurve;
    public final List<RoadLane> lanes = new ArrayList<>();

    //Dynamic variables based on states.
    private boolean isActive;
    public final Map<RoadComponent, ItemRoadComponent> components = new HashMap<>();
    public final Map<RoadComponent, RenderableData> componentRenderables = new HashMap<>();
    private final Map<RoadComponent, RenderableVertices> componentTextureOverlayVertices = new HashMap<>();
    private final Map<RoadComponent, List<RoadTextureOverlayRenderable>> componentTextureOverlayRenderables = new HashMap<>();
    public final List<RenderableData> devRenderables = new ArrayList<>();
    public final List<BoundingBox> blockingBoundingBoxes = new ArrayList<>();
    public final List<Point3D> collisionBlockOffsets;
    public final List<Point3D> collidingBlockOffsets;

    public TileEntityRoad(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, ItemRoadComponent item, IWrapperNBT data) {
        super(world, position, placingPlayer, item, data);

        //Set the bounding box.
        this.boundingBox.heightRadius = definition.road.collisionHeight / 16D / 2D;
        this.boundingBox.globalCenter.y += boundingBox.heightRadius;

        //Set core road component.
        components.put(definition.road.type, (ItemRoadComponent) getStack().getItem());

        //Load from data.
        if (data != null) {
            //Get the active state.
            this.isActive = data.getBoolean("isActive");

            //Load components back in.  Our core component will always be our definition.
            for (RoadComponent componentType : RoadComponent.values()) {
                String packID = data.getString("packID" + componentType.name());
                if (!packID.isEmpty()) {
                    String systemName = data.getString("systemName" + componentType.name());
                    ItemRoadComponent newComponent = PackParser.getItem(packID, systemName);
                    components.put(componentType, newComponent);
                }
            }

            //Load curve and lane data.  We may not have this yet if we're in the process of creating a new road.
            Point3D startingOffset = data.getPoint3d("startingOffset");
            Point3D endingOffset = data.getPoint3d("endingOffset");
            if (!endingOffset.isZero()) {
                this.dynamicCurve = new BezierCurve(startingOffset, endingOffset, new RotationMatrix().setToAngles(data.getPoint3d("startingAngles")), new RotationMatrix().setToAngles(data.getPoint3d("endingAngles")));
            }

            //If we have points for collision due to use creating collision blocks, load them now.
            this.collisionBlockOffsets = data.getPoint3dsCompact("collisionBlockOffsets");
            this.collidingBlockOffsets = data.getPoint3dsCompact("collidingBlockOffsets");
        } else {
            this.collisionBlockOffsets = new ArrayList<>();
            this.collidingBlockOffsets = new ArrayList<>();
        }

        //Don't generate lanes for inactive roads.
        if (isActive()) {
            generateLanes(data);
        }
    }

    @Override
    public double getPlacementRotation(IWrapperPlayer player) {
        if (!definition.road.type.equals(RoadComponent.CORE_DYNAMIC)) {
            int clampAngle = getRotationIncrement();
            //Normally blocks are placed facing us.  For roads though, we want us to have the angles the player is facing.
            return Math.round((player.getYaw()) / clampAngle) * clampAngle % 360;
        } else {
            return 0;
        }
    }

    /**
     * Returns true if this road is active.  Active roads may be considered
     * to have all their collision blocks and no blocking blocks.
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Sets this road as active or inactive.  This happens after collision creation for
     * setting it as active, or prior to destruction for inactive.  This method should
     * handle any logic that needs to happen once the block is active and valid.
     */
    public void setActive(boolean active) {
        isActive = active;
        if (active) {
            generateLanes(null);
        }
    }

    @Override
    public void remove() {
        super.remove();
        for (RenderableData object : componentRenderables.values()) {
            object.destroy();
        }
        destroyTextureOverlayRenderables();
    }

    @Override
    public void destroy(BoundingBox box) {
        super.destroy(box);
        if (isActive) {
            //Set the TE to inactive and remove all road connections.
            setActive(false);
            for (RoadLane lane : lanes) {
                lane.removeConnections();
            }

            //Now remove all collision blocks.
            for (Point3D blockOffset : collisionBlockOffsets) {
                Point3D blockLocation = position.copy().add(blockOffset);
                //Check to make sure we don't destroy non-road blocks.
                //This is required in case our TE is corrupt or someone messes with it.
                if (world.getBlock(blockLocation) instanceof BlockCollision) {
                    world.destroyBlock(blockLocation, true);
                }
            }

            //Finally, spawn component drops.
            for (RoadComponent componentType : RoadComponent.values()) {
                if (componentType != definition.road.type && components.containsKey(componentType)) {
                    world.spawnItemStack(components.get(componentType).getNewStack(null), position, null);
                }
            }
        }
    }

    @Override
    public boolean interact(IWrapperPlayer player) {
        //Check if we aren't active.  If not, try to spawn collision again.
        if (!isActive) {
            spawnCollisionBlocks(player);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Helper method to get information on what was clicked.
     * Takes the player's rotation into account, as well as the block they clicked.
     */
    public RoadClickData getClickData(Point3D blockClicked, boolean curveStart) {
        Point3D blockOffsetClicked = blockClicked.copy().add(0.5, 0, 0.5).subtract(position);
        boolean clickedStart = blockOffsetClicked.isZero() || collisionBlockOffsets.indexOf(blockOffsetClicked) < collisionBlockOffsets.size() / 2;
        JSONLaneSector closestSector = null;
        if (!definition.road.type.equals(RoadComponent.CORE_DYNAMIC)) {
            double closestSectorDistance = Double.MAX_VALUE;
            for (RoadLane lane : lanes) {
                //Only check start points.  End points are for other sectors.
                double distanceToSectorStart = lane.curves.get(0).startPos.distanceTo(blockClicked);
                if (distanceToSectorStart < closestSectorDistance) {
                    closestSectorDistance = distanceToSectorStart;
                    closestSector = definition.road.sectors.get(lane.sectorNumber);
                }
            }
        }
        return new RoadClickData(this, closestSector, clickedStart, curveStart);
    }

    /**
     * Helper method to populate the lanes for this road.  This depends on if we are
     * a static or dynamic road.  Data is passed-in, but may be null if we're generating
     * lanes for the first time.
     */
    public void generateLanes(IWrapperNBT data) {
        int totalLanes = 0;
        if (definition.road.type.equals(RoadComponent.CORE_DYNAMIC)) {
            for (int i = 0; i < definition.road.laneOffsets.length; ++i) {
                lanes.add(new RoadLane(this, 0, i, 0, data != null ? data.getData("lane" + totalLanes) : null));
                ++totalLanes;
            }
        } else {
            for (int i = 0; i < definition.road.sectors.size(); ++i) {
                for (int j = 0; j < definition.road.sectors.get(i).lanes.size(); ++j) {
                    lanes.add(new RoadLane(this, i, totalLanes, j, data != null ? data.getData("lane" + totalLanes) : null));
                    ++totalLanes;
                }
            }
        }
    }

    /**
     * Helper method to spawn collision boxes for this road.  Returns true and makes
     * this road non-holographic if the boxes could be spawned.  False if there are
     * blocking blocks.  OP and creative-mode players override blocking block checks.
     * Road width is considered to extend to the left and right border, minus 1/2 a block.
     */
    protected Map<Point3D, Integer> generateCollisionPoints() {
        collisionBlockOffsets.clear();
        collidingBlockOffsets.clear();
        Map<Point3D, Integer> collisionHeightMap = new HashMap<>();
        if (definition.road.type.equals(RoadComponent.CORE_DYNAMIC)) {
            //Get all the points that make up our collision points for our dynamic curve.
            //If we find any colliding points, note them.
            Point3D testOffset = new Point3D();
            float segmentDelta = (float) (definition.road.roadWidth / (Math.floor(definition.road.roadWidth) + 1));
            for (float f = 0; f < dynamicCurve.pathLength; f += 0.1) {
                for (float offset = 0; offset <= definition.road.roadWidth; offset += segmentDelta) {
                    testOffset.set(offset, 0, 0).rotate(dynamicCurve.getRotationAt(f));
                    dynamicCurve.offsetPointByPositionAt(testOffset, f);
                    testOffset.subtract(position).add(0, definition.road.collisionHeight / 16F, 0);
                    Point3D testPoint = new Point3D((int) testOffset.x, (int) Math.floor(testOffset.y), (int) testOffset.z);

                    //If we don't have a block in this position, check if we need one.
                    if (!testPoint.isZero() && !collisionBlockOffsets.contains(testPoint) && !collidingBlockOffsets.contains(testPoint)) {
                        //Offset the point to the global cordinate space, get the block, and offset back.
                        testPoint.add(position);
                        if (world.isAir(testPoint)) {
                            //Need a collision box here.
                            testPoint.subtract(position);
                            int collisionBoxIndex = (int) ((testOffset.y - testPoint.y) * 16);
                            collisionBlockOffsets.add(testPoint);

                            collisionHeightMap.put(testPoint, collisionBoxIndex);
                        } else if (!(world.getBlock(testPoint) instanceof BlockCollision)) {
                            //Some block is blocking us that's not part of a road.  Flag it.
                            testPoint.subtract(position);
                            collidingBlockOffsets.add(testPoint);
                        }
                    }
                }
            }
        } else {
            //Do static block additions for static component.
            for (JSONRoadCollisionArea collisionArea : definition.road.collisionAreas) {
                for (double x = collisionArea.firstCorner.x + 0.01; x < collisionArea.secondCorner.x + 0.5; x += 0.5) {
                    for (double z = collisionArea.firstCorner.z + 0.01; z < collisionArea.secondCorner.z + 0.5; z += 0.5) {
                        Point3D testPoint = new Point3D(x, collisionArea.firstCorner.y, z).rotate(orientation);
                        testPoint.x = (int) testPoint.x;
                        testPoint.z = (int) testPoint.z;

                        if (!testPoint.isZero() && !collisionBlockOffsets.contains(testPoint) && !collidingBlockOffsets.contains(testPoint)) {
                            //Offset the point to the global cordinate space, get the block, and offset back.
                            testPoint.add(position);
                            if (world.isAir(testPoint)) {
                                //Need a collision box here.
                                testPoint.subtract(position);
                                collisionBlockOffsets.add(testPoint);
                                collisionHeightMap.put(testPoint, collisionArea.collisionHeight == 16 ? 15 : collisionArea.collisionHeight);
                            } else if (!(world.getBlock(testPoint) instanceof BlockCollision)) {
                                //Some block is blocking us that's not part of a road.  Flag it.
                                testPoint.subtract(position);
                                collidingBlockOffsets.add(testPoint);
                            }
                        }
                    }
                }
            }
        }
        return collisionHeightMap;
    }

    /**
     * Method to spawn collision boxes for this road structure.
     * Returns true and makes this TE active if all the boxes could be spawned.
     * False if there are blocking blocks.  OP and creative-mode players override blocking block checks.
     */
    public boolean spawnCollisionBlocks(IWrapperPlayer player) {
        Map<Point3D, Integer> collisionHeightMap = generateCollisionPoints();
        if (collidingBlockOffsets.isEmpty() || (player.isCreative() && player.isOP())) {
            for (Point3D offset : collisionBlockOffsets) {
                world.setBlock(BlockCollision.blockInstances.get(collisionHeightMap.get(offset)), offset.copy().add(position), null, Axis.UP);
            }
            collidingBlockOffsets.clear();
            setActive(true);
            InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityRoadCollisionUpdate(this));
            return true;
        } else {
            collisionBlockOffsets.clear();
            player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_ROAD_BLOCKINGBLOCKS));
            InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityRoadCollisionUpdate(this));
            return false;
        }
    }

    @Override
    protected void renderModel(TransformationMatrix transform, boolean blendingEnabled, float partialTicks) {
        //Don't call super, we don't want to render the normal way.
        if (isActive() ^ blendingEnabled) {
            //Render road components.
            for (RoadComponent component : components.keySet()) {
                if (!componentRenderables.containsKey(component)) {
                    ItemRoadComponent componentItem = components.get(component);
                    switch (component) {
                        case CORE_STATIC: {
                            List<RenderableVertices> parsedModel = AModelParser.parseModel(componentItem.definition.getModelLocation(componentItem.subDefinition), true);
                            int totalVertices = 0;
                            for (RenderableVertices object : parsedModel) {
                                totalVertices += object.vertices.capacity();
                            }

                            //Cache the model now that we know how big it is.
                            FloatBuffer totalModel = FloatBuffer.allocate(totalVertices);
                            boolean hasTextureOverlays = componentItem.definition.rendering.textureOverlays != null && !componentItem.definition.rendering.textureOverlays.isEmpty();
                            FloatBuffer totalOverlayModel = hasTextureOverlays ? FloatBuffer.allocate(totalVertices) : null;
                            for (RenderableVertices object : parsedModel) {
                                FloatBuffer modelVertices = object.vertices.duplicate();
                                modelVertices.rewind();
                                totalModel.put(modelVertices);
                                if (totalOverlayModel != null) {
                                    FloatBuffer overlayVertices = RenderableModelObject.getTextureOverlayVertexObject(object).vertices.duplicate();
                                    overlayVertices.rewind();
                                    totalOverlayModel.put(overlayVertices);
                                }
                            }
                            totalModel.flip();
                            RenderableData renderable = new RenderableData(new RenderableVertices(component.name(), totalModel, true), componentItem.definition.getTextureLocation(componentItem.subDefinition, 0));
                            componentRenderables.put(component, renderable);
                            if (totalOverlayModel != null) {
                                totalOverlayModel.flip();
                                componentTextureOverlayVertices.put(component, new RenderableVertices(component.name() + "_OVERLAYS", totalOverlayModel, true));
                            }
                            break;
                        }
                        case CORE_DYNAMIC: {
                            //Get model and convert to a single buffer of vertices.
                            List<RenderableVertices> parsedModel = AModelParser.parseModel(componentItem.definition.getModelLocation(componentItem.subDefinition), true);
                            int totalVertices = 0;
                            for (RenderableVertices object : parsedModel) {
                                totalVertices += object.vertices.capacity();
                            }
                            FloatBuffer parsedVertices = FloatBuffer.allocate(totalVertices);
                            boolean hasTextureOverlays = componentItem.definition.rendering.textureOverlays != null && !componentItem.definition.rendering.textureOverlays.isEmpty();
                            FloatBuffer parsedOverlayVertices = hasTextureOverlays ? FloatBuffer.allocate(totalVertices) : null;
                            for (RenderableVertices object : parsedModel) {
                                FloatBuffer modelVertices = object.vertices.duplicate();
                                modelVertices.rewind();
                                parsedVertices.put(modelVertices);
                                if (parsedOverlayVertices != null) {
                                    FloatBuffer overlayVertices = RenderableModelObject.getTextureOverlayVertexObject(object).vertices.duplicate();
                                    overlayVertices.rewind();
                                    parsedOverlayVertices.put(overlayVertices);
                                }
                            }
                            parsedVertices.flip();
                            if (parsedOverlayVertices != null) {
                                parsedOverlayVertices.flip();
                            }

                            //Offset vertices to be corner-aligned, as that's how our curve aligns.
                            for (int i = 0; i < parsedVertices.capacity(); i += 8) {
                                parsedVertices.put(i + 5, (float) (parsedVertices.get(i + 5) - definition.road.cornerOffset.x));
                                parsedVertices.put(i + 7, (float) (parsedVertices.get(i + 7) - definition.road.cornerOffset.z));
                                if (parsedOverlayVertices != null) {
                                    parsedOverlayVertices.put(i + 5, (float) (parsedOverlayVertices.get(i + 5) - definition.road.cornerOffset.x));
                                    parsedOverlayVertices.put(i + 7, (float) (parsedOverlayVertices.get(i + 7) - definition.road.cornerOffset.z));
                                }
                            }

                            //Core components need to be transformed to wedges.
                            Point3D position = new Point3D();
                            RotationMatrix rotation;
                            Point3D priorPosition = new Point3D();
                            RotationMatrix priorRotation;
                            Point3D testPoint1 = new Point3D();
                            Point3D testPoint2 = new Point3D();
                            Point3D vertexOffsetPriorLine = new Point3D();
                            Point3D vertexOffsetCurrentLine = new Point3D();
                            Point3D segmentVector = new Point3D();
                            Point3D renderedVertex = new Point3D();
                            float indexDelta = (float) (dynamicCurve.pathLength / Math.floor(dynamicCurve.pathLength / definition.road.segmentLength));
                            boolean finalSegment = false;
                            float priorIndex = 0;
                            float currentIndex = 0;
                            List<float[]> segmentVertices = new ArrayList<>();
                            List<float[]> segmentOverlayVertices = parsedOverlayVertices != null ? new ArrayList<>() : null;
                            while (!finalSegment) {
                                //If we are at the last index, do special logic to get the very end point.
                                //We check here in case FPEs have accumulated and we won't end on the exact end segment.
                                //Otherwise, increment normally.
                                if (currentIndex != dynamicCurve.pathLength && currentIndex + indexDelta * 1.25 > dynamicCurve.pathLength) {
                                    currentIndex = dynamicCurve.pathLength;
                                    finalSegment = true;
                                } else {
                                    currentIndex += indexDelta;
                                }

                                //Get current and prior curve position and rotation.
                                //From this, we know how much to stretch the model to that point's rendering area.
                                dynamicCurve.setPointToPositionAt(priorPosition, priorIndex);
                                priorRotation = dynamicCurve.getRotationAt(priorIndex);
                                priorPosition.subtract(dynamicCurve.startPos);
                                dynamicCurve.setPointToPositionAt(position, currentIndex);
                                rotation = dynamicCurve.getRotationAt(currentIndex);
                                position.subtract(dynamicCurve.startPos);

                                //If we are a really sharp curve, we might have inverted our model at the inner corner.
                                //Check for this, and if we have done so, skip this segment.
                                //If we detect this in the last 3 segments, skip right to the end.
                                //This prevents a missing end segment due to collision.
                                testPoint1.set(definition.road.roadWidth + definition.road.cornerOffset.x, 0, 0);
                                testPoint1.rotate(priorRotation).add(priorPosition);
                                testPoint2.set(definition.road.roadWidth + definition.road.cornerOffset.x, 0, 0);
                                testPoint2.rotate(rotation).add(position);
                                if (currentIndex != dynamicCurve.pathLength && ((position.x - priorPosition.x) * (testPoint2.x - testPoint1.x) < 0 || (position.z - priorPosition.z) * (testPoint2.z - testPoint1.z) < 0)) {
                                    if (currentIndex + 3 * indexDelta > dynamicCurve.pathLength) {
                                        currentIndex = dynamicCurve.pathLength - indexDelta;
                                    }
                                    continue;
                                }

                                //Depending on the vertex position in the model, transform it to match with the offset rotation.
                                //This depends on how far the vertex is from the origin of the model, and how big the delta is.
                                //For all points, their magnitude depends on how far away they are on the Z-axis.
                                for (int i = 0; i < parsedVertices.capacity(); i += 8) {
                                    float[] convertedVertexData = new float[8];
                                    float[] convertedOverlayVertexData = parsedOverlayVertices != null ? new float[8] : null;

                                    //Add the normals and UVs first.  These won't change.
                                    parsedVertices.get(convertedVertexData, 0, 5);
                                    if (parsedOverlayVertices != null) {
                                        parsedOverlayVertices.get(convertedOverlayVertexData, 0, 5);
                                        parsedOverlayVertices.position(parsedOverlayVertices.position() + 3);
                                    }

                                    //Now convert the XYZ points.
                                    float x = parsedVertices.get();
                                    float y = parsedVertices.get();
                                    float z = parsedVertices.get();
                                    vertexOffsetPriorLine.set(x, y, 0);
                                    vertexOffsetPriorLine.rotate(priorRotation).add(priorPosition);
                                    vertexOffsetCurrentLine.set(x, y, 0);
                                    vertexOffsetCurrentLine.rotate(rotation).add(position);

                                    segmentVector.set(vertexOffsetCurrentLine).subtract(vertexOffsetPriorLine).scale(z / definition.road.segmentLength);
                                    renderedVertex.set(vertexOffsetPriorLine).add(segmentVector);

                                    convertedVertexData[5] = (float) renderedVertex.x;
                                    convertedVertexData[6] = (float) renderedVertex.y;
                                    convertedVertexData[7] = (float) renderedVertex.z;

                                    //Add transformed vertices to the segment.
                                    segmentVertices.add(convertedVertexData);
                                    if (convertedOverlayVertexData != null) {
                                        convertedOverlayVertexData[5] = convertedVertexData[5];
                                        convertedOverlayVertexData[6] = convertedVertexData[6];
                                        convertedOverlayVertexData[7] = convertedVertexData[7];
                                        segmentOverlayVertices.add(convertedOverlayVertexData);
                                    }
                                }
                                //Rewind for next segment.
                                parsedVertices.rewind();
                                if (parsedOverlayVertices != null) {
                                    parsedOverlayVertices.rewind();
                                }

                                //Set the last index.
                                priorIndex = currentIndex;
                            }

                            //Cache and compile the segments.
                            FloatBuffer convertedVertices = FloatBuffer.allocate(segmentVertices.size() * 8);
                            for (float[] segmentVertex : segmentVertices) {
                                convertedVertices.put(segmentVertex);
                            }
                            convertedVertices.flip();
                            RenderableData renderable = new RenderableData(new RenderableVertices(component.name(), convertedVertices, true, AModelParser.isMissingModel(parsedModel)), componentItem.definition.getTextureLocation(componentItem.subDefinition, 0));
                            componentRenderables.put(component, renderable);
                            if (segmentOverlayVertices != null) {
                                FloatBuffer convertedOverlayVertices = FloatBuffer.allocate(segmentOverlayVertices.size() * 8);
                                for (float[] segmentVertex : segmentOverlayVertices) {
                                    convertedOverlayVertices.put(segmentVertex);
                                }
                                convertedOverlayVertices.flip();
                                componentTextureOverlayVertices.put(component, new RenderableVertices(component.name() + "_OVERLAYS", convertedOverlayVertices, true, AModelParser.isMissingModel(parsedModel)));
                            }
                            break;
                        }
                    }
                }
                if (!componentTextureOverlayRenderables.containsKey(component)) {
                    RenderableVertices overlayVertices = componentTextureOverlayVertices.containsKey(component) ? componentTextureOverlayVertices.get(component) : componentRenderables.get(component).vertexObject;
                    initializeTextureOverlayRenderables(component, components.get(component), overlayVertices);
                }
                RenderableData object = componentRenderables.get(component);
                if (isActive()) {
                    object.setColor(ColorRGB.WHITE);
                    object.setAlpha(1.0F);
                } else {
                    object.setColor(ColorRGB.GREEN);
                    object.setAlpha(0.5F);
                }
                if (dynamicCurve != null) {
                    object.transform.setTranslation(dynamicCurve.startPos.copy().subtract(position));
                    object.transform.multiply(transform);
                } else {
                    object.transform.set(transform);
                }
                object.setLightValue(worldLightValue);
                object.render();
            }

            //If we are inactive render the blocking blocks and the main block.
            if (!isActive()) {
                if (blockingBoundingBoxes.isEmpty()) {
                    blockingBoundingBoxes.add(new BoundingBox(new Point3D(0, 0.75, 0), 0.15, 0.75, 0.15));
                    for (Point3D location : collidingBlockOffsets) {
                        blockingBoundingBoxes.add(new BoundingBox(location.copy().add(0, 0.55, 0), 0.55, 0.55, 0.55));
                    }
                }
                boolean firstBox = true;
                for (BoundingBox blockingBox : blockingBoundingBoxes) {
                    if (firstBox) {
                        blockingBox.renderHolographic(transform, blockingBox.globalCenter, ColorRGB.BLUE);
                        firstBox = false;
                    } else {
                        blockingBox.renderHolographic(transform, blockingBox.globalCenter, ColorRGB.RED);
                    }
                }
            }
        }

        //Roads use a special combined-mesh renderer and therefore need to submit their texture
        //layers explicitly rather than through AEntityD_Definable's normal model-object path.
        if (blendingEnabled) {
            renderTextureOverlays(transform, partialTicks);
        }
    }

    @Override
    public void resetModelsAndAnimations() {
        super.resetModelsAndAnimations();
        //This override may be invoked from the superclass constructor before these fields initialize.
        if (componentRenderables != null) {
            componentRenderables.values().forEach(RenderableData::destroy);
            componentRenderables.clear();
        }
        if (componentTextureOverlayRenderables != null) {
            destroyTextureOverlayRenderables();
            componentTextureOverlayRenderables.clear();
        }
        if (componentTextureOverlayVertices != null) {
            componentTextureOverlayVertices.clear();
        }
    }

    private void initializeTextureOverlayRenderables(RoadComponent component, ItemRoadComponent componentItem, RenderableVertices sourceVertices) {
        List<RoadTextureOverlayRenderable> overlays = new ArrayList<>();
        if (componentItem.definition.rendering.textureOverlays != null) {
            int overlayCount = componentItem.definition.rendering.textureOverlays.size();
            RenderableVertices overlayVertices = RenderableModelObject.getTextureOverlayVertexObject(sourceVertices);
            for (int overlayIndex = 0; overlayIndex < overlayCount; ++overlayIndex) {
                JSONTextureOverlay overlayDefinition = componentItem.definition.rendering.textureOverlays.get(overlayIndex);
                String texture = RenderableModelObject.resolveOverlayTexture(componentItem.definition, overlayDefinition.texture);
                RenderableData overlayRenderable = new RenderableData(overlayVertices, texture);
                overlayRenderable.setTransucentOverride();
                overlayRenderable.setTextureClampToTransparent();
                overlayRenderable.setRenderingOrder(overlayCount - overlayIndex);
                if (overlayDefinition.isBright) {
                    overlayRenderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
                }
                int[] dimensions = RenderableModelObject.getTextureDimensions(texture);
                String normalTexture = componentItem.definition.getTextureLocation(componentItem.subDefinition, 0);
                if (normalTexture != null && !RenderableData.GLOBAL_TEXTURE_NAME.equals(normalTexture)) {
                    int[] normalTextureDimensions = RenderableModelObject.getTextureDimensions(normalTexture);
                    overlayRenderable.setTextureScale(normalTextureDimensions[0] / (float) dimensions[0], normalTextureDimensions[1] / (float) dimensions[1]);
                }
                overlays.add(new RoadTextureOverlayRenderable(overlayRenderable, dimensions[0], dimensions[1]));
                if (overlayDefinition.animations != null && !overlayDefinition.animations.isEmpty() && !textureOverlaySwitchboxes.containsKey(overlayDefinition)) {
                    textureOverlaySwitchboxes.put(overlayDefinition, new TextureOverlaySwitchbox(this, overlayDefinition));
                }
            }
        }
        componentTextureOverlayRenderables.put(component, overlays);
    }

    private void renderTextureOverlays(TransformationMatrix transform, float partialTicks) {
        for (RoadComponent component : components.keySet()) {
            ItemRoadComponent componentItem = components.get(component);
            List<JSONTextureOverlay> overlayDefinitions = componentItem.definition.rendering.textureOverlays;
            List<RoadTextureOverlayRenderable> overlays = componentTextureOverlayRenderables.get(component);
            if (overlayDefinitions == null || overlays == null) {
                continue;
            }

            for (int overlayIndex = overlayDefinitions.size() - 1; overlayIndex >= 0; --overlayIndex) {
                JSONTextureOverlay overlayDefinition = overlayDefinitions.get(overlayIndex);
                TextureOverlaySwitchbox switchbox = textureOverlaySwitchboxes.get(overlayDefinition);
                if (switchbox != null) {
                    boolean visible = switchbox.runSwitchbox(partialTicks, false);
                    if (!overlayDefinition.blendedAnimations && !visible) {
                        continue;
                    }
                }

                float alpha = overlayDefinition.blendedAnimations && switchbox != null ? switchbox.getVisibilityAlpha() : 1.0F;
                if (!isActive()) {
                    alpha *= 0.5F;
                }
                if (alpha <= 0) {
                    continue;
                }

                RoadTextureOverlayRenderable overlay = overlays.get(overlayIndex);
                double translationX = switchbox != null ? switchbox.translation.x : 0.0D;
                double translationY = switchbox != null ? switchbox.translation.y : 0.0D;
                overlay.renderable.setTextureOffset((float) (-(overlayDefinition.centerPoint.x + translationX) / overlay.textureWidth), (float) (-(overlayDefinition.centerPoint.y + translationY) / overlay.textureHeight));
                overlay.renderable.setColor(isActive() ? ColorRGB.WHITE : ColorRGB.GREEN);
                overlay.renderable.setAlpha(alpha);
                overlay.renderable.setLightValue(worldLightValue);
                if (dynamicCurve != null) {
                    overlay.renderable.transform.setTranslation(dynamicCurve.startPos.copy().subtract(position));
                    overlay.renderable.transform.multiply(transform);
                } else {
                    overlay.renderable.transform.set(transform);
                }
                overlay.renderable.render();
            }
        }
    }

    private void destroyTextureOverlayRenderables() {
        for (List<RoadTextureOverlayRenderable> overlays : componentTextureOverlayRenderables.values()) {
            overlays.forEach(overlay -> overlay.renderable.destroy());
        }
    }

    @Override
    public void renderBoundingBoxes(TransformationMatrix transform) {
        super.renderBoundingBoxes(transform);
        //Render all collision boxes too.
        for (Point3D blockOffset : collisionBlockOffsets) {
            ABlockBase block = world.getBlock(position.copy().add(blockOffset));
            if (block instanceof BlockCollision) {
                //Need to offset by -0.5 as the collision box bounds is centered, but the offset isn't.
                BoundingBox blockBounds = ((BlockCollision) block).blockBounds;
                blockBounds.renderWireframe(this, transform, blockOffset.copy().add(-0.5, 0, -0.5), null);
            }
        }
        //If we are in devMode, render road bounds and colliding boxes.
        if (ConfigSystem.settings.general.devMode.value) {
            if (devRenderables.isEmpty()) {
                generateDevElements(this);
            }
            Point3D invertedPosition = position.copy().invert();
            for (RenderableData renderable : devRenderables) {
                renderable.transform.setTranslation(invertedPosition).multiply(transform);
                renderable.render();
            }
        }
    }

    private static void generateDevElements(TileEntityRoad road) {
        //Create the information hashes.
        Point3D point1 = new Point3D();
        Point3D point2 = new Point3D();
        RotationMatrix rotation;
        RenderableData renderable;
        if (road.dynamicCurve != null) {
            //Render actual curve.
            int numberLines = (int) (road.dynamicCurve.pathLength * 10);
            RenderableVertices vertexObject = new RenderableVertices(numberLines);
            for (float f = 0; --numberLines > 0; f += 0.1) {
                road.dynamicCurve.setPointToPositionAt(point1, f);
                rotation = road.dynamicCurve.getRotationAt(f);
                point2.set(0, 1, 0).rotate(rotation).add(point1);
                vertexObject.addLine(point1, point2);
            }
            renderable = new RenderableData(vertexObject);
            renderable.setColor(ColorRGB.GREEN);
            road.devRenderables.add(renderable);

            //Render the border bounds.
            numberLines = (int) (road.dynamicCurve.pathLength * 10);
            vertexObject = new RenderableVertices(numberLines);
            for (float f = 0; --numberLines > 0; f += 0.1) {
                road.dynamicCurve.setPointToPositionAt(point1, f);
                rotation = road.dynamicCurve.getRotationAt(f);
                point1.set(road.definition.road.roadWidth, 0, 0);

                point2.set(point1).add(0, 1, 0).rotate(rotation);
                point1.rotate(rotation);

                road.dynamicCurve.offsetPointByPositionAt(point1, f);
                road.dynamicCurve.offsetPointByPositionAt(point2, f);
                vertexObject.addLine(point1, point2);
            }
            renderable = new RenderableData(vertexObject);
            renderable.setColor(ColorRGB.CYAN);
            road.devRenderables.add(renderable);
        }

        //Now render the lane curve segments.
        for (RoadLane lane : road.lanes) {
            for (BezierCurve laneCurve : lane.curves) {
                //Render the curve bearing indicator
                RenderableVertices vertexObject = new RenderableVertices(2);

                //Render vertical segment.
                rotation = laneCurve.getRotationAt(0);
                laneCurve.setPointToPositionAt(point1, 0);
                point2.set(0, 3, 0).rotate(rotation).add(point1);
                vertexObject.addLine(point1, point2);

                //Render 1-unit path delta.
                point1.set(point2);
                rotation = laneCurve.getRotationAt(1);
                point1.set(0, 3, 0).rotate(rotation);
                laneCurve.offsetPointByPositionAt(point1, 1);
                vertexObject.addLine(point1, point2);

                renderable = new RenderableData(vertexObject);
                renderable.setColor(ColorRGB.RED);
                road.devRenderables.add(renderable);

                //Render all the points on the curve.
                int numberLines = (int) (laneCurve.pathLength * 10);
                vertexObject = new RenderableVertices(numberLines);
                for (float f = 0; --numberLines > 0; f += 0.1) {
                    laneCurve.setPointToPositionAt(point1, f);
                    rotation = laneCurve.getRotationAt(f);
                    point2.set(0, 1, 0).rotate(rotation).add(point1);
                    vertexObject.addLine(point1, point2);
                }
                renderable = new RenderableData(vertexObject);
                renderable.setColor(ColorRGB.YELLOW);
                road.devRenderables.add(renderable);
            }
        }

        //Render the lane connections.
        for (RoadLane lane : road.lanes) {
            for (List<RoadLaneConnection> curvePriorConnections : lane.priorConnections) {
                BezierCurve currentCurve = lane.curves.get(lane.priorConnections.indexOf(curvePriorConnections));
                for (RoadLaneConnection priorConnection : curvePriorConnections) {
                    TileEntityRoad otherRoad = road.world.getTileEntity(priorConnection.tileLocation);
                    if (otherRoad != null) {
                        RoadLane otherLane = otherRoad.lanes.get(priorConnection.laneNumber);
                        if (otherLane != null) {
                            RenderableVertices vertexObject = new RenderableVertices(1);

                            //Get the connection point.
                            currentCurve.setPointToPositionAt(point1, 0.5F);
                            rotation = currentCurve.getRotationAt(0.5F);
                            point2.set(0, 2.0, 0).rotate(rotation).add(point1);

                            //Render marker.
                            vertexObject.addLine(point1, point2);

                            renderable = new RenderableData(vertexObject);
                            renderable.setColor(ColorRGB.PINK);
                            road.devRenderables.add(renderable);
                        }
                    }
                }
            }
        }
        for (RoadLane lane : road.lanes) {
            for (List<RoadLaneConnection> curveNextConnections : lane.nextConnections) {
                BezierCurve currentCurve = lane.curves.get(lane.nextConnections.indexOf(curveNextConnections));
                for (RoadLaneConnection nextConnection : curveNextConnections) {
                    TileEntityRoad otherRoad = road.world.getTileEntity(nextConnection.tileLocation);
                    if (otherRoad != null) {
                        RoadLane otherLane = otherRoad.lanes.get(nextConnection.laneNumber);
                        if (otherLane != null) {
                            RenderableVertices vertexObject = new RenderableVertices(1);

                            //Get the connection point.
                            currentCurve.setPointToPositionAt(point1, currentCurve.pathLength - 0.5F);
                            rotation = currentCurve.getRotationAt(currentCurve.pathLength - 0.5F);
                            point2.set(0, 2.0, 0).rotate(rotation).add(point1);

                            //Render marker.
                            vertexObject.addLine(point1, point2);

                            renderable = new RenderableData(vertexObject);
                            renderable.setColor(ColorRGB.ORANGE);
                            road.devRenderables.add(renderable);
                        }
                    }
                }
            }
        }
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        //Save isActive state.
        data.setBoolean("isActive", isActive);

        //Save all components.
        for (Entry<RoadComponent, ItemRoadComponent> connectedObjectEntry : components.entrySet()) {
            data.setString("packID" + connectedObjectEntry.getKey().name(), connectedObjectEntry.getValue().definition.packID);
            data.setString("systemName" + connectedObjectEntry.getKey().name(), connectedObjectEntry.getValue().definition.systemName);
        }

        //Save curve data.
        if (dynamicCurve != null) {
            data.setPoint3d("startingOffset", dynamicCurve.startPos);
            data.setPoint3d("endingOffset", dynamicCurve.endPos);
            data.setPoint3d("startingAngles", dynamicCurve.startRotation.angles);
            data.setPoint3d("endingAngles", dynamicCurve.endRotation.angles);
        }

        //Save lane data.
        for (int laneNumber = 0; laneNumber < lanes.size(); ++laneNumber) {
            data.setData("lane" + laneNumber, lanes.get(laneNumber).getData());
        }

        //Save cure collision point data.
        data.setPoint3dsCompact("collisionBlockOffsets", collisionBlockOffsets);
        data.setPoint3dsCompact("collidingBlockOffsets", collidingBlockOffsets);
        return data;
    }

    private static class RoadTextureOverlayRenderable {
        private final RenderableData renderable;
        private final int textureWidth;
        private final int textureHeight;

        private RoadTextureOverlayRenderable(RenderableData renderable, int textureWidth, int textureHeight) {
            this.renderable = renderable;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
        }
    }

    /**
     * Enums for part-specific stuff.
     */
    public enum RoadComponent {
        @JSONDescription("The core component.  This must be placed down before any other road components.  This is a static component with defined lanes and collision.")
        CORE_STATIC,
        @JSONDescription("The core component.  This must be placed down before any other road components.  This is a dynamic component with flexible collision and lane paths, but defined lane counts and offsets.")
        CORE_DYNAMIC
    }
}
