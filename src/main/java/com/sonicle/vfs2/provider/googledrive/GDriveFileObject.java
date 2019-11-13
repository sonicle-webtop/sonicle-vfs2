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
package com.sonicle.vfs2.provider.googledrive;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class GDriveFileObject extends AbstractFileObject {
	protected final GDriveFileSystem fileSystem;
	protected String relPath;
	protected boolean inRefresh = false;
	protected File gdriveFile = null;
	protected String rootID = null;
	protected Map attributes = null;
	
	public GDriveFileObject(AbstractFileName name, GDriveFileSystem fs) throws FileSystemException {
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
	
	private String getGooglePath() {
		return (relPath.equals("/")) ? "" : relPath.substring(1);
	}
	
	private String getGoogleTitle() {
		return (relPath.equals("/")) ? "root" : relPath.substring(relPath.lastIndexOf('/') + 1);
	}
	
	private boolean isInRoot() {
		return (relPath.equals("/")) ? true : relPath.lastIndexOf('/') == 0;
	}
	
	private String getRootID() {
		if(rootID == null) {
			try {
				GDriveClientWrapper wrapper = fileSystem.getClientWrapper();
				About about = (About)wrapper.getClient().about().get().execute();
				rootID = about.getRootFolderId();
			} catch(Exception ex) { /* Do nothing... */ }
		}
		return rootID;
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
		if(gdriveFile != null) {
			if(gdriveFile.getMimeType().equals("application/vnd.google-apps.folder")) {
				return FileType.FOLDER;
			} else {
				return FileType.FILE;
			}
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
		GDriveClientWrapper wrapper = null;
		if(gdriveFile != null) return;
		
		try {
			wrapper = fileSystem.getClientWrapper();
			if(relPath.equals("/")) {
				Drive.Files.Get request = wrapper.getClient().files().get(getRootID());
				gdriveFile = (File)request.execute();
			} else {
				String parentId = getRootID();
				String paths[] = getGooglePath().split("([/])");
				for(int i = 0; i < paths.length; i++) {
					String next = paths[i];
					Drive.Files.List request = wrapper.getClient().files().list();
					request.setQ((new StringBuilder()).append("title='").append(next).append("' AND trashed=false AND '").append(parentId).append("' in parents").toString());
					List list = ((FileList)request.execute()).getItems();
					if(list.size() != 1) break;
					if(i == paths.length - 1) {
						gdriveFile = (File)list.get(0);
					} else {
						parentId = ((File)list.get(0)).getId();
					}
				}
			}
			
		} /*catch (DbxException ex) {
			if(ex.getClass().getName().equals("com.dropbox.core.DbxException$InvalidAccessToken")) ex.printStackTrace();
			gdriveFile = null;
		}*/ catch (IOException ex) {
			throw ex;
		} finally {
			fileSystem.putClientWrapper(wrapper);
		}
	}
	
	@Override
	protected void onChange() throws Exception {
		gdriveFile = null;
		statSelf();
	}
	
	@Override
	protected void doCreateFolder() throws Exception {
		GDriveClientWrapper wrapper = null;
		try {
			statSelf();
			wrapper = fileSystem.getClientWrapper();
			File file = doCreateEntry(wrapper, "application/vnd.google-apps.folder");
			
		} catch(Exception ex) {
			throw ex;
		} finally {
			fileSystem.putClientWrapper(wrapper);
		}
	}
	
	@Override
	protected long doGetLastModifiedTime() throws Exception {
		statSelf();
		try {
			DateTime modtime = gdriveFile.getModifiedDate();
			if(modtime != null) return modtime.getValue();
			
		} catch(Exception ex) { /* Do nothing... */ }
		throw new FileSystemException("Unknown modified time");
	}
	
	@Override
	protected boolean doSetLastModifiedTime(long modtime) throws Exception {
		statSelf();
		try {
			gdriveFile.setModifiedDate(new DateTime(modtime));
			return true;
			
		} catch(Exception ex) { /* Do nothing... */ }
		throw new FileSystemException("Error setting modified time");
	}
	
	@Override
	protected void doDelete() throws Exception {
		GDriveClientWrapper wrapper = null;
		try {
			statSelf();
			wrapper = fileSystem.getClientWrapper();
			FileSystemOptions fso = fileSystem.getFileSystemOptions();
			boolean useTrash = GDriveFileSystemConfigBuilder.getInstance().getUseTrash(fso);
			if(useTrash) {
				Drive.Files.Trash request = wrapper.getClient().files().trash(gdriveFile.getId());
				request.execute();
			} else {
				Drive.Files.Delete request = wrapper.getClient().files().delete(gdriveFile.getId());
				request.execute();
			}
			
		} catch (Exception ex) {
			throw ex;
		} finally {
			fileSystem.putClientWrapper(wrapper);
		}
	}
	
	@Override
	protected void doRename(FileObject newfile) throws Exception {
		GDriveClientWrapper wrapper = null;
		try {
			statSelf();
			wrapper = fileSystem.getClientWrapper();
			File file = new File();
			file.setTitle(newfile.getName().getBaseName());
			Drive.Files.Patch request = wrapper.getClient().files().patch(gdriveFile.getId(), file);
			request.setFields("title");
			request.execute();
			
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
		GDriveClientWrapper wrapper = null;
		try {
			fsmanager = getFileSystem().getFileSystemManager();
			wrapper = fileSystem.getClientWrapper();
			statSelf();
			Drive.Files.List request = wrapper.getClient().files().list();
			if(relPath.equals("/")) {
				request.setQ((new StringBuilder()).append("'").append(getRootID()).append("' in parents AND trashed=false").toString());
			} else {
				request.setQ((new StringBuilder()).append("'").append(gdriveFile.getId()).append("' in parents AND trashed=false").toString());
			}
			FileList filelist = (FileList)request.execute();
			List list = filelist.getItems();
			array = new FileObject[list.size()];
			for(int i = 0; i < list.size(); i++) {
				File child = (File)list.get(i);
				array[i] = fileSystem.resolveFile(fsmanager.resolveName(getName(), UriParser.encode(child.getTitle()).replace(":", "%3A"), NameScope.CHILD));
				((GDriveFileObject)array[i]).gdriveFile = child;
			}
			
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
		try {
			return gdriveFile.getFileSize();

		} catch(Exception ex) {
			throw new FileSystemException("Unknown size", ex);
		}
	}
	
	@Override
	protected RandomAccessContent doGetRandomAccessContent(RandomAccessMode mode) throws Exception {
		return new GDriveRandomAccessContent(this, mode);
	}

	@Override
	protected InputStream doGetInputStream() throws Exception {
		statSelf();
		if(!getType().hasContent()) throw new FileSystemException("Read not file");
		
		GDriveClientWrapper wrapper = fileSystem.getClientWrapper();
		synchronized(fileSystem) {
			HttpResponse response = wrapper.getClient().getRequestFactory().buildGetRequest(new GenericUrl(gdriveFile.getDownloadUrl())).execute();
			return new GDriveInputStream(wrapper, response.getContent());
		}
	}
	
	@Override
	protected OutputStream doGetOutputStream(boolean append) throws Exception {
		statSelf();
		
		GDriveClientWrapper wrapper = fileSystem.getClientWrapper();
		synchronized(fileSystem) {
			
			File file = doCreateEntry(wrapper, "text/plain");
			PipedOutputStream postream = new PipedOutputStream();
			PipedInputStream pistream = new PipedInputStream(postream, 4096);
			long len = -1L;
			try {
				len = Long.parseLong(String.valueOf(super.getContent().getAttribute("Content-Length")));
			} catch(Exception ex) { /* Do nothing... */ }
			InputStreamContent content = new InputStreamContent(file.getMimeType(), pistream);
			if(len != -1L) content.setLength(len);
			
			final Drive.Files.Update request = wrapper.getClient().files().update(file.getId(), file, content);
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						request.execute();
					} catch(Exception ex) { /* Do nothing... */ }
				}
			}, "[vfslib] Update Requester");
			thread.setPriority(1);
			thread.start();
			return new GDriveOutputStream(wrapper, thread, postream);
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
	
	private File doCreateEntry(GDriveClientWrapper wrapper, String mimetype) throws Exception {
		statSelf();
		File body = new File();
		body.setTitle(getGoogleTitle());
		if(mimetype != null) body.setMimeType(mimetype);
		List parentReferences = null;
		if(isInRoot()) {
			parentReferences = new ArrayList(1);
			parentReferences.add((new ParentReference()).setId(getRootID()));
		} else {
			GDriveFileObject parent = (GDriveFileObject)getParent();
			parent.statSelf();
			if(parent.gdriveFile != null) {
				parentReferences = new ArrayList(1);
				parentReferences.add((new ParentReference()).setId(parent.gdriveFile.getId()));
			}
		}
		if(parentReferences != null) {
			body.setParents(parentReferences);
			Drive.Files.Insert request = wrapper.getClient().files().insert(body);
			return (File)request.execute();
		} else {
			return null;
		}
	}
	
	private class GDriveInputStream extends MonitorInputStream {
		
		private GDriveClientWrapper wrapper = null;
		private Thread requester;

		public GDriveInputStream(GDriveClientWrapper wrapper, InputStream in) {
			super(in);
			this.wrapper = wrapper;
		}
		
		@Override
		protected void onClose() throws IOException {
			fileSystem.putClientWrapper(wrapper);
		}
	}
	
	private class GDriveOutputStream extends MonitorOutputStream {
		
		private GDriveClientWrapper wrapper = null;
		private Thread requester = null;
		
		public GDriveOutputStream(GDriveClientWrapper wrapper, Thread requester, OutputStream out) {
			super(out);
			this.wrapper = wrapper;
			this.requester = requester;
		}
		
		@Override
		protected void onClose() throws IOException {
			try {
				requester.join();
			} catch(Exception ex) { /* Do nothing... */ }
			fileSystem.putClientWrapper(wrapper);
		}
	}
}
