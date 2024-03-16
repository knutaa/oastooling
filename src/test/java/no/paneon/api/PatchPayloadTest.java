package no.paneon.api;

import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import no.paneon.api.graph.Node;
import no.paneon.api.model.APIModel;
import no.paneon.api.tooling.Args;
import no.paneon.api.tooling.Args.UserGuide;
import no.paneon.api.tooling.userguide.GenerateUserGuide;
import no.paneon.api.utils.Config;
import no.paneon.api.utils.Out;

public class PatchPayloadTest  {

    static final Logger LOG = LogManager.getLogger(PatchPayloadTest.class);

	public PatchPayloadTest() {
	}
	
    static String file = "./src/test/resources/TMF629-Customer_Management-v5.0.0.oas.yaml";
    
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
        
    @BeforeClass
    public static void runOnceBeforeClass() {     
    	
    	Config.setBoolean("keepMVOFVOResources",true);
    	
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
    	    	
    	LOG.debug("resources={}", resources);

    	assert(resources.contains("Customer_MVO"));
    	
    	JSONObject entityMVO = APIModel.getDefinition("Entity_MVO");
    	
    	LOG.debug("Entity_MVO={}", entityMVO);

    	LOG.debug("Entity_MVO={}", entityMVO);
    	
    	Set<String> properties = APIModel.getPropertiesExpanded("Entity_MVO");

    	LOG.debug("Entity_MVO properties={}", properties);
    	
    	JSONObject patchPayload = APIModel.getResourceForPatch("Customer");
    	
    	LOG.debug("Customer_MVO patchPayload={}", patchPayload);
    	
    	patchPayload = APIModel.getPropertyObjectForResource(patchPayload);
    	
    	LOG.debug("Customer_MVO patchPayload={}", patchPayload);

		JSONObject def = APIModel.getDefinitionByReference("#/components/schemas/Customer_MVO");

    	LOG.debug("Customer_MVO patchPayload={}", def);

     	resources = APIModel.getAllDefinitions();
    	
    	assert(resources.contains("Customer_MVO"));
    	
    	LOG.debug("resources={}", resources);

   
    }

	
}
