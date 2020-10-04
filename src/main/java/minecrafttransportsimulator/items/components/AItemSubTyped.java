package minecrafttransportsimulator.items.components;

import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;

public abstract class AItemSubTyped<JSONDefinition extends AJSONMultiModelProvider<?>> extends AItemPack<JSONDefinition>{
	public final String subName;
	
	public AItemSubTyped(JSONDefinition definition, String subName){
		super(definition);
		this.subName = subName;
	}
	
	@Override
	public String getRegistrationName(){
		return definition.systemName + subName;
	}
	
	@Override
	public String getItemName(){
		for(AJSONMultiModelProvider<?>.SubDefinition subDefinition : definition.definitions){
			if(subDefinition.subName.equals(subName)){
				return subDefinition.name;
			}
		}
		return "";
	}
	
	public String[] getExtraMaterials(){
		for(AJSONMultiModelProvider<?>.SubDefinition subDefinition : definition.definitions){
			if(subDefinition.subName.equals(subName)){
				return subDefinition.extraMaterials;
			}
		}
		return null;
	}
}
