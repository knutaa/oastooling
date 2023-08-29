package no.paneon.api.conformance;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import no.paneon.api.model.APIModel;
import no.paneon.api.tooling.Args;
import no.paneon.api.utils.Config;
import no.paneon.api.utils.Out;

import no.paneon.api.conformance2.GenerateConformanceGuide;

public class CheckAccountConformance  {

	public CheckAccountConformance() {
		
        Config.setBoolean("includeMetaProperties",true);

	}
	
    static String oasFile = "./src/test/resources/TMF666-Account_Management-v5.0.0.oas.yaml";
    static String conformanceFile = "./src/test/resources/TMF666_conformance.yaml";
    static String rulesFile = "./src/test/resources/TMF666_Account.rules.yaml";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
        
    @BeforeClass
    public static void runOnceBeforeClass() {   
    	        
        APIModel.setSwaggerSource(oasFile);
        APIModel.loadAPI(oasFile);

    	Config.init();
        Config.setBoolean("includeMetaProperties",true);

        Out.debug("... include={}", Config.getBoolean("includeMetaProperties"));

        Out.debug("... before class");

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
    	    	
    	assert(resources.contains("BillingAccount"));
    	
    }

    @Test
    public void checkConformance() {
    	
 	
    	Args                             args = new Args();
		Args.ConformanceGuide argsConformance = args.new ConformanceGuide();
    	
		argsConformance.openAPIFile = oasFile;
		argsConformance.targetDirectory = ".";
		argsConformance.conformance = conformanceFile;
		
		argsConformance.outputFileName = "tmf666-conformance-guide.adoc";

		try {
	    	GenerateConformanceGuide generator = new GenerateConformanceGuide(argsConformance);
	    	
	    	ConformanceModel conformance = generator.getConformanceModel();
	    	
	    	conformance.generateConformance();
	    	
	    	List<String[]> conf = conformance.getNonPatchable("BillingAccount");
	    	
	    	Set<String> confItems = conf.stream().map(p -> p[0]).collect(Collectors.toSet());
	    	
	    	Out.debug("... confItems={}", confItems);

	    	Set<String> expected = new HashSet<>( Set.of("@type", "href", "id", "@baseType", "lastUpdate", "@schemaLocation" ) );
	    	
	    	expected.removeAll(confItems);
	    	
	    	Out.debug("... residue={} size={}", expected, expected.size());

	    	assert(expected.isEmpty());
	    	
	    	Out.debug("... test completed");
	    	
		} catch(Exception ex) {
	    	Out.println("... exception:" + ex.getLocalizedMessage());

		}
    	
    	assert(true);
    	    	
    }
	
	
    @Test
    public void generateConformance() {
    	Args                             args = new Args();
		Args.Conformance argsConformance = args.new Conformance();
    	
		argsConformance.openAPIFile = oasFile;
		argsConformance.targetDirectory = ".";
		argsConformance.rulesFile = rulesFile;
		
		argsConformance.outputFileName = "tmf666-conformance.yaml";

		try {
	    	GenerateConformance generator = new GenerateConformance(argsConformance);
	    	
	    	generator.init();
	    	
	    	ConformanceModel conformance = generator.model;
	    	    	
	    	List<String[]> conf = conformance.getNonPatchable("BillingAccount");
	    	
	    	Out.debug("... conf={}", conf);

	    	Set<String> confItems = conf.stream().map(p -> p[0]).collect(Collectors.toSet());
	    	
	    	Out.debug("... confItems={}", confItems);

	    	Set<String> expected = new HashSet<>( Set.of("@type", "href", "id", "@baseType", "@schemaLocation", "lastUpdate", "accountBalance" ) );
	    	
	    	expected.removeAll(confItems);
	    	
	    	Out.debug("... residue={}", expected);

	    	assert(expected.isEmpty());
	    	
	    	Out.debug("... test completed");
	    	
		} catch(Exception ex) {
	    	Out.println("... exception:" + ex.getLocalizedMessage());

		}
    	
    	assert(true);
    	
    	
    }
	
    
    @Test
    public void checkMandatoryInPost() {
    	Args                             args = new Args();
		Args.Conformance argsConformance = args.new Conformance();
    	
		argsConformance.openAPIFile = oasFile;
		argsConformance.targetDirectory = ".";
		// argsConformance.rulesFile = rulesFile;
		
		argsConformance.outputFileName = "tmf666-conformance.yaml";

		try {
	    	GenerateConformance generator = new GenerateConformance(argsConformance);
	    	
	    	generator.init();
	    	
	    	ConformanceModel conformance = generator.model;
	    	    	
	    	List<ConformanceItem> conf = conformance.getMandatoryConformanceInPost(null,"BillingAccount");
	    	
	    	Set<String> confItems = conf.stream().map(p -> p.label).collect(Collectors.toSet());
	    	
	    	Out.debug("... confItems={}", confItems);

	    	Set<String> expected = new HashSet<>( Set.of("relatedParty.role", "@type", "name", "relatedParty", "relatedParty.@type") );
	    	
	    	expected.removeAll(confItems);
	    	
	    	Out.debug("... residue={}", expected);

	    	assert(expected.isEmpty());
	    	
	    	Out.debug("... test completed");
	    	
		} catch(Exception ex) {
	    	Out.println("... exception:" + ex.getLocalizedMessage());

		}
    	
    	assert(true);
    	
    	
    }
    
    
}
