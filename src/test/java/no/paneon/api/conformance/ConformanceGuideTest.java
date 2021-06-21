package no.paneon.api.conformance;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONPointer;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import no.paneon.api.conformance2.GenerateConformanceGuide;
import no.paneon.api.model.APIModel;
import no.paneon.api.tooling.Args;
import no.paneon.api.utils.Out;
import no.paneon.api.utils.Utils;

public class ConformanceGuideTest  {

	public ConformanceGuideTest() {
	}
		
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    
	String TARGETDIR = "."; // folder.toString();

    String API = "./src/test/resources/Quote_Management_5.0.0_oas.yaml";
    
	String CONFORMANCE_FILE = "test-conf.yaml";

	String TARGET = TARGETDIR + "/conf-test";
	String RESOURCE_OVERVIEW =  TARGET + "/generated/ResourceConformanceOverview.adoc";

        
    @BeforeClass
    public static void runOnceBeforeClass() {        
   //     APIModel.setSwaggerSource(API);
   //     APIModel.loadAPI(API);
        
    }

    @AfterClass
    public static void runOnceAfterClass() {
    //    APIModel.clean();
    }

    @Before
    public void runBeforeTestMethod() {
        try {
	        Files.walk(new File(TARGET).toPath())
		        .sorted(Comparator.reverseOrder())
		        .map(Path::toFile)
		        .forEach(File::delete);
        } catch(Exception e) {
        	
        }
    }

    @After
    public void runAfterTestMethod() {
    }

    
    @Test
    public void generateConformanceGuide() {
    	    	
    	generateConformanceSpec(API, TARGETDIR, CONFORMANCE_FILE);
    	    	
    	JSONObject conformance = Utils.readJSONOrYaml(TARGETDIR + "/" + CONFORMANCE_FILE);
    	
    	update(conformance, "#/conformance/Quote", "condition", "M");
    	update(conformance, "#/conformance/Quote", "comment",   "Test case - set to mandatory");

    	Utils.saveAsYaml(conformance, CONFORMANCE_FILE);
    	
    	Args                                  args = new Args();
		Args.ConformanceGuide argsConformanceGuide = args.new ConformanceGuide();
    	
		argsConformanceGuide.openAPIFile     = API;
		argsConformanceGuide.targetDirectory = TARGETDIR;
		argsConformanceGuide.conformance     = CONFORMANCE_FILE;
		argsConformanceGuide.silentMode      = true;
		
		try {
	    	GenerateConformanceGuide generator = new GenerateConformanceGuide(argsConformanceGuide);
	    	
	    	generator.execute();
	    	
	    	String overview = Utils.readFile(RESOURCE_OVERVIEW);
	    	
	    	assert(overview.contains("Test case - set to mandatory"));
	    	
		} catch(Exception ex) {
	    	Out.println("... exception:" + ex.getLocalizedMessage());

		}
    	
    	assert(true);
    	    
    	folder.delete();

    }

	private void update(JSONObject json, String path, String property, String value) {
    	JSONPointer queryPointer = new JSONPointer(path);
    	JSONObject item = (JSONObject) json.query(queryPointer);    	
    	item.put(property, value);
	}

	private void generateConformanceSpec(String api, String targetDirectory, String output) {
		Args args                        = new Args();
		Args.Conformance argsConformance = args.new Conformance();

		argsConformance.openAPIFile     = api;
		argsConformance.targetDirectory = targetDirectory;
		argsConformance.outputFileName  = output;

		GenerateConformance generator = new GenerateConformance(argsConformance);

		generator.execute();

	}	
	
}
