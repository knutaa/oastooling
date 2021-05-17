package no.paneon.api.tooling;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.beust.jcommander.JCommander;

import no.paneon.api.conformance.GenerateConformance;
import no.paneon.api.conformance2.GenerateConformanceAsciiDoc;
import no.paneon.api.tooling.userguide.GenerateUserGuide;
import no.paneon.api.utils.Out;
import no.paneon.api.utils.Timestamp;
	
public class App {

    static final Logger LOG = LogManager.getLogger(App.class);
		
	JCommander commandLine;

	Args.UserGuide     argsUserGuide ;
	Args.UserGuide     argsAsciiDocGuide ;
	Args.Conformance   argsConformance   ;
	Args.ConfDoc  	   argsConfDocument  ;

	static final String ARG_USER_GUIDE  = "userguide";
	static final String ARG_GEN_CONF    = "generate-conformance";
	static final String ARG_CONFORMANCE = "conformance";

	App(String ... argv) {
		     		
		Args args = new Args();
				
		argsUserGuide  = args.new UserGuide();
		argsConformance    = args.new Conformance();
		argsConfDocument   = args.new ConfDoc();

		commandLine = JCommander.newBuilder()
		    .addCommand("userguide",             argsUserGuide)
		    .addCommand("generate-conformance",  argsConformance)
		    .addCommand("conformance",           argsConfDocument)
		    .build();

		try {
			commandLine.parse(argv);						
		} catch(Exception ex) {
			Out.println(ex.getMessage());
			Out.println("Use option --help or -h for usage information");
			
			System.exit(1);
		}	

	}
	
	public static void main(String ... args) {
		App app = new App(args);
		
		try {			
			app.run();
		} catch(Exception ex) {
			Out.println("error: " + ex.getLocalizedMessage());	
			ex.printStackTrace();
			System.exit(1);			
		}
		
		Timestamp.timeStamp("finished", Timestamp.FROM_START);

		
	}


	void run() {
						
		if (commandLine.getParsedCommand()==null) {
            commandLine.usage();
            return;
        }
		
		Timestamp.timeStamp("arguments available");

    	switch(commandLine.getParsedCommand()) {
    	
    	case ARG_USER_GUIDE:    		
    		if(argsUserGuide.outputFileName==null && !argsUserGuide.generatedOnly) {
    			Out.println("... missing output file argument");
    		} else {
    			
    			if(argsUserGuide.generatedTargetDirectory==null) {
    				argsUserGuide.generatedTargetDirectory=argsUserGuide.targetDirectory;
    			}
    			
    			GenerateUserGuide genUserGuideAsciiDoc = new GenerateUserGuide(argsUserGuide);
    			genUserGuideAsciiDoc.execute();
    		}
    		break;
 
       	case ARG_GEN_CONF:
       		if(argsConformance.outputFileName==null) {
    			Out.println("... missing output file argument");
    		} else {
    			GenerateConformance genconf = new GenerateConformance(argsConformance);
    			genconf.execute();
    		}
    		break;

       	case ARG_CONFORMANCE:
       		if(argsConfDocument.outputFileName==null) {
    			Out.println("... missing output file argument");
    		} else {
    			GenerateConformanceAsciiDoc genconf = new GenerateConformanceAsciiDoc(argsConfDocument);
    			genconf.execute();
    		}
    		break;

    		    		
    	default:
    		Out.println("... unrecognized command " + commandLine.getParsedCommand());
    		Out.println("... use --help for command line options");
    		System.exit(1);
    	}
    	
		
	}


}
