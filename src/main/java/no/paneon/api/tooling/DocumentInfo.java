package no.paneon.api.tooling;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import no.paneon.api.model.APIModel;
import no.paneon.api.tooling.Args.UserGuide;
import no.paneon.api.utils.Config;
import no.paneon.api.utils.Out;
import no.paneon.api.utils.Utils;

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

	public String maturityLevel;
	public String teamDate;
	public String releaseStatus;
	public String approvalStatus;
	public String version;
	
	JSONObject rules;
	
	private Map<String,String> userGuideMeta = new HashMap<>();
	
	public DocumentInfo(JSONObject rules) {
		this(rules,null);
	}

	public DocumentInfo(JSONObject rules, Args.Common args) {
		this.rules = rules;
						
		this.addMeta(args);
		
		this.title          = this.getTitle();
		this.docid          = this.getDocID();
		this.release        = this.getRelease();
		this.date           = this.getDate();
		this.year           = this.getYear();

		this.revision       = this.getRevision();
		this.iprMode        = this.getIPRMode();
		this.status         = this.getStatus();
		this.releaseStatus  = this.getReleaseStatus();
		
		this.maturityLevel  = this.getMaturityLevel();
		this.teamDate       = this.getTeamDate();
		this.approvalStatus = this.getApprovalStatus();
		this.version        = this.getVersion();
		

	}
	
	public String getDocID() {
		
		Optional<String> optDocId = getOptionalString(rules,"#/tmfId");		
		Optional<String> optDocIdAPI = getOptionalString(APIModel.getDocumentDetails(), "#/variables/DocumentNumber");

		String docId = optDocId.isPresent()    ? optDocId.get()    : 
			           optDocIdAPI.isPresent() ? optDocIdAPI.get() : 
					   "TBD";

		return docId;
	}

	private Optional<String> getOptionalString(JSONObject source, String path) {
		Optional<String> res = Optional.empty();
		if(source!=null && path!=null) {
			Object value = source.optQuery(path);
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
		Optional<String> optDocId    = getOptionalString(rules, "#/version");
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
		return getVariableReference("RELEASE-STATUS");
	}

	public String getYear() {
		Optional<String> optYearAPI = getOptionalString(APIModel.getDocumentDetails(), "#/variables/Year");

		return optYearAPI.isPresent() ? optYearAPI.get() : "TBD";	
	}

	public String getTitle() {
		Optional<String> optDocTitle = getOptionalString(this.rules,"#/name");
		Optional<String> optDocTitleAPI = getOptionalString(APIModel.getDocumentDetails(), "#/variables/ApiName");

		return optDocTitle.isPresent() ? optDocTitle.get() : 
			optDocTitleAPI.isPresent() ? optDocTitleAPI.get() : 
			"TBD";
	}
	
	
	public String getVariableReference(String meta) {
		String res = this.userGuideMeta.containsKey(meta) ? this.userGuideMeta.get(meta) : "TBD";
		
		LOG.debug("getVariableReference: {}={}", meta, res);

		return res;
			
	}
	
	public String getMaturityLevel() {
		return getVariableReference("MATURITY-LEVEL");
	}
	
	public String getTeamDate() {
		return getVariableReference("TEAM-DATE");
	}
	
	public String getApprovalStatus() {
		return getVariableReference("APPROVAL-STATUS");
	}
	
	public String getVersion() {
		return getVariableReference("VERSION");
	}

	final static String USERGUIDE_DIR = "documentation/userguide";
	final static String CONFORMANCE_DIR = "documentation/conformance";

	public void addMeta(Args.Common args) {
		
		if(args==null) return;
		
		String existing = args.workingDirectory + "/" + USERGUIDE_DIR + "/" + args.outputFileName;
		processMeta(existing);
		
		existing = args.workingDirectory + "/" + CONFORMANCE_DIR + "/" + args.outputFileName;
		processMeta(existing);
	}
	
	public void processMeta(String file) {	
		LOG.debug("processMeta::file={}", file);
		try {
			String content = Utils.readFile(file);
			String[] lines = content.split("\n");
			
			List<String> metaData = Config.get("userguide::metaData");
			
			for(String line : lines) {
				if(line.startsWith(":")) {
					String[] parts = line.split(":",3);
					if(parts.length==3) {
						String property = parts[1];
						String value = parts[2].strip();

						if(metaData.contains(property)) {
							this.userGuideMeta.put(property,value);
							LOG.debug("addMeta::line={} meta={}", line, property);

						}
					}
				}
			}
			
		} catch(Exception e) {
			// nothing to do
		}
	}
	
}
