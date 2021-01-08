package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.LinkedHashMap;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.rendering.components.AnimationsDecor;
import minecrafttransportsimulator.rendering.components.IAnimationProvider;
import minecrafttransportsimulator.rendering.components.ITextProvider;
import minecrafttransportsimulator.rendering.instances.RenderDecor;

/**Decor tile entity.  Contains the definition so we know how
 * to render this in the TESR call, as well as if we need to do
 * crafting operations if we are a crafting decor type.
 *
 * @author don_bruce
 */
public class TileEntityDecor extends ATileEntityBase<JSONDecor> implements IAnimationProvider, ITextProvider{
	public final BoundingBox[] boundingBoxes = new BoundingBox[4];
	public final Map<JSONText, String> text = new LinkedHashMap<JSONText, String>();
	
	private static final AnimationsDecor animator = new AnimationsDecor();
	
	public TileEntityDecor(IWrapperWorld world, Point3i position, IWrapperNBT data){
		super(world, position, data);
		//Add a bounding box for each rotation.
		this.boundingBoxes[0] = new BoundingBox(new Point3d(0, 0, 0), definition.general.width/2D, definition.general.height/2D, definition.general.depth/2D);
		this.boundingBoxes[1] = new BoundingBox(new Point3d(0, 0, 0), definition.general.depth/2D, definition.general.height/2D, definition.general.width/2D);
		this.boundingBoxes[2] = boundingBoxes[0];
		this.boundingBoxes[3] = boundingBoxes[1];
		
		//Get text.
		if(definition.rendering != null && definition.rendering.textObjects != null){
			for(int i=0; i<definition.rendering.textObjects.size(); ++i){
				text.put(definition.rendering.textObjects.get(i), data.getString("textLine" + i));
			}
		}
	}
	
	@Override
    public Point3d getProviderPosition(){
		return doublePosition;
	}
	
	@Override
    public IWrapperWorld getProviderWorld(){
		return world;
	}
	
	@Override
    public AnimationsDecor getAnimationSystem(){
		return animator;
	}
	
	@Override
	public float getLightPower(){
		return (15 - world.getRedstonePower(position))/15F;
	}
	
	@Override
	public Map<JSONText, String> getText(){
		return text;
	}
	
	@Override
	public String getSecondaryTextColor(){
		for(JSONSubDefinition subDefinition : definition.definitions){
			if(subDefinition.subName.equals(currentSubName)){
				return subDefinition.secondColor;
			}
		}
		throw new IllegalArgumentException("ERROR: Tried to get the definition for a decor of subName:" + currentSubName + ".  But that isn't a valid subName for the decor:" + definition.packID + ":" + definition.systemName + ".  Report this to the pack author as this is a missing JSON component!");
	}
	
	@Override
	public boolean renderTextLit(){
		return true;
	}
	
	@Override
	public RenderDecor getRenderer(){
		return new RenderDecor();
	}
	
	@Override
	public void save(IWrapperNBT data){
		super.save(data);
		if(definition.rendering != null && definition.rendering.textObjects != null){
			for(int i=0; i<definition.rendering.textObjects.size(); ++i){
				data.setString("textLine" + i, text.get(definition.rendering.textObjects.get(i)));
			}
		}
	}
}
