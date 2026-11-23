package mcinterface1165.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;

import minecrafttransportsimulator.mcinterface.IPlayerLimbModelPart;
import net.minecraft.client.renderer.model.ModelRenderer;

@Mixin(ModelRenderer.class)
public abstract class ModelRendererMixin implements IPlayerLimbModelPart {
    @Unique
    private float mtsPlayerLimbLengthScale = 1;
    @Unique
    private float mtsPlayerLimbProximalOffset;
    @Unique
    private float mtsPlayerLimbScaleX = 1;
    @Unique
    private float mtsPlayerLimbScaleY = 1;
    @Unique
    private float mtsPlayerLimbScaleZ = 1;
    @Unique
    private float mtsPlayerLimbScaleAnchorX;
    @Unique
    private float mtsPlayerLimbScaleAnchorY;

    @Override
    public void setPlayerLimbLengthScale(float scale) {
        mtsPlayerLimbLengthScale = scale;
    }

    @Override
    public float getPlayerLimbLengthScale() {
        return mtsPlayerLimbLengthScale;
    }

    @Override
    public void setPlayerLimbScale(float scaleX, float scaleY, float scaleZ) {
        mtsPlayerLimbScaleX = scaleX;
        mtsPlayerLimbScaleY = scaleY;
        mtsPlayerLimbScaleZ = scaleZ;
    }

    @Override
    public float getPlayerLimbScaleX() {
        return mtsPlayerLimbScaleX;
    }

    @Override
    public float getPlayerLimbScaleY() {
        return mtsPlayerLimbScaleY;
    }

    @Override
    public float getPlayerLimbScaleZ() {
        return mtsPlayerLimbScaleZ;
    }

    @Override
    public void setPlayerLimbScaleAnchor(float anchorX, float anchorY) {
        mtsPlayerLimbScaleAnchorX = anchorX;
        mtsPlayerLimbScaleAnchorY = anchorY;
    }

    @Override
    public float getPlayerLimbScaleAnchorX() {
        return mtsPlayerLimbScaleAnchorX;
    }

    @Override
    public float getPlayerLimbScaleAnchorY() {
        return mtsPlayerLimbScaleAnchorY;
    }

    @Override
    public void setPlayerLimbProximalOffset(float offset) {
        mtsPlayerLimbProximalOffset = offset;
    }

    @Override
    public float getPlayerLimbProximalOffset() {
        return mtsPlayerLimbProximalOffset;
    }

    @Inject(method = "translateAndRotate", at = @At("TAIL"))
    private void inject_translateAndRotate(MatrixStack matrixStack, CallbackInfo ci) {
        if (mtsPlayerLimbLengthScale != 1) {
            matrixStack.translate(0, mtsPlayerLimbProximalOffset * (mtsPlayerLimbLengthScale - 1), 0);
            matrixStack.scale(1, mtsPlayerLimbLengthScale, 1);
        }
        if (mtsPlayerLimbScaleX != 1 || mtsPlayerLimbScaleY != 1 || mtsPlayerLimbScaleZ != 1) {
            matrixStack.translate(mtsPlayerLimbScaleAnchorX, mtsPlayerLimbScaleAnchorY, 0);
            matrixStack.scale(mtsPlayerLimbScaleX, mtsPlayerLimbScaleY, mtsPlayerLimbScaleZ);
            matrixStack.translate(-mtsPlayerLimbScaleAnchorX, -mtsPlayerLimbScaleAnchorY, 0);
        }
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void inject_copyFrom(ModelRenderer source, CallbackInfo ci) {
        mtsPlayerLimbLengthScale = ((IPlayerLimbModelPart) (Object) source).getPlayerLimbLengthScale();
        mtsPlayerLimbProximalOffset = ((IPlayerLimbModelPart) (Object) source).getPlayerLimbProximalOffset();
        mtsPlayerLimbScaleX = ((IPlayerLimbModelPart) (Object) source).getPlayerLimbScaleX();
        mtsPlayerLimbScaleY = ((IPlayerLimbModelPart) (Object) source).getPlayerLimbScaleY();
        mtsPlayerLimbScaleZ = ((IPlayerLimbModelPart) (Object) source).getPlayerLimbScaleZ();
        mtsPlayerLimbScaleAnchorX = ((IPlayerLimbModelPart) (Object) source).getPlayerLimbScaleAnchorX();
        mtsPlayerLimbScaleAnchorY = ((IPlayerLimbModelPart) (Object) source).getPlayerLimbScaleAnchorY();
    }
}
