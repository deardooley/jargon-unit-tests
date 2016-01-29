package org.iplantc.service.transfer.irods4;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.util.FileUtils;
import org.ietf.jgss.GSSCredential;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.irods.jargon.core.connection.AuthScheme;
import org.irods.jargon.core.connection.GSIIRODSAccount;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.connection.IRODSProtocolManager;
import org.irods.jargon.core.connection.IRODSSession;
import org.irods.jargon.core.connection.IRODSSimpleProtocolManager;
import org.irods.jargon.core.exception.AuthenticationException;
import org.irods.jargon.core.exception.CatNoAccessException;
import org.irods.jargon.core.exception.DataNotFoundException;
import org.irods.jargon.core.exception.DuplicateDataException;
import org.irods.jargon.core.exception.FileNotFoundException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.exception.JargonRuntimeException;
import org.irods.jargon.core.pub.DataObjectAO;
import org.irods.jargon.core.pub.DataTransferOperations;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSAccessObjectFactoryImpl;
import org.irods.jargon.core.pub.IRODSFileSystemAO;
import org.irods.jargon.core.pub.domain.ObjStat;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry.ObjectType;
import org.irods.jargon.core.transfer.TransferStatus;
import org.irods.jargon.core.transfer.TransferStatusCallbackListener;

public class IRODS4 {
    /** integer from some queries signifying user can read a file */
    static final int READ_PERMISSIONS = 1050;
    /** integer from some queries signifying user can write to a file */
    static final int WRITE_PERMISSIONS = 1120;

    private IRODSProtocolManager irodsConnectionManager;
    private IRODSSession irodsSession;
    private IRODSAccount irodsAccount;
    private IRODSAccessObjectFactory accessObjectFactory;
    private DataTransferOperations dataTransferOperations;
    
    private DataObjectAO dataObjectAO;
    private IRODSFileSystemAO fileSystemAO;
    private IRODSFileFactory irodsFileFactory;

    protected AuthScheme type = AuthScheme.STANDARD;

    protected String host;
    protected int port;
    protected String username;
    protected String password;
    protected String resource;
    protected String zone;
    protected String rootDir;
    protected String homeDir;
    protected String internalUsername;
    protected GSSCredential credential;

    public IRODS4(String host, int port, String username, String password, String resource,
            String zone, String homeDir) 
    {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.resource = resource;
        this.zone = zone;
        this.rootDir = "/" + zone + "/home/" + username;

        updateSystemRoots(rootDir, homeDir);
    }
    
    public IRODS4(String host, int port, String username, String password, String resource,
            String zone, String homeDir, AuthScheme type) 
    {
        this(host, port, username, password, resource, zone, homeDir);
        this.type = type;
    }

    /**
     * Cleans up the virutal home and root paths adjusting for relative paths,
     * double slashes, etc.
     * 
     * @throws RemoteAuthenticationException
     */
    public void updateSystemRoots(String rootDir, String homeDir) {
        rootDir = FilenameUtils.normalize(rootDir);
        rootDir = StringUtils.stripEnd(rootDir, " ");
        if (!StringUtils.isEmpty(rootDir)) {
            this.rootDir = rootDir;
            if (!this.rootDir.endsWith("/")) {
                this.rootDir += "/";
            }
        } else {
            this.rootDir = "/";
        }

        homeDir = FilenameUtils.normalize(homeDir);
        if (!StringUtils.isEmpty(homeDir)) {
            this.homeDir = this.rootDir + homeDir;
            if (!this.homeDir.endsWith("/")) {
                this.homeDir += "/";
            }
        } else {
            this.homeDir = this.rootDir;
        }

        this.homeDir = StringUtils.stripEnd(this.homeDir.replaceAll("/+", "/"), " ");
        this.rootDir = StringUtils.stripEnd(this.rootDir.replaceAll("/+", "/"), " ");
    }

    /**
     * Authenticates to the remote system using the credentials from the
     * constructor.
     * This is safe to call repeatedly as the actual authentication call will
     * only
     * be made if there is not already a valid connection.
     * 
     * @throws RemoteAuthenticationException
     */
    public void authenticate() throws RemoteDataException {
        try {
            if (irodsSession == null || !irodsSession.currentConnection(irodsAccount).isConnected()) {
                irodsConnectionManager = IRODSSimpleProtocolManager.instance();
                irodsSession = new IRODSSession(irodsConnectionManager);
                irodsAccount = getAccount();
                accessObjectFactory = IRODSAccessObjectFactoryImpl.instance(irodsSession);

                String accountHomeDir = getDataObjectAO().getIRODSAccount().getHomeDirectory();
                // make a remote call to verify authentication success
                getIRODSFileFactory().instanceIRODSFile(homeDir);
            }
        } catch (AuthenticationException e) {
            throw new RemoteDataException("Failed to authenticate to remote server. "
                    + e.getMessage(), e);
        } catch (JargonException e) {
            if (e.getMessage().toLowerCase().contains("unable to start ssl socket")) {
                throw new RemoteDataException(
                        "Unable to validate SSL certificate on the IRODS server used for PAM authentication.",
                        e);
            } else
                if (e.getMessage().toLowerCase().contains("connection refused")) {
                    throw new RemoteDataException(
                            "Connection refused: Unable to contact IRODS server at " + host + ":"
                                    + port);
                } else {
                    throw new RemoteDataException("Failed to connect to remote server.", e);
                }
        } catch (Exception e) {
            throw new RemoteDataException("Failed to authenticate to remote server.", e);
        }
    }

    /**
     * Instance-level singleton for the {@link IRODSFileFactory} associated with
     * the current {@link AccessObjectFactory}.
     * 
     * @return shared instance of a {@link IRODSFileFactory} for this object.
     * @throws JargonException
     */
    private IRODSFileFactory getIRODSFileFactory() throws JargonException {
        if (irodsFileFactory == null) {
            irodsFileFactory = accessObjectFactory.getIRODSFileFactory(irodsAccount);
        }

        return irodsFileFactory;
    }

    /**
     * Instance-level singleton for the {@link DataObjectAO} associated with
     * the current {@link AccessObjectFactory}.
     * 
     * @return shared instance of a {@link IRODSFileFactory} for this object.
     * @throws JargonException
     */
    private DataObjectAO getDataObjectAO() throws JargonException
    {
        if (dataObjectAO == null) {
            dataObjectAO = accessObjectFactory.getDataObjectAO(irodsAccount);
        }
        
        return dataObjectAO;
    }
    
    /**
     * Instance-level singleton for the {@link DataTransferOperations} associated with
     * the current {@link AccessObjectFactory}.
     * 
     * @return shared instance of a {@link IRODSFileFactory} for this object.
     * @throws JargonException
     */
    private DataTransferOperations getDataTransferOperations() throws JargonException
    {
        if (dataTransferOperations == null) {
            dataTransferOperations = accessObjectFactory.getDataTransferOperations(irodsAccount);
        }
        
        return dataTransferOperations;
    }
    
    
    /**
     * Creates a new {@link IRODSAccount} using the connection parameters
     * provided in the constructor.
     * 
     * @return unverified {@link IRODSAccount} object with the proper
     *         {@link AuthScheme} configured.
     * @throws EncryptionException
     * @throws JargonException
     */
    private IRODSAccount getAccount() throws JargonException {
        IRODSAccount account = null;
        if (this.type == AuthScheme.GSI) {
            account = GSIIRODSAccount.instance(host, port, credential, resource);
            account.setZone(zone);
            account.setHomeDirectory(rootDir);
        } else {
            account = new IRODSAccount(host, port, username, password, rootDir, zone,
                    StringUtils.isEmpty(resource) ? "" : resource);

            if (this.type == null) {
                this.type = AuthScheme.STANDARD;
            }

            account.setAuthenticationScheme(type);

        }

        return account;
    }
    
    private IRODSFileSystemAO getIRODSFileSystemAO() throws JargonException
    {
        if (fileSystemAO == null) {
            fileSystemAO = accessObjectFactory.getIRODSFileSystemAO(irodsAccount);
        }
        
        return fileSystemAO;
    }
    
    /**
     * Perform a {@link IRODSFileSystemAO#getObjStat} on the {@code virtualPath}
     * .
     * Exceptions are swallowed and relayed as appropriate domain exceptions.
     * 
     * @param virtualPath
     *            the virtual path to the remote object or collection
     * @return a {@link ObjStat} for the requested {@code virtualPath}.
     * @throws FileNotFoundException
     *             if the path is invalid or the object does not exist
     * @throws RemoteDataException
     *             on permission or connectivity issues. will wrap
     *             {@link JargonException}
     */
    protected ObjStat stat(String virtualPath) throws IOException, RemoteDataException {
        String resolvedPath = StringUtils.removeEnd(resolvePath(virtualPath), "/");
        if (StringUtils.equals(virtualPath, "/") && StringUtils.isEmpty(resolvedPath)) {
            resolvedPath = "/";
        }

        try {
            return getIRODSFileSystemAO().getObjStat(resolvedPath);
        } catch (FileNotFoundException e) {
            throw new java.io.FileNotFoundException(
                    "File/folder does not exist or user lacks access permission");
        } catch (JargonException e) {
            if (e.getMessage().toLowerCase().contains("unable to start ssl socket")) {
                throw new RemoteDataException(
                        "Unable to validate SSL certificate on the IRODS server used for PAM authentication.",
                        e);
            } else
                if (e.getMessage().toLowerCase().contains("connection refused")) {
                    throw new RemoteDataException(
                            "Connection refused: Unable to contact IRODS server at " + host + ":"
                                    + port);
                } else {
                    throw new RemoteDataException("Failed to connect to remote server.", e);
                }
        }
    }

    /**
     * Resolves a virtualized system path provided by a user to an absolute
     * system path relative
     * to the {@link #rootDir} of this instance. Paths that do not begin with a
     * slash are treated as
     * relative paths and are resolved against the {@link #homeDir}.
     * 
     * @param virtualPath
     *            the virtual path to the remote object or collection
     * @return absolute path on the remote system of the provided virtual path
     * @throws FileNotFoundException
     */
    public String resolvePath(String virtualPath) 
    throws java.io.FileNotFoundException 
    {
        if (StringUtils.isEmpty(virtualPath)) {
            return StringUtils.stripEnd(homeDir, " ");
        } else
            if (virtualPath.startsWith("/")) {
                virtualPath = rootDir + virtualPath.replaceFirst("/", "");
            } else {
                virtualPath = homeDir + virtualPath;
            }

        String adjustedPath = virtualPath;
        if (adjustedPath.endsWith("/..") || adjustedPath.endsWith("/.")) {
            adjustedPath += File.separator;
        }

        if (adjustedPath.startsWith("/")) {
            virtualPath = FileUtils.normalize(adjustedPath);
        } else {
            virtualPath = FilenameUtils.normalize(adjustedPath);
        }

        if (virtualPath == null) {
            throw new java.io.FileNotFoundException(
                    "File/folder does not exist or user lacks access permission");
        } else
            if (!virtualPath.startsWith(rootDir)) {
                if (!virtualPath.equals(StringUtils.removeEnd(rootDir, "/"))) {
                    throw new java.io.FileNotFoundException(
                            "File/folder does not exist or user lacks access permission");
                }
            }

        // prune trailing slashes
        return StringUtils.stripEnd(virtualPath, " ");
    }

    /**
     * Check for existence of a file or folder. Exceptions are thrown for
     * permission
     * and connectivity issues. Otherwise boolean values are returned.
     * 
     * @param virtualPath
     *            the virtual path to the remote object or collection
     * @return true if the object or collection exists, false otherwise
     * @throws IOException
     *             if {@code virtualPath} is invalid
     * @throws RemoteDataException
     *             on permission or connectivity issues. will wrap
     *             {@link JargonException}
     */
    public boolean doesExist(String virtualPath) throws IOException, RemoteDataException {
        try {
            stat(virtualPath);
            return true;
        } catch (java.io.FileNotFoundException e) {
            return false;
        } catch (IOException | RemoteDataException e) {
            throw e;
        }
    }
    
    /**
     * 
     * @param virtualPath the virtual path to the remote object or collection
     * @return true if the {@code virtualPath} is a collection, false otherwise.
     * @throws IOException if the path is invalid or does not exist
     * @throws RemoteDataException
     */
    public boolean isDirectory(String virtualPath) 
    throws IOException, RemoteDataException
    {
        ObjStat stat = stat(virtualPath);
        
        // should be redundant. Anything other than a collection at the path will
        // throw a org.irods.jargon.core.exception.FileNotFoundException
        return stat.getObjectType() == ObjectType.COLLECTION || 
               stat.getObjectType() == ObjectType.LOCAL_DIR;
    }

    /**
     * Creates an instance of an {@link IRODSFile} at the given {@code virtualPath}.
     * Note that the
     * virutal path will be validated and resolved to an absolute path prior to
     * the {@link IRODSFile} instance being created.
     * 
     * @param virtualPath
     *            the virtual path to the remote object or collection
     * @return instance of an {@link IRODSFile}. The underlying object or
     *         collection is not guaranteed to exist.
     * @throws JargonException
     * @throws IOException
     *             if the path is invalid
     */
    protected IRODSFile getIRODSFile(String virtualPath) throws JargonException, IOException {
        String resolvedPath = resolvePath(virtualPath);
        return getIRODSFileFactory().instanceIRODSFile(resolvedPath);
    }

    /**
     * Creates the directory specified by <code>virtualPath</code>. If the
     * parent
     * does not exist, this will throw a {@link FileNotFoundException}. Use the
     * {@link #mkdirs} method in this situation. Note that depending
     * on the underlying implementation and remote system protocol, the
     * directory
     * may or may not be available for writing immediately. In the case of
     * Azure,
     * bucket creation can take up to 30 minutes.
     * 
     * @param virtualPath
     *            the virtual path to the remote object or collection
     * @return true on success, false if the directory already exists
     * @throws IOException
     * @throws RemoteDataException
     */
    public boolean mkdir(String virtualPath) throws IOException, RemoteDataException {
        IRODSFile file = null;

        try {
            file = getIRODSFile(virtualPath);
            getIRODSFileSystemAO().mkdir(file, false);
        } catch (DuplicateDataException e) {
            return false;
        } catch (CatNoAccessException e) {
            throw new RemoteDataException("Failed to create " + virtualPath
                    + " due to insufficient privileges.", e);
        } catch (DataNotFoundException e) {
            throw new java.io.FileNotFoundException("No such file or directory");
        } catch (JargonException e) {
            // check if this means that it already exists, and call that a
            // 'false' instead of an error
            if (e.getMessage().indexOf("-809000") > -1) {
                return false;
            }
            throw new RemoteDataException("Failed to create " + virtualPath, e);
        }

        return true;
    }

    /**
     * Creates the directory(ies) specified by <code>virtualPath</code>. Any
     * missing
     * directories are created automatically. Note that depending
     * on the underlying implementation and remote system protocol, the
     * directory
     * may or may not be available for writing immediately. In the case of
     * Azure,
     * bucket creation can take up to 30 minutes.
     * 
     * @param virtualPath
     *            the virtual path to the remote object or collection
     * @return true on success, false if the directory already exists
     * @throws IOException
     * @throws RemoteDataException
     */
    public boolean mkdirs(String virtualPath) throws IOException, RemoteDataException {
        IRODSFile file = null;

        try {
            file = getIRODSFile(virtualPath);
            getIRODSFileSystemAO().mkdir(file, true);
        } catch (DuplicateDataException e) {
            return false;
        } catch (CatNoAccessException e) {
            throw new RemoteDataException("Failed to create " + virtualPath
                    + " due to insufficient privileges.", e);
        } catch (DataNotFoundException e) {
            throw new java.io.FileNotFoundException("No such file or directory");
        } catch (JargonException e) {
            // check if this means that it already exists, and call that a
            // 'false' instead of an error
            if (e.getMessage().indexOf("-809000") > -1) {
                return false;
            }
            throw new RemoteDataException("Failed to create " + virtualPath, e);
        }

        return true;
    }

    /**
     * Deletes an object or collection at the {@code virtualPath}.
     * 
     * @param virtualPath
     *            to file on remote system
     * @throws IOException
     * @throws RemoteDataException
     */
    public void delete(String virtualPath) throws IOException, RemoteDataException {
        IRODSFile file = null;
        try {
            if (isDirectory(virtualPath)) {
                file = getIRODSFile(virtualPath);
                file.delete();
            } else {
                file = getIRODSFile(virtualPath);
                file.deleteWithForceOption();
            }
        } catch (IOException e) {
            throw e;
        } catch (RemoteDataException e) {
            throw e;
        } catch (JargonRuntimeException | CatNoAccessException e) {
            throw new RemoteDataException("Failed to delete " + virtualPath
                    + " due to insufficient privileges.", e);
        } catch (JargonException e) {
            throw new RemoteDataException("Failed to connect to remote server.", e);
        }
    }
    
    /**
     * Uploads a local {@link File} to a {@code virtualParentPath}.
     * @param localFile the file to upload
     * @param virtualParentPath the directory into which the upload will be copied.
     * @throws IOException
     * @throws RemoteDataException
     */
    public void put(File localFile, String virtualParentPath) throws IOException, RemoteDataException {
        
        IRODSFile destFile;
        try {
            destFile = getIRODSFile(virtualParentPath);
            
            if (!doesExist(virtualParentPath)) {
                mkdirs(virtualParentPath);
            }
            
            DefaultTransferStatusCallbackListener listener = new DefaultTransferStatusCallbackListener();
            
            getDataTransferOperations().putOperation(localFile, destFile, listener, null);
            
            if (listener.hasErrors()) {
                throw listener.getTransferError();
            }
        }
        catch (CatNoAccessException e) {
            throw new RemoteDataException("Failed to put " + localFile.getAbsolutePath() + " due to insufficient privileges.", e);
        } 
        catch (FileNotFoundException | DataNotFoundException e) {
            throw new java.io.FileNotFoundException("File/folder does not exist or user lacks access permission");
        } 
        catch (RemoteDataException | IOException e) {
            throw e;
        }
        catch (Throwable e) {
            throw new RemoteDataException("Failed to transfer file to irods.", e);
        }
    }
    
    /**
     * Close connection and invalidate session.
     */
    public void disconnect()
    {
        try {
            accessObjectFactory.closeSessionAndEatExceptions();
            irodsSession = null;
            accessObjectFactory = null;
        } catch (Exception e) {}
    }
}