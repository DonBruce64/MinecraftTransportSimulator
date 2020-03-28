package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.packets.components.APacketVehicle;
import minecrafttransportsimulator.sound.Radio;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;

/**Packet used to change radio state.  Sent from clients to the server
 * to notify it about the radio state change, and then sent to all clients
 * tracking the vehicle.
 * 
 * @author don_bruce
 */
public class PacketVehicleRadioChange extends APacketVehicle{
	private Radio.RadioSources source;
	private byte presetIndex;
	private float[] equalizerBands;
	
	public PacketVehicleRadioChange(EntityVehicleE_Powered vehicle){
		super(vehicle);
		Radio radio = vehicle.getRadio();
		source = radio.source;
		presetIndex = radio.presetIndex;
		equalizerBands = new float[radio.equalizer.getBandCount()];
		for(int i=0; i<equalizerBands.length; ++ i){
			equalizerBands[i] = radio.equalizer.getBand(i);
		}
	}
	
	public PacketVehicleRadioChange(ByteBuf buf){
		super(buf);
		source = Radio.RadioSources.values()[buf.readByte()];
		presetIndex = buf.readByte();
		equalizerBands = new float[32];
		for(int i=0; i<equalizerBands.length; ++ i){
			equalizerBands[i] = buf.readFloat();
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeByte(source.ordinal());
		buf.writeByte(presetIndex);
		for(int i=0; i<equalizerBands.length; ++ i){
			buf.writeFloat(equalizerBands[i]);
		}
	}
	
	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleE_Powered vehicle){
		Radio radio = vehicle.getRadio();
		if(world.isClient()){
			//Check source changes first.
			//If we didn't change a source, check preset changes.
			//If we didn't change that either, we must have stopped.
			if(!radio.source.equals(source)){
				radio.changeSource(source);
			}else{
				if(presetIndex != -1){
					radio.pressPreset(presetIndex);
				}else{
					radio.stop();
				}
			}
		}else{
			radio.source = source;
			radio.presetIndex = presetIndex;
		}
		for(int i=0; i<equalizerBands.length; ++ i){
			radio.equalizer.setBand(i, equalizerBands[i]);
		}
		return true;
	}
}
