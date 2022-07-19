package no.paneon.api.conformance;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import no.paneon.api.logging.AspectLogger.LogLevel;
import no.paneon.api.logging.LogMethod;
import no.paneon.api.utils.Config;
import no.paneon.api.utils.Out;
import no.paneon.api.utils.Utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
 
public class CoreModel {
	
    static final Logger LOG = LogManager.getLogger(CoreModel.class);
    
    protected JSONObject model;
    
	static final String CONFIG = "config";

    public CoreModel() {
    	Out.debug("CoreModel::constructor");
    	this.model = new JSONObject();
    }
 
	@LogMethod(level=LogLevel.DEBUG)
	public void addToModel(JSONObject delta) {
		for(String key : delta.keySet()) {			
			model.put(key, delta.get(key));
		}
	}
	    
	@LogMethod(level=LogLevel.DEBUG)
	public List<String> getStringsByPath(String path, String element) {
		List<String> res = new LinkedList<>();
		
		LOG.log(Level.DEBUG, "getStringsByPath: path={} element={1}", path, element);

		if(!path.contains(".")) {
			String[] parts = path.split("[.]");
			if(parts.length==1) return res;
			
			String main = parts[0];
			String subPath = path.substring(main.length()+1);
			
			LOG.log(Level.DEBUG, "getStringsByPath: main={} subPath={1}", main, subPath);

			JSONObject o = getObject(main);
	
			if(o!=null) {		
				JSONObject sub = o.optJSONObject(subPath);
				if(sub!=null && sub.optJSONArray(element)!=null) {
					res = sub.optJSONArray(element).toList().stream().map(Object::toString).collect(Collectors.toList());
					
					if(LOG.isDebugEnabled()) {
						LOG.log(Level.DEBUG, "getStringsByPath: res={}", Utils.dump(res));
					}
				}
			}
		} 
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	public JSONObject getConfig(String key) {
		
		if(LOG.isTraceEnabled()) LOG.log(Level.TRACE, "getConfig: key={} model={1}", key, model.toString(2));

		return model.optJSONObject(key);
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public void addConfiguration(JSONObject deltaJSON) {
	    
	    for(String key : deltaJSON.keySet()) {	    	
	    	model.put(key, deltaJSON.get(key));
	    }	   	
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public boolean has(String property) {
		return model!=null && model.has(property);
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public List<String> get(String property) {
		try {
			JSONArray array = model.optJSONArray(property);
			return array.toList().stream().map(Object::toString).collect(Collectors.toList());
		} catch(Exception e) {
			return new LinkedList<>();
		}
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public boolean getBoolean(String property) {
		return model.getBoolean(property);
	}

	@LogMethod(level=LogLevel.DEBUG)
	public String getString(String property) {
		return model.optString(property);
	}

	@LogMethod(level=LogLevel.DEBUG)
	public Map<String,String> getStringMap(String property) {
		Map<String,String> res = new HashMap<>();
		
		JSONObject obj = model.optJSONObject(property);
		if(obj!=null) {
			obj.keySet().forEach(key -> res.put(key, obj.getString(key)));
		}
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	public JSONObject getObject(String property) {
		return model.optJSONObject(property);
	}

	@LogMethod(level=LogLevel.DEBUG)
	public JSONArray getArray(String property) {
		return model.optJSONArray(property);
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public List<String> getStrings(String ... args) {
		List<String> res = new LinkedList<>();
		JSONObject o = getObject(args[0]);
		
		int i=1;
		while(i<args.length-1 && o!=null) {
			o = o.optJSONObject(args[i]);
			
			if(LOG.isDebugEnabled()) {
				LOG.log(Level.DEBUG, "getStrings: i={} args={1} o={2}", i, args[i], ((o!=null) ? o.toString(2) : "null"));
			}
			
			i++;
		}
		
		if(o==null) return res;

		if(o.optJSONArray(args[i])!=null) {
			res = o.optJSONArray(args[i]).toList().stream().map(Object::toString).collect(Collectors.toList());
			if(LOG.isDebugEnabled()) {
				LOG.log(Level.DEBUG, "getStrings: res={}", Utils.dump(res));
			}
		}
		
		return res;
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public String getField(String resource, JSONObject config, String field, String defaultValue) {
		String res=defaultValue;
		
		if(config!=null && config.optJSONObject(CONFIG)!=null) config=config.optJSONObject(CONFIG);
        
		if(config!=null && config.optJSONObject(resource)!=null) {
			res=config.optJSONObject(resource).optString(field);
			if(res.isEmpty()) res=defaultValue;
		}
		
		return res;
	}
		
	@LogMethod(level=LogLevel.DEBUG)
	public List<String> getSortedProperties(Collection<String> properties) {
		List<String> sorted = new LinkedList<>();

		List<String> props1 = properties.stream()
				.filter(x->x.startsWith("@"))
				.sorted().collect(Collectors.toList());

		List<String> props2 = properties.stream()
				.filter(x->!x.startsWith("@"))
				.sorted().collect(Collectors.toList());	        	

		sorted.addAll(props1);
		sorted.addAll(props2);
		
		return sorted;
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	private static int sortByFirstElement(String[] a, String[] b) {
		return a[0].compareTo(b[0]);
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public List<String[]> getSortedPropertiesArray(Collection<String[]> properties) {
		List<String[]> sorted = new LinkedList<>();

		if(!Config.getBoolean("includeMetaProperties")) {
			List<String[]> props1 = properties.stream()
					.filter(x->x[0].startsWith("@"))
					.sorted(CoreModel::sortByFirstElement).collect(Collectors.toList());
			sorted.addAll(props1);
		}
		
		List<String[]> props2 = properties.stream()
				.filter(x->!x[0].startsWith("@"))
				.sorted(CoreModel::sortByFirstElement).collect(Collectors.toList());	        	

		sorted.addAll(props2);
		
		return sorted;
	}
			
}
