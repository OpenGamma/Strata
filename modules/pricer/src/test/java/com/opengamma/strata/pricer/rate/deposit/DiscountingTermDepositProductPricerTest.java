/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.deposit;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.date.HolidayCalendars.EUTA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.finance.rate.deposit.TermDeposit;
import com.opengamma.strata.market.sensitivity.CurveParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;

/**
 * Test {@link DiscountingTermDepositProductPricer}.
 */
@Test
public class DiscountingTermDepositProductPricerTest {
  private static final LocalDate VALUATION_DATE = LocalDate.of(2014, 1, 22);
  private static final LocalDate START_DATE = LocalDate.of(2014, 1, 24);
  private static final LocalDate END_DATE = LocalDate.of(2014, 7, 24);
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
  private static final DiscountingTermDepositProductPricer PRICER = DiscountingTermDepositProductPricer.DEFAULT;
  private static final double TOLERANCE = 1E-12;

  private static final double EPS_FD = 1E-7;
  private static final RatesFiniteDifferenceSensitivityCalculator CAL_FD =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);
  private static final ImmutableRatesProvider IMM_PROV;
  static {
    CombinedInterpolatorExtrapolator interp = CombinedInterpolatorExtrapolatorFactory.getInterpolator(
        Interpolator1DFactory.DOUBLE_QUADRATIC,
        Interpolator1DFactory.FLAT_EXTRAPOLATOR,
        Interpolator1DFactory.FLAT_EXTRAPOLATOR);
    double[] time_eur = new double[] {0.0, 0.5, 1.0, 2.0, 3.0, 4.0, 5.0, 10.0 };
    double[] rate_eur = new double[] {0.0160, 0.0135, 0.0160, 0.0185, 0.0185, 0.0195, 0.0200, 0.0210 };
    InterpolatedDoublesCurve curve_eur = InterpolatedDoublesCurve.from(time_eur, rate_eur, interp);
    YieldCurve dscCurve = new YieldCurve("EUR-Discount", curve_eur);
    IMM_PROV = ImmutableRatesProvider.builder()
        .valuationDate(VALUATION_DATE)
        .discountCurves(ImmutableMap.of(EUR, dscCurve))
        .dayCount(ACT_ACT_ISDA)
        .build();
  }

  public void test_presentValue_notStarted() {
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(VALUATION_DATE);
    double dfStart = 0.99;
    double dfEnd = 0.94;
    when(mockProv.discountFactor(EUR, START_DATE)).thenReturn(dfStart);
    when(mockProv.discountFactor(EUR, END_DATE)).thenReturn(dfEnd);
    CurrencyAmount computed = PRICER.presentValue(TERM_DEPOSIT, mockProv);
    double expected = ((1d + RATE * TERM_DEPOSIT.expand().getYearFraction()) * dfEnd - dfStart) * NOTIONAL;
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected, TOLERANCE * NOTIONAL);
  }

  public void test_presentValue_onStart() {
    LocalDate valuationDate = START_DATE;
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(valuationDate);
    double dfStart = 0.99;
    double dfEnd = 0.94;
    when(mockProv.discountFactor(EUR, START_DATE)).thenReturn(dfStart);
    when(mockProv.discountFactor(EUR, END_DATE)).thenReturn(dfEnd);
    CurrencyAmount computed = PRICER.presentValue(TERM_DEPOSIT, mockProv);
    double expected = ((1d + RATE * TERM_DEPOSIT.expand().getYearFraction()) * dfEnd - dfStart) * NOTIONAL;
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected, TOLERANCE * NOTIONAL);
  }

  public void test_presentValue_started() {
    LocalDate valuationDate = LocalDate.of(2014, 2, 22);
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(valuationDate);
    double dfStart = 0.99;
    double dfEnd = 0.94;
    when(mockProv.discountFactor(EUR, START_DATE)).thenReturn(dfStart);
    when(mockProv.discountFactor(EUR, END_DATE)).thenReturn(dfEnd);
    CurrencyAmount computed = PRICER.presentValue(TERM_DEPOSIT, mockProv);
    double expected = (1d + RATE * TERM_DEPOSIT.expand().getYearFraction()) * dfEnd * NOTIONAL;
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected, TOLERANCE * NOTIONAL);
  }

  public void test_presentValue_OnEnd() {
    LocalDate valuationDate = END_DATE;
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(valuationDate);
    double dfStart = 0.99;
    double dfEnd = 0.94;
    when(mockProv.discountFactor(EUR, START_DATE)).thenReturn(dfStart);
    when(mockProv.discountFactor(EUR, END_DATE)).thenReturn(dfEnd);
    CurrencyAmount computed = PRICER.presentValue(TERM_DEPOSIT, mockProv);
    double expected = (1d + RATE * TERM_DEPOSIT.expand().getYearFraction()) * dfEnd * NOTIONAL;
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected, TOLERANCE * NOTIONAL);
  }

  public void test_presentValue_ended() {
    LocalDate valuationDate = LocalDate.of(2014, 9, 22);
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(valuationDate);
    double dfStart = 0.99;
    double dfEnd = 0.94;
    when(mockProv.discountFactor(EUR, START_DATE)).thenReturn(dfStart);
    when(mockProv.discountFactor(EUR, END_DATE)).thenReturn(dfEnd);
    CurrencyAmount computed = PRICER.presentValue(TERM_DEPOSIT, mockProv);
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), 0.0d, TOLERANCE * NOTIONAL);
  }

  public void test_presentValueSensitivity() {
    PointSensitivities computed = PRICER.presentValueSensitivity(TERM_DEPOSIT, IMM_PROV);
    CurveParameterSensitivity sensiComputed = IMM_PROV.parameterSensitivity(computed);
    CurveParameterSensitivity sensiExpected =
        CAL_FD.sensitivity(IMM_PROV, (p) -> PRICER.presentValue(TERM_DEPOSIT, (p)));
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, NOTIONAL * EPS_FD));
  }

  public void test_parRate() {
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(VALUATION_DATE);
    double dfStart = 0.99;
    double dfEnd = 0.94;
    when(mockProv.discountFactor(EUR, START_DATE)).thenReturn(dfStart);
    when(mockProv.discountFactor(EUR, END_DATE)).thenReturn(dfEnd);
    double parRate = PRICER.parRate(TERM_DEPOSIT, mockProv);
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
    double pvPar = PRICER.presentValue(depositPar, mockProv).getAmount();
    assertEquals(pvPar, 0.0, NOTIONAL * TOLERANCE);
  }

  public void test_parSpread() {
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(VALUATION_DATE);
    double dfStart = 0.99;
    double dfEnd = 0.94;
    when(mockProv.discountFactor(EUR, START_DATE)).thenReturn(dfStart);
    when(mockProv.discountFactor(EUR, END_DATE)).thenReturn(dfEnd);
    double parSpread = PRICER.parSpread(TERM_DEPOSIT, mockProv);
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
    double pvPar = PRICER.presentValue(depositPar, mockProv).getAmount();
    assertEquals(pvPar, 0.0, NOTIONAL * TOLERANCE);
  }

  public void test_parSpreadSensitivity() {
    PointSensitivities computed = PRICER.parSpreadSensitivity(TERM_DEPOSIT, IMM_PROV);
    CurveParameterSensitivity sensiComputed = IMM_PROV.parameterSensitivity(computed);
    CurveParameterSensitivity sensiExpected =
        CAL_FD.sensitivity(IMM_PROV, (p) -> CurrencyAmount.of(EUR, PRICER.parSpread(TERM_DEPOSIT, (p))));
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, NOTIONAL * EPS_FD));
  }
}
