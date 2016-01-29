package org.iplantc.service.transfer.irods4;

import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test class for Jargon behavior against IRODS4 instances. These tests were run 
 * against the {@code agaveapi/irods:4.0.3} Docker image used for integration 
 * testing by the Agave Platform.
 *
 * @author Rion Dooley <dooley@tacc.utexas.edu>
 */
@Test(singleThreaded=true)
public class Irods4PathExistenceTest extends IrodsBaseTestCase {
    
    @DataProvider(parallel=false)
    protected Object[][] testIRODSFileExistsReturnsFalseOnMissingFileProvider() throws Exception {
        
        return new Object[][] {
                { UUID.randomUUID().toString(), "Random missing filename should return false" },
                { UUID.randomUUID().toString() + "/", "Random missing filename with trailing slash should return false" },
                { UUID.randomUUID().toString() + ".txt", "Random missing filename with file extension should return false" },
                { UUID.randomUUID().toString() + ".txt/", "Random missing filename with file extension and trailing slash should return false" },
                
                { "." + UUID.randomUUID().toString(), "Random missing shadow filename should return false" },
                { "." + UUID.randomUUID().toString() + "/", "Random missing shadow filename with trailing slash should return false" },
                { "." + UUID.randomUUID().toString() + ".txt", "Random missing shadow filename with file extension should return false" },
                { "." + UUID.randomUUID().toString() + ".txt/", "Random missing shadow filename with file extension and trailing slash should return false" },
                
                { "I/Do/Not/Exist/unless/some/evil/person/has/this/test/" + UUID.randomUUID().toString(), "Random missing path should return false" },
                { "I/Do/Not/Exist/unless/some/evil/person/has/this/test/" + UUID.randomUUID().toString() + "/", "Random missing directory path with trailing slash should return false" },
                { "I/Do/Not/Exist/unless/some/evil/person/has/this/test/" + UUID.randomUUID().toString() + ".txt", "Random missing path with file extension should return false" },
                { "I/Do/Not/Exist/unless/some/evil/person/has/this/test/" + UUID.randomUUID().toString() + ".txt/", "Random missing path with file extension and trailing slash should return false" },
        };
    }
    
    
    @Test(dataProvider="testIRODSFileExistsReturnsFalseOnMissingFileProvider", priority=1)
    public void testIRODSFileExistsReturnsFalseOnMissingPath(String filename, String message)
    {
        try {
            
            Assert.assertFalse(getClient().doesExist(filename), "doesExist: " + message);
            
            Assert.assertFalse(getClient().getIRODSFile(filename).exists(), "IRODSFile#exists: " + message);
        }
        catch (Exception e) {
            Assert.fail("Exception should not be thrown on existence checks for missing paths", e);
        }
    }
    
    @Test(dataProvider="testIRODSFileExistsReturnsFalseOnMissingFileProvider", priority=2)
    public void testIRODSFileExistsReturnsTrueOnEmptyDirectories(String filename, String message)
    {
        try {
            // create the directory this time and all tests should return true
            Assert.assertTrue(getClient().mkdirs(filename), "Failed to created " + filename + " on remote system.");
        }
        catch (Exception e) {
            Assert.fail("Directory creation should not fail for valid paths", e);
        }
        
        try {
            
            Assert.assertTrue(getClient().doesExist(filename), "doesExist: should return true for valid empty folder");
            
            Assert.assertTrue(getClient().getIRODSFile(filename).exists(), "IRODSFile#exists(): should return true for valid empty folder");
        }
        catch (Exception e) {
            Assert.fail("Exception should not be thrown on existence checks for missing paths", e);
        }
        finally {
            try { 
                getClient().delete(filename); 
            } 
            catch (Exception e) { 
                Assert.fail("Failed to clean up after test", e); 
            }
        }
    }
}