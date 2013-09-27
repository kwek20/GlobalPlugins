/**
 * 
 */
package net.castegaming.plugins.globalplugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Brord
 *
 */
public class GlobalPlugins extends JavaPlugin{
	
	private static String prefix = ChatColor.GREEN + "[" + ChatColor.DARK_RED + "GlobalPlugins" + ChatColor.GREEN + "] " + ChatColor.RESET;
	private static GlobalPlugins plugin;
	protected GlobalConfig config;
	private GlobalCommands commandHandler;
	
	@Override
	public void onLoad() {
		plugin = this;
		config = new GlobalConfig(this);
		checkFileNames();
		//modifyBukkit();
		if (config.hasPlugins()){
			loadPlugins(true);
			enabledPlugins(true);
		}
	}
	
	private void modifyBukkit() {
		
		String path = Bukkit.class.getProtectionDomain().getCodeSource().getLocation().getPath().replaceAll("%20", " ");
		String name = new File(Bukkit.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName(); 
		JarOutputStream tempJar = null;
		try {
			JarFile file = new JarFile(path);
			tempJar = new JarOutputStream(new FileOutputStream(path.replace(".jar", "_1.jar")));
			
			for (Enumeration<JarEntry> entryList = file.entries(); entryList.hasMoreElements();){
				JarEntry entry = entryList.nextElement();
				
				InputStream filereader = file.getInputStream(entry);
				tempJar.putNextEntry(new JarEntry(entry.getName()));
				
				System.out.println(entry.getName());
				
				for(int data = filereader.read(new byte[4096]); data != -1; data = filereader.read(new byte[4096])){
					System.out.println("Wrote: " + data + " to " + entry.getName());
					tempJar.write(data); 
				} 
			}
			
			tempJar.putNextEntry(new JarEntry("org\\bukkit\\plugin\\java\\JavaPluginLoader.class"));
			InputStream stream = GlobalPlugins.class.getClassLoader().getResourceAsStream("net\\castegaming\\plugins\\globalplugins\\JavaPluginLoader.class");
			
					
					
			tempJar.close();
		} catch (IOException e1) {
			e1.printStackTrace();
			
			if (tempJar != null){
//				try {
//					tempJar.close();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
			}
		}
		
		try {
			JarInputStream jar = new JarInputStream(new FileInputStream(path));
		} catch (IOException e) {
			e.printStackTrace();
			log("Could not acces " + name);
		}
		
		ClassLoader loader = Bukkit.class.getClassLoader();
		System.out.println(Bukkit.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		System.out.println(new File(Bukkit.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName());
	}

	@Override
	public void onEnable() {
		log("Current globalPlugins folder at:");
		log(config.getPluginsDir());
		commandHandler = new GlobalCommands(this);
		if (config.hasPlugins()){
			loadPlugins(false);
			enabledPlugins(false);
			log(ChatColor.GOLD + "All done!");
			
			Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				@Override
				public void run() {
					List<File> plugins = new LinkedList<File>();
					for (File f : plugin.getDataFolder().getParentFile().listFiles()){
						if (!f.isDirectory() && !f.isHidden() && f.getName().endsWith(".jar")){
							plugins.add(f);
						}
					}
					for (File f : config.getFiles()){
						if (!f.isDirectory() && !f.isHidden() && f.getName().endsWith(".jar")){
							plugins.add(f);
						}
					}
					for (File f : plugins){
						if (config.getDescription(f) != null){
							String name = config.getDescription(f).getName();
							if (Bukkit.getServer().getPluginManager().getPlugin(name) == null){
								loadPlugin(f);
								enablePlugin(f);
							}
						}
					}
				}
			}, 20l);
		} else {
			log(ChatColor.DARK_RED + "There are no plugins found to load :(");
			log(ChatColor.DARK_RED + "Place some plugins in the globalPlugins folder!");
		}
	}
	
	/**
	 * 
	 * @param startup
	 */
	private void loadPlugins(boolean startup) {
		if (!config.hasPlugins()){
			log("There are no plugins found to load :(");
			log("Place some plugins in the globalPlugins folder!");
		} else {
			List<File> files = null;
			if (startup){
				files = config.getLoadOrder(config.getSTARTUPFiles());
			} else {
				files = config.getLoadOrder(config.getLOADfiles());
			}
			
			for (File f : files){
				String notLoaded = loadPlugin(f);
				if (notLoaded != null){
					log("Could not load " + notLoaded + " :(");
				}
			}
		}
	}
	
	/**
	 * 
	 * @param startup
	 */
	private void enabledPlugins(boolean startup) {
		if (!config.hasPlugins()){
			log("There are no plugins found to enable :(");
			log("Place some plugins in the globalPlugins folder!");
		} else {
			List<File> files = null;
			if (startup){
				files = config.getLoadOrder(config.getSTARTUPFiles());
			} else {
				files = config.getLoadOrder(config.getLOADfiles());
			}
			
			log(ChatColor.GOLD + "" + files.size() + ChatColor.DARK_AQUA + " plugin(s) found! Enabling them now.");
			for (File f : files){
				String notEnabled = enablePlugin(f);
				if (notEnabled != null){
					log("Could not enable " + notEnabled + " :(");
				}
			}
		}
	}
	
	/**
	 * 
	 * @param name
	 * @return
	 */
	public String loadPlugin(String name){
		return loadPlugin(new File(config.getPluginsDir() + name + ".jar"));
	}
	
	/**
	 * 
	 * @param file
	 * @return
	 */
	public String loadPlugin(File file) {
		String name = file.getName().substring(0, file.getName().length()-4);
		boolean succeeded = false;
		try {
			Bukkit.getServer().getPluginManager().loadPlugin(file);
			String realname = config.getDescription(file).getName();
			if (!config.useGlobalConfig(name)){
				try {
					Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin(realname);
					if (plugin != null){
						if (!config.useGlobalConfig(name)){
							try {
								for (Field f : plugin.getClass().getSuperclass().getDeclaredFields()){
									if (f.getName().equals("dataFolder")){
										f.setAccessible(true);
										f.set(plugin, new File(getDataFolder().getParentFile().getAbsolutePath() + File.separator + realname));
										break;
									}
								}
							} catch (SecurityException | IllegalArgumentException | IllegalAccessException | NullPointerException e) {
								log("Error code 1337! Contact plugin dev immideately about " + e.getLocalizedMessage());
								e.printStackTrace();
							}
						}
					} else {
						log(ChatColor.DARK_RED + "Failed to load plugin: " + name + " :(");
					}
					
				} catch (UnknownDependencyException e) {
					log(ChatColor.DARK_RED + "Could not load " + name + " due to an unknown dependency. Will try again on the end. Stacktrace follows.");
					e.printStackTrace();
					return name;
				}
			}
			succeeded = true;
		} catch (UnknownDependencyException | InvalidPluginException | InvalidDescriptionException e) {
			log(ChatColor.DARK_RED + "Failed to load plugin: " + name + " :(");
			log("Error: " + e.getMessage());
			return plugin.getName();
		}
		
		if (succeeded){
			//log(ChatColor.DARK_GREEN + "loaded plugin: " +  name);
			return null;
		} else {
			return name;
		}
	}

	public String enablePlugin(String name){
		return enablePlugin(new File(config.getPluginsDir() + name + ".jar"));
	}
	
	public String enablePlugin(File file){
		if (!file.exists() || file == null){
			return "";
		}
		String name = file.getName().substring(0, file.getName().length()-4);
		
		if (config.getDescription(file) == null || config.getDescription(file).getName() == null){
			return name;
		}
		String realname = config.getDescription(file).getName();
		
		if (Bukkit.getServer().getPluginManager().getPlugin(realname) == null){
			loadPlugin(file);
		}
		
		Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin(realname);
		if (plugin != null){
			if (!config.useGlobalConfig(name)){
				try {
					for (Field f : plugin.getClass().getSuperclass().getDeclaredFields()){
						if (f.getName().equals("dataFolder")){
							f.setAccessible(true);
							f.set(plugin, new File(getDataFolder().getParentFile().getAbsolutePath() + File.separator + realname));
							break;
						}
					}
				} catch (SecurityException | IllegalArgumentException | IllegalAccessException | NullPointerException e) {
					log("Error code 1337! Contact plugin dev immideately about " + e.getLocalizedMessage());
					e.printStackTrace();
				}
			}
			Bukkit.getServer().getPluginManager().enablePlugin(plugin);
			if (Bukkit.getServer().getPluginManager().getPlugin(realname).isEnabled()){
				log(ChatColor.DARK_GREEN + "enabled plugin: " +  realname);
			}else {
				log(ChatColor.DARK_RED + "OOPS! Somethign went wrong :(. Plugin is NOT enabled");
			}
		} else {
			log(ChatColor.DARK_RED + "Failed to enable plugin: " + name + " :(");
		}
		return null;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		return commandHandler.command(sender, command, args);
	}

	public static GlobalPlugins get(){
		return plugin;
	}
	
	public GlobalConfig getGlobalConfig(){
		return config;
	}
	
	public void log(String message) {
		try {
			Bukkit.getServer().getConsoleSender().sendMessage(prefix + "" +  message);
		} catch (Exception e){ 
			this.getLogger().log(Level.INFO, message);
		}
	}
	

	private void checkFileNames() {
		for (File f : new File(config.getPluginsDir()).listFiles()){
			if (!f.isDirectory() && !f.isHidden() && f.getName().endsWith("jar")){
				String name = f.getName().substring(0, f.getName().length()-4);
				if (name.indexOf(".") > -1){
					String nameFix = name.replaceAll("\\.", "_");
					log("Replaced the file " + f.getName() + " to " + nameFix + ".jar");
					f.renameTo(new File(f.getAbsolutePath().replace(f.getName(), nameFix + ".jar")));
				}
			}
		}
	}
	
	public String getPrefix(){
		return prefix;
	}
}
