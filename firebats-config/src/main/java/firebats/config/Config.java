package firebats.config;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.CharSource;
import com.google.common.io.CharStreams;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConcurrentMapConfiguration;

import firebats.properties.IProperties;
import firebats.properties.PropertyFactory;
/**
 * org.apache.commons.configuration.Configuration的封装类，除了初始化需要commons-configuration知识外，使用时使用此最简化接口
 */
public class Config implements IProperties{
	@SuppressWarnings("unused") private static Logger logger = LoggerFactory.getLogger(Config.class);

	/*internal*/ ConcurrentCompositeConfiguration config;
	private PropertyFactory factory;

	public static Config Default=newEmpty();
	private Config() {
		this(new ConcurrentCompositeConfiguration());
	}
	private Config(Configuration config) {
		this.config=new ConcurrentCompositeConfiguration();
		this.config.addConfiguration(new ConcurrentMapConfiguration(config));
		this.factory= PropertyFactory.of(this);
	}
	/**
	 * test use
	 */
	public static Config newEmpty() {
		Config result=new Config();
		return result;
	}
	public void clear() {
		config.clear();
	}
	
	public Config getConfig(String configName) {
		return new Config(config.getConfiguration(configName));
	}
	public Config addFromFile(String configName, Path filePath) {
		Preconditions.checkNotNull(filePath,"filePath should not be null");
		return addFromFile(configName,filePath.toFile());
	}
	public Config addFromFile(String configName, String filePath) {
		Preconditions.checkNotNull(filePath,"filePath should not be null");
 		return addFromFile(configName,new File(filePath));
	}
 	public Config addFromFile(String configName, File file) {
		Preconditions.checkNotNull(file,"file should not be null");
		Preconditions.checkArgument(file.isFile(),"file[%s] not exists",file);
		PropertiesConfiguration p = createPropertiesConfiguration();
		p.setFile(file);
		try {
			p.load();
		} catch (ConfigurationException e) {
			throw new RuntimeException("config add error["+configName+"]: "+file,e);
		}
		return add(configName, p);
	}
	public Config addFromResource(String configName, URL url) {
		PropertiesConfiguration p = createPropertiesConfiguration();
		p.setURL(url);
		try {
			p.load();
		} catch (ConfigurationException e) {
			throw new RuntimeException("config add error["+configName+"]: "+url,e);
		}
		return add(configName, p);
	}
	public Config addFromCharSource(String configName, CharSource charSource) {
   		try {
			return addFromString(configName, charSource.read());
		} catch (IOException e) {
			throw new RuntimeException("config add error["+configName+"]",e);
		}
	}
	public Config addFromString(String configName, String content) {
		try(StringReader reader=new StringReader(content==null?"":content)) {
			PropertiesConfiguration p = createPropertiesConfiguration();
			try {
				p.load(reader);
			} catch (ConfigurationException e) {
				throw new RuntimeException("config add error["+configName+"]",e);
			}
			add(configName, p);
			return this;
		}
	}
	public Config addFromReader(String configName, Reader reader) {
		try {
			return addFromString(configName,CharStreams.toString(reader));
		} catch (IOException e) {
			throw new RuntimeException("config add error["+configName+"]",e);
		}
	}
	public Config add(String configName,Config config){
		return add(configName,config.config);
	}
	public Config add(String configName,Map<String,Object> properties){
		Preconditions.checkNotNull(configName);
		Preconditions.checkNotNull(properties);
		return add(configName,new ConcurrentMapConfiguration(properties));
	}
	public Config add(String configName,Properties properties){
		Preconditions.checkNotNull(configName);
		Preconditions.checkNotNull(properties);
		return add(configName,new ConcurrentMapConfiguration(new MapConfiguration(properties)));
	}
	private Config add(String configName,Configuration configuration){
		Preconditions.checkNotNull(configName);
		Preconditions.checkNotNull(configuration);
		config.addConfiguration(new ConcurrentMapConfiguration(configuration),configName);
		return this;
	}
	@Override
	public String get(String key) {
  		return config.getString(key);
	}
	@Override
	public void put(String key, String value) {
		config.setProperty(key, value);
	}
	public Iterable<String> getKeys(){
 		return Lists.newArrayList(config.getKeys());
 	}
 	/**变量未解析过的所有原始配置*/
 	public Map<String,Object> getOriginal(){
        Map<String,Object> result=new LinkedHashMap<>();
        for (Iterator<?> i = config.getKeys(); i.hasNext();) {
            String name =  (String) i.next();
            Object value = config.getProperty(name);
			result.put(name, value==null?null:value.toString());
        }
        return result;
 	}
 	/**变量解析过的所有配置*/
 	public Map<String,Object> getResloved(){
        Map<String,Object> result=new LinkedHashMap<>();
        for (Iterator<?> i = config.getKeys(); i.hasNext();) {
            String name =  (String) i.next();
            String value = config.getString(name);
			result.put(name, value);
        }
        return result;
 	}
 	
    public void writeToPropertiesFile(String filename){
    	writeToPropertiesFile(new File(filename));
    }
    public void writeToPropertiesFile(File file){
    	ensureDir(file);
    	PropertiesConfiguration p = new PropertiesConfiguration();
    	p.setDelimiterParsingDisabled(false);
    	for (Entry<String,Object> property : getResloved().entrySet()) {
			p.addProperty(property.getKey(), property.getValue());
		}
    	try {
			p.save(file);
		} catch (ConfigurationException e) {
			Throwables.propagate(e);
		}
    }

	public void writeToPropertiesFile(Path path) {
		writeToPropertiesFile(path.toFile());
	}
	private void ensureDir(File file) {
		File parent = file.getParentFile();
		if (parent != null&&!parent.exists()) {
			parent.mkdirs();
		}
	}

	public boolean containsKey(String configName) {
 		return config.containsKey(configName);
	}
	
	public Properties toProperties(){
		return config.getProperties();
	}
	public PropertyFactory getPropertyFactory() {
 		return factory;
	}
	private PropertiesConfiguration createPropertiesConfiguration() {
		PropertiesConfiguration p=new PropertiesConfiguration();
		p.setEncoding(Charsets.UTF_8.name());
		p.setDelimiterParsingDisabled(true);
		return p;
	}

}