package no.paneon.api.tooling.userguide;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import no.paneon.api.generator.GeneratorData;
import no.paneon.api.tooling.DocumentInfo;

public class UserGuideData extends GeneratorData {

	public String imageFormat; 
	
	// public String generatePath = "../../generated"; 

	public Map<String,ResourceData> resources;
	
	public Collection<ResourceData> resourcesData;

	public int numberOfNotifications;

	// public DocumentInfo documentInfo;
		
	public class ResourceData {
		
		public String resource;
		public String resourceLabel;
		
		public String resourceArticle;
		
		public String description;
		
		public List<Sample> samples;
		public Boolean hasSamples;

//		public String sample;
//		public String sampleSource;

		public List<DiagramData> diagrams;
		
		public FieldsData fields;
	
		public List<NotificationData>  notifications;

		public List<OperationData> operations;
		
		public String fileSource;
		
		public String diagramSource = "a test";
		
		public Boolean hasPatchable;
		public Boolean hasNonPatchable;
		
	}
	
	public class DiagramData {
		
		public String sourceLocation;
		
		public String imgfile;
		public String puml;
		
		public String resource;
		
		public String resourceLabel;
		
		public FileData svg_source;
		public FileData png_source;
		public FileData puml_source;
		
		
	}
	
	public class FieldsData {
		
		public String resource;
		
		public String description;
		
		public Boolean hasFields;
		public List<FieldData> fields;
		
		public List<FieldsData> subResources;
		
	}
	
	public class FieldData {
		public String name;
		public String description;
	}
	
	public class NotificationData {
		public String notification;
		public String notificationLabel;
		public String notificationLabelShort;
		public String sample;
		
		public String message;
		
	}
	
	public class OperationData {
		public String operation;
		public String operationLabel;
		public String uRL;
		
		public String description;
		
		public Boolean isCreate = false;
		public Boolean isList = false;
		public Boolean isRetrieve = false;
		public Boolean isReplace = false;
		public Boolean isDelete = false;
		public Boolean isPartialUpdate = false;

		public Boolean hasPatchable = false;
		public Boolean hasNonPatchable = false;

		public List<PropertyRuleData> patchable = new LinkedList<>();
		public List<PropertyRuleData> nonPatchable = new LinkedList<>();

		public Boolean hasMandatory = false;
		public Boolean hasNonMandatory = false;

		public List<PropertyRuleData> mandatory = new LinkedList<>();
		public List<PropertyRuleData> nonMandatory = new LinkedList<>();

		public Boolean hasSamples = false;
		public List<OperationSampleData> samples;
		
	}
	
	public class OperationSampleData {
		public String description;
		public String request;
		public String response;
		
		public FileData requestSource;
		public FileData responseSource;
		
	}
	
	public class FileData {
		public String filename;
	}
	
	public class PropertyRuleData {
		public String name;
		public String rule;
		
		public PropertyRuleData(String[] props) {
			this.name = props[0];
			this.rule = props[1];
		}

		public PropertyRuleData() {
		}
	
	}
	
	public class Sample {		
		public String sample;
		public String sampleSource;
		public String description;
	}

}
