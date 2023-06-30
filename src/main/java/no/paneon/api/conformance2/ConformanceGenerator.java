package no.paneon.api.conformance2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import no.paneon.api.conformance.ConformanceItem;
import no.paneon.api.conformance.ConformanceModel;
import no.paneon.api.generator.GenerateCommon;
import no.paneon.api.graph.APIGraph;
import no.paneon.api.graph.Node;
import no.paneon.api.logging.AspectLogger.LogLevel;
import no.paneon.api.logging.LogMethod;
import no.paneon.api.model.APIModel;
import no.paneon.api.utils.Config;
import no.paneon.api.utils.JSONObjectOrArray;
import no.paneon.api.utils.Out;
import no.paneon.api.utils.Timestamp;
import no.paneon.api.utils.Utils;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import no.paneon.api.conformance.ConformanceModel;
import no.paneon.api.tooling.Args;
import no.paneon.api.tooling.userguide.UserGuideData;
import no.paneon.api.tooling.userguide.UserGuideGenerator;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.SortedMap;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;


public class ConformanceGenerator {

	static final Logger LOG = LogManager.getLogger(ConformanceGenerator.class);
		
	private static final String EXCLUDED_RESOURCES = "userguide::excludedResources";

	private static final String RESOURCE = "resource";
	private static final String RESOURCES = "resources";

	private static final String OPERATIONS = "operations";		
	private static final String NOTIFICATIONS = "notifications";
	
	private static final String OPERATIONS_DETAILS = "operations-details";		
	private static final String RESOURCE_DETAILS = "resource-details";

	private static final String ATTRIBUTES = "attributes";
	
	private static final String COMMENT = "comment";
	
	Args.ConformanceGuide args;
	
	APIGraph apiGraph;
	
	ConformanceModel conformance;

	ConformanceData conformanceData;

	GenerateConformanceGuide generator;
	public ConformanceGenerator(GenerateConformanceGuide generator) {
		
		this.generator = generator;
		
		this.args = generator.args;	
		this.conformance = generator.model;				
		this.conformanceData = new ConformanceData(this.conformance);

	}


	@LogMethod(level=LogLevel.DEBUG)
	public void generateDocument() {
								
		try {
									
			generateConformanceData();
				
			Timestamp.timeStamp("finished conformance data");

			boolean keepExisting=true;

			Map<String,String> filesToCopy = Config.getMap("conformance.filesToCopy");

			LOG.debug("generateDocument: filesToCopy={}",  filesToCopy.keySet());
			
			generator.copyFiles(filesToCopy, keepExisting); // generator.copyFiles(filesToCopy, args.generatedOnly);

			generatePartials(conformanceData);
			
			generator.processTemplates(this.args, conformanceData, "conformance.generated.templates", "conformance.templates", keepExisting);
					
		} catch(Exception ex) {
			Out.printAlways("... error generating userguide: exception=" + ex.getLocalizedMessage());
			ex.printStackTrace();
			System.exit(0);
		}
				
	}
	

	private void processTemplatesHelper(ConformanceData data, Map<String, String> templates, String directory) {
		
		LOG.debug("processTemplatesHelper: data {}", data.resourceDetails.stream().map(Object::toString).collect(Collectors.joining("\n")));

		templates.entrySet().stream().forEach(entry -> {
			String template = entry.getKey();
			String destination = entry.getValue();
			
			LOG.debug("processTemplatesHelper: {}", template);
			
			if(destination.contentEquals("$output")) destination = args.outputFileName;

			processTemplate(template, data, directory + destination);

		});		
	}


	protected void processTemplate(String template, Object userguide, String outputFileName) {
		
		try {
			MustacheFactory mf = new DefaultMustacheFactory();
			InputStream is = Utils.getFileInputStream(template, null, args.workingDirectory, args.templateDirectory);
			Reader reader = new InputStreamReader(is);
			Mustache m = mf.compile(reader, "template");
	
			StringWriter writer = new StringWriter();
			
			m.execute(writer, userguide).flush();
			
			String text = writer.toString();
						
			Utils.save(text, outputFileName);	
			
		} catch(Exception ex) {
			Out.debug("Exception: {}", ex.getLocalizedMessage());
		}
	
	}

	private ConformanceData generatePartials(ConformanceData data) {

		Map<String,String> numberAndCopy = Config.getMap("conformance.filesToNumber");
		int number = Config.getInteger("conformance.filesToNumber.start");
		String destination = Config.getString("conformance.filesToNumber.destination");

		Map<String,String> filesToCopy = new HashMap<>();

		List<String> operationsOrdered = this.conformance.getAllUsedOperationsOrdered();
		
		LOG.debug("generatePartials:: operationsOrdered={}", operationsOrdered);
		
		for(String operation : operationsOrdered ) {
			String source = numberAndCopy.get(operation);

			String formattedNumber = String.format("%02d", number);
			String target = source.replace("0x",  formattedNumber);
			
			target = Utils.getBaseFileName(target);
			
			filesToCopy.put(source,  destination + "/" + target);
			number++;
			numberAndCopy.remove(operation);
			
		}
		
		
		APIModel.ALL_OPS.stream().forEach(op -> numberAndCopy.remove(op));

		for( Entry<String,String> entry : numberAndCopy.entrySet() ) {
			String source = entry.getValue();

			String formattedNumber = String.format("%02d", number);
			String target = source.replace("0x",  formattedNumber);
			
			target = Utils.getBaseFileName(target);

			filesToCopy.put(source,  destination + "/" + target);
			number++;
		}
		
		LOG.debug("generatePartials:: filesToCopy={}", filesToCopy);

		data.parts = generator.copyFilesWithDestination(filesToCopy).stream().map(file -> new ConformanceData.FileData(file)).collect(toList());
				
		return data;
		
	}

	private void generateConformanceData() {
		this.conformanceData.setup();
	}

	
//	private Map<String, Object> getModelFromJson(JSONObject json) throws JSONException {
//	    Map<String,Object> out = new HashMap<String,Object>();
//
//	    Iterator it = json.keys();
//	    while (it.hasNext()) {
//	        String key = (String)it.next();
//
//	        if (json.get(key) instanceof JSONArray) {
//
//	            // Copy an array
//	            JSONArray arrayIn = json.getJSONArray(key);
//	            List<Object> arrayOut = new ArrayList<Object>();
//	            for (int i = 0; i < arrayIn.length(); i++) {
//	                Object item = arrayIn.get(i);
//	                if(item instanceof JSONObject) {
//	                	Map<String, Object> items = getModelFromJson((JSONObject)item);
//	                	arrayOut.add(items);
//	                } else {
//	                	arrayOut.add(item.toString());
//	                }
//	            }
//	            out.put(key, arrayOut);
//	        }
//	        else {
//
//	            // Copy a primitive string
//	            out.put(key, json.getString(key));
//	        }
//	    }
//
//	    return out;
//	}

	
	@LogMethod(level=LogLevel.DEBUG)	
	protected String getJSON(String resource, JSONObject config) {
				
		String fileName = Utils.getFileName(args.workingDirectory, config, "tableSource").replace("${RESOURCE}", resource);
				
		JSONObjectOrArray json = JSONObjectOrArray.readJSONObjectOrArray(fileName);
		
		String content = json.toString(2);
						
		return content;
		
	}


	@LogMethod(level=LogLevel.DEBUG)
	protected String constructStatement(String ... statements) {
		StringBuilder res = new StringBuilder();
		for(String s : statements) {
			String cleaned =  s.trim();
			
			if(!cleaned.isEmpty() && !cleaned.endsWith(".")) cleaned=cleaned+".";
			
			cleaned = Utils.upperCaseFirst(cleaned);
			
			if(res.length()>0) res.append(" ");
			res.append(cleaned);
		}
		return res.toString();
	}


	@LogMethod(level=LogLevel.DEBUG)
	protected String getDescriptionForType(APIGraph apiGraph, String type) {
		String res="";
		Node node = apiGraph.getNode(type);
		if(node!=null) res=node.getDescription();
		return res;
	}

	private final List<String> VOCALS = Arrays.asList("A", "E", "I", "O" );

	@LogMethod(level=LogLevel.DEBUG)
	protected String getArticle(String type) {
		String res="a";
		if(!type.isEmpty()) {
			String firstChar = type.substring(0, 1).toUpperCase();
			if(VOCALS.contains(firstChar)) {
				res = "an";
			}
		}
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	protected String constructDescriptionForType(String type) {
		String res=type;
		if(!type.isEmpty()) {
			String firstChar = type.substring(0, 1).toUpperCase();
			if(VOCALS.contains(firstChar)) {
				res = "An " + type;
			} else {
				res = "A " + type;
			}
		}
		return res;
	}


	@LogMethod(level=LogLevel.DEBUG)
	public List<String> getResources() {
		List<String> resources = APIModel.getResources();
		
		final List<String> excludedResources = Config.get(EXCLUDED_RESOURCES);
		
		Predicate<String> notExcludedResource  = s -> !excludedResources.contains(s);
		
		resources = resources.stream()
						.filter(notExcludedResource)
						.sorted(Comparator.comparingInt(String::length))
						.collect(toList());
		
		return resources;
	}
	
	public String getTargetDirectory(String subdir) {
		return getTargetDirectory(subdir, args.targetDirectory);		
	}
	
	public String getGeneratedTargetDirectory(String subdir) {
		String target=args.generatedTargetDirectory;
		if(target==null) target = Config.getString("conformance.generatedTarget");
		if(target==null || target.isBlank()) target=args.targetDirectory;
		return getTargetDirectory(subdir, target);
	}
	
	public String getTargetDirectory(String subdir, String commandArg) {
		String targetDir;
		
		if(commandArg!=null && !commandArg.isEmpty()) {
			targetDir = commandArg + "/" + subdir;
		} else {
			targetDir = args.workingDirectory!=null ? args.workingDirectory : System.getProperty("user.dir");
			
			File dir = new File(targetDir + "/documentation");
			if(dir.isDirectory()) {
				targetDir = targetDir + "/documentation";
			}
			
			targetDir = targetDir + "/" + subdir;

		}
		
		Utils.createDirectory(targetDir);	
			
		return targetDir;
		
	}
	


	@LogMethod(level=LogLevel.DEBUG)
	private List<String[]> getTableValues(String fragment, JSONObject config) {
		List<String[]> res = new LinkedList<>();

		switch(config.optString("type")) {

		case RESOURCE: 
			List<String> orderedItems = conformance.getOrderedResources();
			
			for(String resource : orderedItems) {
								
				String condition = conformance.getCondition(resource, RESOURCE);
				String comment = conformance.getComment(resource);
				
				addConformanceItem(res, resource, condition, comment);

				Out.debug("getTableValues:: resource={} condition={} comment={}", resource, condition, comment);

			}
			break;

		case NOTIFICATIONS:
			List<String> allNotificationsByResource = new LinkedList<>();
			List<String> orderedResources = conformance.getOrderedList( conformance.getResources(), conformance.getOrdering(RESOURCES));
			
			for(String resource: orderedResources) {
				allNotificationsByResource.addAll(conformance.getNotificationsByResource(resource));
			}
						
			List<String> displaySequence = new LinkedList<>(conformance.getOrdering(NOTIFICATIONS));
			displaySequence.retainAll(allNotificationsByResource);
			
			for(String item : allNotificationsByResource) {
				if(!displaySequence.contains(item)) displaySequence.add(item);
			}

			for(String notification : displaySequence) {
				String condition = conformance.getCondition(notification,"notification");
				String comment = conformance.getComment(notification);
				
				addConformanceItem(res, notification, condition, comment);

			}

			break;

		case OPERATIONS: 
						
			List<String> ops = conformance.getAllUsedOperationsOrdered();
			
			for(String op : ops ) {				
				String condition = conformance.getOperationConditions(op,ops);
				String comment = conformance.getOperationComments(op);

				addConformanceItem(res, op, condition, comment);

			}
			break;

		default:
			Out.println("... ... unable to process ", fragment, "in configuration", config.toString(2));

		}

		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	private void addConformanceItem(List<String[]> res, String label, String condition, String comment) {
		if(!ConformanceModel.isOptional(condition))
			res.add(new String[] {label, condition, comment});		
	}



	@LogMethod(level=LogLevel.DEBUG)
	public Map<String,Object> getResourceDetails(JSONObject config) {
		Map<String,Object> res = new HashMap<>();
		
		for(String resource : conformance.getOrderedResources()) {
			res.put(resource, getResourceDetailsForResource(resource, config) );
			
			Out.debug("getResourceDetails:: resource={}", resource);

		}

		return res;

	}


	@LogMethod(level=LogLevel.DEBUG)
	private List<String[]> getResourceDetailsForResource(String resource, JSONObject config) {
		List<String[]> res = new LinkedList<>();

		List<String> properties = conformance.getPropertiesForResource(resource).stream().sorted().collect(Collectors.toList());

		SortedMap<String,String[]> rowDetails = new java.util.TreeMap<>();
		Set<String> seenResources = new HashSet<>();
		
		for(String property : properties) {
			rowDetails.putAll( createResourceDetailsForProperty(resource, property, config, seenResources) );
		}
					
		LOG.debug("getResourceDetailsForResource: rowDetails = {}", rowDetails);
		
		if(Config.getBoolean("compareByLevel")) {
									
			List<String> ordering = arrangeByLevel(rowDetails);
								
			Map<String,String> processed = new HashMap<>();
						
			for(String key : ordering) {
				res.addAll( getTableRowCompletePath(config, key, rowDetails, processed) );
			}
	
			
		} else {
			
			Comparator<String> compareRule = Comparator.comparing(String::toString);

			List<String> filtered = rowDetails.keySet().stream()
										.sorted(compareRule)
										.collect(Collectors.toList());
			
			Set<String> processed = new HashSet<>();
			for(String key : filtered) {
				res.addAll( getTableRow(config, key, rowDetails, processed) );
			}
			
			
		}	

		return res;
		
	}

	@LogMethod(level=LogLevel.DEBUG)
	private List<String> arrangeByLevel(Map<String, String[]> rowDetails) {
		
		Set<String> allElements = rowDetails.keySet();
		
		List<String> mandatoryPart = rowDetails.entrySet().stream()
				.filter(entry -> getStructuralDepth(entry.getKey())==1 && entry.getValue()[1].contains("M"))
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


	private List<String> arrangeByLevel_old(Map<String, String[]> rowDetails) {
		List<String> mandatoryPart = rowDetails.entrySet().stream()
				.filter(entry -> getStructuralDepth(entry.getKey())==1 && entry.getValue()[1].contains("M"))
				// .map(entry -> entry.getKey())
				.map(Entry::getKey)
				.sorted()
				.collect(Collectors.toList());

		Set<String> remainingPart = rowDetails.keySet().stream()
						.filter(key -> !mandatoryPart.contains(key))
						.collect(Collectors.toSet());
		
		List<String> mandatoryWithSubs = mandatoryPart.stream()
									.filter(key -> !getElementsWithPrefixDirect(rowDetails.keySet(), key).isEmpty())
									.sorted()
									.distinct()
									.collect(Collectors.toList());
		
		List<String> childenOfMandatoryPart = mandatoryWithSubs.stream()
								.map(key -> getElementsWithPrefixDirect(rowDetails.keySet(), key))
								.flatMap(Set::stream)
								.sorted()
								.distinct()
								.collect(Collectors.toList());
		
		mandatoryPart.removeAll(mandatoryWithSubs);
		
		mandatoryWithSubs.addAll(childenOfMandatoryPart);
		
		mandatoryPart.addAll( mandatoryWithSubs.stream().sorted().collect(Collectors.toList()));
		
		remainingPart.removeAll(mandatoryPart);				
		
		List<String> ordering = new LinkedList<>(mandatoryPart);
		
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
		
		OptionalInt level = collection.stream().mapToInt(ConformanceGenerator::getStructuralDepth).min();
		
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
			.comparing(ConformanceGenerator::getStructuralDepth)
			.thenComparing(String::toString);

	
	private Set<String> getElementsWithPrefixDirect(Collection<String> remaining, String key) {
				
		String absolute = key + ".";
		
		return remaining.stream()
					.filter(element -> element.startsWith(absolute))
					.collect(Collectors.toSet());
	}
	

	@LogMethod(level=LogLevel.DEBUG)
	private Map<String,String[]> createResourceDetailsForProperty(String resource, String property, JSONObject config, Set<String> seenResources) {

		String path = resource;
		String condition = conformance.getResourceCondition(resource,property,path);
		String comment   = conformance.getResourceComment(resource,property,path);

		LOG.log(Level.TRACE, "comment: {}", comment);

		boolean filter = Config.getBoolean("filterResourceDetails");		
	
		seenResources.clear();
		Map<String,String[]>  extracted = extractEmbeddedResourceDetails(resource, property, condition, comment, path, config, seenResources);
				
		return extracted.entrySet().stream()
						.filter(entry -> !filter || (filter && !ConformanceModel.isOptional(entry)))
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			
	}
		
	@LogMethod(level=LogLevel.DEBUG)
	private static int getStructuralDepth(String key) {
		return key.replaceAll("[^.]", "").length();
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	private List<String[]> getTableRowCompletePath(JSONObject config, String key, Map<String, String[]> details, Map<String,String> processed) {
		List<String[]> res = new LinkedList<>();
		
		String[] values = details.get(key);

		String parentKey = getParentKey(key);
				
		boolean processItem=true;
		if(!parentKey.isEmpty() && parentKey.contains(".")) {
			processItem = addTableRowParentItem(res, config, key, values, processed);
		} 
		
		String resource = conformance.getResourceByPropertyPath(key);
				
		if(processItem) {
			int indent = parentKey.contains(".") ? 1 : 0;
				
			String comment = conformance.getResourceComment(key);				
			String condition = conformance.getResourceCondition(key);

			String ruleText = "";
			
			if(!condition.isEmpty() && !condition.startsWith("O")) ruleText = condition;

			if(Config.getBoolean("includeCommentsInRules")) {
				if(!comment.isEmpty() && !ruleText.isEmpty()) ruleText = ruleText + "\n\n";
				ruleText = ruleText + comment;
			}
			
			if(processed.containsKey(resource) ) {
				
				String seenAt = processed.get(resource);
				String reference = seenAt.replaceAll("^[^.]+.","");
				
				String referenceText = "See conditions for " + resource + " at " + reference;

				JSONObject schemaDefaults = getSchemaDefaults(resource);
				
				if(schemaDefaults!=null) {
					String schemaComment = schemaDefaults.optString(COMMENT);
					ruleText = ruleText.replace(schemaComment, "");
				} else {
					if(!ruleText.isEmpty()) ruleText = ruleText + "\n\n";
				}
												
				ruleText = ruleText + referenceText;
				
				String optString = condition.startsWith("M") ? "" : Config.getString("parentPresentCondition");

				String[] content = {  values[0] + optString, ruleText };
				
				res.add( content );
				
				processed.put(ROWTITLE + resource, seenAt);
				
			} else {
										
				String[] content = {  values[0], ruleText };
				
				res.add( content );

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
	
	@LogMethod(level=LogLevel.DEBUG)
	private JSONObject getSchemaDefaults(String resource) {
		return conformance.getSchemaDefaults(resource);
	}

	private static String ROWTITLE = "ROW_TITLE_";
	
	@LogMethod(level=LogLevel.DEBUG)
	private boolean addTableRowParentItem(List<String[]> res, JSONObject config, String key, String[] values, Map<String, String> processed) {
		
		boolean processItem=true;
		
		String parentKey = getParentKey(key);
		String parentResource = conformance.getResourceByPropertyPath(parentKey);	
		String parent = parentKey.replaceFirst("^[^.]+.", "");
			
		String comment = conformance.getResourceComment(parentKey);
		
		String condition = conformance.getResourceCondition(parentKey);

		String ruleText = "";
		if(condition.startsWith("M")) ruleText = condition;

		if(Config.getBoolean("includeCommentsInRules")) {
			if(!comment.isEmpty() && !ruleText.isEmpty()) ruleText = ruleText + "\n\n";
			ruleText = ruleText + comment;
		}
		
		if(Config.getBoolean("minimizeResourceDetails")) {
											
			String seenAt = processed.get(parentResource);
		
			if(processed.containsKey(ROWTITLE + parentResource)) {
				processItem=false;
				
				if(!processed.containsKey(parentKey) ) {				
					res.add( getSeeConditions(config, ruleText, condition, seenAt, parent, parentResource) );
					
					processed.put(parentKey, parentKey);		

				} 
				
			} else if(!processed.containsKey(parentResource)) {
				
				String optString = condition.startsWith("M") ? "" : Config.getString("parentPresentCondition");
												
				String[] content = {  parent + optString, ruleText };
				// content is result createTableRow(table, content, config, 0);
				
				processed.put(parentResource,parentKey);
				processed.put(parentKey,parentKey);
				
			} else {
				
				if(!processed.containsKey(parentKey) && !processed.containsKey(ROWTITLE + parentResource)) {
				
					res.add( getSeeConditions(config, ruleText, condition, seenAt, parent, parentResource) ); 

					processed.put(ROWTITLE + parentResource, parentKey);	
					processed.put(parentKey, parentKey);		

					processItem=false;
				} 
				
			}
			
			
		} else {
			if(!processed.containsKey(ROWTITLE + parentKey)) {
				
				JSONObject schemaDefaults = getSchemaDefaults(parentResource);
	
				String[] content = {  parent + Config.getString("parentPresentCondition"), ruleText };
				// content is result -- createTableRow(table, content, config, 0);	
				processed.put(ROWTITLE + parentKey,parentKey);
			} 
		}
		
			
		return processItem;
	}


	private String[] getSeeConditions(JSONObject config, String ruleText, String condition, String seenAt, String parent, String parentResource) {
		
		String reference = seenAt.replaceAll("^[^.]+.","");
		
		String referenceText = "See conditions for " + parentResource + " at " + reference;
				
		JSONObject schemaDefaults = getSchemaDefaults(parentResource);
		
		if(schemaDefaults!=null) {
			String schemaComment = schemaDefaults.optString(COMMENT);
			ruleText = ruleText.replace(schemaComment, "");
		} else {
			if(!ruleText.isEmpty()) ruleText = ruleText + "\n\n";
		}
		
		ruleText = ruleText + referenceText;
		
		String optString = condition.startsWith("M") ? "" : Config.getString("parentPresentCondition");

		String[] content = {  parent + optString, ruleText };
		
		return content;
		
	}


	@LogMethod(level=LogLevel.DEBUG)
	protected List<String[]> getTableRow(JSONObject config, String key, Map<String, String[]> details, Set<String> processed) {
		List<String[]> res = new LinkedList<>();
		
		String[] values = details.get(key);

		if(values==null) values = new String[] { removeResourceFromKey(key), "", "" };
		
		int indent = getStructuralDepth(key);
		if(indent>0) indent--;

		String parentKey = getParentKey(key);
		
		if(parentKey.contains(".") && !processed.contains(parentKey)) {
			res.addAll(getTableRow(config, parentKey, details, processed) );
		}
		
		res.add( createTableRow(values, config, indent) );	
		
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
	private Map<String,String[]>  extractEmbeddedResourceDetails(String resource, String property,
												String condition, String comment, String path, JSONObject config,
												Set<String> seenResources) {

		Map<String,String[]> res = new HashMap<>();
				
		String referencedType = APIModel.getReferencedType(resource,property);

		String subPath = property;
		if(!path.isEmpty()) subPath = path + "." + property;

		if(!referencedType.isEmpty() && seenResources.contains(referencedType)) {			
			res.put(subPath, new String[] {property, condition, comment });	
			return res;
		}

		if(!referencedType.isEmpty()) seenResources.add(referencedType);
		if(!seenResources.contains(resource)) seenResources.add(resource);
		
		res.put(subPath, new String[] {property, condition, comment});	
			
		Set<String> properties = APIModel.getPropertiesForResource(referencedType);

		for(String prop : properties) {
			
			condition = conformance.getResourceCondition(referencedType,prop,subPath);
			comment = conformance.getResourceComment(referencedType,prop,subPath);

			res.putAll(extractEmbeddedResourceDetails(referencedType, prop, condition,  comment, subPath, config, seenResources));
		}
				
		// 2020-07-10: Added back
		if(!referencedType.isEmpty()) seenResources.remove(referencedType);
		
		return res;
		
	}

	private String[] createTableRow(String[] values, JSONObject config, int indent) {
		List<String> tmp = new LinkedList<>();
		for(int i=0; i<indent; i++)  tmp.add("");
		for(String s : values) tmp.add(s);
		return tmp.toArray(new String[0]);
	}

	
}
