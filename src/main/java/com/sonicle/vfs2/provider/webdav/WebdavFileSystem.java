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

import java.net.URLStreamHandler;
import java.util.Collection;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.DefaultURLStreamHandler;
import org.apache.commons.vfs2.provider.GenericFileName;
import org.apache.commons.vfs2.provider.http.HttpFileSystem;

/**
 * A WebDAV file system.
 *
 * @since 2.0
 */
public class WebdavFileSystem extends HttpFileSystem {

	protected WebdavFileSystem(final GenericFileName rootName, final HttpClient client,
			final FileSystemOptions fileSystemOptions) {
		super(rootName, client, fileSystemOptions);
	}

	@Override
	protected HttpClient getClient() {
		// make accessible
		return super.getClient();
	}

	/**
	 * Returns the capabilities of this file system.
	 *
	 * @param caps The Capabilities to add.
	 */
	@Override
	protected void addCapabilities(final Collection<Capability> caps) {
		caps.addAll(WebdavFileProvider.capabilities);
	}

	/**
	 * Creates a file object. This method is called only if the requested file
	 * is not cached.
	 *
	 * @param name the FileName.
	 * @return The created FileObject.
	 */
	@Override
	protected FileObject createFile(final AbstractFileName name) {
		return new WebdavFileObject(name, this);
	}

	/**
	 * Return a URLStreamHandler.
	 *
	 * @return The URLStreamHandler.
	 */
	public URLStreamHandler getURLStreamHandler() {
		return new DefaultURLStreamHandler(getContext(), getFileSystemOptions());
	}
}
