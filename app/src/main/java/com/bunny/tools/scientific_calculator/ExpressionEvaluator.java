package com.bunny.tools.scientific_calculator;

import android.util.Log;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Function;

public class ExpressionEvaluator {
    private static boolean isRadianMode = true;
    private static boolean isInverseMode = false;

    private static final Map<String, Integer> PRECEDENCE = new HashMap<>();
    private static final Map<String, Function<BigDecimal, BigDecimal>> FUNCTIONS = new HashMap<>();

    static {
        PRECEDENCE.put("+", 1);
        PRECEDENCE.put("-", 1);
        PRECEDENCE.put("×", 2);
        PRECEDENCE.put("/", 2);
        PRECEDENCE.put("^", 3);
        PRECEDENCE.put("%", 4);

        FUNCTIONS.put("sin", v -> BigDecimal.valueOf(evaluateFunction("sin", v.doubleValue())));
        FUNCTIONS.put("cos", v -> BigDecimal.valueOf(evaluateFunction("cos", v.doubleValue())));
        FUNCTIONS.put("tan", v -> BigDecimal.valueOf(evaluateFunction("tan", v.doubleValue())));
        FUNCTIONS.put("log", v -> BigDecimal.valueOf(evaluateFunction("log", v.doubleValue())));
        FUNCTIONS.put("ln", v -> BigDecimal.valueOf(evaluateFunction("ln", v.doubleValue())));
        FUNCTIONS.put("√", v -> BigDecimal.valueOf(evaluateFunction("√", v.doubleValue())));
        FUNCTIONS.put("asin", v -> BigDecimal.valueOf(evaluateFunction("asin", v.doubleValue())));
        FUNCTIONS.put("acos", v -> BigDecimal.valueOf(evaluateFunction("acos", v.doubleValue())));
        FUNCTIONS.put("atan", v -> BigDecimal.valueOf(evaluateFunction("atan", v.doubleValue())));
    }

    public static void setModes(boolean radianMode, boolean inverseMode) {
        isRadianMode = radianMode;
        isInverseMode = inverseMode;
    }

    public static BigDecimal evaluateExpression(String expression) {
        if (expression.contains("²")){
            expression = expression.replace("²", "^2");

        }
        List<String> tokens = tokenize(expression);
        List<String> postfix = infixToPostfix(tokens);
        return evaluatePostfix(postfix);
    }

    private static List<String> tokenize(String expression) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                sb.append(c);
            } else if (c == 'π' || c == 'e') {
                if (sb.length() > 0) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
                tokens.add(String.valueOf(c));
            } else {
                if (sb.length() > 0) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
                if (c == '(' || c == ')' || c == '+' || c == '-' || c == '×' || c == '/' || c == '^' || c == '%' || c == '!' || c == '²') {
                    tokens.add(String.valueOf(c));
                } else if (Character.isLetter(c) || c == '√') {
                    String func = String.valueOf(c);
                    while (i + 1 < expression.length() && Character.isLetter(expression.charAt(i + 1))) {
                        func += expression.charAt(++i);
                    }
                    tokens.add(func);
                }
            }
        }
        if (sb.length() > 0) {
            tokens.add(sb.toString());
        }
        return tokens;
    }

    private static List<String> infixToPostfix(List<String> infix) {
        List<String> postfix = new ArrayList<>();
        Stack<String> stack = new Stack<>();

        for (String token : infix) {
            if (isNumber(token)) {
                postfix.add(token);
            } else if (FUNCTIONS.containsKey(token) || token.equals("²")) {
                stack.push(token);
            } else if (token.equals("(")) {
                stack.push(token);
            } else if (token.equals(")")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    postfix.add(stack.pop());
                }
                stack.pop(); // Remove "("
                if (!stack.isEmpty() && (FUNCTIONS.containsKey(stack.peek()) || stack.peek().equals("²"))) {
                    postfix.add(stack.pop());
                }
            } else if (PRECEDENCE.containsKey(token)) {
                while (!stack.isEmpty() && PRECEDENCE.containsKey(stack.peek())) {
                    Integer precedence1 = PRECEDENCE.get(stack.peek());
                    Integer precedence2 = PRECEDENCE.get(token);
                    if (precedence1 != null && precedence2 != null && precedence1.compareTo(precedence2) >= 0) {
                        postfix.add(stack.pop());
                    } else {
                        break;
                    }
                }
                stack.push(token);
            } else if (token.equals("!")) {
                postfix.add(token);
            }
        }

        while (!stack.isEmpty()) {
            postfix.add(stack.pop());
        }

        return postfix;
    }

    static Stack<String> operatorStack = new Stack<>(); //putting this inside evaluatePostfix creates problems

    private static BigDecimal evaluatePostfix(List<String> postfix) {
        Stack<BigDecimal> stack = new Stack<>();

        for (String token : postfix) {
            if (isNumber(token)) {
                if (token.equals("π")) {
                    stack.push(BigDecimal.valueOf(Math.PI));
                } else if (token.equals("e")) {
                    stack.push(BigDecimal.valueOf(Math.E));
                } else {
                    stack.push(new BigDecimal(token));
                }
            } else if (FUNCTIONS.containsKey(token)) {
                BigDecimal operand = stack.pop();
                stack.push(Objects.requireNonNull(FUNCTIONS.get(token)).apply(operand));
            } else if (token.equals("²")) {
                BigDecimal operand = stack.pop();
                stack.push(operand.pow(2));
            } else if (token.equals("!")) {
                int operand = stack.pop().intValue();
                stack.push(BigDecimal.valueOf(factorial(operand)));
            } else if (token.equals("%")) {
                BigDecimal operand = stack.pop();
                BigDecimal percentValue = operand.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
                if (!stack.isEmpty() && !operatorStack.isEmpty() &&
                        (operatorStack.peek().equals("+") || operatorStack.peek().equals("-"))) {

                    BigDecimal base = stack.pop();
                    stack.push(base);

                    stack.push(base.multiply(percentValue));
                } else {
                    stack.push(percentValue);
                }
            } else {
                operatorStack.push(token);
                Log.e("here else operatorStack", "operatorStack: "+operatorStack);
                BigDecimal b = stack.pop();
                BigDecimal a = stack.isEmpty() ? BigDecimal.ZERO : stack.pop();
                switch (token) {
                    case "+":
                        stack.push(a.add(b));
                        break;
                    case "-":
                        stack.push(a.subtract(b));
                        break;
                    case "×":
                        stack.push(a.multiply(b));
                        break;
                    case "/":
                        if (b.compareTo(BigDecimal.ZERO) == 0)
                            throw new ArithmeticException("Division by zero");
                        stack.push(a.divide(b, 10, RoundingMode.HALF_UP));
                        break;
                    case "^":
                        stack.push(BigDecimal.valueOf(Math.pow(a.doubleValue(), b.doubleValue())));
                        break;
                }
            }
        }

        return stack.pop();
    }


    private static boolean isNumber(String token) {
        return token.equals("π") || token.equals("e") || token.matches("-?\\d*\\.?\\d+");
    }

    private static int factorial(int n) {
        if (n < 0)
            throw new IllegalArgumentException("Factorial is not defined for negative numbers");
        if (n == 0 || n == 1) return 1;
        int result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    private static double evaluateFunction(String function, double value) {
        switch (function) {
            case "sin":
                return isRadianMode ? Math.sin(value) : Math.sin(Math.toRadians(value));
            case "cos":
                return isRadianMode ? Math.cos(value) : Math.cos(Math.toRadians(value));
            case "tan":
                return isRadianMode ? Math.tan(value) : Math.tan(Math.toRadians(value));
            case "asin":
                return isRadianMode ? Math.asin(value) : Math.toDegrees(Math.asin(value));
            case "acos":
                return isRadianMode ? Math.acos(value) : Math.toDegrees(Math.acos(value));
            case "atan":
                return isRadianMode ? Math.atan(value) : Math.toDegrees(Math.atan(value));
            case "log":
                return Math.log10(value);
            case "ln":
                return Math.log(value);
            case "√":
                return Math.sqrt(value);
            default:
                throw new IllegalArgumentException("Unknown function: " + function);
        }
    }
}