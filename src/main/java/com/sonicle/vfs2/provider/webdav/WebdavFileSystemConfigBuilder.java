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

import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.http.HttpFileSystemConfigBuilder;

/**
 * Configuration options for WebDav.
 *
 * @since 2.0
 */
public final class WebdavFileSystemConfigBuilder extends HttpFileSystemConfigBuilder {

	private static final WebdavFileSystemConfigBuilder BUILDER = new WebdavFileSystemConfigBuilder();

	private static final boolean DEFAULT_FOLLOW_REDIRECT = false;

	private WebdavFileSystemConfigBuilder() {
		super("webdav.");
	}

	/**
	 * Gets the singleton builder.
	 *
	 * @return the singleton builder.
	 */
	public static HttpFileSystemConfigBuilder getInstance() {
		return BUILDER;
	}

	/**
	 * The user name to be associated with changes to the file.
	 *
	 * @param opts The FileSystem options
	 * @param creatorName The creator name to be associated with the file.
	 */
	public void setCreatorName(final FileSystemOptions opts, final String creatorName) {
		setParam(opts, "creatorName", creatorName);
	}

	/**
	 * Return the user name to be associated with changes to the file.
	 *
	 * @param opts The FileSystem options
	 * @return The creatorName.
	 */
	public String getCreatorName(final FileSystemOptions opts) {
		return getString(opts, "creatorName");
	}

	/**
	 * Gets whether to follow redirects for the connection.
	 *
	 * @param opts The FileSystem options.
	 * @return {@code true} to follow redirects, {@code false} not to.
	 * @see #setFollowRedirect
	 * @since 2.1
	 */
	@Override
	public boolean getFollowRedirect(final FileSystemOptions opts) {
		return getBoolean(opts, KEY_FOLLOW_REDIRECT, DEFAULT_FOLLOW_REDIRECT);
	}

	/**
	 * Whether to use versioning.
	 *
	 * @param opts The FileSystem options.
	 * @param versioning true if versioning should be enabled.
	 */
	public void setVersioning(final FileSystemOptions opts, final boolean versioning) {
		setParam(opts, "versioning", Boolean.valueOf(versioning));
	}

	/**
	 * The cookies to add to the request.
	 *
	 * @param opts The FileSystem options.
	 * @return true if versioning is enabled.
	 */
	public boolean isVersioning(final FileSystemOptions opts) {
		return getBoolean(opts, "versioning", false);
	}

	/**
	 * @return The Webdav FileSystem Class object.
	 */
	@Override
	protected Class<? extends FileSystem> getConfigClass() {
		return WebdavFileSystem.class;
	}
}
