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

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.sonicle.vfs2.util.GDriveApiUtils;
import com.sonicle.vfs2.util.GDriveAppInfo;
import com.sonicle.vfs2.provider.gdrive.pool.GDriveClientInfo;
import com.sonicle.vfs2.provider.gdrive.pool.GDrivePool;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.GenericFileName;

/**
 *
 * @author malbinola
 */
public class GDriveClientWrapper {
	
	private GDriveClientInfo info = null;
	private Drive gdriveClient = null;
	
	private GDriveClientWrapper(GDriveClientInfo info) throws FileSystemException {
		
		try {
			this.info = info;
			this.gdriveClient = GDriveApiUtils.createClient(info.accessToken, new GDriveAppInfo(info.applicationName));
			About about = (About)this.gdriveClient.about().get().execute();
			about.getRootFolderId();
		} catch(Exception ex) {
			throw new FileSystemException(ex);
		}
	}
	
	public GDriveClientInfo getClientInfo() {
		return this.info;
	}
	
	public Drive getClient() {
		return this.gdriveClient;
	}
	
	public static GDriveClientWrapper createClientWrapper(GDriveClientInfo info) throws FileSystemException {
		return new GDriveClientWrapper(info);
	}
	
	public static GDriveClientWrapper getClientWrapper(GenericFileName rootName, FileSystemOptions fso) throws FileSystemException {
		GDriveFileSystemConfigBuilder builder = GDriveFileSystemConfigBuilder.getInstance();
		
		try {
			String token = rootName.getPassword();
			if(StringUtils.isEmpty(token)) token = builder.getAccessToken(fso);
			GDriveClientInfo k = new GDriveClientInfo(builder.getApplicationName(fso), token);
			return GDrivePool.getInstance().getPool().borrowObject(k);
		} catch(Exception ex) {
			throw new FileSystemException(ex);
		}
	}
	
	public static void releaseClientWrapper(GDriveClientWrapper wrapper) {
		GDrivePool.getInstance().getPool().returnObject(wrapper.getClientInfo(), wrapper);
	}
}
