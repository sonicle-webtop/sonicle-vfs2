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

package com.sonicle.vfs2.provider.dropbox;

import com.dropbox.core.DbxEntry;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.Properties;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemOptions;

/**
 *
 * @author malbinola
 */
public class DbxFileSystemConfigBuilder extends FileSystemConfigBuilder {
	
	private static final DbxFileSystemConfigBuilder INSTANCE =  new DbxFileSystemConfigBuilder();
	
	public static final String CLIENT_IDENTIFIER = DbxFileSystemConfigBuilder.class.getName() + ".CLIENT_IDENTIFIER";
	public static final String ACCESS_TOKEN = DbxFileSystemConfigBuilder.class.getName() + ".ACCESS_TOKEN";
	public static final String CHUNK_SIZE = DbxFileSystemConfigBuilder.class.getName() + ".CHUNK_SIZE";
	
	private DbxFileSystemConfigBuilder() {
		super("dropbox.");
	}
	
	public static DbxFileSystemConfigBuilder getInstance() {
		return INSTANCE;
	}

	@Override
	protected Class<? extends FileSystem> getConfigClass() {
		return DbxFileSystem.class;
	}
	
	public String getClientIdentifier(FileSystemOptions fso) {
		return this.getString(fso, CLIENT_IDENTIFIER);
	}
	
	public void setClientIdentifier(FileSystemOptions fso, String value) {
		this.setParam(fso, CLIENT_IDENTIFIER, value);
	}
	
	public String getAccessToken(FileSystemOptions fso) {
		return this.getString(fso, ACCESS_TOKEN);
	}
	
	public void setAccessToken(FileSystemOptions fso, String value) {
		this.setParam(fso, ACCESS_TOKEN, value);
	}
	
	public Integer getChunkSize(FileSystemOptions fso) {
		return this.getInteger(fso, CHUNK_SIZE, 4096);
	}
	
	public void setChunkSize(FileSystemOptions fso, Integer value) {
		if(value != null && value <= 0) {
			throw new IllegalArgumentException("Invalid ChunkSize value");
		} else {
			this.setParam(fso, CHUNK_SIZE, value);
		}
	}
	
	public static Properties convertFromDbxEntry(DbxEntry entry) {
		Properties props = new Properties();
		try {
			StringBuilder builder = new StringBuilder(0);
			entry.toStringMultiline(builder, 0, true);
			LineNumberReader reader = new LineNumberReader(new StringReader(builder.toString()));
			do {
				String line;
				if((line = reader.readLine()) == null) break;
				line = line.trim();
				if(!line.isEmpty()) {
					String converted = readConvert(line);
					int pos = converted.indexOf('=');
					if(pos > -1) {
						String variable = converted.substring(0, pos).trim();
						String value = converted.substring(pos + 1).trim().replaceAll("(^[\"]|[\"]$)", "");
						props.setProperty(variable, value);
					}
				}
			} while(true);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return props;
	}
	
	private static String readConvert(String theString) {
		int len = theString.length();
		int bufLen = len << 1;
		if (bufLen < 0) {
			bufLen = 0x7fffffff;
		}
		StringBuilder outBuffer = new StringBuilder(bufLen);
		for (int x = 0; x < len; x++) {
			char aChar = theString.charAt(x);
			if (aChar == '\\' && x + 1 < len) {
				char lookahead = theString.charAt(x + 1);
				switch (lookahead) {
					case 61: // '='
						outBuffer.append('=');
						x++;
						break;

					case 114: // 'r'
						outBuffer.append('\r');
						x++;
						break;

					case 110: // 'n'
						outBuffer.append('\n');
						x++;
						break;

					case 35: // '#'
						outBuffer.append('#');
						x++;
						break;

					case 92: // '\\'
						outBuffer.append('\\');
						x++;
						break;

					default:
						outBuffer.append(aChar);
						break;
				}
			} else {
				outBuffer.append(aChar);
			}
		}
		return outBuffer.toString();
	}
}
