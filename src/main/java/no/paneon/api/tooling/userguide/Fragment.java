package no.paneon.api.tooling.userguide;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import no.paneon.api.tooling.Args.UserGuide;
import no.paneon.api.utils.Config;
import no.paneon.api.utils.Out;
import no.paneon.api.utils.Utils;
import no.paneon.api.utils.Utils.CopyStyle;

public class Fragment {

	static final Logger LOG = LogManager.getLogger(Fragment.class);

	private static final String NEWLINE = "\n";

	private static final String FRAGMENT_DIR = "/documentation/userguide/fragments";
	
	public Boolean hasFragment;
	public String  sourceLocation;
	
	public Fragment() {
		
	}
	
	public Fragment(String workingDirectory, String resource) {
		JSONObject config = Config.getConfig("fragments");

		resource = resource.replace(" ", "");
		
		if(config!=null) LOG.debug("Fragment:: target={} config={}", resource, config.keySet());

		if(config!=null && config.has(resource)) {
			this.hasFragment = true;
			this.sourceLocation = config.getString(resource);
			
			LOG.debug("Fragment:: target={}", resource);

		} else {
			this.hasFragment = false;
		}
		
	}

	public static void readFragmentDetails(String workingDirectory) {

		String fragmentDirectory = workingDirectory + FRAGMENT_DIR;
		
		JSONObject fragmentConfig = new JSONObject();
		
		try {

			File f = new File(fragmentDirectory);
			if(f.isDirectory()) {
				File[] content = f.listFiles();
				for(File file : content) {
					if(file.isFile()) {
						String name = file.getName().replaceFirst("[.][^.]+$", "");
						// String location = file.getAbsolutePath().replace(fragmentDirectory+"/", "");
						String location = file.getName();
						fragmentConfig.put(name, location);
					}
				}
			}
			
			if(!fragmentConfig.isEmpty()) {
				JSONObject cfg = new JSONObject();
				cfg.put("fragments", fragmentConfig);
				Config.addConfiguration(cfg);
			
				LOG.debug("readFragmentDetails:: cfg={}", cfg.toString(2));
				
			}
			
		} catch(Exception e) {
			
		}
				
	}
	
	public static Fragment getFragment(String workingDirectory, String resource) {
		
		return new Fragment(workingDirectory,resource);
		
//		Fragment res = new Fragment();
//		
//		JSONObject config = Config.getConfig("fragments");
//						
//		if(config!=null && config.has(resource)) {
//			res.hasFragment = true;
//			res.sourceLocation = config.getString(resource);
//		} else {
//			res.hasFragment = false;
//		}
//	
//		return res;

	}

	public static void copyFiles(UserGuide args) {
		if(Config.has("fragments")) {
			LOG.debug("... copy fragment files");
			LOG.debug("... copy fragment files {} : ", args.workingDirectory);
			LOG.debug("... copy fragment files {} : ", args.generatedTargetDirectory);

			if(args.generatedTargetDirectory.contentEquals(args.workingDirectory)) {
				Out.debug("... fragments not copied");
				return;
			}
			
			JSONObject config = Config.getConfig("fragments");
			for(String key : config.keySet()) {
				String source = config.getString(key);
				Out.debug("... copy fragment file '{}'", source);
				
				String sourceDirectory = args.workingDirectory + FRAGMENT_DIR;
				String targetDirectory = args.generatedTargetDirectory + "/fragments";

				Utils.copyFile(source, source, targetDirectory, sourceDirectory, CopyStyle.OVERWRITE);
				
			}
		}		
	}
	
}
