/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.cms;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.basics.value.ValueStep;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.impl.cms.DiscountingCmsPeriodPricer;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.pricer.swaption.SwaptionSabrRateVolatilityDataSet;
import com.opengamma.strata.product.cms.CmsLeg;
import com.opengamma.strata.product.cms.CmsPeriod;
import com.opengamma.strata.product.cms.ResolvedCmsLeg;
import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.SwapIndices;

/**
 * Test {@link DiscountingCmsLegPricer}.
 */
@Test
public class DiscountingCmsLegPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  // CMS legs
  private static final SwapIndex INDEX = SwapIndices.EUR_EURIBOR_1100_5Y;
  private static final LocalDate START = LocalDate.of(2015, 10, 21);
  private static final LocalDate END = LocalDate.of(2020, 10, 21);
  private static final Frequency FREQUENCY = Frequency.P12M;
  private static final BusinessDayAdjustment BUSS_ADJ_EUR =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, EUTA);
  private static final PeriodicSchedule SCHEDULE_EUR =
      PeriodicSchedule.of(START, END, FREQUENCY, BUSS_ADJ_EUR, StubConvention.NONE, RollConventions.NONE);
  private static final List<ValueStep> NOTIONAL_STEPS = new ArrayList<ValueStep>();
  private static final double NOTIONAL_VALUE_0 = 100_000_000;
  private static final double NOTIONAL_VALUE_1 = 1.1e6;
  private static final double NOTIONAL_VALUE_2 = 0.9e6;
  private static final double NOTIONAL_VALUE_3 = 1.2e6;
  static {
    NOTIONAL_STEPS.add(ValueStep.of(1, ValueAdjustment.ofReplace(NOTIONAL_VALUE_1)));
    NOTIONAL_STEPS.add(ValueStep.of(2, ValueAdjustment.ofReplace(NOTIONAL_VALUE_2)));
    NOTIONAL_STEPS.add(ValueStep.of(3, ValueAdjustment.ofReplace(NOTIONAL_VALUE_3)));
  }
  private static final ValueSchedule NOTIONAL = ValueSchedule.of(NOTIONAL_VALUE_0, NOTIONAL_STEPS);
  private static final ResolvedCmsLeg COUPON_LEG = CmsLeg.builder()
      .index(INDEX)
      .notional(NOTIONAL)
      .payReceive(PAY)
      .paymentSchedule(SCHEDULE_EUR)
      .build()
      .resolve(REF_DATA);
  // providers
  private static final LocalDate VALUATION = LocalDate.of(2015, 8, 18);
  private static final ImmutableRatesProvider RATES_PROVIDER =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(VALUATION);
  // providers - valuation after the first payment
  private static final LocalDate AFTER_PAYMENT = LocalDate.of(2016, 11, 25);  // the first cms payment is 2016-10-21.
  private static final LocalDate FIXING = LocalDate.of(2016, 10, 19); // fixing for the second period.
  private static final double OBS_INDEX = 0.013;
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.of(FIXING, OBS_INDEX);
  private static final ImmutableRatesProvider RATES_PROVIDER_AFTER_PERIOD =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(AFTER_PAYMENT, TIME_SERIES);
  // providers - valuation on the payment date
  private static final LocalDate PAYMENT = LocalDate.of(2017, 10, 23); // payment date of the second payment
  private static final ImmutableRatesProvider RATES_PROVIDER_ON_PAY =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(PAYMENT, TIME_SERIES);
  // providers - valuation after maturity date
  private static final LocalDate ENDED = END.plusDays(7);
  private static final ImmutableRatesProvider RATES_PROVIDER_ENDED =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(ENDED);
  // pricers
  private static final double EPS = 1.0e-5;
  private static final double TOLERANCE_PV = 1.0E-2;
  private static final double TOLERANCE_DELTA = 1.0E+3;
  private static final DiscountingCmsPeriodPricer PERIOD_PRICER = DiscountingCmsPeriodPricer.DEFAULT;
  private static final DiscountingCmsLegPricer LEG_PRICER =
      new DiscountingCmsLegPricer(PERIOD_PRICER);
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(EPS);

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    CurrencyAmount computed = LEG_PRICER.presentValue(COUPON_LEG, RATES_PROVIDER);
    double expected = 0d;
    List<CmsPeriod> cms = COUPON_LEG.getCmsPeriods();
    int size = cms.size();
    for (int i = 0; i < size; ++i) {
      expected += PERIOD_PRICER.presentValue(cms.get(i), RATES_PROVIDER).getAmount();
    }
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected, TOLERANCE_PV);
  }

  public void test_presentValue_afterPay() {
    CurrencyAmount computed = LEG_PRICER.presentValue(COUPON_LEG, RATES_PROVIDER_AFTER_PERIOD);
    double expected = 0d;
    List<CmsPeriod> cms = COUPON_LEG.getCmsPeriods();
    int size = cms.size();
    for (int i = 1; i < size; ++i) {
      expected += PERIOD_PRICER.presentValue(
          cms.get(i), RATES_PROVIDER_AFTER_PERIOD).getAmount();
    }
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected, TOLERANCE_PV);
  }

  public void test_presentValue_ended() {
    CurrencyAmount computed = LEG_PRICER.presentValue(COUPON_LEG, RATES_PROVIDER_ENDED);
    assertEquals(computed, CurrencyAmount.zero(EUR));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    PointSensitivityBuilder point = LEG_PRICER.presentValueSensitivity(COUPON_LEG, RATES_PROVIDER);
    CurrencyParameterSensitivities computed = RATES_PROVIDER.parameterSensitivity(point.build());
    CurrencyParameterSensitivities expected =
        FD_CAL.sensitivity(RATES_PROVIDER, p -> LEG_PRICER.presentValue(COUPON_LEG, p));
    assertTrue(computed.equalWithTolerance(expected, TOLERANCE_DELTA));
  }

  public void test_presentValueSensitivity_afterPay() {
    PointSensitivityBuilder point =
        LEG_PRICER.presentValueSensitivity(COUPON_LEG, RATES_PROVIDER_AFTER_PERIOD);
    CurrencyParameterSensitivities computed = RATES_PROVIDER_AFTER_PERIOD.parameterSensitivity(point.build());
    CurrencyParameterSensitivities expected = FD_CAL.sensitivity(
        RATES_PROVIDER_AFTER_PERIOD, p -> LEG_PRICER.presentValue(COUPON_LEG, p));
    assertTrue(computed.equalWithTolerance(expected, TOLERANCE_DELTA));
  }

  public void test_presentValueSensitivity_ended() {
    PointSensitivityBuilder computed = LEG_PRICER.presentValueSensitivity(COUPON_LEG, RATES_PROVIDER_ENDED);
    assertEquals(computed, PointSensitivityBuilder.none());
  }

  //-------------------------------------------------------------------------
  public void test_currentCash() {
    CurrencyAmount computed = LEG_PRICER.currentCash(COUPON_LEG, RATES_PROVIDER);
    assertEquals(computed, CurrencyAmount.zero(EUR));
  }

  public void test_currentCash_onPay() {
    CurrencyAmount computed = LEG_PRICER.currentCash(COUPON_LEG, RATES_PROVIDER_ON_PAY);
    assertEquals(computed.getAmount(), -NOTIONAL_VALUE_1 * OBS_INDEX * 367d / 360d, TOLERANCE_PV);
  }

}
