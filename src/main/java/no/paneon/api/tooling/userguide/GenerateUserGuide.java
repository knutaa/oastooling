package no.paneon.api.tooling.userguide;

import no.paneon.api.generator.GenerateCommon;
import no.paneon.api.logging.AspectLogger.LogLevel;
import no.paneon.api.logging.LogMethod;

import no.paneon.api.conformance.ConformanceModel;
import no.paneon.api.utils.Timestamp;

import no.paneon.api.tooling.Args;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class GenerateUserGuide extends GenerateCommon {

	static final Logger LOG = LogManager.getLogger(GenerateUserGuide.class);

	Args.UserGuide args;
	ConformanceModel model;

	public GenerateUserGuide(Args.UserGuide args) {
		super(args);
		this.args = args;
		this.model  = new ConformanceModel();

	}
	
	@Override
	@LogMethod(level=LogLevel.DEBUG)
	public void execute() {
		
		super.execute();

		Timestamp.timeStamp("start conformance guide generation");
		
		this.model.extractFromSwagger();
		this.model.extractFromRules();
		boolean forced=true;
		this.model.generateConformance(forced);
		
		Fragment.readFragmentDetails(args.workingDirectory);
		
		UserGuideGenerator userGuide = new UserGuideGenerator(this);        	     

		userGuide.generateDocument();			
			  
		Timestamp.timeStamp("finished conformance guide generation");
	
	}
	

}
