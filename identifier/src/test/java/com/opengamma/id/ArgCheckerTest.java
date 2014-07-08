/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.id;

import static com.opengamma.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.testng.annotations.Test;

/**
 * Test ArgumentChecker.
 */
@Test
public class ArgCheckerTest {

  //-------------------------------------------------------------------------
  public void test_notNull_ok() {
    assertEquals(ArgChecker.notNull("OG", "name"), "OG");
    assertEquals(ArgChecker.notNull(Integer.valueOf(1), "name"), Integer.valueOf(1));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_notNull_null() {
    try {
      ArgChecker.notNull(null, "name");
    } catch (IllegalArgumentException ex) {
      assertEquals(ex.getMessage().contains("'name'"), true);
      throw ex;
    }
  }

  //-------------------------------------------------------------------------
  public void test_matches_String_ok() {
    assertEquals(ArgChecker.matches(Pattern.compile("[A-Z]+"), "OG", "name"), "OG");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'pattern'.*")
  public void test_matches_String_nullPattern() {
    ArgChecker.matches(null, "", "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*")
  public void test_matches_String_nullString() {
    ArgChecker.matches(Pattern.compile("[A-Z]+"), null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*")
  public void test_matches_String_empty() {
    ArgChecker.matches(Pattern.compile("[A-Z]+"), "", "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*")
  public void test_matches_String_noMatch() {
    ArgChecker.matches(Pattern.compile("[A-Z]+"), "123", "name");
  }

  //-------------------------------------------------------------------------
  public void test_noNulls_Array_ok() {
    String[] expected = new String[] {"Element"};
    String[] test = ArgChecker.noNulls(expected, "name");
    assertEquals(test, expected);
  }

  public void test_noNulls_Array_ok_empty() {
    Object[] array = new Object[] {};
    ArgChecker.noNulls(array, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_noNulls_Array_null() {
    Object[] array = null;
    try {
      ArgChecker.noNulls(array, "name");
    } catch (IllegalArgumentException ex) {
      assertEquals(ex.getMessage().contains("'name'"), true);
      throw ex;
    }
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_noNulls_Array_nullElement() {
    Object[] array = new Object[] {null};
    try {
      ArgChecker.noNulls(array, "name");
    } catch (IllegalArgumentException ex) {
      assertEquals(ex.getMessage().contains("'name'"), true);
      throw ex;
    }
  }

  //-------------------------------------------------------------------------
  public void test_noNulls_Iterable_ok() {
    List<String> expected = Arrays.asList("Element");
    List<String> test = ArgChecker.noNulls(expected, "name");
    assertEquals(test, expected);
  }

  public void test_noNulls_Iterable_ok_empty() {
    Iterable<?> coll = Arrays.asList();
    ArgChecker.noNulls(coll, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_noNulls_Iterable_null() {
    Iterable<?> coll = null;
    try {
      ArgChecker.noNulls(coll, "name");
    } catch (IllegalArgumentException ex) {
      assertEquals(ex.getMessage().contains("'name'"), true);
      throw ex;
    }
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_noNulls_Iterable_nullElement() {
    Iterable<?> coll = Arrays.asList((Object) null);
    try {
      ArgChecker.noNulls(coll, "name");
    } catch (IllegalArgumentException ex) {
      assertEquals(ex.getMessage().contains("'name'"), true);
      throw ex;
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(ArgChecker.class);
  }

}
