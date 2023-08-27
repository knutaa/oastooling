package no.paneon.api.conformance;

import org.json.JSONObject;

import no.paneon.api.generator.GenerateCommon;
import no.paneon.api.logging.AspectLogger.LogLevel;
import no.paneon.api.logging.LogMethod;
import no.paneon.api.model.APIModel;
import no.paneon.api.utils.Config;
import no.paneon.api.utils.Out;
import no.paneon.api.utils.Utils;

import no.paneon.api.tooling.Args;

import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class GenerateConformance extends GenerateCommon {

    static final Logger LOG = LogManager.getLogger(GenerateConformance.class);

	Args.Conformance args;
	
	ConformanceModel model;
	
	public GenerateConformance(Args.Conformance args) {
		super(args);
		this.args = args;
		this.model  = new ConformanceModel();

	}
	
	@LogMethod(level=LogLevel.DEBUG)
	public void init() {
		
		super.execute();
		
		model.init();
		
		if(args.defaults!=null) {
			model.setDefaults(args.defaults);
		}
		
		model.extractFromSwagger();
		
		model.extractFromRules();

		model.expandDefaults();
		
	}
		
	@Override
	@LogMethod(level=LogLevel.DEBUG)
	public void execute() {
		
		init();
				
		boolean completeGeneration=true;
		
		try {	
			JSONObject conformance = null;
			if(args.mandatoryOnly) {
				conformance = model.mandatoryOnly(model.generateConformance(completeGeneration));
				Out.println("... removed optional resource items (mandatory only)");
			} else {
							
				conformance = model.generateConformance(completeGeneration);
												
				if(!args.complete) {
					conformance = model.removeOptional(conformance);
					Out.println("... removed optional resource items ...");
				}
				
			}
			if(LOG.isTraceEnabled()) LOG.log(Level.TRACE, "conformance: {0}", conformance.toString(2));
				
			if(Config.getBoolean("allAPIResourcesMandatory")) {
				List<String> resources=APIModel.getResources();
				resources.remove("Hub");
				JSONObject conf = conformance.optJSONObject("conformance");
				if(conf!=null) {
					for(String resource : resources) {
						JSONObject resourceConf = conf.optJSONObject(resource);
						if(resourceConf!=null) resourceConf.put("condition", "M");
					}
				}
				
			}
			
			saveConformance(conformance);
	        
		} catch(Exception e) {			
			Out.println("error: ", e.getLocalizedMessage());
		} 
	}

	@LogMethod(level=LogLevel.DEBUG)
	public void saveConformance(JSONObject conformance) {

		String targetDirectory = !args.targetDirectory.isEmpty()  ? args.targetDirectory    :
								 !args.workingDirectory.isEmpty() ? args.workingDirectory   : 
							     ".";
		
		String destination = Utils.getFilenameWithDirectory(targetDirectory, args.outputFileName);			

		LOG.debug("destination; {}", destination);
		
		try {
			if(destination==null) {
				Out.println("... missing output file argument ...");
				System.exit(1);
			} else {

				if(destination.endsWith(".yaml") || destination.endsWith(".yml")) {
					Utils.saveAsYaml(conformance, destination);

				} else if(destination.endsWith(".json")){
					Utils.saveAsJson(conformance, destination);

				} else {
					Out.println("... expected either JSON or YAML output file ...");
					System.exit(1);
				}

				Out.println("... output to " + destination);

			}

		} catch(Exception e) {			
			Out.println("error: " + e.getLocalizedMessage());
		} 		
	}

}
