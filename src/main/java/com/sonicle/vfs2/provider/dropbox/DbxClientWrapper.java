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
package com.sonicle.vfs2.provider.dropbox;

import com.dropbox.core.DbxClient;
import com.sonicle.vfs2.util.DropboxApiUtils;
import com.sonicle.vfs2.provider.dropbox.pool.DbxClientInfo;
import com.sonicle.vfs2.provider.dropbox.pool.DbxPool;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.GenericFileName;

/**
 *
 * @author malbinola
 */
public class DbxClientWrapper {
	private DbxClientInfo info = null;
	private DbxClient dropboxClient = null;
	
	private DbxClientWrapper(DbxClientInfo info) throws FileSystemException {
		
		try {
			this.info = info;
			this.dropboxClient = DropboxApiUtils.createClient(info.accessToken, info.clientIdentifier, "it_IT");
			this.dropboxClient.getMetadata("/");
		} catch(Exception ex) {
			throw new FileSystemException(ex);
		}
	}
	
	public DbxClientInfo getClientInfo() {
		return this.info;
	}
	
	public DbxClient getClient() {
		return this.dropboxClient;
	}
	
	public static DbxClientWrapper createClientWrapper(DbxClientInfo info) throws FileSystemException {
		return new DbxClientWrapper(info);
	}
	
	public static DbxClientWrapper getClientWrapper(GenericFileName rootName, FileSystemOptions fso) throws FileSystemException {
		DbxFileSystemConfigBuilder builder = DbxFileSystemConfigBuilder.getInstance();
		
		try {
			String token = rootName.getPassword();
			if(StringUtils.isEmpty(token)) token = builder.getAccessToken(fso);
			DbxClientInfo k = new DbxClientInfo(builder.getClientIdentifier(fso), token);
			return DbxPool.getInstance().getPool().borrowObject(k);
		} catch(Exception ex) {
			throw new FileSystemException(ex);
		}
	}
	
	public static void releaseClientWrapper(DbxClientWrapper wrapper) {
		DbxPool.getInstance().getPool().returnObject(wrapper.getClientInfo(), wrapper);
	}
}
