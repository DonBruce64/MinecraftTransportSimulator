package mcinterface262.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import mcinterface262.InterfaceClient;
import net.minecraft.client.OptionInstance;

@Mixin(OptionInstance.class)
public abstract class OptionInstanceMixin<T> implements InterfaceClient.Ifov {
    @Shadow
    private OptionInstance.ValueUpdateListener<? super T> onValueUpdate;
    @Shadow
    private T value;

    /**
     * Need this to force lower fov.  Vanilla clamps us at 30 so we get the fov variable and force the lower values.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setManual(Integer value) {
        this.value = (T) value;
        onValueUpdate.valueChanged((T) value);
    }
}
