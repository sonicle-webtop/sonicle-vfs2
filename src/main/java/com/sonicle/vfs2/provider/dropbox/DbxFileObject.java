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

import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxWriteMode;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.NameScope;
import org.apache.commons.vfs2.RandomAccessContent;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.commons.vfs2.util.MonitorInputStream;
import org.apache.commons.vfs2.util.MonitorOutputStream;
import org.apache.commons.vfs2.util.RandomAccessMode;

/**
 *
 * @author malbinola
 */
public class DbxFileObject extends AbstractFileObject {
	public static final DateFormat DATE_FORMAT_LAST_MODIFIED = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss zzz");
	protected final DbxFileSystem fileSystem;
	protected String relPath;
	protected boolean inRefresh = false;
	protected DbxEntry dbxEntry = null;
	protected Properties props = null;
	protected Map attributes = null;
	
	public DbxFileObject(AbstractFileName name, DbxFileSystem fs) throws FileSystemException {
		super(name, fs);
		this.fileSystem = fs;
		this.relPath = UriParser.decode(fs.getRootName().getRelativeName(name));
		if(relPath.equals(".")) {
			relPath = "/";
		} else if(!relPath.startsWith("/")) {
			relPath = (new StringBuilder()).append('/').append(relPath).toString();
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if(attributes != null) attributes.clear();
	}
	
	@Override
	protected void doDetach() throws Exception {
	}
	
	@Override
	public void refresh() throws FileSystemException {
		if(inRefresh) return;
		try {
			inRefresh = true;
			super.refresh();
			inRefresh = false;
		} catch(FileSystemException ex) {
			inRefresh = false;
			throw ex;
		}
	}
	
	@Override
	protected FileType doGetType() throws Exception {
		statSelf();
		if(dbxEntry != null) {
			if(dbxEntry.isFile()) return FileType.FILE;
			if(dbxEntry.isFolder()) return FileType.FOLDER;
		}
		FileType type = FileType.IMAGINARY;
		try {
			Field field = type.getClass().getDeclaredField("hasAttrs");
			field.setAccessible(true);
			field.set(type, true);
		} catch(Exception ex) { /* Do nothing... */ }
		return type;
	}
	
	private void statSelf() throws Exception {
		boolean invalid = false;
		DbxClientWrapper wrapper = null;
		if(dbxEntry != null) return;
		
		try {
			wrapper = fileSystem.getClientWrapper();
			dbxEntry = wrapper.getClient().getMetadata(relPath);
			if(dbxEntry != null) {
				props = DbxFileSystemConfigBuilder.convertFromDbxEntry(dbxEntry);
			} else {
				props = null;
			}
			
		} catch (DbxException.InvalidAccessToken ex) {
			throw ex;
		} catch (DbxException ex) {
			dbxEntry = null;
			props = null;
		} catch (IOException ex) {
			throw ex;
		} finally {
			fileSystem.putClientWrapper(wrapper);
		}
	}
	
	@Override
	protected void onChange() throws Exception {
		dbxEntry = null;
		statSelf();
	}
	
	@Override
	protected void doCreateFolder() throws Exception {
		DbxClientWrapper wrapper = null;
		try {
			statSelf();
			wrapper = fileSystem.getClientWrapper();
			wrapper.getClient().createFolder(relPath);
			
		} catch (DbxException.InvalidAccessToken ex) {
			throw ex;
		} catch(Exception ex) {
			throw ex;
		} finally {
			fileSystem.putClientWrapper(wrapper);
		}
	}
	
	@Override
	protected long doGetLastModifiedTime() throws Exception {
		statSelf();
		if(props != null) {
			try {
				String modtime = props.getProperty("clientMtime");
				if(modtime != null) return DATE_FORMAT_LAST_MODIFIED.parse(modtime).getTime();
			} catch(Exception ex) { /* Do nothing... */ }
		}
		throw new FileSystemException("Unknown modified time");
	}
	
	@Override
	protected boolean doSetLastModifiedTime(long modtime) {
		return false;
	}
	
	@Override
	protected void doDelete() throws Exception {
		DbxClientWrapper wrapper = null;
		try {
			statSelf();
			wrapper = fileSystem.getClientWrapper();
			wrapper.getClient().delete(relPath);
			
		} catch (DbxException.InvalidAccessToken ex) {
			throw ex;
		} catch (Exception ex) {
			throw ex;
		} finally {
			fileSystem.putClientWrapper(wrapper);
		}
	}
	
	@Override
	protected void doRename(FileObject newfile) throws Exception {
		DbxClientWrapper wrapper = null;
		try {
			statSelf();
			wrapper = fileSystem.getClientWrapper();
			wrapper.getClient().move(relPath, ((DbxFileObject)newfile).relPath);
			
		} catch (DbxException.InvalidAccessToken ex) {
			throw ex;
		} catch (DbxException ex) {
			String msg = ex.getMessage();
			if(!(ex instanceof com.dropbox.core.DbxException.BadResponse) || !StringUtils.contains(msg, ".*not expecting \"hash\" field.*")) throw ex;
		} catch (Exception ex) {
			throw ex;
		} finally {
			fileSystem.putClientWrapper(wrapper);
		}
	}
	
	@Override
	protected FileObject[] doListChildrenResolved() throws Exception {
		FileObject array[] = null;
		FileSystemManager fsmanager = null;
		DbxClientWrapper wrapper = null;
		try {
			fsmanager = getFileSystem().getFileSystemManager();
			wrapper = fileSystem.getClientWrapper();
			statSelf();
			com.dropbox.core.DbxEntry.WithChildren listing = wrapper.getClient().getMetadataWithChildren(relPath);
			int size = listing.children.size();
			array = new FileObject[size];
			for(int i = 0; i < size; i++) {
				DbxEntry child = (DbxEntry)listing.children.get(i);
				array[i] = fileSystem.resolveFile(fsmanager.resolveName(getName(), UriParser.encode(child.path), NameScope.CHILD));
			}
			
		} catch (DbxException.InvalidAccessToken ex) {
			throw ex;
		} catch (Exception ex) {
			throw ex;
		} finally {
			fileSystem.putClientWrapper(wrapper);
		}
		return array;
	}
	
	@Override
	protected String[] doListChildren() throws Exception {
		FileObject childs[] = doListChildrenResolved();
		String result[] = new String[childs.length];
		for(int i = 0; i < childs.length; i++) {
			result[i] = String.valueOf(childs[i]);
		}
		return result;
	}

	@Override
	protected long doGetContentSize() throws Exception {
		statSelf();
		if(props != null) {
			try {
				return Long.parseLong(props.getProperty("numBytes"));
			} catch(Exception ex) {
				throw new FileSystemException("Unknown size", ex);
			}
		} else {
			return -1L;
		}
	}
	
	@Override
	protected RandomAccessContent doGetRandomAccessContent(RandomAccessMode mode) throws Exception {
		return new DbxRandomAccessContent(this, mode);
	}	

	@Override
	protected InputStream doGetInputStream() throws Exception {
		statSelf();
		if(!getType().hasContent()) {
			throw new FileSystemException("Read not file");
		}
		DbxClientWrapper wrapper = fileSystem.getClientWrapper();
		synchronized(fileSystem) {
			com.dropbox.core.DbxClient.Downloader downloader = wrapper.getClient().startGetFile(relPath, null);
			return new DbxInputStream(wrapper, downloader, downloader.body);
		}
	}
	
	@Override
	protected OutputStream doGetOutputStream(boolean append) throws Exception {
		statSelf();
		DbxClientWrapper wrapper = fileSystem.getClientWrapper();
		synchronized(fileSystem) {
			FileSystemOptions fso = fileSystem.getFileSystemOptions();
			Integer chunkSize = DbxFileSystemConfigBuilder.getInstance().getChunkSize(fso);
			long len = -1L;
			try {
				len = Long.parseLong(String.valueOf(super.getContent().getAttribute("Content-Length")));
			} catch(Exception ex) { /* Do nothing... */ }
			com.dropbox.core.DbxClient.Uploader uploader = wrapper.getClient().startUploadFileChunked(chunkSize, relPath, DbxWriteMode.force(), len);
			return new DbxOutputStream(wrapper, uploader, uploader.getBody());
		}
	}
	
	@Override
	protected Map doGetAttributes() throws Exception {
		synchronized(this) {
			if(attributes == null) attributes = new HashMap(0);
			return attributes;
		}
	}
	
	@Override
	protected void doSetAttribute(String attrName, Object value) throws Exception {
		doGetAttributes();
		attributes.put(attrName, value);
	}
	
	private class DbxInputStream extends MonitorInputStream {
		
		private DbxClientWrapper wrapper = null;
		private com.dropbox.core.DbxClient.Downloader downloader = null;

		public DbxInputStream(DbxClientWrapper wrapper, com.dropbox.core.DbxClient.Downloader downloader, InputStream in) {
			super(in);
			this.wrapper = wrapper;
			this.downloader = downloader;
		}
		
		@Override
		protected void onClose() throws IOException {
			try {
				downloader.close();
				
			} catch(Exception ex) {
				/* Do nothing... */ 
			} finally {
				fileSystem.putClientWrapper(wrapper);
			}
		}
	}
	
	private class DbxOutputStream extends MonitorOutputStream {
		
		private DbxClientWrapper wrapper = null;
		private com.dropbox.core.DbxClient.Uploader uploader = null;
		
		public DbxOutputStream(DbxClientWrapper wrapper, com.dropbox.core.DbxClient.Uploader uploader, OutputStream out) {
			super(out);
			this.wrapper = wrapper;
			this.uploader = uploader;
		}
		
		@Override
		protected void onClose() throws IOException {
			try {
				uploader.finish();
				uploader.close();
				
			} catch (DbxException.InvalidAccessToken ex) {
				throw new IOException(ex);
			} catch(DbxException ex) {
				throw new IOException(ex);
			} catch(Exception ex) {
				/* Do nothing... */ 
			} finally {
				fileSystem.putClientWrapper(wrapper);
			}
		}
	}
}
