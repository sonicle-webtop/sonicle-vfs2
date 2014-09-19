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

import java.io.DataInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.provider.AbstractRandomAccessStreamContent;
import org.apache.commons.vfs2.util.RandomAccessMode;

/**
 *
 * @author malbinola
 */
public class GDriveRandomAccessContent extends AbstractRandomAccessStreamContent {
	
	protected long filePointer = 0L;
	protected GDriveFileObject fileObject = null;
	protected DataInputStream dataInputStream;
	protected InputStream gdriveInputStream;
	
	GDriveRandomAccessContent(GDriveFileObject fileObject, RandomAccessMode mode) {
		super(mode);
		if(fileObject == null) throw new NullPointerException();
		this.fileObject = fileObject;
	}
	
	@Override
	protected DataInputStream getDataInputStream() throws IOException {
		if(dataInputStream != null) return dataInputStream;
		gdriveInputStream = fileObject.getInputStream();
		for(int i = 0; (long)i < filePointer; i++) gdriveInputStream.read();
		
		dataInputStream = new DataInputStream(new FilterInputStream(gdriveInputStream) {
			
			@Override
			public int read() throws IOException {
				int ret = super.read();
				if(ret > -1) filePointer++;
				return ret;
			}
			
			@Override
			public int read(byte b[]) throws IOException {
				int ret = super.read(b);
				if(ret > -1) filePointer += ret;
				return ret;
			}
			
			@Override
			public int read(byte b[], int off, int len) throws IOException {
				int ret = super.read(b, off, len);
				if(ret > -1) filePointer += ret;
				return ret;
			}
			
			@Override
			public void close() throws IOException {
				GDriveRandomAccessContent.this.close();
			}
		});
		return dataInputStream;
	}
	
	@Override
	public long getFilePointer() throws IOException {
		return filePointer;
	}
	
	@Override
	public void seek(long l) throws IOException {
		if(l == filePointer) return;
		if(l < 0L) throw new FileSystemException("Random access invalid position");
		if(dataInputStream != null) close();
		filePointer = l;
	}
	
	@Override
	public long length() throws IOException {
		return fileObject.getContent().getSize();
	}
	
	@Override
	public void close() throws IOException {
		if(dataInputStream != null) {
			gdriveInputStream.close();
			DataInputStream oldstream = dataInputStream;
			dataInputStream = null;
			oldstream.close();
			gdriveInputStream = null;
		}
	}
}
