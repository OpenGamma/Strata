/**
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
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.swaption.SwaptionSabrRateVolatilityDataSet;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.cms.CmsLeg;
import com.opengamma.strata.product.cms.ResolvedCms;
import com.opengamma.strata.product.cms.ResolvedCmsLeg;
import com.opengamma.strata.product.cms.ResolvedCmsTrade;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.SwapIndices;

/**
 * Test {@link SabrExtrapolationReplicationCmsTradePricer}.
 */
@Test
public class DiscountingCmsTradePricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  // trades
  private static final LocalDate VALUATION = LocalDate.of(2015, 8, 18);
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
  private static final ResolvedCmsLeg CMS_LEG = CmsLeg.builder()
      .index(INDEX)
      .notional(NOTIONAL)
      .payReceive(RECEIVE)
      .paymentSchedule(SCHEDULE_EUR)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwapLeg PAY_LEG = RateCalculationSwapLeg.builder()
      .payReceive(PAY)
      .accrualSchedule(SCHEDULE_EUR)
      .calculation(FixedRateCalculation.of(0.01, ACT_360))
      .paymentSchedule(
          PaymentSchedule.builder().paymentFrequency(FREQUENCY).paymentDateOffset(DaysAdjustment.NONE).build())
      .notionalSchedule(
          NotionalSchedule.of(CurrencyAmount.of(EUR, NOTIONAL_VALUE)))
      .build()
      .resolve(REF_DATA);
  private static final ResolvedCms CMS_TWO_LEGS = ResolvedCms.of(CMS_LEG, PAY_LEG);
  private static final ResolvedCms CMS_ONE_LEG = ResolvedCms.of(CMS_LEG);
  private static final Payment PREMIUM = Payment.of(CurrencyAmount.of(EUR, -0.03 * NOTIONAL_VALUE), VALUATION);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(VALUATION).build();
  private static final ResolvedCmsTrade CMS_TRADE = ResolvedCmsTrade.builder()
      .product(CMS_TWO_LEGS)
      .info(TRADE_INFO)
      .build();
  private static final ResolvedCmsTrade CMS_TRADE_PREMIUM = ResolvedCmsTrade.builder()
      .product(CMS_ONE_LEG)
      .premium(PREMIUM)
      .build();
  // providers
  private static final ImmutableRatesProvider RATES_PROVIDER =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(VALUATION);
  // providers - valuation on payment date
  private static final LocalDate FIXING = LocalDate.of(2016, 10, 19); // fixing for the second period.
  private static final double OBS_INDEX = 0.013;
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.of(FIXING, OBS_INDEX);
  private static final LocalDate PAYMENT = LocalDate.of(2017, 10, 23); // payment date of the second payment
  private static final ImmutableRatesProvider RATES_PROVIDER_ON_PAY =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(PAYMENT, TIME_SERIES);
  // pricers
  private static final DiscountingCmsProductPricer PRODUCT_PRICER = DiscountingCmsProductPricer.DEFAULT;
  private static final DiscountingPaymentPricer PREMIUM_PRICER = DiscountingPaymentPricer.DEFAULT;
  private static final DiscountingCmsTradePricer TRADE_PRICER = DiscountingCmsTradePricer.DEFAULT;
  private static final double TOL = 1.0e-13;

  public void test_presentValue() {
    MultiCurrencyAmount pv1 = TRADE_PRICER.presentValue(CMS_TRADE_PREMIUM, RATES_PROVIDER);
    MultiCurrencyAmount pv2 = TRADE_PRICER.presentValue(CMS_TRADE, RATES_PROVIDER);
    MultiCurrencyAmount pvProd1 = PRODUCT_PRICER.presentValue(CMS_ONE_LEG, RATES_PROVIDER);
    MultiCurrencyAmount pvProd2 = PRODUCT_PRICER.presentValue(CMS_TWO_LEGS, RATES_PROVIDER);
    CurrencyAmount pvPrem = PREMIUM_PRICER.presentValue(PREMIUM, RATES_PROVIDER);
    assertEquals(pv1, pvProd1.plus(pvPrem));
    assertEquals(pv2, pvProd2);
  }

  public void test_presentValueSensitivity() {
    PointSensitivities pt1 = TRADE_PRICER.presentValueSensitivity(CMS_TRADE_PREMIUM, RATES_PROVIDER);
    PointSensitivities pt2 = TRADE_PRICER.presentValueSensitivity(CMS_TRADE, RATES_PROVIDER);
    PointSensitivityBuilder ptProd1 = PRODUCT_PRICER.presentValueSensitivity(CMS_ONE_LEG, RATES_PROVIDER);
    PointSensitivityBuilder ptProd2 = PRODUCT_PRICER.presentValueSensitivity(CMS_TWO_LEGS, RATES_PROVIDER);
    PointSensitivityBuilder ptPrem = PREMIUM_PRICER.presentValueSensitivity(PREMIUM, RATES_PROVIDER);
    assertEquals(pt1, ptProd1.combinedWith(ptPrem).build());
    assertEquals(pt2, ptProd2.build());
  }

  public void test_currencyExposure() {
    MultiCurrencyAmount computed1 = TRADE_PRICER.currencyExposure(CMS_TRADE_PREMIUM, RATES_PROVIDER);
    MultiCurrencyAmount computed2 = TRADE_PRICER.currencyExposure(CMS_TRADE, RATES_PROVIDER);
    MultiCurrencyAmount pv1 = TRADE_PRICER.presentValue(CMS_TRADE_PREMIUM, RATES_PROVIDER);
    PointSensitivities pt1 = TRADE_PRICER.presentValueSensitivity(CMS_TRADE_PREMIUM, RATES_PROVIDER);
    MultiCurrencyAmount expected1 = RATES_PROVIDER.currencyExposure(pt1).plus(pv1);
    MultiCurrencyAmount pv2 = TRADE_PRICER.presentValue(CMS_TRADE, RATES_PROVIDER);
    PointSensitivities pt2 = TRADE_PRICER.presentValueSensitivity(CMS_TRADE, RATES_PROVIDER);
    MultiCurrencyAmount expected2 = RATES_PROVIDER.currencyExposure(pt2).plus(pv2);
    assertEquals(computed1.getAmount(EUR).getAmount(), expected1.getAmount(EUR).getAmount(), NOTIONAL_VALUE * TOL);
    assertEquals(computed2.getAmount(EUR).getAmount(), expected2.getAmount(EUR).getAmount(), NOTIONAL_VALUE * TOL);
  }

  public void test_currentCash() {
    MultiCurrencyAmount cc1 = TRADE_PRICER.currentCash(CMS_TRADE_PREMIUM, RATES_PROVIDER);
    MultiCurrencyAmount cc2 = TRADE_PRICER.currentCash(CMS_TRADE, RATES_PROVIDER);
    assertEquals(cc1, MultiCurrencyAmount.of(PREMIUM.getValue()));
    assertEquals(cc2, MultiCurrencyAmount.of(CurrencyAmount.zero(EUR)));
  }

  public void test_currentCash_onPay() {
    MultiCurrencyAmount cc1 = TRADE_PRICER.currentCash(CMS_TRADE_PREMIUM, RATES_PROVIDER_ON_PAY);
    MultiCurrencyAmount cc2 = TRADE_PRICER.currentCash(CMS_TRADE, RATES_PROVIDER_ON_PAY);
    MultiCurrencyAmount ccProd1 = PRODUCT_PRICER.currentCash(CMS_ONE_LEG, RATES_PROVIDER_ON_PAY);
    MultiCurrencyAmount ccProd2 = PRODUCT_PRICER.currentCash(CMS_TWO_LEGS, RATES_PROVIDER_ON_PAY);
    assertEquals(cc1, ccProd1);
    assertEquals(cc2, ccProd2);
  }

}
