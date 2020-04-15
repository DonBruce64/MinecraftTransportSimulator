package minecrafttransportsimulator.blocks.instances;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.guis.instances.GUIPartBench;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;

public class BlockPartsBench extends ABlockBase{
	private final List<Class<? extends AJSONItem<?>>> validJsonClasses = new ArrayList<Class<? extends AJSONItem<?>>>();
	private final List<String> validPartTypes;
	
	public BlockPartsBench(Class<? extends AJSONItem<?>> validClass, String... validPartTypes){
		super(10.0F, 5.0F);
		this.validJsonClasses.add(validClass);
		this.validPartTypes = Arrays.asList(validPartTypes);
	}
	
	@Override
	public boolean onClicked(WrapperWorld world, Point3i point, Axis axis, WrapperPlayer playerClicking){
		if(world.isClient()){
			WrapperGUI.openGUI(new GUIPartBench(this, playerClicking));
		}
		return true;
	}
	
	public BlockPartsBench addValidClass(Class<? extends AJSONItem<?>> validClass){
		validJsonClasses.add(validClass);
		return this;
	}
	
	public boolean isJSONValid(AJSONItem<?> definition){
		return validJsonClasses.contains(definition.getClass()) && (definition instanceof JSONPart ? validPartTypes.contains(((JSONPart) definition).general.type) : true);
	}
}
