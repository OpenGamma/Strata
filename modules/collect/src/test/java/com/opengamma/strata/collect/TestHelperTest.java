/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import org.testng.annotations.Test;

/**
 * Tes {@link TestHelper}.
 */
public class TestHelperTest {

  @Test
  public void test_assertEquals_objectObject() {
    TestHelper.assertEquals("abc", "abc", null);  // same string
    TestHelper.assertEquals("abc", "abcd".substring(0, 3), null);  // equals string
  }

  @Test
  public void test_assertEquals_nullNull() {
    TestHelper.assertEquals(null, null, null);
  }

  @Test
  public void test_assertEquals_notEqual() {
    try {
      TestHelper.assertEquals("abc", "def", "Oops");
      fail("Should fail");
    } catch (AssertionError ex) {
      assertEquals(ex.getMessage(), "Oops expected [def] but found [abc]");
    }
  }

  @Test
  public void test_assertEquals_objectNull() {
    try {
      TestHelper.assertEquals("abc", null, null);
      fail("Should fail");
    } catch (AssertionError ex) {
      assertEquals(ex.getMessage(), "expected [null] but found [abc]");
    }
  }

  @Test
  public void test_assertEquals_nullObject() {
    try {
      TestHelper.assertEquals(null, "abc", null);
      fail("Should fail");
    } catch (AssertionError ex) {
      assertEquals(ex.getMessage(), "expected [abc] but found [null]");
    }
  }

  @Test
  public void test_assertNotNull_notNull() {
    TestHelper.assertNotNull("abc", "Oops");
  }

  @Test
  public void test_assertNotNull_null() {
    try {
      TestHelper.assertNotNull(null, "Oops");
      fail("Should fail");
    } catch (AssertionError ex) {
      assertEquals(ex.getMessage(), "Oops");
    }
  }

}
