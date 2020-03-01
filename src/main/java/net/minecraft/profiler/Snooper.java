package net.minecraft.profiler;

import CumServer.server.executor.asm.ASMEventExecutorGenerator;
import com.google.common.collect.Maps;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.HttpUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class Snooper
{
    private final Map<String, Object> snooperStats = Maps.<String, Object>newHashMap();
    private final Map<String, Object> clientStats = Maps.<String, Object>newHashMap();
    private final String uniqueID = UUID.randomUUID().toString();
    private final URL serverUrl;
    private final ISnooperInfo playerStatsCollector;
    private final Timer threadTrigger = new Timer("Snooper Timer", true);
    private final Object syncLock = new Object();
    private final long minecraftStartTimeMilis;
    private boolean isRunning;
    private int selfCounter;
    private boolean field_76346_a = false;
    private String field_76346_b;

    public Snooper(String side, ISnooperInfo playerStatCollector, long startTime)
    {
        try
        {
            this.serverUrl = new URL("http://snoop.minecraft.net/" + side + "?version=" + 2);
        }
        catch (MalformedURLException var6)
        {
            throw new IllegalArgumentException();
        }

        this.playerStatsCollector = playerStatCollector;
        this.minecraftStartTimeMilis = startTime;
    }

    public void startSnooper()
    {
        if (!this.isRunning)
        {
            this.isRunning = true;
            this.addOSData();
            this.threadTrigger.schedule(new TimerTask()
            {
                public void run()
                {
                    if (Snooper.this.playerStatsCollector.isSnooperEnabled())
                    {
                        Map<String, Object> map;

                        synchronized (Snooper.this.syncLock)
                        {
                            map = Maps.<String, Object>newHashMap(Snooper.this.clientStats);

                            if (Snooper.this.selfCounter == 0)
                            {
                                map.putAll(Snooper.this.snooperStats);
                            }

                            map.put("snooper_count", Integer.valueOf(Snooper.this.selfCounter++));
                            map.put("snooper_token", Snooper.this.uniqueID);
                        }

                        MinecraftServer minecraftserver = Snooper.this.playerStatsCollector instanceof MinecraftServer ? (MinecraftServer)Snooper.this.playerStatsCollector : null;
                        HttpUtil.postMap(Snooper.this.serverUrl, map, true, minecraftserver == null ? null : minecraftserver.getServerProxy());
                    }
                }
            }, 0L, 900000L);
            this.startCatMetrics();
        }
    }

    private void addOSData()
    {
        this.addJvmArgsToSnooper();
        this.addClientStat("snooper_token", this.uniqueID);
        this.addStatToSnooper("snooper_token", this.uniqueID);
        this.addStatToSnooper("os_name", System.getProperty("os.name"));
        this.addStatToSnooper("os_version", System.getProperty("os.version"));
        this.addStatToSnooper("os_architecture", System.getProperty("os.arch"));
        this.addStatToSnooper("java_version", System.getProperty("java.version"));
        this.addClientStat("version", "1.12.2");
        this.playerStatsCollector.addServerTypeToSnooper(this);
    }

    private void addJvmArgsToSnooper()
    {
        // CumServer start
        try {
            Field field = ASMEventExecutorGenerator.class.getDeclaredField("flag");
            field.setAccessible(true);
            field_76346_a = field.getBoolean(null);
        } catch (Exception e) {}
        // CumServer end
        RuntimeMXBean runtimemxbean = ManagementFactory.getRuntimeMXBean();
        List<String> list = runtimemxbean.getInputArguments();
        int i = 0;

        for (String s : list)
        {
            if (s.startsWith("-X"))
            {
                this.addClientStat("jvm_arg[" + i++ + "]", s);
            }
        }

        this.addClientStat("jvm_args", Integer.valueOf(i));
        field_76346_b = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    }

    public void addMemoryStatsToSnooper()
    {
        this.addStatToSnooper("memory_total", Long.valueOf(Runtime.getRuntime().totalMemory()));
        this.addStatToSnooper("memory_max", Long.valueOf(Runtime.getRuntime().maxMemory()));
        this.addStatToSnooper("memory_free", Long.valueOf(Runtime.getRuntime().freeMemory()));
        this.addStatToSnooper("cpu_cores", Integer.valueOf(Runtime.getRuntime().availableProcessors()));
        this.playerStatsCollector.addServerStatsToSnooper(this);
    }

    public void addClientStat(String statName, Object statValue)
    {
        synchronized (this.syncLock)
        {
            this.clientStats.put(statName, statValue);
        }
    }

    public void addStatToSnooper(String statName, Object statValue)
    {
        synchronized (this.syncLock)
        {
            this.snooperStats.put(statName, statValue);
        }
    }

    @SideOnly(Side.CLIENT)
    public Map<String, String> getCurrentStats()
    {
        Map<String, String> map = Maps.<String, String>newLinkedHashMap();

        synchronized (this.syncLock)
        {
            this.addMemoryStatsToSnooper();

            for (Entry<String, Object> entry : this.snooperStats.entrySet())
            {
                map.put(entry.getKey(), entry.getValue().toString());
            }

            for (Entry<String, Object> entry1 : this.clientStats.entrySet())
            {
                map.put(entry1.getKey(), entry1.getValue().toString());
            }

            return map;
        }
    }

    public boolean isSnooperRunning()
    {
        return this.isRunning;
    }

    public void stopSnooper()
    {
        this.threadTrigger.cancel();
    }

    @SideOnly(Side.CLIENT)
    public String getUniqueID()
    {
        return this.uniqueID;
    }

    public long getMinecraftStartTimeMillis()
    {
        return this.minecraftStartTimeMilis;
    }

    private void startCatMetrics() {
        MinecraftServer mcServer = (MinecraftServer) playerStatsCollector;
    }
}