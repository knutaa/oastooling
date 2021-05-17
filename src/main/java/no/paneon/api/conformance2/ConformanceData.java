package no.paneon.api.conformance2;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.SortedMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import no.paneon.api.conformance.ConformanceItem;
import no.paneon.api.conformance.ConformanceModel;
import no.paneon.api.logging.AspectLogger.LogLevel;
import no.paneon.api.logging.LogMethod;
import no.paneon.api.model.APIModel;
import no.paneon.api.utils.Config;
import no.paneon.api.utils.Out;

import static java.util.stream.Collectors.toList;

public class ConformanceData {

	static final Logger LOG = LogManager.getLogger(ConformanceData.class);

	private static final String RESOURCE = "resource";
	private static final String RESOURCES = "resources";

	private static final String OPERATIONS = "operations";		
	private static final String NOTIFICATIONS = "notifications";
	
	private static final String OPERATIONS_DETAILS = "operations-details";		
	private static final String RESOURCE_DETAILS = "resource-details";

	private static final String ATTRIBUTES = "attributes";
	
	private static final String COMMENT = "comment";

	public List<ConformanceItem> resourceOverview;
	
	public List<ConformanceResourceDetails> resourceDetails;
		
	public List<ConformanceOperationsDetails> resources;

	public List<FileData> parts;
		
	private ConformanceModel model;
	
	public ConformanceData(ConformanceModel model) {
		this.model = model;
	}
				
	static public class FileData {
		public String filename;
		
		FileData(String filename) {
			this.filename=filename;
		}
	}
	

	public class ConformanceResourceDetails {
		public String resourceName;
		public List<ConformanceItem> conformanceItems;
		
		ConformanceResourceDetails(String resourceName) {
			this(resourceName, new LinkedList<>() );
		}
		
		ConformanceResourceDetails(String resourceName, List<ConformanceItem> conformanceItems) {
			this.resourceName = resourceName;
			this.conformanceItems = conformanceItems;
		}
		
		
	}
	
	public class ConformanceOperationsDetails {
		public String resource;
		
		public ConformanceOperationsDetails(String resource) {
			this.resource=resource;
		}
		
		public List<OperationConformance> get;
		public List<OperationConformance> post;
		public List<OperationConformance> delete;
		public List<OperationConformance> patch;
		public List<OperationConformance> put;

		public List<ConformanceItem> mandatoryAttributes;
		public List<ConformanceItem> patchableAttributes;

	}
	
	public class OperationConformance {
		public String path;
		public String description;
		
		public OperationConformance(String path, String description) {
			this.path = path;
			this.description = description;
		}
		
		public OperationConformance(String path) {
			this.path = path;
		}
			
	}
	
	public void setup() {
		this.resourceOverview = generateResourceOverview();
		this.resourceDetails = generateResourceDetails();
		
		this.resources = generateOperationsDetails();
		
		
	}

	private List<ConformanceOperationsDetails> generateOperationsDetails() {
		List<ConformanceOperationsDetails> res = new LinkedList<>();
		
		List<String> orderedResources = model.getOrderedResources();
				
	    for(String resource : orderedResources) {

			ConformanceOperationsDetails resourceOperation = new ConformanceOperationsDetails(resource); 
			
			List<String> allOps = APIModel.ALL_OPS;
			
			for(String operation : APIModel.ALL_OPS) {
				List<String> paths = model.getPaths(resource, operation);
		    	
				LOG.log(Level.TRACE, "generateFragment: operations: {}", paths);
	
				paths = paths.stream().sorted(Comparator.comparingInt(String::length)).collect(Collectors.toList());
	
				List<OperationConformance> opConf = paths.stream().map(path -> {
	    			String description = APIModel.getOperationDescription(path, operation);
	    			return getPathConformance(path, this.addSentences(description));
				}).collect(toList());
				
				switch(operation) {
				case "GET":
					resourceOperation.get = opConf;
					break;
					
				case "POST":
					resourceOperation.post = opConf;
					resourceOperation.mandatoryAttributes = model.getMandatoryConformanceInPost(resource);
					break;
												
				case "DELETE":
					resourceOperation.delete = opConf;
					break;
					
				case "PUT":
					resourceOperation.put = opConf;
					break;
					
				case "PATCH":
					resourceOperation.patch = opConf;
					resourceOperation.patchableAttributes = model.getMandatoryConformanceInPatch(resource);
					break;
					
				}
				
				Out.debug("resource={} paths={}",  resource, paths);

			}
			
			res.add(resourceOperation);

	    }
	    
	    return res;
	    
	}

	private OperationConformance getPathConformance(String path, String description) {
		OperationConformance res = new OperationConformance(path, description);
		
		return res;
		
	}
	
	private List<ConformanceItem> generateResourceOverview() {
		List<ConformanceItem> res = new LinkedList<>();
		
		for(String resource : model.getOrderedResources()) {
							
			String condition = model.getCondition(resource, RESOURCE);
			String comment = model.getComment(resource);
			
			if(!ConformanceModel.isOptional(condition)) {
				res.add( new ConformanceItem(resource, condition, comment) );
			}
		}
		
		return res;
		
	}

	private List<ConformanceResourceDetails> generateResourceDetails() {	
		return model.getOrderedResources().stream().map(this::getResourceDetailsForResource).collect(toList());
	}

	@LogMethod(level=LogLevel.DEBUG)
	private ConformanceResourceDetails getResourceDetailsForResource(String resource) {
		List<ConformanceItem> res = new LinkedList<>();

		List<String> properties = model.getPropertiesForResource(resource).stream().sorted().collect(Collectors.toList());

		SortedMap<String,ConformanceItem> rowDetails = new java.util.TreeMap<>();
		Set<String> seenResources = new HashSet<>();
		
		for(String property : properties) {
			rowDetails.putAll( createResourceDetailsForProperty(resource, property, seenResources) );
		}
						
		if(Config.getBoolean("compareByLevel")) {
									
			List<String> ordering = arrangeByLevel(rowDetails);

			Map<String,String> processed = new HashMap<>();
						
			for(String key : ordering) {
				res.addAll( getTableRowCompletePath(key, rowDetails, processed) );
			}
	
			
		} else {
			
			Comparator<String> compareRule = Comparator.comparing(String::toString);

			List<String> filtered = rowDetails.keySet().stream()
										.sorted(compareRule)
										.collect(Collectors.toList());
			
			Set<String> processed = new HashSet<>();
			for(String key : filtered) {
				res.addAll( getTableRow(key, rowDetails, processed) );
			}
			
			
		}	

		ConformanceResourceDetails resourceDetails = new ConformanceResourceDetails(resource,res);
		
		return resourceDetails;
		
	}

	@LogMethod(level=LogLevel.DEBUG)
	private List<String> arrangeByLevel(Map<String, ConformanceItem> rowDetails) {
		
		Set<String> allElements = rowDetails.keySet();
		
		List<String> mandatoryPart = rowDetails.entrySet().stream()
				.filter(entry -> getStructuralDepth(entry.getKey())==1 && entry.getValue().condition.contains("M"))
				// .map(entry -> entry.getKey())
				.map(Entry::getKey)
				.sorted()
				.collect(Collectors.toList());
				
		Set<String> remainingPart = rowDetails.keySet().stream()
						.filter(key -> !mandatoryPart.contains(key))
						.collect(Collectors.toSet());
		
		List<String> mandatoryWithSubs = mandatoryPart.stream()
									.filter(key -> !getElementsWithPrefixDirect(allElements, key).isEmpty())
									.sorted()
									.distinct()
									.collect(Collectors.toList());
		
		List<String> childenOfMandatoryPart = mandatoryWithSubs.stream()
								.map(key -> getElementsWithPrefixDirect(allElements, key))
								.flatMap(Set::stream)
								.sorted()
								.distinct()
								.collect(Collectors.toList());
		
		mandatoryPart.removeAll(mandatoryWithSubs);
		
		mandatoryWithSubs.addAll(childenOfMandatoryPart);
		
		mandatoryPart.addAll( mandatoryWithSubs.stream().sorted().collect(Collectors.toList()));
		
		remainingPart.removeAll(mandatoryPart);				
				
		List<String> ordering = new LinkedList<>();
		
		ordering.addAll( getOrderingOfProperties(mandatoryPart));		
		ordering.addAll( getOrderingOfProperties(remainingPart) );
		
		return ordering;
		
	}


	class PropertyCollection {
		List<String> collection;
		int level;
		String label;
		
		List<PropertyCollection> subGroups;
		
		
		PropertyCollection(Collection<String> collection, int level) {
			this(collection,level,"");
		}
		
		PropertyCollection(Collection<String> properties, int level, String groupLabel) {
			this.collection = new LinkedList<>(properties).stream().sorted().collect(Collectors.toList());
			this.level = level;
			this.label = groupLabel;
			this.subGroups = new LinkedList<>();
			
			if(!this.label.isEmpty())
				this.collection.remove(this.label);
			else if(this.label.isEmpty() && collection.size()==1) {
				this.label = collection.iterator().next();
				this.collection.clear();
			}
			
			Comparator<PropertyCollection> sortGroups = Comparator
															.comparing(PropertyCollection::getLevel)
															.thenComparing(PropertyCollection::getSize)
															.thenComparing(PropertyCollection::getLabel)
															;

			if(!this.collection.isEmpty()) {
				this.subGroups = createSubGroups(this.collection);
				
				this.subGroups = this.subGroups.stream()
										.sorted(sortGroups)
										.collect(Collectors.toList());
							
			}
			
		}
		
		public String toString() {
			return "@" + label;
		}
		
		String getLabel() {
			return label;
		}
		
		int getLevel() {
			return level;
		}
		
		int getSize() {
			return collection.size();
		}
				
		List<String> getOrdering() {
			List<String> res = new LinkedList<>();
			
			if(!label.isEmpty()) res.add(label);

			if(!subGroups.isEmpty()) {
				subGroups.forEach(group -> {					
					res.addAll(  group.getOrdering() );
				});
				
			} else {
				res.addAll(  collection.stream().sorted().collect(Collectors.toList()) );
			}
							
			return res;
			
		}

		public void setup() {
			
		}
		
		private List<PropertyCollection> createSubGroups(List<String> properties) {
			List<PropertyCollection> res = new LinkedList<>();
			
			if(properties.isEmpty()) return res;
			
			if(properties.size()==1) {
				return res;
			}
			
			List<String> groups = properties.stream()
									.map(this::getPrefix)
									.sorted()
									.distinct()
									.collect(Collectors.toList());
								
			groups.forEach(group -> {
							
				Set<String> members = properties.stream()
						// .filter(item -> !item.equals(group))
						.filter(item -> item.startsWith(group + ".") || item.contentEquals(group))
						.collect(Collectors.toSet());

				if(members.contains(group)) {
					members.remove(group);
					res.add( new PropertyCollection(members, this.level, group) );
				} else if(!members.isEmpty()) {
					res.add( new PropertyCollection(members,this.level+1) );
				} 
				
			});
			
			return res; 
		}
		
		private String getPrefix(String key) {
			String res=key;
			
			int pos = getNth(key, this.level, '.');
			if(pos>0) res = key.substring(0,pos);
			
			return res;
		}

		private int getNth(String key, int n, int ch) {
			int pos=0;
			while(n>0 && pos>=0) {
				pos=key.indexOf(ch, pos+1);
				n--;
			}
			return pos;
		}

	}
	
	private List<String> getOrderingOfProperties(Collection<String> collection) {
		List<String> res = new LinkedList<>();
		
		OptionalInt level = collection.stream().mapToInt(ConformanceData::getStructuralDepth).min();
		
		if(!level.isPresent()) return res;
				
		PropertyCollection group = new PropertyCollection(collection,level.getAsInt());
								
		List<String> ordering = group.getOrdering();
				
		res.addAll( ordering);
		
		return res;
	}


	private int compareLeafNodeStatus(String s1, String s2, Set<String> collection) {
		Optional<String> nonleaf1 = collection.stream().filter(candidate -> candidate.startsWith(s1+".")).findAny();
		Optional<String> nonleaf2 = collection.stream().filter(candidate -> candidate.startsWith(s2+".")).findAny();
				
		if(nonleaf1.isPresent()) {
			if(nonleaf2.isPresent()) 
				return nonleaf1.get().compareTo(nonleaf2.get());
			else
				return 1;
		} else {
			if(nonleaf2.isPresent())
				return -1;
			else
				return 0;
		}
			
	}

	Comparator<String> compareRule = Comparator
			.comparing(ConformanceData::getStructuralDepth)
			.thenComparing(String::toString);

	
	private Set<String> getElementsWithPrefixDirect(Collection<String> remaining, String key) {
				
		String absolute = key + ".";
		
		return remaining.stream()
					.filter(element -> element.startsWith(absolute))
					.collect(Collectors.toSet());
	}
	

	@LogMethod(level=LogLevel.DEBUG)
	private Map<String,ConformanceItem> createResourceDetailsForProperty(String resource, String property, Set<String> seenResources) {

		String path = resource;
		String condition = model.getResourceCondition(resource,property,path);
		String comment   = model.getResourceComment(resource,property,path);

		LOG.log(Level.TRACE, "comment: {}", comment);

		boolean filter = Config.getBoolean("filterResourceDetails");		
	
		seenResources.clear();
		Map<String,ConformanceItem>  extracted = extractEmbeddedResourceDetails(resource, property, condition, comment, path, seenResources);
				
		return extracted.entrySet().stream()
						.filter(entry -> !filter || (filter && !ConformanceModel.isOptional(entry.getValue().condition)))
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			
	}
		
	@LogMethod(level=LogLevel.DEBUG)
	private static int getStructuralDepth(String key) {
		return key.replaceAll("[^.]", "").length();
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	private List<ConformanceItem> getTableRowCompletePath(String key, Map<String, ConformanceItem> details, Map<String,String> processed) {
		List<ConformanceItem> res = new LinkedList<>();
		
		ConformanceItem values = details.get(key);

		String parentKey = getParentKey(key);
				
		LOG.debug("getTableRowCompletePath: parentKey={} values.label={}", parentKey, values.label);

		boolean processItem=true;
		if(!parentKey.isEmpty() && parentKey.contains(".")) {
			processItem = addTableRowParentItem(res, key, values, processed);
		} 
		
		String resource = model.getResourceByPropertyPath(key);
				
		if(processItem) {
			int indent = parentKey.contains(".") ? 1 : 0;
				
			String comment = model.getResourceComment(key);				
			String condition = model.getResourceCondition(key);

			String ruleText = "";
			
			if(Config.getBoolean("includeCommentsInRules")) {
				ruleText = addSentences(ruleText, comment);
			}
			
			if(processed.containsKey(resource) ) {
				
				LOG.debug("getTableRowCompletePath: processed={} values.label={}", processed, values.label);
				
				String seenAt = processed.get(resource);
				String reference = seenAt.replaceAll("^[^.]+.","");
				
				String referenceText = "See conditions for " + resource + " at " + reference;

				JSONObject schemaDefaults = getSchemaDefaults(resource);
				
				if(schemaDefaults!=null) {
					String schemaComment = schemaDefaults.optString(COMMENT);
					ruleText = ruleText.replace(schemaComment, "");
				} 
												
				ruleText = addSentences(ruleText, referenceText);
				
				String optString = condition.startsWith("M") ? "" : Config.getString("parentPresentCondition");

				ConformanceItem content = new ConformanceItem( values.label + optString, condition, ruleText );
				
				res.add( createTableRow(content,indent) );
				
				processed.put(ROWTITLE + resource, seenAt);
				
			} else {
										
				ConformanceItem content = new ConformanceItem(values.label, values.condition, ruleText);
				
				res.add( createTableRow(content,indent) );

			}			
							
			if(indent==0) {
				processed.put(key, key);	
				if(!resource.isEmpty()) { 
					processed.put(resource, key);
				}
			}
		}	
		
		return res;
		
	}
	
	private String addSentences(String ...sentences) {
		StringBuilder res = new StringBuilder();
		String lastAdded = "";
		for(String s : sentences) {
			if(lastAdded.endsWith(".")) res.append(" ");
			lastAdded = s;
			res.append(lastAdded);
			if(!s.isEmpty() && !s.endsWith(".") && !s.endsWith("\n")) {
				lastAdded = ".";
				res.append(lastAdded);
			}
		}
		return res.toString();
	}

	@LogMethod(level=LogLevel.DEBUG)
	private JSONObject getSchemaDefaults(String resource) {
		return model.getSchemaDefaults(resource);
	}

	private static String ROWTITLE = "ROW_TITLE_";
	
	@LogMethod(level=LogLevel.DEBUG)
	private boolean addTableRowParentItem(List<ConformanceItem> res, String key, ConformanceItem values, Map<String, String> processed) {
		
		boolean processItem=true;
		
		String parentKey = getParentKey(key);
		String parentResource = model.getResourceByPropertyPath(parentKey);	
		String parent = parentKey.replaceFirst("^[^.]+.", "");
			
		String comment = model.getResourceComment(parentKey);
		
		String condition = model.getResourceCondition(parentKey);

		String ruleText = "";

		if(Config.getBoolean("includeCommentsInRules")) {
			if(!comment.isEmpty() && !ruleText.isEmpty()) ruleText = ruleText + "\n\n";
			ruleText = addSentences(ruleText, comment);
		}
		
		if(Config.getBoolean("minimizeResourceDetails")) {
											
			String seenAt = processed.get(parentResource);
		
			if(processed.containsKey(ROWTITLE + parentResource)) {
				processItem=false;
				
				if(!processed.containsKey(parentKey) ) {				
					res.add( getSeeConditions(ruleText, condition, seenAt, parent, parentResource) );
					
					processed.put(parentKey, parentKey);		

				} 
				
			} else if(!processed.containsKey(parentResource)) {
				
				String optString = condition.startsWith("M") ? "" : Config.getString("parentPresentCondition");
																
				res.add( new ConformanceItem(parent + optString, "", ruleText));
				
				processed.put(parentResource,parentKey);
				processed.put(parentKey,parentKey);
				
			} else {
				
				if(!processed.containsKey(parentKey) && !processed.containsKey(ROWTITLE + parentResource)) {
				
					res.add( getSeeConditions(ruleText, condition, seenAt, parent, parentResource) ); 

					processed.put(ROWTITLE + parentResource, parentKey);	
					processed.put(parentKey, parentKey);		

					processItem=false;
				} 
				
			}
			
			
		} else {
			if(!processed.containsKey(ROWTITLE + parentKey)) {
				
				JSONObject schemaDefaults = getSchemaDefaults(parentResource);
	
				res.add( createTableRow(new ConformanceItem(parent + Config.getString("parentPresentCondition"), condition, ruleText), 0) );
				processed.put(ROWTITLE + parentKey,parentKey);
			} 
		}
		
			
		return processItem;
	}


	private ConformanceItem getSeeConditions(String ruleText, String condition, String seenAt, String parent, String parentResource) {
		
		String reference = seenAt.replaceAll("^[^.]+.","");
		
		String referenceText = "See conditions for " + parentResource + " at " + reference;
				
		JSONObject schemaDefaults = getSchemaDefaults(parentResource);
		
		if(schemaDefaults!=null) {
			String schemaComment = schemaDefaults.optString(COMMENT);
			ruleText = ruleText.replace(schemaComment, "");
		} 
		
		ruleText = addSentences(ruleText, referenceText);
		
		String optString = condition.startsWith("M") ? "" : Config.getString("parentPresentCondition");

		ConformanceItem content = new ConformanceItem(parent + optString, condition, ruleText);
		
		return content;
		
	}


	@LogMethod(level=LogLevel.DEBUG)
	protected List<ConformanceItem> getTableRow(String key, Map<String, ConformanceItem> details, Set<String> processed) {
		List<ConformanceItem> res = new LinkedList<>();
		
		ConformanceItem conformanceItem = details.get(key);

		if(conformanceItem==null) conformanceItem = new ConformanceItem( removeResourceFromKey(key) );
		
		int indent = getStructuralDepth(key);
		if(indent>0) indent--;

		String parentKey = getParentKey(key);
		
		if(parentKey.contains(".") && !processed.contains(parentKey)) {
			res.addAll(getTableRow(parentKey, details, processed) );
		}
		
		res.add( createTableRow(conformanceItem, indent) );	
		
		processed.add(key);
				
		return res;
	}


	@LogMethod(level=LogLevel.DEBUG)
	private String getParentKey(String str) {
		String res=str;
		
		int lastPos = res.lastIndexOf('.');
		if(lastPos>0) res=res.substring(0,lastPos);
		
		return res;
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	private String removeResourceFromKey(String str) {
		String res=str;
		
		int firstPos = res.indexOf('.');
		if(firstPos>0) res=res.substring(firstPos+1);
		
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	private Map<String,ConformanceItem>  extractEmbeddedResourceDetails(String resource, String property,
												String condition, String comment, String path, Set<String> seenResources) {

		Map<String,ConformanceItem> res = new HashMap<>();
				
		String referencedType = APIModel.getReferencedType(resource,property);

		String subPath = property;
		if(!path.isEmpty()) subPath = path + "." + property;

		if(!referencedType.isEmpty() && seenResources.contains(referencedType)) {			
			res.put(subPath, new ConformanceItem(property, condition, comment) );	
			return res;
		}

		if(!referencedType.isEmpty()) seenResources.add(referencedType);
		if(!seenResources.contains(resource)) seenResources.add(resource);
		
		res.put(subPath, new ConformanceItem(property, condition, comment));	
			
		Set<String> properties = APIModel.getPropertiesForResource(referencedType);

		for(String prop : properties) {
			
			condition = model.getResourceCondition(referencedType,prop,subPath);
			comment = model.getResourceComment(referencedType,prop,subPath);

			res.putAll(extractEmbeddedResourceDetails(referencedType, prop, condition,  comment, subPath, seenResources));
		}
				
		if(!referencedType.isEmpty()) seenResources.remove(referencedType);
		
		return res;
		
	}

	private ConformanceItem createTableRow(ConformanceItem item, int indent) {
		item.setSubordinate(indent>0);
		return item;
	}

	
}
