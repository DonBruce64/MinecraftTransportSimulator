package minecrafttransportsimulator.blocks.instances;

import java.util.Arrays;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.guis.GUIPartBench;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class BlockPartsBench extends ABlockBase{
	private final List<String> validPartTypes;
	private final Class<? extends AJSONItem<?>> validJsonClass;
	public final RenderType renderType;

	
	public BlockPartsBench(Class<? extends AJSONItem<?>> validJsonClass, String... validPartTypes){
		super(10.0F, 5.0F);
		this.validPartTypes = Arrays.asList(validPartTypes);
		this.validJsonClass = validJsonClass;
		this.renderType = validJsonClass.equals(JSONVehicle.class) ? RenderType.SPINNING3D_EXTENDED : (validJsonClass.equals(JSONPart.class) || validJsonClass.equals(JSONDecor.class) ? RenderType.SPINNING3D : RenderType.SIMPLE2D);
	}
	
	@Override
	public boolean onClicked(WrapperWorld world, Point3i point, Axis axis, WrapperPlayer playerClicking){
		if(world.isClient()){
			//TODO this needs to be re-done with abstract GUI.
			FMLCommonHandler.instance().showGuiScreen(new GUIPartBench(this, playerClicking));
		}
		return true;
	}
	
	public boolean isJSONValid(AJSONItem<?> definition){
		if(definition.getClass().equals(validJsonClass)){
			return validJsonClass.equals(JSONPart.class) ? validPartTypes.contains(((JSONPart) definition).general.type) : true;
		}
		return false;
	}
	
	public static enum RenderType{
		SIMPLE2D(false, false),
		SPINNING3D(false, true),
		SPINNING3D_EXTENDED(true, true);
		
		public final boolean isForVehicles;
		public final boolean isFor3DModels;
			
		private RenderType(boolean isForVehicles, boolean isFor3DModels){
			this.isForVehicles = isForVehicles;
			this.isFor3DModels = isFor3DModels;
		}
	}
}
