/* 
 * Copyright (C) 2026 Sonicle S.r.l.
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
 * display the words "Copyright (C) 2026 Sonicle S.r.l.".
 */
package com.sonicle.vfs2.provider.smb;

import com.sonicle.vfs2.util.WindowsFileNames;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemOptions;

/**
 * Options of the "smb" provider.
 * 
 * @author malbinola
 */
public class SmbFileSystemConfigBuilder extends FileSystemConfigBuilder {
	private static final SmbFileSystemConfigBuilder INSTANCE = new SmbFileSystemConfigBuilder();
	
	public static final String NAME_POLICY = SmbFileSystemConfigBuilder.class.getName() + ".NAME_POLICY";
	
	/**
	 * How names that break the Windows naming rules ({@link WindowsFileNames})
	 * are handled when a file or folder is created or renamed. Existing files
	 * are always reachable with their real name, whatever the policy: they can
	 * still be read, renamed and deleted.
	 */
	public enum NamePolicy {
		/**
		 * No check: names go to the server as they are (behaviour before the option existed).
		 */
		NONE,
		/**
		 * Every name breaking the Windows rules is refused with an
		 * {@link com.sonicle.vfs2.InvalidFileNameException}, trailing dots and spaces included.
		 */
		REJECT,
		/**
		 * Like Windows Explorer: trailing dots and spaces are silently stripped
		 * ("D.I." is created as "D.I"), the other invalid names (characters,
		 * reserved device names) are refused with an
		 * {@link com.sonicle.vfs2.InvalidFileNameException}.
		 */
		SANITIZE
	}
	
	public static final NamePolicy DEFAULT_NAME_POLICY = NamePolicy.SANITIZE;
	
	private SmbFileSystemConfigBuilder() {
		super("smb.");
	}
	
	public static SmbFileSystemConfigBuilder getInstance() {
		return INSTANCE;
	}
	
	@Override
	protected Class<? extends FileSystem> getConfigClass() {
		return SmbFileSystem.class;
	}
	
	/**
	 * @param fso the options
	 * @return the name policy, {@link #DEFAULT_NAME_POLICY} if not set
	 */
	public NamePolicy getNamePolicy(FileSystemOptions fso) {
		return this.getEnum(NamePolicy.class, fso, NAME_POLICY, DEFAULT_NAME_POLICY);
	}
	
	/**
	 * Sets how invalid Windows names are handled; set {@link NamePolicy#NONE}
	 * to go back to the old behaviour.
	 * @param fso the options
	 * @param value the policy, null for the default
	 */
	public void setNamePolicy(FileSystemOptions fso, NamePolicy value) {
		this.setParam(fso, NAME_POLICY, value);
	}
}
