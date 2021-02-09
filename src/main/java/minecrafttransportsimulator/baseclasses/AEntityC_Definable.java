package minecrafttransportsimulator.baseclasses;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.rendering.components.ARenderEntity;
import minecrafttransportsimulator.rendering.components.DurationDelayClock;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Base class for entities that are defined via JSON definitions.
 * This level adds various method for said definitions, which include rendering functions. 
 * 
 * @author don_bruce
 */
public abstract class AEntityC_Definable<JSONDefinition extends AJSONMultiModelProvider> extends AEntityB_Existing{
	
	/**The pack definition for this entity.  May contain extra sections if the super-classes
	 * have them in their respective JSONs.
	 */
	public final JSONDefinition definition;

	/**The current subName for this entity.  Used to select which definition represents this entity.*/
	public String currentSubName;
	
	/**Map containing text lines for saved text provided by this entity.**/
	public final LinkedHashMap<JSONText, String> text = new LinkedHashMap<JSONText, String>();
	
	/**Set of variables that are "on" for this entity.  Used for animations.**/
	public final Set<String> variablesOn = new HashSet<String>();
	
	public AEntityC_Definable(WrapperWorld world, WrapperEntity wrapper, WrapperNBT data){
		super(world, wrapper, data);
		//Set definition and current subName.
		AItemSubTyped<JSONDefinition> item = PackParserSystem.getItem(data.getString("packID"), data.getString("systemName"), data.getString("subName")); 
		this.definition = item.definition;
		this.currentSubName = item.subName;
		
		//Load text.
		if(definition.rendering != null && definition.rendering.textObjects != null){
			for(int i=0; i<definition.rendering.textObjects.size(); ++i){
				text.put(definition.rendering.textObjects.get(i), data.getString("textLine" + i));
			}
		}
		
		//Load variables.
		variablesOn.addAll(data.getStrings("variablesOn"));
	}
	
    /**
   	 *  Returns true if this entity is lit up, and text should be rendered lit.
   	 *  Note that what text is lit is dependent on the text's definition, so just
   	 *  because text could be lit, does not mean it will be lit if the pack
   	 *  author doesn't want it to be.
   	 */
    public boolean renderTextLit(){
    	//FIXME make this false for vehicles.
    	return false;
    }
    
    /**
   	 *  Returns a string that represents this entity's secondary text color.  If this color is set,
   	 *  and text is told to render from this provider, and that text is told to use this color, then it will.
   	 *  Otherwise, the text will use its default color.
   	 */
    public String getSecondaryTextColor(){
    	for(JSONSubDefinition subDefinition : definition.definitions){
			if(subDefinition.subName.equals(currentSubName)){
				return subDefinition.secondColor;
			}
		}
		throw new IllegalArgumentException("Tried to get the definition for an object of subName:" + currentSubName + ".  But that isn't a valid subName for the object:" + definition.packID + ":" + definition.systemName + ".  Report this to the pack author as this is a missing JSON component!");
    }
    
    /**
	 *  Gets the renderer for this entity.  No actual rendering should be done in this method, 
	 *  as doing so could result in classes being imported during object instantiation on the server 
	 *  for graphics libraries that do not exist.  Instead, generate a class that does this and call it.
	 */
	public abstract <AnimationEntity extends AEntityC_Definable<JSONDefinition>> ARenderEntity<AnimationEntity> getRenderer();
	
	/**
	 *  Gets the value of the passed-animation for this entity.  Unlike the renderer, animation calls
	 *  are used both on the client and the server, so all methods inside here need to be server-safe.
	 */
	public abstract double getAnimationValue(JSONAnimationDefinition animation, double offset, DurationDelayClock clock, float partialTicks);
    
    @Override
    public void updateSounds(List<SoundInstance> sounds){
    	super.updateSounds(sounds);
    	//FIXME update sounds
    	//Start any looping sounds.
    	/*List<JSONSound> soundDefs = getSoundDefinitions();
    	if(soundDefs != null){
			for(JSONSound soundDef : soundDefs){
				if(soundDef.looping){
					InterfaceSound.playQuickSound(new SoundInstance(this, soundDef.name, soundDef.looping));
				}
			}
    	}*/
    }
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		data.setString("packID", definition.packID);
		data.setString("systemName", definition.systemName);
		data.setString("subName", currentSubName);
		int lineNumber = 0;
		for(String textLine : text.values()){
			data.setString("textLine" + lineNumber++, textLine);
		}
		data.setStrings("variablesOn", variablesOn);
	}
}
