package no.paneon.api.conformance;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import no.paneon.api.logging.AspectLogger.LogLevel;
import no.paneon.api.logging.LogMethod;
import no.paneon.api.model.APIModel;
import no.paneon.api.model.JSONObjectHelper;
import no.paneon.api.utils.Config;
import no.paneon.api.utils.Out;
import no.paneon.api.utils.Utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;

public class ConformanceModel extends CoreModel {

	static final Logger LOG = LogManager.getLogger(ConformanceModel.class);
			
	private static final String GET = "GET";
	private static final String POST = "POST";
	private static final String DELETE = "DELETE";
	private static final String PUT = "PUT";
	private static final String PATCH = "PATCH";

	private static final String CONFORMANCE = "conformance";
	private static final String DEFAULT_CONFORMANCE = "default_conformance";

	private static final String OPERATIONS_DETAILS = "operations-details";
	private static final String MANDATORY = "mandatory";
	private static final String CONDITIONAL = "conditional";
	private static final String NON_MANDATORY = "non-mandatory";
	private static final String PATCHABLE = "patchable";

	private static final String RESOURCE = "resource";
	private static final String RESOURCES = "resources";

	private static final String OPERATION = "operation";
	private static final String OPERATIONS = "operations";
		
	private static final String ATTRIBUTES = "attributes";
	private static final String NOTIFICATIONS = "notifications";
	
	private static final String NOTIFICATION = "notification";

	private static final String CONDITION = "condition";
	private static final String COMMENT = "comment";
	private static final String RULE = "rule";
	private static final String DEFAULT = "default";
	private static final String LAYOUT = "layout";
	
	private static final String VARIABLES = "variables";
	
	private static final String GENERATED_CONFORMANCE = "generated_conformance";

	private static final List<String> GROUPS = Arrays.asList(RESOURCES, NOTIFICATIONS);

	private static final List<String> CONF_ITEMS = Arrays.asList(CONFORMANCE, DEFAULT_CONFORMANCE, LAYOUT );

	private static final String[] OPS = { GET, POST, DELETE, PUT, PATCH };
	
	private static final String CONFORMANCE_SOURCE_ONLY = "conformanceSourceOnly";
	
	public ConformanceModel() {
		super();
	}
	
	public void init() {
	}
	
	List<String> allResources = new LinkedList<>();
	List<String> allNotifications = new LinkedList<>();

	public JSONObject extractFromSwagger() {
		JSONObject conf = new JSONObject();
									
		if(Config.getBoolean(CONFORMANCE_SOURCE_ONLY)) {
			
			JSONObject res = new JSONObject();
			res.put(CONFORMANCE, conf);
			
			model.put("api_extracted_conformance", res);

			return res;
		}
		
		allResources = APIModel.getResources();
		allNotifications = APIModel.getAllNotifications();
				
		conf.put(RESOURCES, new JSONArray(allResources));

		for(String resource : allResources) {
			
			JSONObject confItem = new JSONObject();
			confItem.put(CONDITION, "");
			confItem.put(COMMENT, "");
						
			TreeNode<Conformance> node = getResourceDetails(resource);
						
			confItem.put(ATTRIBUTES, getConformanceItems(node));
			conf.put(resource, confItem);

			node = getOperationsOverview(resource);
			confItem.put(OPERATIONS, getConformanceItems(node));
						
			JSONObject postDetails = getMandatoryForPostFromSwagger(resource);
			
			if(true || !postDetails.isEmpty()) {
				JSONObject mandItem = new JSONObject();
				mandItem.put(MANDATORY, postDetails);
				JSONObject postItem = new JSONObject();
				postItem.put(POST, mandItem);
				confItem.put(OPERATIONS_DETAILS, postItem);
			}
			
			JSONObject patchDetails = getPatchableFromSwagger(resource);
			
			if(Config.getBoolean("onlyMandatoryAsPatchable")) {
				patchDetails = retainMandatoryInPatch(resource, confItem, patchDetails);
			}
			
			if(true || !patchDetails.isEmpty()) {
				JSONObject patchable = new JSONObject();
				patchable.put(PATCHABLE, patchDetails);
				if(confItem.has(OPERATIONS_DETAILS)) {
					confItem.getJSONObject(OPERATIONS_DETAILS).put(PATCH, patchable);
				} else {
					JSONObject patchItem = new JSONObject();
					patchItem.put(PATCH, patchable);
					confItem.put(OPERATIONS_DETAILS, patchItem);
				}
			}
		
		}	
		
		conf.put(NOTIFICATIONS,  new JSONArray(allNotifications));
		
		for(String notification : allNotifications) {				

			JSONObject confItem = new JSONObject();
			confItem.put(CONDITION, "");
			confItem.put(COMMENT, "");
			
			conf.put(Utils.upperCaseFirst(notification), confItem);

		}

		JSONObject res = new JSONObject();
		res.put(CONFORMANCE, conf);
		
		model.put("api_extracted_conformance", res);
				
		Out.println("... extracted from API");
		
		return res;
		
	}
		
	private JSONObject retainMandatoryInPatch(String resource, JSONObject confItem, JSONObject patchDetails) {
		Set<String> allPatchable = patchDetails.keySet();
		
		JSONObject attributes = confItem.optJSONObject(ATTRIBUTES);
		final Set<String> allMandatory = attributes.keySet().stream()
							.filter(attr -> attributes.optJSONObject(attr).getString(CONDITION).startsWith("M"))
							.collect(Collectors.toSet());
		
		Set<String> mandatoryPatchable = allPatchable.stream().filter(attr -> allMandatory.contains(attr)).collect(Collectors.toSet());
		
		Set<String> structured = mandatoryPatchable.stream()
									.filter(attr -> isComplexType(resource,attr))
									.collect(Collectors.toSet());
				
		Set<String> withoutSubordinates = structured.stream()
												.filter(attr -> !allMandatory.contains(attr + "."))
												.collect(Collectors.toSet());

		Set<String> retainedPatchable = new HashSet<>(mandatoryPatchable);
		
		retainedPatchable.removeAll(withoutSubordinates);
		
		Set<String> removeAttributes = new HashSet<>(allPatchable);
		
		removeAttributes.removeAll(retainedPatchable);
						
		for(String key : removeAttributes) {
			patchDetails.remove(key);
		}
		
		return patchDetails;
	}

	private boolean isComplexType(String resource, String attr) {
		String type = getResourceByPropertyPath(resource, attr);
		return type!=null && !type.isEmpty() && !APIModel.isSimpleType(type);		
	}

	@LogMethod(level=LogLevel.DEBUG)
	private TreeNode<Conformance> getOperationsOverview(String resource) {
		Conformance resurceConf = new Conformance(resource);
		TreeNode<Conformance> node = new TreeNode<>(resurceConf);
				
		for(String op : APIModel.getOperationsByResource(resource)) {
			TreeNode<Conformance> opNode = new TreeNode<>(new Conformance(op, "", ""));
			node.addChild(opNode);
		}
		return node;
		
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	private Map<String,Conformance> getConformanceItems(TreeNode<Conformance> node) {
		return getConformanceItems(node, "", new HashMap<>());
		
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	private Map<String,Conformance> getConformanceOperationDetailItems(TreeNode<Conformance> node) {
				
		node = node.getChildren().get(0); // skip the resource and POST levels
		
		Map<String,Conformance>  res = new HashMap<>();
		
		getConformanceItems(node, "", res);
				
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	private Map<String,Conformance> getConformanceItems(TreeNode<Conformance> node, String path, Map<String,Conformance> res) {
		
		node.getChildren().forEach(child -> {
			String subpath = path;
			if(!subpath.isEmpty()) subpath = subpath+".";
			subpath = subpath + child.getData().label;
			res.put(subpath, child.getData());
						
			getConformanceItems(child,subpath, res);

		});	
		
		return res;
		
	}

	@LogMethod(level=LogLevel.DEBUG)
	private static TreeNode<Conformance> getResourceDetails(String resource) {
		Conformance resurceConf = new Conformance(resource);
		TreeNode<Conformance> node = new TreeNode<>(resurceConf);
		
		Set<String> seenResources = new HashSet<>();
		for(String prop : APIModel.getPropertiesForResource(resource)) {
			seenResources.clear();
			node.addChild(getResourceDetailsByProperty(resource, prop, seenResources));
		}
		return node;
	}


	@LogMethod(level=LogLevel.DEBUG)
	private static TreeNode<Conformance> getResourceDetails(String resource, JSONObject resourceObject) {
		Conformance resurceConf = new Conformance(resource);
		TreeNode<Conformance> node = new TreeNode<>(resurceConf);
		
		Set<String> seenResoures = new HashSet<>();
		for(String prop : APIModel.getProperties(resourceObject)) {
			node.addChild(getResourceDetailsByProperty(resource, prop, seenResoures));
		}
		return node;
	}
	
	
	@LogMethod(level=LogLevel.DEBUG)
	private static TreeNode<Conformance> getResourceDetailsByProperty(String resource, String property, Set<String> seenResources) {
		JSONObject propObj = APIModel.getPropertyObjectForResource(resource);
		Map<String,String> propertyCondition = APIModel.getMandatoryOptional(resource);
		seenResources.clear();
		return getResourceDetailsByPropertyHelper(propObj, propertyCondition, resource, property, "", seenResources);
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	private static TreeNode<Conformance> getResourceDetailsByPropertyHelper(JSONObject propObj, Map<String,String> propertyCondition, 
			String resource, String property, String path, Set<String> seenResources) {

		String comment = "";
		String mandOpt = ""; 
		
		if(propertyCondition.containsKey(property)) mandOpt = propertyCondition.get(property);

		String referencedType = APIModel.getReferencedType(propObj,property);

		if(APIModel.isArrayType(resource,property)) {
			comment = "Array of " + referencedType;
		}

		Conformance conformance = new Conformance(property, mandOpt, comment);
		TreeNode<Conformance> node = new TreeNode<>(conformance);

		if(referencedType.isEmpty()) return node;
		if(APIModel.isEnumType(referencedType)) return node;
		if(APIModel.isSimpleType(referencedType)) return node;
		
		if(seenResources.contains(referencedType)) {	
			return node;
		}
		
		seenResources.add(referencedType);

		JSONObject properties = APIModel.getPropertyObjectForResource(referencedType);
		propertyCondition = APIModel.getMandatoryOptional(referencedType);

		String subPath = path + "." + property;
		for(String prop : properties.keySet()) {
			node.addChild(getResourceDetailsByPropertyHelper(properties, propertyCondition, referencedType, prop, subPath, seenResources));
		}

		seenResources.remove(referencedType);
		
		return node;

	}

	@LogMethod(level=LogLevel.DEBUG)
	private JSONObject readRules() {
		
		LOG.log(Level.TRACE, "readRules: rulesSource={}", rulesSource);

		try {
			return Utils.readYamlAsJSON(rulesSource,true);
				
		} catch(Exception e) {
			if(LOG.isErrorEnabled()) LOG.log(Level.ERROR, "setRulesSource: exception={}", e.getLocalizedMessage());
			return null;
		}		
	}

	private String rulesSource=null;
	private JSONObject rules=null;
	
	@LogMethod(level=LogLevel.DEBUG)
	public void setRulesSource(String source) {
		rulesSource=source;
		if(!rulesSource.isEmpty()) {
			JSONObject apiRulesAndConformance = readRules();
			if(apiRulesAndConformance!=null) {
				Optional<String> apikey = apiRulesAndConformance.keySet().stream().filter(x->x.startsWith("api")).findFirst();
							
				if(apikey.isPresent()) {
					JSONObject apiRules=apiRulesAndConformance.optJSONObject(apikey.get());				
					rules=apiRules;
				} else {
					Out.println("... expected api rules, not found");
				}
				
				setConformance(apiRulesAndConformance);
			}
		}
	}

	@LogMethod(level=LogLevel.DEBUG)
	public JSONObject getRules() {
		return rules;
	}


	@LogMethod(level=LogLevel.DEBUG)
	public List<String> getResources() {
		if(allResources.isEmpty()) {	
			allResources = APIModel.getResources();
			allResources = allResources.stream().distinct()
							.map(x -> x.replaceAll(".*/([A-Za-z0-9.]*)", "$1"))
							.collect(Collectors.toList());
			
		}
		
		if(allResources.isEmpty()) {
			JSONObject conf = getConformance();
			if(conf!=null && conf.has(RESOURCES)) {
				allResources.addAll(conf.optJSONArray(RESOURCES).toList().stream().map(Object::toString).collect(Collectors.toSet()));
			}
			if(LOG.isDebugEnabled()) {
				LOG.log(Level.DEBUG, "getResources: from conformance :: resource={}", Utils.dump(allResources));
			}
		}
		return allResources;
	}
		
	
	@LogMethod(level=LogLevel.DEBUG)
	protected List<String> getConfigCondition(String resource, String property) {
		return getStrings(resource,property,CONDITION);
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	protected List<String> getConfigConditionByPath(String path) {
		return getStringsByPath(path, CONDITION);
	}

	@LogMethod(level=LogLevel.DEBUG)
	protected List<String> getConfigConditionByPath(String path, String property) {
		return getStringsByPath(path + "." + property, CONDITION);
	}


	@LogMethod(level=LogLevel.DEBUG)
	public List<String[]> getOptionalInPost(String resource) {
		Map<String, String[]> res = new HashMap<>();
		
		Map<String,String> propertyConditions = APIModel.getMandatoryOptional(resource);
		
		for( Entry<String,String> entry : propertyConditions.entrySet() ) {
			if(entry.getValue().contains("M")) {
				res.put(entry.getKey(), new String[] { entry.getKey(), entry.getValue(), "" });	
			}
		}
					
		JSONObject conformance = getConformance(resource, OPERATIONS_DETAILS, POST, NON_MANDATORY);

		if(conformance!=null) {
			for(String property : conformance.keySet()) {
				JSONObject conf = conformance.optJSONObject(property);
				if(conf!=null) {
					String rule = Utils.getOptString(conf, RULE);
					String defaultValue = Utils.getOptString(conf, DEFAULT);

					res.put(property, new String[] { property, defaultValue, rule });
					
				}
			}
		}

		return getSortedPropertiesArray(res.values());
		
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public JSONObject getPatchableFromSwagger(String resource) {
		JSONObject res = new JSONObject();
		Set<String> properties = APIModel.getProperties(resource + "_Update");
			
		properties.forEach(property -> {
			
			String rule = "";
			
			JSONObject item = new JSONObject();
			item.put("rule",  rule);
			res.put(property, item);
		});
		
		return res;
		
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public List<String[]> getPatchable(String resource) {
		List<String[]> res = new LinkedList<>();
		Set<String> properties = APIModel.getProperties(resource + "_Update");
	
		properties.forEach(property -> {
			// String rule = getSpecialProperty(resource, property, "patchRules", RULE);
			JSONObject conf = getConformance(resource, OPERATIONS_DETAILS, PATCH, PATCHABLE);
			String rule = "";
			if(conf!=null) rule = conf.optString(RULE);
			res.add(new String[] { property, rule });
		});
		
		return getSortedPropertiesArray(res);
		
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public List<String[]> getNonPatchable(String resource) {
		Set<String> properties = APIModel.getProperties(resource);

		List<String[]> res = new LinkedList<>();
		Collection<String> patchable = getPatchable(resource).stream().map(x->x[0]).collect(Collectors.toList());
	
		properties.removeAll(patchable);
		
		properties.forEach(property -> {
			// String rule = getSpecialProperty(resource, property, "patchRules", RULE);
			// TBD
			String rule = "";
			res.add(new String[] { property, rule });
		});
		
		return getSortedPropertiesArray(res);
		
	}

	@LogMethod(level=LogLevel.DEBUG)
	public List<String[]> getMandatoryInPost(String resource) { 
		
		Map<String,String> propertyConditions = APIModel.getMandatoryOptional(APIModel.getResourceForPost(resource));
		JSONObject conformance = getConformance(resource, OPERATIONS_DETAILS, POST, MANDATORY);
		
		if(Config.getBoolean("removeConditionalMandatoryInPost")) {
			Set<String> propsToRemove = new HashSet<>();
			boolean tryRemove=true;
			while(tryRemove) {
				for(String prop : conformance.keySet()) {
					String parent = getParent(prop);
					if(!conformance.has(parent)) propsToRemove.add(prop);
				}
							
				for(String prop : propsToRemove) conformance.remove(prop);
				
				tryRemove=!propsToRemove.isEmpty();
				
				propsToRemove.clear();
			}
		}
		
		return getMandatoryInOperationHelper(conformance, propertyConditions, ValueSource.USE_FOUND);
		
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public List<ConformanceItem> getMandatoryConformanceInPost(String resource) { 
		List<String[]> tmp =  getMandatoryInPost(resource);
		
		return tmp.stream().map(ConformanceItem::new).collect(Collectors.toList());
		
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public List<ConformanceItem> getMandatoryConformanceInPatch(String resource) { 
		List<String[]> tmp =  getMandatoryInPatch(resource);
		
		return tmp.stream().map(ConformanceItem::new).collect(Collectors.toList());
		
	}
	
	
	@LogMethod(level=LogLevel.DEBUG)
	public List<String[]> getMandatoryInPatch(String resource) {
		
		Map<String,String> propertyConditions = APIModel.getMandatoryOptional(APIModel.getResourceForPatch(resource));		
		JSONObject conformance = getConformance(resource, OPERATIONS_DETAILS, PATCH, PATCHABLE);
		
		return getMandatoryInOperationHelper(conformance, propertyConditions, ValueSource.SET_EMPTY);
		
	}
	
	enum ValueSource {
		SET_EMPTY,
		USE_FOUND
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public List<String[]> getMandatoryInOperationHelper(JSONObject conformance, Map<String,String>  propertyConditions, ValueSource valueSource) {
		Map<String,String[]> res = new HashMap<>();
				
		for( Entry<String,String> entry : propertyConditions.entrySet() ) {
			if(entry.getValue().contains("M")) {
				String value = valueSource==ValueSource.SET_EMPTY ? "" :  entry.getValue();
				res.put(entry.getKey(), new String[] { entry.getKey(), value, "" });	
			}
		}
		
		for(String property : conformance.keySet()) {
			JSONObject conf = conformance.optJSONObject(property);
			if(conf!=null) {
				String rule = conf.optString(RULE);
				if("null".contentEquals(rule)) rule = "";
				res.put(property, new String[] { property, rule });
				
			}
		}
		
		return getSortedPropertiesArray(res.values());
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public JSONObject getConformance(String ... args) {
		JSONObject conformance = getConformance();
				
		int idx=0;
		while(conformance!=null && idx<args.length && !args[idx].isEmpty()) {
			if(conformance.has(args[idx])) {
				conformance = conformance.optJSONObject(args[idx]);
			} else {
				conformance = null;
			}
			idx++;
		}
		
		return conformance;
	}

	@LogMethod(level=LogLevel.DEBUG)
	public List<String[]> getConditionalInPost(String resource) {
			
		JSONObject conformance = getConformance(resource, OPERATIONS_DETAILS, POST, CONDITIONAL);

		List<String[]> res = Utils.getJSONObjectMap(conformance).entrySet().stream()
								.map(entry -> {
									String rule = entry.getValue().optString(RULE);
									if("null".contentEquals(rule)) rule = "";
									return new String[] { entry.getKey(), "", rule };
								 })
								.collect(Collectors.toList());
						
		return getSortedPropertiesArray(res);
	}

//	@LogMethod(level=LogLevel.DEBUG)
//	private String getSpecialProperty(String resource, String property, String group, String item) {
//		String res="";		
//		JSONObject conformance = getConformance(resource, group, property);
//		if(conformance!=null) res = conformance.optString(item);
//		
//		return res;
//	}

	String defaultSource;
	
	@LogMethod(level=LogLevel.DEBUG)
	public void setDefaults(String defaults) {
		this.defaultSource = defaults;
		readDefaults();
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	private void readDefaults() {

		if(defaultSource.isEmpty()) return;

		try {
			JSONObject o = Utils.readJSONOrYaml(defaultSource);	
			addToModel(o);
		} catch(Exception e) {
			if(LOG.isDebugEnabled()) 
				LOG.log(Level.DEBUG, "setRulesSource: exception={}", e.getLocalizedMessage());
		}		
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public void setEmbeddedDefaults() {
		JSONObject embedded = Config.getConfig(DEFAULT_CONFORMANCE);
		if(embedded!=null) {
			JSONObject res  = new JSONObject();
			res.put(DEFAULT_CONFORMANCE, embedded);
			addToModel(res);
		}
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public List<String> getAllNotifications() {
		return APIModel.getAllNotifications();
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public JSONObject getLayout() {		
		return getCoreConformance().optJSONObject(LAYOUT);
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public JSONObject getDefaults() {
		return getCoreConformance().optJSONObject(DEFAULT_CONFORMANCE);
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public List<String> getOrdering(String type) {
		List<String> ordering = new LinkedList<>();
				
		JSONObject layout = getLayout();
				
		if(layout!=null && layout.has(type)) {
			JSONArray order = layout.optJSONArray(type);
			for(Object o : order) {
				ordering.add(o.toString());
			}
		} 
		return ordering;
	}

	
	Map<String,Set<Condition>> allMethods = null;
	
	@LogMethod(level=LogLevel.DEBUG)
	public String getOperationConditions(String op, Collection<String> allOps) {
		String res="";
		
		if(allMethods==null) allMethods = getConditionsForAllMethods();
		
		if(!allMethods.containsKey(op)) {
			res = getDefaultConformance(OPERATIONS,op);
		} else {
			Set<Condition> conditions = allMethods.get(op);
			
			String[] allConditions = { "M", "O" };
			Map<String,Set<Condition>> pureConditions = new HashMap<>();
			for(String cond : allConditions) {
				pureConditions.put(cond, conditions.stream().filter(x->x.getCond().equals(cond)).collect(Collectors.toSet()));
			}
			
			int coveredResources = pureConditions.entrySet().stream().map(Map.Entry::getValue).map(Set::size).mapToInt(Integer::intValue).sum();
			
			if(coveredResources<APIModel.getResourcesByOperation(op).size()) {
				res = getDefaultConformance(OPERATIONS,op);
			}
			
			if(coveredResources>0) {
				res = getOperationConditionFromUsage(pureConditions, res);
			}
			
		} 
				
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	private String getOperationConditionFromUsage(Map<String,Set<Condition>> pureConditions, String res) {
		String mostFrequent = selectMostFrequentCondition(pureConditions); 
		
		Set<Condition> pivot = pureConditions.get(mostFrequent);
						
		if(!pivot.isEmpty()) {
			StringBuilder stringBld = new StringBuilder();
			if(!res.isEmpty()) stringBld.append(res + "\nexcept:");
			
			res = extractFromCondition(stringBld, pivot);
		}
	
		pureConditions.remove(mostFrequent);
		
		Iterator<Entry<String,Set<Condition>>> iter = pureConditions.entrySet().iterator();
		
		if(iter.hasNext() ) {
			StringBuilder stringBld = new StringBuilder();
			Entry<String,Set<Condition>> next = iter.next();
			if(!next.getValue().isEmpty() && !res.isEmpty()) stringBld.append(res + "\nexcept:");
			
			res = extractFromCondition(stringBld, next.getValue());

		}		
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	private String extractFromCondition(StringBuilder stringBld, Set<Condition> value) {
		for(Condition cond : value) {
			if(stringBld.length()>0) stringBld.append("\n");
			stringBld.append( cond.label + " : " + cond.getCond() );					
		}
		return stringBld.toString();
	}


	@LogMethod(level=LogLevel.DEBUG)
	private String getDefaultConformance(String topic, String item) {
		JSONObject conformance = model.optJSONObject(DEFAULT_CONFORMANCE);
		if(conformance.has(topic)) conformance = conformance.optJSONObject(topic);
	
		return getValueAsString(conformance,item,CONDITION);		
	}

	@LogMethod(level=LogLevel.DEBUG)
	private String selectMostFrequentCondition(Map<String, Set<Condition>> pureConditions) {
		int max=-1;
		String res="";
		for( Entry<String, Set<Condition>> cond : pureConditions.entrySet()) {
			if(cond.getValue().size()>max) {
				max = cond.getValue().size();
				res = cond.getKey();
			}
		}
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	private Map<String,Set<Condition>> getConditionsForAllMethods() {
		JSONObject conformance = getConformance();
		
		Map<String,JSONObject> allOps = getConfiguration(conformance, RESOURCE, OPERATIONS);
		
		Map<String,Set<Condition>> res = new HashMap<>();
		
		allOps.forEach((resource,val) -> {
			if(val!=null) {
				val.keySet().forEach(method -> {
					if(!res.containsKey(method)) res.put(method, new HashSet<>());
					Set<Condition> conditions = res.get(method);
					conditions.add(new Condition(val.optJSONObject(method).optString(CONDITION),resource));
				});
			}
		});
		
		return res;
	}
	
	private class Condition {
		protected String cond;
		protected String label;
		
		Condition(String cond, String label) {
			this.cond = cond; 
			this.label = label;
		}
		
		protected String getCond() {
			return cond;
		}
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public String getOperationComments(String op) {		
		return "";
	}
	
	
	@LogMethod(level=LogLevel.DEBUG)
	private Map<String, JSONObject> getConfiguration(JSONObject conformance, String pattern, String property) {
		Map<String,JSONObject> res = new HashMap<>();
		
		if(conformance==null) return res;
		
		List<String> resources = conformance.keySet().stream()
									.filter(x->x.startsWith(pattern))
									.map(x->x.replaceAll(pattern,"").trim())
									.collect(Collectors.toList());
				
		conformance.put(RESOURCES, new JSONArray(resources));
		
		for(String resource : resources) {
			JSONObject conf = conformance.optJSONObject(resource);

			if(conf!=null && conf.has(property)) {
				res.put(resource, conf.optJSONObject(property));
			}
		}
		
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	public String getComment(String item) {		
		return getValueAsString(item,COMMENT);
	}

	@LogMethod(level=LogLevel.DEBUG)
	public String getValueAsString(String key, String property) {
		String res="";
		
		JSONObject conformance = getConformance();

		if(conformance!=null) res = getValueAsString(conformance,key,property);
				
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	public String getCondition(String item, String type) {				
		String res = getValueAsString(item,CONDITION);
				
		if(res.isEmpty()) {
			JSONObject conformance = model.optJSONObject(DEFAULT_CONFORMANCE);
			res = getValueAsString(conformance,type,CONDITION);
			if(res.isEmpty()) res = "O";
		}

		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	public Set<String> getAllOperations() {
		Set<String> res = new HashSet<>();	
		res.addAll(APIModel.getAllOperations());
		
		if(res.isEmpty()) {
			for(String resource : getResources()) {
				res.addAll(this.getOperationsForResource(resource));
			}
		}
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	public static void setSwaggerSource(String swaggerSource) {
		APIModel.setSwaggerSource(swaggerSource);
	}
		
	@LogMethod(level=LogLevel.DEBUG)
	private void setConformance(JSONObject json) {
		JSONObject apiExtract = new JSONObject(json.toString());
				
		for(String item : json.keySet()) {
			if(!CONF_ITEMS.contains(item)) {
				apiExtract.remove(item);
			}
		}
		
		if(!apiExtract.isEmpty()) {
			JSONObject res = new JSONObject();
			res.put("rules_extracted_conformance", apiExtract);
			this.addToModel(res);
		}

	}

	@LogMethod(level=LogLevel.DEBUG)
	private JSONObject correctCapitalizationErrors(JSONObject json, String property, List<String> allItems) {
		
		if(json.has(property)) {
			JSONArray current = json.getJSONArray(property);
			JSONArray corrected = new JSONArray();
			
			if(allItems!=null) {
				Map<String,String> map = new HashMap<>();
				allItems.forEach(item -> map.put(item.toUpperCase(), item));
				
				current.forEach( item -> {
					String upper = item.toString().toUpperCase();	
					if(map.containsKey(upper)) corrected.put(map.get(upper));
				});
				json.put(property, corrected);
			} else {
				// No action - could possibly capitalize
			}
		}
		
		return json;
	
	}

	@LogMethod(level=LogLevel.DEBUG)
	public List<String> getNotificationsByResource(String resource) {
		List<String> res = APIModel.getNotificationsByResource(resource, getRules());
		
		if(res.isEmpty()) {
			JSONObject conf = getConformanceForResource(resource);
			if(conf!=null) conf = conf.optJSONObject(NOTIFICATIONS);
			if(conf!=null) res.addAll(conf.keySet());
		}
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	public String getResourceCondition(String resource, String property, String path) {				
		String baseResource = getBaseResource(path);
		JSONObject conformance = getAttributeConformanceForResource(baseResource);
	
		String attributePath = getAttibutePath(path);

		if(!attributePath.isEmpty()) attributePath = attributePath + ".";
		attributePath = attributePath + property;
		
		return getValueAsString(conformance,attributePath,CONDITION);
				
	}

	@LogMethod(level=LogLevel.DEBUG)
	public String getResourceComment(String resource, String property, String path) {		
		String baseResource = getBaseResource(path);
		JSONObject conformance = getAttributeConformanceForResource(baseResource);
	
		String attributePath = getAttibutePath(path);
		if(!attributePath.isEmpty()) attributePath = attributePath + ".";
		attributePath = attributePath + property;
		
		return getValueAsString(conformance,attributePath,COMMENT);
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public String getResourceComment(String resource, String path) {		
		String baseResource = getBaseResource(path);
		JSONObject conformance = getAttributeConformanceForResource(baseResource);
			
		return getValueAsString(conformance, path, COMMENT);
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public String getResourceComment(String path) {		
		String baseResource = getBaseResource(path);
		JSONObject conformance = getAttributeConformanceForResource(baseResource);
			
		path = path.replaceAll("^[^.]+.", "");
		
		String res = getValueAsString(conformance, path, COMMENT);
		
		return res;
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public String getResourceCondition(String path) {		
		String baseResource = getBaseResource(path);
		JSONObject conformance = getAttributeConformanceForResource(baseResource);
			
		path = path.replaceAll("^[^.]+.", "");
		
		String res = getValueAsString(conformance, path, CONDITION);
		
		return res;
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	private JSONObject getAttributeConformanceForResource(String resource) {
		return getConformance(resource, ATTRIBUTES);
	}

	@LogMethod(level=LogLevel.DEBUG)
	public JSONObject getOperationConformanceForResource(String resource) {
		return getConformance(resource, OPERATIONS);
	}

	@LogMethod(level=LogLevel.DEBUG)
	public String getAttibutePath(String path) {
		return path.replaceAll("^[^.]+.", "");
	}

	@LogMethod(level=LogLevel.DEBUG)
	private JSONObject getConformanceForResource(String resource) {
		return getConformance(resource);	
	}

	@LogMethod(level=LogLevel.DEBUG)
	private String getBaseResource(String path) {
		return path.split("\\.")[0];
	}

	@LogMethod(level=LogLevel.DEBUG)
	private String getValueAsString(JSONObject object, String path, String subpath) {
		String res="";
				
		if(object!=null && object.has(path)) {
			object = object.optJSONObject(path);
			if(object!=null && object.has(subpath)) {
				Object value = object.get(subpath);
				if(value instanceof String) {
					res = value.toString();	
					if("null".contentEquals(res)) res="";
				}
			}
		} 
						
		return res;
		
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public JSONObject getCoreConformance() {
		generateConformance();
		return model.optJSONObject(GENERATED_CONFORMANCE);	
	}

	@LogMethod(level=LogLevel.DEBUG)
	public JSONObject getConformance() {
		JSONObject conformance = getCoreConformance();
				
		if(conformance!=null && conformance.has(CONFORMANCE)) conformance = conformance.optJSONObject(CONFORMANCE);
		return conformance;
	}
	
	private boolean generatedConformance = false;
	@LogMethod(level=LogLevel.DEBUG)
	public JSONObject generateConformance() {
				
		if(generatedConformance) {
			return model.optJSONObject(GENERATED_CONFORMANCE);
		}
		
		JSONObject api             = model.optJSONObject("api_extracted_conformance");
		JSONObject rulesExtract    = model.optJSONObject("rules_extracted_conformance");
		JSONObject ruleconf        = model.optJSONObject("rules_conformance");
		JSONObject defaults        = model.optJSONObject("default_expanded_conformance");
		
		JSONObject schema_defaults = model.optJSONObject("default_schema_conformance");

		JSONObject docconf         = getExtractedConformance();
		JSONObject explicit        = model.optJSONObject("explicit_conformance");

		JSONObject res = createSkeleton(api); 
				
		// addVariablesSection(res);
		
		//
		// add conformance from sources other than the API proper
		// 
		
		boolean useConformanceSourceOnly = Config.getBoolean(CONFORMANCE_SOURCE_ONLY);
				
		if(!useConformanceSourceOnly) {
			combineConformance(res, rulesExtract);
			if(LOG.isTraceEnabled()) LOG.log(Level.TRACE, "generateConformance: with rules :: {}", res.toString(2));
		}
		
		if(!useConformanceSourceOnly) {
			combineConformance(res, ruleconf);
			if(LOG.isTraceEnabled()) LOG.log(Level.TRACE, "generateConformance: with ruleconf :: {}", res.toString(2));
		}
		
		if(!useConformanceSourceOnly) {
			combineConformance(res, docconf);
			if(LOG.isTraceEnabled()) LOG.log(Level.TRACE, "generateConformance: with docconf :: {}", res.toString(2));
		}
			
				
		combineConformance(res, explicit, useConformanceSourceOnly);
						
		if(LOG.isTraceEnabled()) LOG.log(Level.TRACE, "generateConformance: with explicit :: {}", res.toString(2));

		addDefaultConformance(res, model.optJSONObject(DEFAULT_CONFORMANCE));
		addDefaultConformance(res, defaults);
		
		model.put(GENERATED_CONFORMANCE, res);
		generatedConformance=true;		
		
		addSchemaConformance(res, schema_defaults);

		model.put(GENERATED_CONFORMANCE, res);
		
		return res;
		
	}

	private void addVariablesSection(JSONObject res) {
		JSONObject conformance = res;
		if(conformance.has(CONFORMANCE)) conformance = conformance.optJSONObject(CONFORMANCE);
		if(conformance!=null) conformance.put(VARIABLES, new JSONObject());
	}

	@LogMethod(level=LogLevel.DEBUG)
	private JSONObject addSchemaConformance(JSONObject res, JSONObject schema_defaults) {
	
		if(schema_defaults==null || schema_defaults.isEmpty()) return res;
		
		JSONObject conformance = res;
								
		if(conformance.has(CONFORMANCE)) conformance = conformance.optJSONObject(CONFORMANCE);
		
		JSONArray resourceArray = conformance.optJSONArray(RESOURCES);
		
		List<String> resources = resourceArray.toList().stream().map(Object::toString).collect(Collectors.toList());
				
		for(String resource : resources) {
			JSONObject resourceElement = conformance.optJSONObject(resource);
			
			if(resourceElement==null) {
				if("EventSubscription".equals(resource)) {
					resource = "Hub";
					resourceElement = conformance.optJSONObject(resource);
					Out.printAlways("... NOTE: replacing 'EventSubsription' with 'Hub' - expecting 'Hub' as the proper label");
					
				} else {
					Out.printAlways("... ERROR? Unable to locate conformance details for resource=" + resource + " - not processed");
					continue;
				}
			}
			
			JSONObject attributes = resourceElement.optJSONObject(ATTRIBUTES);
			
			Set<String> processed = new HashSet<>();
			List<String> parents = attributes.keySet().stream()
									.filter(attribute -> attribute.contains("."))
									.map(this::getParent)
									.sorted()
									.distinct()
									.collect(Collectors.toList());
						
			for(String parent : parents) {
								
				if(!processed.contains(parent)) {
					processed.add(parent);
					
					String parentType = this.getResourceByPropertyPath(resource, parent);
															
					if(!parentType.isEmpty()) {
						
						JSONObject typeDefault = schema_defaults.optJSONObject(parentType);
						
						if(typeDefault!=null) {
												
							updateCommentDefaults(attributes, parent, typeDefault);
							
							JSONObject attributeDefaults = typeDefault.optJSONObject(ATTRIBUTES);
							
							if(attributeDefaults!=null) {
								for(String attr : attributeDefaults.keySet()) {
																	
									String attribute = parent + "." + attr;
									
									if(attributes.has(attribute)) {
										updateAttributeDefaults(attributes, attribute, attributeDefaults.optJSONObject(attr));	
									} else {
										Out.println("... combine schema conformance defaults: property " + attr + " not found for " + parent);
									}
								}
							}
	
						}
					}
				}
				
				
			}
		}
		
		return res;
				
		
	}

	private void updateCommentDefaults(JSONObject attributes, String attributePath, JSONObject typeDefault) {
				
		JSONObject current = attributes.optJSONObject(attributePath);
		if(current!=null && typeDefault!=null) {	
			
			combineComment(current, typeDefault);
			
		}
	}

	@LogMethod(level=LogLevel.DEBUG)
	private void updateAttributeDefaults(JSONObject attributes, String attributePath, JSONObject defaults) {
				
		JSONObject current = attributes.optJSONObject(attributePath);
		if(defaults!=null && !defaults.isEmpty() && current!=null) {			
			overrideCondition(current, defaults);
			combineComment(current, defaults);
		}
	}

	@LogMethod(level=LogLevel.DEBUG)
	private JSONObject createSkeleton(JSONObject base) {
		JSONObject res; 
		
		if(Config.getBoolean(CONFORMANCE_SOURCE_ONLY)) {
			Out.println("... not using API definitions in the conformance profile");
		}
		
		if(base==null || Config.getBoolean(CONFORMANCE_SOURCE_ONLY)) 
			res = new JSONObject();
		else 
			res = new JSONObject(base.toString());
						
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	private JSONObject addDefaultConformance(JSONObject res, JSONObject defaults) {
		if(defaults==null) return res;
				
		JSONObject conformance = res;
		
		if(conformance.has(LAYOUT) && defaults.has(LAYOUT)) {
			conformance.put(LAYOUT, combineConformanceUnlessPresent(conformance.getJSONObject(LAYOUT),  defaults.getJSONObject(LAYOUT)));
			
		} else if(defaults.has(LAYOUT)) {
			conformance.put(LAYOUT, defaults.getJSONObject(LAYOUT));
		}
		
		if(conformance.has(CONFORMANCE) && defaults.has(CONFORMANCE)) {
			conformance.put(CONFORMANCE, combineConformanceUnlessPresent(conformance.getJSONObject(CONFORMANCE),  defaults.getJSONObject(CONFORMANCE)));
		} 

		if(conformance.has(CONFORMANCE)) conformance = conformance.optJSONObject(CONFORMANCE);
				
		final List<String> resources = conformance.optJSONArray(RESOURCES).toList().stream()
											.map(Object::toString)
											.collect(Collectors.toList());
		
		final JSONObjectHelper conf = new JSONObjectHelper(conformance);
		
		Set<String> keys = conf.getKeysForContainedJSONObjects().stream()
								.filter(key -> !GROUPS.contains(key))
								.collect(Collectors.toSet());
				
		addResourcesAndNotificationsDefaults(keys, conformance, resources, defaults);
		
		return res;
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	private void addResourcesAndNotificationsDefaults(Set<String> keys, JSONObject conformance, List<String> resources, JSONObject defaults) {
		for(String key : keys) {
					
			JSONObject obj = conformance.optJSONObject(key);
			if(resources.contains(key)) {		
				addResourcesDefaults(obj, conformance, defaults);
			} else {
				addNotificationDefaults(obj, defaults);
			}
						
			conformance.put(key,obj);
		}		
	}

	@LogMethod(level=LogLevel.DEBUG)
	private void addNotificationDefaults(JSONObject obj, JSONObject defaults) {
		JSONObject candidate = defaults.optJSONObject(NOTIFICATIONS);
		if(override(obj,candidate)) {
			obj.put(CONDITION, candidate.get(CONDITION));
		}		
	}

	@LogMethod(level=LogLevel.DEBUG)
	private void addResourcesDefaults(JSONObject resource, JSONObject conformance, JSONObject defaults) {
		if(resource!=null) {
			JSONObject candidate = defaults.optJSONObject(RESOURCE);
			
			overrideCondition(resource,candidate);
		
			overrideOperations(resource, defaults);
			
		}
	}

	@LogMethod(level=LogLevel.DEBUG)
	private void overrideOperations(JSONObject resource, JSONObject defaults) {
		JSONObject operations = resource.optJSONObject(OPERATIONS);
		JSONObject defaultOperations = defaults.optJSONObject(OPERATIONS);
		
		if(operations!=null && defaultOperations!=null) {
			for(String operation : operations.keySet()) {
				JSONObject candidate = defaultOperations.optJSONObject(operation);
				JSONObject current = operations.optJSONObject(operation);

				if(override(current,candidate)) {
					operations.put(operation,candidate);
				}
			}	
		}
	}

	@LogMethod(level=LogLevel.DEBUG)
	private void overrideProperty(JSONObject resource, JSONObject candidate, String property, boolean checked) {
		if((checked || override(resource,candidate)) && resource.has(property) && candidate.has(property)) 
			resource.put(property, candidate.get(property));
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	private void overrideCondition(JSONObject resource, JSONObject candidate) {
		overrideProperty(resource, candidate, CONDITION, false);
	}

	@LogMethod(level=LogLevel.DEBUG)
	private void combineComment(JSONObject resource, JSONObject candidate) {
		
		String res = resource.optString(COMMENT);
		String cand = candidate.optString(COMMENT);

		if(!res.contains(cand)) {
			if(!res.isEmpty() && !cand.isEmpty()) res = res + "\n\n";
			res = res + cand;
			resource.put(COMMENT,  res);
		}
		
	}			
	
	@LogMethod(level=LogLevel.DEBUG)
	private JSONObject combineConformanceUnlessPresent(JSONObject partA, JSONObject partB) {
		JSONObject res = new JSONObject(partA.toString());
		
		for(String key : partB.keySet()) {
			if(!res.has(key)) {
				res.put(key, partB.get(key));
			} else {
				Object partAByKey = res.get(key);
				Object partBByKey = partB.get(key);
				
				if(partAByKey instanceof JSONObject && partBByKey instanceof JSONObject) {
					res.put(key, combineConformanceUnlessPresent((JSONObject)partAByKey, (JSONObject)partBByKey));
				} else if(partAByKey instanceof JSONArray && partBByKey instanceof JSONArray) {
					JSONArray arrayA = (JSONArray) partAByKey;
					JSONArray arrayB = (JSONArray) partBByKey;
					res.put(key, Utils.merge(arrayA.toList(), arrayB.toList()));

				} else {
					res.put(key, partAByKey); // keeping the value of A
				}
			}
		}
		
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	private JSONObject combineConformance(JSONObject res, JSONObject candidate, boolean filterPresentInCandidate) {
		if(candidate==null) return res;
		
		for(String key : candidate.keySet()) {
						
			boolean notAllowUndefined = CONFORMANCE.contentEquals(key);
			if(!res.has(key)) {
				res.put(key,  candidate.get(key));
			} else {
				combineConformanceHelper(res.optJSONObject(key), candidate.optJSONObject(key), notAllowUndefined, filterPresentInCandidate);
			}
		}
		
		if(filterPresentInCandidate) {
			Set<String> keys = res.keySet().stream().collect(Collectors.toSet());
			keys.removeAll(candidate.keySet());
						
			keys.forEach(key -> res.remove(key));
		}

		return res;
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	private JSONObject combineConformance(JSONObject res, JSONObject candidate) {
		return combineConformance(res, candidate, false);
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	private JSONObject combineConformanceHelper(JSONObject res, JSONObject candidate, boolean notAllowUndefined, boolean filterPresentInCandidate) {
		
		if(candidate==null) return res;

		for(String key : candidate.keySet()) {
		
			boolean override = key.startsWith(OPERATIONS_DETAILS) || key.startsWith(VARIABLES);

			notAllowUndefined = notAllowUndefined && !override;
			
			if(!res.has(key)) {
				
				if(notAllowUndefined) {
					Out.println("... found '" + key + "' not expected");
				} else {
					Object candidateValue = candidate.get(key);
					res.put(key, candidateValue);
				}
				if(LOG.isTraceEnabled()) {
					LOG.log(Level.TRACE, "combineConformance: resource now has key={} :: {}", key, res.has(key));
				}

			} else {
				
				combineConformanceExistInTarget(res, key, candidate, notAllowUndefined, filterPresentInCandidate );
		
			}
		}
		
		if(filterPresentInCandidate) {
			Set<String> keys = res.keySet().stream().collect(Collectors.toSet());
			keys.removeAll(candidate.keySet());
			keys.forEach(key -> res.remove(key));
			
		}
		
		
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	private void combineConformanceExistInTarget(JSONObject res, String key, JSONObject candidate, 
			boolean notAllowUndefined, boolean filterPresentInCandidate ) {
		
		Object currentValue = res.get(key);
		Object candidateValue = candidate.get(key);

		switch(key) {
		
		case CONDITION: 
		case RULE:
		case DEFAULT:
			res.put(key, combinedStringValues(currentValue, candidateValue));
			break;

		case COMMENT:
			res.put(key, combinedComment(currentValue, candidateValue));
			break;
			
		default:
			combineObjectsAndArrays(res, candidate, key, notAllowUndefined, filterPresentInCandidate );
		}		
	}

	@LogMethod(level=LogLevel.DEBUG)
	private void combineObjectsAndArrays(JSONObject res, JSONObject candidate, String key, 
			boolean notAllowUndefined, boolean filterPresentInCandidate) {
		
		Object candidateValue = candidate.get(key);
		
		if(candidateValue instanceof JSONObject) {
			res.put(key, combineConformanceHelper(res.getJSONObject(key), (JSONObject) candidateValue, notAllowUndefined, filterPresentInCandidate));
			
		} else if(candidateValue instanceof JSONArray) {
			
			candidate.getJSONArray(key).forEach(element -> {
				if(!(element instanceof String || element instanceof Integer))
					res.getJSONArray(key).put(element);
				else {
					if(!res.getJSONArray(key).toList().contains(element)) {
						res.getJSONArray(key).put(element);
					}
				}
			});
			
		} else {
			Out.println("... expected object in conformance configuration but found", candidateValue.getClass().toString());
			Out.println("... ...", candidateValue.toString());
		}		
	}

	@LogMethod(level=LogLevel.DEBUG)
	private String combinedStringValues(Object currentValue, Object candidateValue) {			
		String current = currentValue.toString().trim();
		String candidate = candidateValue.toString().trim();
		
		if("null".contentEquals(current)) current="";
		if("null".contentEquals(candidate)) candidate="";

		return candidate.isEmpty() ? current : candidate;
				
	}

	@LogMethod(level=LogLevel.DEBUG)
	private String combinedComment(Object currentValue, Object candidateValue) {
		
		String current = currentValue.toString().trim();
		String candidate = candidateValue.toString().trim();
		
		if("null".contentEquals(current)) current="";
		if("null".contentEquals(candidate)) candidate="";

		if(current.contentEquals(candidate)) candidate="";
				
		String res;
		if(candidate.contains(current)) {
			res = candidate;
		} else if(current.contains(candidate)) {
			res = current;
		} else {
			res = current;
			if(!res.isEmpty() && !candidate.isEmpty()) res = res + "\n\n";
			res = res + candidate;
		}
		
		return res;
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	private boolean override(JSONObject current, JSONObject candidate) {
		boolean res = false;
		
		if(current!=null && candidate!=null) {
			String currentValue = getValueOfAttribute(current,CONDITION);
			String candidateValue = getValueOfAttribute(candidate,CONDITION);
	
			res = (currentValue.startsWith("O") && !candidateValue.startsWith("O")) ||
				  (currentValue.isEmpty() && !candidateValue.isEmpty()) ;

		}
		
		return res;

	}

	@LogMethod(level=LogLevel.DEBUG)
	private String getValueOfAttribute(JSONObject obj, String attribute) {
		String res="";	
		
		if(obj!=null) {
			if(obj.has(attribute)) {
				res = obj.get(attribute).toString();
			} else {
				res = obj.toString();
			}
			if("null".contentEquals(res)) res="";
		}
		
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	public void expandDefaults() {
		JSONObject obj = model.optJSONObject(DEFAULT_CONFORMANCE);
		
		if(obj==null) {
			Out.println("... no conformance defaults found");
			return;
		}
		
		JSONObject expanded = new JSONObject(obj.toString());
		
		if(expanded.has(OPERATIONS)) {
			JSONObject ops = expanded.optJSONObject(OPERATIONS);
			if(ops!=null) {
				if(!ops.has(CONDITION)) ops.put(CONDITION,  "");
				for(String op : OPS) {
					if(!ops.has(op)) {
						JSONObject opCondition = new JSONObject();
						opCondition.put(CONDITION, ops.getString(CONDITION));
						ops.put(op, opCondition);
					}
				}
				ops.remove(CONDITION);
			}
			expanded.put(OPERATIONS, ops);
		}
				
		model.put("default_expanded_conformance", expanded);
		
		if(LOG.isTraceEnabled()) {
			LOG.log(Level.TRACE, "default_expanded_conformance: {}", expanded.toString(2));
		}
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public List<String> getOrderedResources() {
		List<String> res = getOrderedList( getResources(), getOrdering(RESOURCES));
				
		if(Config.getBoolean("onlyMandatoryResources")) {
			res = res.stream()
					.filter(resource -> getCondition(resource, RESOURCE).contains("M"))
					.collect(Collectors.toList());
		}
		
		return res;
		
	}


	@LogMethod(level=LogLevel.DEBUG)
	public static List<String> getOrderedList(List<String> items, List<String> ordering) {
		List<String> res = new LinkedList<>(ordering);
		
		if(items!=null) {
			res.retainAll(items);
			for(String s : items) {
				if(!res.contains(s)) res.add(s);
			}
		}

		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	public String getOperationConditionByResource(String resource, String op) {
		JSONObject conf = getOperationConformanceForResource(resource);
		return getValueAsString(conf,op,CONDITION);
	}

	@LogMethod(level=LogLevel.DEBUG)
	public String getOperationCommentByResource(String resource, String op) {
		JSONObject conf = getOperationConformanceForResource(resource);
		return getValueAsString(conf,op,COMMENT);
	}


	@LogMethod(level=LogLevel.DEBUG)
	public JSONObject setConformance(String conformance) {
		JSONObject json = Utils.readJSONOrYaml(conformance);
		model.put("explicit_conformance", json);
		if(json.optJSONObject(VARIABLES)!=null) {
			Config.addConfiguration(new JSONObject().put(VARIABLES,json.optJSONObject(VARIABLES)));
		}
		
		if(LOG.isTraceEnabled()) {
			LOG.log(Level.TRACE, "setConformance: read json = {}", json.toString(2));
		}
		
		return json;
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public JSONObject setSchemaDefaults(String conformance) {
		JSONObject json = Utils.readJSONOrYaml(conformance);
		model.put("default_schema_conformance", json);
		
		if(LOG.isTraceEnabled()) {
			LOG.log(Level.TRACE, "setSchemaDefaults: read json = {}", json.toString(2));
		}
		
		return json;
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public JSONObject mandatoryOnly(JSONObject source) {	
		JSONObject conformance = new JSONObject();
		JSONObject target = new JSONObject();
		
		if(source.has(CONFORMANCE)) source = source.optJSONObject(CONFORMANCE);
		target.put(CONFORMANCE, conformance);
					
		if(source.has(VARIABLES)) {
			conformance.put(VARIABLES,  source.get(VARIABLES));
		}
	
		List<String> mandatoryResources = getOrderedResources();
		List<String> mandatoryNotifications = mandatoryResources.stream()
												.map(this::getNotificationsByResource)
												.flatMap(List::stream)
												.filter(this::isMandatoryNotification)
												.collect(Collectors.toList());

		if(!mandatoryResources.isEmpty()) {
			conformance.put(RESOURCES, mandatoryResources);
		}

		if(!mandatoryNotifications.isEmpty()) {
			conformance.put(NOTIFICATIONS, mandatoryNotifications);
		}
		
		for(String resource : mandatoryResources) {
			
			JSONObject res = retainMandatoryOnly(source.getJSONObject(resource));
						
			for(String op : ALL_OPS) {
				JSONObject ops = res.optJSONObject(OPERATIONS);
				if(ops!=null && !ops.has(op)) {
					removeElement(res, OPERATIONS_DETAILS, op);
				}
			}
			
			removeElement(res, OPERATIONS_DETAILS, POST, CONDITIONAL);
			
			conformance.put(resource,  res);
		}

		for(String notification : mandatoryNotifications) {			
			JSONObject res = new JSONObject();
			
			res.put(CONDITION, getValueAsString(source, notification, CONDITION));
			
			String comment = getValueAsString(source, notification, COMMENT);
			if(!comment.isEmpty()) res.put(COMMENT, comment);

			conformance.put(notification,  res);
		}
			
		return target;
		
	}

	@LogMethod(level=LogLevel.DEBUG) 
	private void removeElement(JSONObject target, String ... path) {
		int idx=0; 
		while(target!=null && idx<path.length-1) {
			target = target.optJSONObject(path[idx]);
			idx++;
		}
		if(target!=null) target.remove(path[idx]);
		
	}

	@LogMethod(level=LogLevel.DEBUG) 
	private boolean isMandatoryNotification(String notification) {
		String cond = getCondition(notification, NOTIFICATION);
		return !cond.isEmpty() && cond.startsWith("M");
	}
	
	
	@LogMethod(level=LogLevel.DEBUG)
	public JSONObject retainMandatoryOnly(JSONObject source) {			
		JSONObject target = new JSONObject();

		if(source.has(CONDITION)) {
			String condition = source.optString(CONDITION);
			if(condition.isEmpty() || !condition.startsWith("M")) {
				return target;
			}
		}
			
		for(String key : source.keySet()) {
			if(COMMENT.contentEquals(key)) {
				String res = source.getString(key);
				if(!res.isEmpty()) target.put(key, res);
			} else {
				Object res = retainMandatoryOnly(source.opt(key));
				if(res instanceof JSONObject) {
					if(!((JSONObject) res).isEmpty()) target.put(key, res); 
				} else if(res!=null) {
					target.put(key, res);
				}
			}
		}
				
		return target;
		
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	private Object retainMandatoryOnly(Object o) {
		Object res=o;
		if(o instanceof JSONObject) {
			res = retainMandatoryOnly( (JSONObject) o);
		} 
		return res;
	}

	
	@LogMethod(level=LogLevel.DEBUG)
	public JSONObject removeOptional(JSONObject source) {			
		JSONObject target = new JSONObject();
					
		for(String key : source.keySet()) {
			
			String value = source.optString(key);
			switch(key) {
			case COMMENT:
				if(!value.isEmpty()) target.put(key,value);
				break;
				
			case CONDITION:
				if(!value.isEmpty() && !value.equals("O")) target.put(key,value);
				break;
				
			case RULE:
				if(value!=null)
					target.put(key,value);
				else
					target.put(key, "--");
				break;
				
			default:
				Object sub = removeOptional(source.opt(key));
				if(sub!=null) target.put(key, sub);
			}
		}
		
		return target;
		
	}

	@LogMethod(level=LogLevel.DEBUG)
	public void removeNonMandatoryAsPatchable() {	
		
//		for(String resource : getResources()) {
//			
//			JSONObject patchable = getConformance(resource, OPERATIONS_DETAILS, PATCH, PATCHABLE);
//			
//			JSONObject attributes = getAttributeConformanceForResource(resource);
//			
//			Out.println("... patchable: patchable=" + patchable + " attributes=" + attributes );
//
//			if(patchable==null || attributes==null) continue;
//			
//			List<String> toRemove = new LinkedList<>();
//			
//			for(String candidate : patchable.keySet()) {
//				
//				Out.println("... patchable: candidate " + candidate );
//
//				JSONObject attribute = attributes.optJSONObject(candidate);
//				if(attribute!=null) {
//					String condition = attribute.optString(CONDITION);
//					if(!condition.startsWith(MANDATORY)) toRemove.add(candidate);
//				}
//			}
//			
//			for(String attribute : toRemove) {
//				Out.println("... patchable: remove non-mandatory attribute " + attribute );
//				patchable.remove(attribute);
//			}
//		}
				
	}
	
	
	@LogMethod(level=LogLevel.DEBUG)
	private Object removeOptional(Object o) {
		Object res=o;
		if(o instanceof JSONObject) {
			JSONObject sub = removeOptional( (JSONObject) o);
			if(!sub.isEmpty()) res=sub;
		} 
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	public JSONObject getDocumentDetails() {
		return APIModel.getDocumentDetails();
	}

	@LogMethod(level=LogLevel.DEBUG)
	public void setExisting(JSONObject json) {
		this.setExtracted(json);		
	}

	@LogMethod(level=LogLevel.DEBUG)
	public void setExtracted(JSONObject json) {
		JSONObject extracted = new JSONObject();

		extracted.put("doc_extracted_conformance", json);
		
		addToModel(extracted);

	}

	@LogMethod(level=LogLevel.DEBUG)
	public JSONObject getExtractedConformance() {
		JSONObject res = new JSONObject();
						
		JSONObject extracted = model.optJSONObject("doc_extracted_conformance");
		if(extracted!=null) {
			res = extracted;
			extracted = extracted.optJSONObject(CONFORMANCE);

			JSONObject generated = model.optJSONObject(GENERATED_CONFORMANCE);
			if(generated!=null) generated = generated.optJSONObject(CONFORMANCE);
			
			if(generated!=null && extracted!=null) {		
				for(String key : generated.keySet()) {
					if(!extracted.has(key)) {
						extracted.put(key, generated.get(key));
						Out.println("... adding missing " + key + " from API specification ");
					}
				}
			}
		}
		
		return res;
	}

	private static final List<String> ALL_OPS = Arrays.asList(GET, POST, DELETE, PUT, PATCH);

	@LogMethod(level=LogLevel.DEBUG)
	public Set<String> getUnusedOperations() {
		Set<String> res = new HashSet<>(ALL_OPS);
		res.removeAll(getAllOperations());
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	public String getResourceByPath(String path) {
		return APIModel.getResourceByPath(path);
	}

	@LogMethod(level=LogLevel.DEBUG)
	public JSONObject extractFromRules() {
		JSONObject conf = new JSONObject();

		if(getRules()==null) return conf;
		
		boolean seenDetails = false; 

		for(String resource : getResources() ) {
			boolean addedDetails;
			
			String rulesKey = "rules " + resource;
			JSONObject rulesForResource = getRules().optJSONObject(rulesKey);

			if(rulesForResource==null) continue;
			
			Collection<String> operations = getOperationsForResource(resource).stream().map(String::toUpperCase).collect(Collectors.toList());
			conf.put(resource, new JSONObject());
			conf.getJSONObject(resource).put(OPERATIONS_DETAILS, new JSONObject());
			JSONObject operationDetails = conf.getJSONObject(resource).getJSONObject(OPERATIONS_DETAILS);
	
			JSONObject mandConf = new JSONObject();
			JSONObject condConf = new JSONObject();

			addedDetails = addMandatoryForPostFromRules(rulesForResource, mandConf, condConf, operationDetails); 
			seenDetails = seenDetails || addedDetails;
			
			addedDetails = addSubMandatoryForPost(rulesForResource, mandConf, condConf, operationDetails, operations); 
			seenDetails = seenDetails || addedDetails;
			
			addedDetails = addConditionForPost(resource, condConf, operationDetails); 
			seenDetails = seenDetails || addedDetails;
			
			addedDetails = addPatchable(resource, operationDetails); 
			seenDetails = seenDetails || addedDetails;
						
		}
				
		JSONObject res = new JSONObject();
		if(seenDetails) {
			res.put(CONFORMANCE, conf);
							
			model.put("rules_conformance", res);
									
			Out.println("... extracted from API rules");
		}				
		return res;
						
	}

	private boolean addPatchable(String resource, JSONObject operationDetails) {

		boolean seenDetails = false;

		List<String[]> operationRules = getPatchable(resource);
		if(!operationRules.isEmpty()) {
			JSONObject condConf = new JSONObject();
			for(String[] opRule : operationRules) {
				JSONObject confItem = new JSONObject();
				String property = opRule[0];
				String rule = opRule[1];
				
				confItem.put(RULE, rule);																			
				condConf.put(property, confItem);
				seenDetails = true;
			}
			if(!operationDetails.has(PATCH)) operationDetails.put(PATCH, new JSONObject());
			operationDetails.getJSONObject(PATCH).put(PATCHABLE, condConf);
		}			

		return seenDetails;
	}

	private boolean addConditionForPost(String resource, JSONObject condConf, JSONObject operationDetails) {

		boolean seenDetails = false;

		List<String[]> operationRules = getConditionalInPost(resource);
		if(!operationRules.isEmpty()) {
			for(String[] opRule : operationRules) {
				JSONObject confItem = new JSONObject();
				String property = opRule[0];
				String defaultValue = opRule[1];
				String rule = opRule[2];
				
				confItem.put(DEFAULT, defaultValue);																			
				confItem.put(RULE, rule);																			
				condConf.put(property, confItem);
				seenDetails = true;
			}
			if(!operationDetails.has(POST)) operationDetails.put(POST, new JSONObject());
			operationDetails.getJSONObject(POST).put(CONDITIONAL, condConf);
		}		
		
		return seenDetails;
	}

	private boolean addSubMandatoryForPost(JSONObject rulesForResource, JSONObject mandConf, 
											JSONObject condConf, JSONObject operationDetails, Collection<String> operations) {
		boolean seenDetails = false;
				
		List<String> subMandatory = rulesForResource.keySet().stream()
				.filter(key -> key.contains("sub") && key.contains(MANDATORY))
				.collect(Collectors.toList());
					
		for(String key : subMandatory) {
			JSONObject item = rulesForResource.optJSONObject(key);
			for(String prop : item.keySet()) {
				
				String[] elements = item.getString(prop).trim().split(",");
								
				boolean seen = addSubMandatoryElements(elements, prop, mandConf, condConf);
				seenDetails = seenDetails || seen;
				
			}
		
			for(String op : operations) {
				if(key.toUpperCase().contains(op.toUpperCase())) {
					if(!operationDetails.has(op)) operationDetails.put(op, new JSONObject());
					operationDetails.getJSONObject(op).put(CONDITIONAL, condConf);
				}
			}
			
		}
		return seenDetails;
	}

	private boolean addSubMandatoryElements(String[] elements, String prop, JSONObject mandConf, JSONObject condConf) {
		boolean seenDetails = false;
		
		boolean removeConditionalMandatoryInPost = Config.getBoolean("removeConditionalMandatoryInPost");

		for(String part : elements) {
			String property = prop + "." + part.trim();
				
			JSONObject confItem = new JSONObject();
			confItem.put(RULE, "");
					
			if(mandConf.has(prop) || !removeConditionalMandatoryInPost) 
				mandConf.put(property, confItem);
			else
				condConf.put(property, confItem);
			
			seenDetails = true;
			
		}
		return seenDetails;
	}

	private boolean addMandatoryForPostFromRules(JSONObject rulesForResource, JSONObject mandConf, JSONObject condConf, JSONObject operationDetails) {

		boolean seenDetails = false;
		List<String> mandatory = rulesForResource.keySet().stream()
				.filter(key -> key.startsWith("mandatory in"))
				.collect(Collectors.toList());
						
		for(String key : mandatory) {
			List<String> properties = getStringArray(rulesForResource, key);
			for(String property : properties) {
									
				JSONObject confItem = new JSONObject();
				confItem.put(RULE, "");	
				
				if(!property.contains(".") || containsParent(mandConf,property)) {
					mandConf.put(property, confItem);
				} else {
					condConf.put(property, confItem);
				}
				seenDetails = true;
						
			}
											
		}		
				
		if(!operationDetails.has(POST)) operationDetails.put(POST, new JSONObject());
		operationDetails.getJSONObject(POST).put(MANDATORY, mandConf);

		
		return seenDetails;
	}

	
	private JSONObject getMandatoryForPostFromSwagger(String resource) {

		JSONObject res = new JSONObject();
		
		TreeNode<Conformance> node = getResourceDetails(resource, APIModel.getResourceForPost(resource));

		Map<String,Conformance> conformanceItems = getConformanceItems(node);
		
		for(Entry<String,Conformance> entry : conformanceItems.entrySet()) {
			String property = entry.getKey();
			Conformance conf = entry.getValue();

			boolean removeConditionalMandatoryInPost = Config.getBoolean("removeConditionalMandatoryInPost");
			if(removeConditionalMandatoryInPost) {
				if(conf.condition.contains("M") && parentIsMandatory(conformanceItems,property)) {
					JSONObject confItem = new JSONObject();
					confItem.put(RULE, "");	
					res.put(property,  confItem);
				}
			} else {
				if(conf.condition.contains("M")) {
					JSONObject confItem = new JSONObject();
					confItem.put(RULE, "");	
					res.put(property,  confItem);
				}
			}
		}
				
		return res;
	}

	
	private boolean parentIsMandatory(Map<String, Conformance> conformanceItems, String property) {
		boolean res=false;
		String parent = getParent(property);
		if(!parent.contentEquals(property) && conformanceItems.containsKey(parent)) {
			Conformance conf = conformanceItems.get(parent);
			if(conf.condition.contains("M") && parentIsMandatory(conformanceItems,parent)) res=true;
		} else if(parent.contentEquals(property)) {
			res=true; // returning true if no parent 
		}
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	private boolean containsParent(JSONObject mandConf, String property) {
		String parent = getParent(property);
		return mandConf.has(parent);
	}

	@LogMethod(level=LogLevel.DEBUG)
	private String getParent(String property) {
		String res=property;
		int pos=property.lastIndexOf('.');
		if(pos>0) res=res.substring(0,pos);
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	private List<String> getStringArray(JSONObject json, String key) {
		List<String> res = new LinkedList<>();
		
		Object value = json.get(key);
		if(value instanceof JSONArray) {
			res.addAll(((JSONArray)value).toList().stream().map(Object::toString).collect(Collectors.toList()));
		} else if(value instanceof String) {
			String s = (String)value;
			List<String> l = Arrays.asList(s.split(",")).stream().map(String::trim).collect(Collectors.toList());
			res.addAll(l);
		} else {
			Out.println("... expected string or array type for " + key);
		}
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	public Set<String> getPropertiesForResource(String resource) {
		Set<String>  res = new HashSet<>();	
		res.addAll( APIModel.getPropertiesForResource(resource) );
		
		if(res.isEmpty()) {
			// JSONObject conf = getConformanceForResource(resource);
			// if(conf!=null) conf = conf.optJSONObject(ATTRIBUTES);
			JSONObject conf = getConformance(resource, ATTRIBUTES);
			if(conf!=null) res.addAll(conf.keySet());
		}
		
		return res;
	
	}

	@LogMethod(level=LogLevel.DEBUG)
	public List<String> getPathsForResource(String resource, String config) {
		List<String> res = new LinkedList<>();
		res.addAll( APIModel.getPaths(resource,  config) );
	
//		if(res.isEmpty()) {
//			JSONObject conf = getConformanceForResource(resource);
//			if(conf!=null) conf = conf.optJSONObject(OPERATIONS);
//			if(conf!=null) res.addAll(conf.keySet());
//		}
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	public List<String> getOperationsForResource(String resource) {
		List<String> res = new LinkedList<>();
		res.addAll( APIModel.getOperationsByResource(resource) );
		
		if(res.isEmpty()) {		
			// JSONObject conf = getConformanceForResource(resource);
			// if(conf!=null) conf = conf.optJSONObject(OPERATIONS);
			JSONObject conf = getConformance(resource, OPERATIONS);
			if(conf!=null) res.addAll(conf.keySet());
		}
		return res.stream().map(String::toUpperCase).collect(Collectors.toList());
	}

	@LogMethod(level=LogLevel.DEBUG)
	public Set<String> getOnlyOptionalOperations() {
		Set<String> seen = new HashSet<>();
		for(String resource : getResources()) {
			Collection<String> ops = getOperationsForResource(resource);
			for(String op : ops) {
				String condition = getOperationConditionByResource(resource, op);
				if(!(condition.isEmpty() || "O".equals(condition))) seen.add(op);
			}
		}
		Set<String> onlyOptional = getAllOperations();
		onlyOptional.removeAll(seen);
				
		return onlyOptional;
	}

	@LogMethod(level=LogLevel.DEBUG)
	public Set<String> getMandatoryNotifications() {
		Set<String> res = new HashSet<>();
		for(String notification : getAllNotifications()) {
			String condition = getCondition(notification, NOTIFICATIONS);
			if(!isOptional(condition)) res.add(notification);
		}
		return res;
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public static boolean isOptional(Entry<String, String[]> item) {
		
		String[] value = item.getValue();
		String condition = value[1];
				
		return isOptional(condition);
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public static boolean isOptional(String condition) {			
		return condition.isEmpty() || condition.equals("O");
	}

	@LogMethod(level=LogLevel.DEBUG)
	private boolean isTopLevel(String[] list) {
		return !list[0].contains(".");
	}

	@LogMethod(level=LogLevel.DEBUG)
	public List<String> getOrderedOperationsForResource(String resource) {
		return Utils.filterList(getOrdering(OPERATIONS), getOperationsForResource(resource));
	}
		
	@LogMethod(level=LogLevel.DEBUG)
	public List<String> getAllUsedOperationsOrdered() {
		return Utils.filterList(getOrdering(OPERATIONS), getAllOperations());
	}

	@LogMethod(level=LogLevel.DEBUG)
	public List<String> getPaths(String resource, String operation) {
		List<String> res = new LinkedList<>();	
		res.addAll(APIModel.getPaths(resource,operation));
		return res;
	}

	@LogMethod(level=LogLevel.INFO)
	public String getResourceByPropertyPath(String propertyPath) {
		String res="";
		
		String resourceName = propertyPath.substring(0,propertyPath.indexOf('.'));
		JSONObject resourceDef = APIModel.getDefinition(resourceName);
		if(resourceDef!=null) {
			int dotIdx = propertyPath.indexOf('.');
			propertyPath = propertyPath.substring(dotIdx+1);
			res = getResourceByPropertyPath(resourceName, propertyPath);
		}
		return res;
	}

	@LogMethod(level=LogLevel.INFO)
	private String getResourceByPropertyPath(String resource, String propertyPath) {
		String res=resource;
		
		if(!propertyPath.isEmpty()) {
			String property = propertyPath; 
			if(propertyPath.contains(".")) {
				int dotIdx = propertyPath.indexOf('.');
				property = propertyPath.substring(0,dotIdx);
				propertyPath = propertyPath.substring(dotIdx+1);
			} else {
				propertyPath = "";
			}
			
			JSONObject properties = APIModel.getPropertyObjectForResource(resource);
			
			resource = getTypeByProperty(properties,property);
			
			res = getResourceByPropertyPath(resource, propertyPath);
		}

		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	private String getTypeByProperty(JSONObject properties, String property) {
		String res="";
		properties = properties.optJSONObject(property);
		
		String ref = properties.optString("$ref");
		
		if(ref.isEmpty() && properties.has("items")) ref = properties.optJSONObject("items").optString("$ref");
		
		if(!ref.isEmpty()) res = APIModel.getTypeByReference(ref);
		
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	public JSONObject getSchemaDefaults(String resource) {
		JSONObject schemaDefaults = model.optJSONObject("default_schema_conformance");
		if(schemaDefaults!=null) schemaDefaults = schemaDefaults.optJSONObject(resource);
				
		return schemaDefaults;
	}

	
}
