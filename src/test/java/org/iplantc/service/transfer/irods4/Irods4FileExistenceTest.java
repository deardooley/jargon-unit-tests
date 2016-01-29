package org.iplantc.service.transfer.irods4;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test class for Jargon behavior against IRODS4 instances. These tests were run 
 * against the {@code agaveapi/irods:4.0.3} Docker image used for integration 
 * testing by the Agave Platform.
 *
 * @author Rion Dooley <dooley@tacc.utexas.edu>
 */
@Test(singleThreaded=true)
public class Irods4FileExistenceTest extends IrodsBaseTestCase {
    
    @Test(priority=1)
    public void testIRODSFileExistsReturnsTrueForUploadedFile()
    {
        File localFile = stageTestFile();
        
        try {
            Assert.assertTrue(getClient().doesExist(localFile.getName()), "doesExist: should return true for uploaded file");
            
            IRODSFile irodsFile = getClient().getIRODSFile(localFile.getName());
            
            Assert.assertTrue(irodsFile.exists(), "IRODSFile#exists should return true for uploaded file");
        }
        catch (Exception e) {
            Assert.fail("Exception should not be thrown on existence checks for valid files and collections.", e);
        }
        finally {
            try { getClient().delete(localFile.getName()); } catch (Exception e) {}
            FileUtils.deleteQuietly(localFile);
        }
    }
    
    @Test(priority=1)
    public void testIRODSFileExistsReturnsFalseAfterFileIsDeletedNotUsingIRODSFileDelete()
    {
        File localFile = stageTestFile();
        
        try {
            IRODSFile irodsFile = getClient().getIRODSFile(localFile.getName());
            
            Assert.assertTrue(irodsFile.exists(), "IRODSFile#exists should return true for uploaded file");
            
            getClient().delete(localFile.getName());
            
            Assert.assertFalse(irodsFile.exists(), "IRODSFile#exists same instance should return false after deletion");
        }
        catch (Exception e) {
            Assert.fail("Exception should not be thrown on existence checks for valid files and collections.", e);
        }
        finally {
            try { getClient().delete(localFile.getName()); } catch (Exception e) {}
            FileUtils.deleteQuietly(localFile);
        }
    }
    
    @Test(priority=1)
    public void testIRODSFileExistsReturnsFalseFromNewInstanceAfterFileIsDeletedNotUsingIRODSFileDelete()
    {
        File localFile = stageTestFile();
        
        try {
            IRODSFile irodsFile = getClient().getIRODSFile(localFile.getName());
            
            Assert.assertTrue(irodsFile.exists(), "IRODSFile#exists should return true for uploaded file");
            
            getClient().delete(localFile.getName());
            
            irodsFile = getClient().getIRODSFile(localFile.getName());
            
            Assert.assertFalse(irodsFile.exists(), "IRODSFile#exists new instance should return false after deletion");
        }
        catch (Exception e) {
            Assert.fail("Exception should not be thrown on existence checks for valid files and collections.", e);
        }
        finally {
            try { getClient().delete(localFile.getName()); } catch (Exception e) {}
            FileUtils.deleteQuietly(localFile);
        }
    }
    
    @Test(priority=1)
    public void testIRODSFileExistsReturnsFalseAfterFileIsDeletedUsingIRODSFileDelete()
    {
        File localFile = stageTestFile();
        
        try {
            IRODSFile irodsFile = getClient().getIRODSFile(localFile.getName());
            
            Assert.assertTrue(irodsFile.exists(), "IRODSFile#exists should return true for uploaded file");
            
            irodsFile.deleteWithForceOption();
            
            Assert.assertFalse(irodsFile.exists(), "IRODSFile#exists same instance should return false after deletion");
        }
        catch (Exception e) {
            Assert.fail("Exception should not be thrown on existence checks for valid files and collections.", e);
        }
        finally {
            try { getClient().delete(localFile.getName()); } catch (Exception e) {}
            FileUtils.deleteQuietly(localFile);
        }
    }
    
    @Test(priority=1)
    public void testIRODSFileExistsReturnsFalseFromNewInstanceAfterFileIsDeletedUsingIRODSFileDelete()
    {
        File localFile = stageTestFile();
        
        try {
            IRODSFile irodsFile = getClient().getIRODSFile(localFile.getName());
            
            Assert.assertTrue(irodsFile.exists(), "IRODSFile#exists should return true for uploaded file");
            
            irodsFile.deleteWithForceOption();
            
            irodsFile = getClient().getIRODSFile(localFile.getName());
            
            Assert.assertFalse(irodsFile.exists(), "IRODSFile#exists new instance should return false after deletion");
        }
        catch (Exception e) {
            Assert.fail("Exception should not be thrown on existence checks for valid files and collections.", e);
        }
        finally {
            try { getClient().delete(localFile.getName()); } catch (Exception e) {}
            FileUtils.deleteQuietly(localFile);
        }
    }
    
    @Test(priority=1)
    public void testDoesExistReturnsFalseAfterFileIsDeletedNotUsingIRODSFileDelete()
    {
        File localFile = stageTestFile();
        
        try {
            Assert.assertTrue(getClient().doesExist(localFile.getName()), "doesExist: should return true for uploaded file");
            
            getClient().delete(localFile.getName());
            
            Assert.assertFalse(getClient().doesExist(localFile.getName()), "doesExist: should return false for uploaded file after deletion");
        }
        catch (Exception e) {
            Assert.fail("Exception should not be thrown on existence checks for valid files and collections.", e);
        }
        finally {
            try { getClient().delete(localFile.getName()); } catch (Exception e) {}
            FileUtils.deleteQuietly(localFile);
        }
    }
    
    @Test(priority=1)
    public void testDoesExistReturnsFalseAfterFileIsDeletedUsingIRODSFileDelete()
    {
        File localFile = stageTestFile();
        
        try {
            Assert.assertTrue(getClient().doesExist(localFile.getName()), "doesExist: should return true for uploaded file");
            
            IRODSFile irodsFile = getClient().getIRODSFile(localFile.getName());
            
            irodsFile.deleteWithForceOption();
            
            Assert.assertFalse(getClient().doesExist(localFile.getName()), "doesExist: should return false for uploaded file after deletion");
        }
        catch (Exception e) {
            Assert.fail("Exception should not be thrown on existence checks for valid files and collections.", e);
        }
        finally {
            try { getClient().delete(localFile.getName()); } catch (Exception e) {}
            FileUtils.deleteQuietly(localFile);
        }
    }
    
}