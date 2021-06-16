package no.paneon.api.tooling.userguide;

import org.json.JSONArray;
import org.json.JSONObject;

import no.paneon.api.conformance.ConformanceItem;
import no.paneon.api.conformance.ConformanceModel;
import no.paneon.api.graph.APIGraph;
import no.paneon.api.graph.Node;
import no.paneon.api.graph.Property;
import no.paneon.api.logging.AspectLogger.LogLevel;
import no.paneon.api.logging.LogMethod;
import no.paneon.api.model.APIModel;
import no.paneon.api.utils.Config;
import no.paneon.api.utils.JSONObjectOrArray;
import no.paneon.api.utils.Out;
import no.paneon.api.utils.Timestamp;
import no.paneon.api.utils.Utils;

import no.paneon.api.tooling.Args;
import no.paneon.api.tooling.userguide.UserGuideData.DiagramData;
import no.paneon.api.tooling.userguide.UserGuideData.FieldsData;
import no.paneon.api.tooling.userguide.UserGuideData.ResourceData;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;

import static java.util.stream.Collectors.toList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResourcesFragment {

	static final Logger LOG = LogManager.getLogger(ResourcesFragment.class);

	private static final String RESOURCE_MODEL	 	= "resourceModel";
	private static final String FIELD_DESCRIPTIONS 	= "fieldDescription";
	
	private static final String RESOURCE_TABLE = "resourceTable";
	
	private static final String JSON_REPRESENTATIONS = "jsonRepresentations";
	private static final String JSON_TABLE = "jsonTable";

	private static final String BLANK_LINE = "";
	
	private static final String META_PROPERTIES = "userguide::metaProperties";
				
	Args.UserGuide args;
	
	UserGuideGenerator generator;
	UserGuideData userGuideData;

	public ResourcesFragment(UserGuideGenerator generator) {	
		this.generator = generator;		
		this.args = generator.args;
		this.userGuideData = generator.userGuideData;
		
	}


	@LogMethod(level=LogLevel.DEBUG)
	protected void process() {
		
		userGuideData.resources = new HashMap<>();
		userGuideData.resourcesData = new LinkedList<>();

		JSONObject config = Config.getConfig("userguide::resourceFragments");

		List<String> directories = new LinkedList<>();
		directories.add(args.workingDirectory);
		directories.add(args.workingDirectory + "/documentation/diagrams" );
		directories.add("");
		
		String filename = args.diagrams!=null ? args.diagrams : "diagrams.yaml";
				
		try {
			
			File file = Utils.getFile(filename, directories);
			if(file==null || !file.exists()) {
				Out.printAlways("... unable to locate the configuration file with diagram source locations (" + filename + ")" );
				System.exit(0);
			}
			
			InputStream is = new FileInputStream(file);
			config.put("diagrams", Utils.readJSONOrYaml(is));
			config.put("diagrams-directory", file.getParent() );

		} catch(Exception ex) {
			Out.printAlways("... unable to read the configuration file with diagram source locations (" + filename + ")" );
			ex.printStackTrace();
			System.exit(0);
		}
		
		List<String> resources = generator.getResources();
				
		for(String resource : resources) {		
			UserGuideData.ResourceData data = createResourceDetailsForResource(resource, config);
			userGuideData.resources.put(resource, data);
			userGuideData.resourcesData.add(data);
		}
						
	}
	

	@LogMethod(level=LogLevel.DEBUG)
	private UserGuideData.ResourceData createResourceDetailsForResource(String resource, JSONObject config) {
		UserGuideData.ResourceData res = userGuideData.new ResourceData();
	
		APIGraph apiGraph = new APIGraph(resource);

		res.resource = resource;		
		
		res.resourceArticle = generator.getArticle(resource);
		res.description = generator.constructStatement(apiGraph.getNode(resource).getDescription() );		
		res.diagrams = getResourceDiagrams(apiGraph, resource, config);		
		res.fields = getFieldDescriptions(apiGraph, resource, config);
		res.sample = getJSONRepresentations(apiGraph, resource, config);

		return res;
		
	}

	
	@LogMethod(level=LogLevel.DEBUG)
	private List<UserGuideData.DiagramData> getResourceDiagrams(APIGraph apiGraph, String resource, JSONObject config) {
		List<UserGuideData.DiagramData> res = new LinkedList<>();

		JSONObject diagramConfig = config.optJSONObject("diagrams");
		
		LOG.debug("diagramConfig:{} " , diagramConfig);
				  				
		if(diagramConfig.has("graphs")) {
					
			Predicate<JSONObject> isNotNull = o -> o != null;

			Predicate<String> isDiagramForResource = s -> s.contentEquals(resource) || s.startsWith(resource + "_");

			Predicate<JSONObject> diagramForResource = o -> o.keySet().stream().anyMatch(isDiagramForResource);
						
			JSONArray diagramArray = diagramConfig.optJSONArray("graphs");
			
			List<JSONObject> diagrams = extractJSONObjects(diagramArray).stream()
											.filter(JSONObject.class::isInstance)
											.map(JSONObject.class::cast)
											.filter(isNotNull)
											.collect(toList());
			
			Optional<JSONObject> optResourceDiagram = diagrams.stream().filter(diagramForResource).findFirst();
			
			if(optResourceDiagram.isPresent()) {
					
				res.add( getDiagramDetails(optResourceDiagram.get(), resource, config) );
							
				Set<String> subDiagrams = diagramConfig.keySet().stream().filter(s -> s.startsWith(resource + "_")).collect(toSet());
				
				for(String subDiagram : subDiagrams) {
					
					Out.debug("resource: {} subDiagram:{} " , resource, subDiagram);

					res.add( getDiagramDetails(diagramConfig, resource, subDiagram, config) );
				}
				
			} else {
				Out.debug("... possible incorrect diagram configuration: resource " + resource + " not found in {}", diagramConfig);
			}
		} else {
			Out.debug("... possible incorrect diagram configuration: 'graphs' not found in {}", diagramConfig);
		}
		
		return res;
		
	}

	private Collection<JSONObject> extractJSONObjects(JSONArray array) {
		List<JSONObject> res = new LinkedList<>();
		for(int i=0; i<array.length(); i++) {
			JSONObject o = array.optJSONObject(i);
			if(o!=null) res.add(o);
		}
		return res;
	}


	private DiagramData getDiagramDetails(JSONObject diagramConfig, String resource, JSONObject config) {
		return getDiagramDetails(diagramConfig, resource, resource, config) ;
	}


	private UserGuideData.DiagramData getDiagramDetails(JSONObject diagramConfig, String resource, String activeResource, JSONObject config) {
		String imgFile = diagramConfig.getString(activeResource);
		
		String dir = args.workingDirectory + "/documentation/diagrams/";
		if(config.has("diagrams-directory")) {
			dir = config.getString("diagrams-directory") + "/";
		}
		
		LOG.debug("dir=" + config.getString("diagrams-directory") );
		
		UserGuideData.DiagramData data = userGuideData.new DiagramData();

		String fileName = dir + imgFile.replace(".png",  ".puml");
		try {
			String puml = Utils.readFile(fileName);		
			data.puml = puml;

		} catch(Exception ex) {
			Out.printAlways("... unable to read from file: " + fileName);
			return data;
		}

		String sourceDir = new File(dir).getPath();
		
		data.sourceLocation = sourceDir;		
		data.imgfile = imgFile;

		data.resource = activeResource;
		
		if(activeResource.contains("_")) {
			activeResource =  activeResource.replace(resource + "_","");
			data.resourceLabel = activeResource;
		} else {
			data.resourceLabel = resource;			
		}
		
		return data;
	}


	@LogMethod(level=LogLevel.DEBUG)
	private UserGuideData.FieldsData getFieldDescriptions(APIGraph apiGraph, String resource, JSONObject config) {

		config = Config.getConfig(config, FIELD_DESCRIPTIONS);

		Node resourceNode = apiGraph.getNode(resource);
		
		JSONObject tableConfig = config.optJSONObject(RESOURCE_TABLE);
			
		UserGuideData.FieldsData res = getFieldsForResource(apiGraph, tableConfig, resourceNode); 

		Predicate<Node> includeNode = n -> !n.isSimpleType() && !n.isEnumNode();

		List<Node> subGraph = apiGraph.getSubGraph(resourceNode).stream()
									.sorted()
									.filter(includeNode)
									.collect(toList());
				
		res.subResources = new LinkedList<>();

		for(Node subResource : subGraph) {
			UserGuideData.FieldsData data = getFieldsForResource(apiGraph, tableConfig, subResource); 
			res.subResources.add(data);
		}
		
		return res;
		
	}
	
	
	private FieldsData getFieldsForResource(APIGraph apiGraph, JSONObject config, Node resource) {
		UserGuideData.FieldsData res = userGuideData.new FieldsData();
		
		if(!resource.getDescription().isEmpty()) {
			String description = resource.getDescription();
			if(!description.endsWith(".")) description = description + ".";
			res.description = description;
		}
					
		res.resource = resource.getName();
		
		res.fields = getResourceDetailsTable(apiGraph, config, resource);
		res.hasFields = !res.fields.isEmpty();
		
		return res;

	}


	@LogMethod(level=LogLevel.DEBUG)
	private List<UserGuideData.FieldData>  getResourceDetailsTable(APIGraph apiGraph, JSONObject config, Node resource) {		
		List<UserGuideData.FieldData> res = new LinkedList<>();

		List<Property> properties = resource.getProperties();
				
		List<String> metaProperties = Config.get(META_PROPERTIES);
		
		boolean includeAllProperties = Utils.difference(properties.stream().map(Property::getName).collect(toList()), metaProperties).isEmpty();
		
		Predicate<Property> isEligibleProperty = p -> includeAllProperties || !metaProperties.contains(p.getName());
		
		properties.stream()
			.filter(isEligibleProperty)
			.sorted(Comparator.comparing(Property::getName))
			.collect(Collectors.partitioningBy(property -> property.getName().startsWith("@")))
			.values()
			.stream()
			.flatMap(List::stream)
			.forEach(property -> {
								
//				String[] description = { generator.constructDescriptionForType(property.getType()), 
//									     property.getDescription(), 
//									     generator.getDescriptionForType(apiGraph, property.getType()) };
				
				String[] description = { generator.constructDescriptionForType(property.getType()),
										 property.getDescription()
					                   };
				
				UserGuideData.FieldData data = userGuideData.new FieldData();
				data.name = property.getName();
				data.description = generator.constructStatement(description);
				
				res.add(data);
				
			});
			
		return res;
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	private String getJSONRepresentations(APIGraph apiGraph, String resource, JSONObject config) {
		config = Config.getConfig(config, JSON_REPRESENTATIONS);

		JSONObject tableConfig = config.optJSONObject(JSON_TABLE);
									
		return generator.getJSON(resource, tableConfig);
		
	}


	public UserGuideData generatePartials(UserGuideData userguide) {

		List<String> resources = generator.getResources();
				
		for(String resource : resources) {		
						
			UserGuideData.ResourceData resourceConfig = userguide.resources.get(resource);
		
			resourceConfig.fileSource    = resource + ".adoc";
			resourceConfig.diagramSource = resource + "_diagram.adoc";

			copyImages(resourceConfig);
			copySample(resource, resourceConfig);

			createResourceDiagramFragment(resourceConfig);						
			createResourceFragment(resourceConfig);
			
		}
				
		
				
		return userguide;
	}


	private void createResourceFragment(ResourceData resourceConfig) {		
		String generatedTarget = Config.getString("userguide.generatedTarget");		
		if(generatedTarget.isEmpty()) generatedTarget = "generated/";
		
		String targetDirectory = generator.getGeneratedTargetDirectory(generatedTarget);
		String outputfile = targetDirectory + resourceConfig.fileSource;
		
		generator.processTemplate("userguide.resource.mustache", resourceConfig, outputfile);
	}


	private void copySample(String resource, UserGuideData.ResourceData resourceConfig) {
		resourceConfig.sampleSource =  "Sample_" + resource + ".json";
		String targetDirectory = generator.getGeneratedTargetDirectory("samples/");
		
		LOG.debug("copySample: target={}",  targetDirectory);
		
		Utils.save(resourceConfig.sample, targetDirectory + resourceConfig.sampleSource);
	}


	private void createResourceDiagramFragment(UserGuideData.ResourceData resourceConfig) {		
		
		String generatedTarget = Config.getString("userguide.generatedTarget");
		
		if(generatedTarget.isEmpty()) generatedTarget = "generated/";
		
		String targetDirectory = generator.getGeneratedTargetDirectory(generatedTarget);
		
		String outputfile = targetDirectory + resourceConfig.diagramSource;
		
		generator.processTemplate("userguide.resource_diagram.mustache", resourceConfig, outputfile);
		
	}


	private void copyImages(UserGuideData.ResourceData resourceConfig) {
		for(UserGuideData.DiagramData diagram : resourceConfig.diagrams) {
			
			String coreFilename = diagram.imgfile.replaceAll(".[a-z]*$", "");	
			
			List<String> imageTypes = Arrays.asList("png", "svg", "puml");
			
			for(String type : imageTypes) {
				String diagramName = coreFilename + "." + type;
								
				if( Utils.copyFile(diagram.sourceLocation + "/" + diagramName, "images/" + diagramName, args.generatedTargetDirectory, args.workingDirectory, Utils.OVERWRITE) ) {
					UserGuideData.FileData details = userGuideData.new FileData();
					details.filename = diagramName;
					
					switch(type) {
					case "svg":
						diagram.svg_source = details;				
						break;
						
					case "png":
						diagram.png_source = details;				
						break;
						
					case "puml":
						diagram.puml_source = details;				
						break;
						
					default:
						break;
					}
				}
			}
					
		}
		
	}


}
