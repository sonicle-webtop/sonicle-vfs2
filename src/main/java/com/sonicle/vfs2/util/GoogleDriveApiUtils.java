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

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Tokeninfo;
import com.google.api.services.oauth2.model.Userinfoplus;
import java.io.IOException;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author malbinola
 */
public class GoogleDriveApiUtils {
	static final Logger logger = (Logger) LoggerFactory.getLogger(GoogleDriveApiUtils.class);
	public static final HttpTransport TRANSPORT = new NetHttpTransport();
	public static final JsonFactory JSON_FACTORY = new JacksonFactory();
	public static final String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";
	public static final String SCOPE_USERINFO_EMAIL = "https://www.googleapis.com/auth/userinfo.email";
	public static final String SCOPE_USERINFO_PROFILE = "https://www.googleapis.com/auth/userinfo.profile";
	
	public static GoogleDriveAppInfo createAppInfo(String applicationName, String clientId, String clientSecret) {
		return new GoogleDriveAppInfo(applicationName, clientId, clientSecret);
	}
	
	public static String getAuthorizationUrl(GoogleDriveAppInfo appInfo) {
		logger.debug("Building authorization URL");
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
			TRANSPORT, JSON_FACTORY, appInfo.clientId, appInfo.clientSecret, Arrays.asList(DriveScopes.DRIVE, SCOPE_USERINFO_EMAIL, SCOPE_USERINFO_PROFILE))
			.setAccessType("offline")
			.setApprovalPrompt("auto")
			.build();
		return flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI).build();
	}
	
	public static GoogleCredential exchangeAuthorizationCode(String code, GoogleDriveAppInfo appInfo) throws IOException {
		logger.debug("Exchanging authentication code {}", code);
		GoogleAuthorizationCodeTokenRequest request = new GoogleAuthorizationCodeTokenRequest(TRANSPORT, JSON_FACTORY, appInfo.clientId, appInfo.clientSecret, code, REDIRECT_URI);
		GoogleTokenResponse response = request.execute();
		GoogleCredential credential = new GoogleCredential.Builder()
			.setTransport(TRANSPORT)
			.setJsonFactory(JSON_FACTORY)
			.setClientSecrets(appInfo.clientId, appInfo.clientSecret)
			.build();
		return credential.setFromTokenResponse(response);
	}
	
	public static GoogleCredential refreshToken(String refreshToken, GoogleDriveAppInfo appInfo) throws IOException {
		logger.debug("Refreshing accessToken using {}", refreshToken);
		GoogleRefreshTokenRequest request = new GoogleRefreshTokenRequest(TRANSPORT, JSON_FACTORY, refreshToken, appInfo.clientId, appInfo.clientSecret);
		GoogleTokenResponse response = request.execute();
		return new GoogleCredential().setFromTokenResponse(response);
	}
	
	public static String refreshTokenIfNecessary(String accessToken, String refreshToken, GoogleDriveAppInfo appInfo) throws IOException {
		// Gets current token info
		try {
			Tokeninfo info = getTokenInfo(accessToken, appInfo);
			if(info.getExpiresIn() > 60) return null;
			logger.debug("Token {} will expire in less than 60s", accessToken);
		} catch(IOException ex) {
			logger.debug("Token {} is expired", accessToken, ex);
		}
		
		// Try to get a new token
		GoogleCredential cred = refreshToken(refreshToken, appInfo);
		return cred.getAccessToken();
	}
	
	public static Tokeninfo getTokenInfo(String accessToken, GoogleDriveAppInfo appInfo) throws IOException {
		Oauth2 oauth2 = new Oauth2.Builder(TRANSPORT, JSON_FACTORY, null)
			.setApplicationName(appInfo.applicationName)
			.build();
		return oauth2.tokeninfo().setAccessToken(accessToken).execute();
	}
	
	public static Userinfoplus getUserInfo(String accessToken, GoogleDriveAppInfo appInfo) throws IOException {
		GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
		Oauth2 oauth = new Oauth2.Builder(TRANSPORT, JSON_FACTORY, credential)
			.setApplicationName(appInfo.applicationName)
			.build();
		return oauth.userinfo().get().execute();
	}
	
	public static Drive createClient(String accessToken, GoogleDriveAppInfo appInfo) {
		GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
		Drive.Builder builder = new Drive.Builder(TRANSPORT, JSON_FACTORY, credential);
		builder.setApplicationName(appInfo.applicationName);
		return builder.build();
	}
}
