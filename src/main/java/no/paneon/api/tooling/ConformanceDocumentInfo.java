package no.paneon.api.tooling;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import no.paneon.api.utils.Config;

public class ConformanceDocumentInfo extends DocumentInfo {

	static final Logger LOG = LogManager.getLogger(ConformanceDocumentInfo.class);
	
	public ConformanceDocumentInfo(JSONObject rules, Args.Common args) {
		super(rules,args);
	}
	
	public String getDocID() {
		return super.getDocID() + Config.getString("conformance.docId.postfix");
	}

	public String getTitle() {
		return super.getTitle() + " Conformance Profile";
	}
	
}
