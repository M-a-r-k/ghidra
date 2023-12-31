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
package ghidra.feature.vt.gui.filters;

import static ghidra.feature.vt.gui.filters.Filter.FilterEditingStatus.*;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

import javax.swing.InputVerifier;
import javax.swing.JFormattedTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import generic.theme.GColor;
import generic.theme.GThemeDefaults.Colors;
import ghidra.feature.vt.gui.filters.Filter.FilterEditingStatus;
import ghidra.util.SystemUtilities;

public class FilterFormattedTextField extends JFormattedTextField {
	private static final Color ERROR_BACKGROUND_COLOR =
		new GColor("color.bg.version.tracking.filter.formatted.field.error");
	private static final Color EDITING_BACKGROUND_COLOR =
		new GColor("color.bg.version.tracking.filter.formatted.field.editing");
	private static final Color EDITING_FOREGROUND_COLOR =
		new GColor("color.fg.version.tracking.filter.formatted.field.editing");

	private Set<FilterStatusListener> listeners = new HashSet<>();

	private FilterEditingStatus currentStatus = NONE;
	private final Object defaultValue;
	private final String defaultText;
	private boolean isError;
	private boolean ignoreFocusEditChanges;

	/** A flag to let us know when we can ignore focus updates */
	private boolean isProcessingFocusEvent;

	public FilterFormattedTextField(AbstractFormatterFactory factory, Object defaultValue) {
		super(factory);
		setValue(defaultValue);
		this.defaultValue = defaultValue;
		this.defaultText = getText(); // get the formatted text
		this.currentStatus = NONE;

		getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				updateText();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				updateText();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateText();
			}
		});

		addPropertyChangeListener("value", evt -> editingFinished());

		update();
	}

	public void disableFocusEventProcessing() {
		ignoreFocusEditChanges = true;
	}

	@Override
	public int getFocusLostBehavior() {
		if (ignoreFocusEditChanges) {
			return -1; // force us to ignore the focus event
		}
		return super.getFocusLostBehavior();
	}

	@Override
	protected void processFocusEvent(FocusEvent e) {
		isProcessingFocusEvent = true;
		super.processFocusEvent(e);
		isProcessingFocusEvent = false;
	}

	public FilterEditingStatus getFilterStatus() {
		return currentStatus;
	}

	public void addFilterStatusListener(FilterStatusListener listener) {
		listeners.add(listener);
	}

	private void filterStatusChanged(FilterEditingStatus status) {
		currentStatus = status;
		if (listeners == null) {
			return; // happens during construction
		}

		for (FilterStatusListener listener : listeners) {
			listener.filterStatusChanged(status);
		}
	}

	private void updateText() {
		if (isProcessingFocusEvent) {
			return; // ignore transient events
		}

		InputVerifier verifier = getInputVerifier();
		if (verifier != null) {
			setIsError(!verifier.verify(this));
		}

		update();
	}

	@Override
	public void setText(String t) {
		if (SystemUtilities.isEqual(getText(), t)) {
			return;
		}
		super.setText(t);
		update();
	}

	public void setIsError(boolean isError) {
		//            if ( isError && !this.isError ) {
		//                warn(); // only warn if we were not already in an error situation
		//            }
		this.isError = isError;
		update();
	}

	public void editingFinished() {
		update();
	}

	private boolean hasNonDefaultValue() {
		if (defaultText == null) {
			return false; // not yet initialized
		}

		AbstractFormatter formatter = getFormatter();
		if (formatter == null) {
			return hasNonDefaultText(); // no formatter implies a text only field
		}

		try {
			Object value = formatter.stringToValue(getText());
			if (value == null) {
				return true; // assume empty string or invalid text
			}
			return !value.equals(defaultValue);
		}
		catch (ParseException e) {
			return true;
		}
	}

	private boolean hasNonDefaultText() {
		return !getText().equals(defaultText);
	}

	private void update() {

		updateStatus();
		if (isError) {
			setForeground(Colors.FOREGROUND);
			setBackground(ERROR_BACKGROUND_COLOR);
		}
		else if (hasNonDefaultValue()) {
			setForeground(EDITING_FOREGROUND_COLOR);
			setBackground(EDITING_BACKGROUND_COLOR);
		}
		else { // default
			setForeground(Colors.FOREGROUND);
			setBackground(Colors.BACKGROUND);
		}

		filterStatusChanged(currentStatus);
	}

	private void updateStatus() {
		FilterEditingStatus oldStatus = currentStatus;
		if (isError) {
			currentStatus = FilterEditingStatus.ERROR;
		}

		else if (hasNonDefaultValue()) {
			currentStatus = APPLIED;
		}
		else {
			currentStatus = NONE;
		}

		if (oldStatus != currentStatus) {
			filterStatusChanged(currentStatus);
		}
	}

}
