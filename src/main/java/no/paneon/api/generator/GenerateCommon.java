package no.paneon.api.generator;

import no.paneon.api.tooling.Args;
import no.paneon.api.logging.LogMethod;
import no.paneon.api.logging.AspectLogger.LogLevel;
import no.paneon.api.model.APIModel;
import no.paneon.api.utils.Config;
import no.paneon.api.utils.JSONObjectOrArray;
import no.paneon.api.utils.Out;
import no.paneon.api.utils.Timestamp;
import no.paneon.api.utils.Utils;
import no.paneon.api.utils.Utils.CopyStyle;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import static java.util.stream.Collectors.toList;


public class GenerateCommon {
	
	protected Args.Common common;
	
    static final Logger LOG = LogManager.getLogger(GenerateCommon.class);

	public GenerateCommon(Args.Common common) {
		this.common = common;
		
		List<String> dirs = getDirectories(common.workingDirectory);
				
		try {
			APIModel.loadAPI(common.openAPIFile, Utils.getFile(common.openAPIFile, dirs));
		
			Timestamp.timeStamp("api specification loaded");
		} catch(Exception ex) {
			Out.println("... unable to read API specification from " + common.openAPIFile);
			System.exit(0);
		}

		Out.silentMode = common.silentMode;
		
		setLogLevel( Utils.getLevelmap().get(common.debug));

        Config.setConfigSources(common.configs);
        
    	if(common.debug!=null && Utils.getLevelmap().containsKey(common.debug)) {
			setLogLevel( Utils.getLevelmap().get(common.debug) );
    	}
        
    	if(common.timestamp) {
    		Timestamp.setActive();  		
    	}
    	
    	if(common.openAPIFile!=null) {
    		APIModel.setSwaggerSource(common.openAPIFile);
    	}
 
    	if(common.conformanceSourceOnly) {
    		Config.setBoolean("conformanceSourceOnly",true);
    	}
    	
    	if(common.defaults!=null) {
    		Config.setDefaults(common.defaults);
    		LOG.debug("set defaults from file={}", common.defaults);
    	}   	
    	
    	if(common.rulesFile!=null) {
    		Config.setRulesSource(common.rulesFile);
    		LOG.debug("set rules source ={}", common.rulesFile);
    	}
    	
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	private List<String> getDirectories(String baseDir) {
		List<String> res = new LinkedList<>();
		if(baseDir!=null) {
			res.add(baseDir);
			if(!baseDir.isEmpty()) {
				res.add(baseDir + "/swaggers" );
			}
		}
		
		return res;
	}

	@LogMethod(level=LogLevel.DEBUG)
	protected void setLogLevel(org.apache.logging.log4j.Level level) {
//		LoggerContext context = (LoggerContext) LogManager.getContext(false);
//		Configuration config = context.getConfiguration();
//		LoggerConfig rootConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
//		rootConfig.setLevel(level);	
//		
//		AspectLogger.setGlobalDebugLevel(level);
		
	}

	@LogMethod(level=LogLevel.DEBUG)
	protected void execute() {
		Timestamp.timeStamp("common execute finished");
	}
	
	public void processTemplates(Args.Common args, GeneratorData data, String generatedTemplates, String templates, boolean generatedOnly) {
		
		String targetDir          = getTargetDirectory("", args.workingDirectory, args.targetDirectory);
		String generatedTargetDir = getTargetDirectory("", args.workingDirectory, args.generatedTargetDirectory);
		
		String relativePathToGeneratedDirectory = extractRelativePath(targetDir,generatedTargetDir);
				
		data.generatedPath = relativePathToGeneratedDirectory;
		
		LOG.debug("targetDir={}",  targetDir);
		LOG.debug("generatedTargetDir={}",  generatedTargetDir);

		LOG.debug("relativePathToGeneratedDirectory: {}", relativePathToGeneratedDirectory); 
		
		Map<String,String> templatesToProcess = Config.getMap(generatedTemplates);
		templatesToProcess.entrySet().stream()
	        .sorted(Map.Entry.comparingByKey())
			.forEach(entry -> {
				String template = entry.getKey();
				String destination = entry.getValue();
				
				String target = generatedTargetFileName(generatedTargetDir, destination);
	
				processTemplate(template, data, target);

			});

		templatesToProcess = Config.getMap(templates); 								
		templatesToProcess.entrySet().stream()
	        .sorted(Map.Entry.comparingByKey())
			.forEach(entry -> {
				String template = entry.getKey();
				String destination = entry.getValue();
				
				if(destination.contentEquals("$output")) destination = args.outputFileName;
				
				String target = generatedTargetFileName(targetDir, destination);
				
				if(!generatedOnly || !fileExists(target) ) {
					processTemplate(template, data, target);
				} else {
					Out.println("... file " + destination + " exists - not overwritten");
				}
			
			});

		
	}


	private static boolean fileExists(String filename) {
		File f = new File(filename);
		return f.exists();
	}


	private static String generatedTargetFileName(String targetDirectory, String filename) {
		String target = filename;
		File f = new File(target);
		
		LOG.debug("generatedTargetFileName: filename={}",  filename);
		
		if(!f.isAbsolute()) {
			target = targetDirectory + target;
		}
		return target;
	}


	public static String extractRelativePath(String dir1, String dir2) {

		
		if(dir1.isEmpty()) return dir2;
		
		String[] dir1parts = dir1.split("/");
		String[] dir2parts = dir2.split("/");

		LOG.debug("dir1={} dir1parts.length={} dir2={} dir2parts.length={} steps={}", dir1, dir1parts.length, dir2, dir2parts.length);

		int pos=0;
		boolean done = dir1parts.length==0;

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

		int steps = dir1parts.length;

		StringBuilder res = new StringBuilder();
		while(steps>0) {
			res.append("../");
			steps--;
		}

		res.append(String.join("/", dir2parts));

		LOG.debug("dir1={} dir2={} steps={} res={}", dir1, dir2, steps, res);

		return res.toString().replace("//", "/");

	}
	
	public void copyFiles(Map<String,String> filesToCopy, String targetDirectory, boolean keepExisting) {		
		
		filesToCopy.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
		.forEach(entry -> {
			String source = entry.getKey();
			
			String file = Utils.getBaseFileName(source);
						
			String dir = entry.getValue();
			
			String target = dir + "/" + file;
			
			CopyStyle copyStyle = keepExisting ? CopyStyle.KEEP_ORIGINAL : CopyStyle.OVERWRITE;
			
			Boolean copied = Utils.copyFile(source, target, targetDirectory, common.templateDirectory, copyStyle);
			
			if(!copied) {
				Out.debug("... unable to copy file {}", file);
			}

		});
		
	}
	
	public void copyFiles(Map<String,String> filesToCopy, boolean keepExisting) {		
				
		filesToCopy.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
		.forEach(entry -> {
			String source = entry.getKey();
			
			String file = Utils.getBaseFileName(source);
						
			String dir = entry.getValue();
			
			String target = dir + "/" + file;
			
			CopyStyle copyStyle = keepExisting ? CopyStyle.KEEP_ORIGINAL : CopyStyle.OVERWRITE;
			
			Boolean copied = Utils.copyFile(source, target, common.targetDirectory, common.templateDirectory, copyStyle);
			
			if(!copied) {
				Out.debug("... unable to copy file {}", file);
			}

		});
		
	}

	protected static void processTemplate(String template, Object data, String outputFileName) {
		
		LOG.debug("processTemplate: {} outputFileName: {}",  template, outputFileName);
		
		if(Config.has(template)) template = Config.getString(template);
		
		try {
			MustacheFactory mf = new DefaultMustacheFactory();
			InputStream is = Utils.getFileInputStream(template, null);
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
	protected static String getJSON(String resource, JSONObject config, String workingDirectory) {
				
		String fileName = Utils.getFileName(workingDirectory, config, "tableSource").replace("${RESOURCE}", resource);
		
		String content = "";
		if(isDirectoryPresent(fileName)) {
			JSONObjectOrArray json = JSONObjectOrArray.readJSONObjectOrArray(fileName);			
			content = json.toString(2);
		}
		return content;
		
	}

	
	private static Set<String> directoriesNotSeen = new HashSet<>();
	
	private static boolean isDirectoryPresent(String fileName) {
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
	
	protected static String getTargetDirectory(String subdir, String workingDirectory, String targetDirectory) {
		String targetDir;
		
		if(targetDirectory!=null && !targetDirectory.isEmpty()) {
			targetDir = targetDirectory + "/" + subdir;
		} else {
			targetDir = workingDirectory!=null ? workingDirectory : System.getProperty("user.dir");
			
			File dir = new File(targetDir + "/documentation");
			if(dir.isDirectory()) {
				targetDir = targetDir + "/documentation";
			}
			
			targetDir = targetDir + "/" + subdir;

		}
		
		Utils.createDirectory(targetDir);	
			
		return targetDir;
		
	}
		

	public List<String> copyFilesWithDestination(Map<String,String> filesToCopy) {
		List<String> res = new LinkedList<>();

		filesToCopy.entrySet().stream().forEach(entry -> {
			String source = entry.getKey();
			
			String file = Utils.getBaseFileName(source);
						
			String target = entry.getValue();
			
			Boolean copied = Utils.copyFile(source, target, common.targetDirectory, common.templateDirectory);
			if(!copied) {
				Out.debug("... unable to copy file {}", file);
			} else {
				res.add(Utils.getBaseFileName(target));
			}

		});
		
		return res.stream().sorted().collect(toList());
	}



}
