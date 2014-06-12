/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph.convert;

import static org.testng.AssertJUnit.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class StringParserTest {

  @Test
  public void parse() {
    String str = "a~1 1 abc, def, \"gh i\", \"jk,l\", \"m,n\\\" o\" a \" 'b \" 1.2 -3 \"quo\\\"ted\"";
    List<String> expected =
        ImmutableList.of("a~1", "1", "abc", "def", "gh i", "jk,l", "m,n\" o", "a", " 'b ", "1.2", "-3", "quo\"ted");
    List<String> parsed = StringParser.parse(str);
    assertEquals(expected, parsed);
  }
}
