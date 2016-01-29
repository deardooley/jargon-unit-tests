package org.iplantc.service.transfer.irods4;

import java.util.ArrayList;
import java.util.List;
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
public class Irods4SpecialCharacterSupportTest extends IrodsBaseTestCase {
    
    @DataProvider
    protected Object[][] testSpecialCharacterCollectionNameSupportProvider() throws Exception {
        List<Object[]> tests = new ArrayList<Object[]>();
        for (char c: SPECIAL_CHARS.toCharArray()) {
            String sc = String.valueOf(c); 
            if (c == ' ' || c == '.') {
                continue;
            } else {
                tests.add(new Object[] { sc, "Directory name with single special character '" + sc + "' should be created" });
            }
            tests.add(new Object[] { sc + "leading", "Directory name with leading single special character '" + sc + "' should be created" });
            tests.add(new Object[] { "trailing" + sc, "Directory name with trailing single special character '" + sc + "' should be created" });
            tests.add(new Object[] { sc + "bookend" + sc, "Directory name with leading and trailing single special character '" + sc + "' should be created" });
            tests.add(new Object[] { "sand" + sc + "wich", "Directory name with singleinternal special character '" + sc + "' should be created" });
        }
        
        return tests.toArray(new Object[][] {});
    }
    
    @DataProvider
    protected Object[][] testRepeatedSpecialCharacterCollectionNameSupportProvider() throws Exception {
        List<Object[]> tests = new ArrayList<Object[]>();
        for (char c: SPECIAL_CHARS.toCharArray()) {
            String chars = String.valueOf(c) + String.valueOf(c);
            if (c == ' ' || c == '.') {
                continue;
            } else {
                tests.add(new Object[] { chars, "Directory name with only repeated special character '" + chars + "' should be created" });
            }
            tests.add(new Object[] { chars + "leading", "Directory name with repeated repeated special character '" + chars + "' should be created" });
            tests.add(new Object[] { "trailing" + chars, "Directory name with trailing repeated special character '" + chars + "' should be created" });
            tests.add(new Object[] { chars + "bookend" + chars, "Directory name with leading and trailing repeated special character '" + chars + "' should be created" });
            tests.add(new Object[] { "sand" + chars + "wich", "Directory name with repeated internal special character '" + chars + "' should be created" });
        }
        
        return tests.toArray(new Object[][] {});
    }

    @Test(dataProvider="testSpecialCharacterCollectionNameSupportProvider")
    public void testSpecialCharacterCollectionNameSupport(String filename, String message)
    throws java.io.FileNotFoundException
    {
        _testSpecialCharacterCollectionNameSupport(filename, message);
    }
    
    @Test(dataProvider="testSpecialCharacterCollectionNameSupportProvider")
    public void testSpecialCharacterCollectionAbsolutePathNameSupport(String filename, String message)
    throws java.io.FileNotFoundException
    {
        String absolutePath = "/" + getClass().getSimpleName() + "/thread-" + Thread.currentThread().getId() +  "/" + "absolute_path_test/";
        
        _testSpecialCharacterCollectionNameSupport(absolutePath + filename, message);
    }
    
    @Test(dataProvider="testRepeatedSpecialCharacterCollectionNameSupportProvider")
    public void testRepeatedSpecialCharacterCollectionNameSupport(String filename, String message)
    throws java.io.FileNotFoundException
    {
        _testSpecialCharacterCollectionNameSupport(filename, message);
    }
    
    @Test(dataProvider="testRepeatedSpecialCharacterCollectionNameSupportProvider")
    public void testRepeatedSpecialCharacterCollectionAbsolutePathNameSupport(String filename, String message)
    throws java.io.FileNotFoundException
    {
        String absolutePath = "/" + getClass().getSimpleName() + "/thread-" + Thread.currentThread().getId() +  "/" + "absolute_path_test/";
        
        _testSpecialCharacterCollectionNameSupport(absolutePath + filename, message);
    }
    
    protected void _testSpecialCharacterCollectionNameSupport(String filename, String message) 
    throws java.io.FileNotFoundException
    {
        try {
            Assert.assertTrue(getClient().mkdirs(filename), message);
            Assert.assertTrue(getClient().doesExist(filename), "mkdirs returned false positive when creating a collection with special characters in the name.");
        } 
        catch (Exception e) 
        {
            Assert.fail(message, e);
        }
        
        try { 
            getClient().delete(filename);
        } catch (Exception e) {
            Assert.fail("Unable to delete directory " + getClient().resolvePath(filename), e);
        }
    }
}