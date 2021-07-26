package no.paneon.api.userguide;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.*;

import no.paneon.api.generator.GenerateCommon;

import no.paneon.api.utils.Out;

public class GeneratedPath  {

	static final Logger LOG = LogManager.getLogger(GeneratedPath.class);

	public GeneratedPath() {
	}
	
	@Test
    public void test0() {

		String dir1 = "";
		String dir2 = "generated";
		
		String extractRelativePath = GenerateCommon.extractRelativePath(dir1,dir2);
		
		Out.debug("checkGeneratedPath dir1={} dir2={} extractRelativePath={}", dir1, dir2, extractRelativePath);
		
    	assert(extractRelativePath.contentEquals("generated"));
    	
    }
	
	@Test
    public void test1() {

		String dir1 = "userguide";
		String dir2 = "generated";
		
		String extractRelativePath = GenerateCommon.extractRelativePath(dir1,dir2);
		
		Out.debug("checkGeneratedPath dir1={} dir2={} extractRelativePath={}", dir1, dir2, extractRelativePath);
		
    	assert(extractRelativePath.contentEquals("../generated"));
    	
    }

	@Test
    public void test2() {

		String dir1 = "documentation/userguide";
		String dir2 = "generated";
		
		String extractRelativePath = GenerateCommon.extractRelativePath(dir1,dir2);
		
		LOG.debug("checkGeneratedPath dir1={} dir2={} extractRelativePath={}", dir1, dir2, extractRelativePath);
		
    	assert(extractRelativePath.contentEquals("../../generated"));
    	
    }
	

	@Test
    public void test3() {

		String dir1 = "documentation/userguide";
		String dir2 = "generated/userguide";
		
		String extractRelativePath = GenerateCommon.extractRelativePath(dir1,dir2);
		
		LOG.debug("checkGeneratedPath dir1={} dir2={} extractRelativePath={}", dir1, dir2, extractRelativePath);
		
    	assert(extractRelativePath.contentEquals("../../generated/userguide"));
    	
    }
	
	@Test
    public void test4() {

		String dir1 = "/a/b/c/d/e/f/documentation/userguide";
		String dir2 = "/a/b/c/d/e/f/generated/userguide";
		
		String extractRelativePath = GenerateCommon.extractRelativePath(dir1,dir2);
		
		LOG.debug("checkGeneratedPath dir1={} dir2={} extractRelativePath={}", dir1, dir2, extractRelativePath);
		
    	assert(extractRelativePath.contentEquals("../../generated/userguide"));
    	
    }
	
	
	
	@Test
    public void test5() {

		String dir1 = "/a/b/c/d/e/f2/documentation/userguide";
		String dir2 = "/a/b/c/d/e/f/generated/userguide";
		
		String extractRelativePath = GenerateCommon.extractRelativePath(dir1,dir2);
		
		LOG.debug("checkGeneratedPath dir1={} dir2={} extractRelativePath={}", dir1, dir2, extractRelativePath);
		
    	assert(extractRelativePath.contentEquals("../../../f/generated/userguide"));
    	
    }
	
	
	
}
