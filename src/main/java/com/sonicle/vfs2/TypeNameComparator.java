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
package com.sonicle.vfs2;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;

/**
 *
 * @author malbinola
 */
public class TypeNameComparator extends NameComparator {
	private final Direction direction;
	
	public TypeNameComparator() {
		this(Direction.ASC);
	}
	
	public TypeNameComparator(Direction typeDirection) {
		super();
		this.direction = typeDirection;
	}
	
	public TypeNameComparator(Direction typeDirection, Direction nameDirection) {
		super(nameDirection);
		this.direction = typeDirection;
	}

	@Override
	public int compare(FileObject o1, FileObject o2) {
		int ret = compareType(o1, o2);
		if(ret != 0) return ret;
		return super.compare(o1, o2);
	}
	
	private int compareType(FileObject o1, FileObject o2) {
		try {
			if(o1.getType().equals(FileType.FOLDER) && o2.getType().equals(FileType.FILE)) {
				return direction.equals(Direction.ASC) ? -1 : 1;
			} else if(o1.getType().equals(FileType.FILE) && o2.getType().equals(FileType.FOLDER)) {
				return direction.equals(Direction.ASC) ? 1 : -1;
			} else {
				return 0;
			}
		} catch(FileSystemException ex) {
			return 0;
		}
	}
}
