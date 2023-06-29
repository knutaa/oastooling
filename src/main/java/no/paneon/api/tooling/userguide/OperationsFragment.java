package no.paneon.api.tooling.userguide;

import org.json.JSONArray;
import org.json.JSONObject;

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
import no.paneon.api.tooling.userguide.UserGuideData.OperationSampleData;
import no.paneon.api.tooling.userguide.UserGuideData.PropertyRuleData;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

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
			
		Timestamp.timeStamp("finished operations fragment");

	}

	
	private void addOperationConfig(List<String> sampleFiles) {
		
		LOG.debug("addOperationConfig: samplesFiles={}",  sampleFiles);
		
		JSONObject combinedConfig = new JSONObject();
		sampleFiles.forEach( file -> {			
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
			Out.printAlways("... *** unable to locate operation sample directory: " + dir);
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
			
			try {
				res = Utils.readFile(file);

				JSONObjectOrArray json = JSONObjectOrArray.readJSONObjectOrArray(file);

				Out.println("readOperationDetailsByPattern: file={}", file);

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
						
						List<String[]> propertyRules = generator.conformance.getPatchable(opDetail,resource);

						LOG.debug("getOperationsDetailsForResource: PATCH resource={} propertyRules={}", resource, propertyRules);
						
						if(propertyRules!=null) {
							data.patchable = propertyRules.stream().map(this::createPropertyRuleData).collect(toList());
						}
						data.hasPatchable = !data.patchable.isEmpty();
						
						propertyRules = generator.conformance.getNonPatchable(resource);

						if(propertyRules!=null) {
							data.nonPatchable = propertyRules.stream().map(this::createPropertyRuleData).collect(toList());
						}
						data.hasNonPatchable = !data.nonPatchable.isEmpty();

						LOG.debug("getOperationsDetailsForResource: PATCH resource={} propertyRules={}", resource, propertyRules);

						break;
						
					case OP_CREATE:
						data.isCreate = true;

						propertyRules = generator.conformance.getMandatoryInPost(opDetail,resource);

						LOG.debug("getOperationsDetailsForResource: CREATE resource={} propertyRules={}", resource, propertyRules);

						if(propertyRules!=null) {
							data.mandatory = propertyRules.stream().map(this::createPropertyRuleData).collect(toList());
						}
						data.hasMandatory = !data.mandatory.isEmpty();

						
					default:

					}
										
					List<UserGuideData.OperationSampleData> sampleResults = new LinkedList<>();
					
					int origSequenceNumber = sequenceNumber;
											
					if(origSequenceNumber==sequenceNumber) {
						sequenceNumber = processOperationSamplesFromRules(sampleResults, sequenceNumber, resource, op, operation, path, opDetail);	
					}
						
					if(origSequenceNumber==sequenceNumber) {
						sequenceNumber = processOperationSamplesFromAPI(sampleResults, sequenceNumber, resource, op, operation, path, opDetail);	
					}
				
					if(origSequenceNumber==sequenceNumber) {
						sequenceNumber = processOperationSamplesFromSamples(operationsFragmentsConfig, opConfig, sampleResults, sequenceNumber, resource, op, operation, path, opDetail);	
					}

					if(origSequenceNumber==sequenceNumber) {
						if(!data.isDelete) Out.printAlways("... *** samples not found for {} {} ({})", op.toUpperCase(), path, operation );
					}
					
					data.samples = sampleResults;
					data.hasSamples = !sampleResults.isEmpty();
					
				}
				
			}

		}
				
		return res;
		
	}
		
	private int processOperationSamplesFromRules(List<OperationSampleData> sampleResults, int sequenceNumber,
			String resource, String op, String operation, String path, JSONObject opDetail) {

		JSONObject rulesFragmentForRequest = Config.getRulesForResource(resource); 
		
		if(rulesFragmentForRequest==null) return sequenceNumber;
		
		LOG.debug("rulesFragmentForRequest={}", rulesFragmentForRequest.toString());

		JSONObject rulesForOperation = Config.getRulesForOperation(rulesFragmentForRequest, op); 
			
		if(rulesForOperation==null) return sequenceNumber;
		LOG.debug("rulesForOperation={}", rulesForOperation.toString());
			
		JSONArray examples = rulesForOperation.optJSONArray("examples");
		
		if(examples!=null) {
			for(int idx=0; idx<examples.length(); idx++ ) {	
				Object o = examples.get(idx);
				if(o instanceof JSONObject) {
					JSONObject example = (JSONObject) o;
					sequenceNumber = generateOperationSample(sampleResults, sequenceNumber, resource, op, operation, path, example);
				}
			}		
		} else {
			JSONObject example = rulesForOperation.optJSONObject("example");
			if(example!=null) {
				sequenceNumber = generateOperationSample(sampleResults, sequenceNumber, resource, op, operation, path, example);
			}
		}				
	
		return sequenceNumber;
		
	}
	

	private int generateOperationSample(List<OperationSampleData> sampleResults, int sequenceNumber, String resource,
			String op, String operation, String path, JSONObject sample) {
		
		LOG.debug("generateOperationSample resource={} op={} operation() =path={} example={}", resource, op, operation, path, sample);

		if(operation.contentEquals("list") && !sample.optBoolean("isCollection")) return sequenceNumber;
		if(operation.contentEquals("retrieve") && sample.optBoolean("isCollection")) return sequenceNumber;

		UserGuideData.OperationSampleData sampleDetails = userGuideData.new OperationSampleData();
																	
		sampleDetails.description = sample.optString("description");
																													
		sampleDetails.request  = generateRequestPayloadFromConfig(op, path, sample);				
		sampleDetails.response = generateResponseBodyFromConfig(op, path, sample);
								
		sequenceNumber++;
		
		sampleDetails.requestSource = saveSampleFile(sampleDetails.request , "Resource_" + resource + "_request_sample_" + sequenceNumber + ".json");					
		sampleDetails.responseSource = saveSampleFile(sampleDetails.response , "Resource_" + resource + "_response_sample_" + sequenceNumber + ".json");
				
		sampleResults.add(sampleDetails);
		
		return sequenceNumber;

	}

	private String generateRequestPayloadFromConfig(String op, String path, JSONObject config) {
		StringBuilder res = new StringBuilder();
		
		res.append(op.toUpperCase() + " " );
		
		String endPoint = updatePathParams(path, config);	
				
		res.append(endPoint);
		
		String queryParams = generateQueryParam(config);
		
		if(!queryParams.isBlank()) {
			res.append("?");
			res.append(queryParams);
		}
		
		res.append(NEWLINE);
		
		String contentType = config.optString("content-type");
		contentType = !contentType.isEmpty() ? contentType : "application/json";
		
		res.append("Content-Type: " + contentType );
		
		LOG.debug("example: {}", config);
		
		String requestPayload = Samples.readPayload(this.args.workingDirectory, config, "request");
		if(!requestPayload.isEmpty()) {
			res.append(NEWLINE);
			res.append(requestPayload);			
		}
		
		return res.toString();
	}


	private String generateQueryParam(JSONObject example) {
		StringBuilder res = new StringBuilder();
		JSONArray queryParams = example.optJSONArray("queryParameters");
		
		if(queryParams==null) return res.toString();

		for(int i=0; i<queryParams.length(); i++) {
			Object param = queryParams.get(i);
			if(param instanceof JSONObject) {
				JSONObject json = (JSONObject) param;
				if(i>0) res.append("&");
				if("fields".contentEquals(json.optString("name"))) {
					res.append( json.optString("name") + "=" + json.optString("value") );
				} else if("filtering".contentEquals(json.optString("name"))) {
					res.append( json.optString("value") );
				} else {
					res.append( json.optString("name") + "=" + json.optString("value") );
				}
 			}
		}
		return res.toString();
	}

	private String updatePathParams(String path, JSONObject example) {
		JSONArray pathParams = example.optJSONArray("pathParameters");
		if(pathParams==null) return path;
		
		for(int i=0; i<pathParams.length(); i++) {
			Object param = pathParams.get(i);
			if(param instanceof JSONObject) {
				JSONObject json = (JSONObject) param;
				String oldText = "{" + json.optString("name") + "}";
				String newText = json.optString("value");
				if(pathParams.length()>1) {
					path = path.replace(oldText, newText);
				} else {
					path = path.replaceAll("\\{[^\\}]+\\}", newText);
					LOG.debug("path={} newText={}", path, newText);
				}
 			}
		}
		return path;
	}

	private int processOperationSamplesFromSamples(JSONObject operationsFragmentsConfig, JSONObject opConfig, 
			List<OperationSampleData> sampleResults, int sequenceNumber,
			String resource, String op, String operation, String path, JSONObject opDetail) {
		
		JSONObject operationConfig = Config.getConfig(opConfig, operation);
		JSONArray samples = operationConfig.optJSONArray("samples");

		LOG.debug("operationConfig={}",  operationConfig);

		if(samples==null) return sequenceNumber;
		
		LOG.debug("processOperationSamplesFromSamples: resource={} op={} operation() path={} samples={}",  resource, op, operation, path, samples);

		for(int idx=0; idx<samples.length(); idx++ ) {	
			UserGuideData.OperationSampleData sampleDetails = userGuideData.new OperationSampleData();

			JSONObject sampleConfig = samples.optJSONObject(idx);
					
			if(sampleConfig==null) continue;
					
			LOG.debug("sampleConfig={}",  sampleConfig.toString(2));

			sampleDetails.description = sampleConfig.optString("description");
																														
			sampleDetails.request  = generateRequestPayload(op, path, operationsFragmentsConfig, sampleConfig);				
			sampleDetails.response = generateResponseBody(op, path, sampleConfig);
									
			sequenceNumber++;
			
			sampleDetails.requestSource = saveSampleFile(sampleDetails.request , "Resource_" + resource + "_request_sample_" + sequenceNumber + ".json");					
			sampleDetails.responseSource = saveSampleFile(sampleDetails.response , "Resource_" + resource + "_response_sample_" + sequenceNumber + ".json");
					
			sampleResults.add(sampleDetails);
			
		}
		
		return sequenceNumber;
	}


	private int processOperationSamplesFromAPI(List<UserGuideData.OperationSampleData> sampleResults, int sequenceNumber, 
												String resource, String op, String operation, String path, JSONObject opDetail) {
				
		JSONObject requestBody = APIModel.getOperationRequestsByResource(opDetail);
		Map<String,JSONObject> responses = APIModel.getOperationResponsesByResource(opDetail);
			
		Map<String,JSONObject> requestExamples = APIModel.getOperationExamples(requestBody);

		Predicate<String> isValid = s -> s.startsWith("2");
		
		Set<String> validOpCodes = responses.keySet().stream().filter(isValid).collect(toSet());
				
		LOG.debug("resource={} validOpCodes={}", resource, validOpCodes);
		
		for(String opCode : validOpCodes) {
			JSONObject response = responses.get(opCode);
			
			Map<String,JSONObject> responseExamples = APIModel.getOperationExamples(response);

			LOG.debug("resource={} operation={} responses={} examples={}",  resource, operation, responses.keySet(), responseExamples.keySet());

			if(!responseExamples.isEmpty() || !requestExamples.isEmpty()) {
				
				LOG.debug("resource={} operation={} requestExamples={}",  resource, operation, requestExamples.keySet());
				LOG.debug("resource={} operation={} responseExamples={}",  resource, operation, responseExamples.keySet());

				LOG.debug("resource={} operation={} responses={} examples={}",  resource, operation, responses.keySet(), responseExamples.keySet());
				LOG.debug("examples={}",  responseExamples);

				UserGuideData.OperationSampleData sampleDetails = userGuideData.new OperationSampleData();
																																
				sampleDetails.description = "TBD - not retrievable from API";
							
				String filtering = "";
				String contentType = "";
				
				JSONObject payload = !requestExamples.isEmpty() ? requestExamples.values().iterator().next() : new JSONObject();
				sampleDetails.request = generateRequestPayload(op, path, payload, filtering, contentType );
				
				if(!responseExamples.isEmpty()) {
					payload = responseExamples.values().iterator().next();
					
					LOG.debug("### payload={}",  payload);
					
					sampleDetails.response = generateResponseBody(op, path, payload );
				}

				sequenceNumber++;
				
				sampleDetails.requestSource = saveSampleFile(sampleDetails.request , "Resource_" + resource + "_request_sample_" + sequenceNumber + ".json");					
				sampleDetails.responseSource = saveSampleFile(sampleDetails.response , "Resource_" + resource + "_response_sample_" + sequenceNumber + ".json");
						
				sampleResults.add(sampleDetails);
	
			}
				
		}
		
		return sequenceNumber;
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

	private String generateResponseBodyFromConfig(String op, String path, JSONObject config, JSONObject sampleConfig) {
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
	
	private String generateResponseBodyFromConfig(String op, String path, JSONObject config) {
		StringBuilder res = new StringBuilder();

		String responseCode = APIModel.getSuccessResponseCode(path,op);

		if(!responseCode.isEmpty()) {
			res.append(responseCode);
			res.append(NEWLINE);
			String payload = readPayload(op, path, config, config, "response");
			if(payload!=null) res.append(payload);
		}
		
		return res.toString();
	}
	
	private String generateResponseBody(String op, String path, JSONObject config) {
		StringBuilder res = new StringBuilder();

		String responseCode = APIModel.getSuccessResponseCode(path,op);

		if(!responseCode.isEmpty()) {
			res.append(responseCode);
			res.append(NEWLINE);
			String payload = readPayload(op, path, config, config, "response");
			if(payload!=null) res.append(payload);
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
	
	private String generateRequestPayload(String op, String path, JSONObject payload, String filtering, String contentType) {
		StringBuilder res = new StringBuilder();
		
		res.append(op.toUpperCase() + " " );
		
		String endPoint = path;		
		if(endPoint.endsWith("}")) {
			endPoint = endPoint.replaceAll("\\{[^\\}]+\\}", payload.optString("id"));
		}
		
		res.append(endPoint);
		
		if(!filtering.isEmpty()) {
			res.append("?" + filtering);
		}
		
		res.append(NEWLINE);
		
		contentType = !contentType.isEmpty() ? contentType : "application/json";
		
		res.append("Content-Type: " + contentType );
		
		String requestPayload = payload.toString(2);
		if(!requestPayload.isEmpty()) {
			res.append(NEWLINE);
			res.append(requestPayload);			
		}
		
		return res.toString();
	}

	private String readPayload(String op, String path, JSONObject config, JSONObject sampleConfig, String requestResponse) {
		StringBuilder res = new StringBuilder();
				
		if(sampleConfig==null) {
			Out.printOnce("... missing payload examples");
			return res.toString();
		}
		
		if(sampleConfig.optJSONObject(requestResponse)!=null) {
			String content = Samples.readPayload(args.workingDirectory, sampleConfig, requestResponse);
			res.append(content);
			
		} else {
			String source = sampleConfig.optString(requestResponse);
			
			LOG.debug("readPayload: op={} path={} source={} ", op, path, source);
	
			if(!source.isEmpty()) {
				source = source.replaceAll("\\$_", "");
				
				String request = readOperationDetailsByPattern(config, source);
				
				res.append(request);
				
			}
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
	
//	private String generateURL(String path, JSONObject opDetail) {
//		StringBuilder res = new StringBuilder();
//		res.append(path);
//		if(path.endsWith("}")) res.append(FIELD_FILTER);
//		return res.toString();
//	}

	private String generateURL(String path, JSONObject opDetail) {
		StringBuilder res = new StringBuilder();
		res.append(path);
		// if(path.endsWith("}")) res.append(FIELD_FILTER);
		
		List<String> paramLabels = new LinkedList<>();
		JSONArray params = opDetail.optJSONArray("parameters");
		if(params!=null) {
			params.forEach(item -> {
				if(item instanceof JSONObject) {
					JSONObject o = (JSONObject)item;
					if(o.has("$ref")) {
						String ref = o.getString("$ref");
						String parts[] = ref.split("/");
						String label = parts[parts.length-1];
						paramLabels.add(label);
					}
				}
			});
		}
		
		boolean filtering=false;
		JSONObject responses = opDetail.optJSONObject("responses");
		if(responses!=null) {
			if(responses.has("200") || responses.has("202")) filtering=true; 
		}
		
		if(params!=null) {
			params.forEach(item -> {
				if(item instanceof JSONObject) {
					JSONObject o = (JSONObject)item;
					if(o.has("$ref")) {
						String ref = o.getString("$ref");
						String parts[] = ref.split("/");
						String label = parts[parts.length-1];
						paramLabels.add(label);
					}
				}
			});
		}
		
		LOG.debug("generateURL: path={} paramLabels={} filtering={}", path, paramLabels, filtering);

		if(paramLabels.contains("Fields")) {
			res.append("?fields=...");
			if(filtering) res.append("&");
		} else if(filtering) {
			res.append("?");
		}
		if(filtering) res.append("{filtering}");
		
		return res.toString();
	}

	
}
