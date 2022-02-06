package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.rendering.instances.RenderDecor;

/**Decor tile entity.  Contains the definition so we know how
 * to render this in the TESR call, as well as if we need to do
 * crafting operations if we are a crafting decor type.
 *
 * @author don_bruce
 */
public class TileEntityDecor extends ATileEntityBase<JSONDecor>{
	
	public static final String CLICKED_VARIABLE = "clicked";
	public static final String ACTIVATED_VARIABLE = "activated";
	
	private static RenderDecor renderer;
	
	public TileEntityDecor(WrapperWorld world, Point3d position, WrapperPlayer placingPlayer, WrapperNBT data){
		super(world, position, placingPlayer, data);
		//Set our bounding box based on our rotation and parameters.
		if(definition.decor.height != 1.0){
			//Need custom bounding box.  Default assumes centered to position.
			this.boundingBox = new BoundingBox(position.copy().add(0, definition.decor.height/2D - 0.5D, 0), 0, 0, 0);
		}
		boundingBox.heightRadius = definition.decor.height/2D;
		if(Math.abs(angles.y%180) == 0){
			boundingBox.widthRadius = definition.decor.width/2D;
			boundingBox.depthRadius = definition.decor.depth/2D;
		}else{
			boundingBox.widthRadius = definition.decor.depth/2D;
			boundingBox.depthRadius = definition.decor.width/2D;
		}
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			//Reset clicked state.
			setVariable(CLICKED_VARIABLE, 0);
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public boolean interact(WrapperPlayer player){
		if(player.isHoldingItemType(ItemComponentType.PAINT_GUN)){
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
			setVariable(CLICKED_VARIABLE, 1);
			toggleVariable(ACTIVATED_VARIABLE);
			InterfacePacket.sendToAllClients(new PacketEntityVariableSet(this, CLICKED_VARIABLE, 1));
			InterfacePacket.sendToAllClients(new PacketEntityVariableToggle(this, ACTIVATED_VARIABLE));
		}
		return true;
	}
	
	@Override
	public int getRotationIncrement(){
		return 90;
	}
	
	@Override
	public float getLightProvided(){
    	return definition.decor.lightLevel;
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
