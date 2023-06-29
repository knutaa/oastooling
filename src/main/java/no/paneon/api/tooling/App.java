package no.paneon.api.tooling;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.beust.jcommander.JCommander;

import no.paneon.api.conformance.GenerateConformance;
import no.paneon.api.conformance2.GenerateConformanceGuide;
import no.paneon.api.tooling.userguide.GenerateUserGuide;
import no.paneon.api.utils.Config;
import no.paneon.api.utils.Out;
import no.paneon.api.utils.Timestamp;
	
public class App {

    static final Logger LOG = LogManager.getLogger(App.class);
		
	JCommander commandLine;

	Args.UserGuide         argsUserGuide ;
	Args.UserGuide         argsAsciiDocGuide ;
	Args.Conformance       argsConformance   ;
	Args.ConformanceGuide  argsConfDocument  ;
	Args.Usage  		   argsUsage  ;

	static final String ARG_USER_GUIDE  = "userguide";
	static final String ARG_GEN_CONF    = "conformance-data";
	static final String ARG_CONFORMANCE = "conformance-guide";

	App(String ... argv) {
		     		
		Args args = new Args();
				
		argsUserGuide    = args.new UserGuide();
		argsConformance  = args.new Conformance();
		argsConfDocument = args.new ConformanceGuide();
		argsUsage 		 = args.new Usage();

		commandLine = JCommander.newBuilder()
		    .addCommand(ARG_USER_GUIDE,      	argsUserGuide)
		    .addCommand(ARG_GEN_CONF,  			argsConformance)
		    .addCommand(ARG_CONFORMANCE,     	argsConfDocument)
		    .addCommand("--help",       		argsUsage)
		    .addCommand("help",               	argsUsage)
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
		
		try {	
			
			Timestamp.timeStamp("... start");
			
			App app = new App(args);

			app.run();
			
		} catch(Exception ex) {
			Out.println("error: " + ex.getLocalizedMessage());	
			ex.printStackTrace();
			System.exit(1);			
		}
		
		Timestamp.timeStamp("finished", Timestamp.FROM_START);

		
	}


	void run() {
				
		Config.init();
		
		if (commandLine.getParsedCommand()==null) {
            commandLine.usage();
			System.exit(1);			
        }
		
		try {
			final Properties properties = new Properties();
			properties.load(this.getClass(). getClassLoader().getResourceAsStream("project.properties"));		
			String version = properties.getProperty("version");
			String artifactId = properties.getProperty("artifactId");
			
			String command = commandLine.getParsedCommand()!=null ? commandLine.getParsedCommand() : "";

			Out.printAlways("{} {} {}", artifactId, version, command);
			
		} catch(Exception e) {
			Out.printAlways("... version information not available: {}", e.getLocalizedMessage());
		}

		Timestamp.timeStamp("arguments available");

    	switch(commandLine.getParsedCommand()) {
    	
       	case "--help":
    	case "help":
    		commandLine.usage();
    		break;
 
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
    			GenerateConformanceGuide genconf = new GenerateConformanceGuide(argsConfDocument);
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
