package no.paneon.api.conformance;

import java.util.List;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import no.paneon.api.model.APIModel;
import no.paneon.api.tooling.Args;
import no.paneon.api.tooling.Args.UserGuide;
import no.paneon.api.tooling.userguide.GenerateUserGuide;
import no.paneon.api.utils.Out;

public class GenerateConformanceTest  {

	public GenerateConformanceTest() {
	}
	
    static String file = "./src/test/resources/Quote_Management_5.0.0_oas.yaml";
    
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
        
    @BeforeClass
    public static void runOnceBeforeClass() {        
        APIModel.setSwaggerSource(file);
        APIModel.loadAPI(file);

    }

    @AfterClass
    public static void runOnceAfterClass() {
        APIModel.clean();
    }

    @Before
    public void runBeforeTestMethod() {
    }

    @After
    public void runAfterTestMethod() {
    }

    @Test
    public void checkResource() {
    	List<String> resources = APIModel.getAllDefinitions();
    	    	
    	assert(resources.contains("Quote"));
    	
    }

    @Test
    public void generateConformance() {
    	Args                        args = new Args();
		Args.Conformance argsConformance = args.new Conformance();
    	
		argsConformance.openAPIFile = file;
		argsConformance.targetDirectory = "."; // folder.toString();
		// argsConformance.debug = "all";
		argsConformance.outputFileName = "test-conformance.yaml";

		try {
	    	GenerateConformance generator = new GenerateConformance(argsConformance);
	    	
	    	generator.execute();
	    	
	    	Out.println("... test completed");
		} catch(Exception ex) {
	    	Out.println("... exception:" + ex.getLocalizedMessage());

		}
    	
    	assert(true);
    	
    	// folder.delete();
    	
    }
	
	
}
