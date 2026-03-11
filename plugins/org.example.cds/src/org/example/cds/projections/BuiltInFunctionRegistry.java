package org.example.cds.projections;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.example.cds.projections.FunctionDefinition.ArgType;

/**
 * Registry of SAP CAP built-in functions.
 * Similar to AnnotationRegistry pattern from Phase 21.
 *
 * Supports:
 * - String functions: CONCAT, UPPER, LOWER, SUBSTRING, LENGTH, TRIM
 * - Numeric functions: ROUND, FLOOR, CEIL, CEILING, ABS
 * - Date/Time functions: CURRENT_DATE, CURRENT_TIME, CURRENT_TIMESTAMP, NOW
 * - Conversion functions: STRING
 */
public class BuiltInFunctionRegistry {

    private final Map<String, FunctionDefinition> functions = new HashMap<>();

    public BuiltInFunctionRegistry() {
        registerStringFunctions();
        registerNumericFunctions();
        registerDateTimeFunctions();
        registerConversionFunctions();
    }

    /**
     * Registers string manipulation functions.
     */
    private void registerStringFunctions() {
        // CONCAT(str1, str2, ...) -> String (variadic)
        register("CONCAT", 1, -1,  // At least 1 argument, unlimited max
            List.of(ArgType.STRING), ArgType.STRING, false,
            "Concatenates strings");

        // UPPER(str) -> String
        register("UPPER", 1, 1,
            List.of(ArgType.STRING), ArgType.STRING, true,
            "Converts string to uppercase");

        // LOWER(str) -> String
        register("LOWER", 1, 1,
            List.of(ArgType.STRING), ArgType.STRING, true,
            "Converts string to lowercase");

        // SUBSTRING(str, start, length?) -> String
        register("SUBSTRING", 2, 3,
            List.of(ArgType.STRING, ArgType.NUMERIC, ArgType.NUMERIC),
            ArgType.STRING, false,
            "Extracts substring from position (1-based)");

        // LENGTH(str) -> Integer
        register("LENGTH", 1, 1,
            List.of(ArgType.STRING), ArgType.NUMERIC, false,
            "Returns string length");

        // TRIM(str) -> String
        register("TRIM", 1, 1,
            List.of(ArgType.STRING), ArgType.STRING, true,
            "Trims whitespace from both ends");
    }

    /**
     * Registers numeric functions.
     */
    private void registerNumericFunctions() {
        // ROUND(num, decimals?) -> Decimal
        register("ROUND", 1, 2,
            List.of(ArgType.NUMERIC, ArgType.NUMERIC),
            ArgType.NUMERIC, true,
            "Rounds to specified decimals (default 0)");

        // FLOOR(num) -> Integer
        register("FLOOR", 1, 1,
            List.of(ArgType.NUMERIC), ArgType.NUMERIC, false,
            "Returns largest integer <= num");

        // CEIL(num) -> Integer
        register("CEIL", 1, 1,
            List.of(ArgType.NUMERIC), ArgType.NUMERIC, false,
            "Returns smallest integer >= num");

        // CEILING(num) -> Integer (alias for CEIL)
        register("CEILING", 1, 1,
            List.of(ArgType.NUMERIC), ArgType.NUMERIC, false,
            "Alias for CEIL");

        // ABS(num) -> same as input
        register("ABS", 1, 1,
            List.of(ArgType.NUMERIC), ArgType.NUMERIC, true,
            "Returns absolute value");
    }

    /**
     * Registers date/time functions.
     */
    private void registerDateTimeFunctions() {
        // CURRENT_DATE() -> Date
        register("CURRENT_DATE", 0, 0,
            List.of(), ArgType.TEMPORAL, false,
            "Returns current date");

        // CURRENT_TIME() -> Time
        register("CURRENT_TIME", 0, 0,
            List.of(), ArgType.TEMPORAL, false,
            "Returns current time");

        // CURRENT_TIMESTAMP() -> DateTime
        register("CURRENT_TIMESTAMP", 0, 0,
            List.of(), ArgType.TEMPORAL, false,
            "Returns current timestamp");

        // NOW() -> DateTime (alias)
        register("NOW", 0, 0,
            List.of(), ArgType.TEMPORAL, false,
            "Alias for CURRENT_TIMESTAMP");
    }

    /**
     * Registers conversion functions.
     */
    private void registerConversionFunctions() {
        // STRING(value) -> String
        register("STRING", 1, 1,
            List.of(ArgType.ANY), ArgType.STRING, false,
            "Converts value to string");
    }

    /**
     * Helper to register a function definition.
     */
    private void register(String name, int minArgs, int maxArgs,
                         List<ArgType> argTypes, ArgType returnType,
                         boolean returnsInputType, String description) {
        FunctionDefinition func = new FunctionDefinition(
            name, minArgs, maxArgs, argTypes, returnType, returnsInputType, description);
        functions.put(name.toUpperCase(), func);
    }

    /**
     * Gets a function definition by name (case-insensitive).
     * Returns null if function is not registered.
     */
    public FunctionDefinition getFunction(String name) {
        if (name == null) return null;
        return functions.get(name.toUpperCase());
    }

    /**
     * Returns true if the function name is registered.
     */
    public boolean isKnownFunction(String name) {
        return getFunction(name) != null;
    }

    /**
     * Returns all registered function names (for suggestions).
     */
    public Set<String> getAllFunctionNames() {
        return functions.keySet();
    }
}
