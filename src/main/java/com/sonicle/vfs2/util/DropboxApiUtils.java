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
package com.sonicle.vfs2.util;

import com.dropbox.core.DbxAccountInfo;
import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuthNoRedirect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author malbinola
 */
public class DropboxApiUtils {
	static final Logger logger = (Logger) LoggerFactory.getLogger(DropboxApiUtils.class);
	
	public static DbxAppInfo createAppInfo(String appKey, String appSecret) {
		return new DbxAppInfo(appKey, appSecret);
	}
	
	public static DbxRequestConfig createRequestConfig(String clientIdentifier, String userLocale) {
		return new DbxRequestConfig(clientIdentifier, userLocale);
	}
	
	public static String getAuthorizationUrl(String clientIdentifier, String userLocale, String appKey, String appSecret) {
		DbxAppInfo appInfo = DropboxApiUtils.createAppInfo(appKey, appSecret);
		DbxRequestConfig config = DropboxApiUtils.createRequestConfig(clientIdentifier, userLocale);
		return getAuthorizationUrl(config, appInfo);
	}
	
	public static String getAuthorizationUrl(DbxRequestConfig reqConfig, DbxAppInfo appInfo) {
		DbxWebAuthNoRedirect webAuth = createWebAuthNoRedirect(reqConfig, appInfo);
		logger.debug("Building authorization URL");
		return webAuth.start();
	}
	
	public static DbxAuthFinish exchangeAuthorizationCode(String code, DbxRequestConfig reqConfig, DbxAppInfo appInfo) throws DbxException {
		DbxWebAuthNoRedirect webAuth = createWebAuthNoRedirect(reqConfig, appInfo);
		logger.debug("Exchanging authentication code {}", code);
		return webAuth.finish(code);
	}
	
	public static DbxAccountInfo getAccountInfo(String accessToken, DbxRequestConfig reqConfig) throws DbxException {
		return DropboxApiUtils.createClient(accessToken, reqConfig).getAccountInfo();
	}
	
	public static DbxClient createClient(String accessToken, String clientIdentifier, String userLocale) {
		DbxRequestConfig reqConfig = new DbxRequestConfig(clientIdentifier, userLocale);
		return createClient(accessToken, reqConfig);
	}
	
	public static DbxClient createClient(String accessToken, DbxRequestConfig reqConfig) {
		logger.debug("Creating DbxClient for {}@{}", accessToken, reqConfig.clientIdentifier);
		return new DbxClient(reqConfig, accessToken);
	}
	
	private static DbxWebAuthNoRedirect createWebAuthNoRedirect(DbxRequestConfig reqConfig, DbxAppInfo appInfo) {
		return new DbxWebAuthNoRedirect(reqConfig, appInfo);
	}
}
