package no.paneon.api.tooling;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import no.paneon.api.model.APIModel;
import no.paneon.api.utils.Config;
import no.paneon.api.utils.Out;

public class DocumentInfo {

	static final Logger LOG = LogManager.getLogger(DocumentInfo.class);

	public String title;
	public String docid;
	public String release;
	public String date;
	public String year;
	public String revision;
	public String iprMode;
	public String status;
	public String releaseStatus;

	JSONObject rules;
	
	public DocumentInfo(JSONObject rules) {
		this.rules = rules;
				
		this.title          = this.getTitle();
		this.docid          = this.getDocID();
		this.release        = this.getRelease();
		this.date           = this.getDate();
		this.year           = this.getYear();

		this.revision       = this.getRevision();
		this.iprMode        = this.getIPRMode();
		this.status         = this.getStatus();
		this.releaseStatus  = this.getReleaseStatus();

	}
	
	public String getDocID() {
		Optional<String> optDocId = getOptionalString(rules,"#/api/tmfId");		
		Optional<String> optDocIdAPI = getOptionalString(APIModel.getDocumentDetails(), "#/variables/DocumentNumber");

		String docId = optDocId.isPresent()    ? optDocId.get()    : 
			           optDocIdAPI.isPresent() ? optDocIdAPI.get() : 
					   "TBD";

		return docId;
	}

	private Optional<String> getOptionalString(JSONObject source, String path) {
		Optional<String> res = Optional.empty();
		if(source!=null) {
			Object value = source.query(path);
			if(value!=null) res = Optional.of(value.toString() );
		}
		
		LOG.debug("path: {} res={}",  path, res);
		
		return res;
	}

	public String getRelease() {
		Optional<String> optReleaseAPI = getOptionalString(APIModel.getDocumentDetails(), "#/variables/Release");

		return optReleaseAPI.isPresent() ? optReleaseAPI.get() : "TBD";	
	}

	public String getDate() {
		Optional<String> optDateAPI = getOptionalString(APIModel.getDocumentDetails(), "#/variables/Date");

		return optDateAPI.isPresent() ? optDateAPI.get() : "TBD";	
	}

	public String getRevision() {
		Optional<String> optDocId    = getOptionalString(rules, "#/api/version");
		Optional<String> optDocIdAPI = getOptionalString(APIModel.getDocumentDetails(), "#/variables/DocumentVersion");

		LOG.debug("doc details: {}", APIModel.getDocumentDetails().toString(4));
		
		return optDocId.isPresent() ? optDocId.get() : 
			   optDocIdAPI.isPresent() ? optDocIdAPI.get() :
			   "TBD";	
		
	}

	public String getIPRMode() {
		return Config.getString("iprMode");
	}

	public String getStatus() {
		return "TBD";
	}

	public String getReleaseStatus() {
		return "TBD";
	}

	public String getYear() {
		Optional<String> optYearAPI = getOptionalString(APIModel.getDocumentDetails(), "#/variables/Year");

		return optYearAPI.isPresent() ? optYearAPI.get() : "TBD";	
	}

	public String getTitle() {
		Optional<String> optDocTitle = getOptionalString(this.rules,"#/api/name");
		Optional<String> optDocTitleAPI = getOptionalString(APIModel.getDocumentDetails(), "#/variables/ApiName");

		return optDocTitle.isPresent() ? optDocTitle.get() : 
			optDocTitleAPI.isPresent() ? optDocTitleAPI.get() : 
			"TBD";
	}
}
