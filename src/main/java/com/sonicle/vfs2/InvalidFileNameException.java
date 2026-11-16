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
package com.sonicle.vfs2;

import com.sonicle.vfs2.util.WindowsFileNames;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystemException;

/**
 * Thrown when a file or folder cannot be created, or renamed, because its name
 * breaks the Windows naming rules (see {@link WindowsFileNames}).
 * 
 * @author malbinola
 */
public class InvalidFileNameException extends FileSystemException {
	private static final String CODE = "vfs.sonicle/invalid-file-name.error";
	private final String invalidName;
	private final WindowsFileNames.Problem problem;
	
	public InvalidFileNameException(FileName fileName, String invalidName, WindowsFileNames.Problem problem) {
		super(CODE, fileName);
		this.invalidName = invalidName;
		this.problem = problem;
	}
	
	/**
	 * @return the rejected name component
	 */
	public String getInvalidName() {
		return invalidName;
	}
	
	/**
	 * @return why the name has been rejected
	 */
	public WindowsFileNames.Problem getProblem() {
		return problem;
	}
	
	@Override
	public String getMessage() {
		// The code is not in the commons-vfs2 bundle: build the message here
		return "Invalid file name \"" + invalidName + "\" (" + problem + ") in \"" + getInfo()[0] + "\"";
	}
}
