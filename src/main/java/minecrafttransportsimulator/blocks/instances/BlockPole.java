package minecrafttransportsimulator.blocks.instances;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_Sign;
import minecrafttransportsimulator.guis.GUISign;
import minecrafttransportsimulator.items.packs.ItemPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPoleChange;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.wrappers.WrapperNBT;
import minecrafttransportsimulator.wrappers.WrapperNetwork;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**Pole block class.  This class allows for dynamic collision boxes and dynamic
 * placement of components on poles via the Tile Entity.
 *
 * @author don_bruce
 */
public class BlockPole extends ABlockBase implements IBlockTileEntity<JSONPoleComponent>{
	private final Map<Axis, BoundingBox> axisBounds = new HashMap<Axis, BoundingBox>();
	
	public BlockPole(){
		super(10.0F, 5.0F);
		double connectorRadius = 0.125D;
		double axialRadius = (0.5D - connectorRadius)/2D;
		double axialCenterPoint = 0.5D - axialRadius;
		axisBounds.put(Axis.NONE, new BoundingBox(0, 0, 0, connectorRadius, connectorRadius, connectorRadius));
		axisBounds.put(Axis.UP, new BoundingBox(0, axialCenterPoint, 0, connectorRadius, axialRadius, connectorRadius));
		axisBounds.put(Axis.DOWN, new BoundingBox(0, -axialCenterPoint, 0, connectorRadius, axialRadius, connectorRadius));
		axisBounds.put(Axis.NORTH, new BoundingBox(0, 0, -axialCenterPoint, connectorRadius, connectorRadius, axialRadius));
		axisBounds.put(Axis.SOUTH, new BoundingBox(0, 0, axialCenterPoint, connectorRadius, connectorRadius, axialRadius));
		axisBounds.put(Axis.EAST, new BoundingBox(axialCenterPoint, 0, 0, axialRadius, connectorRadius, connectorRadius));
		axisBounds.put(Axis.WEST, new BoundingBox(-axialCenterPoint, 0, 0, axialRadius, connectorRadius, connectorRadius));
	}
	
	@Override
	public void onPlaced(WrapperWorld world, Point3i location, WrapperPlayer player){
		//If there's no NBT data, this is a new pole and needs to have its initial component added.
		if(!player.getHeldStack().hasTagCompound()){
			TileEntityPole pole = (TileEntityPole) world.getTileEntity(location);
			pole.components.put(Axis.NONE, TileEntityPole.createComponent(((ItemPoleComponent) player.getHeldStack().getItem()).definition));
		}
	}
	
	@Override
	public boolean onClicked(WrapperWorld world, Point3i location, Axis axis, WrapperPlayer player, boolean rightClick){
		TileEntityPole pole = (TileEntityPole) world.getTileEntity(location);
		if(rightClick){
			//If we can place a component here, try and do so now.
			if(player.getHeldStack().getItem() instanceof ItemPoleComponent && !pole.components.containsKey(axis)){
				//We can place a component here.  Try and do so.
				ItemPoleComponent item = (ItemPoleComponent) player.getHeldStack().getItem();
				if(!world.isClient()){
					//Check for interaction and send out packet if we could interact.
					if((!ConfigSystem.configObject.general.opSignEditingOnly.value || player.isOP())){
						ATileEntityPole_Component component = TileEntityPole.createComponent(item.definition);
						//Add text if we created a sign component and have text from the NBT tag.
						if(component instanceof TileEntityPole_Sign && player.getHeldStack().hasTagCompound()){
							((TileEntityPole_Sign) component).textLines.addAll(new WrapperNBT(player.getHeldStack().getTagCompound()).getStrings("textLines", component.definition.general.textLines.length));
						}
						pole.components.put(axis, component);
						world.markTileEntityChanged(pole.position);
						WrapperNetwork.sendToAllClients(new PacketTileEntityPoleChange(pole, pole.components.get(axis), axis));
						return true;
					}
				}
				return true;
			}else{
				//If we are on the client, and we clicked a sign component, open the GUI.
				if(world.isClient() && pole.components.get(axis) instanceof TileEntityPole_Sign){
					FMLCommonHandler.instance().showGuiScreen(new GUISign((TileEntityPole_Sign) pole.components.get(axis)));
				}
			}
		}else{
			//Try to remove a sign component if we have one.
			if(!world.isClient()){
				if(pole.components.containsKey(axis) && (!ConfigSystem.configObject.general.opSignEditingOnly.value || player.isOP())){
					pole.components.remove(axis);
					world.markTileEntityChanged(pole.position);
					WrapperNetwork.sendToAllClients(new PacketTileEntityPoleChange(pole, null, axis));
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public void onWorldUpdate(WrapperWorld world, Point3i location, boolean redstonePower){
		TileEntityPole pole = (TileEntityPole) world.getTileEntity(location);
		if(pole != null){
			pole.connections.clear();
			pole.solidConnections.clear();
			for(Axis axis : Axis.values()){
				Point3i offsetPoint = axis.getOffsetPoint(location);
				if(world.getBlock(offsetPoint) instanceof BlockPole){
					pole.connections.put(axis, true);
					pole.solidConnections.put(axis, false);
				}else if(world.isBlockSolid(offsetPoint)){
					pole.connections.put(axis, false);
					pole.solidConnections.put(axis, true);
				}else{
					pole.connections.put(axis, false);
					pole.solidConnections.put(axis, false);
				}
			}
		}
	}
	
	@Override
	public void addCollisionBoxes(WrapperWorld world, Point3i location, List<BoundingBox> collidingBoxes){
		//For every connection or component we have, return a collision box.
		TileEntityPole pole = (TileEntityPole) world.getTileEntity(location);
		if(pole != null){
			for(Axis axis : Axis.values()){
				if(pole.connections.containsKey(axis) || pole.solidConnections.containsKey(axis)){
					collidingBoxes.add(axisBounds.get(axis));
				}
			}
		}else{
			super.addCollisionBoxes(world, location, collidingBoxes);
		}
	}
	
	@Override
	public TileEntityPole createTileEntity(){
		return new TileEntityPole();
	}
}
