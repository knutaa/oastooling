package no.paneon.api.tooling.userguide;

import org.json.JSONObject;

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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import no.paneon.api.tooling.Args;
import no.paneon.api.tooling.DocumentInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
// import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UserGuideGenerator {

	static final Logger LOG = LogManager.getLogger(UserGuideGenerator.class);
		
	private static final String EXCLUDED_RESOURCES = "userguide::excludedResources";
	private static final String RESOURCE_LAYOUT = "userguide::resourceLayout";

	private static final boolean KEEP_EXISTING = true;

	Args.UserGuide args;
	
	APIGraph apiGraph;
	
	ConformanceModel conformance;

	UserGuideData userGuideData;

	GenerateUserGuide generator;
	
	public UserGuideGenerator(GenerateUserGuide generator) {
		
		Timestamp.timeStamp("... start set-up");
		
		this.generator = generator;
		this.args = generator.args;	
		this.conformance = generator.model;				
		this.userGuideData = new UserGuideData();
		
		this.userGuideData.imageFormat = this.args.imageFormat;
				
		Timestamp.timeStamp("... finished set-up");

	}


	@LogMethod(level=LogLevel.DEBUG)
	public void generateDocument() {
				
		try {
			
			Timestamp.timeStamp("start user guide data");
			
			generateUserGuideData();
				
			Timestamp.timeStamp("finished user guide data");

			this.userGuideData.documentInfo = new DocumentInfo(Config.getRules());
			
			generatePartials(userGuideData);
			
			generator.processTemplates(args, userGuideData, "userguide.generated.templates", "userguide.templates", args.generatedOnly);

			Map<String,String> filesToCopy = Config.getMap("userguide.generated.filesToCopy");
								
			generator.copyFiles(filesToCopy, args.generatedTargetDirectory, KEEP_EXISTING);
			
			filesToCopy = Config.getMap("userguide.filesToCopy");
			generator.copyFiles(filesToCopy, KEEP_EXISTING);


		} catch(Exception ex) {
			Out.printAlways("... error generating userguide: exception=" + ex.getLocalizedMessage());
			ex.printStackTrace();
			System.exit(0);
		}

				
	}
	
	private void generateUserGuideData() {
		
		new ResourcesFragment(this).process();
		
		new NotificationsFragment(this).process();
		
		new OperationsFragment(this).process();
		
	}

	private UserGuideData generatePartials(UserGuideData userguide) {

		UserGuideData res = new ResourcesFragment(this).generatePartials(userguide);
		
		return res;
		
	}

	private void processTemplates(UserGuideData data, boolean generatedOnly) {
		
		String targetDirectory = this.getTargetDirectory("");
		
		String generatedTargetDirectory = this.getGeneratedTargetDirectory();
		
		String relativePathToGeneratedDirectory = GenerateCommon.extractRelativePath(targetDirectory,generatedTargetDirectory);
				
		data.generatedPath = relativePathToGeneratedDirectory;
		
		LOG.debug("relativePathToGeneratedDirectory: {}", relativePathToGeneratedDirectory); 
		
		Map<String,String> templatesToProcess = Config.getMap("userguide.generated.templates");

		templatesToProcess.entrySet().stream()
	       .sorted(Map.Entry.comparingByKey())
			.forEach(entry -> {
				String template = entry.getKey();
				String destination = entry.getValue();
				
				String target = generatedTargetFileName(generatedTargetDirectory, destination);
	
				processTemplate(template, data, target);
	
			});

		templatesToProcess = Config.getMap("userguide.templates");
								
		templatesToProcess.entrySet().stream()
	       .sorted(Map.Entry.comparingByKey())
			.forEach(entry -> {
				String template = entry.getKey();
				String destination = entry.getValue();
				
				LOG.debug("template: {}", template);
				
				if(destination.contentEquals("$output")) destination = args.outputFileName;
				
				String target = generatedTargetFileName(targetDirectory, destination);
				
				if(!generatedOnly || !fileExists(target) ) {
					processTemplate(template, data, target);
				} else {
					Out.println("... file " + destination + " exists - not overwritten");
				}
				
			});

		
	}


	private boolean fileExists(String filename) {
		File f = new File(filename);
		return f.exists();
	}


	private String generatedTargetFileName(String targetDirectory, String filename) {
		String target = filename;
		File f = new File(target);
		
		LOG.debug("generatedTargetFileName: filename={}",  filename);
		
		if(!f.isAbsolute()) {
			target = targetDirectory + target;
		}
		return target;
	}


	protected void processTemplate(String template, Object data, String outputFileName) {
		
		LOG.debug("processTemplate: {} outputFileName: {}",  template, outputFileName);
		
		if(Config.has(template)) template = Config.getString(template);
		
		try {
			MustacheFactory mf = new DefaultMustacheFactory();
			InputStream is = Utils.getFileInputStream(template, null, args.workingDirectory, args.templateDirectory);
			Reader reader = new InputStreamReader(is);
			Mustache m = mf.compile(reader, "template");
	
			StringWriter writer = new StringWriter();
			
			m.execute(writer, data).flush();
			
			String text = writer.toString();
						
			Utils.save(text, outputFileName);	
			
		} catch(Exception ex) {
			Out.debug("*** exception: {}", ex.getLocalizedMessage());
		}
	
	}


	@LogMethod(level=LogLevel.DEBUG)	
	protected String getJSON(String resource, JSONObject config) {
				
		String fileName = Utils.getFileName(args.workingDirectory, config, "tableSource").replace("${RESOURCE}", resource);
		
		String content = "";
		if(isDirectoryPresent(fileName)) {
			JSONObjectOrArray json = JSONObjectOrArray.readJSONObjectOrArray(fileName);			
			content = json.toString(2);
			
			LOG.debug("getJSON: file={}", fileName);

		}
		return content;
		
	}

	
	Set<String> directoriesNotSeen = new HashSet<>();
	
	private boolean isDirectoryPresent(String fileName) {
		File file = new File(fileName);
				
		file = file.getParentFile();
				
		if(file.isDirectory())
			return true;
		
		String dir=file.getName();
		if(!directoriesNotSeen.contains(dir)) {
			LOG.info("... *** unable to locate directory " + dir);
			directoriesNotSeen.add(dir);
		}
	
		return false;
		
	}


	@LogMethod(level=LogLevel.DEBUG)
	protected String constructStatement(List<String> statements) {
		StringBuilder res = new StringBuilder();
		for(String s : statements) {
			String cleaned =  s.trim();
			
			if(!cleaned.isEmpty() && !cleaned.endsWith(".")) cleaned=cleaned+".";
			
			cleaned = Utils.upperCaseFirst(cleaned);
			
			if(res.length()>0) res.append(" ");
			res.append(cleaned);
		}
		
		LOG.debug("constructStatement: statements={} res={}", statements, res);

		return res.toString();
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	protected String constructStatement(String ... statements) {
		return constructStatement(Arrays.asList(statements));
	}


	@LogMethod(level=LogLevel.DEBUG)
	protected String getDescriptionForType(APIGraph apiGraph, String type) {
		String res="";
		Node node = apiGraph.getNode(type);
		if(node!=null) res=node.getDescription();
		return res;
	}

	private final List<String> VOCALS = Arrays.asList("A", "E", "I", "O" );

	@LogMethod(level=LogLevel.DEBUG)
	protected String getArticle(String type) {
		String res="a";
		if(!type.isEmpty()) {
			String firstChar = type.substring(0, 1).toUpperCase();
			if(VOCALS.contains(firstChar)) {
				res = "an";
			}
		}
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	protected String constructDescriptionForType(String type) {
		String res=type;
		if(!type.isEmpty()) {
			String firstChar = type.substring(0, 1).toUpperCase();
			if(VOCALS.contains(firstChar)) {
				res = "An " + type;
			} else {
				res = "A " + type;
			}
		}
		return res;
	}


	@LogMethod(level=LogLevel.DEBUG)
	public List<String> getResources() {
		List<String> resources = APIModel.getResources();
		
		LOG.debug("getResources:: {}", resources);

		final List<String> excludedResources = Config.get(EXCLUDED_RESOURCES);
		
		Predicate<String> notExcludedResource  = s -> !excludedResources.contains(s);
		
		resources = resources.stream()
						.filter(notExcludedResource)
						.sorted(Comparator.comparingInt(String::length))
						.collect(toList());
		
		List<String> configSequence = Config.get(RESOURCE_LAYOUT);
		
		if(!configSequence.isEmpty()) {
			configSequence = configSequence.stream().filter(configSequence::contains).collect(toList());
			resources.removeAll(configSequence);
			configSequence.addAll(resources);
			resources=configSequence;
		}
		
		return resources;
	}


	public String getTargetDirectory() {
		return getTargetDirectory("", args.targetDirectory);		
	}

	public String getTargetDirectory(String subdir) {
		return getTargetDirectory(subdir, args.targetDirectory);		
	}
	
	public String getGeneratedTargetDirectory() {
		return getGeneratedTargetDirectory("");
	}

	public String getGeneratedTargetDirectory(String subdir) {
		return getTargetDirectory(subdir, args.generatedTargetDirectory);
	}
	
	public String getTargetDirectory(String subdir, String commandArg) {
		String targetDir;
		
		if(commandArg!=null && !commandArg.isEmpty()) {
			targetDir = commandArg + "/" + subdir;
		} else {
			targetDir = args.workingDirectory!=null ? args.workingDirectory : System.getProperty("user.dir");
			
			File dir = new File(targetDir + "/documentation");
			if(dir.isDirectory()) {
				targetDir = targetDir + "/documentation";
			}
			
			targetDir = targetDir + "/" + subdir;

		}
		
		Utils.createDirectory(targetDir);	
			
		return targetDir;
		
	}
	
	
}
