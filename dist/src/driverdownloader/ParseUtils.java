package driverdownloader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Mark Van Peteghem
 */
public class ParseUtils {
    /**
     * Convenience method for searching in a String object.
     * It does the same as the indexOf method of String,
     * but throws an exception if the substring is not found.
     *
     * @param src The string to look in
     * @param sub The substring to find
     * @param start Start position
     * @return the position where the substring was found
     * @throws UnexpectedFormatException
     */
    static int indexOf(String src, String sub, int start, boolean caseSensitive) {
        if (caseSensitive) {
            int pos = src.indexOf(sub, start);
            return pos;
        } else {
            for (int pos=start; pos<=src.length()-sub.length(); ++pos) {
                boolean match = true;
                for (int i=0; i<sub.length(); ++i) {
                    if (Character.toLowerCase(src.charAt(pos+i))!=Character.toLowerCase(sub.charAt(i))) {
                        match = false;
                        break;
                    }
                }
                if (match)
                    return pos;
            }
            return -1;
        }
    }

     /**
     * Convenience method for searching in a String object.
     * It does the same as the indexOf method of String,
     * but throws an exception if the substring is not found.
     *
     * @param src The string to look in
     * @param sub The substring to find
     * @param start Start position
     * @return the position where the substring was found
     * @throws UnexpectedFormatException
     */
    static int safeIndexOf(String src, String sub, int start, boolean caseSensitive) throws UnexpectedFormatException {
        if (caseSensitive) {
            int pos = src.indexOf(sub, start);
            if (pos<0)
                throw new UnexpectedFormatException();
            return pos;
        } else {
            for (int pos=start; pos<=src.length()-sub.length(); ++pos) {
                boolean match = true;
                for (int i=0; i<sub.length(); ++i) {
                    if (Character.toLowerCase(src.charAt(pos+i))!=Character.toLowerCase(sub.charAt(i))) {
                        match = false;
                        break;
                    }
                }
                if (match)
                    return pos;
            }
            throw new UnexpectedFormatException();
        }
    }

     /**
     * Convenience method for searching in a String object.
     * It does the same as the indexOf method of String,
     * but throws an exception if the substring is not found.
     *
     * @param src The string to look in
     * @param sub The substring to find
     * @param start Start position
     * @return the position where the substring was found
     * @throws UnexpectedFormatException
     */
    static int safeLastIndexOf(String src, String sub, int start, boolean caseSensitive) throws UnexpectedFormatException {
        if (caseSensitive) {
            int pos = src.lastIndexOf(sub, start);
            if (pos<0)
                throw new UnexpectedFormatException();
            return pos;
        } else {
            for (int pos=start; pos>=0; --pos) {
                boolean match = true;
                for (int i=0; i<sub.length(); ++i) {
                    if (Character.toLowerCase(src.charAt(pos+i))!=Character.toLowerCase(sub.charAt(i))) {
                        match = false;
                        break;
                    }
                }
                if (match)
                    return pos;
            }
            throw new UnexpectedFormatException();
        }
    }

     /**
     * Convenience method for searching in a String object.
     * It does the same as the indexOf method of String,
     * but throws an exception if the substring is not found.
     *
     * @param src The string to look in
     * @param sub The substring to find
     * @param start Start position
     * @return the position where the substring was found
     * @throws UnexpectedFormatException
     */
    static int safeLastIndexOf(String src, String sub, boolean caseSensitive) throws UnexpectedFormatException {
        return safeLastIndexOf(src, sub, src.length(), caseSensitive);
    }

    /**
     * Looks for part of a string that is between two given pieces
     * @param src The text to look for
     * @param before The text that should be just before the returned text
     * @param after The text that should be just after the returned text
     * @return The found text
     * @throws UnexpectedFormatException
     */
    static String findSubstringByHeaderString(String src, String before, String after, boolean caseSensitive)
            throws UnexpectedFormatException {
        return findSubstringByHeaderString(src, 0, before, after, caseSensitive);
    }

    /**
     * Looks for part of a string that is between two given pieces, given a start position
     * @param src The text to look for
     * @param start Start position to look for the text
     * @param before The text that should be just before the returned text
     * @param after The text that should be just after the returned text
     * @return The found text
     * @throws UnexpectedFormatException
     */
    static String findSubstringByHeaderString(String src, int start, String before, String after, boolean caseSensitive)
            throws UnexpectedFormatException {
        int pos = safeIndexOf(src, before, start, caseSensitive);
        int end = after.length()==0 ? src.length() : safeIndexOf(src, after, pos+before.length(), caseSensitive);
        char chars[] = new char[end-(pos+before.length())];
        src.getChars(pos+before.length(), end, chars, 0);
        return new String(chars);
    }

    /**
     * Looks for part of a string that is between two given pieces, but only after a certain piece of text
     * @param src The text to look for
     * @param start Text after which the method should start looking
     * @param before The text that should be just before the returned text
     * @param after The text that should be just after the returned text
     * @return The found text
     * @throws UnexpectedFormatException
     */
    static String findSubstringByHeaderString(String src, String start, String before, String after, boolean caseSensitive)
            throws UnexpectedFormatException {
        int pos = safeIndexOf(src, start, 0, caseSensitive);
        return findSubstringByHeaderString(src, pos+start.length(), before, after, caseSensitive);
    }

    /**
     * Looks for part of a string that is between two given pieces, but only after a certain piece of text
     * @param src The text to look for
     * @param start Text after which the method should start looking
     * @param before The text that should be just before the returned text
     * @param after The text that should be just after the returned text
     * @return The found text
     * @throws UnexpectedFormatException
     */
    static String findSubstringByHeaderString(String src, String start1, String start2, String before, String after, boolean caseSensitive)
            throws UnexpectedFormatException {
        int pos = safeIndexOf(src, start1, 0, caseSensitive);
        pos = safeIndexOf(src, start2, pos, caseSensitive);
        return findSubstringByHeaderString(src, pos+start2.length(), before, after, caseSensitive);
    }

    /**
     * Looks for part of a string that is between two given pieces
     * @param src The text to look for
     * @param before The text that should be just before the returned text
     * @param after The text that should be just after the returned text
     * @return The found text
     * @throws UnexpectedFormatException
     */
    static String findSubstringByFooterString(String src, String before, String after, boolean caseSensitive)
            throws UnexpectedFormatException {
        return findSubstringByFooterString(src, 0, before, after, caseSensitive);
    }

    /**
     * Looks for part of a string that is between two given pieces, given a start position
     * @param src The text to look for
     * @param start Start position to look for the text
     * @param before The text that should be just before the returned text
     * @param after The text that should be just after the returned text
     * @return The found text
     * @throws UnexpectedFormatException
     */
    static String findSubstringByFooterString(String src, int start, String before, String after, boolean caseSensitive)
            throws UnexpectedFormatException {
        int end = safeIndexOf(src, after, start, caseSensitive);
        int pos = before.length()==0 ? 0 : safeLastIndexOf(src, before, end, caseSensitive);
        char chars[] = new char[end-(pos+before.length())];
        src.getChars(pos+before.length(), end, chars, 0);
        return new String(chars);
    }

    /**
     * Looks for part of a string that is between two given pieces, but only after a certain piece of text
     * @param src The text to look for
     * @param start Text after which the method should start looking
     * @param before The text that should be just before the returned text
     * @param after The text that should be just after the returned text
     * @return The found text
     * @throws UnexpectedFormatException
     */
    static String findSubstringByFooterString(String src, String start, String before, String after, boolean caseSensitive)
            throws UnexpectedFormatException {
        int pos = safeIndexOf(src, start, 0, caseSensitive);
        return findSubstringByFooterString(src, pos+start.length(), before, after, caseSensitive);
    }

    /**
     * Looks for part of a string that is between two given pieces, given a start position
     * @param src The text to look for
     * @param start Start position to look for the text
     * @param before The text that should be just before the returned text
     * @param after The text that should be just after the returned text
     * @return The found text
     * @throws UnexpectedFormatException
     */
    static String findLastSubstringBySurroundingStrings(String src, int start, String before, String after, boolean caseSensitive)
            throws UnexpectedFormatException {
        int pos = safeLastIndexOf(src, before, start, caseSensitive);
        int end = after.length()==0 ? src.length() : safeIndexOf(src, after, pos+before.length(), caseSensitive);
        return src.substring(pos+before.length(), end);
    }

    /**
     * Looks for part of a string that is between two given pieces, but only after a certain piece of text
     * @param src The text to look for
     * @param start Text after which the method should start looking
     * @param before The text that should be just before the returned text
     * @param after The text that should be just after the returned text
     * @return The found text
     * @throws UnexpectedFormatException
     */
    static String findLastSubstringBySurroundingStrings(String src, String start, String before, String after, boolean caseSensitive)
            throws UnexpectedFormatException {
        int pos = safeIndexOf(src, start, 0, caseSensitive);
        return findLastSubstringBySurroundingStrings(src, pos+start.length(), before, after, caseSensitive);
    }

    static ArrayList<String> getSubStrings(String src, String before, String after, boolean caseSensitive) throws UnexpectedFormatException {
        ArrayList<String> strings = new ArrayList<String>();

        int pos = 0;
        while (true) {
            pos = indexOf(src, before, pos, caseSensitive);
            if (pos<0)
                break;
            int endPos = indexOf(src, after, pos+before.length(), caseSensitive);
            if (endPos<0)
                break;
            char chars[] = new char[endPos-(pos+before.length())];
            src.getChars(pos+before.length(), endPos, chars, 0);
            strings.add(new String(chars));
            pos = endPos+after.length();
        }

        return strings;
    }

	static public Map<String, String> getInputs(String content, int formIdx)
		throws UnexpectedFormatException {
		
		Map<String, String> postData = new HashMap<String, String>();
		
		int pos = 0;
		for (int i=0; i<formIdx; ++i) {
			pos = safeIndexOf(content, "<form", pos, false)+5;
		}
		
		String form = ParseUtils.findSubstringByHeaderString(content, pos, "<form", "</form>", false);
		List<String> inputs = ParseUtils.getSubStrings(form, "<input", "/>", false);
		for (String input: inputs) {
			String name = ParseUtils.findSubstringByHeaderString(input, "name=\"", "\"", false);
			String value = "";
			if (input.contains("value"))
				value = ParseUtils.findSubstringByHeaderString(input, "value=\"", "\"", false);
			postData.put(name, value);
		}
		return postData;
	}

	static public String getUrlDomain(String url) {
		int pos = url.indexOf('/', 8); // 8 to skip http://
		if (pos<0)
			return "";
		else
			return url.substring(0, pos);
	}

	static public String getUrlFolder(String url) {
		int pos = url.lastIndexOf('/');
		if (pos<0)
			return "";
		else
			return url.substring(0, pos+1);
	}
}
