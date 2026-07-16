package deliciousbread481.rsmonitor.mixin;

import deliciousbread481.rsmonitor.ConcurrentAccessReporter;
import net.minecraft.util.ThreadingDetector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThreadingDetector.class)
public class ThreadingDetectorMixin {

    @Inject(method = "checkAndLock", at = @At("HEAD"))
    private void rsmonitor$onCheckAndLock(CallbackInfo ci) {
        ConcurrentAccessReporter.onAccess("ThreadingDetector#checkAndLock");
    }
}