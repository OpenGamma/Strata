/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.measure.fxopt.FxVanillaOptionMethod.BLACK;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.product.fxopt.FxVanillaOptionTrade;

/**
 * Test {@link FxVanillaOptionMethod}.
 */
@Test
public class FxVanillaOptionMethodTest {

  private static final FxVanillaOptionTrade TRADE = FxVanillaOptionTradeCalculationFunctionTest.TRADE;
  private static final CalculationTarget TARGET = new CalculationTarget() {};

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  public static Object[][] data_name() {
    return new Object[][] {
        {FxVanillaOptionMethod.BLACK, "Black"},
        {FxVanillaOptionMethod.VANNA_VOLGA, "VannaVolga"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(FxVanillaOptionMethod convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(FxVanillaOptionMethod convention, String name) {
    assertEquals(FxVanillaOptionMethod.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException().isThrownBy(() -> FxVanillaOptionMethod.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> FxVanillaOptionMethod.of(null));
  }

  //-------------------------------------------------------------------------
  public void test_filter() {
    assertEquals(BLACK.filter(TRADE, Measures.PRESENT_VALUE), Optional.of(BLACK));
    assertEquals(BLACK.filter(TARGET, Measures.PRESENT_VALUE), Optional.empty());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(FxVanillaOptionMethod.class);
  }

  public void test_serialization() {
    assertSerialization(FxVanillaOptionMethod.BLACK);
  }

  public void test_jodaConvert() {
    assertJodaConvert(FxVanillaOptionMethod.class, FxVanillaOptionMethod.BLACK);
  }

}
