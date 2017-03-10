package minecraftflightsimulator.packets.general;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import minecraftflightsimulator.blocks.TileEntityTrackFake;
import minecraftflightsimulator.minecrafthelpers.BlockHelper;
import net.minecraft.client.Minecraft;

public class TileEntityFakeTrackHeightPacket implements IMessage{
	private int x;
	private int y;
	private int z;
	private float height;

	public TileEntityFakeTrackHeightPacket() {}
	
	public TileEntityFakeTrackHeightPacket(TileEntityTrackFake fake){
		this.x = fake.xCoord;
		this.y = fake.yCoord;
		this.z = fake.zCoord;
		this.height = fake.height;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.x=buf.readInt();
		this.y=buf.readInt();
		this.z=buf.readInt();
		this.height=buf.readFloat();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.x);
		buf.writeInt(this.y);
		buf.writeInt(this.z);
		buf.writeFloat(this.height);
	}

	public static class Handler implements IMessageHandler<TileEntityFakeTrackHeightPacket, IMessage> {
		public IMessage onMessage(TileEntityFakeTrackHeightPacket message, MessageContext ctx){
			TileEntityTrackFake fake;
			if(ctx.side.isServer()){
				fake = (TileEntityTrackFake) BlockHelper.getTileEntityFromCoords(ctx.getServerHandler().playerEntity.worldObj, message.x, message.y, message.z);
				return new TileEntityFakeTrackHeightPacket(fake);
			}else{
				fake = (TileEntityTrackFake) BlockHelper.getTileEntityFromCoords(Minecraft.getMinecraft().theWorld, message.x, message.y, message.z);
				fake.height = message.height;
				return null;
			}
		}
	}	
}
