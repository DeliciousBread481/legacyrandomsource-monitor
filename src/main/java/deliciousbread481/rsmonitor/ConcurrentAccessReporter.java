package deliciousbread481.rsmonitor;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.forgespi.language.IModInfo;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ConcurrentAccessReporter {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SERVER_THREAD = "Server thread";

    private static final Map<String, IModInfo> PACKAGE_MOD_CACHE = new HashMap<>();
    private static volatile boolean cached = false;

    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

    private ConcurrentAccessReporter() {}

    public static void onAccess(String site) {
        Thread current = Thread.currentThread();
        String threadName = current.getName();

        if (SERVER_THREAD.equals(threadName)) {
            return;
        }

        StackTraceElement[] stack = current.getStackTrace();

        String key = threadName + "|" + topFrames(stack, 4);
        if (!REPORTED.add(key)) {
            return;
        }

        String suspects;
        try {
            suspects = resolveSuspectedMods(stack);
        } catch (Throwable t) {
            suspects = "（分析失败: " + t + "）";
        }

        LOGGER.warn(
            "\n============================================================\n" +
            "[RSMonitor] 检测到在非 Server thread 上访问受保护资源!\n" +
            "  访问点   : {}\n" +
            "  线程     : {}\n" +
            "  疑似模组 : {}\n" +
            "  堆栈     :\n{}" +
            "============================================================",
            site, threadName, suspects.isBlank() ? "（未匹配到任何模组包名）" : suspects, formatStack(stack)
        );
    }

    private static String resolveSuspectedMods(StackTraceElement[] stack) {
        cacheModList();

        Set<IModInfo> suspected = new LinkedHashSet<>();
        for (StackTraceElement e : stack) {
            IModInfo mod = findMod(e.getClassName());
            if (mod != null) {
                suspected.add(mod);
            }
        }

        if (suspected.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (IModInfo mod : suspected) {
            sb.append("\n\t")
              .append(mod.getDisplayName())
              .append(" (").append(mod.getModId()).append("),")
              .append(" Version: ").append(mod.getVersion());
        }
        return sb.toString();
    }

    private static void cacheModList() {
        if (cached) {
            return;
        }
        ModList modList = ModList.get();
        ModuleLayer gameLayer = FMLLoader.getGameLayer();
        if (modList == null) {
            return;
        }

        modList.getMods().forEach(iModInfo -> {
            if (!iModInfo.getModId().equals("forge") && !iModInfo.getModId().equals("minecraft")) {
                Set<String> packages = new HashSet<>();
                gameLayer.findModule(iModInfo.getModId())
                        .ifPresent(module -> packages.addAll(module.getPackages()));
                packages.forEach(s -> PACKAGE_MOD_CACHE.put(s, iModInfo));
            }
        });
        cached = true;
    }

    private static IModInfo findMod(String className) {
        for (Map.Entry<String, IModInfo> entry : PACKAGE_MOD_CACHE.entrySet()) {
            if (className.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String topFrames(StackTraceElement[] stack, int n) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (StackTraceElement e : stack) {
            String cn = e.getClassName();
            if (cn.startsWith("deliciousbread481.rsmonitor")
                    || cn.startsWith("java.")
                    || cn.startsWith("jdk.")) {
                continue;
            }
            if (count++ >= n) break;
            sb.append(cn).append('#').append(e.getMethodName()).append(';');
        }
        return sb.toString();
    }

    private static String formatStack(StackTraceElement[] stack) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement e : stack) {
            sb.append("\t\tat ").append(e).append('\n');
        }
        return sb.toString();
    }
}