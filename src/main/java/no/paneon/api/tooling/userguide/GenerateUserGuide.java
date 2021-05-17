package no.paneon.api.tooling.userguide;

import no.paneon.api.generator.GenerateCommon;
import no.paneon.api.logging.AspectLogger.LogLevel;
import no.paneon.api.logging.LogMethod;
import no.paneon.api.model.APIModel;
import no.paneon.api.model.JSONObjectHelper;
import no.paneon.api.utils.Config;
import no.paneon.api.utils.Out;
import no.paneon.api.utils.Utils;

import no.paneon.api.conformance.ConformanceItem;
import no.paneon.api.conformance.ConformanceModel;
import no.paneon.api.graph.APIGraph;
import no.paneon.api.graph.Node;
import no.paneon.api.utils.JSONObjectOrArray;
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

		UserGuideGenerator userGuide = new UserGuideGenerator(args,this.model);        	     
		userGuide.generateDocument();			
			  
		Timestamp.timeStamp("finished conformance guide generation");
	
	}
	

}
