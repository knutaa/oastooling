package no.paneon.api.tooling.userguide;

import org.json.JSONArray;
import org.json.JSONObject;

import no.paneon.api.conformance.ConformanceItem;
import no.paneon.api.conformance.ConformanceModel;
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

import no.paneon.api.tooling.Args;
import no.paneon.api.tooling.userguide.UserGuideData.FileData;
import no.paneon.api.tooling.userguide.UserGuideData.PropertyRuleData;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import static java.util.stream.Collectors.toList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OperationsFragment {

	static final Logger LOG = LogManager.getLogger(OperationsFragment.class);

	private static final String OPERATION_SAMPLES_FOLDER = "operation-samples-folder";
	
	private static final String OPERATION_CONFIG_PATTERN = "operation-samples-config-pattern";

	private static final String OPERATION_CONFIG = "operationConfig";

	private static final String NEWLINE = "\n";
	
	private static final String OP_INTRO_TEXT = "operationsIntroText";

	private static final String OP_LIST = "list";
	
	private static final String OP_RETRIEVE = "retrieve";
	
	private static final String OP_CREATE = "create";
	
	private static final String OP_REPLACE = "replace";
	
	private static final String OP_PARTIAL_UPDATE = "partialupdate";
	
	private static final String OP_DELETE = "delete";
	

	Args.UserGuide args;
	
	UserGuideGenerator generator;
	UserGuideData userGuideData;
	
	public OperationsFragment(UserGuideGenerator generator) {	
		this.generator = generator;
		this.args = generator.args;		
		this.userGuideData = generator.userGuideData;
	}

	@LogMethod(level=LogLevel.DEBUG)
	public void process() {

		JSONObject config = Config.getConfig("userguide::operationsFragments");
						
		addOperationConfig( getOperationConfig(config) );
										
		List<String> resources = generator.getResources();
		
		for(String resource : resources) {
			userGuideData.resources.get(resource).operations = getOperationsDetailsForResource(config, resource);
		}
						
	}

	
	private void addOperationConfig(List<String> sampleFiles) {
		
		LOG.debug("addOperationConfig: samplesFiles={}",  sampleFiles);
		
		JSONObject combinedConfig = new JSONObject();
		sampleFiles.stream().forEach( file -> {			
			JSONObject json = Utils.readJSONOrYaml(file);
			if(json!=null) {
				json.keySet().stream().forEach(key -> combinedConfig.put(key, json.get(key)));
			}			
		});	
		JSONObject operationConfig = new JSONObject();
		operationConfig.put(OPERATION_CONFIG, combinedConfig);
		Config.addConfiguration(operationConfig);
	}

	private List<String> getOperationConfig(JSONObject config) {
		List<String> res = new LinkedList<>();
		
		String dir = Utils.getFileName(args.workingDirectory, config, OPERATION_SAMPLES_FOLDER);
		File sampleDir = new File(dir);
		
		LOG.debug("getOperationConfig:: dir={} sampleDir={}",  dir, sampleDir);
		
		List<String> pattern = Config.getList(config, OPERATION_CONFIG_PATTERN);
		
		if(sampleDir.isDirectory()) {
			for(String file : new File(dir).list()) {
				if( pattern.stream().anyMatch(file::endsWith)) res.add(dir + "/" + file);
			}
		} else {
			Out.printAlways("... unable to locate operation sample directory: " + dir);
		}
			
		return res;
	}

	private String readOperationDetailsByPattern(JSONObject config, String pattern) {	
		String res="";
		String dir = Utils.getFileName(args.workingDirectory, config, OPERATION_SAMPLES_FOLDER);
					
		List<String> matches = Arrays.asList(new File(dir).list()).stream()
									.filter(file -> file.toUpperCase().startsWith(pattern.toUpperCase()))
									.map(file -> dir + "/" + file)
									.collect(toList());
				
		if(matches.size()!=1) {
			Out.println("... unable to locate unique file with pattern " + pattern);
		} else {
			String file = matches.get(0);
			res = Utils.readFile(file);
			
			try {
				JSONObjectOrArray json = JSONObjectOrArray.readJSONObjectOrArray(file);
			} catch(Exception ex) {
				Out.printAlways("... error in JSON file: " + new File(matches.get(0)).getName());
				Out.printAlways("... error message: " + ex.getLocalizedMessage());
			}
			
		}
		
		return res;
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	private List<UserGuideData.OperationData> getOperationsDetailsForResource(JSONObject operationsFragmentsConfig, String resource) {
		List<UserGuideData.OperationData> res = new LinkedList<>();
		
		JSONObject opConfig = Config.getConfig(OPERATION_CONFIG);
		
		opConfig = Config.getConfig(opConfig, resource);
				
		LOG.debug("getOperationsDetailsForResource: opConfig={}",  opConfig.toString(2));
		
		List<String> allOps = new LinkedList<>( Arrays.asList( "GET", "POST", "PATCH", "PUT", "DELETE" ) );

		allOps.retainAll( APIModel.getOperationsByResource(resource) );
		
		JSONObject config = Config.getConfig(operationsFragmentsConfig, "operationsDetails");

		List<String> allPaths = APIModel.getPaths(resource);
				
		int sequenceNumber = 0;
		
		for(String op : allOps) {

			for(String path : allPaths) {
				JSONObject opDetail = APIModel.getOperationsDetailsByPath(path, op);
							
				if(opDetail!=null) {

					UserGuideData.OperationData data = userGuideData.new OperationData();
					res.add(data);
					
					String summary = opDetail.optString("summary");
					String opURL =  op.toUpperCase() + " " + generateURL(path,opDetail);

					data.operation      = summary;
					data.operationLabel = summary;
					data.uRL = opURL;
					
					String operation = getSampleKey(op,path,opDetail);
					
					if(config.optJSONObject(OP_INTRO_TEXT)!=null && config.optJSONObject(OP_INTRO_TEXT).optJSONArray(operation)!=null) {
						JSONArray operationIntroText = config.optJSONObject(OP_INTRO_TEXT).optJSONArray(operation);
									
						String text = getTextFragment(operationIntroText, resource);
						
						data.description = text;
						
					}
																
					switch(operation) {
					case OP_LIST:
						data.isList = true;
						break;
						
					case OP_RETRIEVE:
						data.isRetrieve = true;
						break;
						
					case OP_REPLACE:
						data.isReplace = true;
						break;
						
					case OP_DELETE:
						data.isDelete = true;
						break;
						
					case OP_PARTIAL_UPDATE: 
						data.isPartialUpdate = true;
						
						List<String[]> propertyRules = generator.conformance.getPatchable(resource);

						if(propertyRules!=null) {
							data.patchable = propertyRules.stream().map(this::createPropertyRuleData).collect(toList());
						}
						data.hasPatchable = !data.patchable.isEmpty();
						
						propertyRules = generator.conformance.getNonPatchable(resource);

						if(propertyRules!=null) {
							data.nonPatchable = propertyRules.stream().map(this::createPropertyRuleData).collect(toList());
						}
						data.hasNonPatchable = !data.nonPatchable.isEmpty();

						break;
						
					case OP_CREATE:
						data.isCreate = true;

						propertyRules = generator.conformance.getMandatoryInPost(resource);

						if(propertyRules!=null) {
							data.mandatory = propertyRules.stream().map(this::createPropertyRuleData).collect(toList());
						}
						data.hasMandatory = !data.mandatory.isEmpty();

						
					default:

					}
										
					JSONObject operationConfig = Config.getConfig(opConfig, operation);
					JSONArray samples = operationConfig.optJSONArray("samples");

					LOG.debug("operationConfig={}",  operationConfig);

					List<UserGuideData.OperationSampleData> sampleResults = new LinkedList<>();
					
					if(samples==null) {
						Out.printAlways("... samples not found for resource '" + resource + "' and operation '" + operation + "'");
					} else {
						for(int idx=0; idx<samples.length(); idx++ ) {	
							UserGuideData.OperationSampleData sampleDetails = userGuideData.new OperationSampleData();
	
							JSONObject sampleConfig = samples.optJSONObject(idx);
									
							LOG.debug("sampleConfig={}",  sampleConfig);
	
							if(sampleConfig==null) continue;
																		
							sampleDetails.description = sampleConfig.optString("description");
																																		
							sampleDetails.request = generateRequestPayload(op, path, operationsFragmentsConfig, sampleConfig);				
							sampleDetails.response = generateResponseBpdy(op, path, operationsFragmentsConfig, sampleConfig);
													
							sequenceNumber++;
							
							sampleDetails.requestSource = saveSampleFile(sampleDetails.request , "Resource_" + resource + "_request_sample_" + sequenceNumber + ".json");					
							sampleDetails.responseSource = saveSampleFile(sampleDetails.response , "Resource_" + resource + "_response_sample_" + sequenceNumber + ".json");
									
							sampleResults.add(sampleDetails);
							
						}
					}
					
					data.samples = sampleResults;
					data.hasSamples = !sampleResults.isEmpty();
				}
				
			}

		}
				
		return res;
	}

	private FileData saveSampleFile(String content, String filename) {
		
		String destDir = generator.getGeneratedTargetDirectory("samples/");

		FileData res = userGuideData.new FileData();
		res.filename = filename;
		Utils.save(content, destDir + filename);
				
		return res;
	}

	private PropertyRuleData createPropertyRuleData(String[] props) {				
		return this.userGuideData.new PropertyRuleData(props);
	}
	
	private String getTextFragment(JSONArray text, String resource) {
		String res = text.toList().stream().map(Object::toString).collect(Collectors.joining(NEWLINE));
		
		List<String> resourceInWords = Arrays.asList(resource.split("(?=[A-Z])") );
		
		Map<String,String> params = new HashMap<>();
		params.put("RESOURCE_LOWER", resourceInWords.stream().map(Utils::lowerCaseFirst).collect(Collectors.joining(" ")));
		params.put("RESOURCE_ARTICLE", generator.getArticle(resource));
		
		res = Utils.replaceVariables(res, params);
				
		return res;
	}

	private String generateResponseBpdy(String op, String path, JSONObject config, JSONObject sampleConfig) {
		StringBuilder res = new StringBuilder();

		String responseCode = APIModel.getSuccessResponseCode(path,op);

		if(!responseCode.isEmpty()) {
			res.append(responseCode);
			res.append(NEWLINE);
			String payload = readPayload(op, path, config, sampleConfig, "response");
			res.append(payload);
		}
		
		return res.toString();
	}

	private String generateRequestPayload(String op, String path, JSONObject config, JSONObject sampleConfig) {
		StringBuilder res = new StringBuilder();
		
		res.append(op.toUpperCase() + " " );
		
		String endPoint = path;		
		if(endPoint.endsWith("}")) {
			endPoint = endPoint.replaceAll("\\{[^\\}]+\\}", sampleConfig.optString("objectId"));
		}
		
		res.append(endPoint);
		
		String filtering = sampleConfig.optString("filtering");
		if(!filtering.isEmpty()) {
			res.append("?" + filtering);
		}
		
		res.append(NEWLINE);
		
		String contentType = sampleConfig.optString("content-type");
		contentType = !contentType.isEmpty() ? contentType : "application/json";
		
		res.append("Content-Type: " + contentType );
		
		String requestPayload = readPayload(op, path, config, sampleConfig, "request");
		if(!requestPayload.isEmpty()) {
			res.append(NEWLINE);
			res.append(requestPayload);			
		}
		
		return res.toString();
	}

	private String readPayload(String op, String path, JSONObject config, JSONObject sampleConfig, String requestResponse) {
		StringBuilder res = new StringBuilder();
				
		String source = sampleConfig.optString(requestResponse);
		if(!source.isEmpty()) {
			source = source.replaceAll("\\$_", "");
			
			String request = readOperationDetailsByPattern(config, source);
			
			res.append(NEWLINE);
			res.append(request);
			
		}
		
		return res.toString();
	}
	
	private String getSampleKey(String op, String path, JSONObject opDetail) {
		String res="";
		switch(op.toUpperCase()) {
		case "GET": 
			res = path.endsWith("}") ? "retrieve" : OP_LIST; 
			break;
			
		case "POST": 
			res = OP_CREATE;
			break;

		case "PUT": 
			res = OP_REPLACE;
			break;

		case "PATCH": 
			res = OP_PARTIAL_UPDATE;
			break;

		case "DELETE": 
			res = OP_DELETE;
			break;

		default:
		}
		
		return res;
	}

	final String FIELD_FILTER = "?fields=...&{filtering}";
	
	private String generateURL(String path, JSONObject opDetail) {
		StringBuilder res = new StringBuilder();
		res.append(path);
		if(path.endsWith("}")) res.append(FIELD_FILTER);
		return res.toString();
	}



	
}