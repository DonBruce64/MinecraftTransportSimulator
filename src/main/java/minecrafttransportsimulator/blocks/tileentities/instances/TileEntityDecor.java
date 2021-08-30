package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityTickable;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.items.instances.ItemItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.rendering.instances.RenderDecor;

/**Decor tile entity.  Contains the definition so we know how
 * to render this in the TESR call, as well as if we need to do
 * crafting operations if we are a crafting decor type.
 *
 * @author don_bruce
 */
public class TileEntityDecor extends ATileEntityBase<JSONDecor> implements ITileEntityTickable{
	public final BoundingBox[] boundingBoxes = new BoundingBox[4];
	
	private static RenderDecor renderer;
	
	public TileEntityDecor(WrapperWorld world, Point3d position, WrapperPlayer placingPlayer, WrapperNBT data){
		super(world, position, placingPlayer, data);
		//Add a bounding box for each rotation.
		this.boundingBoxes[0] = new BoundingBox(new Point3d(), definition.decor.width/2D, definition.decor.height/2D, definition.decor.depth/2D);
		this.boundingBoxes[1] = new BoundingBox(new Point3d(), definition.decor.depth/2D, definition.decor.height/2D, definition.decor.width/2D);
		this.boundingBoxes[2] = boundingBoxes[0];
		this.boundingBoxes[3] = boundingBoxes[1];
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			//Reset clicked state.
			if(variablesOn.contains("clicked")){
				variablesOn.remove("clicked");
			}
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public boolean interact(WrapperPlayer player){
    	AItemBase heldItem = player.getHeldItem();
		if(heldItem instanceof ItemItem && ((ItemItem) heldItem).definition.item.equals(ItemComponentType.PAINT_GUN)){
			//Don't do decor actions if we are holding a paint gun.
			return false;
		}else if(definition.decor.crafting != null){
			if(!world.isClient()){
				player.sendPacket(new PacketEntityGUIRequest(this, player, PacketEntityGUIRequest.EntityGUIType.PART_BENCH));
				
			}
		}else if(!text.isEmpty()){
			if(!world.isClient()){
				player.sendPacket(new PacketEntityGUIRequest(this, player, PacketEntityGUIRequest.EntityGUIType.TEXT_EDITOR));
			}
		}
		if(!world.isClient()){
			variablesOn.add("clicked");
			if(variablesOn.contains("activated")){
				variablesOn.remove("activated");
			}else{
				variablesOn.add("activated");
			}
			InterfacePacket.sendToAllClients(new PacketEntityVariableToggle(this, "clicked"));
			InterfacePacket.sendToAllClients(new PacketEntityVariableToggle(this, "activated"));
		}
		return true;
	}
	
	@Override
	public int getRotationIncrement(){
		return 90;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public RenderDecor getRenderer(){
		if(renderer == null){
			renderer = new RenderDecor();
		}
		return renderer;
	}
}
