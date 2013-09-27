package net.castegaming.plugins.globalplugins;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoadOrder;
import org.yaml.snakeyaml.error.YAMLException;

public class GlobalConfig {

	GlobalPlugins plugin;
	private FileConfiguration config;
	private String DATAFOLDER;
	
	public GlobalConfig(GlobalPlugins plugin) {
		this.plugin = plugin;
		DATAFOLDER = plugin.getDataFolder() + File.separator;
		loadConfig();
	}
	
	public void loadConfig() {
		config = plugin.getConfig();
		File f = new File(DATAFOLDER + "config.yml");
		if (!f.exists()){
			try {
				plugin.saveDefaultConfig();
				config = plugin.getConfig();
				plugin.log(ChatColor.DARK_GREEN + "Created default config file.");
			} catch (SecurityException e) {
				plugin.log(ChatColor.DARK_RED + "Could not create the default config file! Is the disk full?");
			}
		} else if (!config.getBoolean("scannedFiles", false)){
			scan();
			config.set("scannedFiles", true);
			saveConfig();
		}
		
		for (String name : getEnabledPlugins()){
			File file = new File(getPluginsDir() + name + ".jar");
			if (!file.exists()){
				plugin.log("Detected removal of: " + name + " Removing!");
				removePlugin(name);
			}
		}
	}
	
	/**
	 * Runs a scan for new plugins in the jar, and enables them<br/>
	 * This will cause the config to be set to every plugin in the folder.<br/>
	 * Previously removed plugins from the config will thus be re-added
	 * @return
	 */
	public int scan(){
		int found = 0;
		File dir = new File(getPluginsDir());
		List<File> files = new LinkedList<File>();
		if (dir.exists()){
			for (File file : dir.listFiles()){
				int i = file.getName().lastIndexOf(".");
				int p = Math.max(file.getName().lastIndexOf("/"), file.getName().lastIndexOf("\\"));
				String extension = "";
				if (i > p){
					extension = file.getName().substring(i+1);
				}
				if (!file.isDirectory() && !file.isHidden() && extension.equals("jar") && !config.contains("plugins." + file.getName().substring(0, i))){
					config.set("plugins." + file.getName().substring(0, i) + ".globalconfig", false);
					files.add(file);
					found++;
				}
			}
			saveConfig();
		}
		List<File> orderedFiles = getLoadOrder(files);
		for (File f : orderedFiles){
			plugin.loadPlugin(f);
		}
		for (File f : orderedFiles){
			plugin.enablePlugin(f);
		}
		
		return found;
	}
	
	public List<File> getSTARTUPFiles(){
		List<File> files = getFiles();
		files.removeAll(getLOADfiles());
		System.out.println(Arrays.toString(files.toArray()));
		return files;
	}
	
	public List<File> getLOADfiles(){
		List<File> files = getFiles();
		for (int i=files.size()-1; i>=0; i--){
			PluginDescriptionFile desc = getDescription(files.get(i));
			if (desc != null && desc.getLoad().equals(PluginLoadOrder.STARTUP)){
				files.remove(files.get(i));
			}
		}
		return files;
	}
	
	public List<File> getLoadOrder(List<File> files){
		if (files == null || files.isEmpty()){
			return new LinkedList<File>();
		}
		
		List<String> names = new LinkedList<String>();
		
		for (File f : files){
			PluginDescriptionFile desc = getDescription(f);
			if (desc != null){
				if (desc.getName() != null){
					names.add(desc.getName());
				}
			}
		}
		
		for (File f : files){
			PluginDescriptionFile desc = getDescription(f);
			if (desc != null){
				String name = desc.getName();
				if (desc.getDepend() != null && name != null){
					List<String> dependsList = new LinkedList<String>(desc.getDepend());
					if (desc.getSoftDepend() != null){
						dependsList.addAll(desc.getSoftDepend());
					}
					
					for (String depends : dependsList){
						if (names.contains(depends)){
							if (names.indexOf(depends) > names.indexOf(name)){
								names.remove(depends);
								names.add(0, depends);
							}
						}
					}
				}
			}
		}
		
		List<File> orderedFiles = new LinkedList<File>();
		for (String name : names){
			for (File f : files){
				PluginDescriptionFile desc = getDescription(f);
				if (desc != null && desc.getName().equals(name)){
					orderedFiles.add(f);
				}
			}
		}
		
		return orderedFiles;
	}
	
	public int indexOf(LinkedHashMap<String, Integer> map, String value){
		if (map.containsKey(value)){
			int index = 0;
			for (String key : map.keySet()){
				if (key.equals(value)){
					return index;
				} else {
					index++;
				}
			}
		}
		return -1;
	}

	public String getPluginsDir() {
		if (config.getString("pluginsDir") != null){
			if (!new File(config.getString("pluginsDir")).exists()){
				if (!new File(config.getString("pluginsDir")).mkdir()){
					//cannot create folder
					plugin.log(ChatColor.DARK_RED + "Could not create the folder:");
					plugin.log(ChatColor.DARK_RED + config.getString("pluginsDir"));
				}
			}
			return config.getString("pluginsDir");
		} else {
			String pluginsFolder = plugin.getDataFolder().getParentFile().getAbsolutePath();
			String serverfolder = pluginsFolder.substring(0, pluginsFolder.length() - 8);
			String folderAboveServer = (serverfolder.substring(0, serverfolder.lastIndexOf(File.separator)) + File.separator);
			
			//System.out.println(folderAboveServer);
			//                                     plugins         server          dir above      globalPlugins   
			String folder = folderAboveServer + "globalplugins" + File.separator;
			File f = new File(folder);
			if (!f.exists()){
				//folder doesnt exist
				if (!f.mkdir()){
					//cannot create folder
					plugin.log(ChatColor.DARK_RED + "Could not create a default globalPlugins folder! using our own directory.");
					folder = plugin.getDataFolder().getPath();
				}
			}
			config.set("pluginsDir", folder);
			saveConfig();
			return folder;
		}
		
	}

	public Set<String> getEnabledPlugins() {
		if (config.getConfigurationSection("plugins") != null){
			return config.getConfigurationSection("plugins").getKeys(false);
		} else {
			return new HashSet<String>();
		}
	}
	
	public boolean hasPlugins() {
		return !getEnabledPlugins().isEmpty();
	}
	
	public void saveConfig(){
		try {
			config.save(DATAFOLDER + "config.yml");
		} catch (IOException e) {
			plugin.log(ChatColor.DARK_RED + "Could not save the config file! Is the disk full?");
		}
	}
	
	public boolean useGlobalConfig(String name){
		if (!config.contains("plugins." + name)){
			return false;
		} else {
			return config.getBoolean("plugins." + name);
		}
	}

	public void removePlugin(String name) {
		if (config.contains("plugins." + name)){
			config.set("plugins." + name, null);
		}
		saveConfig();
	}

	public PluginDescriptionFile getDescription(File file) {
		Validate.notNull(file, "File cannot be null");

        JarFile jar = null;
        InputStream stream = null;

        try {
            jar = new JarFile(file);
            JarEntry entry = jar.getJarEntry("plugin.yml");

            if (entry == null) {
                return null;
            }

            stream = jar.getInputStream(entry);

            return new PluginDescriptionFile(stream);

        } catch (IOException ex) {
            //
        } catch (YAMLException ex) {
            //
        } catch (InvalidDescriptionException e) {
			// 
		} finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException e) {
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
        
        return null;
	}

	public List<File> getFiles() {
		List<File> files = new LinkedList<File>();
		for (String name : getEnabledPlugins()){
			files.add(new File(getPluginsDir() + name + ".jar"));
		}
		return files;
	}

	public void refreshConfig() {
		config = plugin.getConfig();
	}
}
