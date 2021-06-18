package no.paneon.api.conformance;

import org.json.JSONObject;

import no.paneon.api.generator.GenerateCommon;
import no.paneon.api.logging.AspectLogger.LogLevel;
import no.paneon.api.logging.LogMethod;
import no.paneon.api.utils.Out;
import no.paneon.api.utils.Utils;

import no.paneon.api.tooling.Args;

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
	
	@Override
	@LogMethod(level=LogLevel.DEBUG)
	public void execute() {
		
		super.execute();
		
		model.init();
		
		model.extractFromSwagger();
		
		model.extractFromRules();
		
		model.expandDefaults();
				

		try {	
			JSONObject conformance = null;
			if(args.mandatoryOnly) {
				conformance = model.mandatoryOnly(model.generateConformance());
				Out.println("... removed optional resource items (mandatory only)");
			} else {
							
				conformance = model.generateConformance();
												
				if(!args.complete) {
					conformance = model.removeOptional(conformance);
					Out.println("... removed optional resource items ...");
				}
				
			}
			if(LOG.isTraceEnabled()) LOG.log(Level.TRACE, "conformance: {0}", conformance.toString(2));
				
			saveConformance(conformance);
	        
		} catch(Exception e) {			
			Out.println("error: ", e.getLocalizedMessage());
		} 
	}

	@LogMethod(level=LogLevel.DEBUG)
	public void saveConformance(JSONObject conformance) {

		String destination = Utils.getFilenameWithDirectory(common.workingDirectory, common.outputFileName);			

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
