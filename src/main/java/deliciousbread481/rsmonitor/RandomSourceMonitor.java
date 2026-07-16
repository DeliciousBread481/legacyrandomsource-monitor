package deliciousbread481.rsmonitor;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(RandomSourceMonitor.MODID)
public class RandomSourceMonitor {

    public static final String MODID = "rsmonitor";
    private static final Logger LOGGER = LogUtils.getLogger();

    public RandomSourceMonitor(FMLJavaModLoadingContext context) {
        LOGGER.info("[RSMonitor] LegacyRandomSource thread monitor loaded. " +
                "将在非 Server thread 并发访问随机源时输出 WARN 日志。");
    }
}