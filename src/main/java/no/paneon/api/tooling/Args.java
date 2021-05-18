package no.paneon.api.tooling;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.beust.jcommander.Parameter;

public class Args {

	public class UserGuide extends Common {

		@Parameter(names = { "--intro-section" }, description = "Text to include in the API intro section (file in .docx format)")
		public String introSection = null;

		@Parameter(names = { "--insert-text" }, description = "Text fragments to insert given as 'fragment=filename' (without any space - file in .docx format)")
		public List<String> insertText = new LinkedList<>();

		@Parameter(names = { "--skip-toc" }, description = "Do not add the ToC - this may be relevant to NOT have the warning fields ('This document contains fields that may refer to other files. Do you want to update the fields in this document?')")
		public boolean skipTOC = false;

		@Parameter(names = { "--export-template" }, description = "Export the embedded template (in .docx format) to file specified with --output")
		public boolean exportTemplate = false;
		
		@Parameter(names = { "--diagrams" }, description = "Diagram source configuration (diagrams.yaml)")
		public String diagrams = null;
		
		@Parameter(names = { "--template-directory" }, description = "Document template directory, defaults to current directory")
		public String templateDirectory = ".";

		@Parameter(names = { "--generated-only" }, description = "Only create the files with generated content, do not add the template files")
		public boolean generatedOnly = false;

	}

	public class ConformanceGuide extends Common {

		@Parameter(names = { "--template-directory" }, description = "Document template directory, defaults to current directory")
		public String templateDirectory = ".";

		@Parameter(names = { "--generated-only" }, description = "Only create the files with generated content, do not add the template files")
		public boolean generatedOnly = false;

	}

	public class Conformance extends Common {

		@Parameter(names = { "--complete" }, description = "Include also optional elements")
		public boolean complete = false;

		@Parameter(names = { "--mandatory-only" }, description = "Only include mandatory elements")
		public boolean mandatoryOnly = false;

	}

	public class Extract extends Common {

		@Parameter(names = { "--complete" }, description = "Include also optional elements")
		public boolean complete = false;

	}

	public class Common {

		@Parameter(names = { "--explicit-document-details" }, description = "Use explicit document details (specified using the --config option)")
		public boolean explicitDocumentDetails = false;

		@Parameter(names = { "--conformance" }, description = "User defined conformance in JSON or YAML format (file name)")
		public String conformance = null;

		@Parameter(names = { "--defaults" }, description = "Default conformance settings in JSON or YAML format (file name)")
		public String defaults = null;

		@Parameter(names = { "--existing" }, description = "Existing conformance specification (.docx file)")
		public String existingSpecification = null;

		@Parameter(names = { "--rules" }, description = "API rules file")
		public String rulesFile = null;

		@Parameter(names = { "-c", "--config" }, description = "Config files (.json) - one or more")
		public List<String> configs = new ArrayList<>();

		@Parameter(names = { "--ignore-internal-config" }, description = "Do not include the internal configuration file (default=false)")
		public boolean ignoreInternalConfig = false;

		@Parameter(names = { "-o", "--output" }, description = "Output file name - used for both conformance specification (.docx) or extraction (.json or .yaml)")
		public String outputFileName = null;

		@Parameter(names = { "-d", "--debug" }, description = "Debug mode (off,all,info,debug,error,trace,warn,fatal)")
		public String debug = "verbose";

		@Parameter(names = { "--keep-non-printable" }, description = "Keep non-printable characters in extracted text, default is false")
		public boolean keepNonPrintable = false;

		@Parameter(names = { "--no-linebreaks" }, description = "Do not insert linebreaks in extracted text, default is false")
		public boolean noLinebreaks = false;

		@Parameter(names = { "--working-directory" }, description = "Working directory, default is the current directory")
		public String workingDirectory  = ".";

		@Parameter(names = { "--conformance-source-only" }, description = "Only include conformance in explicit --conformance statement")
		public boolean conformanceSourceOnly = false;

		@Parameter(names = { "--silent" }, description = "Do not include progress messages")
		public boolean silentMode = false;

		@Parameter(names = { "--schema-defaults" }, description = "Common schema conformance defaults in JSON or YAML format (file name)")
		public String schemaDefaultsSource = null;

		@Parameter(names = { "--target-directory" }, description = "Target directory for output. Default is the current directory")
		public String targetDirectory  = ".";

		@Parameter(names = { "--generated-target-directory" }, description = "Directory for generated results. Default is the target directory")
		public String generatedTargetDirectory  = null;
		
		@Parameter(names = { "-f", "--file", "--swagger", "--openapi" }, description = "Input OpenAPI file (or optionally as default argument)")
		public String openAPIFile;

		@Parameter(names = { "--resource" }, description = "Specific resource to process (default is all)")
		public String resource = null;

		@Parameter(names = { "-s", "--source" }, description = "Include source details in footer (0=no, 1=basic, >1 include filename)")
		public Integer source = 0;

		@Parameter(names = { "-t", "--template" }, description = "Document template (.docx or .mustache file) - defaults to embedded version")
		public String template = null;

	}

	public class Usage {

		@Parameter(names = { "-h", "--help" }, description = "Usage details", help = true)
		public boolean help = false;

	}

}
