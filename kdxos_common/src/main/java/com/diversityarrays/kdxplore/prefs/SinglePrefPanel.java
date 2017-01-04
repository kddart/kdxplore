/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016,2017  Diversity Arrays Technology, Pty Ltd.
    
    KDXplore may be redistributed and may be modified under the terms
    of the GNU General Public License as published by the Free Software
    Foundation, either version 3 of the License, or (at your option)
    any later version.
    
    KDXplore is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    
    You should have received a copy of the GNU General Public License
    along with KDXplore.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.diversityarrays.kdxplore.prefs;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.function.Function;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.diversityarrays.kdxplore.prefs.FileSelector.ForWhat;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.MsgBox;

class SinglePrefPanel extends JPanel {

	enum Card {
        BOOLEAN(Boolean.class),
        STRING(String.class),
        FILE(File.class),
        COLOUR(Color.class),

        // All strict should come first
        NUMBER(Number.class, false),
        ENUM(Enum.class, false),
        
        ERROR(null)
	    ;
	    
	    final Class<?> clazz;
	    final boolean strict;
	    Card(Class<?> c) {
	        this(c, true);
	    }
	    Card(Class<?> c, boolean s) {
	        clazz = c;
	        strict = s;
	    }

        public static Card lookupValueClass(Class<?> valueClass) {
            Card result = ERROR;
            for (Card card : values()) {
                if (ERROR == card) {
                    continue;
                }
                if (card.strict) {
                    if (card.clazz == valueClass) {
                        result = card;
                        break;
                    }
                }
                else if (card.clazz.isAssignableFrom(valueClass)) {
                    result = card;
                    break;
                }
            }
            return result;
        }
	}

    static private Component wrapped(JComponent comp) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(comp, BorderLayout.NORTH);
        return p;
    }

    private boolean initialising;

	private final JLabel label = new JLabel();
	
	private final JColorChooser colorChooser = new JColorChooser();
    private final Action setColorAction = new AbstractAction("\u2713") { //$NON-NLS-1$
		@Override
		public void actionPerformed(ActionEvent e) {
			if (! initialising) {
				Color color = colorChooser.getSelectionModel().getSelectedColor();
				updatePreferenceValue(color);
			}
		}
	};

	private final JLabel errorLabel = new JLabel();
	
	private final JTextField textField = new JTextField();
	private final Action saveTextChanges = new AbstractAction(Msg.ACTION_APPLY()) {
        @Override
        public void actionPerformed(ActionEvent e) {
            String text = textField.getText();
            updatePreferenceValue(text);
        }	    
	};
	
	private final Action resetAction = new AbstractAction(Msg.ACTION_RESET()) {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (editingPref.defaultValue != null) {
                updatePreferenceValue(editingPref.defaultValue);
                setPreference(editingPref);
            }
        }
	    
	};
	
	@SuppressWarnings("rawtypes")
	private final DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel<>();
	@SuppressWarnings("unchecked")
	private final JComboBox<?> comboBox = new JComboBox<>(comboBoxModel);
	
	private final FileSelector fileSelector = new FileSelector();
	
	private final SpinnerNumberModel numberModel = new SpinnerNumberModel();
	private final JSpinner numberSpinner = new JSpinner(numberModel);
	
	private final CardLayout cardLayout = new CardLayout();
	private final JPanel cardPanel = new JPanel(cardLayout);
	
	private JCheckBox checkBox = new JCheckBox();

	@SuppressWarnings("rawtypes")
	private KdxPreference editingPref;
	
	private final ItemListener checkBoxItemListener = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (! initialising) {
				updatePreferenceValue(checkBox.isSelected());
			}
		}
	};
	private final ItemListener comboBoxItemListener = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (! initialising) {
				updatePreferenceValue(comboBox.getSelectedItem());
			}
		}
	};

	private ChangeListener fileChangeListener = new ChangeListener() {
		@Override
		public void stateChanged(ChangeEvent e) {
			if (! initialising) {
				updatePreferenceValue(fileSelector.getSelectedFile());
			}
		}
	};

    private KdxplorePreferences preferences;
	
	SinglePrefPanel(KdxplorePreferences prefs) {
		super(new BorderLayout());
		
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		this.preferences = prefs;
		
		Box top = Box.createHorizontalBox();
		top.add(label);
		top.add(Box.createHorizontalGlue());
		top.add(new JButton(resetAction));
		resetAction.setEnabled(false);
		
		add(top, BorderLayout.NORTH);
		add(cardPanel, BorderLayout.CENTER);
		
		Box controls = Box.createHorizontalBox();
        controls.add(Box.createHorizontalGlue());
        controls.add(new JButton(setColorAction));
        controls.add(Box.createHorizontalGlue());
        
		JPanel colorPanel = new JPanel(new BorderLayout());
		colorPanel.add(colorChooser, BorderLayout.CENTER);
		colorPanel.add(controls, BorderLayout.SOUTH);
	
		cardPanel.add(errorLabel, Card.ERROR.name());
		
		Box textEditBox = Box.createHorizontalBox();
		textEditBox.add(textField);
		textEditBox.add(new JButton(saveTextChanges));
		
		cardPanel.add(wrapped(textEditBox),     Card.STRING.name());
		cardPanel.add(wrapped(checkBox),      Card.BOOLEAN.name());
		cardPanel.add(wrapped(comboBox),      Card.ENUM.name());
		cardPanel.add(colorPanel,             Card.COLOUR.name());
		cardPanel.add(wrapped(numberSpinner), Card.NUMBER.name());
		cardPanel.add(wrapped(fileSelector),  Card.FILE.name());

		numberSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (! initialising) {
					Number number = numberModel.getNumber();
					Object value;
					if (Double.class == editingPref.valueClass) {
						value = number.doubleValue();
					}
					else if (Float.class == editingPref.valueClass) {
						value = number.floatValue();
					}
					else if (Integer.class == editingPref.valueClass) {
						value = number.intValue();
					}
					else if (Long.class == editingPref.valueClass) {
						value = number.longValue();
					}
					else {
						throw new RuntimeException(
								"Unsupported: " + editingPref.valueClass.getName()); //$NON-NLS-1$
					}
					
					updatePreferenceValue(value);
				}
			}
		});
		
		comboBox.addItemListener(comboBoxItemListener);
		checkBox.addItemListener(checkBoxItemListener);

		textField.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				if (! initialising) {
					updatePreferenceValue(textField.getText());
				}
			}
			@Override
			public void focusGained(FocusEvent e) {
			}
		});

		fileSelector.addChangeListener(fileChangeListener);
	}

	@SuppressWarnings("unchecked")
	private void updatePreferenceValue(Object value) {
	    if (editingPref.validator != null) {
	        Function<Object, String> v = editingPref.validator;
	        String errmsg = v.apply(value);
	        if (! Check.isEmpty(errmsg)) {
	            MsgBox.warn(this, errmsg, editingPref.getName());
	            return;
	        }	        
	    }
		preferences.savePreferenceValue(editingPref, value);
	}

	public void setPreference(KdxPreference<?> pref) {
		this.editingPref = pref;
		initialising = true;
		try {
		    resetAction.setEnabled(pref.defaultValue != null);
			initPreference(pref);
		}
		finally {
			initialising = false;
		}
	}
	
	@SuppressWarnings("unchecked")
	private void initPreference(KdxPreference<?> pref) {
		
        String title = pref.getName();

		Object value = preferences.getPreferenceValue(pref);

		Card card = Card.lookupValueClass(pref.valueClass);
	    switch (card) {
        case BOOLEAN:
            Boolean b = (Boolean) value;
            checkBox.setSelected(b);
            break;
        
        case COLOUR:
            colorChooser.setColor(value==null ? Color.black : (Color) value);
            break;

        case ENUM:
            comboBoxModel.removeAllElements();
            Object[] cs =pref.valueClass.getEnumConstants();
            for (Object obj : cs) {
                comboBoxModel.addElement(obj);
            }
            comboBox.setSelectedItem(value);
            break;

        case FILE:
            FileSelector.ForWhat forWhat = pref.isForInputDir() ? ForWhat.INPUT_DIR : ForWhat.OUTPUT_DIR;
            fileSelector.initialise(title, (File) value, forWhat);
            
            break;
        
        case NUMBER:
            Number n = (Number) value;
            String fmt;
            if (Double.class == editingPref.valueClass) {
                fmt = "#"; //$NON-NLS-1$
            }
            else if (Float.class == editingPref.valueClass) {
                fmt = "%f"; //$NON-NLS-1$
            }
            else if (Integer.class == editingPref.valueClass) {
                fmt = "#"; //$NON-NLS-1$
            }
            else if (Long.class == editingPref.valueClass) {
                fmt = "0"; //$NON-NLS-1$
            }
            else {
                throw new RuntimeException(
                        "Unsupported: " + editingPref.valueClass.getName()); //$NON-NLS-1$
            }

            numberModel.setValue(n);
            numberSpinner.setEditor(new JSpinner.NumberEditor(
                    numberSpinner, fmt));
            break;
            
        case STRING:
            String str = (String) value;
            textField.setText(str==null ? "" : str); //$NON-NLS-1$
            break;

        default:
            errorLabel.setText("Unsupported: " + pref.valueClass.getName()); //$NON-NLS-1$
            break;
	    
	    }

	    
        cardLayout.show(cardPanel, card.name());
		label.setText(title);
	}
}
