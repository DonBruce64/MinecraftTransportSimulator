package minecrafttransportsimulator.items.components;

import java.util.List;

import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.WrapperNBT;

public abstract class AItemSubTyped<JSONDefinition extends AJSONMultiModelProvider> extends AItemPack<JSONDefinition>{
	public final String subName;
	
	public AItemSubTyped(JSONDefinition definition, String subName, String sourcePackID){
		super(definition, sourcePackID);
		this.subName = subName;
	}
	
	@Override
	public String getRegistrationName(){
		return super.getRegistrationName() + subName;
	}
	
	@Override
	public String getItemName(){
		for(JSONSubDefinition subDefinition : definition.definitions){
			if(subDefinition.subName.equals(subName)){
				return subDefinition.name != null ? subDefinition.name : super.getItemName();
			}
		}
		return "";
	}
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, WrapperNBT data){
		super.addTooltipLines(tooltipLines, data);
		for(JSONSubDefinition subDefinition : definition.definitions){
			if(subDefinition.subName.equals(subName)){
				if(subDefinition.description != null){
					for(String tooltipLine : subDefinition.description.split("\n")){
						tooltipLines.add(tooltipLine);
					}
				}
			}
		}
	}
	
	@Override
	public void populateDefaultData(WrapperNBT data){
		super.populateDefaultData(data);
		data.setString("subName", subName);
	}
	
	public List<String> getExtraMaterials(){
		for(JSONSubDefinition subDefinition : definition.definitions){
			if(subDefinition.subName.equals(subName)){
				return subDefinition.extraMaterials;
			}
		}
		return null;
	}
}
