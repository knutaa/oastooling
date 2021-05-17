package no.paneon.api.tooling;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;

import no.paneon.api.logging.AspectLogger.LogLevel;
import no.paneon.api.logging.LogMethod;
import no.paneon.api.utils.Out;
import no.paneon.api.utils.Utils;

import no.paneon.api.conformance.ConfSpecException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;

public class Config {

    static Logger logger;

    private static final List<String> configFiles = new LinkedList<>();
    
    public static void setConfigSources(List<String> files) {
    	configFiles.addAll(files);
    	forceConfig();
    }
    
    private static boolean skipInternalConfiguration=false;
    public static void setSkipInternalConfiguration(boolean val) {
    	skipInternalConfiguration = val;
    }
    
    private static JSONObject json = new JSONObject();

	@LogMethod(level=LogLevel.TRACE)
    public static void getConfig() {
    	logger = LogManager.getLogger(Config.class);
       	Config.init();	
    }
    
	@LogMethod(level=LogLevel.TRACE)
    public static void forceConfig() {
    	initStatus=false;
    	getConfig();
    }
    
    private Config() {    	
    }
    
	@LogMethod(level=LogLevel.TRACE)
	public static void usage() {				
		if(json!=null) {
			Out.println(
					"Default configuration json (--config option):" + "\n" + 
					json.toString(2)
					);
			Out.println();
		}
	}
	
    private static boolean initStatus = false;
    
    private static void init() {
    	if(initStatus) return;
    	initStatus = true;

    	try {
    		InputStream is ;
    		if(!skipInternalConfiguration) {
    			is = new ClassPathResource("configuration.json").getInputStream();
    			addConfiguration(is,"configuration.json");    			
    		} 
    		
    		for(String file : configFiles) {
    			Out.println("... adding configuration from file " + file);

    			is = Utils.openFileStream(workingDirectory, file);
    			
    			if(is!=null) {
    				addConfiguration(is,file);
    			} else {
    				Out.println("Error processng configuration file: " + file + " not found");
    			}
    		}
    		  	    
    		
    		Config.readRules();
    	    
		} catch (Exception e) {
			Out.println("Error processing configuration files: " + e);
			System.exit(1);
		}
    }
    
	private static String workingDirectory; 
	
	@LogMethod(level=LogLevel.TRACE)
	public static void setWorkingDirectory(String directory) {
		workingDirectory = directory;
	}
	
	@LogMethod(level=LogLevel.TRACE)
    private static void readRules() {
    	
		logger.log(Level.TRACE, "readRules: rulesSource={}", rulesSource);

    	if(rulesSource==null) return;
    	
    	try {
			JSONObject o = Utils.readYamlAsJSON(rulesSource,true);
			// next level, only one containing api attribute
			rules=o.getJSONObject(o.keySet().iterator().next());
			
			if(logger.isDebugEnabled())
				logger.log(Level.DEBUG, "setRulesSource: rules={}", rules.toString(2));

		} catch(Exception e) {
			Out.println("... unable to read rules from " + rulesSource);
			
			if(logger.isDebugEnabled())
				logger.log(Level.DEBUG, "setRulesSource: exception={}", e.getLocalizedMessage());
		}		
	}

	@LogMethod(level=LogLevel.TRACE)
	private static void addConfiguration(InputStream is, String name) throws ConfSpecException {
		try {
		    String config = IOUtils.toString(is, StandardCharsets.UTF_8.name());
		    
		    if(name.endsWith("yaml")) config = Utils.convertYamlToJson(config);
		    
		    JSONObject deltaJSON = new JSONObject(config); 
		    
		    addConfiguration(deltaJSON);
		} catch(Exception ex) {
			throw(new ConfSpecException());
		}
   	
	}

	@LogMethod(level=LogLevel.TRACE)
	public static void addConfiguration(JSONObject deltaJSON) {  	 		
	    for(String key : deltaJSON.keySet()) {	    	
	    	json.put(key, deltaJSON.get(key));
	    }	   	
	}
	
	@LogMethod(level=LogLevel.TRACE)
	private static JSONObject getConfiguration() {  	 		
		return json;   	
	}
	
	public static boolean has(String property) {
		init();
		return json!=null && json.has(property);
	}
	
	@LogMethod(level=LogLevel.TRACE)
	public static List<String> get(String property) {
		try {
			JSONArray array = json.optJSONArray(property);
			return array.toList().stream().map(Object::toString).collect(Collectors.toList());
		} catch(Exception e) {
			return new LinkedList<>();
		}
	}
	
	@LogMethod(level=LogLevel.TRACE)
	public static boolean getBoolean(String property) {
		return json.optBoolean(property);
	}

	@LogMethod(level=LogLevel.TRACE)
	public static String getString(String property) {
		return json.optString(property);
	}

	@LogMethod(level=LogLevel.TRACE)
	public static Map<String,String> getStringMap(String property) {
		Map<String,String> res = new HashMap<>();

		JSONObject obj = json.optJSONObject(property);			
		if(obj != null) {
			obj.keySet().forEach(key -> res.put(key, obj.get(key).toString()) );
		}

		return res;
	}

	@LogMethod(level=LogLevel.TRACE)
	public static JSONObject getObject(String property) {
		return json.optJSONObject(property);
	}

	@LogMethod(level=LogLevel.TRACE)
	public static JSONArray getArray(String property) {
		return json.optJSONArray(property);
	}

	private static String rulesSource=null;
	private static JSONObject rules=null;
	
	@LogMethod(level=LogLevel.TRACE)
	public static void setRulesSource(String rules) {
		
		if(rules==null) return;
		
		rulesSource=rules;
		readRules();
	}
	
	@LogMethod(level=LogLevel.TRACE)
	public static JSONObject getRules() {
		if(rules==null && rulesSource!=null) readRules();
		return rules;
	}

	@LogMethod(level=LogLevel.TRACE)
	public static List<String> getStrings(String ... args) {
		List<String> res = new LinkedList<>();
		JSONObject o = getObject(args[0]);
		
		int i=1;
		while(i<args.length-1 && o!=null) {
			o = o.optJSONObject(args[i]);
			
			if(logger.isDebugEnabled())	
				logger.log(Level.DEBUG, "getStrings: i={} args={} o={}", i, args[i], ((o!=null) ? o.toString(2) : "null"));

			i++;
		}
		
		if(o==null) return res;

		if(o.optJSONArray(args[i])!=null) {
			res = o.optJSONArray(args[i]).toList().stream().map(Object::toString).collect(Collectors.toList());
			
			if(logger.isDebugEnabled())
				logger.log(Level.DEBUG, "getStrings: res={}", Utils.dump(res));
		}
		
		return res;
	}

	@LogMethod(level=LogLevel.TRACE)
	public static List<String> getStringsByPath(String path, String element) {
		List<String> res = new LinkedList<>();
				
		try {
			Object o = getObjectByPath(getConfiguration(),path);
			if(o instanceof JSONObject) {
				JSONObject jo = (JSONObject)o;
				if(jo.optJSONArray(element)!=null) {
					res = jo.optJSONArray(element).toList().stream().map(Object::toString).collect(Collectors.toList());
					
					if(logger.isDebugEnabled())
						logger.log(Level.DEBUG, "getStringsByPath: res={}", Utils.dump(res));
				}
			}
			
		} catch(Exception e) {
			
			logger.log(Level.DEBUG, "getStringsByPath: exception={}", e.getLocalizedMessage());
			
		}
		
		return res;
	}
	
	@LogMethod(level=LogLevel.TRACE)
	public static Object getObjectByPath(JSONObject config, String path) {
		Object res = null;
		
		if(logger.isDebugEnabled()) {
			logger.log(Level.DEBUG, "getObjectByPath: path={} config keys={}", path, config.keySet());
		}
		
		try {
			String[] parts = path.split("[.]");
			
			String main = parts[0];
			if(config.has(main)) {
				res = config.opt(main);
				if((res instanceof JSONObject) && parts.length>1) {
					String subPath = path.substring(main.length()+1);
					
					logger.log(Level.DEBUG, "getObjectByPath: main={} subPath={}", main, subPath);

					res = getObjectByPath((JSONObject)res, subPath);
				} 
			}
		} catch(Exception e) {
			if(logger.isDebugEnabled())
				logger.log(Level.DEBUG, "getObjectByPath: exception={}", e.getLocalizedMessage());
		}
		
		return res;
	}

	@LogMethod(level=LogLevel.TRACE)
	public static List<String> getList(JSONObject config, String key) {
		List<String> res = new LinkedList<>();
		
		if(config==null) return res;
		
		if(config.optJSONArray(key)!=null) {
			res.addAll(config.optJSONArray(key).toList().stream().map(Object::toString).collect(Collectors.toList()));
		}
		return res;
	}

	@LogMethod(level=LogLevel.TRACE)
	public static JSONObject getConfig(String key) {
		
		if(logger.isTraceEnabled()) {
			logger.log(Level.TRACE, "getConfig: key={} model={}", key, json.toString(2));
		}
	
		return json.optJSONObject(key);
	}

	@LogMethod(level=LogLevel.TRACE)
	public static JSONObject getConfig(JSONObject config, String key) {
		JSONObject res=config;
		
		if(config==null) return res;
		
		JSONObject direct=config.optJSONObject(key);
		if(direct==null) {
			key=config.optString(key);
			if(!key.isEmpty()) res=getConfig(key);
		} else {
			res=direct;
		}
		return res;
	}

	@LogMethod(level=LogLevel.TRACE)
	public static Map<String,JSONObject> getConfigByPattern(JSONObject json, String pattern) {
		Map<String,JSONObject> res = new HashMap<>();
		
		if(json==null) return res;
		
		json.keySet().forEach(key -> {
			if(key.contains(pattern)) {
				JSONObject obj = json.optJSONObject(key);
				String label = key.replace(pattern, "").trim();
				if(label.startsWith("'")) label = label.replace("'", "");
				if(obj!=null) res.put(label,obj);
			}
		});
		return res;
	}

	public static void setBoolean(String key, boolean value) {
		json.put(key, value);
	}

	public static Map<String, String> getTypeMapping() {
		return getMap("typeMapping");
	}

	public static Map<String, String> getFormatToType() {
		return getMap("formatToType");
	}

	@LogMethod(level=LogLevel.TRACE)
	public static Map<String,String> getMap(String property) {
		Map<String,String> res = new HashMap<>();
		
		JSONObject obj = json.optJSONObject(property);
		
		obj.keySet().stream().forEach(key -> res.put(key,  obj.opt(key).toString()));

		return res;
	
	}
	
	
}
