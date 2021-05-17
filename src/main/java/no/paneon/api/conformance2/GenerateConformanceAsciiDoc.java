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


public class GenerateConformanceAsciiDoc extends GenerateCommon {

	static final Logger LOG = LogManager.getLogger(GenerateConformanceAsciiDoc.class);

	Args.ConfDoc args;
	ConformanceModel model;

	public GenerateConformanceAsciiDoc(Args.ConfDoc args) {
		super(args);
		this.args = args;
		this.model  = new ConformanceModel();

	}
	
	@Override
	@LogMethod(level=LogLevel.DEBUG)
	public void execute() {
		
		super.execute();

		ConformanceGenerator confGen = new ConformanceGenerator(args, this.model);        	     
		confGen.generateDocument();			
			  
		Timestamp.timeStamp("finished conformance generation");
	
	}
	

}
