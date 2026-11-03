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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.provider.AbstractOriginatingFileProvider;
import org.apache.commons.vfs2.provider.FileProvider;
import jcifs.CIFSContext;
import jcifs.CIFSException;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;

/**
 * A provider for SMB (Samba, Windows share) file systems, built on
 * org.codelibs:jcifs 2.1.x: it negotiates the highest dialect the server
 * supports, from SMB1 up to SMB 2.1 (jcifs defaults, see
 * jcifs.smb.client.minVersion / jcifs.smb.client.maxVersion).
 * <p>
 * Ported by Sonicle from the commons-vfs2 sandbox provider (jcifs 1.3.17,
 * SMB1 only), which it replaces.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class SmbFileProvider
    extends AbstractOriginatingFileProvider
    implements FileProvider
{
    protected final static Collection<Capability> capabilities = Collections.unmodifiableCollection(Arrays.asList(new Capability[]
    {
        Capability.CREATE,
        Capability.DELETE,
        Capability.RENAME,
        Capability.GET_TYPE,
        Capability.GET_LAST_MODIFIED,
        Capability.LIST_CHILDREN,
        Capability.READ_CONTENT,
        Capability.URI,
        Capability.WRITE_CONTENT,
        Capability.APPEND_CONTENT,
        Capability.RANDOM_ACCESS_READ,
        Capability.RANDOM_ACCESS_WRITE
    }));

    public final static UserAuthenticationData.Type[] AUTHENTICATOR_TYPES = new UserAuthenticationData.Type[]
        {
            UserAuthenticationData.USERNAME, UserAuthenticationData.PASSWORD, UserAuthenticationData.DOMAIN
        };

    private final Object contextLock = new Object();
    private CIFSContext context;

    public SmbFileProvider()
    {
        super();
        setFileNameParser(SmbFileNameParser.getInstance());
    }

    /**
     * Returns the jcifs context shared by all the file systems of this provider
     * (it holds the connection pool). It is private to the provider, not the
     * jcifs singleton, so it lives and dies with the VFS manager.
     */
    CIFSContext getCifsContext() throws FileSystemException
    {
        synchronized (contextLock)
        {
            if (context == null)
            {
                try
                {
                    context = new BaseContext(new PropertyConfiguration(new Properties()));
                }
                catch (CIFSException e)
                {
                    throw new FileSystemException(e);
                }
            }
            return context;
        }
    }

    @Override
    public void close()
    {
        super.close();
        synchronized (contextLock)
        {
            if (context != null)
            {
                try
                {
                    context.close();
                }
                catch (CIFSException e)
                {
                    // quiet close
                }
                context = null;
            }
        }
    }

    /**
     * Creates the filesystem.
     */
    @Override
    protected FileSystem doCreateFileSystem(final FileName name, final FileSystemOptions fileSystemOptions)
        throws FileSystemException
    {
        return new SmbFileSystem(name, fileSystemOptions, getCifsContext());
    }

    public Collection<Capability> getCapabilities()
    {
        return capabilities;
    }
}
