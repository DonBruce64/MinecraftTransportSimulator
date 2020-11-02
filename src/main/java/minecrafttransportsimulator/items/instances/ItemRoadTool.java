package minecrafttransportsimulator.items.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.instances.BlockRoad;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad.RoadComponent;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.IItemBlock;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.PackParserSystem;

public class ItemRoadTool extends AItemBase implements IItemBlock{
	public final Map<RoadComponent, ItemRoadComponent> currentlySelectedComponents = new HashMap<RoadComponent, ItemRoadComponent>();
	private final Map<IWrapperPlayer, Point3i> lastRoadPositionClicked = new HashMap<IWrapperPlayer, Point3i>();
	private final Set<IWrapperPlayer> lastRoadInverted = new HashSet<IWrapperPlayer>();
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data){
		for(byte i=1; i<=5; ++i){
			tooltipLines.add(MasterLoader.coreInterface.translate("info.item.roadtool.line" + String.valueOf(i)));
		}
		for(Entry<RoadComponent, ItemRoadComponent> componentEntry : currentlySelectedComponents.entrySet()){
			tooltipLines.add(componentEntry.getKey().name() + ": " + componentEntry.getValue().getItemName());
		}
	}
	
	@Override
	public boolean onBlockClicked(IWrapperWorld world, IWrapperPlayer player, Point3i point, Axis axis){
		if(!world.isClient()){
			if(world.getBlock(point) instanceof BlockRoad){
				if(player.isSneaking()){
					TileEntityRoad roadClicked = world.getTileEntity(point);
					lastRoadPositionClicked.put(player, point);
					double deltaYaw = player.getHeadYaw() - roadClicked.rotation;
					while(deltaYaw > 180){
						deltaYaw -= 360;
					}
					while(deltaYaw < -180){
						deltaYaw += 360;
					}
					System.out.println(deltaYaw);
					if(Math.abs(deltaYaw) > 90){
						System.out.println("WIDE");
						lastRoadInverted.add(player);
					}else{
						System.out.println("NAR");
						lastRoadInverted.remove(player);
					}
					player.sendPacket(new PacketPlayerChatMessage("interact.roadtool.set"));
				}else{
					//TODO check to see if the sub-blocks for collision can be placed down and make this non-holographic.
					//player.sendPacket(new PacketPlayerChatMessage("interact.roadtool.blockingblocks"));
				}
				
			}else if(!currentlySelectedComponents.isEmpty()){
				if(!player.isSneaking()){
					Point3i blockPlacementPoint = point.copy().add(0, 1, 0);
					if(world.setBlock(getBlock(), blockPlacementPoint, player, axis)){
						//Set supplemental TE data now that we've set the block.
						TileEntityRoad road = world.getTileEntity(blockPlacementPoint);
						for(Entry<RoadComponent, ItemRoadComponent> componentEntry : currentlySelectedComponents.entrySet()){
							road.components.put(componentEntry.getKey(), componentEntry.getValue());
						}
						
						//If our last road block is close enough, create a connection.
						if(lastRoadPositionClicked.containsKey(player)){
							if(road.position.distanceTo(lastRoadPositionClicked.get(player)) < TileEntityRoad.MAX_SEGMENT_LENGTH){
								TileEntityRoad priorRoad = world.getTileEntity(lastRoadPositionClicked.get(player));
								//TODO save connection number somewhere.
								if(priorRoad != null){
									if(lastRoadInverted.contains(player)){
										for(int connectionNumber = 0; road.curveConnectionPoints.length > connectionNumber && priorRoad.curveConnectionPoints.length > priorRoad.curveConnectionPoints.length - 1 - connectionNumber  && priorRoad.curveConnectionPoints.length - 1 - connectionNumber >= 0; ++connectionNumber){
											road.setCurve(connectionNumber, priorRoad, priorRoad.curveConnectionPoints.length - 1 - connectionNumber, true);
										}
									}else{
										for(int connectionNumber = 0; road.curveConnectionPoints.length > connectionNumber && priorRoad.curveConnectionPoints.length > connectionNumber; ++connectionNumber){
											road.setCurve(connectionNumber, priorRoad, connectionNumber, false);
										}
									}
									//TODO set this only when the road turns from normal to holographic.
									lastRoadPositionClicked.put(player, blockPlacementPoint);
								}else{
									lastRoadPositionClicked.remove(player);
									player.sendPacket(new PacketPlayerChatMessage("interact.roadtool.invalid"));
								}
							}else{
								player.sendPacket(new PacketPlayerChatMessage("interact.roadtool.toofar"));
							}
						}else{
							lastRoadPositionClicked.put(player, blockPlacementPoint);
						}
					}
					return true;
				}else{
					lastRoadPositionClicked.remove(player);
					player.sendPacket(new PacketPlayerChatMessage("interact.roadtool.reset"));
				}
			}else{
				player.sendPacket(new PacketPlayerChatMessage("interact.roadtool.nocomponents"));
			}
		}
		return false;
	}
	
	@Override
	public boolean onUsed(IWrapperWorld world, IWrapperPlayer player){
		if(world.isClient()){
			//TODO open road customization GUI.  remove test code once we have one.
		}
		//TODO this is hard-coded and goes away when we have a GUI.
		for(AItemPack<?> item : PackParserSystem.getAllPackItems()){
			if(item instanceof ItemRoadComponent && (player.isSneaking() ? item.definition.systemName.contains("2lane") : item.definition.systemName.contains("4lane"))){
				currentlySelectedComponents.put(RoadComponent.CORE, (ItemRoadComponent) item);
				//Need to set the NBT of the tool to the currently-selected road core component.
				//This is needed as the TE to be created needs to know its core setup.
				IWrapperNBT data = player.getHeldStack().getData();
				data.setString("packID", item.definition.packID);
				data.setString("systemName", item.definition.systemName);
				player.getHeldStack().setData(data);
			}
		}
		return true;
	}
	
	@Override
	public Class<? extends ABlockBase> getBlockClass(){
		return BlockRoad.class;
	}
	
	@Override
	public boolean canBeStacked(){
		return false;
	}
}
