/**
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.StandardId;

/**
 * Test {@link JumpToDefault}.
 */
@Test
public class JumpToDefaultTest {

  private static final StandardId ID_ABC = StandardId.of("OG", "ABC");
  private static final StandardId ID_DEF = StandardId.of("OG", "DEF");

  public void test_of() {
    JumpToDefault test = JumpToDefault.of(GBP, ImmutableMap.of(ID_ABC, 1.1d, ID_DEF, 2.2d));
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getAmounts().size(), 2);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    JumpToDefault test = JumpToDefault.of(GBP, ImmutableMap.of(ID_ABC, 1.1d, ID_DEF, 2.2d));
    coverImmutableBean(test);
    JumpToDefault test2 = JumpToDefault.of(USD, ImmutableMap.of(ID_DEF, 2.3d));
    coverBeanEquals(test, test2);
  }

}
