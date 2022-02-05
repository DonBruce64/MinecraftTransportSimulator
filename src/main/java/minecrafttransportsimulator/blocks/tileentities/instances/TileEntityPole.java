package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.instances.BlockPole;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.items.instances.ItemPoleComponent.PoleComponentType;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperItemStack;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest.EntityGUIType;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPoleChange;
import minecrafttransportsimulator.rendering.instances.RenderPole;
import minecrafttransportsimulator.systems.ConfigSystem;

/**Pole tile entity.  Remembers what components we have attached and the state of the components.
 * This tile entity does not tick, as states can be determined without ticks or are controlled
 * from other tickable TEs.
*
* @author don_bruce
*/
public class TileEntityPole extends ATileEntityBase<JSONPoleComponent>{
	public final Map<Axis, ATileEntityPole_Component> components = new HashMap<Axis, ATileEntityPole_Component>();
	
	private float maxTotalLightLevel;
	private static RenderPole renderer;
	
	public TileEntityPole(WrapperWorld world, Point3d position, WrapperPlayer placingPlayer, WrapperNBT data){
		super(world, position, placingPlayer, data);
		//Need custom bounding box.  Default assumes centered to position.
		this.boundingBox = new BoundingBox(new Point3d(), 0, 0, 0);
		
		//Load components back in.
		for(Axis axis : Axis.values()){
			WrapperNBT componentData = data.getData(axis.name());
			if(componentData != null){
				ATileEntityPole_Component newComponent = PoleComponentType.createComponent(this, placingPlayer, axis, componentData);
				changeComponent(axis, newComponent);
			}else if(axis.equals(Axis.NONE)){
				//Add our core component to the NONE axis.
				//This is done for ease of rendering and lookup routines.
				changeComponent(axis, PoleComponentType.createComponent(this, placingPlayer, axis, data));
			}
		}
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			//Forward update call to components.
			for(ATileEntityPole_Component component : components.values()){
				component.update();
			}
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public void remove(){
		super.remove();
		//Remove components as these all come with the main pole.
		for(ATileEntityPole_Component component : components.values()){
			component.remove();
		}
	}
	
	@Override
	public boolean interact(WrapperPlayer player){
		//Fire a packet to interact with this pole.  Will either add, remove, or allow editing of the pole.
		//Only fire packet if player is holding a pole component that's not an actual pole, a wrench,
		//or is clicking a sign with text.
		TileEntityPole pole = (TileEntityPole) world.getTileEntity(position);
		if(pole != null){
			Axis axis = Axis.getFromRotation(player.getYaw(), pole.definition.pole.allowsDiagonals).getOpposite();
			WrapperItemStack heldStack = player.getHeldStack();
			AItemBase heldItem = heldStack.getItem();
			ATileEntityPole_Component clickedComponent = pole.components.get(axis);
			if(!ConfigSystem.configObject.general.opSignEditingOnly.value || player.isOP()){
				if(player.isHoldingItemType(ItemComponentType.WRENCH)){
					//Holding a wrench, try to remove the component.
					//Need to check if it will fit in the player's inventory.
					if(pole.components.containsKey(axis)){
						ATileEntityPole_Component component = pole.components.get(axis);
						if(player.isCreative() || player.getInventory().addStack(component.getItem().getNewStack(component.save(new WrapperNBT())))){
							changeComponent(axis, null);
							InterfacePacket.sendToAllClients(new PacketTileEntityPoleChange(this, player, axis, null));
						}
						return true;
					}
				}else if(clickedComponent instanceof TileEntityPole_Sign && clickedComponent.definition.rendering != null && clickedComponent.definition.rendering.textObjects != null){
					//Player clicked a sign with text.  Open the GUI to edit it.
					player.sendPacket(new PacketEntityGUIRequest(clickedComponent, player, EntityGUIType.TEXT_EDITOR));
					return true;
				}else if(heldItem instanceof ItemPoleComponent && !((ItemPoleComponent) heldItem).definition.pole.type.equals(PoleComponentType.CORE) && !pole.components.containsKey(axis)){
					//Player is holding component that could be added.  Try and do so.
					ItemPoleComponent componentItem = (ItemPoleComponent) heldItem;
					WrapperNBT stackData = heldStack.getData();
					componentItem.populateDefaultData(stackData);
					ATileEntityPole_Component newComponent = PoleComponentType.createComponent(pole, player, axis, stackData);
					changeComponent(axis, newComponent);
					if(!player.isCreative()){
						player.getInventory().removeFromSlot(player.getHotbarIndex(), 1);
					}
					InterfacePacket.sendToAllClients(new PacketTileEntityPoleChange(this, player, axis, newComponent.save(new WrapperNBT())));
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
    public int getRotationIncrement(){
		return 360;
    }
	
	@Override
	public BoundingBox getCollisionBox(){
		//Update collisions before returning.
		boundingBox.widthRadius = definition.pole.radius;
		boundingBox.heightRadius = definition.pole.radius;
		boundingBox.depthRadius = definition.pole.radius;
		boundingBox.globalCenter.setTo(position);
		for(Axis axis : Axis.values()){
			if(axis.blockBased){
				if(world.getBlock(axis.getOffsetPoint(position)) instanceof BlockPole || world.isBlockSolid(axis.getOffsetPoint(position), axis.getOpposite()) || components.containsKey(axis)){
					switch(axis){
						case NORTH: {
							if(boundingBox.depthRadius == definition.pole.radius){
								boundingBox.depthRadius = definition.pole.radius + 0.25;
								boundingBox.globalCenter.z = position.z - (0.5 - definition.pole.radius)/2D;
							}else{
								boundingBox.depthRadius = 0.5;
								boundingBox.globalCenter.z = position.z;
							}
							break;
						}
						case SOUTH: {
							if(boundingBox.depthRadius == definition.pole.radius){
								boundingBox.depthRadius = definition.pole.radius + 0.25;
								boundingBox.globalCenter.z = position.z + (0.5 - definition.pole.radius)/2D;
							}else{
								boundingBox.depthRadius = 0.5;
								boundingBox.globalCenter.z = position.z;
							}
							break;
						}
						case EAST: {
							if(boundingBox.widthRadius == definition.pole.radius){
								boundingBox.widthRadius = definition.pole.radius + 0.25;
								boundingBox.globalCenter.x = position.x + (0.5 - definition.pole.radius)/2D;
							}else{
								boundingBox.widthRadius = 0.5;
								boundingBox.globalCenter.x = position.x;
							}
							break;
						}
						case WEST: {
							if(boundingBox.widthRadius == definition.pole.radius){
								boundingBox.widthRadius = definition.pole.radius + 0.25;
								boundingBox.globalCenter.x = position.x - (0.5 - definition.pole.radius)/2D;
							}else{
								boundingBox.widthRadius = 0.5;
								boundingBox.globalCenter.x = position.x;
							}
							break;
						}
						case UP: {
							if(boundingBox.heightRadius == definition.pole.radius){
								boundingBox.heightRadius = definition.pole.radius + 0.25;
								boundingBox.globalCenter.y = position.y + (0.5 - definition.pole.radius)/2D;
							}else{
								boundingBox.heightRadius = 0.5;
								boundingBox.globalCenter.y = position.y;
							}
							break;
						}
						case DOWN: {
							if(boundingBox.heightRadius == definition.pole.radius){
								boundingBox.heightRadius = definition.pole.radius + 0.25;
								boundingBox.globalCenter.y = position.y - (0.5 - definition.pole.radius)/2D;
							}else{
								boundingBox.heightRadius = 0.5;
								boundingBox.globalCenter.y = position.y;
							}
							break;
						}
						default: break;
					}
				}
			}
		}
		return super.getCollisionBox();
    }
	
	@Override
	public float getLightProvided(){
		return maxTotalLightLevel;
	}
	
	@Override
	public void addDropsToList(List<WrapperItemStack> drops){
		for(Axis axis : Axis.values()){
			ATileEntityPole_Component component = components.get(axis);
			if(component != null){
				drops.add(component.getItem().getNewStack(component.save(new WrapperNBT())));
			}
		}
	}
	
	/**
	 * Helper method to add/remove components to this pole.  Ensures all states are maintained
	 * for bounding boxes and component position.  To remove a component, pass-in null for the axis.
	 */
	public void changeComponent(Axis newAxis, ATileEntityPole_Component newComponent){
		//Update component map.
		if(newComponent != null){
			components.put(newAxis, newComponent);
			if(newAxis.equals(Axis.NONE)){
				newComponent.position.setTo(position);
				newComponent.angles.setTo(angles);
			}else{
				newComponent.position.set(0, 0, definition.pole.radius + 0.001).rotateY(newAxis.yRotation).add(position);
				newComponent.angles.set(0, newAxis.yRotation, 0).add(angles);
			}
			newComponent.prevPosition.setTo(newComponent.position);
			newComponent.prevAngles.setTo(newComponent.angles);
			world.addEntity(newComponent);
		}else if(components.containsKey(newAxis)){
			components.remove(newAxis).remove();
		}
		
		//Update lighting state.
		maxTotalLightLevel = 0;
		for(ATileEntityPole_Component component : components.values()){
			maxTotalLightLevel = Math.max(maxTotalLightLevel, component.getLightProvided());
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public RenderPole getRenderer(){
		if(renderer == null){
			renderer = new RenderPole();
		}
		return renderer;
	}
	
	@Override
    public WrapperNBT save(WrapperNBT data){
		super.save(data);
		//Save all components.
		for(Entry<Axis, ATileEntityPole_Component> connectedObjectEntry : components.entrySet()){
			data.setData(connectedObjectEntry.getKey().name(), connectedObjectEntry.getValue().save(new WrapperNBT()));
		}
		return data;
	}
}
