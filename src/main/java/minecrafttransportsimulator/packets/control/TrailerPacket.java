package minecrafttransportsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.packets.general.PacketChat;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Ground;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class TrailerPacket implements IMessage{	
	private int id;

	public TrailerPacket() { }
	
	public TrailerPacket(int id){
		this.id=id;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
	}

	public static class Handler implements IMessageHandler<TrailerPacket, IMessage>{
		public IMessage onMessage(final TrailerPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityVehicleF_Ground vehicle;
					if(ctx.side.isServer()){
						vehicle = (EntityVehicleF_Ground) ctx.getServerHandler().player.world.getEntityByID(message.id);
					}else{
						vehicle = (EntityVehicleF_Ground) Minecraft.getMinecraft().world.getEntityByID(message.id);
					}
					if(vehicle!=null){
						if(vehicle.towedVehicle != null){
							vehicle.towedVehicle.towedByVehicle = null;
							vehicle.towedVehicle = null;
							if(ctx.side.isServer()){
								MTS.MTSNet.sendToAll(message);
								MTS.MTSNet.sendTo(new PacketChat("interact.trailer.disconnect"), ctx.getServerHandler().player);
							}
						}else if(vehicle.definition.motorized.hitchPos != null){
							for(Entity entity : vehicle.world.loadedEntityList){
								if(entity instanceof EntityVehicleF_Ground){
									EntityVehicleF_Ground testVehicle = (EntityVehicleF_Ground) entity;
									if(testVehicle.definition.motorized.hookupPos != null){
										//Make sure clients hitch vehicles that the server sees.  Little more lenient here.
										Vec3d hitchOffset = new Vec3d(vehicle.definition.motorized.hitchPos[0], vehicle.definition.motorized.hitchPos[1], vehicle.definition.motorized.hitchPos[2]);
										Vec3d hitchPos = RotationSystem.getRotatedPoint(hitchOffset, vehicle.rotationPitch, vehicle.rotationYaw, vehicle.rotationRoll).add(vehicle.getPositionVector());
										Vec3d hookupOffset = new Vec3d(testVehicle.definition.motorized.hookupPos[0], testVehicle.definition.motorized.hookupPos[1], testVehicle.definition.motorized.hookupPos[2]);
										Vec3d hookupPos = RotationSystem.getRotatedPoint(hookupOffset, testVehicle.rotationPitch, testVehicle.rotationYaw, testVehicle.rotationRoll).add(testVehicle.getPositionVector());
										if(hitchPos.distanceTo(hookupPos) < (ctx.side.isServer() ? 2 : 3)){
											for(String hitchType : vehicle.definition.motorized.hitchTypes){
												if(hitchType.equals(testVehicle.definition.motorized.hookupType)){
													testVehicle.towedByVehicle = vehicle;
													vehicle.towedVehicle = testVehicle;
													if(ctx.side.isServer()){
														MTS.MTSNet.sendToAll(message);
														MTS.MTSNet.sendTo(new PacketChat("interact.trailer.connect"), ctx.getServerHandler().player);
													}
													return;
												}
											}
											MTS.MTSNet.sendTo(new PacketChat("interact.trailer.wronghitch"), ctx.getServerHandler().player);
											return;
										}
									}
								}
							}
							MTS.MTSNet.sendTo(new PacketChat("interact.trailer.notfound"), ctx.getServerHandler().player);
						}else{
							if(ctx.side.isServer()){
								MTS.MTSNet.sendTo(new PacketChat("interact.trailer.nohitch"), ctx.getServerHandler().player);
							}
						}
					}
				}
			});
			return null;
		}
	}
}