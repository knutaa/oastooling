package no.paneon.api;

import java.util.List;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import no.paneon.api.model.APIModel;
import no.paneon.api.tooling.Args;
import no.paneon.api.tooling.Args.UserGuide;
import no.paneon.api.tooling.userguide.GenerateUserGuide;

public class OAS2Test  {

	public OAS2Test() {
	}
	
    static String file = "./src/test/resources/TMF620-ProductCatalog-v4.1.0.swagger.json";
    
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
    	    	
    	assert(resources.contains("Catalog"));
    	
    }

//    @Test
//    public void generateDiagrams() {
//    	Args               args = new Args();
//		UserGuide argsUserguide = args.new UserGuide();
//    	
//		argsUserguide.openAPIFile = file;
//		argsUserguide.targetDirectory = folder.toString();
//
//    	GenerateUserGuide generator = new GenerateUserGuide(argsUserguide);
//    	
//    	generator.execute();
//    	
//    	assert(true);
//    	
//    	folder.delete();
//    }
	
	
}
