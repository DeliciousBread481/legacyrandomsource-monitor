package deliciousbread481.rsmonitor;

import com.mojang.logging.LogUtils;
import net.minecraftforge.logging.CrashReportAnalyser;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ConcurrentAccessReporter {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SERVER_THREAD = "Server thread";


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
            suspects = CrashReportAnalyser.appendSuspectedMods(new Throwable("off-thread access"), stack);
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