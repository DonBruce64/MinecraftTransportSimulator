package minecrafttransportsimulator.blocks.tileentities.instances;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import mcinterface.WrapperNBT;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.rendering.instances.RenderDecor;
import minecrafttransportsimulator.sound.IRadioProvider;
import minecrafttransportsimulator.sound.Radio;
import minecrafttransportsimulator.sound.SoundInstance;

/**Decor tile entity.  Contains the definition so we know how
 * to render this in the TESR call, as well as if we need to do
 * crafting operations if we are a crafting decor type.
 *
 * @author don_bruce
 */
public class TileEntityDecor extends ATileEntityBase<JSONDecor> implements IRadioProvider{
	public final BoundingBox[] boundingBoxes = new BoundingBox[4];
	
	//Internal radio variables.
	private final Radio radio;
	private final FloatBuffer soundPosition;
	private final Point3d soundVelocity = new Point3d(0D, 0D, 0D);
	
	public TileEntityDecor(WrapperWorld world, Point3i position, WrapperNBT data){
		super(world, position, data);
		//Add a bounding box for each rotation.
		boundingBoxes[0] = new BoundingBox(new Point3d(0, 0, 0), definition.general.width/2D, definition.general.height/2D, definition.general.depth/2D);
		boundingBoxes[1] = new BoundingBox(new Point3d(0, 0, 0), definition.general.depth/2D, definition.general.height/2D, definition.general.width/2D);
		boundingBoxes[2] = boundingBoxes[0];
		boundingBoxes[3] = boundingBoxes[1];
		if(definition.general.type != null && definition.general.type.equals("radio")){
			this.soundPosition = ByteBuffer.allocateDirect(3*Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
			soundPosition.put(position.x);
			soundPosition.put(position.y);
			soundPosition.put(position.z);
			soundPosition.flip();
			this.radio = new Radio(this, data);
		}else{
			this.soundPosition = null;
			this.radio = null;
		}
	}
	
	@Override
	public void remove(){
		super.remove();
		if(radio != null){
			radio.stop();
		}
	}
	
	@Override
	public RenderDecor getRenderer(){
		return new RenderDecor();
	}
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		if(radio != null){
			radio.save(data);
		}
	}

	@Override
	public void startSounds(){}

	@Override
	public void updateProviderSound(SoundInstance sound){}

	@Override
	public FloatBuffer getProviderPosition(){
		return soundPosition;
	}

	@Override
	public Point3d getProviderVelocity(){
		return soundVelocity;
	}

	@Override
	public WrapperWorld getProviderWorld(){
		return world;
	}

	@Override
	public Radio getRadio(){
		return radio;
	}
}
