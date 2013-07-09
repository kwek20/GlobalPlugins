package net.castegaming.plugins.globalplugins;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.yaml.snakeyaml.error.YAMLException;

public class GlobalConfig {

	GlobalPlugins plugin;
	FileConfiguration config;
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
				plugin.log(ChatColor.DARK_GREEN + "Created default config file.");
			} catch (SecurityException e) {
				plugin.log(ChatColor.DARK_RED + "Could not create the default config file! Is the disk full?");
			}
		} else if (!config.getBoolean("scannedFiles", false)){
			scan();
			config.set("scannedFiles", true);
			saveConfig();
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
					plugin.enablePlugin(file);
					found++;
				}
			}
			
			saveConfig();
		}
		return found;
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
			
			System.out.println(folderAboveServer);
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
		return config.getConfigurationSection("plugins").getKeys(false);
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
}
