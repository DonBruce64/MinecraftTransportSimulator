package mcinterface1165.mixin.client;

import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.client.audio.Listener;
import net.minecraft.util.math.vector.Vector3f;

@Mixin(Listener.class)
public abstract class ListenerMixin {
    private final Point3D forwards = new Point3D();
    private final Point3D up = new Point3D();

    /**
     * Need this to adjust rotation in roll for listener properties if we are riding a vehicle since MC doesn't support this.
     */
    @Inject(method = "setListenerOrientation", at = @At(value = "TAIL"))
    public void inject_setListenerOrientation(Vector3f pClientViewVector, Vector3f pViewVectorRaised, CallbackInfo ci) {
        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
        if (player != null) {
            AEntityB_Existing playerRiding = player.getEntityRiding();
            if (playerRiding != null) {
                forwards.set(0, 0, 1).rotate(playerRiding.orientation).rotate(playerRiding.riderRelativeOrientation);
                up.set(0, 1, 0).rotate(playerRiding.orientation).rotate(playerRiding.riderRelativeOrientation);
                AL10.alListenerfv(AL10.AL_ORIENTATION, new float[] { (float) forwards.x, (float) forwards.y, (float) forwards.z, (float) up.x, (float) up.y, (float) up.z });
            }
        }
    }
}
