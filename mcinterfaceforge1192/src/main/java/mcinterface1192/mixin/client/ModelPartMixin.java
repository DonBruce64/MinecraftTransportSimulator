package mcinterface1192.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import minecrafttransportsimulator.rendering.IPlayerLimbModelPart;
import net.minecraft.client.model.geom.ModelPart;

@Mixin(ModelPart.class)
public abstract class ModelPartMixin implements IPlayerLimbModelPart {
    @Unique
    private float mts$playerLimbLengthScale = 1;
    @Unique
    private float mts$playerLimbProximalOffset;
    @Unique
    private float mts$playerLimbScaleX = 1;
    @Unique
    private float mts$playerLimbScaleY = 1;
    @Unique
    private float mts$playerLimbScaleZ = 1;
    @Unique
    private float mts$playerLimbScaleAnchorX;
    @Unique
    private float mts$playerLimbScaleAnchorY;

    @Override
    public void setPlayerLimbLengthScale(float scale) {
        mts$playerLimbLengthScale = scale;
    }

    @Override
    public float getPlayerLimbLengthScale() {
        return mts$playerLimbLengthScale;
    }

    @Override
    public void setPlayerLimbScale(float scaleX, float scaleY, float scaleZ) {
        mts$playerLimbScaleX = scaleX;
        mts$playerLimbScaleY = scaleY;
        mts$playerLimbScaleZ = scaleZ;
    }

    @Override
    public float getPlayerLimbScaleX() {
        return mts$playerLimbScaleX;
    }

    @Override
    public float getPlayerLimbScaleY() {
        return mts$playerLimbScaleY;
    }

    @Override
    public float getPlayerLimbScaleZ() {
        return mts$playerLimbScaleZ;
    }

    @Override
    public void setPlayerLimbScaleAnchor(float anchorX, float anchorY) {
        mts$playerLimbScaleAnchorX = anchorX;
        mts$playerLimbScaleAnchorY = anchorY;
    }

    @Override
    public float getPlayerLimbScaleAnchorX() {
        return mts$playerLimbScaleAnchorX;
    }

    @Override
    public float getPlayerLimbScaleAnchorY() {
        return mts$playerLimbScaleAnchorY;
    }

    @Override
    public void setPlayerLimbProximalOffset(float offset) {
        mts$playerLimbProximalOffset = offset;
    }

    @Override
    public float getPlayerLimbProximalOffset() {
        return mts$playerLimbProximalOffset;
    }

    @Inject(method = "translateAndRotate", at = @At("TAIL"))
    private void inject_translateAndRotate(PoseStack poseStack, CallbackInfo ci) {
        if (mts$playerLimbLengthScale != 1) {
            poseStack.translate(0, mts$playerLimbProximalOffset * (mts$playerLimbLengthScale - 1), 0);
            poseStack.scale(1, mts$playerLimbLengthScale, 1);
        }
        if (mts$playerLimbScaleX != 1 || mts$playerLimbScaleY != 1 || mts$playerLimbScaleZ != 1) {
            poseStack.translate(mts$playerLimbScaleAnchorX, mts$playerLimbScaleAnchorY, 0);
            poseStack.scale(mts$playerLimbScaleX, mts$playerLimbScaleY, mts$playerLimbScaleZ);
            poseStack.translate(-mts$playerLimbScaleAnchorX, -mts$playerLimbScaleAnchorY, 0);
        }
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void inject_copyFrom(ModelPart source, CallbackInfo ci) {
        mts$playerLimbLengthScale = ((IPlayerLimbModelPart) (Object) source).getPlayerLimbLengthScale();
        mts$playerLimbProximalOffset = ((IPlayerLimbModelPart) (Object) source).getPlayerLimbProximalOffset();
        mts$playerLimbScaleX = ((IPlayerLimbModelPart) (Object) source).getPlayerLimbScaleX();
        mts$playerLimbScaleY = ((IPlayerLimbModelPart) (Object) source).getPlayerLimbScaleY();
        mts$playerLimbScaleZ = ((IPlayerLimbModelPart) (Object) source).getPlayerLimbScaleZ();
        mts$playerLimbScaleAnchorX = ((IPlayerLimbModelPart) (Object) source).getPlayerLimbScaleAnchorX();
        mts$playerLimbScaleAnchorY = ((IPlayerLimbModelPart) (Object) source).getPlayerLimbScaleAnchorY();
    }
}
