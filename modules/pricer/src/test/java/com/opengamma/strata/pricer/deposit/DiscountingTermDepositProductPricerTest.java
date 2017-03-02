/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.deposit;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.deposit.ResolvedTermDeposit;
import com.opengamma.strata.product.deposit.TermDeposit;

/**
 * Test {@link DiscountingTermDepositProductPricer}.
 */
@Test
public class DiscountingTermDepositProductPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2014, 1, 22);
  private static final LocalDate START_DATE = date(2014, 1, 24);
  private static final LocalDate END_DATE = date(2014, 7, 24);
  private static final double NOTIONAL = 100000000d;
  private static final double RATE = 0.0750;
  private static final BusinessDayAdjustment BD_ADJ = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA);
  private static final TermDeposit TERM_DEPOSIT = TermDeposit.builder()
      .buySell(BuySell.BUY)
      .startDate(START_DATE)
      .endDate(END_DATE)
      .businessDayAdjustment(BD_ADJ)
      .dayCount(ACT_360)
      .notional(NOTIONAL)
      .currency(EUR)
      .rate(RATE)
      .build();
  private static final ResolvedTermDeposit RTERM_DEPOSIT = TERM_DEPOSIT.resolve(REF_DATA);
  private static final DiscountingTermDepositProductPricer PRICER = DiscountingTermDepositProductPricer.DEFAULT;
  private static final double TOLERANCE = 1E-12;

  private static final double EPS_FD = 1E-7;
  private static final RatesFiniteDifferenceSensitivityCalculator CAL_FD =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);
  private static final ImmutableRatesProvider IMM_PROV;
  static {
    CurveInterpolator interp = CurveInterpolators.DOUBLE_QUADRATIC;
    DoubleArray time_eur = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 3.0, 4.0, 5.0, 10.0);
    DoubleArray rate_eur = DoubleArray.of(0.0160, 0.0135, 0.0160, 0.0185, 0.0185, 0.0195, 0.0200, 0.0210);
    InterpolatedNodalCurve dscCurve =
        InterpolatedNodalCurve.of(Curves.zeroRates("EUR-Discount", ACT_360), time_eur, rate_eur, interp);
    IMM_PROV = ImmutableRatesProvider.builder(VAL_DATE)
        .discountCurve(EUR, dscCurve)
        .build();
  }
  private static final double DF_START = 0.99;
  double DF_END = 0.94;

  //-------------------------------------------------------------------------
  public void test_presentValue_notStarted() {
    SimpleRatesProvider prov = provider(VAL_DATE, DF_START, DF_END);
    CurrencyAmount computed = PRICER.presentValue(RTERM_DEPOSIT, prov);
    double expected = ((1d + RATE * RTERM_DEPOSIT.getYearFraction()) * DF_END - DF_START) * NOTIONAL;
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected, TOLERANCE * NOTIONAL);
  }

  public void test_presentValue_onStart() {
    SimpleRatesProvider prov = provider(START_DATE, 1.0d, DF_END);
    CurrencyAmount computed = PRICER.presentValue(RTERM_DEPOSIT, prov);
    double expected = ((1d + RATE * RTERM_DEPOSIT.getYearFraction()) * DF_END - 1.0d) * NOTIONAL;
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected, TOLERANCE * NOTIONAL);
  }

  public void test_presentValue_started() {
    SimpleRatesProvider prov = provider(date(2014, 2, 22), 1.2d, DF_END);
    CurrencyAmount computed = PRICER.presentValue(RTERM_DEPOSIT, prov);
    double expected = (1d + RATE * RTERM_DEPOSIT.getYearFraction()) * DF_END * NOTIONAL;
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected, TOLERANCE * NOTIONAL);
  }

  public void test_presentValue_onEnd() {
    SimpleRatesProvider prov = provider(END_DATE, 1.2d, 1.0d);
    CurrencyAmount computed = PRICER.presentValue(RTERM_DEPOSIT, prov);
    double expected = (1d + RATE * RTERM_DEPOSIT.getYearFraction()) * 1.0d * NOTIONAL;
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected, TOLERANCE * NOTIONAL);
  }

  public void test_presentValue_ended() {
    SimpleRatesProvider prov = provider(date(2014, 9, 22), 1.2d, 1.1d);
    CurrencyAmount computed = PRICER.presentValue(RTERM_DEPOSIT, prov);
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), 0.0d, TOLERANCE * NOTIONAL);
  }

  public void test_presentValueSensitivity() {
    PointSensitivities computed = PRICER.presentValueSensitivity(RTERM_DEPOSIT, IMM_PROV);
    CurrencyParameterSensitivities sensiComputed = IMM_PROV.parameterSensitivity(computed);
    CurrencyParameterSensitivities sensiExpected =
        CAL_FD.sensitivity(IMM_PROV, (p) -> PRICER.presentValue(RTERM_DEPOSIT, (p)));
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, NOTIONAL * EPS_FD));
  }

  public void test_parRate() {
    SimpleRatesProvider prov = provider(VAL_DATE, DF_START, DF_END);
    double parRate = PRICER.parRate(RTERM_DEPOSIT, prov);
    TermDeposit depositPar = TermDeposit.builder()
        .buySell(BuySell.BUY)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .businessDayAdjustment(BD_ADJ)
        .dayCount(ACT_360)
        .notional(NOTIONAL)
        .currency(EUR)
        .rate(parRate)
        .build();
    double pvPar = PRICER.presentValue(depositPar.resolve(REF_DATA), prov).getAmount();
    assertEquals(pvPar, 0.0, NOTIONAL * TOLERANCE);
  }

  public void test_parSpread() {
    SimpleRatesProvider prov = provider(VAL_DATE, DF_START, DF_END);
    double parSpread = PRICER.parSpread(RTERM_DEPOSIT, prov);
    TermDeposit depositPar = TermDeposit.builder()
        .buySell(BuySell.BUY)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .businessDayAdjustment(BD_ADJ)
        .dayCount(ACT_360)
        .notional(NOTIONAL)
        .currency(EUR)
        .rate(RATE + parSpread)
        .build();
    double pvPar = PRICER.presentValue(depositPar.resolve(REF_DATA), prov).getAmount();
    assertEquals(pvPar, 0.0, NOTIONAL * TOLERANCE);
  }

  public void test_parSpreadSensitivity() {
    PointSensitivities computed = PRICER.parSpreadSensitivity(RTERM_DEPOSIT, IMM_PROV);
    CurrencyParameterSensitivities sensiComputed = IMM_PROV.parameterSensitivity(computed);
    CurrencyParameterSensitivities sensiExpected =
        CAL_FD.sensitivity(IMM_PROV, (p) -> CurrencyAmount.of(EUR, PRICER.parSpread(RTERM_DEPOSIT, (p))));
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, NOTIONAL * EPS_FD));
  }

  public void test_parRateSensitivity() {
    PointSensitivities computedSpread = PRICER.parSpreadSensitivity(RTERM_DEPOSIT, IMM_PROV);
    PointSensitivities computedRate = PRICER.parRateSensitivity(RTERM_DEPOSIT, IMM_PROV);
    assertTrue(computedSpread.equalWithTolerance(computedRate, NOTIONAL * EPS_FD));
  }

  private SimpleRatesProvider provider(LocalDate valuationDate, double dfStart, double dfEnd) {
    DiscountFactors mockDf = mock(DiscountFactors.class);
    when(mockDf.discountFactor(START_DATE)).thenReturn(dfStart);
    when(mockDf.discountFactor(END_DATE)).thenReturn(dfEnd);
    SimpleRatesProvider prov = new SimpleRatesProvider(valuationDate, mockDf);
    return prov;
  }

}
