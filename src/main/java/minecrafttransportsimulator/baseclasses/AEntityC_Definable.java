package minecrafttransportsimulator.baseclasses;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition.AnimationComponentType;
import minecrafttransportsimulator.jsondefs.JSONSound;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.rendering.components.AAnimationsBase;
import minecrafttransportsimulator.rendering.components.ARenderEntity;
import minecrafttransportsimulator.rendering.components.DurationDelayClock;
import minecrafttransportsimulator.rendering.components.LightType;
import minecrafttransportsimulator.rendering.components.RenderTickData;
import minecrafttransportsimulator.sound.InterfaceSound;
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
	public String subName;
	
	/**Map containing text lines for saved text provided by this entity.**/
	public final LinkedHashMap<JSONText, String> text = new LinkedHashMap<JSONText, String>();
	
	/**Set of variables that are "on" for this entity.  Used for animations.**/
	public final Set<String> variablesOn = new HashSet<String>();
	
	/**Render data to help us render on the proper tick and time.**/
	public final RenderTickData renderData;
	
	private final LinkedHashMap<JSONSound, LinkedHashMap<JSONAnimationDefinition, DurationDelayClock>> soundVolumeClocks = new LinkedHashMap<JSONSound, LinkedHashMap<JSONAnimationDefinition, DurationDelayClock>>();
	private final LinkedHashMap<JSONSound, LinkedHashMap<JSONAnimationDefinition, DurationDelayClock>> soundPitchClocks = new LinkedHashMap<JSONSound, LinkedHashMap<JSONAnimationDefinition, DurationDelayClock>>();
	
	public AEntityC_Definable(WrapperWorld world, WrapperNBT data){
		super(world, data);
		//Set definition and current subName.
		AItemSubTyped<JSONDefinition> item = PackParserSystem.getItem(data.getString("packID"), data.getString("systemName"), data.getString("subName"));
		if(item != null){
			this.definition = item.definition;
			this.subName = item.subName;
		}else{
			this.definition = generateDefaultDefinition();
			this.subName = "";
		}
		
		//Load text.
		if(definition.rendering != null && definition.rendering.textObjects != null){
			for(int i=0; i<definition.rendering.textObjects.size(); ++i){
				text.put(definition.rendering.textObjects.get(i), data.getString("textLine" + i));
			}
		}
		
		//Load variables.
		this.variablesOn.addAll(data.getStrings("variablesOn"));
		
		//Make sure the generic light is in the variable set.
		this.variablesOn.add(LightType.GENERICLIGHT.lowercaseName);
		
		//Create render data if we are on the client.
		this.renderData = world.isClient() ? new RenderTickData(world) : null;
		
		//Create all sound clocks.
		if(definition.rendering != null && definition.rendering.sounds != null){
			for(JSONSound soundDef : definition.rendering.sounds){
				LinkedHashMap<JSONAnimationDefinition, DurationDelayClock> volumeClocks = new LinkedHashMap<JSONAnimationDefinition, DurationDelayClock>();				
				for(JSONAnimationDefinition animation : soundDef.volumeAnimations){
					volumeClocks.put(animation, new DurationDelayClock(animation));
				}
				soundVolumeClocks.put(soundDef, volumeClocks);
				
				LinkedHashMap<JSONAnimationDefinition, DurationDelayClock> pitchClocks = new LinkedHashMap<JSONAnimationDefinition, DurationDelayClock>();
				if(soundDef.pitchAnimations != null){
					for(JSONAnimationDefinition animation : soundDef.pitchAnimations){
						pitchClocks.put(animation, new DurationDelayClock(animation));
					}
				}
				soundPitchClocks.put(soundDef, pitchClocks);
			}
		}
	}
	
	/**
	 *  Returns the current item for this entity.
	 */
	public <ItemInstance extends AItemPack<JSONDefinition>> ItemInstance getItem(){
		return PackParserSystem.getItem(definition.packID, definition.systemName, subName);
	}
	
	/**
	 *  Generates the default definition for this entity. Used if the item can't be found.
	 *  This allows for internally-definable entities.
	 */
	public JSONDefinition generateDefaultDefinition(){
		throw new IllegalArgumentException("Was asked to auto-generate a definition on an entity with one not defined.  This is NOT allowed.  The entity must be missing its item, but this should be impossible as it shouldn't even have loaded?");
	}
	
    /**
   	 *  Returns true if this entity is lit up, and text should be rendered lit.
   	 *  Note that what text is lit is dependent on the text's definition, so just
   	 *  because text could be lit, does not mean it will be lit if the pack
   	 *  author doesn't want it to be.
   	 */
    public boolean renderTextLit(){
    	return true;
    }
    
    /**
   	 *  Returns a string that represents this entity's secondary text color.  If this color is set,
   	 *  and text is told to render from this provider, and that text is told to use this color, then it will.
   	 *  Otherwise, the text will use its default color.
   	 */
    public String getSecondaryTextColor(){
    	for(JSONSubDefinition subDefinition : definition.definitions){
			if(subDefinition.subName.equals(subName)){
				return subDefinition.secondColor;
			}
		}
		throw new IllegalArgumentException("Tried to get the definition for an object of subName:" + subName + ".  But that isn't a valid subName for the object:" + definition.packID + ":" + definition.systemName + ".  Report this to the pack author as this is a missing JSON component!");
    }
    
    /**
	 *  Called to update the text on this entity.  Normally just sets the text to the passed-in values,
	 *  but may do supplemental logic if desired.
	 */
    public void updateText(List<String> textLines){
    	int linesChecked = 0;
		for(Entry<JSONText, String> textEntry : text.entrySet()){
			textEntry.setValue(textLines.get(linesChecked));
			++linesChecked;
		}
    }
    
    /**
	 *  Gets the renderer for this entity.  No actual rendering should be done in this method, 
	 *  as doing so could result in classes being imported during object instantiation on the server 
	 *  for graphics libraries that do not exist.  Instead, generate a class that does this and call it.
	 *  This method is assured to be only called on clients, so you can just do the construction of the
	 *  renderer in this method and pass it back as the return.
	 */
	public abstract <RendererInstance extends ARenderEntity<AnimationEntity>, AnimationEntity extends AEntityC_Definable<?>> RendererInstance getRenderer();
	
	 /**
	 *  Returns the animator for this entity. Unlike the renderer, animator is used on both
	 *  the client and the server, so all methods inside here need to be server-safe.
	 */
	public abstract <AnimatorInstance extends AAnimationsBase<AnimationEntity>, AnimationEntity extends AEntityC_Definable<?>> AnimatorInstance getAnimator();
    
    @Override
    public void updateSounds(List<SoundInstance> sounds){
    	super.updateSounds(sounds);
    	//Update passed-in sounds to the definitions that created them.    	
    	//If the sound is playing, but shouldn't be, stop it.
    	//If the sound isn't playing, but should be, start it.
		for(SoundInstance sound : sounds){
			for(JSONSound soundDef : soundVolumeClocks.keySet()){
				if(sound.soundName.equals(soundDef.name)){
					//Adjust volume.
					if(!soundDef.volumeAnimations.isEmpty()){
						boolean definedVolume = false;
						sound.volume = 0;
						for(JSONAnimationDefinition animation : soundDef.volumeAnimations){
							if(animation.animationType.equals(AnimationComponentType.TRANSLATION)){
								definedVolume = true;
								sound.volume += Math.signum(animation.axis.z)*getAnimator().getAnimatedVariableValue(this, animation, -animation.offset, soundVolumeClocks.get(soundDef).get(animation), 0) + animation.offset;
							}else if(animation.animationType.equals(AnimationComponentType.ROTATION)){
								definedVolume = true;
								sound.volume += Math.signum(animation.axis.z)*Math.pow(getAnimator().getAnimatedVariableValue(this, animation, -animation.offset, soundVolumeClocks.get(soundDef).get(animation), 0), 2) + animation.offset;
							}
						}
						if(!definedVolume){
							sound.volume = 1;
						}else if(sound.volume < 0){
							sound.volume = 0;
						}
					}
					
					//Adjust pitch.
					if(soundDef.pitchAnimations != null && !soundDef.pitchAnimations.isEmpty()){
						sound.pitch = 0;
						for(JSONAnimationDefinition animation : soundDef.pitchAnimations){
							if(animation.animationType.equals(AnimationComponentType.TRANSLATION)){
								sound.pitch += Math.signum(animation.axis.z)*getAnimator().getAnimatedVariableValue(this, animation, -animation.offset, soundPitchClocks.get(soundDef).get(animation), 0) + animation.offset;
							}else if(animation.animationType.equals(AnimationComponentType.ROTATION)){
								sound.pitch += Math.signum(animation.axis.z)*Math.pow(getAnimator().getAnimatedVariableValue(this, animation, -animation.offset, soundPitchClocks.get(soundDef).get(animation), 0), 2) + animation.offset;
							}
						}
						if(sound.pitch < 0){
							sound.pitch = 0;
						}
					}
					break;
				}
			}
		}
		
		//Next start or stop sounds.
		for(JSONSound soundDef : soundVolumeClocks.keySet()){
			//Check if we should be playing this sound.
			boolean shouldSoundPlay = true;
			for(JSONAnimationDefinition animation : soundDef.volumeAnimations){
				if(animation.animationType.equals(AnimationComponentType.VISIBILITY)){
					//We use the clock here to check if the state of the variable changed, not
					//to clamp the value used in the testing.
					double value = getAnimator().getAnimatedVariableValue(this, animation, 0, null, 0);
					DurationDelayClock clock = soundVolumeClocks.get(soundDef).get(animation);
					clock.getFactoredState(this, value);
					if(value < animation.clampMin || value > animation.clampMax || (!soundDef.looping && !clock.movedThisUpdate)){
						shouldSoundPlay = false;
						break;
					}
				}
			}
			
			if(shouldSoundPlay){
				//Sound should play.  If it's not playing, start it.
				boolean isSoundPlaying = false;
				for(SoundInstance sound : sounds){
					if(sound.soundName.equals(soundDef.name)){
						isSoundPlaying = true;
						break;
					}
				}
				if(!isSoundPlaying){
					InterfaceSound.playQuickSound(new SoundInstance(this, soundDef.name, soundDef.looping));
				}
			}else if(soundDef.looping){
				//If sound is playing, stop it.
				for(SoundInstance sound : sounds){
					if(sound.soundName.equals(soundDef.name)){
						sound.stop();
						break;
					}
				}
			}
		}
    }
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		data.setString("packID", definition.packID);
		data.setString("systemName", definition.systemName);
		data.setString("subName", subName);
		int lineNumber = 0;
		for(String textLine : text.values()){
			data.setString("textLine" + lineNumber++, textLine);
		}
		data.setStrings("variablesOn", variablesOn);
	}
}
