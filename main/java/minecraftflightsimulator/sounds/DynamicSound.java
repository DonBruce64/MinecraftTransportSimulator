package minecraftflightsimulator.sounds;

import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public class DynamicSound extends MovingSound{
	protected Entity entity;
	protected TileEntity tile;
	
	private DynamicSound(ResourceLocation location, float volume){
		super(location);
		this.volume=volume;
		this.repeat=true;
	}

	public DynamicSound(ResourceLocation location, Entity entity, float volume){
		this(location, volume);
		this.entity = entity;
		this.tile = null;
		this.xPosF=(float) entity.posX;
		this.yPosF=(float) entity.posY;
		this.zPosF=(float) entity.posZ;
	}
	
	public DynamicSound(ResourceLocation location, TileEntity tile, float volume){
		this(location, volume);
		this.entity = null;
		this.tile = tile;
		this.xPosF= tile.xCoord;
		this.yPosF= tile.yCoord;
		this.zPosF= tile.zCoord;
	}
	
	@Override
	public void update(){
		if(this.entity != null){
			this.xPosF=(float) entity.posX;
			this.yPosF=(float) entity.posY;
			this.zPosF=(float) entity.posZ;
		}
	}
}
