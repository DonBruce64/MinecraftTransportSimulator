package minecrafttransportsimulator.blocks.instances;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_Sign;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.items.instances.ItemPoleComponent.PoleComponentType;
import minecrafttransportsimulator.items.instances.ItemWrench;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPoleChange;
import net.minecraft.item.ItemStack;

/**Pole block class.  This class allows for dynamic collision boxes and dynamic
 * placement of components on poles via the Tile Entity.
 *
 * @author don_bruce
 */
public class BlockPole extends ABlockBase implements IBlockTileEntity<TileEntityPole>{
	private final Map<Axis, BoundingBox> axisBounds = new HashMap<Axis, BoundingBox>();
	
	public BlockPole(){
		super(10.0F, 5.0F);
		double connectorRadius = 0.125D;
		double axialRadius = (0.5D - connectorRadius)/2D;
		double axialCenterPoint = 0.5D - axialRadius;
		axisBounds.put(Axis.NONE, new BoundingBox(new Point3d(), connectorRadius, connectorRadius, connectorRadius));
		axisBounds.put(Axis.UP, new BoundingBox(new Point3d(0, axialCenterPoint, 0), connectorRadius, axialRadius, connectorRadius));
		axisBounds.put(Axis.DOWN, new BoundingBox(new Point3d(0, -axialCenterPoint, 0), connectorRadius, axialRadius, connectorRadius));
		axisBounds.put(Axis.NORTH, new BoundingBox(new Point3d(0, 0, -axialCenterPoint), connectorRadius, connectorRadius, axialRadius));
		axisBounds.put(Axis.SOUTH, new BoundingBox(new Point3d(0, 0, axialCenterPoint), connectorRadius, connectorRadius, axialRadius));
		axisBounds.put(Axis.EAST, new BoundingBox(new Point3d(axialCenterPoint, 0, 0), axialRadius, connectorRadius, connectorRadius));
		axisBounds.put(Axis.WEST, new BoundingBox(new Point3d(-axialCenterPoint, 0, 0), axialRadius, connectorRadius, connectorRadius));
	}
	
	@Override
	public boolean onClicked(WrapperWorld world, Point3d position, Axis axis, WrapperPlayer player){
		//Change the axis to match the 8-dim axis for poles.  Blocks only get a 4-dim axis.
		axis = Axis.getFromRotation(player.getYaw()).getOpposite();
		
		//Fire a packet to interact with this pole.  Will either add, remove, or allow editing of the pole.
		//Only fire packet if player is holding a pole component that's not an actual pole, a wrench,
		//or is clicking a sign with text.
		TileEntityPole pole = (TileEntityPole) world.getTileEntity(position);
		if(pole != null){
			ItemStack heldStack = player.getHeldStack();
			AItemBase heldItem = player.getHeldItem();
			ATileEntityPole_Component clickedComponent = pole.components.get(axis);
			boolean isPlayerHoldingWrench = heldItem instanceof ItemWrench;
			boolean isPlayerClickingEditableSign = clickedComponent instanceof TileEntityPole_Sign && clickedComponent.definition.rendering != null && clickedComponent.definition.rendering.textObjects != null;
			boolean isPlayerHoldingComponent = heldItem instanceof ItemPoleComponent;
			boolean isPlayerHoldingCore = isPlayerHoldingComponent && ((ItemPoleComponent) heldItem).definition.pole.type.equals(PoleComponentType.CORE);
			if(world.isClient()){
				if(isPlayerHoldingWrench){
					InterfacePacket.sendToServer(new PacketTileEntityPoleChange(pole, axis, null, null, true));
				}else if(isPlayerClickingEditableSign){
					InterfacePacket.sendToServer(new PacketTileEntityPoleChange(pole, axis, null, null, false));
				}else if(isPlayerHoldingComponent && !isPlayerHoldingCore){
					List<String> textLines = null;
					ItemPoleComponent component = (ItemPoleComponent) heldItem;
					if(component.definition.rendering != null && component.definition.rendering.textObjects != null){
						textLines = new WrapperNBT(heldStack).getStrings("textLines", component.definition.rendering.textObjects.size());
					}
					InterfacePacket.sendToServer(new PacketTileEntityPoleChange(pole, axis, component, textLines, false));	
				}else{
					return false;
				}
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void addCollisionBoxes(WrapperWorld world, Point3d position, List<BoundingBox> collidingBoxes){
		//For every connection or component we have, return a collision box.
		TileEntityPole pole = (TileEntityPole) world.getTileEntity(position);
		if(pole != null){
			for(Axis axis : Axis.values()){
				if(axis.blockBased){
					if(world.getBlock(axis.getOffsetPoint(position)) instanceof BlockPole || world.isBlockSolid(axis.getOffsetPoint(position), axis.getOpposite()) || pole.components.containsKey(axis)){
						collidingBoxes.add(axisBounds.get(axis));
					}
				}else if(axis.equals(Axis.NONE)){
					collidingBoxes.add(axisBounds.get(axis));
				}
			}
		}else{
			super.addCollisionBoxes(world, position, collidingBoxes);
		}
	}
	
	@Override
	public TileEntityPole createTileEntity(WrapperWorld world, Point3d position, WrapperNBT data){
		return new TileEntityPole(world, position, data);
	}

	@Override
	public Class<TileEntityPole> getTileEntityClass(){
		return TileEntityPole.class;
	}
}
