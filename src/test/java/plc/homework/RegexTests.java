package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. Test structure for steps 1 & 2 are
 * provided, you must create this yourself for step 3.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Capitalization", "bigBoi@gmail.com", true),
                Arguments.of("Hotmail Domain", "smallGuy@hotmail.com", true),
                Arguments.of("Random Capitalization .net", "DecentGem@joLLy.net", true),
                Arguments.of("Random Capitalization .gov", "toTalSiv@kachoW.gov", true),
                Arguments.of("Random Capitalization .to", "SevenTree@saBi.to", true),

                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),
                Arguments.of("Inverted I", "ba!f@gmail.com", false),
                Arguments.of("# for @", "solpy#hotmail.com", false),
                Arguments.of("$ for an s .io", "favy@ha$le.io", false),
                Arguments.of("& for a period", "chopil@gmail&com", false),
                Arguments.of("special characters in domain", "flOAt@g@#$%.net", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testEvenStringsRegex(String test, String input, boolean success) {
        test(input, Regex.EVEN_STRINGS, success);
    }

    public static Stream<Arguments> testEvenStringsRegex() {
        return Stream.of(
                //what has ten letters and starts with gas?
                Arguments.of("10 Characters", "automobile", true),
                Arguments.of("14 Characters", "i<3pancakes10!", true),
                Arguments.of("16 Characters", "juGt&5U75#EtF%90", true),
                Arguments.of("18 Characters", "sugn@7fHud$6yfh9*u", true),
                Arguments.of("20 Characters", "l(o&jfn*h^IU%HNVt^$f", true),

                Arguments.of("6 Characters", "6chars", false),
                Arguments.of("13 Characters", "i<3pancakes9!", false),
                Arguments.of("15 Characters", "sugn@7fHud$6yfh", false),
                Arguments.of("17 Characters", "juGt&5U75#EtF%90j", false),
                Arguments.of("21 Characters", "l(o&jfn*h^IU%HNVt^$f&", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testIntegerListRegex(String test, String input, boolean success) {
        test(input, Regex.INTEGER_LIST, success);
    }

    public static Stream<Arguments> testIntegerListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "[1]", true),
                Arguments.of("Spaced Single Digits", "[1, 2, 3]", true),
                Arguments.of("Blank Brackets", "[]", true),
                Arguments.of("Double Digit Single", "[40]", true),
                Arguments.of("Multiple Double Digits", "[10,13,20]", true),
                Arguments.of("Single and Double", "[1,45,6]", true),

                Arguments.of("Missing Brackets", "1,2,3", false),
                Arguments.of("Missing Commas", "[1 2 3]", false),
                Arguments.of("Trailing Comma", "[1,]", false),
                Arguments.of("Early and Trailing Commas", "[,5,]", false),
                Arguments.of("Blank Number", "[ ,2]", false),
                Arguments.of("Multiple Trailing Commas", "[4,,,]", false),
                Arguments.of("Multiple Leading Commas", "[,,,9]", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testNumberRegex(String test, String input, boolean success) {
        test(input, Regex.NUMBER, success);
    }

    public static Stream<Arguments> testNumberRegex() {
        return Stream.of(
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Single Trailing Zero", "2.0", true),
                Arguments.of("Multiple Trailing Zeroes", "5.000", true),
                Arguments.of("Double Digits", "67.89", true),
                Arguments.of("Double Leading and Trailing Zeroes", "00.00", true),
                Arguments.of("Positive Integer", "+4", true),
                Arguments.of("Negative Integer", "-5", true),
                Arguments.of("Positive Decimal", "+1.98", true),
                Arguments.of("Negative Decimal", "-2.54", true),

                Arguments.of("No Digit Before Decimal", ".9", false),
                Arguments.of("No Digits After Decimal", "1.", false),
                Arguments.of("Interrupting Decimals", "0.2.4", false),
                Arguments.of("Surrounding Decimals", ".8.2.", false),
                Arguments.of("Multiple Decimals", "12...17", false),
                Arguments.of("Double Positive Integer", "++4", false),
                Arguments.of("Double Negative Integer", "--56", false),
                Arguments.of("Double Positive Decimal", "++2.53", false),
                Arguments.of("Double Negative Decimal", "--43.623", false),
                Arguments.of("Interlaced Positives", "+42+4+", false),
                Arguments.of("Interlaced Negatives", "-3-654-", false),
                Arguments.of("Freestyle Signs", "+45-1-2+345+3-11", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success);
    }

    public static Stream<Arguments> testStringRegex() {
        return Stream.of(
                Arguments.of("Only Quotes", "\"\"", true),
                Arguments.of("Testing Tab", "\"1\\t2\"", true),
                Arguments.of("Testing New Line", "\"5\\n7\"", true),
                Arguments.of("Testing \b", "\"2\\b6\"", true),
                Arguments.of("Testing \r", "\"7\\r2\"", true),
                Arguments.of("Testing \'", "\"hwe\\'sdf\"", true),
                Arguments.of("Double Literal Backslashes", "\"hwe\\'sdf\"", true),

                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                Arguments.of("Quotes Before", "\"\"word", false),
                Arguments.of("Quotes After", "test\"\"", false),
                Arguments.of("Single Literal Backslash ", "\"hw#^e\\l(d4&f\"", false),
                Arguments.of("Single Quote End", "unterminated\"", false)
        );
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
