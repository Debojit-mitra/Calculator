package com.bunny.tools.scientific_calculator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bunny.tools.scientific_calculator.updater.AppUpdater;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView lastDisplay, intermediateResult, noHistoryTextView;
    private EditText numDisplay;
    private StringBuilder currentInput;
    private String lastOperation;
    private BigDecimal lastResult;
    private LinearLayout displayLayout;
    private ImageView btnHistory;
    private static final String KEY_CURRENT_INPUT = "currentInput";
    private static final String KEY_LAST_OPERATION = "lastOperation";
    private static final String KEY_LAST_RESULT = "lastResult";
    private static final String KEY_LAST_DISPLAY = "lastDisplay";
    private boolean isRadianMode = false;
    private boolean isInverseMode = false;
    private static final String KEY_IS_RADIAN_MODE = "isRadianMode";
    private HorizontalScrollView numDisplayScrollView;
    private int cursorPosition = 0;
    //history
    private RecyclerView historyRecyclerView;
    private HistoryAdapter historyAdapter;
    private List<String> calculationHistory;
    private static final int MAX_HISTORY_SIZE = 8;
    private static final String PREF_CALCULATION_HISTORY = "calculationHistory";
    private AppUpdater appUpdater;
    public AppUpdater.UpdateReceiver updateReceiver;
    public static final int REQUEST_INSTALL_PACKAGES = 1001;
    private boolean updateCheckPerformed = false;
    //long press btnDel
    private final Handler longPressHandler = new Handler(Looper.getMainLooper());
    private static final long INITIAL_DELAY = 500; // milliseconds
    private static final long REPEAT_DELAY = 50; // milliseconds
    private boolean isLongPressing = false;

    private static final Set<String> FUNCTIONS = new HashSet<>(Arrays.asList(
            "sin", "cos", "tan", "log", "ln", "√", "asin", "acos", "atan"
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        initializeHistoryView();
        if (savedInstanceState != null) {
            updateCheckPerformed = savedInstanceState.getBoolean("updateCheckPerformed", false);
            restoreState(savedInstanceState);
        } else {
            if (!updateCheckPerformed) {
                appUpdater.checkForUpdates(false);
                updateCheckPerformed = true;
            }
            currentInput = new StringBuilder();
            lastOperation = "";
            lastResult = BigDecimal.ZERO;
            loadHistoryFromPreferences();  // Load history here
        }
        updateDisplay();
    }

    @SuppressLint({"ClickableViewAccessibility", "UnspecifiedRegisterReceiverFlag"})
    private void initializeViews() {
        appUpdater = new AppUpdater(MainActivity.this);
        lastDisplay = findViewById(R.id.lastDisplay);
        numDisplay = findViewById(R.id.numDisplay);
        intermediateResult = findViewById(R.id.intermediateResult);
        numDisplayScrollView = findViewById(R.id.numDisplayScrollView);

        noHistoryTextView = findViewById(R.id.noHistoryTextView);
        displayLayout = findViewById(R.id.displayLayout);

        btnHistory = findViewById(R.id.btnHistory);
        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> toggleHistoryView());
        } else {
            Log.e("MainActivity", "btnHistory not found in layout");
        }

        if (numDisplay != null) {
            numDisplay.setCursorVisible(true);
            numDisplay.requestFocus();
            numDisplay.setSelection(numDisplay.getText().length());
            numDisplay.setShowSoftInputOnFocus(false);

            numDisplay.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.requestFocus();
                    numDisplay.setCursorVisible(true);
                    return false;
                }
                return false;
            });
            numDisplay.setOnClickListener(null);
        }

        Button radButton = findViewById(R.id.btnRad);
        Button degButton = findViewById(R.id.btnDeg);

        if (radButton != null && degButton != null) {
            setButtonColor(radButton, isRadianMode);
            setButtonColor(degButton, !isRadianMode);

            radButton.setOnClickListener(this);
            degButton.setOnClickListener(this);
        } else {
            Log.e("MainActivity", "Rad or Deg button not found in layout");
        }

        int[] buttonIds = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
                R.id.btnDot, R.id.btnPlus, R.id.btnMinus, R.id.btnMultiply, R.id.btnDivide, R.id.btnEquals,
                R.id.btnAc, R.id.btnDel, R.id.btnPercentage, R.id.btnParenthesesFirst, R.id.btnParenthesesSecond,
                R.id.btnRootOver, R.id.btnSquare, R.id.btnSin, R.id.btnCos, R.id.btnTan, R.id.btnInv,
                R.id.btnLog, R.id.btnLn, R.id.btnPi, R.id.btnE, R.id.btnExponent, R.id.btnFactorial
        };

        for (int id : buttonIds) {
            View view = findViewById(id);
            if (view != null) {
                view.setOnClickListener(this);
            } else {
                Log.e("MainActivity", "Button not found: " + getResources().getResourceEntryName(id));
            }

            Button btnDel = findViewById(R.id.btnDel);
            if (btnDel != null) {
                btnDel.setOnClickListener(this);
                btnDel.setOnLongClickListener(v -> {
                    isLongPressing = true;
                    longPressHandler.postDelayed(deleteRunnable, INITIAL_DELAY);
                    return true;
                });
                btnDel.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                        isLongPressing = false;
                        longPressHandler.removeCallbacks(deleteRunnable);
                    }
                    return false;
                });
            }
        }

        updateReceiver = new AppUpdater.UpdateReceiver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateReceiver, new IntentFilter(AppUpdater.INSTALL_ACTION), RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(updateReceiver, new IntentFilter(AppUpdater.INSTALL_ACTION));
        }
    }

    private void initializeHistoryView() {
        historyRecyclerView = findViewById(R.id.historyRecyclerView);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        calculationHistory = new ArrayList<>();
        historyAdapter = new HistoryAdapter(
                calculationHistory,
                this::onHistoryItemClick,
                this::onClearHistoryClick
        );
        historyRecyclerView.setAdapter(historyAdapter);
    }

    private void toggleHistoryView() {
        if (displayLayout.getVisibility() == View.VISIBLE) {
            fadeOutView(displayLayout, () -> {
                displayLayout.setVisibility(View.GONE);
                updateHistoryVisibility();
                historyRecyclerView.smoothScrollToPosition(0);
            });
        } else {
            View viewToFadeOut = calculationHistory.isEmpty() ? noHistoryTextView : historyRecyclerView;
            fadeOutView(viewToFadeOut, () -> {
                historyRecyclerView.setVisibility(View.GONE);
                noHistoryTextView.setVisibility(View.GONE);
                fadeInView(displayLayout);
                numDisplay.requestFocus();
            });
        }
    }

    private void onHistoryItemClick(String calculation, List<String> fullHistory, int position) {
        String[] parts = calculation.split("=");
        if (parts.length == 2) {
            String input = parts[0].trim();
            String result = parts[1].trim();

            currentInput = new StringBuilder(input);
            numDisplay.setText(result);
            intermediateResult.setText("");
            cursorPosition = input.length();

            // Check if there's a previous calculation and if it's linked
            if (position + 1 < fullHistory.size()) {
                String previousCalculation = fullHistory.get(position + 1);
                String[] previousParts = previousCalculation.split("=");
                if (previousParts.length == 2) {
                    String previousResult = previousParts[1].trim();
                    // Check if the previous result is used in the current calculation
                    if (input.contains(previousResult)) {
                        lastDisplay.setText(previousCalculation);
                    } else {
                        lastDisplay.setText("");
                    }
                } else {
                    lastDisplay.setText("");
                }
            } else {
                lastDisplay.setText("");
            }
        }
        toggleHistoryView();
        updateDisplay();
    }

    private void onClearHistoryClick() {
        calculationHistory.clear();
        historyAdapter.clearHistory();
        saveHistoryToPreferences();
        updateHistoryVisibility();
        new Handler(Looper.getMainLooper()).postDelayed(() -> btnHistory.performClick(), 400);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                hideKeyboard(v);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onClick(View view) {
        if (!(view instanceof Button)) {
            return;
        }

        Button button = (Button) view;
        String buttonText = button.getText().toString();

        // Get current cursor position
        cursorPosition = numDisplay.getSelectionStart();

        int id = button.getId();
        if (id == R.id.btnRad || id == R.id.btnDeg) {
            toggleAngleMode(button);
            return;
        }

        switch (buttonText) {
            case "AC":
                clearAll();
                break;
            case "⌫":
                if (!isLongPressing) {
                    deleteLastChar();
                }
                break;
            case "=":
                calculateResult();
                break;
            case "+":
            case "-":
            case "×":
            case "/":
            case "^":
            case "%":
                handleOperator(buttonText);
                break;
            case "RAD":
            case "DEG":
                toggleAngleMode(button);
                return;
            case "INV":
                toggleInverseMode(button);
                return;
            case "sin":
            case "cos":
            case "tan":
            case "asin":
            case "acos":
            case "atan":
            case "log":
            case "ln":
            case "√":
            case "10^":
            case "e^":
            case "^2":
                handleFunction(buttonText);
                break;
            case "x²":
                appendToInput("²");
                break;
            case "π":
            case "e":
                handleConstant(buttonText);
                break;
            case "!":
                handleFactorial();
                break;
            case "(":
            case ")":
            default:
                appendToInput(buttonText);
                break;
        }

        updateDisplay();
    }

    private void clearAll() {
        currentInput.setLength(0);
        lastOperation = "";
        lastResult = BigDecimal.ZERO;
        lastDisplay.setText("");
        intermediateResult.setText("");
        cursorPosition = 0;
        updateDisplay();
    }

    private void deleteLastChar() {
        if (currentInput.length() > 0 && cursorPosition > 0) {
            // Check if we're deleting a function name
            String functionToDelete = getFunctionToDelete();
            if (!functionToDelete.isEmpty()) {
                currentInput.delete(cursorPosition - functionToDelete.length(), cursorPosition);
                cursorPosition -= functionToDelete.length();
            } else {
                currentInput.deleteCharAt(cursorPosition - 1);
                cursorPosition--;
            }
            updateDisplay();
        }
    }

    private String getFunctionToDelete() {
        String[] functions = {"sin(", "cos(", "tan(", "asin(", "acos(", "atan(", "log(", "ln(", "√("};
        for (String function : functions) {
            if (cursorPosition >= function.length() &&
                    currentInput.substring(cursorPosition - function.length(), cursorPosition).equals(function)) {
                return function;
            }
        }
        return "";
    }

    private void calculateResult() {
        try {
            String expression = currentInput.toString();

            // Add closing parenthesis if missing
            int openParenCount = (int) expression.chars().filter(ch -> ch == '(').count();
            int closeParenCount = (int) expression.chars().filter(ch -> ch == ')').count();
            if (openParenCount > closeParenCount) {
                expression += ")".repeat(openParenCount - closeParenCount);
                currentInput.append(")".repeat(openParenCount - closeParenCount));
            }

            ExpressionEvaluator.setModes(isRadianMode, isInverseMode);
            BigDecimal result = ExpressionEvaluator.evaluateExpression(expression);
            expression = expression + " = ";
            lastDisplay.setText(expression);
            String formattedResult = formatNumber(result);
            String calculation = expression + formattedResult;
            numDisplay.setText(formattedResult);
            scrollNumDisplayToEnd();
            lastResult = result;
            currentInput.setLength(0);
            currentInput.append(formattedResult);  // Make sure this line is present
            intermediateResult.setText("");
            cursorPosition = formattedResult.length();
            // Add to history
            addToHistory(calculation);
            saveHistoryToPreferences();
        } catch (Exception e) {
            numDisplay.setText(R.string.error);
            scrollNumDisplayToEnd();
            intermediateResult.setText("");
            cursorPosition = 0;
        }
        updateDisplay();
    }

    private void addToHistory(String calculation) {
        calculationHistory.add(0, calculation);
        if (calculationHistory.size() > MAX_HISTORY_SIZE) {
            calculationHistory.remove(calculationHistory.size() - 1);
        }
        historyAdapter.updateHistory(calculationHistory);
        saveHistoryToPreferences();
    }

    private void updateHistoryVisibility() {
        if (calculationHistory.isEmpty()) {
            historyRecyclerView.setVisibility(View.GONE);
            noHistoryTextView.setVisibility(View.VISIBLE);
            displayLayout.setVisibility(View.GONE);
        } else {
            historyRecyclerView.setVisibility(View.VISIBLE);
            noHistoryTextView.setVisibility(View.GONE);
        }
    }


    private void handleOperator(String operator) {
        if (currentInput.length() == 0 && operator.equals("-")) {
            // Allow minus sign at the beginning for negative numbers
            currentInput.insert(cursorPosition, operator);
            cursorPosition += operator.length();
        } else if (currentInput.length() > 0) {
            char charBeforeCursor = cursorPosition > 0 ? currentInput.charAt(cursorPosition - 1) : ' ';
            char charAtCursor = cursorPosition < currentInput.length() ? currentInput.charAt(cursorPosition) : ' ';

            // Check if the number is negative
            boolean isNegative = cursorPosition == currentInput.length() && currentInput.charAt(0) == '-';

            if (Character.isDigit(charBeforeCursor) || charBeforeCursor == ')' || charBeforeCursor == 'π' || charBeforeCursor == 'e' || charBeforeCursor == '²' || charBeforeCursor == '%' || isNegative) {
                currentInput.insert(cursorPosition, operator);
                cursorPosition += operator.length();
            } else if (isOperator(charBeforeCursor) && !operator.equals("²")) {
                // Replace the operator before the cursor, unless it's '²' or '%'
                currentInput.setCharAt(cursorPosition - 1, operator.charAt(0));
            } else if (isOperator(charAtCursor) && !operator.equals("²")) {
                // Replace the operator at the cursor, unless it's '²' or '%'
                currentInput.setCharAt(cursorPosition, operator.charAt(0));
                cursorPosition++;
            } else {
                // Insert the operator at the cursor position
                currentInput.insert(cursorPosition, operator);
                cursorPosition += operator.length();
            }
        } else if (lastResult.compareTo(BigDecimal.ZERO) != 0 && !operator.equals("²")) {
            currentInput.insert(cursorPosition, formatNumber(lastResult) + operator);
            cursorPosition += formatNumber(lastResult).length() + operator.length();
        }
        lastOperation = operator;
        updateDisplay();
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '×' || c == '/' || c == '^' || c == '%';
    }

    private void handleFunction(String actualFunction) {
        if (currentInput.length() > 0) {
            char charBeforeCursor = cursorPosition > 0 ? currentInput.charAt(cursorPosition - 1) : ' ';
            if (Character.isDigit(charBeforeCursor) || charBeforeCursor == ')' || charBeforeCursor == 'π' || charBeforeCursor == 'e' || charBeforeCursor == '%') {
                currentInput.insert(cursorPosition, "×");
                cursorPosition++;
            }
        }

        currentInput.insert(cursorPosition, actualFunction);
        cursorPosition += actualFunction.length();
        if (!actualFunction.equals("^2") && !actualFunction.equals("10^") && !actualFunction.equals("e^")) {
            currentInput.insert(cursorPosition, "(");
            cursorPosition++;
        }
        updateDisplay();
    }

    private void handleConstant(String constant) {
        if (currentInput.length() > 0) {
            char charBeforeCursor = cursorPosition > 0 ? currentInput.charAt(cursorPosition - 1) : ' ';
            if (Character.isDigit(charBeforeCursor) || charBeforeCursor == ')' || charBeforeCursor == 'π' || charBeforeCursor == 'e') {
                currentInput.insert(cursorPosition, "×");
                cursorPosition++;
            }
        }
        currentInput.insert(cursorPosition, constant);
        cursorPosition += constant.length();
        Log.d("Calculator", "Current input after adding constant: " + currentInput.toString());
        updateDisplay();
    }

    private void handleFactorial() {
        if (currentInput.length() > 0) {
            currentInput.insert(cursorPosition, "!");
            cursorPosition++;
        }
        updateDisplay();
    }

    private void appendToInput(String value) {
        if (value.equals("(")) {
            if (cursorPosition > 0) {
                char charBeforeCursor = currentInput.charAt(cursorPosition - 1);
                if (Character.isDigit(charBeforeCursor) || charBeforeCursor == 'π' || charBeforeCursor == 'e' || charBeforeCursor == ')' || charBeforeCursor == '%' || charBeforeCursor == '²') {
                    currentInput.insert(cursorPosition, "×");
                    cursorPosition++;
                }
            }
            currentInput.insert(cursorPosition, value);
            cursorPosition++;
        } else {
            if (currentInput.length() > 0 && cursorPosition > 0) {
                char charBeforeCursor = currentInput.charAt(cursorPosition - 1);
                if ((Character.isDigit(charBeforeCursor) || charBeforeCursor == 'π' || charBeforeCursor == 'e' || charBeforeCursor == ')' || charBeforeCursor == '%' || charBeforeCursor == '²') &&
                        (value.equals("π") || value.equals("e") || FUNCTIONS.contains(value))) {
                    currentInput.insert(cursorPosition, "×");
                    cursorPosition++;
                } else if (charBeforeCursor == '%' && Character.isDigit(value.charAt(0))) {
                    // Insert "×" when a number follows "%"
                    currentInput.insert(cursorPosition, "×");
                    cursorPosition++;
                }
            }
            currentInput.insert(cursorPosition, value);
            cursorPosition += value.length();
        }
        updateDisplay();
    }

    private void toggleAngleMode(Button clickedButton) {
        isRadianMode = clickedButton.getId() == R.id.btnRad;
        Button radButton = findViewById(R.id.btnRad);
        Button degButton = findViewById(R.id.btnDeg);

        Log.d("Calculator", "Toggling angle mode. RAD mode: " + isRadianMode);

        setButtonColor(radButton, isRadianMode);
        setButtonColor(degButton, !isRadianMode);

        // Force button state change
        radButton.setPressed(isRadianMode);
        degButton.setPressed(!isRadianMode);
        radButton.setSelected(isRadianMode);
        degButton.setSelected(!isRadianMode);

        // Force immediate draw
        radButton.invalidate();
        degButton.invalidate();

        ExpressionEvaluator.setModes(isRadianMode, isInverseMode);
        updateIntermediateResult();
    }

    private void toggleInverseMode(Button button) {
        isInverseMode = !isInverseMode;
        setButtonColor(button, isInverseMode);
        updateFunctionButtonLabels();
        ExpressionEvaluator.setModes(isRadianMode, isInverseMode);
        updateIntermediateResult();
    }

    private void setButtonColor(Button button, boolean isActive) {
        if (button != null) {
            @ColorInt int backgroundColor = isActive ?
                    getThemeColor(com.google.android.material.R.attr.colorTertiaryContainer) :
                    getThemeColor(com.google.android.material.R.attr.colorPrimaryContainer);

            button.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        }
    }


    @ColorInt
    private int getThemeColor(int colorAttr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(colorAttr, typedValue, true);
        return typedValue.data;
    }

    private void updateFunctionButtonLabels() {
        updateButtonText(R.id.btnSin, isInverseMode ? "asin" : "sin");
        updateButtonText(R.id.btnCos, isInverseMode ? "acos" : "cos");
        updateButtonText(R.id.btnTan, isInverseMode ? "atan" : "tan");
        updateButtonText(R.id.btnLog, isInverseMode ? "10^" : "log");
        updateButtonText(R.id.btnLn, isInverseMode ? "e^" : "ln");
        updateButtonText(R.id.btnSquare, isInverseMode ? "√" : "x²");
    }

    private void updateButtonText(int buttonId, String text) {
        Button button = findViewById(buttonId);
        if (button != null) {
            button.setText(text);
        } else {
            Log.e("MainActivity", "Button not found: " + getResources().getResourceEntryName(buttonId));
        }
    }

    private void updateDisplay() {
        String displayText = currentInput.toString();
        numDisplay.setText(displayText);
        numDisplay.setSelection(cursorPosition);
        scrollNumDisplayToEnd();
        updateIntermediateResult();
    }

    private void updateIntermediateResult() {
        try {
            String expression = currentInput.toString();
            if (isIncompleteExpression(expression)) {
                ExpressionEvaluator.setModes(isRadianMode, isInverseMode);
                BigDecimal result = ExpressionEvaluator.evaluateExpression(expression);
                String formattedResult = formatNumber(result);
                intermediateResult.setText(formattedResult);
            } else {
                intermediateResult.setText("");
            }
        } catch (Exception e) {
            intermediateResult.setText("");
        }
    }

    private boolean isIncompleteExpression(String expression) {
        if (expression.isEmpty()) {
            return false;
        }

        // Check for functions without closing parenthesis
        for (String function : FUNCTIONS) {
            if (expression.endsWith(function + "(") || expression.contains(function + "(") && !expression.contains(")")) {
                return true;
            }
        }

        // Other checks for valid expressions
        boolean containsOperatorOrFunction = expression.matches(".*[+\\-×/^%!²].*") ||
                expression.matches(".*(sin|cos|tan|log|ln|√|asin|acos|atan)\\(.*");

        boolean containsConstant = expression.contains("π") || expression.contains("e");

        return containsOperatorOrFunction || containsConstant;
    }


    private boolean isValidExpression(String expression) {
        if (expression.isEmpty()) {
            return false;
        }

        // Check for balanced parentheses
        int parenthesesCount = 0;
        for (char c : expression.toCharArray()) {
            if (c == '(') parenthesesCount++;
            if (c == ')') parenthesesCount--;
            if (parenthesesCount < 0) return false;
        }
        if (parenthesesCount != 0) return false;

        // Check for valid operators and functions
        boolean containsOperatorOrFunction = expression.matches(".*[+\\-×/^%!²].*") ||
                expression.matches(".*(sin|cos|tan|log|ln|√|asin|acos|atan)\\(.*\\)");


        boolean containsConstant = expression.contains("π") || expression.contains("e");

        // Check if the expression ends with a number, constant, closed parenthesis, or percentage
        boolean endsCorrectly = expression.matches(".*[0-9πe)%!]$") || expression.endsWith("²");

        return (containsOperatorOrFunction || containsConstant) && endsCorrectly;
    }

    private String formatLargeNumber(BigDecimal number) {
        String plainString = number.stripTrailingZeros().toPlainString();
        if (plainString.replace(".", "").length() > 20) {
            // Convert to scientific notation
            return String.format(Locale.getDefault(), "%.15E", number.doubleValue());
        } else {
            return plainString;
        }
    }

    private String formatNumber(BigDecimal number) {
        if (number.abs().compareTo(new BigDecimal("1E20")) >= 0 ||
                (number.abs().compareTo(new BigDecimal("1E-20")) > 0 && number.abs().compareTo(BigDecimal.ONE) < 0)) {
            return formatLargeNumber(number);
        } else if (number.stripTrailingZeros().scale() <= 0) {
            return number.toBigInteger().toString();
        } else {
            return number.stripTrailingZeros().toPlainString();
        }
    }

    private void scrollNumDisplayToEnd() {
        numDisplay.post(() -> {
            int scrollAmount = numDisplay.getWidth() - numDisplayScrollView.getWidth();
            if (scrollAmount > 0) {
                numDisplayScrollView.smoothScrollTo(scrollAmount, 0);
            }
        });
    }

    private void saveHistoryToPreferences() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_CALCULATION_HISTORY, new Gson().toJson(calculationHistory));
        editor.apply();
    }

    private void fadeOutView(View view, Runnable onAnimationEnd) {
        AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(250);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
                if (onAnimationEnd != null) {
                    onAnimationEnd.run();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        view.startAnimation(fadeOut);
    }

    private void fadeInView(View view) {
        view.setVisibility(View.VISIBLE);
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(300);
        view.startAnimation(fadeIn);
    }


    private void loadHistoryFromPreferences() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        String savedHistory = prefs.getString(PREF_CALCULATION_HISTORY, "");
        if (!savedHistory.isEmpty()) {
            Type listType = new TypeToken<ArrayList<String>>() {
            }.getType();
            calculationHistory = new Gson().fromJson(savedHistory, listType);
        } else {
            calculationHistory = new ArrayList<>();
        }
        if (historyAdapter != null) {
            historyAdapter.updateHistory(calculationHistory);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_CURRENT_INPUT, currentInput.toString());
        outState.putBoolean(KEY_IS_RADIAN_MODE, isRadianMode);
        outState.putString(KEY_LAST_OPERATION, lastOperation);
        outState.putString(KEY_LAST_RESULT, lastResult.toString());
        outState.putString(KEY_LAST_DISPLAY, lastDisplay.getText().toString());
        outState.putBoolean("updateCheckPerformed", updateCheckPerformed);
        outState.putStringArrayList(PREF_CALCULATION_HISTORY, new ArrayList<>(calculationHistory));
    }

    private void restoreState(Bundle savedInstanceState) {
        currentInput = new StringBuilder(savedInstanceState.getString(KEY_CURRENT_INPUT, ""));
        isRadianMode = savedInstanceState.getBoolean(KEY_IS_RADIAN_MODE, false);
        lastOperation = savedInstanceState.getString(KEY_LAST_OPERATION, "");
        lastResult = new BigDecimal(savedInstanceState.getString(KEY_LAST_RESULT, "0"));
        lastDisplay.setText(savedInstanceState.getString(KEY_LAST_DISPLAY, ""));
        ArrayList<String> savedHistory = savedInstanceState.getStringArrayList(PREF_CALCULATION_HISTORY);
        calculationHistory = savedHistory != null ? savedHistory : new ArrayList<>();
        historyAdapter.updateHistory(calculationHistory);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_INSTALL_PACKAGES) {
            if (hasInstallPermission()) {
                Toast.makeText(this, "Permission to install packages granted", Toast.LENGTH_SHORT).show();
                appUpdater.checkAndDownloadPendingUpdate();
            } else {
                Toast.makeText(this, "Permission to install packages is required for updates", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean hasInstallPermission() {
        return getPackageManager().canRequestPackageInstalls();
    }

    private final Runnable deleteRunnable = new Runnable() {
        @Override
        public void run() {
            if (isLongPressing) {
                deleteLastChar();
                longPressHandler.postDelayed(this, REPEAT_DELAY);
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        saveHistoryToPreferences();
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveHistoryToPreferences();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(updateReceiver);
        updateCheckPerformed = false;
    }
}