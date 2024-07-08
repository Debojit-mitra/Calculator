package com.bunny.tools.scientific_calculator;

import static android.net.http.SslCertificate.restoreState;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Stack;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView lastDisplay;
    private EditText numDisplay;
    private StringBuilder currentInput;
    private String lastOperation;
    private double lastResult;
    private static final String KEY_CURRENT_INPUT = "currentInput";
    private static final String KEY_LAST_OPERATION = "lastOperation";
    private static final String KEY_LAST_RESULT = "lastResult";
    private static final String KEY_LAST_DISPLAY = "lastDisplay";

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
            lastResult = -1;
        }
        updateDisplay();
    }

    private void initializeViews() {
        lastDisplay = findViewById(R.id.lastDisplay);
        numDisplay = findViewById(R.id.numDisplay);

        if (numDisplay != null) {
            numDisplay.setInputType(InputType.TYPE_NULL);
            numDisplay.setCursorVisible(true);
            numDisplay.setFocusableInTouchMode(true);
            numDisplay.requestFocus();
        }

        int[] buttonIds = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
                R.id.btnDot, R.id.btnPlus, R.id.btnMinus, R.id.btnMultiply, R.id.btnDivide, R.id.btnEquals,
                R.id.btnAc, R.id.btnDel, R.id.btnPercentage, R.id.btnParenthesesFirst, R.id.btnParenthesesSecond,
                R.id.btnRootOver, R.id.btnSquare
        };

        for (int id : buttonIds) {
            View view = findViewById(id);
            if (view != null) {
                view.setOnClickListener(this);
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
            case "*":
            case "/":
                handleOperator(buttonText);
                break;
            case "%":
                handlePercentage();
                break;
            case "√":
                handleSquareRoot();
                break;
            case "x²":
                handleSquare();
                break;
            case "(":
            case ")":
                appendToInput(buttonText);
                break;
            default:
                appendToInput(buttonText);
                break;
        }

        updateDisplay();
    }
    private void clearAll() {
        currentInput.setLength(0);
        lastOperation = "";
        lastResult = 0;
        if (lastDisplay != null) {
            lastDisplay.setText("");
        }
    }
    private void deleteLastChar() {
        if (currentInput.length() > 0) {
            currentInput.deleteCharAt(currentInput.length() - 1);
        }
    }
    private void calculateResult() {
        try {
            String expression = currentInput.toString();
            double result = evaluateExpression(expression);
            lastDisplay.setText(expression);
            numDisplay.setText(String.valueOf(result));
            currentInput.setLength(0);
            currentInput.append(result);
        } catch (Exception e) {
            numDisplay.setText("Error");
        }
    }

    private double evaluateExpression(String expression) {
        Stack<Double> numbers = new Stack<>();
        Stack<Character> operators = new Stack<>();

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (Character.isDigit(c) || c == '.') {
                StringBuilder num = new StringBuilder();
                while (i < expression.length() && (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) {
                    num.append(expression.charAt(i));
                    i++;
                }
                i--;
                numbers.push(Double.parseDouble(num.toString()));
            } else if (c == '(') {
                operators.push(c);
            } else if (c == ')') {
                while (!operators.isEmpty() && operators.peek() != '(') {
                    numbers.push(applyOperation(operators.pop(), numbers.pop(), numbers.pop()));
                }
                operators.pop(); // Remove '('
            } else if (c == '+' || c == '-' || c == '*' || c == '/') {
                while (!operators.isEmpty() && hasPrecedence(c, operators.peek())) {
                    numbers.push(applyOperation(operators.pop(), numbers.pop(), numbers.pop()));
                }
                operators.push(c);
            }
        }

        while (!operators.isEmpty()) {
            numbers.push(applyOperation(operators.pop(), numbers.pop(), numbers.pop()));
        }

        return numbers.pop();
    }

    private boolean hasPrecedence(char op1, char op2) {
        if (op2 == '(' || op2 == ')') {
            return false;
        }
        if ((op1 == '*' || op1 == '/') && (op2 == '+' || op2 == '-')) {
            return false;
        } else {
            return true;
        }
    }

    private double applyOperation(char operator, double b, double a) {
        switch (operator) {
            case '+':
                return a + b;
            case '-':
                return a - b;
            case '*':
                return a * b;
            case '/':
                if (b == 0) throw new ArithmeticException("Division by zero");
                return a / b;
        }
        return 0;
    }

    private void handleOperator(String operator) {
        if (currentInput.length() > 0) {
            currentInput.append(operator);
        } else if (lastResult != 0) {
            currentInput.append(lastResult).append(operator);
        }
    }

    private void handlePercentage() {
        if (currentInput.length() > 0) {
            double value = Double.parseDouble(currentInput.toString());
            value /= 100;
            lastDisplay.setText(currentInput + "%");
            currentInput.setLength(0);
            currentInput.append(value);
        }
    }

    private void handleSquareRoot() {
        if (currentInput.length() > 0) {
            double value = Double.parseDouble(currentInput.toString());
            if (value >= 0) {
                lastDisplay.setText("√(" + currentInput + ")");
                value = Math.sqrt(value);
                currentInput.setLength(0);
                currentInput.append(value);
            } else {
                numDisplay.setText("Error");
            }
        }
    }

    private void handleSquare() {
        if (currentInput.length() > 0) {
            double value = Double.parseDouble(currentInput.toString());
            lastDisplay.setText("(" + currentInput + ")²");
            value = value * value;
            currentInput.setLength(0);
            currentInput.append(value);
        }
    }

    private void appendToInput(String value) {
        currentInput.append(value);
    }

    private void updateDisplay() {
        if (numDisplay != null) {
            numDisplay.setText(currentInput.toString());
            numDisplay.setSelection(numDisplay.getText().length());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_CURRENT_INPUT, currentInput.toString());
        outState.putString(KEY_LAST_OPERATION, lastOperation);
        outState.putDouble(KEY_LAST_RESULT, lastResult);
        outState.putString(KEY_LAST_DISPLAY, lastDisplay.getText().toString());
    }

    private void restoreState(Bundle savedInstanceState) {
        currentInput = new StringBuilder(savedInstanceState.getString(KEY_CURRENT_INPUT, ""));
        lastOperation = savedInstanceState.getString(KEY_LAST_OPERATION, "");
        lastResult = savedInstanceState.getDouble(KEY_LAST_RESULT, 0);
        lastDisplay.setText(savedInstanceState.getString(KEY_LAST_DISPLAY, "0"));
    }
}