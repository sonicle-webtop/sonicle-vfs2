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
package com.sonicle.vfs2.util;

import java.util.Locale;

/**
 * Windows (Win32) naming rules for a single path component.
 * <p>
 * Windows clients cannot handle names that the SMB protocol, and most servers
 * (Samba, illumos smbsrv on ZFS...), happily accept: Explorer strips trailing
 * dots and spaces before sending a name, so a folder created as "D.I." by
 * another client can be neither opened nor deleted from Windows.
 * <p>
 * The rules are the ones of
 * <a href="https://learn.microsoft.com/windows/win32/fileio/naming-a-file">Naming Files, Paths, and Namespaces</a>:
 * <ul>
 * <li>no trailing dot or space;</li>
 * <li>none of the characters <code>&lt; &gt; : " / \ | ? *</code> and no control characters (0-31);</li>
 * <li>no reserved device name (CON, PRN, AUX, NUL, COM0-9, LPT0-9, also with
 * the superscript digits 1, 2 and 3), with or without an extension: "nul.txt" is reserved too.</li>
 * </ul>
 *
 * @author malbinola
 */
public final class WindowsFileNames {

	public enum Problem {
		/**
		 * The name is empty, or only made of dots and spaces.
		 */
		EMPTY,
		/**
		 * The name ends with a dot or a space.
		 */
		TRAILING_DOT_OR_SPACE,
		/**
		 * The name contains one of <code>&lt; &gt; : " / \ | ? *</code> or a control character.
		 */
		INVALID_CHARACTER,
		/**
		 * The name is a reserved device name (CON, NUL, COM1...), extension or not.
		 */
		RESERVED_NAME
	}

	private WindowsFileNames() {}

	/**
	 * Removes trailing dots and spaces, as Windows does: "D.I." becomes "D.I",
	 * "a . ." becomes "a". A name made only of dots and spaces becomes empty.
	 * @param name a single path component
	 * @return the name without trailing dots and spaces
	 */
	public static String stripTrailingDotsAndSpaces(String name) {
		if (name == null) return null;
		int end = name.length();
		while (end > 0) {
			char c = name.charAt(end - 1);
			if (c != '.' && c != ' ') break;
			end--;
		}
		return end == name.length() ? name : name.substring(0, end);
	}

	/**
	 * Checks if a single name component ends with a dot or a space.
	 * @param name a single path component
	 * @return true if Windows would strip something
	 */
	public static boolean hasTrailingDotOrSpace(String name) {
		if (name == null || name.isEmpty()) return false;
		char c = name.charAt(name.length() - 1);
		return c == '.' || c == ' ';
	}

	/**
	 * Checks if any component of a slash-separated path ends with a dot or a
	 * space. The special components "." and ".." are not counted.
	 * @param path a path like "/a/b./c"
	 * @return true if at least one component would be stripped by Windows
	 */
	public static boolean hasComponentWithTrailingDotOrSpace(String path) {
		if (path == null) return false;
		int start = 0;
		final int len = path.length();
		while (start <= len) {
			int end = path.indexOf('/', start);
			if (end < 0) end = len;
			final String comp = path.substring(start, end);
			if (!".".equals(comp) && !"..".equals(comp) && hasTrailingDotOrSpace(comp)) return true;
			start = end + 1;
		}
		return false;
	}

	/**
	 * Strips trailing dots and spaces from every component of a slash-separated path.
	 * The special components "." and ".." are kept as they are.
	 * @param path a path like "/a/b./c"
	 * @return the sanitized path
	 */
	public static String stripTrailingDotsAndSpacesFromPath(String path) {
		if (path == null || !hasComponentWithTrailingDotOrSpace(path)) return path;
		final StringBuilder sb = new StringBuilder(path.length());
		int start = 0;
		final int len = path.length();
		while (start <= len) {
			int end = path.indexOf('/', start);
			if (end < 0) end = len;
			final String comp = path.substring(start, end);
			sb.append(".".equals(comp) || "..".equals(comp) ? comp : stripTrailingDotsAndSpaces(comp));
			if (end < len) sb.append('/');
			start = end + 1;
		}
		return sb.toString();
	}

	/**
	 * Checks a single name component against the Windows rules.
	 * @param name a single path component
	 * @return the first problem found, or null if Windows accepts the name
	 */
	public static Problem check(String name) {
		if (name == null || stripTrailingDotsAndSpaces(name).isEmpty()) return Problem.EMPTY;
		for (int i = 0; i < name.length(); i++) {
			if (isInvalidChar(name.charAt(i))) return Problem.INVALID_CHARACTER;
		}
		if (isReservedName(name)) return Problem.RESERVED_NAME;
		if (hasTrailingDotOrSpace(name)) return Problem.TRAILING_DOT_OR_SPACE;
		return null;
	}

	/**
	 * Checks a single name component against the Windows rules.
	 * @param name a single path component
	 * @return true if Windows accepts the name
	 */
	public static boolean isValid(String name) {
		return check(name) == null;
	}

	/**
	 * Checks if a character can never appear in a Windows file name.
	 * @param c the character
	 * @return true for <code>&lt; &gt; : " / \ | ? *</code> and the control characters 0-31
	 */
	public static boolean isInvalidChar(char c) {
		if (c < 32) return true;
		switch (c) {
			case '<':
			case '>':
			case ':':
			case '"':
			case '/':
			case '\\':
			case '|':
			case '?':
			case '*':
				return true;
			default:
				return false;
		}
	}

	/**
	 * Checks if a name is a reserved device name. Windows looks at the part
	 * before the first dot, ignoring its trailing spaces: "NUL", "nul.txt",
	 * "Com1.tar.gz" and "CON .txt" are all reserved, "CONSOLE" and "xnul" are not.
	 * @param name a single path component
	 * @return true if reserved
	 */
	public static boolean isReservedName(String name) {
		if (name == null) return false;
		int dot = name.indexOf('.');
		String base = dot < 0 ? name : name.substring(0, dot);
		int end = base.length();
		while (end > 0 && base.charAt(end - 1) == ' ') end--;
		base = base.substring(0, end).toUpperCase(Locale.ROOT);

		switch (base.length()) {
			case 3:
				return "CON".equals(base) || "PRN".equals(base) || "AUX".equals(base) || "NUL".equals(base);
			case 4:
				if (base.startsWith("COM") || base.startsWith("LPT")) {
					char d = base.charAt(3);
					return (d >= '0' && d <= '9') || d == '¹' || d == '²' || d == '³';
				}
				return false;
			default:
				return false;
		}
	}
}
