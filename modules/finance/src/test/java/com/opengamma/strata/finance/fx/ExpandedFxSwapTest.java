/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Test {@link ExpandedFxSwap}.
 */
@Test
public class ExpandedFxSwapTest {

  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final CurrencyAmount GBP_M1000 = CurrencyAmount.of(GBP, -1_000);
  private static final CurrencyAmount USD_P1550 = CurrencyAmount.of(USD, 1_550);
  private static final CurrencyAmount USD_M1600 = CurrencyAmount.of(USD, -1_600);
  private static final CurrencyAmount EUR_P1590 = CurrencyAmount.of(EUR, 1_590);
  private static final LocalDate DATE_2011_11_21 = date(2011, 11, 21);
  private static final LocalDate DATE_2011_12_21 = date(2011, 12, 21);
  private static final ExpandedFxSingle NEAR_LEG = ExpandedFxSingle.of(GBP_P1000, USD_M1600, DATE_2011_11_21);
  private static final ExpandedFxSingle FAR_LEG = ExpandedFxSingle.of(GBP_M1000, USD_P1550, DATE_2011_12_21);

  //-------------------------------------------------------------------------
  public void test_of() {
    ExpandedFxSwap test = ExpandedFxSwap.of(NEAR_LEG, FAR_LEG);
    assertEquals(test.getNearLeg(), NEAR_LEG);
    assertEquals(test.getFarLeg(), FAR_LEG);
  }

  public void test_of_wrongOrder() {
    assertThrowsIllegalArg(() -> ExpandedFxSwap.of(FAR_LEG, NEAR_LEG));
  }

  public void test_of_wrongBaseCurrency() {
    ExpandedFxSingle nearLeg = ExpandedFxSingle.of(EUR_P1590, USD_M1600, DATE_2011_11_21);
    assertThrowsIllegalArg(() -> ExpandedFxSwap.of(nearLeg, FAR_LEG));
  }

  public void test_of_wrongCounterCurrency() {
    ExpandedFxSingle nearLeg = ExpandedFxSingle.of(USD_P1550, EUR_P1590.negated(), DATE_2011_11_21);
    ExpandedFxSingle farLeg = ExpandedFxSingle.of(GBP_M1000, EUR_P1590, DATE_2011_12_21);
    assertThrowsIllegalArg(() -> ExpandedFxSwap.of(nearLeg, farLeg));
  }

  public void test_of_sameSign() {
    ExpandedFxSingle farLeg = ExpandedFxSingle.of(GBP_M1000.negated(), USD_P1550.negated(), DATE_2011_12_21);
    assertThrowsIllegalArg(() -> ExpandedFxSwap.of(NEAR_LEG, farLeg));
  }

  public void test_expand() {
    ExpandedFxSwap base = ExpandedFxSwap.of(NEAR_LEG, FAR_LEG);
    ExpandedFxSwap test = base.expand();
    assertEquals(test, base);
  }

  //-------------------------------------------------------------------------
  public void converage() {
    ExpandedFxSwap test1 = ExpandedFxSwap.of(NEAR_LEG, FAR_LEG);
    coverImmutableBean(test1);
    ExpandedFxSingle nearLeg =
        ExpandedFxSingle.of(CurrencyAmount.of(GBP, 1_100), CurrencyAmount.of(USD, -1_650), DATE_2011_11_21);
    ExpandedFxSingle farLeg = ExpandedFxSingle.of(CurrencyAmount.of(GBP, -1_100), CurrencyAmount.of(USD, 1_750), DATE_2011_12_21);
    ExpandedFxSwap test2 = ExpandedFxSwap.of(nearLeg, farLeg);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    ExpandedFxSwap test = ExpandedFxSwap.of(NEAR_LEG, FAR_LEG);
    assertSerialization(test);
  }

}
