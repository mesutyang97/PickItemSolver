package InputWriter;


import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/** JUnit tests for the LibStruc class.
 *  @author mesutyang97
 */

public class WITest {
    @Rule
    public Timeout globalTimeout = Timeout.seconds(10);

    @Test
    public void testSplitCallNum() {
        WriteInput.generate();
    }



}
