/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.measure.fxopt.FxSingleBarrierOptionMethod.BLACK;
import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.product.fxopt.FxSingleBarrierOptionTrade;

/**
 * Test {@link FxSingleBarrierOptionMethod}.
 */
@Test
public class FxSingleBarrierOptionMethodTest {

  private static final FxSingleBarrierOptionTrade TRADE = FxSingleBarrierOptionTradeCalculationFunctionTest.TRADE;
  private static final CalculationTarget TARGET = new CalculationTarget() {};

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {FxSingleBarrierOptionMethod.BLACK, "Black"},
        {FxSingleBarrierOptionMethod.TRINOMIAL_TREE, "TrinomialTree"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(FxSingleBarrierOptionMethod convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(FxSingleBarrierOptionMethod convention, String name) {
    assertEquals(FxSingleBarrierOptionMethod.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> FxSingleBarrierOptionMethod.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> FxSingleBarrierOptionMethod.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_filter() {
    assertEquals(BLACK.filter(TRADE, Measures.PRESENT_VALUE), Optional.of(BLACK));
    assertEquals(BLACK.filter(TARGET, Measures.PRESENT_VALUE), Optional.empty());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(FxSingleBarrierOptionMethod.class);
  }

  public void test_serialization() {
    assertSerialization(FxSingleBarrierOptionMethod.BLACK);
  }

  public void test_jodaConvert() {
    assertJodaConvert(FxSingleBarrierOptionMethod.class, FxSingleBarrierOptionMethod.BLACK);
  }

}
