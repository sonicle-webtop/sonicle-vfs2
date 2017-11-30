/*
 * Copyright (C) 2014 Sonicle S.r.l.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation with the addition of the following permission
 * added to Section 15 as permitted in Section 7(a): FOR ANY PART OF THE COVERED
 * WORK IN WHICH THE COPYRIGHT IS OWNED BY SONICLE, SONICLE DISCLAIMS THE
 * WARRANTY OF NON INFRINGEMENT OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301 USA.
 *
 * You can contact Sonicle S.r.l. at email address sonicle[at]sonicle.com
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License version 3.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public License
 * version 3, these Appropriate Legal Notices must retain the display of the
 * Sonicle logo and Sonicle copyright notice. If the display of the logo is not
 * reasonably feasible for technical reasons, the Appropriate Legal Notices must
 * display the words "Copyright (C) 2014 Sonicle S.r.l.".
 */
package com.sonicle.vfs2.provider.webdav;

import com.sonicle.vfs2.provider.webdavs.WebdavsFileProvider;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.provider.GenericFileName;
import org.apache.commons.vfs2.provider.http.HttpClientFactory;
import org.apache.commons.vfs2.provider.http.HttpFileProvider;
import org.apache.commons.vfs2.util.UserAuthenticatorUtils;

/**
 * A provider for WebDAV.
 *
 * @since 2.0
 */
public class WebdavFileProvider extends HttpFileProvider {

	/**
	 * The authenticator types used by the WebDAV provider.
	 *
	 * @deprecated Might be removed in the next major version.
	 */
	@Deprecated
	public static final UserAuthenticationData.Type[] AUTHENTICATOR_TYPES = new UserAuthenticationData.Type[]{
		UserAuthenticationData.USERNAME, UserAuthenticationData.PASSWORD};

	/**
	 * The capabilities of the WebDAV provider
	 */
	protected static final Collection<Capability> capabilities = Collections
			.unmodifiableCollection(Arrays.asList(new Capability[]{Capability.CREATE, Capability.DELETE,
		Capability.RENAME, Capability.GET_TYPE, Capability.LIST_CHILDREN, Capability.READ_CONTENT,
		Capability.URI, Capability.WRITE_CONTENT, Capability.GET_LAST_MODIFIED, Capability.ATTRIBUTES,
		Capability.RANDOM_ACCESS_READ, Capability.DIRECTORY_READ_CONTENT,}));
	
	/**
	 * Gets the proper physical URL scheme on logical WebDAV scheme. 'https' on 'webdavs', 'http' otherwise.
	 * @param name The FileName.
	 * @return proper physical URL scheme on logical WebDAV scheme. 'https' on 'webdavs', 'http' otherwise
	 */
	static String getURLScheme(final GenericFileName name) {
		return WebdavsFileProvider.SCHEME.equals(name.getScheme()) ? "https" : "http";
	}

	public WebdavFileProvider() {
		super();

		setFileNameParser(WebdavFileNameParser.getInstance());
	}

	/**
	 * Creates a {@link FileSystem}.
	 * <p>
	 * If you're looking at this method and wondering how to get a
	 * FileSystemOptions object bearing the proxy host and credentials
	 * configuration through to this method so it's used for resolving a
	 * {@link org.apache.commons.vfs2.FileObject FileObject} in the FileSystem,
	 * then be sure to use correct signature of the
	 * {@link org.apache.commons.vfs2.FileSystemManager FileSystemManager}
	 * resolveFile method.
	 *
	 * @see
	 * org.apache.commons.vfs2.impl.DefaultFileSystemManager#resolveFile(FileObject,
	 * String, FileSystemOptions)
	 */
	@Override
	protected FileSystem doCreateFileSystem(final FileName name, final FileSystemOptions fileSystemOptions)
			throws FileSystemException {
		// Create the file system
		final GenericFileName rootName = (GenericFileName) name;
		final FileSystemOptions fsOpts = fileSystemOptions == null ? new FileSystemOptions() : fileSystemOptions;

		UserAuthenticationData authData = null;
		HttpClient httpClient;
		try {
			authData = UserAuthenticatorUtils.authenticate(fsOpts, AUTHENTICATOR_TYPES);
			httpClient = HttpClientFactory.createConnection(WebdavFileSystemConfigBuilder.getInstance(),
					getURLScheme(rootName),
					rootName.getHostName(), rootName.getPort(),
					UserAuthenticatorUtils.toString(UserAuthenticatorUtils.getData(authData,
							UserAuthenticationData.USERNAME, UserAuthenticatorUtils.toChar(rootName.getUserName()))),
					UserAuthenticatorUtils.toString(UserAuthenticatorUtils.getData(authData,
							UserAuthenticationData.PASSWORD, UserAuthenticatorUtils.toChar(rootName.getPassword()))),
					fsOpts);
			
			final HostConfiguration hostConf = httpClient.getHostConfiguration();
			if (hostConf.getProtocol().isSecure()) {
				final String host = hostConf.getHost();
				final int port = hostConf.getPort();
				final Protocol proto = hostConf.getProtocol();
				hostConf.setHost(host, port, new Protocol(proto.getScheme(), new EasySSLProtocolSocketFactory(), proto.getDefaultPort()));
				httpClient.setHostConfiguration(hostConf);
			}
			
		} finally {
			UserAuthenticatorUtils.cleanup(authData);
		}

		return new WebdavFileSystem(rootName, httpClient, fsOpts);
	}

	@Override
	public FileSystemConfigBuilder getConfigBuilder() {
		return WebdavFileSystemConfigBuilder.getInstance();
	}

	@Override
	public Collection<Capability> getCapabilities() {
		return capabilities;
	}
}
