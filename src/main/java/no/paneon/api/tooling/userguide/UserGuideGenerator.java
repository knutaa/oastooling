package no.paneon.api.tooling.userguide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import no.paneon.api.conformance.ConformanceItem;
import no.paneon.api.conformance.ConformanceModel;
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

import no.paneon.api.conformance.ConformanceModel;
import no.paneon.api.tooling.Args;
import no.paneon.api.tooling.DocumentInfo;
import no.paneon.api.tooling.Args.ConformanceGuide;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

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
	
	Args.UserGuide args;
	
	APIGraph apiGraph;
	
	ConformanceModel conformance;

	UserGuideData userGuideData;

	public UserGuideGenerator(Args.UserGuide args, ConformanceModel conformanceModel) {
		
		this.args = args;	
		this.conformance = conformanceModel;				
		this.userGuideData = new UserGuideData();
		
		this.userGuideData.imageFormat = this.args.imageFormat;
				
	}


	@LogMethod(level=LogLevel.DEBUG)
	public void generateDocument() {
				
		try {
			
			generateUserGuideData();
				
			Timestamp.timeStamp("finished user guide data");

			this.userGuideData.documentInfo = new DocumentInfo(conformance.getRules());
			
			generatePartials(userGuideData);

			processTemplates(userGuideData);
					
			copyFilesForGenerated();
			
			if(!args.generatedOnly) {
				copyFiles();
							
				// Boolean res = Utils.copyFile("UserGuide_template.adoc", args.outputFileName, args.targetDirectory, args.templateDirectory);
				//if(!res) {
				//	Out.debug("... unable to copy user guide to file {}", args.outputFileName);
				//} else {
				//	Out.println("... saved user guide to file {}", args.outputFileName);
				// }
			} else {
				Out.println("... template files are not copied (only the files with generated content from the API specification)" );
			}

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

	private void processTemplates(UserGuideData data) {
		Map<String,String> templatesToProcess = Config.getMap("userguide.generated.templates");
		
		String targetDirectory = this.getTargetDirectory("");
		
		String generatedTargetDirectory = this.getGeneratedTargetDirectory("");
		
		String relativePathToGeneratedDirectory = extractRelativePath(targetDirectory,generatedTargetDirectory);
				
		data.generatePath = relativePathToGeneratedDirectory;
		
		LOG.debug("relativePathToGeneratedDirectory: {}", relativePathToGeneratedDirectory); 
		
		templatesToProcess.entrySet().stream().forEach(entry -> {
			String template = entry.getKey();
			String destination = entry.getValue();
			
			processTemplate(template, data, generatedTargetDirectory + destination);

		});

		templatesToProcess = Config.getMap("userguide.templates");
								
		templatesToProcess.entrySet().stream().forEach(entry -> {
			String template = entry.getKey();
			String destination = entry.getValue();
			
			if(destination.contentEquals("$output")) destination = args.outputFileName;
			
			processTemplate(template, data, targetDirectory + destination);

		});

		
		
	}


	private String extractRelativePath(String dir1, String dir2) {
				
		String[] dir1parts = dir1.split("/");
		String[] dir2parts = dir2.split("/");

		boolean done = false;
		int pos=0;
		
		while(!done) {
			if(pos>=dir1parts.length) done=true;
			if(pos>=dir2parts.length) done=true;
			
			if(!done) {
				if(dir1parts[pos].contentEquals(dir2parts[pos])) {
					pos++;
				} else {
					done=true;
				}
			}
		}
		
		dir1parts = Arrays.copyOfRange(dir1parts, pos, dir1parts.length);
		dir2parts = Arrays.copyOfRange(dir2parts, pos, dir2parts.length);

		dir1 = String.join("/", dir1parts);
		dir2 = String.join("/", dir2parts);

		dir1 = dir1.replace("./", "");
		dir2 = dir2.replace("./", "");
		
		int steps = 2 + dir1.replaceAll("[^/]*", "").replaceAll("^/", "").length();
		
		LOG.debug("dir1=" + dir1 + " dir2=" + dir2 + " steps=" + steps);
		
		StringBuilder res = new StringBuilder();
		while(steps>0) {
			res.append("../");
			steps--;
		}
		
		res.append(dir2);
		return res.toString();
		
	}


	private void copyFiles() {
		Map<String,String> filesToCopy = Config.getMap("userguide.filesToCopy");	
		copyFiles(filesToCopy, args.targetDirectory);
	}

	private void copyFilesForGenerated() {
		Map<String,String> filesToCopy = Config.getMap("userguide.generated.filesToCopy");
		copyFiles(filesToCopy, args.generatedTargetDirectory);
	}

	private void copyFiles(Map<String,String> filesToCopy, String targetDirectory) {		
		filesToCopy.entrySet().stream().forEach(entry -> {
			String file = entry.getKey();
			String dir = entry.getValue();
			
			Boolean res = Utils.copyFile(file, dir + "/" + file, targetDirectory, args.templateDirectory);
			if(!res) {
				Out.debug("... unable to copy file {}", file);
			}

		});
		
	}

	protected void processTemplate(String template, Object data, String outputFileName) {
		
		LOG.debug("processTemplate: {} outputFileName: {}",  template, outputFileName);
		
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
		}
		return content;
		
	}

	
	Set<String> directoriesNotSeen = new HashSet<>();
	
	private boolean isDirectoryPresent(String fileName) {
		File file = new File(fileName);
		
		if(file!=null) file = file.getParentFile();
		
		boolean isPresent = file!=null && file.getParentFile().isDirectory();
		
		if(isPresent)
			return true;
		
		if(file!=null) {	
			String dir=file.getName();
			if(!directoriesNotSeen.contains(dir)) {
				Out.printAlways("... *** unable to locate directory " + dir);
				directoriesNotSeen.add(dir);
			}
		} else
			Out.printAlways("... unable to locate file " + fileName);
		
		return false;
		
	}


	@LogMethod(level=LogLevel.DEBUG)
	protected String constructStatement(String ... statements) {
		StringBuilder res = new StringBuilder();
		for(String s : statements) {
			String cleaned =  s.trim();
			
			if(!cleaned.isEmpty() && !cleaned.endsWith(".")) cleaned=cleaned+".";
			
			cleaned = Utils.upperCaseFirst(cleaned);
			
			if(res.length()>0) res.append(" ");
			res.append(cleaned);
		}
		return res.toString();
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
		
		final List<String> excludedResources = Config.get(EXCLUDED_RESOURCES);
		
		Predicate<String> notExcludedResource  = s -> !excludedResources.contains(s);
		
		resources = resources.stream()
						.filter(notExcludedResource)
						.sorted(Comparator.comparingInt(String::length))
						.collect(toList());
		
		return resources;
	}


	public String getTargetDirectory(String subdir) {
		return getTargetDirectory(subdir, args.targetDirectory);		
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
