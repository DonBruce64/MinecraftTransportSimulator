package minecrafttransportsimulator.items.instances;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.instances.BlockRoad;
import minecrafttransportsimulator.blocks.instances.BlockRoadCollision;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.IItemBlock;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;

public class ItemRoadComponent extends AItemPack<JSONRoadComponent> implements IItemBlock{
	private final Map<IWrapperPlayer, Point3i> lastPositionClicked = new HashMap<IWrapperPlayer, Point3i>();
	private final Map<IWrapperPlayer, TileEntityRoad> lastRoadClicked = new HashMap<IWrapperPlayer, TileEntityRoad>();
	private final Map<IWrapperPlayer, Integer> lastLaneClicked = new HashMap<IWrapperPlayer, Integer>();
	
	public ItemRoadComponent(JSONRoadComponent definition){
		super(definition);
	}
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data){
		if(definition.general.type.equals("core")){
			for(byte i=1; i<=5; ++i){
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.roadcomponent.line" + String.valueOf(i)));
			}
		}
	}
	
	@Override
	public boolean onBlockClicked(IWrapperWorld world, IWrapperPlayer player, Point3i point, Axis axis){
		//Create curves between the prior clicked point and the current point.
		//If the prior point is null, set it when we click it.
		//This could be either a block or a road itself.
		//If we click a road, we need to figure out what lane number we will connect to.
		if(!world.isClient()){
			if(player.isSneaking() || !lastPositionClicked.containsKey(player)){
				//Set starting point.  This may or may not be a road segment.
				lastPositionClicked.put(player, point);
				ABlockBase clickedBlock = world.getBlock(point);
				TileEntityRoad clickedRoad;
				if(clickedBlock instanceof BlockRoad){
					clickedRoad = world.getTileEntity(point);
				}else if(clickedBlock instanceof BlockRoadCollision){
					clickedRoad = ((BlockRoadCollision) clickedBlock).getRoadForBlock(world, point);
				}else{
					clickedRoad = null;
				}
				
				//If we clicked a road, get the lane number clicked.
				if(clickedRoad != null){
					//FIXME save lane number somewhere.  This allows for dual-road splits.  Requires collision boxes
					lastRoadClicked.put(player, clickedRoad);
					//lastLaneClicked.put(roadClicked.curves);
				}else{
					lastRoadClicked.remove(player);
					lastLaneClicked.remove(player);
				}
				player.sendPacket(new PacketPlayerChatMessage("interact.roadcomponent.set"));
			}else{
				if(point.distanceTo(lastPositionClicked.get(player)) < TileEntityRoad.MAX_SEGMENT_LENGTH){
					//If we clicked a road, then we need to try to connect to it.
					//If we didn't click a road, we need to try to make one for us to start from.
					TileEntityRoad startingRoad;
					int startingLane;
					ABlockBase clickedBlock = world.getBlock(point);
					if(clickedBlock instanceof BlockRoad){
						startingRoad = world.getTileEntity(point);
					}else if(clickedBlock instanceof BlockRoadCollision){
						startingRoad = ((BlockRoadCollision) clickedBlock).getRoadForBlock(world, point);
					}else{
						startingRoad = null;
					}
					
					//Now that we have a starting road, get the ending segment reference.
					TileEntityRoad endingRoad = lastRoadClicked.get(player);
					
					//We have both the starting and ending references.  Create the road and connect it.
					Point3i blockPlacementPoint = point.copy().add(0, 1, 0);
					if(world.setBlock(getBlock(), blockPlacementPoint, player, axis)){
						TileEntityRoad newRoad = world.getTileEntity(blockPlacementPoint);
						//FIXME do connection logic here.
						
						//FIXME  This needs to go into the road component item logic, not here.
						//player.sendPacket(new PacketPlayerChatMessage("interact.roadtool.blockingblocks"));
						
						//FIXME this needs to be done to connect roads.
						//Point3d endPointOffset = new Point3d(endTile.position).add(endTile.laneOffsets.get(endLaneNumber)).add(-position.x, -position.y, -position.z).subtract(laneOffsets.get(startLaneNumber));
						//curves.get(startLaneNumber).add(new BezierCurve(endPointOffset, (float) rotation, (float) (reversed ? endTile.rotation : endTile.rotation + 180)));
						lastRoadClicked.put(player, newRoad);
					}else{
						player.sendPacket(new PacketPlayerChatMessage("interact.roadcomponent.blockedplacement"));
					}
				}else{
					player.sendPacket(new PacketPlayerChatMessage("interact.roadcomponent.toofar"));
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public Class<? extends ABlockBase> getBlockClass(){
		return BlockRoad.class;
	}
}
