package org.example.cds.projections;

import java.util.List;

/**
 * Defines a built-in function with its signature and return type.
 * Used by BuiltInFunctionRegistry for function validation.
 */
public class FunctionDefinition {

    /**
     * Argument type categories for validation.
     */
    public enum ArgType {
        STRING,    // String or LargeString
        NUMERIC,   // Integer, Integer64, Decimal, Double
        TEMPORAL,  // Date, Time, DateTime, Timestamp
        BOOLEAN,   // Boolean
        ANY        // Any type accepted
    }

    private final String name;
    private final int minArgs;
    private final int maxArgs;  // -1 for unlimited (variadic)
    private final List<ArgType> argTypes;
    private final ArgType returnType;
    private final boolean returnsInputType;  // e.g., UPPER(String) -> String
    private final String description;

    public FunctionDefinition(String name, int minArgs, int maxArgs,
                            List<ArgType> argTypes, ArgType returnType,
                            boolean returnsInputType, String description) {
        this.name = name;
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
        this.argTypes = argTypes;
        this.returnType = returnType;
        this.returnsInputType = returnsInputType;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public int getMinArgs() {
        return minArgs;
    }

    public int getMaxArgs() {
        return maxArgs;
    }

    public List<ArgType> getArgTypes() {
        return argTypes;
    }

    public ArgType getReturnType() {
        return returnType;
    }

    public boolean returnsInputType() {
        return returnsInputType;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns true if this function accepts a variable number of arguments.
     */
    public boolean isVariadic() {
        return maxArgs == -1;
    }

    /**
     * Returns true if the given argument count is valid for this function.
     */
    public boolean acceptsArgCount(int count) {
        return count >= minArgs && (maxArgs == -1 || count <= maxArgs);
    }

    @Override
    public String toString() {
        return name + "(" + minArgs + (isVariadic() ? "+" : "-" + maxArgs) + " args)";
    }
}
