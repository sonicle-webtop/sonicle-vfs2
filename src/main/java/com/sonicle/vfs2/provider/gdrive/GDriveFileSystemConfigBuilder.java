/*
 * cloud-services is a library developed by Sonicle S.r.l.
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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301 USA.
 *
 * You can contact Sonicle S.r.l. at email address sonicle@sonicle.com
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

package com.sonicle.vfs2.provider.gdrive;

import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemOptions;

/**
 *
 * @author malbinola
 */
public class GDriveFileSystemConfigBuilder extends FileSystemConfigBuilder {
	
	private static final GDriveFileSystemConfigBuilder INSTANCE =  new GDriveFileSystemConfigBuilder();
	
	public static final String APPLICATION_NAME = GDriveFileSystemConfigBuilder.class.getName() + ".APPLICATION_NAME";
	public static final String ACCESS_TOKEN = GDriveFileSystemConfigBuilder.class.getName() + ".ACCESS_TOKEN";
	public static final String USE_TRASH = GDriveFileSystemConfigBuilder.class.getName() + ".USE_TRASH";
	
	private GDriveFileSystemConfigBuilder() {
		super("gdrive.");
	}
	
	public static GDriveFileSystemConfigBuilder getInstance() {
		return INSTANCE;
	}

	@Override
	protected Class<? extends FileSystem> getConfigClass() {
		return GDriveFileSystem.class;
	}
	
	public String getApplicationName(FileSystemOptions fso) {
		return this.getString(fso, APPLICATION_NAME);
	}
	
	public void setApplicationName(FileSystemOptions fso, String value) {
		this.setParam(fso, APPLICATION_NAME, value);
	}
	
	public String getAccessToken(FileSystemOptions fso) {
		return this.getString(fso, ACCESS_TOKEN);
	}
	
	public void setAccessToken(FileSystemOptions fso, String value) {
		this.setParam(fso, ACCESS_TOKEN, value);
	}
	
	public Boolean getUseTrash(FileSystemOptions fso) {
		return this.getBoolean(fso, USE_TRASH, true);
	}
	
	public void setUseTrash(FileSystemOptions fso, boolean value) {
		this.setParam(fso, USE_TRASH, value);
	}
}
