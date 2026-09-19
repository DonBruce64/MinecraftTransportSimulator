package mcinterface261.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import mcinterface261.InterfaceLoader;
import mcinterface261.MtsRiderRenderState;
import mcinterface261.WrapperEntity;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.PartSeat;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
    private static ItemStack mts_heldStackHolder = null;
    private static final Point3D mts_entityScale = new Point3D();
    private static final RotationMatrix mts_bodyOrientation = new RotationMatrix();

    /**
     * In MC 26.1's render-state system, entity data is extracted into a render state object
     * before rendering. We hook here to:
     * 1) Temporarily remove the held item for gun-holders (so the state captures an empty hand).
     * 2) Reset the per-frame transformation flag so it is never stale.
     */
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("HEAD"))
    public void inject_extractPre(T entity, S state, float partialTicks, CallbackInfo ci) {
        MtsRiderRenderState.clear(state);
        if (entity instanceof Player) {
            EntityPlayerGun gunEntity = EntityPlayerGun.playerClientGuns.get(entity.getUUID());
            if (gunEntity != null && gunEntity.activeGun != null) {
                Player player = (Player) entity;
                mts_heldStackHolder = player.getMainHandItem();
                player.getInventory().setItem(player.getInventory().getSelectedSlot(), ItemStack.EMPTY);
            }
        }
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    public void inject_extractPost(T entity, S state, float partialTicks, CallbackInfo ci) {
        if (mts_heldStackHolder != null) {
            Player player = (Player) entity;
            player.getInventory().setItem(player.getInventory().getSelectedSlot(), mts_heldStackHolder);
            mts_heldStackHolder = null;
        }

        WrapperEntity entityWrapper = WrapperEntity.getWrapperFor(entity);
        AEntityB_Existing ridingEntity = entityWrapper.getEntityRiding();
        if (ridingEntity != null) {
            ridingEntity.getInterpolatedOrientation(mts_bodyOrientation, partialTicks);
            mts_bodyOrientation.convertToAngles();
            if (ridingEntity instanceof PartSeat) {
                PartSeat seat = (PartSeat) ridingEntity;
                mts_entityScale.set(seat.riderScale);
                if (seat.definition.seat.playerScale != null) {
                    mts_entityScale.multiply(seat.definition.seat.playerScale);
                }
                if (seat.placementDefinition.playerScale != null) {
                    mts_entityScale.multiply(seat.placementDefinition.playerScale);
                }
            } else {
                mts_entityScale.set(1, 1, 1);
            }

            state.yRot = (float) -ridingEntity.riderRelativeOrientation.convertToAngles().y;
            state.xRot = (float) ridingEntity.riderRelativeOrientation.angles.x;
            state.bodyRot = 0;

            TransformationMatrix m = MtsRiderRenderState.put(state);
            m.applyTranslation(0, -entityWrapper.getSeatOffset(), 0);
            m.applyRotation(mts_bodyOrientation);
            m.applyScaling(mts_entityScale);
            m.applyTranslation(0, entityWrapper.getSeatOffset(), 0);

        }
    }

    /**
     * Push a new pose scope and mul T into it at the very start of submit().
     *
     * Using HEAD / RETURN boundary injects instead of @At INVOKE pushPose shift=AFTER
     * because NeoForge's transformer wraps the method prologue (RenderPlayerEvent.Pre cancel
     * check) around the first pushPose() call; the shift=AFTER site becomes unreachable when
     * the cancel-path short-circuits, which Mixin silently accepts (no InvalidInjectionException)
     * but never actually executes. Method-boundary injects are immune to this.
     *
     * Matrix chain:
     *   M_outer * T * (vanilla: S * Y(180) * Sm * T_anchor) * v
     * — identical to the working 1211 chain; T is world-aligned, vehicle yaw pivots correctly.
     *
     * Leak safety: we own the pushPose/popPose pair; vanilla's inner pushPose is inside ours;
     * the dispatcher's outer popPose is outside ours. Zero risk of stack imbalance.
     */
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At("HEAD"))
    public void inject_submitHead(S state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        MtsRiderRenderState.pushed = false;
        TransformationMatrix m = MtsRiderRenderState.get(state);
        if (m != null) {
            MtsRiderRenderState.applyTo(poseStack, m);
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At("RETURN"))
    public void inject_submitReturn(S state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (MtsRiderRenderState.pushed) {
            poseStack.popPose();
            MtsRiderRenderState.pushed = false;
        }
    }
}
