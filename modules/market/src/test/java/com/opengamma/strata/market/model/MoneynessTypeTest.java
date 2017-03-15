/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.model;

import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link MoneynessType}.
 */
@Test
public class MoneynessTypeTest {

  public void test_basics() {
    assertEquals(MoneynessType.of("Price"), MoneynessType.PRICE);
    assertEquals(MoneynessType.of("Rates"), MoneynessType.RATES);
    assertEquals(MoneynessType.PRICE.toString(), "Price");
    assertEquals(MoneynessType.RATES.toString(), "Rates");
  }

  public void coverage() {
    coverEnum(MoneynessType.class);
  }

}
