package org.iplantc.service.transfer.irods4;

import java.io.File;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

public class IrodsBaseTestCase {

    public static String SPECIAL_CHARS = "&";
    protected ThreadLocal<IRODS4> threadClient = new ThreadLocal<IRODS4>();

    public IrodsBaseTestCase() {
        super();
    }
    
    private String getEnvironmentVariable(String variableName, String defaultValue) {
        String variableValue = System.getenv(variableName);
        return StringUtils.isEmpty(variableValue) ? defaultValue : variableValue;
    }

    /**
     * Threadsafe singleton accessor for the {@link IRODS4} client used in the tests.
     * Each client has a threadsafe work directory to guarantee safe parallel execution
     * of test cases.
     * @return threadsafe instance of a {@link IRODS4} client
     * @throws RemoteCredentialException 
     * @throws RemoteDataException 
     */
    protected IRODS4 getClient() {
        if (threadClient.get() == null) {
            IRODS4 client = new IRODS4(
                    getEnvironmentVariable("IRODS_HOST", "docker.example.com"),
                    Integer.valueOf(getEnvironmentVariable("IRODS_PORT", "1257")),
                    getEnvironmentVariable("IRODS_USERNAME", "testuser"),
                    getEnvironmentVariable("IRODS_PASSWORD", "testuser"),
                    getEnvironmentVariable("IRODS_RESOURCE", "demoResc"),
                    getEnvironmentVariable("IRODS_ZONE", "iplant"),
                    getClass().getSimpleName() + "/thread-" + Thread.currentThread().getId());
            
            threadClient.set(client);
        } 
        
        return threadClient.get();
    }

    @BeforeClass(alwaysRun = true)
    protected void beforeSubclass() throws Exception {
    
        getClient().authenticate();
        if (!getClient().mkdirs("")) {
            if (!getClient().doesExist(""))
                Assert.fail("Test home directory " + getClient().resolvePath("") + " exists, but is not a directory.");
        }
    }

    @AfterClass(alwaysRun = true)
    protected void afterClass() throws Exception {
        try
        {
            getClient().authenticate();
            // remove test directory
            getClient().delete("..");
            Assert.assertFalse(getClient().doesExist(""), "Failed to clean up home directory " + getClient().resolvePath("") + "after test.");
        } 
        catch (Exception e) {
            Assert.fail("Failed to clean up test home directory " + getClient().resolvePath("") + " after test method.", e);
        }
        finally {
            try { getClient().disconnect(); } catch (Exception e) {}
        }
    }
    

    /**
     * Generates a temp file and uploads to test home directory on remote system
     * @return {@link File} reference to local file. Remote {@link IRODSFile} object has same name.
     */
    protected File stageTestFile() {
        File localFile = null;
        try {
            localFile = File.createTempFile(UUID.randomUUID().toString(), "tmp");
            FileUtils.write(localFile, "Some temp data");
            
            getClient().put(localFile, localFile.getName());
        }
        catch (Exception e) {
            Assert.fail("Failed to upload test file.", e);
        }
        
        return localFile;
    }
    
    /**
     * Generates a temp directory tree and uploads to test home directory on remote system
     * @return {@link File} reference to local directory. Remote {@link IRODSFile} collection has same name.
     */
    protected File stageTestDirectory() {
        File testDir = null;
        try {
            
            testDir = new File(FileUtils.getTempDirectory(), UUID.randomUUID().toString());
            if (!testDir.exists()) {
                testDir.mkdirs();
            }
            
            FileUtils.write(new File(testDir, "alpha.txt"), "this is alpha.txt");
            FileUtils.write(new File(testDir, "beta.txt"), "this is beta.txt");
            File subdir = new File(testDir, "sub1");
            if (!subdir.exists()) {
                subdir.mkdirs();
            }
            FileUtils.write(new File(subdir, "sub_alpha.txt"), "this is sub1/sub_alpha.txt");
            FileUtils.write(new File(subdir, "sub_beta.txt"), "this is sub1/sub_beta.txt");
            
            
            getClient().put(testDir, testDir.getName());
        }
        catch (Exception e) {
            Assert.fail("Failed to upload test directory.", e);
        }
        
        return testDir;
    }

}