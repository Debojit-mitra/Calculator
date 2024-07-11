package com.bunny.tools.scientific_calculator;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView lastDisplay, intermediateResult;
    private EditText numDisplay;
    private StringBuilder currentInput;
    private String lastOperation;
    private BigDecimal lastResult;
    private static final String KEY_CURRENT_INPUT = "currentInput";
    private static final String KEY_LAST_OPERATION = "lastOperation";
    private static final String KEY_LAST_RESULT = "lastResult";
    private static final String KEY_LAST_DISPLAY = "lastDisplay";
    private boolean isRadianMode = false;
    private boolean isInverseMode = false;
    private static final String KEY_IS_RADIAN_MODE = "isRadianMode";
    private HorizontalScrollView numDisplayScrollView;


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
        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else {
            currentInput = new StringBuilder();
            lastOperation = "";
            lastResult = BigDecimal.ZERO;
        }
        updateDisplay();
    }

    private void initializeViews() {
        lastDisplay = findViewById(R.id.lastDisplay);
        numDisplay = findViewById(R.id.numDisplay);
        intermediateResult = findViewById(R.id.intermediateResult);
        numDisplayScrollView = findViewById(R.id.numDisplayScrollView);

        numDisplay.setOnClickListener(v -> numDisplay.setSelection(numDisplay.getText().length()));

        if (numDisplay != null) {
            numDisplay.setInputType(InputType.TYPE_NULL);
            numDisplay.setCursorVisible(true);
            numDisplay.setFocusableInTouchMode(true);
            numDisplay.requestFocus();
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
        }
    }

    @Override
    public void onClick(View view) {
        if (!(view instanceof Button)) {
            return;
        }

        Button button = (Button) view;
        String buttonText = button.getText().toString();

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
                deleteLastChar();
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
        updateDisplay();
    }

    private void deleteLastChar() {
        if (currentInput.length() > 0) {
            currentInput.deleteCharAt(currentInput.length() - 1);
            updateDisplay();
        }
    }

    private void calculateResult() {
        try {
            String expression = currentInput.toString();
            ExpressionEvaluator.setModes(isRadianMode, isInverseMode);
            BigDecimal result = ExpressionEvaluator.evaluateExpression(expression);
            expression = expression + " =";
            lastDisplay.setText(expression);
            String formattedResult = formatNumber(result);
            numDisplay.setText(formattedResult);
            scrollNumDisplayToEnd();
            lastResult = result;
            currentInput.setLength(0);
            currentInput.append(formattedResult);
            intermediateResult.setText("");
        } catch (Exception e) {
            numDisplay.setText(R.string.error);
            scrollNumDisplayToEnd();
            intermediateResult.setText("");
        }
    }

    private void handleOperator(String operator) {
        if (currentInput.length() > 0) {
            char lastChar = currentInput.charAt(currentInput.length() - 1);
            if (Character.isDigit(lastChar) || lastChar == ')' || lastChar == 'π' || lastChar == 'e' || lastChar == '²' || lastChar == '%') {
                currentInput.append(operator);
            } else if (isOperator(lastChar) && !operator.equals("²")) {
                // Replace the last operator, unless it's '²' or '%'
                currentInput.setCharAt(currentInput.length() - 1, operator.charAt(0));
            }
        } else if (lastResult.compareTo(BigDecimal.ZERO) != 0 && !operator.equals("²")) {
            currentInput.append(formatNumber(lastResult)).append(operator);
        }
        lastOperation = operator;
        updateDisplay();
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '×' || c == '/' || c == '^' || c == '%';
    }

    private void handleFunction(String actualFunction) {
        if (currentInput.length() > 0) {
            char lastChar = currentInput.charAt(currentInput.length() - 1);
            if (Character.isDigit(lastChar) || lastChar == ')' || lastChar == 'π' || lastChar == 'e') {
                currentInput.append("×");
            }
        }


        currentInput.append(actualFunction);
        if (!actualFunction.equals("^2") && !actualFunction.equals("10^") && !actualFunction.equals("e^")) {
            currentInput.append("(");
        }
        updateDisplay();
    }

    private void handleConstant(String constant) {
        if (currentInput.length() > 0) {
            char lastChar = currentInput.charAt(currentInput.length() - 1);
            if (Character.isDigit(lastChar) || lastChar == ')' || lastChar == 'π' || lastChar == 'e') {
                currentInput.append("*");
            }
        }
        currentInput.append(constant);
        Log.d("Calculator", "Current input after adding constant: " + currentInput.toString());
        updateDisplay();
    }

    private void handleFactorial() {
        if (currentInput.length() > 0) {
            currentInput.append("!");
        }
        updateDisplay();
    }

    private void appendToInput(String value) {
        if (currentInput.length() > 0) {
            char lastChar = currentInput.charAt(currentInput.length() - 1);
            if ((Character.isDigit(lastChar) || lastChar == 'π' || lastChar == 'e' || lastChar == ')') &&
                    (value.equals("(") || value.equals("π") || value.equals("e") ||
                            FUNCTIONS.contains(value))) {
                currentInput.append("×");
            }
        }
        currentInput.append(value);
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
                    getThemeColor(com.google.android.material.R.attr.colorPrimaryContainer) :
                    getThemeColor(com.google.android.material.R.attr.colorPrimaryInverse);

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
        scrollNumDisplayToEnd();
        updateIntermediateResult();
    }

    private void updateIntermediateResult() {
        try {
            String expression = currentInput.toString();
            if (isValidExpression(expression)) {
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
        boolean endsCorrectly = expression.matches(".*[0-9πe)%]$") || expression.endsWith("²");

        return (containsOperatorOrFunction || containsConstant) && endsCorrectly;
    }

    private String formatNumber(BigDecimal number) {
        if (number.stripTrailingZeros().scale() <= 0) {
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

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_CURRENT_INPUT, currentInput.toString());
        outState.putBoolean(KEY_IS_RADIAN_MODE, isRadianMode);
        outState.putString(KEY_LAST_OPERATION, lastOperation);
        outState.putString(KEY_LAST_RESULT, lastResult.toString());
        outState.putString(KEY_LAST_DISPLAY, lastDisplay.getText().toString());
    }

    private void restoreState(Bundle savedInstanceState) {
        currentInput = new StringBuilder(savedInstanceState.getString(KEY_CURRENT_INPUT, ""));
        isRadianMode = savedInstanceState.getBoolean(KEY_IS_RADIAN_MODE, false);
        lastOperation = savedInstanceState.getString(KEY_LAST_OPERATION, "");
        lastResult = new BigDecimal(savedInstanceState.getString(KEY_LAST_RESULT, "0"));
        lastDisplay.setText(savedInstanceState.getString(KEY_LAST_DISPLAY, ""));
    }
}