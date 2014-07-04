/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.id;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertSame;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class ExternalSchemeTest {

  public void test_of() {
    ExternalScheme test = ExternalScheme.of("IATA");
    assertEquals("IATA", test.getName());
  }

  public void test_of_cached() {
    assertSame(ExternalScheme.of("ISO"), ExternalScheme.of("ISO"));
  }

  @DataProvider(name = "invalidFormat")
  Object[][] data_invalidFormat() {
    return new Object[][] {
        {null},
        {""},
        {"~Scheme"},
        {"Scheme~"},
        {"Sch~eme"},
    };
  }

  @Test(dataProvider = "invalidFormat", expectedExceptions = IllegalArgumentException.class)
  public void test_of_invalidFormat(String text) {
    ExternalScheme.of(text);
  }

  //-------------------------------------------------------------------------
  public void test_compareTo() {
    ExternalScheme d1 = ExternalScheme.of("d1");
    ExternalScheme d2 = ExternalScheme.of("d2");
    
    assertEquals(d1.compareTo(d1) == 0, true);
    assertEquals(d1.compareTo(d2) < 0, true);
    
    assertEquals(d2.compareTo(d1) > 0, true);
    assertEquals(d2.compareTo(d2) == 0, true);
  }

  public void test_equals() {
    ExternalScheme d1a = ExternalScheme.of("d1");
    ExternalScheme d1b = ExternalScheme.of("d1");
    ExternalScheme d2 = ExternalScheme.of("d2");
    
    assertEquals(d1a.equals(d1a), true);
    assertEquals(d1a.equals(d1b), true);
    assertEquals(d1a.equals(d2), false);
    
    assertEquals(d1b.equals(d1a), true);
    assertEquals(d1b.equals(d1b), true);
    assertEquals(d1b.equals(d2), false);
    
    assertEquals(d2.equals(d1a), false);
    assertEquals(d2.equals(d1b), false);
    assertEquals(d2.equals(d2), true);
    
    assertEquals(d1b.equals("d1"), false);
    assertEquals(d1b.equals(null), false);
  }

  public void test_hashCode() {
    ExternalScheme d1a = ExternalScheme.of("d1");
    ExternalScheme d1b = ExternalScheme.of("d1");
    
    assertEquals(d1a.hashCode(), d1b.hashCode());
  }

  public void test_toString() {
    ExternalScheme test = ExternalScheme.of("Scheme");
    assertEquals("Scheme", test.toString());
  }

}
