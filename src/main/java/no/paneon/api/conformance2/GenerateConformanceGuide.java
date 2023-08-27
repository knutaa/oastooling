package no.paneon.api.conformance2;

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

import no.paneon.api.tooling.Args;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class GenerateConformanceGuide extends GenerateCommon {

	static final Logger LOG = LogManager.getLogger(GenerateConformanceGuide.class);

	Args.ConformanceGuide args;
	ConformanceModel model;

	public GenerateConformanceGuide(Args.ConformanceGuide args) {
		super(args);
		this.args = args;
		this.model  = new ConformanceModel();

    	if(this.args.conformance!=null) {
    		this.model.setConformance(this.args.conformance);
    	}
	}
	
	@Override
	@LogMethod(level=LogLevel.DEBUG)
	public void execute() {
		
		super.execute();

		model.init();
		
		model.extractFromSwagger();
		
		model.extractFromRules();
		
		model.expandDefaults();
		
		boolean completeGeneration=true;
		model.generateConformance(completeGeneration);

		LOG.debug("GenerateConformanceGuide: model={}", model);
		
		ConformanceGenerator confGen = new ConformanceGenerator(this);        	     
		confGen.generateDocument();			
			  
		Timestamp.timeStamp("finished conformance generation");
	
	}
	
	public ConformanceModel getConformanceModel() {
		return this.model;
	}

}
