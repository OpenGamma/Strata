/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.product.credit.SeniorityLevel.SENIOR_SECURED_DOMESTIC;
import static com.opengamma.strata.product.credit.SeniorityLevel.SENIOR_UNSECURED_FOREIGN;
import static com.opengamma.strata.product.credit.SeniorityLevel.SUBORDINATE_LOWER_TIER_2;
import static com.opengamma.strata.product.credit.SeniorityLevel.SUBORDINATE_TIER_1;
import static com.opengamma.strata.product.credit.SeniorityLevel.SUBORDINATE_UPPER_TIER_2;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.Sets;

/**
 * Test SeniorityLevel.
 */
@Test
public class SeniorityLevelTest {

  //-----------------------------------------------------------------------
  public void test_constants() {
    assertEquals(SeniorityLevel.valueOf("SENIOR_SECURED_DOMESTIC"), SENIOR_SECURED_DOMESTIC);
    assertEquals(SeniorityLevel.valueOf("SENIOR_UNSECURED_FOREIGN"), SENIOR_UNSECURED_FOREIGN);
    assertEquals(SeniorityLevel.valueOf("SUBORDINATE_LOWER_TIER_2"), SUBORDINATE_LOWER_TIER_2);
    assertEquals(SeniorityLevel.valueOf("SUBORDINATE_TIER_1"), SUBORDINATE_TIER_1);
    assertEquals(SeniorityLevel.valueOf("SUBORDINATE_UPPER_TIER_2"), SUBORDINATE_UPPER_TIER_2);
  }

  //-----------------------------------------------------------------------
  public void test_all_values() {
    Set<SeniorityLevel> available = Sets.newHashSet(SeniorityLevel.values());
    assertTrue(available.contains(SENIOR_SECURED_DOMESTIC));
    available.remove(SENIOR_SECURED_DOMESTIC);
    assertTrue(available.contains(SENIOR_UNSECURED_FOREIGN));
    available.remove(SENIOR_UNSECURED_FOREIGN);
    assertTrue(available.contains(SUBORDINATE_LOWER_TIER_2));
    available.remove(SUBORDINATE_LOWER_TIER_2);
    assertTrue(available.contains(SUBORDINATE_TIER_1));
    available.remove(SUBORDINATE_TIER_1);
    assertTrue(available.contains(SUBORDINATE_UPPER_TIER_2));
    available.remove(SUBORDINATE_UPPER_TIER_2);
    assertTrue(available.isEmpty());
  }

  //-----------------------------------------------------------------------
  @DataProvider(name = "ofBad")
  Object[][] data_ofBad() {
    return new Object[][] {
        {""},
        {"NA"},
        {"SENIORSECUREDDOMESTIC"},
        {"SeniorSecureDomestic"},
        {"SRNFOR"},
        {null},
    };
  }

  @Test(dataProvider = "ofBad", expectedExceptions = {IllegalArgumentException.class, NullPointerException.class})
  public void test_of_String_bad(String input) {
    SeniorityLevel.valueOf(input);
  }

  //-----------------------------------------------------------------------
  public void test_equals_hashCode() {
    SeniorityLevel a1 = SENIOR_SECURED_DOMESTIC;
    SeniorityLevel a2 = SeniorityLevel.valueOf("SENIOR_SECURED_DOMESTIC");
    SeniorityLevel b = SUBORDINATE_LOWER_TIER_2;
    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(a2), true);

    assertEquals(a2.equals(a1), true);
    assertEquals(a2.equals(a2), true);
    assertEquals(a2.equals(b), false);

    assertEquals(b.equals(a1), false);
    assertEquals(b.equals(a2), false);
    assertEquals(b.equals(b), true);

    assertEquals(a1.hashCode(), a2.hashCode());
  }

  public void test_equals_bad() {
    SeniorityLevel a = SENIOR_SECURED_DOMESTIC;
    assertEquals(a.equals(null), false);
    assertEquals(a.equals("String"), false);
    assertEquals(a.equals(new Object()), false);
  }

  //-----------------------------------------------------------------------
  public void test_toString() {
    SeniorityLevel test = SENIOR_SECURED_DOMESTIC;
    assertEquals("SENIOR_SECURED_DOMESTIC", test.toString());
  }

  //-----------------------------------------------------------------------
  public void coverage() {
    coverEnum(SeniorityLevel.class);
  }

  public void test_serialization() {
    assertSerialization(SENIOR_SECURED_DOMESTIC);
    assertSerialization(SeniorityLevel.valueOf("SENIOR_SECURED_DOMESTIC"));
  }

  public void test_jodaConvert() {
    assertJodaConvert(SeniorityLevel.class, SENIOR_SECURED_DOMESTIC);
    assertJodaConvert(SeniorityLevel.class, SeniorityLevel.valueOf("SENIOR_SECURED_DOMESTIC"));
  }

}
