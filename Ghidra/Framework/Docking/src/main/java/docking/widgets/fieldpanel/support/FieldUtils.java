/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package docking.widgets.fieldpanel.support;

import java.util.ArrayList;
import java.util.List;

import docking.widgets.fieldpanel.field.FieldElement;

/**
 * A utility class for working with Field objects.
 */
public class FieldUtils {

	public static final String WORD_WRAP_OPTION_NAME = "Enable Word Wrapping";
	public static final String WORD_WRAP_OPTION_DESCRIPTION =
		"Enables word wrapping.  When on, each line of text is wrapped as needed to fit within " +
			"the current width.  When off, comments are displayed as entered by the user.  Lines " +
			"that are too long for the field are truncated.";

	private FieldUtils() { // utility class
	}

	public static List<FieldElement> wrap(List<FieldElement> fieldElements, int width) {
		List<FieldElement> wrappedElements = new ArrayList<>();
		for (FieldElement fieldElement : fieldElements) {
			wrappedElements.addAll(wrap(fieldElement, width));
		}
		return wrappedElements;
	}

	/**
	 * Splits the given FieldElement into sub-elements by wrapping the element on whitespace.
	 * 
	 * @param fieldElement The element to wrap
	 * @param width The maximum width to allow before wrapping
	 * @return The wrapped elements
	 */
	public static List<FieldElement> wrap(FieldElement fieldElement, int width) {
		List<FieldElement> lines = new ArrayList<>();
		if (fieldElement.getStringWidth() <= width) {
			lines.add(fieldElement);
			return lines;
		}

		FieldElement element = fieldElement;
		int wordWrapPos = findWordWrapPosition(element, width);
		while (wordWrapPos > 0) {
			lines.add(element.substring(0, wordWrapPos));
			element = element.substring(wordWrapPos);
			wordWrapPos = findWordWrapPosition(element, width);
		}
		lines.add(element);
		return lines;
	}

	/**
	 * Splits the given FieldElement into sub-elements by wrapping the element in some fashion.
	 * If breakOnWhiteSpace is indicated, wrapping will break lines on a white space character
	 * if possible, otherwise wrapping occurs on the last possible character.
	 * @param fieldElement is the element to wrap
	 * @param width is the maximum width to allow before wrapping
	 * @param breakOnWhiteSpace determines whether line breaks should happen at white space chars
	 * @return the wrapped elements
	 */
	public static List<FieldElement> wrap(FieldElement fieldElement, int width,
			boolean breakOnWhiteSpace) {

		if (breakOnWhiteSpace) {
			return wrap(fieldElement, width);
		}

		List<FieldElement> lines = new ArrayList<>();
		if (fieldElement.getStringWidth() <= width) {
			lines.add(fieldElement);
			return lines;
		}

		FieldElement element = fieldElement;
		int wordWrapPos = element.getMaxCharactersForWidth(width);
		if (wordWrapPos == element.length()) {
			wordWrapPos = 0;
		}

		while (wordWrapPos > 0) {
			lines.add(element.substring(0, wordWrapPos));
			element = element.substring(wordWrapPos);
			wordWrapPos = element.getMaxCharactersForWidth(width);
			if (wordWrapPos == element.length()) {
				wordWrapPos = 0;
			}
		}
		lines.add(element);
		return lines;
	}

	/**
	 * Finds the position within the given element at which to split the line for word wrapping.
	 * This method finds the last whitespace character that completely fits within the given width.
	 * If there is no whitespace character before the width break point, it finds the first
	 * whitespace character after the width.  If no whitespace can be found, then 0 will be returned
	 * to signal that there is no spot to break the line.
	 * 
	 * @param element the element to split
	 * @param width the max width to allow before looking for a word wrap positions
	 * @return 0 if the element cannot be split, else the character position of the string
	 * to be split, exclusive
	 */
	private static int findWordWrapPosition(FieldElement element, int width) {

		String text = element.getText();
		int wrapPosition = element.getMaxCharactersForWidth(width);
		if (wrapPosition == element.length() || wrapPosition == 0) {
			return 0;
		}

		// inclusive
		int whiteSpacePosition = text.lastIndexOf(" ", wrapPosition - 1);
		if (whiteSpacePosition >= 0) {
			return whiteSpacePosition + 1; // exclusive
		}

		return wrapPosition;
	}

	/**
	 * Trims unwanted characters off of the given label, like spaces, '[',']', etc.
	 * @param string The string to be trimmed
	 * @return The trimmed string.
	 */
	public static String trimString(String string) {
		// short-circuit case where the given string starts normally, but contains invalid
		// characters (e.g., param_1[EAX])
		StringBuilder buffer = new StringBuilder(string);
		if (Character.isJavaIdentifierPart(buffer.charAt(0))) {
			// in this case just take all valid characters and then exit
			for (int index = 1; index < buffer.length(); index++) {
				int charAt = buffer.charAt(index);
				if (!Character.isJavaIdentifierPart(charAt)) {
					return buffer.substring(0, index);
				}
			}
			return buffer.toString();
		}

		// the following case is when the given string is surrounded by "goofy" characters
		int index = 0;
		int charAt = buffer.charAt(index);
		while (!Character.isJavaIdentifierPart(charAt) && buffer.length() > 0) {
			buffer.deleteCharAt(0);
			charAt = buffer.charAt(0);
		}

		index = buffer.length() - 1;
		charAt = buffer.charAt(index);
		while (!Character.isJavaIdentifierPart(charAt) && index > 0) {
			buffer.deleteCharAt(index);
			charAt = buffer.charAt(--index);
		}

		return buffer.toString();
	}

}
