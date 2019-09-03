/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.cms;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapLegPricer;
import com.opengamma.strata.pricer.swaption.SabrParametersSwaptionVolatilities;
import com.opengamma.strata.pricer.swaption.SwaptionSabrRateVolatilityDataSet;
import com.opengamma.strata.product.cms.CmsLeg;
import com.opengamma.strata.product.cms.ResolvedCms;
import com.opengamma.strata.product.cms.ResolvedCmsLeg;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.SwapIndices;

/**
 * Test {@link SabrExtrapolationReplicationCmsProductPricer}.
 */
public class SabrExtrapolationReplicationCmsProductPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  // CMS products
  private static final SwapIndex INDEX = SwapIndices.EUR_EURIBOR_1100_5Y;
  private static final LocalDate START = LocalDate.of(2015, 10, 21);
  private static final LocalDate END = LocalDate.of(2020, 10, 21);
  private static final Frequency FREQUENCY = Frequency.P12M;
  private static final BusinessDayAdjustment BUSS_ADJ_EUR =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, EUTA);
  private static final PeriodicSchedule SCHEDULE_EUR =
      PeriodicSchedule.of(START, END, FREQUENCY, BUSS_ADJ_EUR, StubConvention.NONE, RollConventions.NONE);
  private static final double NOTIONAL_VALUE = 1.0e6;
  private static final ValueSchedule NOTIONAL = ValueSchedule.of(NOTIONAL_VALUE);
  private static final ValueSchedule CAP = ValueSchedule.of(0.0145);
  private static final ResolvedCmsLeg CMS_LEG = CmsLeg.builder()
      .index(INDEX)
      .notional(NOTIONAL)
      .payReceive(RECEIVE)
      .paymentSchedule(SCHEDULE_EUR)
      .capSchedule(CAP)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwapLeg PAY_LEG = RateCalculationSwapLeg.builder()
      .payReceive(PAY)
      .accrualSchedule(SCHEDULE_EUR)
      .calculation(
          FixedRateCalculation.of(0.0035, ACT_360))
      .paymentSchedule(
          PaymentSchedule.builder().paymentFrequency(FREQUENCY).paymentDateOffset(DaysAdjustment.NONE).build())
      .notionalSchedule(
          NotionalSchedule.of(CurrencyAmount.of(EUR, NOTIONAL_VALUE)))
      .build()
      .resolve(REF_DATA);
  private static final ResolvedCms CMS_TWO_LEGS = ResolvedCms.of(CMS_LEG, PAY_LEG);
  private static final ResolvedCms CMS_ONE_LEG = ResolvedCms.of(CMS_LEG);
  // providers
  private static final LocalDate VALUATION = LocalDate.of(2015, 8, 18);
  private static final ImmutableRatesProvider RATES_PROVIDER =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(VALUATION);
  private static final SabrParametersSwaptionVolatilities VOLATILITIES =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(VALUATION, true);
  // providers - valuation on payment date
  private static final LocalDate FIXING = LocalDate.of(2016, 10, 19); // fixing for the second period.
  private static final double OBS_INDEX = 0.013;
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.of(FIXING, OBS_INDEX);
  private static final LocalDate PAYMENT = LocalDate.of(2017, 10, 23); // payment date of the second payment
  private static final ImmutableRatesProvider RATES_PROVIDER_ON_PAY =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(PAYMENT, TIME_SERIES);
  private static final SabrParametersSwaptionVolatilities VOLATILITIES_ON_PAY =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(PAYMENT, true);
  // pricers
  private static final double CUT_OFF_STRIKE = 0.10;
  private static final double MU = 2.50;
  private static final SabrExtrapolationReplicationCmsPeriodPricer PERIOD_PRICER =
      SabrExtrapolationReplicationCmsPeriodPricer.of(CUT_OFF_STRIKE, MU);
  private static final SabrExtrapolationReplicationCmsLegPricer CMS_LEG_PRICER =
      new SabrExtrapolationReplicationCmsLegPricer(PERIOD_PRICER);
  private static final DiscountingSwapLegPricer SWAP_LEG_PRICER = DiscountingSwapLegPricer.DEFAULT;
  private static final SabrExtrapolationReplicationCmsProductPricer PRODUCT_PRICER =
      new SabrExtrapolationReplicationCmsProductPricer(CMS_LEG_PRICER, SWAP_LEG_PRICER);
  private static final double TOL = 1.0e-13;

  @Test
  public void test_presentValue() {
    MultiCurrencyAmount pv1 = PRODUCT_PRICER.presentValue(CMS_ONE_LEG, RATES_PROVIDER, VOLATILITIES);
    MultiCurrencyAmount pv2 = PRODUCT_PRICER.presentValue(CMS_TWO_LEGS, RATES_PROVIDER, VOLATILITIES);
    CurrencyAmount pvCms = CMS_LEG_PRICER.presentValue(CMS_LEG, RATES_PROVIDER, VOLATILITIES);
    CurrencyAmount pvPay = SWAP_LEG_PRICER.presentValue(PAY_LEG, RATES_PROVIDER);
    assertThat(pv1).isEqualTo(MultiCurrencyAmount.of(pvCms));
    assertThat(pv2).isEqualTo(MultiCurrencyAmount.of(pvCms).plus(pvPay));
  }

  @Test
  public void test_presentValueSensitivity() {
    PointSensitivityBuilder pt1 = PRODUCT_PRICER.presentValueSensitivityRates(CMS_ONE_LEG, RATES_PROVIDER, VOLATILITIES);
    PointSensitivityBuilder pt2 = PRODUCT_PRICER.presentValueSensitivityRates(CMS_TWO_LEGS, RATES_PROVIDER, VOLATILITIES);
    PointSensitivityBuilder ptCms =
        CMS_LEG_PRICER.presentValueSensitivityRates(CMS_LEG, RATES_PROVIDER, VOLATILITIES);
    PointSensitivityBuilder ptPay = SWAP_LEG_PRICER.presentValueSensitivity(PAY_LEG, RATES_PROVIDER);
    assertThat(pt1).isEqualTo(ptCms);
    assertThat(pt2).isEqualTo(ptCms.combinedWith(ptPay));
  }

  @Test
  public void test_presentValueSensitivitySabrParameter() {
    PointSensitivities pt1 =
        PRODUCT_PRICER.presentValueSensitivityModelParamsSabr(CMS_ONE_LEG, RATES_PROVIDER, VOLATILITIES).build();
    PointSensitivities pt2 =
        PRODUCT_PRICER.presentValueSensitivityModelParamsSabr(CMS_TWO_LEGS, RATES_PROVIDER, VOLATILITIES).build();
    PointSensitivities ptCms =
        CMS_LEG_PRICER.presentValueSensitivityModelParamsSabr(CMS_LEG, RATES_PROVIDER, VOLATILITIES).build();
    assertThat(pt1).isEqualTo(ptCms);
    assertThat(pt2).isEqualTo(ptCms);
  }

  @Test
  public void test_presentValueSensitivityStrike() {
    double sensi1 = PRODUCT_PRICER.presentValueSensitivityStrike(CMS_ONE_LEG, RATES_PROVIDER, VOLATILITIES);
    double sensi2 = PRODUCT_PRICER.presentValueSensitivityStrike(CMS_TWO_LEGS, RATES_PROVIDER, VOLATILITIES);
    double sensiCms = CMS_LEG_PRICER.presentValueSensitivityStrike(CMS_LEG, RATES_PROVIDER, VOLATILITIES);
    assertThat(sensi1).isEqualTo(sensiCms);
    assertThat(sensi2).isEqualTo(sensiCms);
  }

  @Test
  public void test_currencyExposure() {
    MultiCurrencyAmount computed1 = PRODUCT_PRICER.currencyExposure(CMS_ONE_LEG, RATES_PROVIDER, VOLATILITIES);
    MultiCurrencyAmount computed2 = PRODUCT_PRICER.currencyExposure(CMS_TWO_LEGS, RATES_PROVIDER, VOLATILITIES);
    MultiCurrencyAmount pv1 = PRODUCT_PRICER.presentValue(CMS_ONE_LEG, RATES_PROVIDER, VOLATILITIES);
    PointSensitivityBuilder pt1 = PRODUCT_PRICER.presentValueSensitivityRates(CMS_ONE_LEG, RATES_PROVIDER, VOLATILITIES);
    MultiCurrencyAmount expected1 = RATES_PROVIDER.currencyExposure(pt1.build()).plus(pv1);
    MultiCurrencyAmount pv2 = PRODUCT_PRICER.presentValue(CMS_TWO_LEGS, RATES_PROVIDER, VOLATILITIES);
    PointSensitivityBuilder pt2 = PRODUCT_PRICER.presentValueSensitivityRates(CMS_TWO_LEGS, RATES_PROVIDER, VOLATILITIES);
    MultiCurrencyAmount expected2 = RATES_PROVIDER.currencyExposure(pt2.build()).plus(pv2);
    assertThat(computed1.getAmount(EUR).getAmount()).isCloseTo(expected1.getAmount(EUR).getAmount(), offset(NOTIONAL_VALUE * TOL));
    assertThat(computed2.getAmount(EUR).getAmount()).isCloseTo(expected2.getAmount(EUR).getAmount(), offset(NOTIONAL_VALUE * TOL));
  }

  @Test
  public void test_currentCash() {
    MultiCurrencyAmount cc1 = PRODUCT_PRICER.currentCash(CMS_ONE_LEG, RATES_PROVIDER, VOLATILITIES);
    MultiCurrencyAmount cc2 = PRODUCT_PRICER.currentCash(CMS_TWO_LEGS, RATES_PROVIDER, VOLATILITIES);
    assertThat(cc1).isEqualTo(MultiCurrencyAmount.of(CurrencyAmount.zero(EUR)));
    assertThat(cc2).isEqualTo(MultiCurrencyAmount.of(CurrencyAmount.zero(EUR)));
  }

  @Test
  public void test_currentCash_onPay() {
    MultiCurrencyAmount cc1 = PRODUCT_PRICER.currentCash(CMS_ONE_LEG, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY);
    MultiCurrencyAmount cc2 = PRODUCT_PRICER.currentCash(CMS_TWO_LEGS, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY);
    CurrencyAmount ccCms = CMS_LEG_PRICER.currentCash(CMS_LEG, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY);
    CurrencyAmount ccPay = SWAP_LEG_PRICER.currentCash(PAY_LEG, RATES_PROVIDER_ON_PAY);
    assertThat(cc1).isEqualTo(MultiCurrencyAmount.of(ccCms));
    assertThat(cc2).isEqualTo(MultiCurrencyAmount.of(ccCms).plus(ccPay));
  }

  @Test
  public void test_pvExplain() {
    ExplainMap explain1 = PRODUCT_PRICER.explainPresentValue(CMS_ONE_LEG, RATES_PROVIDER, VOLATILITIES);
    assertThat(explain1.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("CmsSwap");
    assertThat(explain1.get(ExplainKey.LEGS).get()).hasSize(1);
    ExplainMap explain2 = PRODUCT_PRICER.explainPresentValue(CMS_TWO_LEGS, RATES_PROVIDER, VOLATILITIES);
    assertThat(explain2.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("CmsSwap");
    assertThat(explain2.get(ExplainKey.LEGS).get()).hasSize(2);
    ExplainMap explainCms = CMS_LEG_PRICER.explainPresentValue(CMS_LEG, RATES_PROVIDER, VOLATILITIES);
    ExplainMap explainOther = SWAP_LEG_PRICER.explainPresentValue(CMS_TWO_LEGS.getPayLeg().get(), RATES_PROVIDER);
    assertThat(explain2.get(ExplainKey.LEGS).get().get(0)).isEqualTo(explainCms);
    assertThat(explain2.get(ExplainKey.LEGS).get().get(1)).isEqualTo(explainOther);
  }

}
