/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui;

import com.codename1.cloud.BindTarget;
import com.codename1.compat.java.util.Objects;
import com.codename1.impl.CodenameOneImplementation;
import com.codename1.io.Log;
import com.codename1.ui.TextSelection.Span;
import com.codename1.ui.TextSelection.Spans;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ActionSource;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.LookAndFeel;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.ui.util.UITimer;
import java.io.IOException;
import java.util.ArrayList;

/**
 * <p>An optionally multi-line editable region that can display text and allow a user to edit it.
 * By default the text area will grow based on its content.<br>
 * {@code TextArea} is useful both for text input and for displaying multi-line data, it is used internally
 * by components such as {@link com.codename1.components.SpanLabel} &amp;  
 * {@link com.codename1.components.SpanButton}.</p>
 * 
 * <p>
 * {@code TextArea} &amp; {@link com.codename1.ui.TextField} are very similar, we discuss the main differences
 * between the two {@link com.codename1.ui.TextField here}.  In fact they are so similar that our sample code
 * below was written for {@link com.codename1.ui.TextField} but should be interchangeable with {@code TextArea}.
 * </p>
 * 
 * <script src="https://gist.github.com/codenameone/fb63dd5d6efdb95932be.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-text-component.png" alt="Text field input sample" />
 *
 * @author Chen Fishbein
 */
public class TextArea extends Component implements ActionSource, TextHolder {
    private EventDispatcher listeners = new EventDispatcher();
    private ActionListener doneListener;
    
    private static int defaultValign = TOP;

    /**
     * Indicates the default vertical alignment for a text field, only applies to single line text fields
     * @return the defaultValign
     */
    public static int getDefaultValign() {
        return defaultValign;
    }

    /**
     * Indicates the default vertical alignment for a text field, only applies to single line text fields
     * @param aDefaultValign the defaultValign to set
     */
    public static void setDefaultValign(int aDefaultValign) {
        defaultValign = aDefaultValign;
    }

    private int valign = defaultValign;

    private static int defaultMaxSize = 124;
    private static boolean autoDegradeMaxSize = false;
    private static boolean hadSuccessfulEdit = false;

    private int linesToScroll = 1;

    /**
     * Indicates the enter key to be used for editing the text area and by the
     * text field
     */
    private static final char ENTER_KEY = '\n';

    /**
     * Unsupported characters is a string that contains characters that cause issues 
     * when rendering on some problematic fonts. The rendering engine can thus remove them
     * when drawing.
     */
    private String unsupportedChars = "\t\r";

    /**
     * By default text area uses charWidth since its much faster on some devices
     * than string width. However, with some fonts and especially some languages (such
     * as Arabic, Korean etc.) the width of the string drawn might not equal the summary
     * of the width of the chars. Hence for portability to those languages/fonts this
     * flag must be set to true.
     */
    private static boolean useStringWidth;

    /**
     * Allows any type of input into a text field, if a constraint is not supported
     * by an underlying implementation this will be the default.
     */
    public static final int ANY = 0;

    /**
     * The user is allowed to enter an e-mail address.
     */
    public static final int EMAILADDR = 1;

    /**
     * The user is allowed to enter only an integer value.
     */
    public static final int NUMERIC = 2;

    /**
     * The user is allowed to enter a phone number.
     */
    public static final int PHONENUMBER = 3;

    /**
     * The user is allowed to enter a URL.
     */
    public static final int URL = 4;

    /**
     * The user is allowed to enter numeric values with optional decimal 
     * fractions, for example "-123", "0.123", or ".5".
     */
    public static final int DECIMAL = 5;
    
    /**
     * Indicates that the text entered is confidential data that should be 
     * obscured whenever possible.
     */
    public static final int PASSWORD = 0x10000;

    /**
     *  Indicates that editing is currently disallowed.
     */
    public static final int UNEDITABLE = 0x20000;

    /**
     * Indicates that the text entered is sensitive data that the 
     * implementation must never store into a dictionary or table for use 
     * in predictive, auto-completing, or other accelerated input schemes.
     */
    public static final int SENSITIVE = 0x40000;

    /**
     * Indicates that the text entered does not consist of words that are 
     * likely to be found in dictionaries typically used by predictive input 
     * schemes.
     */
    public static final int NON_PREDICTIVE= 0x80000;

    /**
     * This flag is a hint to the implementation that during text editing, 
     * the initial letter of each word should be capitalized.
     */
    public static final int INITIAL_CAPS_WORD = 0x100000;

    /**
     * This flag is a hint to the implementation that during text editing, 
     * the initial letter of each sentence should be capitalized.
     */
    public static final int INITIAL_CAPS_SENTENCE = 0x200000;
    //private int modifierFlag = 0x00000;
    
    /**
     * This flag is a hint to the implementation that this field contains
     * a username.
     */
    public static final int USERNAME = 0x400000;
    
    /**
     * This flag is a hint to the implementation that the text in this 
     * field should be upper case
     */
    public static final int UPPERCASE = 0x800000;
    
             
    /**
     * Input constraint which should be one of ANY, NUMERIC,
     * PHONENUMBER, URL or EMAIL
     */
    private int constraint = INITIAL_CAPS_SENTENCE;
    
    private  String text="";
    
    private  boolean editable = true;
    
    private int maxSize = defaultMaxSize ; //maximum size (number of characters) that can be stored in this TextField.
    
    private int rows = 1;
    
    private int columns = 3;
    
    private int growLimit = -1;
    
    private boolean endsWith3Points = false;

    /**
     * This flag indicates that the text area should try to act as a label and try to fix more accurately within it's bounds 
     * this might make it slower as a result
     */
    private boolean actAsLabel;
    
    
    // problematic  maxSize = 20; //maximum size (number of characters) that can be stored in this TextField.
    
    private ArrayList rowStrings;
    private int widthForRowCalculations = -1;

    private int rowsGap = 2;

    private boolean triggerClose;

    private EventDispatcher actionListeners = null;
    private EventDispatcher bindListeners = null;
    private EventDispatcher closeListeners = null;
    private String lastTextValue = "";
    
    /**
     * Indicates that the text area should "grow" in height based on the content beyond the
     * limits indicate by the rows variable
     */
    private boolean growByContent = true;

    /**
     * Indicates the widest character in the alphabet, this is useful for detecting
     * linebreaks internally. In CJK languages the widest char is different than W
     * hence this functionality is exposed to developers.
     */
    private static char widestChar = 'W';

    /**
     * Indicates whether this is a single line text area, in which case "growing" won't
     * work as expected.
     */
    private boolean singleLineTextArea;

    private int currentRowWidth;
    
    private Label hintLabel;

    /**
     * Creates an area with the given rows and columns
     * 
     * @param rows the number of rows
     * @param columns - the number of columns
     * @throws IllegalArgumentException if rows <= 0 or columns <= 1
     */
    public TextArea(int rows, int columns){
        this("", defaultMaxSize, rows, columns, ANY);
    }

    /**
     * Creates an area with the given rows, columns and constraint 
     * 
     * @param rows the number of rows
     * @param columns - the number of columns
     * @param constraint one of ANY, EMAILADDR, NUMERIC, PHONENUMBER, URL, DECIMAL
     * it can be bitwised or'd with one of PASSWORD, UNEDITABLE, SENSITIVE, NON_PREDICTIVE,
     * INITIAL_CAPS_SENTENCE, INITIAL_CAPS_WORD. E.g. ANY | PASSWORD.
     * @throws IllegalArgumentException if rows <= 0 or columns <= 1
     */
    public TextArea(int rows, int columns, int constraint){
        this("", defaultMaxSize, rows, columns, constraint);
    }
    
    /**
     * Creates an area with the given text, rows and columns
     * 
     * @param text the text to be displayed; if text is null, the empty 
     * string "" will be displayed
     * @param rows the number of rows
     * @param columns - the number of columns
     * @throws IllegalArgumentException if rows <= 0 or columns <= 1
     */
    public TextArea(String text, int rows, int columns){
        this(text,defaultMaxSize, rows, columns, ANY); //String , maxSize, constraints= 0 (ANY)
    }

    /**
     * Creates an area with the given text, rows, columns and constraint 
     * 
     * @param text the text to be displayed; if text is null, the empty 
     * string "" will be displayed
     * @param rows the number of rows
     * @param columns - the number of columns
     * @param constraint one of ANY, EMAILADDR, NUMERIC, PHONENUMBER, URL, DECIMAL
     * it can be bitwised or'd with one of PASSWORD, UNEDITABLE, SENSITIVE, NON_PREDICTIVE,
     * INITIAL_CAPS_SENTENCE, INITIAL_CAPS_WORD. E.g. ANY | PASSWORD.
     * @throws IllegalArgumentException if rows <= 0 or columns <= 1
     */
    public TextArea(String text, int rows, int columns, int constraint){
        this(text,defaultMaxSize, rows, columns, constraint); 
    }

    /**
     * Creates an area with the given text and maximum size, this constructor
     * will create a single line text area similar to a text field! 
     * 
     * @param text the text to be displayed; if text is null, the empty 
     * string "" will be displayed
     * @param maxSize text area maximum size
     */
    public TextArea(String text, int maxSize){
        this(text,maxSize, 1, 3, ANY);
    }

    /**
     * Creates an area with the given text, this constructor
     * will create a single line text area similar to a text field! 
     * 
     * @param text the text to be displayed; if text is null, the empty 
     * string "" will be displayed
     */
    public TextArea(String text) {
        this(text, Math.max(defaultMaxSize, nl(text)), 1, numCols(text), ANY);
    }

    /**
     * To work around race conditions in UI bindings (on Android at least), we want to 
     * send action events early.  Even the focusLost() event isn't early enough to ensure
     * that the action event is sent before an action event in a button that would trigger
     * focus lost.  We add this form press listener to the form when we add the textarea
     * and remove it when we remove the textarea.
     * 
     * Reference bug https://github.com/codenameone/CodenameOne/issues/2472
     */
    private final ActionListener formPressListener = new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
            Form f = getComponentForm();
            if (f != null) {
                if (isEditing() && f.getComponentAt(evt.getX(), evt.getY()) != TextArea.this) {
                    fireActionEvent();
                    setSuppressActionEvent(true);
                }
            }
        }
    };

    @Override
    protected void initComponent() {
        super.initComponent();
        Form f = getComponentForm();
        if (f != null) {
            // To be able to send action events early.
            // https://github.com/codenameone/CodenameOne/issues/2472
            f.addPointerPressedListener(formPressListener);
        }
    }

    @Override
    protected void deinitialize() {
        Form f = getComponentForm();
        if (f != null) {
            // For sending action events early
            // https://github.com/codenameone/CodenameOne/issues/2472
            f.removePointerPressedListener(formPressListener);
        }
        super.deinitialize();
    }

    private static int numCols(String t) {
        if(t == null) {
            return 3;
        }
        int s = t.length();
        if(s < 3) {
            return 3;
        }
        if(s > 80) {
            return 80;
        }
        return s;
    }
    
    private static int nl(String t) {
        if(t == null) return 0;
        return t.length();
    }
    
    /**
     * Creates an empty text area, this constructor
     * will create a single line text area similar to a text field! 
     */
    public TextArea() {
        this("");
    }
    
    /**
     * Creates an area with the given text, maximum size, rows, columns and constraint 
     * 
     * @param text the text to be displayed; if text is null, the empty 
     * string "" will be displayed
     * @param maxSize text area maximum size
     * @param rows the number of rows
     * @param columns - the number of columns
     * @param constraint one of ANY, EMAILADDR, NUMERIC, PHONENUMBER, URL, DECIMAL
     * it can be bitwised or'd with one of PASSWORD, UNEDITABLE, SENSITIVE, NON_PREDICTIVE,
     * INITIAL_CAPS_SENTENCE, INITIAL_CAPS_WORD. E.g. ANY | PASSWORD.
     * @throws IllegalArgumentException if rows <= 0 or columns <= 1
     */
    private TextArea(String text, int maxSize, int rows, int columns, int constraint){
        setUIID("TextArea");
        setPreferredTabIndex(0);
        this.maxSize = maxSize;
        setText(text);
        setConstraint(constraint);
        if(rows <= 0){
            throw new IllegalArgumentException("rows must be positive");
        }
        if(columns <= 1 && rows != 1){
            throw new IllegalArgumentException("columns must be larger than 1");
        }
        this.rows = rows;
        this.columns = columns;
        setCursor(Component.TEXT_CURSOR);
    }

    /**
     * {@inheritDoc}
     */
    protected void initLaf(UIManager uim) {
        super.initLaf(uim);
        setSelectCommandText(uim.localize("edit", "Edit"));
        LookAndFeel laf = uim.getLookAndFeel();
        setSmoothScrolling(laf.isDefaultSmoothScrolling());
    }
    
    
    /**
     * Sets the constraint which provides a hint to the virtual keyboard input, notice this <b>doesn't</b>
     * limit input type in any way!
     * 
     * @param constraint one of ANY, EMAILADDR, NUMERIC, PHONENUMBER, URL, DECIMAL
     * it can be bitwised or'd with one of PASSWORD, UNEDITABLE, SENSITIVE, NON_PREDICTIVE,
     * INITIAL_CAPS_SENTENCE, INITIAL_CAPS_WORD. E.g. ANY | PASSWORD.
     */
    public void setConstraint(int constraint) {
        this.constraint = constraint;
    }


    /**
     * Returns the editing constraint value
     * 
     * @return the editing constraint value
     * @see #setConstraint
     */
    public int getConstraint() {
        return constraint;
    }

    /**
     * {@inheritDoc}
     */
    public void setWidth(int width) {
        if(width != getWidth()) {
            rowStrings = null;
            if(growByContent) {
                setShouldCalcPreferredSize(true);
            }
        }
        super.setWidth(width);
    }

    
    /**
     * Sets the text within this text area
     * 
     * @param t new value for the text area
     */
    public void setText(String t) {
        String old = this.text;
        if (t != null ? !t.equals(old) : old != null) {
            // If we've previously suppressed action events,
            // we need to unsuppress them upon the text changing again.
            setSuppressActionEvent(false);
        }
        this.text = (t != null) ? t : "";
        setShouldCalcPreferredSize(true);
        if(maxSize < text.length()) {
            maxSize = text.length() + 1;
        }
        
        synchronized(this) {
            //zero the ArrayList in order to initialize it on the next paint
            rowStrings=null; 
        }
        if (!Objects.equals(text, old)) {
            fireDataChanged(DataChangedListener.CHANGED, -1);
        }
        // while native editing we don't need the cursor animations
        if(Display.getInstance().isNativeInputSupported() && Display.getInstance().isTextEditing(this)) {
            if (!text.equals(old)) {
                Display.impl.updateNativeEditorText(this, text);
            }
            return;
        }
        repaint();
    }

    /**
     * Returns the text in the text area
     * 
     * @return the text in the text area
     */
    public String getText() {
        return text;
    }
    
    /**
     * Convenience method for numeric text fields, returns the value as a number or invalid if the value in the 
     * text field isn't a number
     * @param invalid in case the text isn't an integer this number will be returned
     * @return the int value of the text field
     */
    public int getAsInt(int invalid) {
        try {
            return Integer.parseInt(text);
        } catch(NumberFormatException e) {
            return invalid;
        }
    }
    
    /**
     * Convenience method for numeric text fields, returns the value as a number or invalid if the value in the 
     * text field isn't a number
     * @param invalid in case the text isn't a long this number will be returned
     * @return the long value of the text field
     */
    public long getAsLong(long invalid) {
        try {
            return Long.parseLong(text);
        } catch(NumberFormatException e) {
            return invalid;
        }
    }

    /**
     * Convenience method for numeric text fields, returns the value as a number or invalid if the value in the 
     * text field isn't a number
     * @param invalid in case the text isn't an double this number will be returned
     * @return the double value of the text field
     */
    public double getAsDouble(double invalid) {
        try {
            return Double.parseDouble(text);
        } catch(NumberFormatException e) {
            return invalid;
        }
    }
    
    /**
     * Returns true if this area is editable
     * 
     * @return true if this area is editable
     */
    public boolean isEditable() {
        return editable;
    }

    @Override
    public int getPreferredTabIndex() {
        if (isEditable()) {
            return super.getPreferredTabIndex();
        }
        return -1;
    }

    private void updateCursor() {
        setCursor(isEditable() || isTextSelectionEnabled() ? TEXT_CURSOR : DEFAULT_CURSOR);
    }
    
    /**
     * Sets this text area to be editable or readonly
     * 
     * @param b true is text are is editable; otherwise false
     */
    public void setEditable(boolean b) {
        editable = b;
        updateCursor();
    }

    /**
     * Returns the maximum size for the text area
     * 
     * @return the maximum size for the text area
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Sets the maximum size of the text area
     * 
     * @param maxSize the maximum size of the text area
     */
    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }
    
    /**
     * {@inheritDoc}
     */
    public void keyPressed(int keyCode) {
        super.keyPressed(keyCode);
        setSuppressActionEvent(false);
        int action = com.codename1.ui.Display.getInstance().getGameAction(keyCode);

        // this works around a bug where fire is also a softkey on devices such as newer Nokia
        // series 40's (e.g. the Nokia emulator). It closes its native text box on fire then
        // as a result of a Nokia bug we get the key released of that closing and assume the
        // users wants to edit the text... When means the only way to exit the native text box
        // is via the cancel option (after pressing OK once).
        triggerClose = action == Display.GAME_FIRE;

        //scroll the TextArea
        Rectangle rect = new Rectangle(getScrollX(), getScrollY(), getWidth(), getHeight());
        Font textFont = getStyle().getFont();
        if(action == Display.GAME_DOWN){
            if((getScrollY() + getHeight()) <(rowsGap + getStyle().getFont().getHeight()) * getLines()) {
                rect.setY(rect.getY() + (textFont.getHeight() + rowsGap) * linesToScroll);
                scrollRectToVisible(rect, this);
            } else {
                setHandlesInput(false);
            }
        } else {
            if(action == Display.GAME_UP){
                if(getScrollY() > 0) {
                    rect.setY(Math.max(0, rect.getY() - (textFont.getHeight() + rowsGap) * linesToScroll));
                    scrollRectToVisible(rect, this);
                } else {
                    setHandlesInput(false);
                }
            }
        }
        if(action == Display.GAME_RIGHT || action == Display.GAME_LEFT){
            setHandlesInput(false);
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    protected void fireClicked() {
        onClick();
    }
    
    /**
     * {@inheritDoc}
     */
    protected boolean isSelectableInteraction() {
        return editable;
    }

    
    
    private boolean isTypedKey(int code) {
        if (true) return code > 0;
        
        return (code >= 48 && code <= 90) // 0-9a-z
                || 
                (code >= 96 && code <= 111) // number pad and arithmetic
                ||
                (code >= 186 && code <= 192) // punctuation
                || 
                (code >= 219 && code <= 222); // brackets & quotes
    }
    
    /**
     * {@inheritDoc}
     */
    public void keyReleased(int keyCode) {
        int action = com.codename1.ui.Display.getInstance().getGameAction(keyCode);
        if(isEditable()){
            // this works around a bug where fire is also a softkey on devices such as newer Nokia
            // series 40's
            if (triggerClose && (action == Display.GAME_FIRE || isEnterKey(keyCode))) {
                triggerClose = false;
                onClick();
                return;
            }
            Display d = Display.getInstance();
            if(action == 0 && isTypedKey(keyCode)) {
                //registerAsInputDevice();
                Display.getInstance().editString(this, getMaxSize(), getConstraint(), getText(), keyCode);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isScrollableY() {
        return isFocusable() && getScrollDimension().getHeight() > getHeight();
    }

    void deinitializeImpl() {
        super.deinitializeImpl(); 
        Display.getInstance().stopEditing(this);
    }

    
        
    void onClick(){
        if(isEditable()) {
            editString();
        }
    }
        
    void editString() {
        if(autoDegradeMaxSize && (!hadSuccessfulEdit) && (maxSize > 1024)) {
            try {
                //registerAsInputDevice();
                Display.getInstance().editString(this, getMaxSize(), getConstraint(), getText());
            } catch(IllegalArgumentException err) {
                maxSize -= 1024;
                setDefaultMaxSize(maxSize);
                editString();
            }
        } else {
            //registerAsInputDevice();
            Display.getInstance().editString(this, getMaxSize(), getConstraint(), getText());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void pointerHover(int[] x, int[] y) {
        if (!Display.getInstance().isDesktop()) {
            requestFocus();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void pointerHoverReleased(int[] x, int[] y) {
        if (!Display.getInstance().isDesktop()) {
            requestFocus();
        }
    }

    boolean showLightweightVKB() {
        return false;
    }
    
    /**
     * {@inheritDoc}
     */
    public void pointerReleased(int x, int y) {
        // prevent a drag operation from going into edit mode
        if (isDragActivated()) {
            super.pointerReleased(x, y);
        } else {
            super.pointerReleased(x, y);
            if (isEditable() && isEnabled() && !isCellRenderer()) {
                if(Display.impl.isNativeInputImmediate()) {
                    editString();
                    return;
                }
                if (Display.getInstance().isTouchScreenDevice()) {
                    if(showLightweightVKB() || !(Display.getInstance().getDefaultVirtualKeyboard() instanceof Dialog)) {
                        if (!Display.getInstance().isVirtualKeyboardShowing()) {
                            Display.getInstance().setShowVirtualKeyboard(true);
                        }
                    } else {
                        onClick();                    
                    }
                } else {
                    onClick();
                }
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    void focusGainedInternal() {
        setSuppressActionEvent(false);
        super.focusGainedInternal();
        setHandlesInput(isScrollableY());
        
    }

    /**
     * {@inheritDoc}
     */
    void focusLostInternal() {
        super.focusLostInternal();
        setHandlesInput(false);
        if (isEditing()) {
            fireActionEvent();
            setSuppressActionEvent(true);
        }
    }
    
    /**
     * Returns the number of columns in the text area
     * 
     * @return the number of columns in the text area
     */
    public int getColumns() {
        return columns;
    }
    
    /**
     * Returns the number of actual rows in the text area taking into consideration
     * growsByContent
     * 
     * @return the number of rows in the text area
     */
    public int getActualRows() {
        if(growByContent) {
            if(growLimit > -1) {
                return Math.min(Math.max(rows, getLines()), growLimit);
            }
            return Math.max(rows, getLines());
        }
        return rows;
    }
    
    /**
     * Returns the number of rows in the text area
     * 
     * @return the number of rows in the text area
     */
    public int getRows() {
        return rows;
    }
    
    /**
     * Sets the number of columns in the text area
     * 
     * @param columns number of columns
     */
    public void setColumns(int columns) {
        setShouldCalcPreferredSize(true);
        this.columns = columns;
    }
    
    /**
     * Sets the number of rows in the text area
     * 
     * @param rows number of rows
     */
    public void setRows(int rows) {
        setShouldCalcPreferredSize(true);
        this.rows = rows;
    }
    
    void initComponentImpl() {
        getRowStrings();
        super.initComponentImpl();
    }
    
    private ArrayList getRowStrings() {
        if(rowStrings == null || widthForRowCalculations != getWidth() - getUnselectedStyle().getHorizontalPadding()){
            initRowString();
            setShouldCalcPreferredSize(true);
        }
        return rowStrings;
    }
    
    
    /**
     * Returns the number of text lines in the TextArea
     * 
     * @return the number of text lines in the TextArea
     */
    public int getLines(){
        int retVal;
        ArrayList v = getRowStrings();
        retVal = v.size();
        return retVal;
    }
    
    /**
     * Returns the text in the given row of the text box
     * 
     * @param line the line number in the text box
     * @return the text of the line
     */
    public String getTextAt(int line){
        ArrayList rowsV = getRowStrings();
        int size = rowsV.size();
        if(size == 0){
            return "";
        }
        if(line >= size){
            return (String)rowsV.get(size-1);        
        }            
        return (String)rowsV.get(line);
    }
    
    private int indexOf(char[] t, char c, int offset, int length) {
        int tlen = t.length;
        for(int iter = offset ; iter < tlen && iter < offset+length; iter++) {
            if(t[iter] == c) {
                return iter;
           }
       }
       return -1;
   }

    
    /**
     * Override this to modify the text for rendering in cases of invalid characters 
     * for display, this method allows the developer to replace such characters e.g.:
     * replace "\\t" with 4 spaces
     * 
     * @param text the text to process
     * @return the given string as a processed char array ready for rendering
     */
    protected char[] preprocess(String text) {
        return text.toCharArray();
    }

    private int updateRowWidth(char c, Font font) {
        currentRowWidth += font.charWidth(c);
        return currentRowWidth;
    }
    
    private int updateRowWidth(String str, Font font) {
        currentRowWidth += font.stringWidth(str);
        return currentRowWidth;
    }

    private boolean fastCharWidthCheck(char[] chrs, int off, int length, int width, int charWidth, Font f) {
        if(length * charWidth < width) {
            return true;
        }
        length = Math.min(chrs.length, length);
        return f.charsWidth(chrs, off, length) < width;
    }

    private void initRowString() {
        if(!Display.getInstance().isEdt()) {
            if(rowStrings == null) {
                rowStrings = new ArrayList();
                rowStrings.add(getText());
                return;
            }
        }        
        Style style = getUnselectedStyle();
        rowStrings= new ArrayList();
        widthForRowCalculations = getWidth() - style.getHorizontalPadding();
        // single line text area is essentially a text field, we call the method
        // to allow subclasses to override it
        if (isSingleLineTextArea()) {
            rowStrings.add(getText());
            return;
        }
        if (widthForRowCalculations <= 0) {
            rowStrings.add(getText());
            setShouldCalcPreferredSize(true);
            return;
        }
        if(text == null || text.equals("")){
            return;
        }
        Font font = style.getFont();
        if(actAsLabel && text.length() <= columns && text.indexOf('\n') < 0) {
            int w = font.stringWidth(text);
            if(w <= getWidth()) {
                if(rowStrings == null) {
                    rowStrings = new ArrayList();
                    rowStrings.add(getText());
                    return;
                } else {
                    rowStrings.clear();
                    rowStrings.add(getText());
                    return;
                }
            }
        }
        char[] text = preprocess(getText());
        int rows = this.rows;
        if(growByContent) {
            rows = Math.max(rows, getLines());
        }
        
        int charWidth = font.charWidth(widestChar);
        Style selectedStyle = getSelectedStyle();
        if(selectedStyle.getFont() != style.getFont()) {
            int cw = selectedStyle.getFont().charWidth(widestChar);
            if(cw > charWidth) {
                charWidth = cw;
                font = selectedStyle.getFont();
            }
        }
        style = getStyle();
        int tPadding = style.getHorizontalPadding();
        int textAreaWidth = getWidth() - tPadding;
        /*if(textAreaWidth <= 0) {
            if(columns < 1) {
                textAreaWidth = Math.min(Display.getInstance().getDisplayWidth() - tPadding, getText().length()) * charWidth;
            } else {
                textAreaWidth = Math.min(Display.getInstance().getDisplayWidth() - tPadding, columns) * charWidth;
            }
        }*/
        if(textAreaWidth <= charWidth) {
            if(!isInitialized()) {
                rowStrings.add(getText());
            } else {
                // special case for the edge case of "no room".
                // Its important since sometimes this case occurs in the GUI builder by accident
                int tlen = text.length;
                for(int iter = 0 ; iter < tlen ; iter++) {
                    rowStrings.add("" + text[iter]);
                }
            }
            return;
        }
        
        int minCharactersInRow = Math.max(1, textAreaWidth / charWidth);
        int from=0;
        int to=from+minCharactersInRow;
        int textLength=text.length;
        String rowText = null;
        int i,spaceIndex;
        
        // if there is any possibility of a scrollbar we need to reduce the textArea
        // width to accommodate it
        if(textLength / minCharactersInRow > Math.max(2, rows)) {
            textAreaWidth -= getUIManager().getLookAndFeel().getVerticalScrollWidth();
            textAreaWidth -= charWidth/2;
        }
        String unsupported = getUnsupportedChars();
        
        /*
        iteration over the string using indexes, from - the beginning of the row , to - end of a row
        for each row we will try to search for a "space" character at the end of the row ( row is text area available width)
        indorder to improve the efficiency we do not search an entire row but we start from minCharactersInRow which indicates
        what is the minimum amount of characters that can feet in the text area width.
        if we dont find we will go backwards and search for the first space available,
        if there is no space in the entire row we will cut the line inorder to fit in.
         */

        //Don't rely on the fact that short text has no newline character. we always have to parse the text.
        to = Math.max( Math.min(textLength-1,to), 0 );
        while(to<textLength) {
            if(to>textLength){
                to=textLength;
            }

            spaceIndex=-1;
            rowText="";
            int maxLength = to;

            if(useStringWidth || actAsLabel) {
                // fix for an infinite loop issue: http://forums.java.net/jive/thread.jspa?messageID=482802
                //currentRowWidth = 0;
                String currentRow = "";
                
                // search for "space" character at close as possible to the end of the row
                for( i=to; i < textLength && fastCharWidthCheck(text, from, i - from + 1, textAreaWidth, charWidth, font) ; i++){
                    char c = text[i];
                    /*if(updateRowWidth(c, font) >= textAreaWidth) {
                        break;
                    }*/
                    currentRow+=c;
                    if (i < textLength-1 && Character.isSurrogatePair(c, text[i+1]) ) {
                        // Surrogate pairs (e.g. emojis) shouldn't be split up.
                        currentRow+=text[++i];
                        maxLength+=2;
                        if(font.stringWidth(currentRow) >= textAreaWidth) {
                            break;
                        }
                        continue;
                    }
                    if(font.stringWidth(currentRow) >= textAreaWidth) {
                        break;
                    }
                    if(unsupported.indexOf(c) > -1) {
                        text[i] = ' ';
                        c = ' ';
                    }
                    if(c == ' ' || c == '\n') {
                        spaceIndex=i;
                        // newline has been found. We can end the loop here as the line cannot grow more
                        if (c == '\n')
                            break;
                    }
                    maxLength++;
                }
            } else {
                currentRowWidth = 0;
                if(to != from) {
                    currentRowWidth = font.charsWidth(text, from, to - from);
                }

                // search for "space" character at close as possible to the end of the row
                for( i=to; i < textLength ; i++){
                    char c = text[i];
                    if (i < textLength-1 && Character.isSurrogatePair(c, text[i+1]) ) {
                        // Surrogate pairs (e.g. emojis) shouldn't be split up.
                        
                        String testStr = new String(new char[]{text[i], text[i+1]});
                        
                        i++;
                        if (updateRowWidth(testStr, font) >= textAreaWidth) {
                            break;
                        }
                        maxLength+=2;
                        
                        continue;
                    }
                    if(updateRowWidth(c, font) >= textAreaWidth) {
                        break;
                    }
                    if(unsupported.indexOf(c) > -1) {
                        text[i] = ' ';
                        c = ' ';
                    }
                    if(c == ' ' || c == '\n') {
                        spaceIndex=i;
                        // newline has been found. We can end the loop here as the line cannot grow more
                        if (c == '\n')
                            break;
                    }
                    maxLength++;
                }
            }
            // if we got to the end of the text use the entire row,
            // also if space is next character (in the next row) we can cut the line
            if(i == textLength || text[i] == ' ' || text[i] == '\n') {
                spaceIndex=i;
            }

            // if we found space in the limit width of the row (searched only from minCharactersInRow)
            if(spaceIndex!=-1){
                // make sure that if we have a newline character before the end of the line we should
                // break there instead
                int newLine = indexOf(text, '\n', from, spaceIndex - from);
                if(newLine > -1 && newLine < spaceIndex) {
                    spaceIndex = newLine;
                }

                rowText = new String(text, from, spaceIndex - from);
                from=spaceIndex+1;

            } // if there is no space from minCharactersInRow to limit need to search backwards
            else{
                for( i=to; spaceIndex==-1 && i>=from ; i--){
                    char chr = text[i];
                    if(chr == ' ' || chr == '\n' || chr == '\t') {
                        spaceIndex=i;
                        
                        // don't forget to search for line breaks in the
                        // remaining part. otherwise we overlook possible
                        // line breaks!
                        int newLine = indexOf(text, '\n', from, i - from);
                        if(newLine > -1 && newLine < spaceIndex) {
                           spaceIndex = newLine;
                        }
                        rowText = new String(text, from, spaceIndex - from);
                        from=spaceIndex+1;
                    }

                }
                if(spaceIndex==-1) {
                    // from = to + 1;
                    if(maxLength <= 0) {
                        maxLength = 1;
                    }
                    spaceIndex = maxLength;
                    if (spaceIndex > 0 && spaceIndex < textLength && Character.isSurrogatePair(text[spaceIndex-1], text[spaceIndex])) {
                        // Make sure the space index isn't on the 2nd char of a surrogate pair (e.g. for emojis).
                        spaceIndex++;
                        maxLength++;
                    }
                    rowText = new String(text, from, spaceIndex - from);
                    from = spaceIndex;
                }
            }
            if(rowText.length() == 0) {
                // This happens due to a race condition or something, no idea why???
                if(textAreaWidth <= charWidth) {
                    if(!isInitialized()) {
                        rowStrings.add(getText());
                    } else {
                        // special case for the edge case of "no room".
                        // Its important since sometimes this case occurs in the GUI builder by accident
                        int tlen = text.length;
                        for(int iter = 0 ; iter < tlen ; iter++) {
                            rowStrings.add("" + text[iter]);
                        }
                    }
                    return;
                }
            }
            rowStrings.add(rowText);
            //adding minCharactersInRow doesn't work if what is left is less
            //then minCharactersInRow
            to=from;//+minCharactersInRow;
        }
        if(text[text.length -1 ] == '\n'){
            rowStrings.add("");
        }
    }
    
    /**
     * Gets the num of pixels gap between the rows
     * 
     * @return the gap between rows in pixels
     */
    public int getRowsGap() {
        return rowsGap;
    }

    /**
     * The gap in pixels between rows
     * 
     * @param rowsGap num of pixels to gap between rows
     */
    public void setRowsGap(int rowsGap) {
        this.rowsGap = rowsGap;
    }

    /**
     * {@inheritDoc}
     */
    public void paint(Graphics g) {

        if(Display.getInstance().isNativeEditorVisible(this)) {
            if (!Display.impl.nativeEditorPaintsHint()) {
                paintHint(g);
            }
            return;
        }
        
        getUIManager().getLookAndFeel().drawTextArea(g, this);
        paintHint(g);
    }

    void paintHint(Graphics g) {
        if(Display.getInstance().isNativeEditorVisible(this) && Display.impl.nativeEditorPaintsHint()) {
            return;
        }
        super.paintHint(g);
    }
    
    /**
     * {@inheritDoc}
     */
    protected Dimension calcPreferredSize(){
        if(shouldShowHint()) {
            Label l = getHintLabelImpl();
            if(l != null) {
                Dimension d1 = getUIManager().getLookAndFeel().getTextAreaSize(this, true);
                Dimension d2 = l.getPreferredSize();
                return new Dimension(Math.max(d1.getWidth(), d2.getWidth()), Math.max(d1.getHeight(), d2.getHeight()));
            }
        }
        return getUIManager().getLookAndFeel().getTextAreaSize(this, true);
    }
        
    /**
     * {@inheritDoc}
     */
    protected Dimension calcScrollSize(){
        return getUIManager().getLookAndFeel().getTextAreaSize(this, false);
    }
        
    /**
     * Add an action listener which is invoked when the text area was modified not during
     * modification. A text <b>field</b> might never fire an action event if it is edited
     * in place and the user never leaves the text field!
     * 
     * @param a actionListener
     */
    public void addActionListener(ActionListener a) {
        if(actionListeners == null) {
            actionListeners = new EventDispatcher();
        }
        actionListeners.addListener(a);
    }

    /**
     * Removes an action listener
     * 
     * @param a actionListener
     */
    public void removeActionListener(ActionListener a) {
        if(actionListeners == null) {
            return;
        }
        actionListeners.removeListener(a);
        if(!actionListeners.hasListeners()) {
            actionListeners = null;
        }
    }
    
    /**
     * Flag to indicate whether the action event is suppressed.
     * FocusLost will trigger an action event if editing is in progress, 
     * and then set this flag.
     * The flag should be unset on focus gained, and on start editing.
     */
    private boolean suppressActionEvent;
    
    /**
     * Since the action event is triggered on the end of editing, and that may not
     * happen until a couple of EDT cycles after the onFocus event, we want to be 
     * able to fire the action event in focus lost, and then suppress the normal
     * action event that would be fired on editing end.  We use this flag to
     * suppress action events.
     * @param suppress 
     */
    void setSuppressActionEvent(boolean suppress) {
        suppressActionEvent = suppress;
        
    }
    
    /**
     * Checks to see if the action event is suppressed.
     * @return 
     */
    boolean isSuppressActionEvent() {
        return suppressActionEvent;
    }
    
    /**
     * Notifies listeners of a change to the text area
     */
    void fireActionEvent() {
        if (suppressActionEvent) {
            return;
        }
        if(actionListeners != null) {
            ActionEvent evt = new ActionEvent(this,ActionEvent.Type.Edit);
            actionListeners.fireActionEvent(evt);
        }
        if(bindListeners != null) {
            String t = getText();
            bindListeners.fireBindTargetChange(this, "text", lastTextValue, t);
            lastTextValue = t;
        }
    }
    
    /**
     * Adds a listener to be called with this TextArea is "closed".  I.e. when it is
     * no longer the active virtual input device for the form.
     * @param l 
     * @see Form#setCurrentInputDevice(com.codename1.ui.VirtualInputDevice) 
     */
    public void addCloseListener(ActionListener l) {
        if (closeListeners == null) {
            closeListeners = new EventDispatcher();
        }
        closeListeners.addListener(l);
    }
    
    /**
     * Removes close listener.
     * @param l 
     * @see #addCloseListener(com.codename1.ui.events.ActionListener) 
     * @see Form#setCurrentInputDevice(com.codename1.ui.VirtualInputDevice) 
     */
    public void removeCloseListener(ActionListener l) {
        if (closeListeners != null && closeListeners.hasListeners()) {
            closeListeners.removeListener(l);
            if (!closeListeners.hasListeners()) {
                closeListeners = null;
            }
        }
    }
    
    /**
     * Fires a close event.  This is fired when the TextArea is no longer the active
     * virtual input device for the form.
     * 
     * @see Form#setCurrentInputDevice(com.codename1.ui.VirtualInputDevice) 
     */
    void fireCloseEvent() {
        if (closeListeners != null && closeListeners.hasListeners()) {
            ActionEvent evt = new ActionEvent(this);
            closeListeners.fireActionEvent(evt);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    void onEditComplete(String text) {
        if (!Display.getInstance().getImplementation().isAsyncEditMode()) {
            setText(text);
        }
        if(getParent() != null) {
            getParent().revalidate();
        }
    }
    
    /**
     * Sets the default limit for the native text box size
     * 
     * @param value default value for the size of the native text box
     */
    public static void setDefaultMaxSize(int value) {
        defaultMaxSize = value;
    }

    /**
     * Indicates that the text area should "grow" in height based on the content beyond the
     * limits indicate by the rows variable
     * 
     * @return true if the text component should grow and false otherwise
     */
    public boolean isGrowByContent() {
        return growByContent;
    }

    /**
     * Indicates that the text area should "grow" in height based on the content beyond the
     * limits indicate by the rows variable
     * 
     * @param growByContent true if the text component should grow and false otherwise
     */
    public void setGrowByContent(boolean growByContent) {
        this.growByContent = growByContent;
    }
    
    /**
     * Indicates whether a high value for default maxSize will be reduced to a lower
     * value if the underlying platform throws an exception.
     * 
     * @param value new value for autoDegradeMaxSize
     */
    public static void setAutoDegradeMaxSize(boolean value) {
        autoDegradeMaxSize = value;
    }

    /**
     * Indicates whether a high value for default maxSize will be reduced to a lower
     * value if the underlying platform throws an exception.
     * 
     * @return value for autoDegradeMaxSize
     */
    public static boolean isAutoDegradeMaxSize() {
        return autoDegradeMaxSize;
    }

    /**
     * Unsupported characters is a string that contains characters that cause issues 
     * when rendering on some problematic fonts. The rendering engine can thus remove them
     * when drawing.
     * 
     * @return unsupported characters string
     */
    public String getUnsupportedChars() {
        return unsupportedChars;
    }

    /**
     * Unsupported characters is a string that contains characters that cause issues 
     * when rendering on some problematic fonts. The rendering engine can thus remove them
     * when drawing.
     * 
     * @param unsupportedChars the unsupported character string
     */
    public void setUnsupportedChars(String unsupportedChars) {
        this.unsupportedChars = unsupportedChars;
    }


    /**
     * Indicates the number of lines to scroll with every scroll operation
     * 
     * @return number bigger or equal to 1
     */
    public int getLinesToScroll() {
        return linesToScroll;
    }

    /**
     * Indicates the number of lines to scroll with every scroll operation
     * 
     * @param linesToScroll number bigger or equal to 1
     */
    public void setLinesToScroll(int linesToScroll) {
        if (linesToScroll < 1) {
            throw new IllegalArgumentException("lines to scroll has to be >= 1");
        }
        this.linesToScroll = linesToScroll;
    }

    /**
     * Indicates the widest character in the alphabet, this is useful for detecting
     * linebreaks internally. In CJK languages the widest char is different than W
     * hence this functionality is exposed to developers.
     * 
     * @param widestC the widest character
     */
    public static void setWidestChar(char widestC) {
        widestChar = widestC;
    }


    /**
     * Indicates the widest character in the alphabet, this is useful for detecting
     * linebreaks internally. In CJK languages the widest char is different than W
     * hence this functionality is exposed to developers.
     * 
     * @return the widest character
     */
    public static char getWidestChar() {
        return widestChar;
    }

    /**
     * Indicates whether this is a single line text area, in which case "growing" won't
     * work as expected.
     *
     * @param singleLineTextArea set to true to force a single line text
     */
    public void setSingleLineTextArea(boolean singleLineTextArea) {
        this.singleLineTextArea = singleLineTextArea;
    }

    /**
     * Indicates whether this is a single line text area, in which case "growing" won't
     * work as expected.
     *
     * @return  true if this is a single line text area
     */
    public boolean isSingleLineTextArea() {
        return singleLineTextArea;
    }

    /**
     * Sets the Alignment of the TextArea to one of: CENTER, LEFT, RIGHT
     *
     * @param align alignment value
     * @see #CENTER
     * @see #LEFT
     * @see #RIGHT
     * @deprecated use Style.setAlignment instead
     */
    public void setAlignment(int align) {
        getAllStyles().setAlignment(align);
    }

    /**
     * Returns the alignment of the TextArea
     *
     * @return the alignment of the TextArea one of: CENTER, LEFT, RIGHT
     * @see #CENTER
     * @see #LEFT
     * @see #RIGHT
     * @deprecated use Style.getAlignment instead
     */
    public int getAlignment() {
        return getStyle().getAlignment();
    }

    /**
     * Returns the absolute alignment of the TextArea
     * In RTL LEFT alignment is actually RIGHT, but this method returns the actual alignment
     *
     * @return the alignment of the TextArea one of: CENTER, LEFT, RIGHT
     * @see #CENTER
     * @see #LEFT
     * @see #RIGHT
     * @deprecated this method is redundant and no longer used
     */
    public int getAbsoluteAlignment(){
        int a = getAlignment();
        if(isRTL()) {
            switch(a) {
                case RIGHT:
                    return LEFT;
                case LEFT:
                    return RIGHT;
            }
        }
        return a;
    }

    /**
     * Returns true if the text field is waiting for a commit on editing
     *
     * @return true if a commit is pending
     */
    public boolean isPendingCommit() {
        return false;
    }

    /**
     * Returns the position of the cursor char position
     *
     * @return the cursor position
     */
    public int getCursorPosition() {
        return -1;
    }
    
    /**
     * Returns the position of the cursor line position
     * 
     * @return the cursor line position
     */
    public int getCursorY() {
        return -1;
    }    

    /**
     * Returns the position of the cursor char position in the current line.
     * 
     * @return the cursor char position in the current line
     */
    public int getCursorX() {
        return -1;
    }
    
    /**
     * True is this is a qwerty device or a device that is currently in
     * qwerty mode.
     *
     * @return currently defaults to false
     */
    public boolean isQwertyInput() {
        return false;
    }

    /**
     * Returns the currently selected input mode
     *
     * @return the display name of the input mode by default the following modes
     * are supported: Abc, ABC, abc, 123
     */
    public String getInputMode() {
        return null;
    }

    /**
     * Returns the order in which input modes are toggled
     *
     * @return the order of the input modes
     */
    public String[] getInputModeOrder() {
        return null;
    }

    /**
     * Indicates whether text field input should scroll to the right side when no
     * more room for the input is present.
     *
     * @return true if scrolling is enabled
     */
    public boolean isEnableInputScroll() {
        return false;
    }

    /**
     * Indicates the enter key to be used for editing the text area and by the
     * text field
     *
     * @param keyCode the key tested
     */
    protected boolean isEnterKey(int keyCode) {
        return keyCode == ENTER_KEY;
    }

    /**
     * Searches the given string for the widest character using char width, this operation should only
     * be performed once and it solves cases where a devices language might have a char bigger than 'W'
     * that isn't consistently bigger.
     * Notice that this method will use the TextArea style font which might differ when switching themes etc.
     *
     * @param s string to search using charWidth
     */
    public static void autoDetectWidestChar(String s) {
        Font f = UIManager.getInstance().getComponentStyle("TextArea").getFont();
        int widest = 0;
        int slen = s.length();
        for(int iter = 0 ; iter < slen ; iter++) {
            char c = s.charAt(iter);
            int w = f.charWidth(c);
            if(w > widest) {
                widest = w;
                setWidestChar(c);
            }
        }
    }

    /**
     * By default text area uses charWidth since its much faster on some devices
     * than string width. However, with some fonts and especially some languages (such
     * as Arabic, Korean etc.) the width of the string drawn might not equal the summary
     * of the width of the chars. Hence for portability to those languages/fonts this
     * flag must be set to true.
     *
     * @return the value of useStringWidth
     */
    public static boolean isUseStringWidth() {
        return useStringWidth;
    }

    /**
     * By default text area uses charWidth since its much faster on some devices
     * than string width. However, with some fonts and especially some languages (such
     * as Arabic, Korean etc.) the width of the string drawn might not equal the summary
     * of the width of the chars. Hence for portability to those languages/fonts this
     * flag must be set to true.
     *
     * @param aUseStringWidth the new value for useStringWidth
     */
    public static void setUseStringWidth(boolean aUseStringWidth) {
        useStringWidth = aUseStringWidth;
    }
    
    /**
     * Sets the TextArea hint text, the hint text  is displayed on the TextArea
     * When there is no text in the TextArea
     * 
     * @param hint the hint text to display
     */
    public void setHint(String hint){
        super.setHint(hint, getHintIcon());
    }

    /**
     * Returns the hint text
     *
     * @return the hint text or null
     */
    public String getHint() {
        return super.getHint();
    }

    /**
     * Sets the TextArea hint icon, the hint is displayed on the TextArea
     * When there is no text in the TextArea
     *
     * @param icon the icon
     */
    public void setHintIcon(Image icon){
        setHint(getHint(), icon);
    }

    /**
     * Returns the hint icon
     *
     * @return the hint icon
     */
    public Image getHintIcon() {
        return super.getHintIcon();
    }

    /**
     * Sets the TextArea hint text and Icon, the hint text and icon are 
     * displayed on the TextArea when there is no text in the TextArea
     * 
     * @param hint the hint text to display
     * @param icon the hint icon to display
     */
    public void setHint(String hint, Image icon){
        super.setHint(hint, icon);
    }

    /**
     * Returns the hint label component that can be customized directly
     * @return hint label component
     */
    public Label getHintLabel() {
        return getHintLabelImpl();
    }
    
    Label getHintLabelImpl() {
        return hintLabel;
    }

    void setHintLabelImpl(Label hintLabel) {
        this.hintLabel = hintLabel;
    }

    boolean shouldShowHint() {
        return getText().equals("");
    }

    /**
     * Sets the vertical alignment of the text field to one of: CENTER, TOP, BOTTOM<br>
     * only applies to single line text field
     * 
     * <p><strong>NOTE:</strong> If the text area is an editable, multi-line text field,
     * and the platform doesn't support vertical alignment with its native text editor,
     * then {@link #getVerticalAlignment() } will always return {@link Component#TOP}, no
     * matter what value you set here.  Currently no platforms support
     * vertical alignment of multiline text areas.</p>
     *
     * @param valign alignment value
     * @see #CENTER
     * @see #TOP
     * @see #BOTTOM
     */
    public void setVerticalAlignment(int valign) {
        if(valign != CENTER && valign != TOP && valign != BOTTOM){
            throw new IllegalArgumentException("Alignment can't be set to " + valign);
        }
        this.valign = valign;
    }

    /**
     * Returns the vertical alignment of the text field, this only applies to single line text field
     * 
     * <p><strong>NOTE:</strong> If the text area is an editable, multi-line text field,
     * and the platform doesn't support vertical alignment with its native text editor,
     * then this will always return {@link Component#TOP}.  Currently no platforms support
     * vertical alignment of multiline text areas.</p>
     *
     *
     * @return the vertical alignment of the TextField one of: CENTER, TOP, BOTTOM
     * @see #CENTER
     * @see #TOP
     * @see #BOTTOM
     */
    public int getVerticalAlignment(){
        if (valign != TOP && !isSingleLineTextArea() && isEditable() && !Display.impl.supportsNativeTextAreaVerticalAlignment()) {
            // If this is a multiline text field, then most platforms don't support
            // vertical alignment in their native text areas, so it looks bad
            // if the lightweight rendering is in the middle, and then the native is
            // aligned top.
            // This is not a perfect solution (forcing it to top in such cases),
            // but it is better than alternatives.
            return TOP;
        }
        return valign;
    }
    
    /**
     * {@inheritDoc}
     */
    public String[] getBindablePropertyNames() {
        return new String[] {"text"};
    }
    
    /**
     * {@inheritDoc}
     */
    public Class[] getBindablePropertyTypes() {
        return new Class[] {String.class};
    }
    
    /**
     * {@inheritDoc}
     */
    public void bindProperty(String prop, BindTarget target) {
        if(prop.equals("text")) {
            if(bindListeners == null) {
                bindListeners = new EventDispatcher();
            }
            bindListeners.addListener(target);
            return;
        }
        super.bindProperty(prop, target);
    }
    
    /**
     * {@inheritDoc}
     */
    public void unbindProperty(String prop, BindTarget target) {
        if(prop.equals("text")) {
            if(bindListeners == null) {
                return;
            }
            bindListeners.removeListener(target);
            if(!bindListeners.hasListeners()) {
                bindListeners = null;
            }
            return;
        }
        super.unbindProperty(prop, target);
    }
    
    /**
     * {@inheritDoc}
     */
    public Object getBoundPropertyValue(String prop) {
        if(prop.equals("text")) {
            return getText();
        }
        return super.getBoundPropertyValue(prop);
    }

    /**
     * {@inheritDoc}
     */
    public void setBoundPropertyValue(String prop, Object value) {
        if(prop.equals("text")) {
            if(value == null) {
                setText("");
            } else {
                setText((String)value);
            }
            return;
        }
        super.setBoundPropertyValue(prop, value);
    }

    /**
     * Indicates the maximum number of rows in a text area after it has grown, -1 indicates no limit
     * @return the growLimit
     */
    public int getGrowLimit() {
        return growLimit;
    }

    /**
     * Indicates the maximum number of rows in a text area after it has grown, -1 indicates no limit
     * @param growLimit the growLimit to set
     */
    public void setGrowLimit(int growLimit) {
        this.growLimit = growLimit;
    }
    
    /**
     * If the TextArea text is too long to fit the text to the widget this will add "..."
     * at the last displayable row. This flag is only applicable when there is a grow limit on the TextArea.
     * E.g. a TextArea with potentially 10 rows can be displayed in 4 rows where the last row can be truncated 
     * and end with 3 points. By default this is set to false
     * 
     * @param endsWith3Points true if text should add "..." at the end
     */
    public void setEndsWith3Points(boolean endsWith3Points){
        this.endsWith3Points = endsWith3Points;
    }

    /**
     * If the TextArea text is too long to fit the text to the widget this will add "..."
     * at the last displayable row. This flag is only applicable when there is a grow limit on the TextArea.
     * E.g. a TextArea with potentially 10 rows can be displayed in 4 rows where the last row can be truncated 
     * and end with 3 points. By default this is set to false
     * 
     * @return true if this TextArea adds "..." when the text is too long
     */
    public boolean isEndsWith3Points() {
        return endsWith3Points;
    }
    
    private static class TextAreaInputDevice implements VirtualInputDevice {
        private TextArea editedTextArea;
        private boolean deferStopEditingToNativeLayer;
        private boolean enabled = true;

        TextAreaInputDevice(TextArea ta) {
            editedTextArea = ta;
        }


        public void close() throws Exception {
            if (!enabled) {
                return;
            }
            editedTextArea.fireCloseEvent();
            if (deferStopEditingToNativeLayer) {
                return;
            }
            if (editedTextArea.isEditing()) {
                editedTextArea.stopEditing();
            }
        }
        
    }
    
    /**
     * Registers this TextArea as the current input device for the current form.
     * 
     * @deprecated Don't call this method directly, unless you really know what you're doing.  It is used
     * primarily by implementation APIs.
     */
    public void registerAsInputDevice() {
        final TextArea cmp = this;
        Form f = this.getComponentForm();
        
        if (f != null && Display.impl.getEditingText() != this) {
            try {
                TextAreaInputDevice previousInput = null;
                if (f.getCurrentInputDevice() instanceof TextAreaInputDevice) {
                    previousInput = (TextAreaInputDevice)f.getCurrentInputDevice();
                    if (previousInput.editedTextArea == this) {
                        // If the previous input is the same input, let's disable it's close 
                        // handler altogether.
                        previousInput.enabled = false;
                    }
                    
                }
                if (previousInput != null) {
                    previousInput.deferStopEditingToNativeLayer = true;
                }
                TextAreaInputDevice currInput = new TextAreaInputDevice(this);
                f.setCurrentInputDevice(currInput);
                
                
            } catch (Exception ex) {
                Log.e(ex);
                // Failed to edit string because the previous input device would not
                // give up control
                return;
            }
        }
    }
    
    /**
     * Launches the text field editing, notice that calling this in a callSerially is generally considered good practice
     */
    public void startEditing() {
        if(!Display.getInstance().isTextEditing(this)) {
            final TextArea cmp = this;
            //registerAsInputDevice();
            Display.getInstance().editString(this, maxSize, constraint, text);
        }
    }

    /**
     * Launches the text field editing in a callserially call
     */
    @Override
    public void startEditingAsync() {
        if(!Display.getInstance().isTextEditing(this)) {
            if (Display.impl.usesInvokeAndBlockForEditString()) {
                // Implementations that use invokeAndBlock for edit string
                // need to have the existing text area's editing stopped 
                // before starting a new edit session or the previous text
                // field won't be updated until the next one is finished editing.
                Component c = Display.impl.getEditingText();
                if (c != this && c != null) {
                    if (c instanceof TextArea) {
                        //System.out.println("Stopping editing");
                        ((TextArea)c).stopEditing();
                        final TextArea ta = (TextArea)c;
                        UITimer.timer(30, false, new Runnable() {
                            public void run() {
                                ta.repaint();
                                //registerAsInputDevice();
                                Display.getInstance().editString(TextArea.this, maxSize, constraint, text);
                            }
                        });
                        return;
                    }
                }
            }
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    //registerAsInputDevice();
                    Display.getInstance().editString(TextArea.this, maxSize, constraint, text);
                }
            });
        }
    }
    
    /**
     * Indicates whether we are currently editing this text area
     * @return true if Display.getInstance().isTextEditing(this)
     */
    @Override
    public boolean isEditing() {
        return Display.getInstance().isTextEditing(this);
    }
    
    /**
     * Stops text editing of this field if it is being edited
     */
    public void stopEditing() {
        if(isEditing()) {
            Display.getInstance().stopEditing(this);
        }
    }

    /**
     * Stops text editing of this field if it is being edited
     * @param onFinish invoked when editing stopped
     */
    @Override
    public void stopEditing(Runnable onFinish) {
        if(isEditing()) {
            Display.getInstance().stopEditing(this, onFinish);
        } else {
            if (onFinish != null) {
                onFinish.run();
            }
        }
    }

    /**
     * {@inheritDoc}
     * We override get style here to return the selected style when editing
     * @return the selected style if editing, <code>super.getStyle()</code> otherwise
     */
    @Override
    public Style getStyle() {
        if(isEditing()) {
            return getSelectedStyle();
        }
        return super.getStyle(); 
    }
    
    /**
     * Adds a listener for data change events it will be invoked for every change
     * made to the text field, notice most platforms will invoke only the 
     * DataChangedListener.CHANGED event
     * 
     * @param d the listener
     */
    public void addDataChangedListener(DataChangedListener d) {
        listeners.addListener(d);
    }

    /**
     * Removes the listener for data change events 
     * 
     * @param d the listener
     */
    public void removeDataChangedListener(DataChangedListener d) {
        listeners.removeListener(d);
    }
    
    /**
     * Adds a listener for data change events it will be invoked for every change
     * made to the text field, notice most platforms will invoke only the 
     * DataChangedListener.CHANGED event
     * 
     * @param d the listener
     * @deprecated use #addDataChangedListener(DataChangedListener) instead
     */
    public void addDataChangeListener(DataChangedListener d) {
        listeners.addListener(d);
    }

    /**
     * Removes the listener for data change events 
     * 
     * @param d the listener
     * @deprecated use #removeDataChangedListener(DataChangedListener) instead
     */
    public void removeDataChangeListener(DataChangedListener d) {
        listeners.removeListener(d);
    }
    
    /**
     * Alert the TextField listeners the text has been changed on the TextField
     * @param type the event type: Added, Removed or Change
     * @param index cursor location of the event
     */
    public void fireDataChanged(int type, int index) {
        if(listeners != null) {
            listeners.fireDataChangeEvent(index, type);
        }
    }
    

    /**
     * Sets a Done listener on the TextField - notice this listener will be called
     * only on supported platforms that supports done action on the keyboard
     * 
     * @param l the listener
     */
    public void setDoneListener(ActionListener l) {
        doneListener = l;
    }

    /**
     * Gets the done listener of this TextField.
     * 
     * @return the done listener or null if not exists
     */ 
    public ActionListener getDoneListener() {
        return doneListener;
    }
    
    /**
     * Fire the done event to done listener
     */ 
    public void fireDoneEvent() {
        fireDoneEvent(-1);
    }
    public void fireDoneEvent(final int keyEvent) {
        if (doneListener != null) {
            if (!Display.getInstance().isEdt()) {
                Display.getInstance().callSerially(new Runnable() {

                    public void run() {
                        fireDoneEvent(keyEvent);
                    }
                });
                return;
            }
            doneListener.actionPerformed(new ActionEvent(this,ActionEvent.Type.Done,keyEvent));
        }
    }

    /**
     * This flag indicates that the text area should try to act as a label and try to fix more accurately within it's bounds
     * this might make it slower as a result
     * @return the actAsLabel
     */
    public boolean isActAsLabel() {
        return actAsLabel;
    }

    /**
     * This flag indicates that the text area should try to act as a label and try to fix more accurately within it's bounds
     * this might make it slower as a result
     * @param actAsLabel the actAsLabel to set
     */
    public void setActAsLabel(boolean actAsLabel) {
        this.actAsLabel = actAsLabel;
    }

    /**
     * Special case for text components, if they are editing they should always render the selected state
     * {@inheritDoc}
     * @return true if editing
     */
    protected boolean shouldRenderComponentSelection() {
        return isEditing() || super.shouldRenderComponentSelection();
    }
    
    /**
     * Calculates the spans for the the given text selection.  This should generally
     * just delegate to the appropriate method in the look and feel for performing the
     * layout calculation.
     * @param sel The TextSelection
     * @return 
     * @since 7.0
     */
    protected Spans calculateTextSelectionSpan(TextSelection sel) {
        return getUIManager().getLookAndFeel().calculateTextAreaSpan(sel, TextArea.this);
    }
    
    private boolean textSelectionEnabled;
    
    /**
     * Enables text selection on this TextArea.  Text selection must also be enabled on the Form in order to
     * text selection to be activated.
     * @param enabled 
     * @see #setTextSelectionEnabled(boolean) 
     * @see Form#getTextSelection() 
     * @see TextSelection#setEnabled(boolean) 
     * @since 7.0
     */
    public void setTextSelectionEnabled(boolean enabled) {
        this.textSelectionEnabled = enabled;
        updateCursor();
    }
    
    /**
     * Returns true if text selection is enabled on this label.  Default is {@literal false}.  To enable text selection,
     * you must enable text selection on the Form with {@link Form#getTextSelection() } and {@link TextSelection#setEnabled(boolean) },
     * and also ensure that the label's text selection is enabled via {@link #setTextSelectionEnabled(boolean) }.
     * @return 
     * @see #setTextSelectionEnabled(boolean) 
     * @since 7.0
     */
    public boolean isTextSelectionEnabled() {
        return textSelectionEnabled;
    }
    
    
    private TextSelection.Spans span;
    private TextSelection.TextSelectionSupport textSelectionSupport;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public TextSelection.TextSelectionSupport getTextSelectionSupport() {
        if (textSelectionSupport == null) {
            textSelectionSupport = new TextSelection.TextSelectionSupport() {

                public TextSelection.Spans getTextSelectionForBounds(TextSelection sel, Rectangle bounds) {
                    span = calculateTextSelectionSpan(sel);
                    if (span == null) {
                        return sel.newSpans();
                    }
                    
                    TextSelection.Spans result = span.getIntersection(bounds, true);
                    TextSelection.Spans out = sel.newSpans();
                    out.add(result);
                    return out;
                            
                }


                public boolean isTextSelectionEnabled(TextSelection sel) {
                    return (!isEditable() && textSelectionEnabled)|| (isEditable() && !isEnabled());
                }


                public boolean isTextSelectionTriggerEnabled(TextSelection sel) {
                    return (!isEditable() && textSelectionEnabled)|| (isEditable() && !isEnabled());
                }


                public TextSelection.Span triggerSelectionAt(TextSelection sel, int x, int y) {
                    span = getUIManager().getLookAndFeel().calculateTextAreaSpan(sel, TextArea.this);
                    if (span.isEmpty()) {
                        return null;
                    }
                    TextSelection.Char charAtPoint = span.charAt(x, y);
                    if (charAtPoint == null) {
                        return null;
                    }
                    Span sp = span.spanOfCharAt(x, y);
                    int startPos = charAtPoint.getPosition();
                    int endPos = charAtPoint.getPosition()+1;
                    String dividers = " \t\r\n-.;";
                    
                    while (startPos > sp.first().getPosition()) {
                        if (dividers.indexOf(TextArea.this.getText().substring(startPos, startPos+1)) < 0) {
                            startPos--;
                        } else {
                            if (startPos < sp.last().getPosition()) {
                                startPos++;
                            }
                            break;
                        }
                    }
                    
                    while (endPos < sp.last().getPosition()+1) {
                        if (dividers.indexOf(TextArea.this.getText().substring(endPos-1, endPos))<0) {
                            endPos++;
                        } else {
                            if (endPos > sp.first().getPosition()) {
                                endPos--;
                            }
                            break;
                        }
                    }
                    
                    return sp.subspan(startPos, endPos);
                }


                public String getTextForSpan(TextSelection sel, TextSelection.Span span) {
                    int offset = span.getStartPos();
                    offset = Math.max(0, offset);
                    offset = Math.min(getText().length()-1, offset);
                    int end = span.getEndPos()+1;
                    end = Math.min(getText().length(), end);
                    return getText().substring(offset, end);
                    
                }
                
            };
        }
        return textSelectionSupport;
    };

}
