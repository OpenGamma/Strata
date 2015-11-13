/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.product.credit.RestructuringClause.CUM_RESTRUCTURING_2003;
import static com.opengamma.strata.product.credit.RestructuringClause.CUM_RESTRUCTURING_2014;
import static com.opengamma.strata.product.credit.RestructuringClause.MODIFIED_RESTRUCTURING_2003;
import static com.opengamma.strata.product.credit.RestructuringClause.MODIFIED_RESTRUCTURING_2014;
import static com.opengamma.strata.product.credit.RestructuringClause.MOD_MOD_RESTRUCTURING_2003;
import static com.opengamma.strata.product.credit.RestructuringClause.MOD_MOD_RESTRUCTURING_2014;
import static com.opengamma.strata.product.credit.RestructuringClause.NO_RESTRUCTURING_2003;
import static com.opengamma.strata.product.credit.RestructuringClause.NO_RESTRUCTURING_2014;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.Sets;

/**
 * Test RestructuringClause.
 */
@Test
public class RestructuringClauseTest {

  //-----------------------------------------------------------------------
  public void test_constants() {
    assertEquals(RestructuringClause.valueOf("MOD_MOD_RESTRUCTURING_2014"), MOD_MOD_RESTRUCTURING_2014);
    assertEquals(RestructuringClause.valueOf("MOD_MOD_RESTRUCTURING_2003"), MOD_MOD_RESTRUCTURING_2003);
    assertEquals(RestructuringClause.valueOf("MODIFIED_RESTRUCTURING_2014"), MODIFIED_RESTRUCTURING_2014);
    assertEquals(RestructuringClause.valueOf("MODIFIED_RESTRUCTURING_2003"), MODIFIED_RESTRUCTURING_2003);
    assertEquals(RestructuringClause.valueOf("CUM_RESTRUCTURING_2014"), CUM_RESTRUCTURING_2014);
    assertEquals(RestructuringClause.valueOf("CUM_RESTRUCTURING_2003"), CUM_RESTRUCTURING_2003);
    assertEquals(RestructuringClause.valueOf("NO_RESTRUCTURING_2014"), NO_RESTRUCTURING_2014);
    assertEquals(RestructuringClause.valueOf("NO_RESTRUCTURING_2003"), NO_RESTRUCTURING_2003);
  }

  //-----------------------------------------------------------------------
  public void test_all_values() {
    Set<RestructuringClause> available = Sets.newHashSet(RestructuringClause.values());
    assertTrue(available.contains(MOD_MOD_RESTRUCTURING_2014));
    available.remove(MOD_MOD_RESTRUCTURING_2014);
    assertTrue(available.contains(MOD_MOD_RESTRUCTURING_2003));
    available.remove(MOD_MOD_RESTRUCTURING_2003);
    assertTrue(available.contains(MODIFIED_RESTRUCTURING_2014));
    available.remove(MODIFIED_RESTRUCTURING_2014);
    assertTrue(available.contains(MODIFIED_RESTRUCTURING_2003));
    available.remove(MODIFIED_RESTRUCTURING_2003);
    assertTrue(available.contains(CUM_RESTRUCTURING_2014));
    available.remove(CUM_RESTRUCTURING_2014);
    assertTrue(available.contains(CUM_RESTRUCTURING_2003));
    available.remove(CUM_RESTRUCTURING_2003);
    assertTrue(available.contains(NO_RESTRUCTURING_2014));
    available.remove(NO_RESTRUCTURING_2014);
    assertTrue(available.contains(NO_RESTRUCTURING_2003));
    available.remove(NO_RESTRUCTURING_2003);
    assertTrue(available.isEmpty());
  }

  //-----------------------------------------------------------------------
  @DataProvider(name = "ofBad")
  Object[][] data_ofBad() {
    return new Object[][] {
        {""},
        {"NA"},
        {"MODMODRESTRUCTURING2014"},
        {"ModModRestructuring"},
        {"MMR"},
        {null},
    };
  }

  @Test(dataProvider = "ofBad", expectedExceptions = {IllegalArgumentException.class, NullPointerException.class})
  public void test_of_String_bad(String input) {
    RestructuringClause.valueOf(input);
  }

  //-----------------------------------------------------------------------
  public void test_equals_hashCode() {
    RestructuringClause a1 = MOD_MOD_RESTRUCTURING_2014;
    RestructuringClause a2 = RestructuringClause.valueOf("MOD_MOD_RESTRUCTURING_2014");
    RestructuringClause b = NO_RESTRUCTURING_2003;
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
    RestructuringClause a = MOD_MOD_RESTRUCTURING_2014;
    assertEquals(a.equals(null), false);
    assertEquals(a.equals("String"), false);
    assertEquals(a.equals(new Object()), false);
  }

  //-----------------------------------------------------------------------
  public void test_toString() {
    RestructuringClause test = MOD_MOD_RESTRUCTURING_2014;
    assertEquals("MOD_MOD_RESTRUCTURING_2014", test.toString());
  }

  //-----------------------------------------------------------------------
  public void coverage() {
    coverEnum(RestructuringClause.class);
  }

  public void test_serialization() {
    assertSerialization(MOD_MOD_RESTRUCTURING_2014);
    assertSerialization(RestructuringClause.valueOf("MOD_MOD_RESTRUCTURING_2014"));
  }

  public void test_jodaConvert() {
    assertJodaConvert(RestructuringClause.class, MOD_MOD_RESTRUCTURING_2014);
    assertJodaConvert(RestructuringClause.class, RestructuringClause.valueOf("MOD_MOD_RESTRUCTURING_2014"));
  }

}
