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
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;

/**
 * Test {@link DiscountingTermDepositProductPricerBeta}.
 */
@Test
public class DiscountingTermDepositProductPricerBetaTest {
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
  private static final DiscountingTermDepositProductPricerBeta PRICER = DiscountingTermDepositProductPricerBeta.DEFAULT;
  private static final double TOLERANCE = 1E-12;
  private static final double EPS_FD = 1E-7;

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

  public void test_presentValueSensitivity() {
    RatesFiniteDifferenceSensitivityCalculator fdCal = new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);
    CombinedInterpolatorExtrapolator interp = CombinedInterpolatorExtrapolatorFactory.getInterpolator(
        Interpolator1DFactory.DOUBLE_QUADRATIC,
        Interpolator1DFactory.FLAT_EXTRAPOLATOR,
        Interpolator1DFactory.FLAT_EXTRAPOLATOR);
    double[] time_eur = new double[] {0.0, 0.5, 1.0, 2.0, 3.0, 4.0, 5.0, 10.0 };
    double[] rate_eur = new double[] {0.0160, 0.0135, 0.0160, 0.0185, 0.0185, 0.0195, 0.0200, 0.0210 };
    InterpolatedDoublesCurve curve_eur = InterpolatedDoublesCurve.from(time_eur, rate_eur, interp);
    YieldCurve dscCurve = new YieldCurve("EUR-Discount", curve_eur);
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
        .valuationDate(VALUATION_DATE)
        .discountCurves(ImmutableMap.of(EUR, dscCurve))
        .dayCount(ACT_ACT_ISDA)
        .build();
    PointSensitivities computed = PRICER.presentValueSensitivity(TERM_DEPOSIT, prov);
    CurveParameterSensitivity sensiComputed = prov.parameterSensitivity(computed);
    CurveParameterSensitivity sensiExpected = fdCal.sensitivity(prov, (p) -> PRICER.presentValue(TERM_DEPOSIT, (p)));
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
    RatesProvider mockProv = mock(RatesProvider.class);
    DiscountFactors mockDsc = mock(DiscountFactors.class);
    when(mockProv.getValuationDate()).thenReturn(VALUATION_DATE);
    double drStart = 0.05;
    double drEnd = 0.03;
    double startTime = ACT_360.relativeYearFraction(VALUATION_DATE, BD_ADJ.adjust(START_DATE));
    double endTime = ACT_360.relativeYearFraction(VALUATION_DATE, BD_ADJ.adjust(END_DATE));
    double dfStart = Math.exp(-drStart * startTime);
    double dfEnd = Math.exp(-drEnd * endTime);
    when(mockProv.discountFactor(EUR, START_DATE)).thenReturn(dfStart);
    when(mockProv.discountFactor(EUR, END_DATE)).thenReturn(dfEnd);
    when(mockProv.discountFactors(EUR)).thenReturn(mockDsc);
    when(mockDsc.pointSensitivity(START_DATE))
        .thenReturn(ZeroRateSensitivity.of(EUR, START_DATE, -startTime * dfStart));
    when(mockDsc.pointSensitivity(END_DATE)).thenReturn(ZeroRateSensitivity.of(EUR, END_DATE, -endTime * dfEnd));
    PointSensitivities computed = PRICER.parSpreadSensitivity(TERM_DEPOSIT, mockProv);
    RatesProvider[] mockProvs = new RatesProvider[4];
    for (int i = 0; i < 4; ++i) {
      mockProvs[i] = mock(RatesProvider.class);
      when(mockProvs[i].getValuationDate()).thenReturn(VALUATION_DATE);
      when(mockProvs[i].discountFactor(EUR, START_DATE)).thenReturn(dfStart);
      when(mockProvs[i].discountFactor(EUR, END_DATE)).thenReturn(dfEnd);
    }
    when(mockProvs[0].discountFactor(EUR, START_DATE)).thenReturn(Math.exp(-(drStart + EPS_FD) * startTime));
    when(mockProvs[1].discountFactor(EUR, START_DATE)).thenReturn(Math.exp(-(drStart - EPS_FD) * startTime));
    when(mockProvs[2].discountFactor(EUR, END_DATE)).thenReturn(Math.exp(-(drEnd + EPS_FD) * endTime));
    when(mockProvs[3].discountFactor(EUR, END_DATE)).thenReturn(Math.exp(-(drEnd - EPS_FD) * endTime));
    double startUp = PRICER.parSpread(TERM_DEPOSIT, mockProvs[0]);
    double startDw = PRICER.parSpread(TERM_DEPOSIT, mockProvs[1]);
    double endUp = PRICER.parSpread(TERM_DEPOSIT, mockProvs[2]);
    double endDw = PRICER.parSpread(TERM_DEPOSIT, mockProvs[3]);
    double startExpected = 0.5 * (startUp - startDw) / EPS_FD;
    double endExpected = 0.5 * (endUp - endDw) / EPS_FD;
    PointSensitivityBuilder expected = ZeroRateSensitivity.of(EUR, START_DATE, startExpected);
    expected = expected.combinedWith(ZeroRateSensitivity.of(EUR, END_DATE, endExpected));
    assertTrue(computed.normalized().equalWithTolerance(expected.build().normalized(), EPS_FD));
  }
}
