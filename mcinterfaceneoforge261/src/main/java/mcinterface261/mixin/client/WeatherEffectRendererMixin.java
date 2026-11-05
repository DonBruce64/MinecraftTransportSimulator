package mcinterface261.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import mcinterface261.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3D;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherEffectRendererMixin {

    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"))
    private int redirect_extractRenderStateGetHeight(Level world, Heightmap.Types heightmapType, int x, int z) {
        Point3D position = new Point3D(x + 0.5, world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z + 0.5);
        WrapperWorld.getWrapperFor(world).adjustHeightForRain(position);
        return (int) Math.ceil(position.y);
    }

    @Redirect(method = "tickRainParticles", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/multiplayer/ClientLevel;getHeightmapPos(Lnet/minecraft/world/level/levelgen/Heightmap$Types;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos redirect_tickRainParticlesGetHeightmapPos(ClientLevel world, Heightmap.Types heightmapType, BlockPos pos) {
        BlockPos heightmapPos = world.getHeightmapPos(heightmapType, pos);
        Point3D position = new Point3D(heightmapPos.getX() + 0.5, heightmapPos.getY(), heightmapPos.getZ() + 0.5);
        WrapperWorld.getWrapperFor(world).adjustHeightForRain(position);
        //If MTS raised the height, a vehicle roof covers this position.
        //Return a very large Y so the vanilla "heightmapY <= cameraY + 10" check fails → no splash particle inside vehicle.
        return (int) Math.ceil(position.y) > heightmapPos.getY()
            ? BlockPos.containing(pos.getX(), Short.MAX_VALUE, pos.getZ())
            : heightmapPos;
    }
}
