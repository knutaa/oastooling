package no.paneon.api.tooling.userguide;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import no.paneon.api.utils.Out;
import no.paneon.api.utils.Utils;

public class Samples {

	static final Logger LOG = LogManager.getLogger(Samples.class);

	private static final String NEWLINE = "\n";

	public static String readPayload(String workingDirectory, JSONObject example, String requestResponse) {
		StringBuilder res = new StringBuilder();
				
		if(example==null) return res.toString();
		
		LOG.debug("readPayload: requestResponse={} example={}", requestResponse, example.toString(2));
		
		try {
			Object source = example.optQuery("/" + requestResponse + "/file" );
			
			if(source!=null) {
				String sourceFile = source.toString();
				
				try {
					String payload = Utils.readFile(workingDirectory + "/" + sourceFile);
				
					res.append(NEWLINE);
					res.append(payload);
				} catch(Exception e) {
					Out.printAlways("... unable to read sample payload from " + sourceFile);
				}
				
			}
			
		} catch(Exception e) {
			
		}
				
		return res.toString();
	}
}
