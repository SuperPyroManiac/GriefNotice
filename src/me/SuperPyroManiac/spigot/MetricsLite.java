package me.SuperPyroManiac.spigot;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.scheduler.BukkitTask;

public class MetricsLite
{

  private final Plugin plugin;
  private final YamlConfiguration configuration;
  private final File configurationFile;
  private final String guid;
  private final boolean debug;
  private final Object optOutLock = new Object();
  private volatile BukkitTask task = null;
  
  public MetricsLite(Plugin plugin)
    throws IOException
  {
    if (plugin == null) {
      throw new IllegalArgumentException("Plugin cannot be null");
    }
    this.plugin = plugin;
    
    this.configurationFile = getConfigFile();
    this.configuration = YamlConfiguration.loadConfiguration(this.configurationFile);
    
    this.configuration.addDefault("opt-out", Boolean.valueOf(false));
    this.configuration.addDefault("guid", UUID.randomUUID().toString());
    this.configuration.addDefault("debug", Boolean.valueOf(false));
    if (this.configuration.get("guid", null) == null)
    {
      this.configuration.options().header("http://mcstats.org").copyDefaults(true);
      this.configuration.save(this.configurationFile);
    }
    this.guid = this.configuration.getString("guid");
    this.debug = this.configuration.getBoolean("debug", false);
  }
  
  public boolean start()
  {
    synchronized (this.optOutLock)
    {
      if (isOptOut()) {
        return false;
      }
      if (this.task != null) {
        return true;
      }
      this.task = this.plugin.getServer().getScheduler().runTaskTimerAsynchronously(this.plugin, new Runnable()
      {
        private boolean firstPost = true;
        
        public void run()
        {
          try
          {
            synchronized (MetricsLite.this.optOutLock)
            {
              if ((MetricsLite.this.isOptOut()) && (MetricsLite.this.task != null))
              {
                MetricsLite.this.task.cancel();
                MetricsLite.this.task = null;
              }
            }
            MetricsLite.this.postPlugin(!this.firstPost);
            
            this.firstPost = false;
          }
          catch (IOException e)
          {
            if (MetricsLite.this.debug) {
              Bukkit.getLogger().log(Level.INFO, "[Metrics] " + e.getMessage());
            }
          }
        }
      }, 0L, 18000L);
      
      return true;
    }
  }
  
  public boolean isOptOut()
  {
    synchronized (this.optOutLock)
    {
      try
      {
        this.configuration.load(getConfigFile());
      }
      catch (IOException ex)
      {
        if (this.debug) {
          Bukkit.getLogger().log(Level.INFO, "[Metrics] " + ex.getMessage());
        }
        return true;
      }
      catch (InvalidConfigurationException ex)
      {
        if (this.debug) {
          Bukkit.getLogger().log(Level.INFO, "[Metrics] " + ex.getMessage());
        }
        return true;
      }
      return this.configuration.getBoolean("opt-out", false);
    }
  }
  
  public void enable()
    throws IOException
  {
    synchronized (this.optOutLock)
    {
      if (isOptOut())
      {
        this.configuration.set("opt-out", Boolean.valueOf(false));
        this.configuration.save(this.configurationFile);
      }
      if (this.task == null) {
        start();
      }
    }
  }
  
  public void disable()
    throws IOException
  {
    synchronized (this.optOutLock)
    {
      if (!isOptOut())
      {
        this.configuration.set("opt-out", Boolean.valueOf(true));
        this.configuration.save(this.configurationFile);
      }
      if (this.task != null)
      {
        this.task.cancel();
        this.task = null;
      }
    }
  }
  
  public File getConfigFile()
  {
    File pluginsFolder = this.plugin.getDataFolder().getParentFile();
    
    return new File(new File(pluginsFolder, "PluginMetrics"), "config.yml");
  }
  
  private void postPlugin(boolean isPing)
    throws IOException
  {
    PluginDescriptionFile description = this.plugin.getDescription();
    String pluginName = description.getName();
    boolean onlineMode = Bukkit.getServer().getOnlineMode();
    String pluginVersion = description.getVersion();
    String serverVersion = Bukkit.getVersion();
    //int playersOnline = Bukkit.getServer().getOnlinePlayers().length; This has been broken since 1.6
    
    StringBuilder json = new StringBuilder(1024);
    json.append('{');
    
    appendJSONPair(json, "guid", this.guid);
    appendJSONPair(json, "plugin_version", pluginVersion);
    appendJSONPair(json, "server_version", serverVersion);
    //appendJSONPair(json, "players_online", Integer.toString(playersOnline)); - If issues happen, this is where.
    
    String osname = System.getProperty("os.name");
    String osarch = System.getProperty("os.arch");
    String osversion = System.getProperty("os.version");
    String java_version = System.getProperty("java.version");
    int coreCount = Runtime.getRuntime().availableProcessors();
    if (osarch.equals("amd64")) {
      osarch = "x86_64";
    }
    appendJSONPair(json, "osname", osname);
    appendJSONPair(json, "osarch", osarch);
    appendJSONPair(json, "osversion", osversion);
    appendJSONPair(json, "cores", Integer.toString(coreCount));
    appendJSONPair(json, "auth_mode", onlineMode ? "1" : "0");
    appendJSONPair(json, "java_version", java_version);
    if (isPing) {
      appendJSONPair(json, "ping", "1");
    }
    json.append('}');
    
    URL url = new URL("http://report.mcstats.org" + String.format("/plugin/%s", new Object[] { urlEncode(pluginName) }));
    URLConnection connection;
    if (isMineshafterPresent()) {
      connection = url.openConnection(Proxy.NO_PROXY);
    } else {
      connection = url.openConnection();
    }
    byte[] uncompressed = json.toString().getBytes();
    byte[] compressed = gzip(json.toString());
    
    connection.addRequestProperty("User-Agent", "MCStats/7");
    connection.addRequestProperty("Content-Type", "application/json");
    connection.addRequestProperty("Content-Encoding", "gzip");
    connection.addRequestProperty("Content-Length", Integer.toString(compressed.length));
    connection.addRequestProperty("Accept", "application/json");
    connection.addRequestProperty("Connection", "close");
    
    connection.setDoOutput(true);
    if (this.debug) {
      System.out.println("[Metrics] Prepared request for " + pluginName + " uncompressed=" + uncompressed.length + " compressed=" + compressed.length);
    }
    OutputStream os = connection.getOutputStream();
    os.write(compressed);
    os.flush();
    
    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    String response = reader.readLine();
    
    os.close();
    reader.close();
    if ((response == null) || (response.startsWith("ERR")) || (response.startsWith("7")))
    {
      if (response == null) {
        response = "null";
      } else if (response.startsWith("7")) {
        response = response.substring(response.startsWith("7,") ? 2 : 1);
      }
      throw new IOException(response);
    }
  }
  
  /* Error */
  public static byte[] gzip(String input)
  {
	return null;
    // Byte code:
    //   0: new 480	java/io/ByteArrayOutputStream
    //   3: dup
    //   4: invokespecial 482	java/io/ByteArrayOutputStream:<init>	()V
    //   7: astore_1
    //   8: aconst_null
    //   9: astore_2
    //   10: new 483	java/util/zip/GZIPOutputStream
    //   13: dup
    //   14: aload_1
    //   15: invokespecial 485	java/util/zip/GZIPOutputStream:<init>	(Ljava/io/OutputStream;)V
    //   18: astore_2
    //   19: aload_2
    //   20: aload_0
    //   21: ldc_w 488
    //   24: invokevirtual 490	java/lang/String:getBytes	(Ljava/lang/String;)[B
    //   27: invokevirtual 492	java/util/zip/GZIPOutputStream:write	([B)V
    //   30: goto +42 -> 72
    //   33: astore_3
    //   34: aload_3
    //   35: invokevirtual 493	java/io/IOException:printStackTrace	()V
    //   38: aload_2
    //   39: ifnull +46 -> 85
    //   42: aload_2
    //   43: invokevirtual 496	java/util/zip/GZIPOutputStream:close	()V
    //   46: goto +39 -> 85
    //   49: astore 5
    //   51: goto +34 -> 85
    //   54: astore 4
    //   56: aload_2
    //   57: ifnull +12 -> 69
    //   60: aload_2
    //   61: invokevirtual 496	java/util/zip/GZIPOutputStream:close	()V
    //   64: goto +5 -> 69
    //   67: astore 5
    //   69: aload 4
    //   71: athrow
    //   72: aload_2
    //   73: ifnull +12 -> 85
    //   76: aload_2
    //   77: invokevirtual 496	java/util/zip/GZIPOutputStream:close	()V
    //   80: goto +5 -> 85
    //   83: astore 5
    //   85: aload_1
    //   86: invokevirtual 497	java/io/ByteArrayOutputStream:toByteArray	()[B
    //   89: areturn
    // Line number table:
    //   Java source line #363	-> byte code offset #0
    //   Java source line #364	-> byte code offset #8
    //   Java source line #367	-> byte code offset #10
    //   Java source line #368	-> byte code offset #19
    //   Java source line #369	-> byte code offset #30
    //   Java source line #370	-> byte code offset #34
    //   Java source line #372	-> byte code offset #38
    //   Java source line #373	-> byte code offset #42
    //   Java source line #374	-> byte code offset #46
    //   Java source line #371	-> byte code offset #54
    //   Java source line #372	-> byte code offset #56
    //   Java source line #373	-> byte code offset #60
    //   Java source line #374	-> byte code offset #64
    //   Java source line #376	-> byte code offset #69
    //   Java source line #372	-> byte code offset #72
    //   Java source line #373	-> byte code offset #76
    //   Java source line #374	-> byte code offset #80
    //   Java source line #378	-> byte code offset #85
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	90	0	input	String
    //   7	79	1	baos	java.io.ByteArrayOutputStream
    //   9	68	2	gzos	java.util.zip.GZIPOutputStream
    //   33	2	3	e	IOException
    //   54	16	4	localObject	Object
    //   49	1	5	localIOException1	IOException
    //   67	1	5	localIOException2	IOException
    //   83	1	5	localIOException3	IOException
    // Exception table:
    //   from	to	target	type
    //   10	30	33	java/io/IOException
    //   42	46	49	java/io/IOException
    //   10	38	54	finally
    //   60	64	67	java/io/IOException
    //   76	80	83	java/io/IOException
  }
  
  private boolean isMineshafterPresent()
  {
    try
    {
      Class.forName("mineshafter.MineServer");
      return true;
    }
    catch (Exception e) {}
    return false;
  }
  
  private static void appendJSONPair(StringBuilder json, String key, String value)
    throws UnsupportedEncodingException
  {
    boolean isValueNumeric = false;
    try
    {
      if ((value.equals("0")) || (!value.endsWith("0")))
      {
        Double.parseDouble(value);
        isValueNumeric = true;
      }
    }
    catch (NumberFormatException e)
    {
      isValueNumeric = false;
    }
    if (json.charAt(json.length() - 1) != '{') {
      json.append(',');
    }
    json.append(escapeJSON(key));
    json.append(':');
    if (isValueNumeric) {
      json.append(value);
    } else {
      json.append(escapeJSON(value));
    }
  }
  
  private static String escapeJSON(String text)
  {
    StringBuilder builder = new StringBuilder();
    
    builder.append('"');
    for (int index = 0; index < text.length(); index++)
    {
      char chr = text.charAt(index);
      switch (chr)
      {
      case '"': 
      case '\\': 
        builder.append('\\');
        builder.append(chr);
        break;
      case '\b': 
        builder.append("\\b");
        break;
      case '\t': 
        builder.append("\\t");
        break;
      case '\n': 
        builder.append("\\n");
        break;
      case '\r': 
        builder.append("\\r");
        break;
      default: 
        if (chr < ' ')
        {
          String t = "000" + Integer.toHexString(chr);
          builder.append("\\u" + t.substring(t.length() - 4));
        }
        else
        {
          builder.append(chr);
        }
        break;
      }
    }
    builder.append('"');
    
    return builder.toString();
  }
  
  private static String urlEncode(String text)
    throws UnsupportedEncodingException
  {
    return URLEncoder.encode(text, "UTF-8");
  }
}
