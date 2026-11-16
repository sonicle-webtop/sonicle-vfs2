/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sonicle.vfs2.provider.smb;

import com.sonicle.vfs2.InvalidFileNameException;
import com.sonicle.vfs2.util.WindowsFileNames;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import jcifs.CIFSContext;
import jcifs.smb.NtStatus;

import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.FileTypeHasNoContentException;
import org.apache.commons.vfs2.RandomAccessContent;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.commons.vfs2.util.RandomAccessMode;
import org.apache.commons.vfs2.util.UserAuthenticatorUtils;

/**
 * A file in an SMB file system.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class SmbFileObject
    extends AbstractFileObject
{
    // private final String fileName;
    private SmbFile file;
	// jcifs URL this object points to: the raw name, unless the name policy
    // maps it to its Windows form (see resolveUri)
    private String resolvedUri;

    protected SmbFileObject(final AbstractFileName name,
                            final SmbFileSystem fileSystem) throws FileSystemException
    {
        super(name, fileSystem);
        // this.fileName = UriParser.decode(name.getURI());
    }

    /**
     * Attaches this file object to its file resource.
     */
    @Override
    protected void doAttach() throws Exception
    {
        // Defer creation of the SmbFile to here
        if (file == null)
        {
            file = createSmbFile(getName());
        }
    }

    @Override
    protected void doDetach() throws Exception
    {
        // file closed through content-streams
        file = null;
		resolvedUri = null;
    }
	
	private SmbFileSystemConfigBuilder.NamePolicy getNamePolicy()
    {
        return ((SmbFileSystem) getFileSystem()).getNamePolicy();
    }
	
    private CIFSContext createAuthContext(SmbFileName smbFileName) throws FileSystemException
    {
        UserAuthenticationData authData = null;
        try
        {
            authData = UserAuthenticatorUtils.authenticate(getFileSystem().getFileSystemOptions(), SmbFileProvider.AUTHENTICATOR_TYPES);

            String domain = UserAuthenticatorUtils.toString(
                UserAuthenticatorUtils.getData(
                    authData,
                    UserAuthenticationData.DOMAIN,
                    UserAuthenticatorUtils.toChar(smbFileName.getDomain())));
            String username = UserAuthenticatorUtils.toString(
                UserAuthenticatorUtils.getData(
                    authData,
                    UserAuthenticationData.USERNAME,
                    UserAuthenticatorUtils.toChar(smbFileName.getUserName())));
            String password = UserAuthenticatorUtils.toString(
                UserAuthenticatorUtils.getData(
                    authData,
                    UserAuthenticationData.PASSWORD,
                    UserAuthenticatorUtils.toChar(smbFileName.getPassword())));

            // The credentials wrap the shared context: connections stay pooled per server
            CIFSContext base = ((SmbFileSystem) getFileSystem()).getCifsContext();
            if (username == null || username.isEmpty())
            {
                // Same as jcifs 1.3.17, which defaulted to the GUEST user
                return base.withGuestCrendentials(); // sic: typo in the jcifs 2.x API
            }
            else
            {
                return base.withCredentials(new NtlmPasswordAuthenticator(domain, username, password));
            }
        }
        finally
        {
            UserAuthenticatorUtils.cleanup(authData);
        }
    }
	
	private SmbFile createSmbFile(FileName fileName) throws MalformedURLException, SmbException, FileSystemException
    {
        SmbFileName smbFileName = (SmbFileName) fileName;
        CIFSContext auth = createAuthContext(smbFileName);
        String path = (fileName == getName()) ? getResolvedUri() : smbFileName.getUriWithoutAuth();
        return newSmbFile(path, auth);
    }
	
	private static SmbFile newSmbFile(String path, CIFSContext auth) throws MalformedURLException, SmbException
    {
        SmbFile file = new SmbFile(path, auth);
        if (file.isDirectory() && !file.toString().endsWith("/"))
        {
            file = new SmbFile(path + "/", auth);
        }
        return file;
    }
	
	/**
     * Returns the jcifs URL (without credentials) of this file: see {@link #resolveUri}.
     */
    String getResolvedUri() throws FileSystemException
    {
        synchronized (getFileSystem())
        {
            if (resolvedUri == null)
            {
                resolvedUri = resolveUri(false);
            }
            return resolvedUri;
        }
    }

    /**
     * Maps the name of this file to the jcifs URL it stands for.
     * <p>
     * With {@link SmbFileSystemConfigBuilder.NamePolicy#SANITIZE}, a path
     * component ending with dots or spaces stands for its Windows form
     * ("D.I." for "D.I"), unless a file with the raw name already exists: such
     * files, created by other clients, stay reachable (and deletable) here.
     * When the component is being created, or is the target of a rename
     * (<code>creating</code>), the Windows form is always used.
     * <p>
     * Each component is mapped by its own file object, so a child of a mapped
     * folder lands inside the mapped folder.
     */
    private String resolveUri(boolean creating) throws FileSystemException
    {
        final SmbFileName name = (SmbFileName) getName();
        final String rawUri = name.getUriWithoutAuth();
        if (getNamePolicy() != SmbFileSystemConfigBuilder.NamePolicy.SANITIZE)
        {
            return rawUri;
        }
        if (!WindowsFileNames.hasComponentWithTrailingDotOrSpace(name.getPathDecoded()))
        {
            // Nothing to map, anywhere in the path: no server round trip
            return rawUri;
        }

        final String leaf = getDecodedBaseName();
        final String strippedLeaf = WindowsFileNames.stripTrailingDotsAndSpaces(leaf);
        final String parentUri = getParentUri();
        if (strippedLeaf.equals(leaf) || strippedLeaf.isEmpty())
        {
            // Only some ancestor is mapped. An empty Windows form ("...") would
            // point to the parent itself, so it is never used: creating it fails.
            return parentUri + leaf;
        }
        if (!creating)
        {
            final String rawLeafUri = parentUri + leaf;
            try
            {
                if (new SmbFile(rawLeafUri, createAuthContext(name)).exists())
                {
                    return rawLeafUri;
                }
            }
            catch (MalformedURLException | SmbException e)
            {
                throw new FileSystemException("vfs.provider/get-type.error", getName(), e);
            }
        }
        return parentUri + strippedLeaf;
    }
	
	private String getParentUri() throws FileSystemException
    {
        final FileObject parent = getParent();
        final String uri;
        if (parent instanceof SmbFileObject)
        {
            uri = ((SmbFileObject) parent).getResolvedUri();
        }
        else
        {
            // Decorated file objects: the ancestors are taken as they are
            uri = ((SmbFileName) getName().getParent()).getUriWithoutAuth();
        }
        return uri.endsWith("/") ? uri : uri + "/";
    }

    private String getDecodedBaseName() throws FileSystemException
    {
        final String path = ((SmbFileName) getName()).getPathDecoded();
        return path.substring(path.lastIndexOf('/') + 1);
    }
	
	/**
     * Checks the name of this file before creating it (folder, new file,
     * rename target), according to the name policy. Only the last component
     * is checked: the missing ancestors are checked when VFS creates them,
     * the existing ones are used as they are.
     */
    private void checkNameForCreation() throws FileSystemException
    {
        final SmbFileSystemConfigBuilder.NamePolicy policy = getNamePolicy();
        if (policy == SmbFileSystemConfigBuilder.NamePolicy.NONE)
        {
            return;
        }
        final String leaf = getDecodedBaseName();
        final WindowsFileNames.Problem problem = WindowsFileNames.check(
            policy == SmbFileSystemConfigBuilder.NamePolicy.SANITIZE ? WindowsFileNames.stripTrailingDotsAndSpaces(leaf) : leaf);
        if (problem != null)
        {
            throw new InvalidFileNameException(getName(), leaf, problem);
        }
    }
	
	/**
     * Checks the name before creating this file and, with
     * {@link SmbFileSystemConfigBuilder.NamePolicy#SANITIZE}, points it to its
     * Windows form: it may still point to a raw name that existed when the
     * object was attached and has been deleted since.
     */
    private void prepareForCreation() throws Exception
    {
        checkNameForCreation();
        if (getNamePolicy() == SmbFileSystemConfigBuilder.NamePolicy.SANITIZE
            && WindowsFileNames.hasTrailingDotOrSpace(getDecodedBaseName()))
        {
            synchronized (getFileSystem())
            {
                final String uri = resolveUri(true);
                if (!uri.equals(resolvedUri))
                {
                    resolvedUri = uri;
                    file = newSmbFile(uri, createAuthContext((SmbFileName) getName()));
                }
            }
        }
    }
	
	/**
     * Same as {@link #prepareForCreation()}, for operations that may also
     * work on an existing file (writing, random access): an existing file
     * keeps working whatever its name, the check only applies if it is missing.
     */
    private void prepareIfMissing() throws Exception
    {
        if (getNamePolicy() == SmbFileSystemConfigBuilder.NamePolicy.NONE)
        {
            return;
        }
        if (WindowsFileNames.isValid(getDecodedBaseName()))
        {
            // No server round trip for valid names
            return;
        }
        if (!file.exists())
        {
            prepareForCreation();
        }
    }

    /**
     * Determines the type of the file, returns null if the file does not
     * exist.
     */
    @Override
    protected FileType doGetType() throws Exception
    {
        if (!file.exists())
        {
            return FileType.IMAGINARY;
        }
        else if (file.isDirectory())
        {
            return FileType.FOLDER;
        }
        else if (file.isFile())
        {
            return FileType.FILE;
        }

        throw new FileSystemException("vfs.provider.smb/get-type.error", getName());
    }

    /**
     * Lists the children of the file.  Is only called if {@link #doGetType}
     * returns {@link FileType#FOLDER}.
     */
    @Override
    protected String[] doListChildren() throws Exception
    {
        // VFS-210: do not try to get listing for anything else than directories
        if (!file.isDirectory())
        {
            return null;
        }

        return UriParser.encode(file.list());
    }

    /**
     * Determines if this file is hidden.
     */
    @Override
    protected boolean doIsHidden() throws Exception
    {
        return file.isHidden();
    }

    /**
     * Deletes the file.
     */
    @Override
    protected void doDelete() throws Exception
    {
        file.delete();
    }

    @Override
    protected void doRename(FileObject newfile) throws Exception
    {
		if (newfile instanceof SmbFileObject)
        {
            final SmbFileObject dest = (SmbFileObject) newfile;
            dest.checkNameForCreation();
            synchronized (getFileSystem())
            {
                // The target is always created with its Windows form, even
                // when the raw name existed (VFS deletes it before renaming)
                final String targetUri = dest.resolveUri(true);
                final SmbFile target = newSmbFile(targetUri, createAuthContext((SmbFileName) dest.getName()));
                file.renameTo(target);
                dest.resolvedUri = targetUri;
                if (dest.file != null)
                {
                    dest.file = target;
                }
            }
        }
        else
        {
            file.renameTo(createSmbFile(newfile.getName()));
        }
    }

    /**
     * Creates this file as a folder.
     */
    @Override
    protected void doCreateFolder() throws Exception
    {
		prepareForCreation();
        file.mkdir();
        file = createSmbFile(getName());
    }

    /**
     * Returns the size of the file content (in bytes).
     */
    @Override
    protected long doGetContentSize() throws Exception
    {
        return file.length();
    }

    /**
     * Returns the last modified time of this file.
     */
    @Override
    protected long doGetLastModifiedTime()
        throws Exception
    {
        return file.lastModified();
    }

    /**
     * Creates an input stream to read the file content from.
     */
    @Override
    protected InputStream doGetInputStream() throws Exception
    {
        try
        {
            return new SmbFileInputStream(file);
        }
        catch (SmbException e)
        {
            if (e.getNtStatus() == NtStatus.NT_STATUS_NO_SUCH_FILE
						|| e.getNtStatus() == NtStatus.NT_STATUS_OBJECT_NAME_NOT_FOUND)
            {
                throw new org.apache.commons.vfs2.FileNotFoundException(getName());
            }
            else if (file.isDirectory())
            {
                throw new FileTypeHasNoContentException(getName());
            }

            throw e;
        }
    }

    /**
     * Creates an output stream to write the file content to.
     */
    @Override
    protected OutputStream doGetOutputStream(boolean bAppend) throws Exception
    {
		prepareIfMissing();
        return new SmbFileOutputStream(file, bAppend);
    }

    /**
     * random access
     */
    @Override
    protected RandomAccessContent doGetRandomAccessContent(final RandomAccessMode mode) throws Exception
    {
		if (mode.requestWrite())
        {
            prepareIfMissing();
        }
        return new SmbFileRandomAccessContent(file, mode);
    }
}
