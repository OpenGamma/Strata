/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;

/**
 * Test {@link FxSwap}.
 */
@Test
public class FxSwapTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final CurrencyAmount GBP_M1000 = CurrencyAmount.of(GBP, -1_000);
  private static final CurrencyAmount USD_P1550 = CurrencyAmount.of(USD, 1_550);
  private static final CurrencyAmount USD_M1600 = CurrencyAmount.of(USD, -1_600);
  private static final CurrencyAmount EUR_P1590 = CurrencyAmount.of(EUR, 1_590);
  private static final LocalDate DATE_2011_11_21 = date(2011, 11, 21);
  private static final LocalDate DATE_2011_12_21 = date(2011, 12, 21);
  private static final FxSingle NEAR_LEG = FxSingle.of(GBP_P1000, USD_M1600, DATE_2011_11_21);
  private static final FxSingle FAR_LEG = FxSingle.of(GBP_M1000, USD_P1550, DATE_2011_12_21);
  private static final BusinessDayAdjustment BDA = BusinessDayAdjustment.of(FOLLOWING, GBLO);

  //-------------------------------------------------------------------------
  public void test_of() {
    FxSwap test = sut();
    assertEquals(test.getNearLeg(), NEAR_LEG);
    assertEquals(test.getFarLeg(), FAR_LEG);
  }

  public void test_of_wrongOrder() {
    assertThrowsIllegalArg(() -> FxSwap.of(FAR_LEG, NEAR_LEG));
  }

  public void test_of_wrongBaseCurrency() {
    FxSingle nearLeg = FxSingle.of(EUR_P1590, USD_M1600, DATE_2011_11_21);
    assertThrowsIllegalArg(() -> FxSwap.of(nearLeg, FAR_LEG));
  }

  public void test_of_wrongCounterCurrency() {
    FxSingle nearLeg = FxSingle.of(USD_P1550, EUR_P1590.negated(), DATE_2011_11_21);
    FxSingle farLeg = FxSingle.of(GBP_M1000, EUR_P1590, DATE_2011_12_21);
    assertThrowsIllegalArg(() -> FxSwap.of(nearLeg, farLeg));
  }

  public void test_of_sameSign() {
    FxSingle farLeg = FxSingle.of(GBP_M1000.negated(), USD_P1550.negated(), DATE_2011_12_21);
    assertThrowsIllegalArg(() -> FxSwap.of(NEAR_LEG, farLeg));
  }

  public void test_ofForwardPoints() {
    double nearRate = 1.6;
    double fwdPoint = 0.1;
    FxSwap test =
        FxSwap.ofForwardPoints(GBP_P1000, FxRate.of(GBP, USD, nearRate), fwdPoint, DATE_2011_11_21, DATE_2011_12_21);
    FxSingle nearLegExp = FxSingle.of(GBP_P1000, CurrencyAmount.of(USD, -1000.0 * nearRate), DATE_2011_11_21);
    FxSingle farLegExp = FxSingle.of(GBP_M1000, CurrencyAmount.of(USD, 1000.0 * (nearRate + fwdPoint)), DATE_2011_12_21);
    assertEquals(test.getNearLeg(), nearLegExp);
    assertEquals(test.getFarLeg(), farLegExp);
  }

  public void test_ofForwardPoints_withAdjustment() {
    double nearRate = 1.6;
    double fwdPoint = 0.1;
    FxSwap test =
        FxSwap.ofForwardPoints(GBP_P1000, FxRate.of(GBP, USD, nearRate), fwdPoint, DATE_2011_11_21, DATE_2011_12_21, BDA);
    FxSingle nearLegExp = FxSingle.of(GBP_P1000, CurrencyAmount.of(USD, -1000.0 * nearRate), DATE_2011_11_21, BDA);
    FxSingle farLegExp = FxSingle.of(GBP_M1000, CurrencyAmount.of(USD, 1000.0 * (nearRate + fwdPoint)), DATE_2011_12_21, BDA);
    assertEquals(test.getNearLeg(), nearLegExp);
    assertEquals(test.getFarLeg(), farLegExp);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    FxSwap base = sut();
    ResolvedFxSwap test = base.resolve(REF_DATA);
    assertEquals(test.getNearLeg(), NEAR_LEG.resolve(REF_DATA));
    assertEquals(test.getFarLeg(), FAR_LEG.resolve(REF_DATA));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static FxSwap sut() {
    return FxSwap.of(NEAR_LEG, FAR_LEG);
  }

  static FxSwap sut2() {
    FxSingle nearLeg = FxSingle.of(CurrencyAmount.of(GBP, 1_100), CurrencyAmount.of(USD, -1_650), DATE_2011_11_21);
    FxSingle farLeg = FxSingle.of(CurrencyAmount.of(GBP, -1_100), CurrencyAmount.of(USD, 1_750), DATE_2011_12_21);
    return FxSwap.of(nearLeg, farLeg);
  }

}
