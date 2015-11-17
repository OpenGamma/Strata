/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_6M;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.Guavate.toImmutableMap;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.math.impl.differentiation.FiniteDifferenceType;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;

/**
 * Test {@link CurveGammaCalculator}.
 */
@Test
public class CurveGammaCalculatorTest {

  // Data, based on RatesProviderDataSets.SINGLE_USD but different valuation date
  private static final LocalDate VAL_DATE_2015_04_27 = LocalDate.of(2015, 4, 27);
  private static final Curve USD_SINGLE_CURVE = InterpolatedNodalCurve.of(
      Curves.zeroRates(RatesProviderDataSets.USD_SINGLE_NAME, ACT_360),
      RatesProviderDataSets.TIMES_1,
      RatesProviderDataSets.RATES_1_1,
      RatesProviderDataSets.INTERPOLATOR);
  private static final Map<Currency, Curve> USD_SINGLE_CCY_MAP = ImmutableMap.of(USD, USD_SINGLE_CURVE);
  private static final Map<Index, Curve> USD_SINGLE_IND_MAP = ImmutableMap.of(
      USD_FED_FUND, USD_SINGLE_CURVE,
      USD_LIBOR_3M, USD_SINGLE_CURVE,
      USD_LIBOR_6M, USD_SINGLE_CURVE);
  private static final ImmutableRatesProvider SINGLE = ImmutableRatesProvider.builder()
      .valuationDate(VAL_DATE_2015_04_27)
      .discountCurves(USD_SINGLE_CCY_MAP)
      .indexCurves(USD_SINGLE_IND_MAP)
      .build();
  private static final Currency SINGLE_CURRENCY = Currency.USD;
  // Conventions
  private static final BusinessDayAdjustment BDA_MF = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, USNY);
  private static final BusinessDayAdjustment BDA_P = BusinessDayAdjustment.of(PRECEDING, USNY);
  // Instrument
  private static final Swap SWAP = swapUsd(LocalDate.of(2016, 6, 30), LocalDate.of(2022, 6, 30), RECEIVE,
      NotionalSchedule.of(USD, 10_000_000), 0.01);
  // Calculators and pricers
  private static final DiscountingSwapProductPricer PRICER_SWAP = DiscountingSwapProductPricer.DEFAULT;
  private static final double FD_SHIFT = 1.0E-5;
  private static final CurveGammaCalculator GAMMA_CAL =
      new CurveGammaCalculator(FiniteDifferenceType.CENTRAL, FD_SHIFT);
  // Constants
  private static final double TOLERANCE_GAMMA = 1.0E+1;

  //-------------------------------------------------------------------------
  public void semiParallelGammaValue() {
    ImmutableRatesProvider provider = SINGLE;
    NodalCurve curve = Iterables.getOnlyElement(provider.getDiscountCurves().values()).toNodalCurve();
    Currency curveCurrency = SINGLE_CURRENCY;
    DoubleArray y = curve.getYValues();
    int nbNode = y.size();
    DoubleArray gammaExpected = DoubleArray.of(nbNode, i -> {
      double[][][] yBumped = new double[2][2][nbNode];
      double[][] pv = new double[2][2];
      for (int pmi = 0; pmi < 2; pmi++) {
        for (int pmP = 0; pmP < 2; pmP++) {
          yBumped[pmi][pmP] = y.toArray();
          yBumped[pmi][pmP][i] += (pmi == 0 ? 1.0 : -1.0) * FD_SHIFT;
          for (int j = 0; j < nbNode; j++) {
            yBumped[pmi][pmP][j] += (pmP == 0 ? 1.0 : -1.0) * FD_SHIFT;
          }
          Curve curveBumped = curve.withYValues(DoubleArray.copyOf(yBumped[pmi][pmP]));
          ImmutableRatesProvider providerBumped = provider.toBuilder()
              .discountCurves(provider.getDiscountCurves().keySet().stream()
                  .collect(toImmutableMap(Function.identity(), k -> curveBumped)))
              .indexCurves(provider.getIndexCurves().keySet().stream()
                  .collect(toImmutableMap(Function.identity(), k -> curveBumped)))
              .build();
          pv[pmi][pmP] = PRICER_SWAP.presentValue(SWAP, providerBumped).getAmount(USD).getAmount();
        }
      }
      return (pv[1][1] - pv[1][0] - pv[0][1] + pv[0][0]) / (4 * FD_SHIFT * FD_SHIFT);
    });
    CurveCurrencyParameterSensitivity sensitivityComputed = GAMMA_CAL.calculateSemiParallelGamma(
        curve,
        curveCurrency,
        c -> buildSensitivities(c, provider));
    assertEquals(sensitivityComputed.getMetadata(), curve.getMetadata());
    DoubleArray gammaComputed = sensitivityComputed.getSensitivity();
    assertTrue(gammaComputed.equalWithTolerance(gammaExpected, TOLERANCE_GAMMA));
  }

  // Checks that different finite difference types and shifts give similar results.
  public void semiParallelGammaCoherency() {
    ImmutableRatesProvider provider = SINGLE;
    NodalCurve curve = Iterables.getOnlyElement(provider.getDiscountCurves().values()).toNodalCurve();
    Currency curveCurrency = SINGLE_CURRENCY;
    double toleranceCoherency = 1.0E+5;
    CurveGammaCalculator calculatorForward5 = new CurveGammaCalculator(FiniteDifferenceType.FORWARD, FD_SHIFT);
    CurveGammaCalculator calculatorBackward5 = new CurveGammaCalculator(FiniteDifferenceType.BACKWARD, FD_SHIFT);
    CurveGammaCalculator calculatorCentral4 = new CurveGammaCalculator(FiniteDifferenceType.CENTRAL, 1.0E-4);
    DoubleArray gammaCentral5 = GAMMA_CAL.calculateSemiParallelGamma(
        curve, curveCurrency, c -> buildSensitivities(c, provider)).getSensitivity();

    DoubleArray gammaForward5 = calculatorForward5.calculateSemiParallelGamma(
        curve, curveCurrency, c -> buildSensitivities(c, provider)).getSensitivity();
    assertTrue(gammaForward5.equalWithTolerance(gammaCentral5, toleranceCoherency));

    DoubleArray gammaBackward5 = calculatorBackward5.calculateSemiParallelGamma(
        curve, curveCurrency, c -> buildSensitivities(c, provider)).getSensitivity();
    assertTrue(gammaForward5.equalWithTolerance(gammaBackward5, toleranceCoherency));

    DoubleArray gammaCentral4 = calculatorCentral4.calculateSemiParallelGamma(
        curve, curveCurrency, c -> buildSensitivities(c, provider)).getSensitivity();
    assertTrue(gammaForward5.equalWithTolerance(gammaCentral4, toleranceCoherency));
  }

  //-------------------------------------------------------------------------
  private static CurveCurrencyParameterSensitivity buildSensitivities(NodalCurve bumpedCurve, ImmutableRatesProvider ratesProvider) {
    RatesProvider bumpedRatesProvider = ratesProvider.toBuilder()
        .discountCurves(ratesProvider.getDiscountCurves().keySet().stream()
            .collect(toImmutableMap(Function.identity(), k -> bumpedCurve)))
        .indexCurves(ratesProvider.getIndexCurves().keySet().stream()
            .collect(toImmutableMap(Function.identity(), k -> bumpedCurve)))
        .build();
    PointSensitivities pointSensitivities = PRICER_SWAP.presentValueSensitivity(SWAP, bumpedRatesProvider).build();
    CurveCurrencyParameterSensitivities paramSensitivities = bumpedRatesProvider.curveParameterSensitivity(pointSensitivities);
    return Iterables.getOnlyElement(paramSensitivities.getSensitivities());
  }

  // swap USD standard conventions- TODO: replace by a template when available
  private static Swap swapUsd(LocalDate start, LocalDate end, PayReceive payReceive,
      NotionalSchedule notional, double fixedRate) {
    SwapLeg fixedLeg =
        fixedLeg(start, end, Frequency.P6M, payReceive, notional, fixedRate, StubConvention.SHORT_INITIAL);
    SwapLeg iborLeg =
        iborLeg(start, end, USD_LIBOR_3M, (payReceive == PAY) ? RECEIVE : PAY, notional, StubConvention.SHORT_INITIAL);
    return Swap.of(fixedLeg, iborLeg);
  }

  // fixed rate leg
  private static SwapLeg fixedLeg(
      LocalDate start, LocalDate end, Frequency frequency,
      PayReceive payReceive, NotionalSchedule notional, double fixedRate, StubConvention stubConvention) {

    return RateCalculationSwapLeg.builder()
        .payReceive(payReceive)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(start)
            .endDate(end)
            .frequency(frequency)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(stubConvention)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(frequency)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.builder()
            .dayCount(THIRTY_U_360)
            .rate(ValueSchedule.of(fixedRate))
            .build())
        .build();
  }

  // fixed rate leg
  private static SwapLeg iborLeg(
      LocalDate start, LocalDate end, IborIndex index,
      PayReceive payReceive, NotionalSchedule notional, StubConvention stubConvention) {
    Frequency freq = Frequency.of(index.getTenor().getPeriod());
    return RateCalculationSwapLeg.builder()
        .payReceive(payReceive)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(start)
            .endDate(end)
            .frequency(freq)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(stubConvention)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(freq)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.builder()
            .index(index)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, index.getFixingCalendar(), BDA_P))
            .build())
        .build();
  }

}
