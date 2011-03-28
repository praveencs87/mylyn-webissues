package net.sf.webissues.api;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class UtilTest {

    /**
     * Test arguments in a response can be parsed properly
     */
    @Test
    public void testParseLine() {
        List<String> args = Util
                        .parseLine("A 'quoted text' 'quote text with a quote (\\') inside it' 'quoted text with a\\nnewline' unquoted 1 2 3 items={\"1\",\"2\\'t\",\"3\"}");
        assertEquals(9, args.size());
        assertEquals("A", args.get(0));
        assertEquals("quoted text", args.get(1));
        assertEquals("quote text with a quote (\') inside it", args.get(2));
        assertEquals("quoted text with a\nnewline", args.get(3));
        assertEquals("unquoted", args.get(4));
        assertEquals("1", args.get(5));
        assertEquals("2", args.get(6));
        assertEquals("3", args.get(7));
        assertEquals("items={\"1\",\"2\\'t\",\"3\"}", args.get(8));
        
    }
    
    @Test 
    public void testEmptyLastArgument() {
        List<String> args = Util.parseLine("H 28 25 1268680663 1 4 'Duplicate' ''");
        assertEquals(8, args.size());        
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testUnbalancedQuotes() {
        List<String> args = Util.parseLine("'");
        assertEquals(8, args.size());        
    }
}
