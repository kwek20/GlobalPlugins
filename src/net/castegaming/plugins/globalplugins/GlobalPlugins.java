/**
 * 
 */
package net.castegaming.plugins.globalplugins;

import java.io.File;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Attributes.Name;
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
	private GlobalConfig config;
	private GlobalCommands commandHandler;
	
	@Override
	public void onLoad() {
		plugin = this;
		config = new GlobalConfig(this);
		loadPlugins();
	}
	
	@Override
	public void onEnable() {
		log("Current globalPlugins folder at:");
		log(config.getPluginsDir());
		commandHandler = new GlobalCommands(this);
		enabledPlugins();
		
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

			@Override
			public void run() {
				List<File> plugins = new LinkedList<File>();
				for (File f : plugin.getDataFolder().getParentFile().listFiles()){
					if (!f.isDirectory() && !f.isHidden() && f.getName().endsWith(".jar")){
						plugins.add(f);
					}
				}
				for (File f : new File(config.getPluginsDir()).listFiles()){
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
	}
	
	private void loadPlugins() {
		if (!config.hasPlugins()){
			log("There are no plugins found to load :(");
			log("Place some plugins in the globalPlugins folder!");
		} else {
			log(config.getEnabledPlugins().size() + " plugin(s) found! Loading them now.");
			List<String> noDependencyfound = new LinkedList<String>();
			for (String name : config.getEnabledPlugins()){
				String notEnabled = loadPlugin(name);
				if (notEnabled != null){
					noDependencyfound.add(name);
				}
			}
			for (String name : noDependencyfound){
				try {
					Bukkit.getServer().getPluginManager().loadPlugin(new File(config.getPluginsDir() + name));
					log("Enabled plugin: " + name);
				} catch (UnknownDependencyException | InvalidPluginException | InvalidDescriptionException e) {
					log("Could stil not enable " + name + " due to an unknown dependency :(. Stacktrace follows.");
					e.printStackTrace();
				}
			}
		}
	}
	
	private void enabledPlugins() {
		if (!config.hasPlugins()){
			log("There are no plugins found to load :(");
			log("Place some plugins in the globalPlugins folder!");
		} else {
			log(config.getEnabledPlugins().size() + " plugin(s) found! Loading them now.");
			List<String> noDependencyfound = new LinkedList<String>();
			for (String name : config.getEnabledPlugins()){
				String notEnabled = enablePlugin(name);
				if (notEnabled != null){
					noDependencyfound.add(name);
				}
			}
			final List<String> errorPlugins = new LinkedList<String>(noDependencyfound);
			
			Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				
				@Override
				public void run() {
					for (String name : errorPlugins){
						try {
							Bukkit.getServer().getPluginManager().loadPlugin(new File(config.getPluginsDir() + name));
							log(ChatColor.DARK_GREEN + "Enabled plugindfhcd: " + name);
						} catch (UnknownDependencyException | InvalidPluginException | InvalidDescriptionException e) {
							log(ChatColor.DARK_RED + "Could stil not enable " + name + " due to an unknown dependency :(. Stacktrace follows.");
							e.printStackTrace();
						}
					}
				}
			}, 1l);
			
		}
	}

	@Override
	public void onDisable() {
		
	}
	
	public String loadPlugin(String name){
		return loadPlugin(new File(config.getPluginsDir() + name + ".jar"));
	}
	
	private String loadPlugin(File file) {
		String name = file.getName().substring(0, file.getName().length()-4);
		try {
			Bukkit.getServer().getPluginManager().loadPlugin(file);
			log(ChatColor.DARK_GREEN + "loaded plugin: " +  name);
			return null;
		} catch (UnknownDependencyException | InvalidPluginException | InvalidDescriptionException e) {
			log(ChatColor.DARK_RED + "Failed to load plugin: " + name + " :(");
			e.printStackTrace();
			if (e.getMessage().startsWith("java.io.FileNotFoundException")){
				log(ChatColor.DARK_RED + "Detected removal of " + name + ". Removing from the list!");
				config.removePlugin(name);
			} else {
				e.printStackTrace();
			}
			return plugin.getName();
		}
	}

	public String enablePlugin(String name){
		return enablePlugin(new File(config.getPluginsDir() + name + ".jar"));
	}
	
	public String enablePlugin(File file){
		String name = file.getName().substring(0, file.getName().length()-4);
		String realname = config.getDescription(file).getName();
		
		if (Bukkit.getServer().getPluginManager().getPlugin(realname) == null){
			loadPlugin(file);
		}
		try {
			Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin(config.getDescription(file).getName());
			if (plugin != null){
				if (!config.useGlobalConfig(name)){
					try {
						for (Field f : plugin.getClass().getSuperclass().getDeclaredFields()){
							f.setAccessible(true);
							if (f.getName().equals("dataFolder")){
								f.set(plugin, new File(getDataFolder().getParentFile().getAbsolutePath() + File.separator + name));
								break;
							}
						}
						if (Bukkit.getServer().getPluginManager().getPlugin(realname) != null){
							Bukkit.getServer().getPluginManager().enablePlugin(plugin);
							log(ChatColor.DARK_GREEN + "enabled plugin: " +  name);
						}
					} catch (SecurityException | IllegalArgumentException | IllegalAccessException | NullPointerException e) {
						log("Error code 1337! Contact plugin dev immideately about " + e.getLocalizedMessage());
						e.printStackTrace();
					}
				} else {
					log(ChatColor.DARK_GREEN + "enabled plugin: " +  name);
					Bukkit.getServer().getPluginManager().enablePlugin(plugin);
				}
			} else {
				log(ChatColor.DARK_RED + "Failed to enable plugin: " + name + " :(");
			}
			
		} catch (UnknownDependencyException e) {
			log(ChatColor.DARK_RED + "Could not enable " + name + " due to an unknown dependency. Will try again on the end. Stacktrace follows.");
			e.printStackTrace();
			return name;
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
	
	public String getPrefix(){
		return prefix;
	}
}
