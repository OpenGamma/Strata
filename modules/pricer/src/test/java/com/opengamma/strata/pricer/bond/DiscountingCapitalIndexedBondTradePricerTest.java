/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ICMA;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;
import static com.opengamma.strata.basics.index.PriceIndices.US_CPI_U;
import static com.opengamma.strata.market.value.CompoundedRateType.CONTINUOUS;
import static com.opengamma.strata.market.value.CompoundedRateType.PERIODIC;
import static com.opengamma.strata.product.bond.YieldConvention.US_IL_REAL;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

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
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.view.IssuerCurveDiscountFactors;
import com.opengamma.strata.pricer.impl.bond.DiscountingCapitalIndexedBondPaymentPeriodPricer;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityLink;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.UnitSecurity;
import com.opengamma.strata.product.bond.CapitalIndexedBond;
import com.opengamma.strata.product.bond.CapitalIndexedBondPaymentPeriod;
import com.opengamma.strata.product.bond.CapitalIndexedBondTrade;
import com.opengamma.strata.product.swap.InflationRateCalculation;

/**
 * Test {@link DiscountingCapitalIndexedBondTradePricer}.
 */
@Test
public class DiscountingCapitalIndexedBondTradePricerTest {

  // detachment date (for nonzero ex-coupon days) < valuation date < payment date
  private static final LocalDate VALUATION = LocalDate.of(2014, 7, 13);
  private static final LocalDate PAYMENT = LocalDate.of(2014, 7, 15);
  private static final LocalDateDoubleTimeSeries TS = CapitalIndexedBondCurveDataSet.getTimeSeries(VALUATION);
  private static final ImmutableRatesProvider RATES_PROVIDER =
      CapitalIndexedBondCurveDataSet.getRatesProvider(VALUATION, TS);
  private static final ImmutableRatesProvider RATES_PROVIDER_ON_PAY =
      CapitalIndexedBondCurveDataSet.getRatesProvider(PAYMENT, TS);
  private static final LegalEntityDiscountingProvider ISSUER_RATES_PROVIDER =
      CapitalIndexedBondCurveDataSet.getLegalEntityDiscountingProvider(VALUATION);
  private static final IssuerCurveDiscountFactors ISSUER_DISCOUNT_FACTORS =
      CapitalIndexedBondCurveDataSet.getIssuerCurveDiscountFactors(VALUATION);
  private static final double Z_SPREAD = 0.015;
  private static final int PERIOD_PER_YEAR = 4;

  private static final double NOTIONAL = 10_000_000d;
  private static final double START_INDEX = 198.47742;
  private static final double REAL_COUPON_VALUE = 0.01;
  private static final ValueSchedule REAL_COUPON = ValueSchedule.of(REAL_COUPON_VALUE);
  private static final InflationRateCalculation RATE_CALC = InflationRateCalculation.builder()
      .gearing(REAL_COUPON)
      .index(US_CPI_U)
      .lag(Period.ofMonths(3))
      .interpolated(true)
      .build();
  private static final BusinessDayAdjustment EX_COUPON_ADJ =
      BusinessDayAdjustment.of(BusinessDayConventions.PRECEDING, USNY);
  private static final DaysAdjustment SETTLE_OFFSET = DaysAdjustment.ofBusinessDays(3, USNY);
  private static final StandardId LEGAL_ENTITY = CapitalIndexedBondCurveDataSet.getIssuerId();
  private static final LocalDate START = LocalDate.of(2006, 1, 15);
  private static final LocalDate END = LocalDate.of(2016, 1, 15);
  private static final Frequency FREQUENCY = Frequency.P6M;
  private static final BusinessDayAdjustment BUSINESS_ADJUST =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, USNY);
  private static final PeriodicSchedule SCHEDULE =
      PeriodicSchedule.of(START, END, FREQUENCY, BUSINESS_ADJUST, StubConvention.NONE, RollConventions.NONE);
  private static final CapitalIndexedBond PRODUCT = CapitalIndexedBond.builder()
      .notional(NOTIONAL)
      .currency(USD)
      .dayCount(ACT_ACT_ICMA)
      .rateCalculation(RATE_CALC)
      .legalEntityId(LEGAL_ENTITY)
      .yieldConvention(US_IL_REAL)
      .settlementDateOffset(SETTLE_OFFSET)
      .periodicSchedule(SCHEDULE)
      .startIndexValue(START_INDEX)
      .build();
  private static final DaysAdjustment EX_COUPON = DaysAdjustment.ofCalendarDays(-5, EX_COUPON_ADJ);
  private static final CapitalIndexedBond PRODUCT_EX_COUPON = CapitalIndexedBond.builder()
      .notional(NOTIONAL)
      .currency(USD)
      .dayCount(ACT_ACT_ICMA)
      .rateCalculation(RATE_CALC)
      .legalEntityId(LEGAL_ENTITY)
      .yieldConvention(US_IL_REAL)
      .settlementDateOffset(SETTLE_OFFSET)
      .periodicSchedule(SCHEDULE)
      .exCouponPeriod(EX_COUPON)
      .startIndexValue(START_INDEX)
      .build();

  private static final long QUANTITY = 100L;
  private static final StandardId SECURITY_ID = StandardId.of("OG-Ticker", "BOND1");
  private static final LocalDate SETTLEMENT_BEFORE = VALUATION.minusWeeks(1);
  private static final LocalDate SETTLEMENT_EARLY = VALUATION;
  private static final LocalDate SETTLEMENT_LATE = LocalDate.of(2015, 2, 19);
  private static final LocalDate SETTLEMENT_STANDARD = SETTLE_OFFSET.adjust(VALUATION);
  private static final TradeInfo TRADE_INFO_SETTLED = TradeInfo.builder().settlementDate(SETTLEMENT_BEFORE).build();
  private static final TradeInfo TRADE_INFO_EARLY = TradeInfo.builder().settlementDate(SETTLEMENT_EARLY).build();
  private static final TradeInfo TRADE_INFO_LATE = TradeInfo.builder().settlementDate(SETTLEMENT_LATE).build();
  private static final TradeInfo TRADE_INFO_STANDARD = TradeInfo.builder().settlementDate(SETTLEMENT_STANDARD).build();
  private static final Security<CapitalIndexedBond> SECURITY =
      UnitSecurity.builder(PRODUCT).standardId(SECURITY_ID).build();
  private static final Security<CapitalIndexedBond> SECURITY_EX_COUPON =
      UnitSecurity.builder(PRODUCT_EX_COUPON).standardId(SECURITY_ID).build();
  private static final SecurityLink<CapitalIndexedBond> SECURITY_LINK = SecurityLink.resolved(SECURITY);
  private static final SecurityLink<CapitalIndexedBond> SECURITY_LINK_EX_COUPON = SecurityLink.resolved(SECURITY_EX_COUPON);
  private static final CapitalIndexedBondTrade TRADE_SETTLED =
      CapitalIndexedBondTrade.of(SECURITY_LINK, TRADE_INFO_SETTLED, QUANTITY);
  private static final CapitalIndexedBondTrade TRADE_EARLY =
      CapitalIndexedBondTrade.of(SECURITY_LINK, TRADE_INFO_EARLY, QUANTITY);
  private static final CapitalIndexedBondTrade TRADE_EX_COUPON_EARLY =
      CapitalIndexedBondTrade.of(SECURITY_LINK_EX_COUPON, TRADE_INFO_EARLY, QUANTITY);
  private static final CapitalIndexedBondTrade TRADE_LATE =
      CapitalIndexedBondTrade.of(SECURITY_LINK, TRADE_INFO_LATE, QUANTITY);
  private static final CapitalIndexedBondTrade TRADE_STANDARD =
      CapitalIndexedBondTrade.of(SECURITY_LINK, TRADE_INFO_STANDARD, QUANTITY);
  
  private static final double REAL_CLEAN_PRICE = 1.0203;

  private static final double TOL = 1.0e-12;
  private static final double EPS = 1.0e-6;
  private static final DiscountingCapitalIndexedBondTradePricer PRICER = DiscountingCapitalIndexedBondTradePricer.DEFAULT;
  private static final DiscountingCapitalIndexedBondProductPricer PRODUCT_PRICER =
      DiscountingCapitalIndexedBondProductPricer.DEFAULT;
  private static final DiscountingCapitalIndexedBondPaymentPeriodPricer PERIOD_PRICER =
      DiscountingCapitalIndexedBondPaymentPeriodPricer.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(EPS);

  //-------------------------------------------------------------------------
  public void test_netAmount_standard() {
    CurrencyAmount computed = PRICER.netAmount(TRADE_STANDARD, RATES_PROVIDER, REAL_CLEAN_PRICE);
    double expected = (REAL_CLEAN_PRICE + PRODUCT_PRICER.accruedInterest(PRODUCT, SETTLEMENT_STANDARD) / NOTIONAL) *
        PERIOD_PRICER.forecastValue(TRADE_STANDARD.getSettlement(), RATES_PROVIDER);
    assertEquals(computed.getAmount(), expected, QUANTITY * NOTIONAL * TOL);
  }

  public void test_netAmount_late() {
    CurrencyAmount computed = PRICER.netAmount(TRADE_LATE, RATES_PROVIDER, REAL_CLEAN_PRICE);
    double expected = (REAL_CLEAN_PRICE + PRODUCT_PRICER.accruedInterest(PRODUCT, SETTLEMENT_LATE) / NOTIONAL) *
        PERIOD_PRICER.forecastValue(TRADE_LATE.getSettlement(), RATES_PROVIDER);
    assertEquals(computed.getAmount(), expected, QUANTITY * NOTIONAL * TOL);
  }

  public void test_netAmountSensitivity_standard() {
    PointSensitivities point = PRICER.netAmountSensitivity(TRADE_STANDARD, RATES_PROVIDER, REAL_CLEAN_PRICE).build();
    CurveCurrencyParameterSensitivities computed = RATES_PROVIDER.curveParameterSensitivity(point);
    CurveCurrencyParameterSensitivities expected =
        FD_CAL.sensitivity(RATES_PROVIDER, p -> PRICER.netAmount(TRADE_STANDARD, p, REAL_CLEAN_PRICE));
    assertTrue(computed.equalWithTolerance(expected, QUANTITY * NOTIONAL * EPS));
  }

  public void test_netAmountSensitivity_late() {
    PointSensitivities point = PRICER.netAmountSensitivity(TRADE_LATE, RATES_PROVIDER, REAL_CLEAN_PRICE).build();
    CurveCurrencyParameterSensitivities computed = RATES_PROVIDER.curveParameterSensitivity(point);
    CurveCurrencyParameterSensitivities expected =
        FD_CAL.sensitivity(RATES_PROVIDER, p -> PRICER.netAmount(TRADE_LATE, p, REAL_CLEAN_PRICE));
    assertTrue(computed.equalWithTolerance(expected, QUANTITY * NOTIONAL * EPS));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueFromCleanPrice_standard() {
    CurrencyAmount computed =
        PRICER.presentValueFromCleanPrice(TRADE_STANDARD, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE);
    CurrencyAmount netAmount = PRICER.netAmount(TRADE_STANDARD, RATES_PROVIDER, REAL_CLEAN_PRICE);
    double expected = netAmount.getAmount() *
        ISSUER_RATES_PROVIDER.repoCurveDiscountFactors(SECURITY_ID, LEGAL_ENTITY, USD).discountFactor(SETTLEMENT_STANDARD);
    assertEquals(computed.getAmount(), expected, NOTIONAL * QUANTITY * TOL);
  }

  public void test_presentValueFromCleanPrice_early() {
    CurrencyAmount computed =
        PRICER.presentValueFromCleanPrice(TRADE_EARLY, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE);
    CurrencyAmount netAmount = PRICER.netAmount(TRADE_STANDARD, RATES_PROVIDER, REAL_CLEAN_PRICE);
    CapitalIndexedBondPaymentPeriod period = PRODUCT.expand().getPeriodicPayments().get(16);
    double pvDiff = PERIOD_PRICER.presentValue(period, RATES_PROVIDER, ISSUER_DISCOUNT_FACTORS) * QUANTITY;
    double expected = -pvDiff + netAmount.getAmount() *
        ISSUER_RATES_PROVIDER.repoCurveDiscountFactors(SECURITY_ID, LEGAL_ENTITY, USD).discountFactor(SETTLEMENT_STANDARD);
    assertEquals(computed.getAmount(), expected, NOTIONAL * QUANTITY * TOL);
  }

  public void test_presentValueFromCleanPrice_early_exCoupon() {
    CurrencyAmount computed = PRICER.presentValueFromCleanPrice(
        TRADE_EX_COUPON_EARLY, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE);
    CurrencyAmount netAmount = PRICER.netAmount(TRADE_STANDARD, RATES_PROVIDER, REAL_CLEAN_PRICE);
    double expected = netAmount.getAmount() *
        ISSUER_RATES_PROVIDER.repoCurveDiscountFactors(SECURITY_ID, LEGAL_ENTITY, USD).discountFactor(SETTLEMENT_STANDARD);
    assertEquals(computed.getAmount(), expected, NOTIONAL * QUANTITY * TOL);
  }

  public void test_presentValueFromCleanPrice_late() {
    CurrencyAmount computed =
        PRICER.presentValueFromCleanPrice(TRADE_LATE, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE);
    CurrencyAmount netAmount = PRICER.netAmount(TRADE_STANDARD, RATES_PROVIDER, REAL_CLEAN_PRICE);
    CapitalIndexedBondPaymentPeriod period = PRODUCT.expand().getPeriodicPayments().get(17);
    double pvDiff = PERIOD_PRICER.presentValue(period, RATES_PROVIDER, ISSUER_DISCOUNT_FACTORS) * QUANTITY;
    double expected = pvDiff + netAmount.getAmount() *
        ISSUER_RATES_PROVIDER.repoCurveDiscountFactors(SECURITY_ID, LEGAL_ENTITY, USD).discountFactor(SETTLEMENT_STANDARD);
    assertEquals(computed.getAmount(), expected, NOTIONAL * QUANTITY * TOL);
  }

  public void test_presentValueFromCleanPriceWithZSpread_standard() {
    CurrencyAmount computed = PRICER.presentValueFromCleanPriceWithZSpread(
        TRADE_STANDARD, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurrencyAmount netAmount = PRICER.netAmount(TRADE_STANDARD, RATES_PROVIDER, REAL_CLEAN_PRICE);
    double expected = netAmount.getAmount() *
        ISSUER_RATES_PROVIDER.repoCurveDiscountFactors(SECURITY_ID, LEGAL_ENTITY, USD).discountFactor(SETTLEMENT_STANDARD);
    assertEquals(computed.getAmount(), expected, NOTIONAL * QUANTITY * TOL);
  }

  public void test_presentValueFromCleanPriceWithZSpread_early() {
    CurrencyAmount computed = PRICER.presentValueFromCleanPriceWithZSpread(
        TRADE_EARLY, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, CONTINUOUS, 0);
    CurrencyAmount netAmount = PRICER.netAmount(TRADE_STANDARD, RATES_PROVIDER, REAL_CLEAN_PRICE);
    CapitalIndexedBondPaymentPeriod period = PRODUCT.expand().getPeriodicPayments().get(16);
    double pvDiff = PERIOD_PRICER.presentValueWithZSpread(
        period, RATES_PROVIDER, ISSUER_DISCOUNT_FACTORS, Z_SPREAD, CONTINUOUS, 0) * QUANTITY;
    double expected = -pvDiff + netAmount.getAmount() *
        ISSUER_RATES_PROVIDER.repoCurveDiscountFactors(SECURITY_ID, LEGAL_ENTITY, USD).discountFactor(SETTLEMENT_STANDARD);
    assertEquals(computed.getAmount(), expected, NOTIONAL * QUANTITY * TOL);
  }

  public void test_presentValueFromCleanPriceWithZSpread_early_exCoupon() {
    CurrencyAmount computed = PRICER.presentValueFromCleanPriceWithZSpread(
        TRADE_EX_COUPON_EARLY, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, CONTINUOUS, 0);
    CurrencyAmount netAmount = PRICER.netAmount(TRADE_STANDARD, RATES_PROVIDER, REAL_CLEAN_PRICE);
    double expected = netAmount.getAmount() *
        ISSUER_RATES_PROVIDER.repoCurveDiscountFactors(SECURITY_ID, LEGAL_ENTITY, USD).discountFactor(SETTLEMENT_STANDARD);
    assertEquals(computed.getAmount(), expected, NOTIONAL * QUANTITY * TOL);
  }

  public void test_presentValueFromCleanPriceWithZSpread_late() {
    CurrencyAmount computed = PRICER.presentValueFromCleanPriceWithZSpread(
        TRADE_LATE, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, CONTINUOUS, 0);
    CurrencyAmount netAmount = PRICER.netAmount(TRADE_STANDARD, RATES_PROVIDER, REAL_CLEAN_PRICE);
    CapitalIndexedBondPaymentPeriod period = PRODUCT.expand().getPeriodicPayments().get(17);
    double pvDiff = PERIOD_PRICER.presentValueWithZSpread(
        period, RATES_PROVIDER, ISSUER_DISCOUNT_FACTORS, Z_SPREAD, CONTINUOUS, 0) * QUANTITY;
    double expected = pvDiff + netAmount.getAmount() *
        ISSUER_RATES_PROVIDER.repoCurveDiscountFactors(SECURITY_ID, LEGAL_ENTITY, USD).discountFactor(SETTLEMENT_STANDARD);
    assertEquals(computed.getAmount(), expected, NOTIONAL * QUANTITY * TOL);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivityFromRealCleanPrice_standard() {
    PointSensitivities point = PRICER.presentValueSensitivityFromRealCleanPrice(
        TRADE_STANDARD, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE).build();
    CurveCurrencyParameterSensitivities computed = ISSUER_RATES_PROVIDER.curveParameterSensitivity(point)
        .combinedWith(RATES_PROVIDER.curveParameterSensitivity(point));
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(RATES_PROVIDER,
        p -> PRICER.presentValueFromCleanPrice(TRADE_STANDARD, p, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE))
            .combinedWith(FD_CAL.sensitivity(ISSUER_RATES_PROVIDER,
            p -> PRICER.presentValueFromCleanPrice(TRADE_STANDARD, RATES_PROVIDER, p, REAL_CLEAN_PRICE)));
    assertTrue(computed.equalWithTolerance(expected, NOTIONAL * QUANTITY * EPS));
  }

  public void test_presentValueSensitivityFromRealCleanPrice_early_exCoupon() {
    PointSensitivities point = PRICER.presentValueSensitivityFromRealCleanPrice(
        TRADE_EARLY, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE).build();
    CurveCurrencyParameterSensitivities computed = ISSUER_RATES_PROVIDER.curveParameterSensitivity(point)
        .combinedWith(RATES_PROVIDER.curveParameterSensitivity(point));
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(RATES_PROVIDER,
        p -> PRICER.presentValueFromCleanPrice(TRADE_EARLY, p, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE))
        .combinedWith(FD_CAL.sensitivity(ISSUER_RATES_PROVIDER,
            p -> PRICER.presentValueFromCleanPrice(TRADE_EARLY, RATES_PROVIDER, p, REAL_CLEAN_PRICE)));
    assertTrue(computed.equalWithTolerance(expected, NOTIONAL * QUANTITY * EPS));
  }

  public void test_presentValueSensitivityFromRealCleanPrice_early() {
    PointSensitivities point = PRICER.presentValueSensitivityFromRealCleanPrice(
        TRADE_EX_COUPON_EARLY, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE).build();
    CurveCurrencyParameterSensitivities computed = ISSUER_RATES_PROVIDER.curveParameterSensitivity(point)
        .combinedWith(RATES_PROVIDER.curveParameterSensitivity(point));
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(RATES_PROVIDER,
        p -> PRICER.presentValueFromCleanPrice(TRADE_EX_COUPON_EARLY, p, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE))
        .combinedWith(FD_CAL.sensitivity(ISSUER_RATES_PROVIDER,
            p -> PRICER.presentValueFromCleanPrice(TRADE_EX_COUPON_EARLY, RATES_PROVIDER, p, REAL_CLEAN_PRICE)));
    assertTrue(computed.equalWithTolerance(expected, NOTIONAL * QUANTITY * EPS));
  }

  public void test_presentValueSensitivityFromRealCleanPrice_late() {
    PointSensitivities point = PRICER.presentValueSensitivityFromRealCleanPrice(
        TRADE_LATE, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE).build();
    CurveCurrencyParameterSensitivities computed = ISSUER_RATES_PROVIDER.curveParameterSensitivity(point)
        .combinedWith(RATES_PROVIDER.curveParameterSensitivity(point));
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(RATES_PROVIDER,
        p -> PRICER.presentValueFromCleanPrice(TRADE_LATE, p, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE))
        .combinedWith(FD_CAL.sensitivity(ISSUER_RATES_PROVIDER,
            p -> PRICER.presentValueFromCleanPrice(TRADE_LATE, RATES_PROVIDER, p, REAL_CLEAN_PRICE)));
    assertTrue(computed.equalWithTolerance(expected, NOTIONAL * QUANTITY * EPS));
  }

  public void test_presentValueSensitivityFromRealCleanPriceWithZSpread_standard() {
    PointSensitivities point = PRICER.presentValueSensitivityFromRealCleanPriceWithZSpread(
        TRADE_STANDARD, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, CONTINUOUS, 0).build();
    CurveCurrencyParameterSensitivities computed = ISSUER_RATES_PROVIDER.curveParameterSensitivity(point)
        .combinedWith(RATES_PROVIDER.curveParameterSensitivity(point));
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(RATES_PROVIDER,
        p -> PRICER.presentValueFromCleanPriceWithZSpread(
            TRADE_STANDARD, p, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, CONTINUOUS, 0))
        .combinedWith(FD_CAL.sensitivity(ISSUER_RATES_PROVIDER,
            p -> PRICER.presentValueFromCleanPriceWithZSpread(
                TRADE_STANDARD, RATES_PROVIDER, p, REAL_CLEAN_PRICE, Z_SPREAD, CONTINUOUS, 0)));
    assertTrue(computed.equalWithTolerance(expected, NOTIONAL * QUANTITY * EPS));
  }

  public void test_presentValueSensitivityFromRealCleanPriceWithZSpread_early_exCoupon() {
    PointSensitivities point = PRICER.presentValueSensitivityFromRealCleanPriceWithZSpread(TRADE_EX_COUPON_EARLY,
        RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR).build();
    CurveCurrencyParameterSensitivities computed = ISSUER_RATES_PROVIDER.curveParameterSensitivity(point)
        .combinedWith(RATES_PROVIDER.curveParameterSensitivity(point));
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(RATES_PROVIDER,
        p -> PRICER.presentValueFromCleanPriceWithZSpread(
            TRADE_EX_COUPON_EARLY, p, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR))
        .combinedWith(FD_CAL.sensitivity(ISSUER_RATES_PROVIDER,
            p -> PRICER.presentValueFromCleanPriceWithZSpread(
                TRADE_EX_COUPON_EARLY, RATES_PROVIDER, p, REAL_CLEAN_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR)));
    assertTrue(computed.equalWithTolerance(expected, NOTIONAL * QUANTITY * EPS));
  }

  public void test_presentValueSensitivityFromRealCleanPriceWithZSpread_early() {
    PointSensitivities point = PRICER.presentValueSensitivityFromRealCleanPriceWithZSpread(TRADE_EARLY,
        RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR).build();
    CurveCurrencyParameterSensitivities computed = ISSUER_RATES_PROVIDER.curveParameterSensitivity(point)
        .combinedWith(RATES_PROVIDER.curveParameterSensitivity(point));
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(RATES_PROVIDER,
        p -> PRICER.presentValueFromCleanPriceWithZSpread(
            TRADE_EARLY, p, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR))
        .combinedWith(FD_CAL.sensitivity(ISSUER_RATES_PROVIDER,
            p -> PRICER.presentValueFromCleanPriceWithZSpread(
                TRADE_EARLY, RATES_PROVIDER, p, REAL_CLEAN_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR)));
    assertTrue(computed.equalWithTolerance(expected, NOTIONAL * QUANTITY * EPS));
  }

  public void test_presentValueSensitivityFromRealCleanPriceWithZSpread_late() {
    PointSensitivities point = PRICER.presentValueSensitivityFromRealCleanPriceWithZSpread(TRADE_LATE,
        RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR).build();
    CurveCurrencyParameterSensitivities computed = ISSUER_RATES_PROVIDER.curveParameterSensitivity(point)
        .combinedWith(RATES_PROVIDER.curveParameterSensitivity(point));
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(RATES_PROVIDER,
        p -> PRICER.presentValueFromCleanPriceWithZSpread(
            TRADE_LATE, p, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR))
        .combinedWith(FD_CAL.sensitivity(ISSUER_RATES_PROVIDER,
            p -> PRICER.presentValueFromCleanPriceWithZSpread(
                TRADE_LATE, RATES_PROVIDER, p, REAL_CLEAN_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR)));
    assertTrue(computed.equalWithTolerance(expected, NOTIONAL * QUANTITY * EPS));
  }

  //-------------------------------------------------------------------------
  public void test_presentValue_standard() {
    CurrencyAmount computed =
        PRICER.presentValue(TRADE_STANDARD, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE);
    CurrencyAmount expected = PRICER.presentValueFromCleanPrice(TRADE_STANDARD, RATES_PROVIDER, ISSUER_RATES_PROVIDER,
        REAL_CLEAN_PRICE).plus(PRODUCT_PRICER.presentValue(
        PRODUCT, RATES_PROVIDER, ISSUER_RATES_PROVIDER, SETTLEMENT_STANDARD).multipliedBy(QUANTITY));
    assertEquals(computed.getAmount(), expected.getAmount(), NOTIONAL * QUANTITY * TOL);
  }

  public void test_presentValue_late() {
    CurrencyAmount computed =
        PRICER.presentValue(TRADE_LATE, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE);
    CurrencyAmount expected = PRICER.presentValueFromCleanPrice(TRADE_LATE, RATES_PROVIDER, ISSUER_RATES_PROVIDER,
        REAL_CLEAN_PRICE).plus(PRODUCT_PRICER.presentValue(
        PRODUCT, RATES_PROVIDER, ISSUER_RATES_PROVIDER, SETTLEMENT_LATE).multipliedBy(QUANTITY));
    assertEquals(computed.getAmount(), expected.getAmount(), NOTIONAL * QUANTITY * TOL);
  }

  public void test_presentValueWithZSpread_standard() {
    CurrencyAmount computed = PRICER.presentValueWithZSpread(
        TRADE_STANDARD, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, CONTINUOUS, 0);
    CurrencyAmount expected = PRICER.presentValueFromCleanPriceWithZSpread(TRADE_STANDARD, RATES_PROVIDER,
        ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, CONTINUOUS, 0).plus(
        PRODUCT_PRICER.presentValueWithZSpread(PRODUCT, RATES_PROVIDER, ISSUER_RATES_PROVIDER, SETTLEMENT_STANDARD,
            Z_SPREAD, CONTINUOUS, 0).multipliedBy(QUANTITY));
    assertEquals(computed.getAmount(), expected.getAmount(), NOTIONAL * QUANTITY * TOL);
  }

  public void test_presentValueWithZSpread_late() {
    CurrencyAmount computed = PRICER.presentValueWithZSpread(
        TRADE_LATE, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurrencyAmount expected = PRICER.presentValueFromCleanPriceWithZSpread(TRADE_LATE, RATES_PROVIDER,
        ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR).plus(
        PRODUCT_PRICER.presentValueWithZSpread(PRODUCT, RATES_PROVIDER, ISSUER_RATES_PROVIDER, SETTLEMENT_LATE,
            Z_SPREAD, PERIODIC, PERIOD_PER_YEAR).multipliedBy(QUANTITY));
    assertEquals(computed.getAmount(), expected.getAmount(), NOTIONAL * QUANTITY * TOL);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity_standard() {
    PointSensitivities point =
        PRICER.presentValueSensitivity(TRADE_STANDARD, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE).build();
    CurveCurrencyParameterSensitivities computed = ISSUER_RATES_PROVIDER.curveParameterSensitivity(point)
        .combinedWith(RATES_PROVIDER.curveParameterSensitivity(point));
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(RATES_PROVIDER,
        p -> PRICER.presentValue(TRADE_STANDARD, p, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE)).combinedWith(
        FD_CAL.sensitivity(ISSUER_RATES_PROVIDER,
            p -> PRICER.presentValue(TRADE_STANDARD, RATES_PROVIDER, p, REAL_CLEAN_PRICE)));
    assertTrue(computed.equalWithTolerance(expected, NOTIONAL * QUANTITY * EPS));
  }

  public void test_presentValueSensitivity_late() {
    PointSensitivities point =
        PRICER.presentValueSensitivity(TRADE_LATE, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE).build();
    CurveCurrencyParameterSensitivities computed = ISSUER_RATES_PROVIDER.curveParameterSensitivity(point)
        .combinedWith(RATES_PROVIDER.curveParameterSensitivity(point));
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(RATES_PROVIDER,
        p -> PRICER.presentValue(TRADE_LATE, p, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE)).combinedWith(
        FD_CAL.sensitivity(ISSUER_RATES_PROVIDER,
            p -> PRICER.presentValue(TRADE_LATE, RATES_PROVIDER, p, REAL_CLEAN_PRICE)));
    assertTrue(computed.equalWithTolerance(expected, NOTIONAL * QUANTITY * EPS));
  }

  public void test_presentValueSensitivityWithZSpread_standard() {
    PointSensitivities point = PRICER.presentValueSensitivityWithZSpread(
        TRADE_STANDARD, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR).build();
    CurveCurrencyParameterSensitivities computed = ISSUER_RATES_PROVIDER.curveParameterSensitivity(point)
        .combinedWith(RATES_PROVIDER.curveParameterSensitivity(point));
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(RATES_PROVIDER, p -> PRICER.presentValueWithZSpread(
        TRADE_STANDARD, p, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR)).combinedWith(
        FD_CAL.sensitivity(ISSUER_RATES_PROVIDER, p -> PRICER.presentValueWithZSpread(
            TRADE_STANDARD, RATES_PROVIDER, p, REAL_CLEAN_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR)));
    assertTrue(computed.equalWithTolerance(expected, NOTIONAL * QUANTITY * EPS));
  }

  public void test_presentValueSensitivityWithZSpread_late() {
    PointSensitivities point = PRICER.presentValueSensitivityWithZSpread(
        TRADE_LATE, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR).build();
    CurveCurrencyParameterSensitivities computed = ISSUER_RATES_PROVIDER.curveParameterSensitivity(point)
        .combinedWith(RATES_PROVIDER.curveParameterSensitivity(point));
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(RATES_PROVIDER, p -> PRICER.presentValueWithZSpread(
        TRADE_LATE, p, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR)).combinedWith(
        FD_CAL.sensitivity(ISSUER_RATES_PROVIDER, p -> PRICER.presentValueWithZSpread(
            TRADE_LATE, RATES_PROVIDER, p, REAL_CLEAN_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR)));
    assertTrue(computed.equalWithTolerance(expected, NOTIONAL * QUANTITY * EPS));
  }

  //-------------------------------------------------------------------------
  public void test_currencyExposure() {
    MultiCurrencyAmount computed =
        PRICER.currencyExposure(TRADE_STANDARD, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE);
    PointSensitivities point = PRICER.presentValueSensitivity(
        TRADE_STANDARD, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE).build();
    MultiCurrencyAmount expected = RATES_PROVIDER.currencyExposure(point).plus(
        PRICER.presentValue(TRADE_STANDARD, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE));
    assertEquals(computed.getAmounts().size(), 1);
    assertEquals(computed.getAmount(USD).getAmount(), expected.getAmount(USD).getAmount(), NOTIONAL * QUANTITY * TOL);
  }

  public void test_currencyExposureWithZSpread() {
    MultiCurrencyAmount computed = PRICER.currencyExposureWithZSpread(
        TRADE_STANDARD, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    PointSensitivities point = PRICER.presentValueSensitivityWithZSpread(
        TRADE_STANDARD, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR).build();
    MultiCurrencyAmount expected = RATES_PROVIDER.currencyExposure(point).plus(PRICER.presentValueWithZSpread(
        TRADE_STANDARD, RATES_PROVIDER, ISSUER_RATES_PROVIDER, REAL_CLEAN_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR));
    assertEquals(computed.getAmounts().size(), 1);
    assertEquals(computed.getAmount(USD).getAmount(), expected.getAmount(USD).getAmount(), NOTIONAL * QUANTITY * TOL);
  }

  public void test_currentCash() {
    CurrencyAmount computed = PRICER.currentCash(TRADE_SETTLED, RATES_PROVIDER_ON_PAY, REAL_CLEAN_PRICE);
    CurrencyAmount expected = PRODUCT_PRICER.currentCash(PRODUCT, RATES_PROVIDER_ON_PAY, SETTLEMENT_BEFORE);
    assertEquals(computed.getAmount(), expected.getAmount(), NOTIONAL * QUANTITY * TOL);
  }

  public void test_currentCash_early() {
    CurrencyAmount computed = PRICER.currentCash(TRADE_EARLY, RATES_PROVIDER, REAL_CLEAN_PRICE);
    CurrencyAmount expected = PRICER.netAmount(TRADE_EARLY, RATES_PROVIDER, REAL_CLEAN_PRICE);
    assertEquals(computed.getAmount(), expected.getAmount(), NOTIONAL * QUANTITY * TOL);
  }

  //-------------------------------------------------------------------------
  private static final double CLEAN_REAL_FROM_CURVES;
  private static final double CLEAN_REAL_FROM_CURVES_ZSPREAD;
  static {
    double dirtyNominal = PRODUCT_PRICER.dirtyNominalPriceFromCurves(SECURITY, RATES_PROVIDER, ISSUER_RATES_PROVIDER);
    double cleanNominal = PRODUCT_PRICER.cleanNominalPriceFromDirtyNominalPrice(
        PRODUCT, RATES_PROVIDER, SETTLEMENT_STANDARD, dirtyNominal);
    CLEAN_REAL_FROM_CURVES = PRODUCT_PRICER.realPriceFromNominalPrice(
        PRODUCT, RATES_PROVIDER, SETTLEMENT_STANDARD, cleanNominal);
    double dirtyNominalZSpread = PRODUCT_PRICER.dirtyNominalPriceFromCurvesWithZSpread(
        SECURITY, RATES_PROVIDER, ISSUER_RATES_PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    double cleanNominalZSpread = PRODUCT_PRICER.cleanNominalPriceFromDirtyNominalPrice(
        PRODUCT, RATES_PROVIDER, SETTLEMENT_STANDARD, dirtyNominalZSpread);
    CLEAN_REAL_FROM_CURVES_ZSPREAD = PRODUCT_PRICER.realPriceFromNominalPrice(
        PRODUCT, RATES_PROVIDER, SETTLEMENT_STANDARD, cleanNominalZSpread);
  }

  public void test_presentValue_coherency_standard() {
    CurrencyAmount pvFromCleanPrice = PRICER.presentValueFromCleanPrice(
        TRADE_STANDARD, RATES_PROVIDER, ISSUER_RATES_PROVIDER, CLEAN_REAL_FROM_CURVES).multipliedBy(-1d / QUANTITY);
    CurrencyAmount pvFromCurves = PRODUCT_PRICER.presentValue(
        PRODUCT, RATES_PROVIDER, ISSUER_RATES_PROVIDER, SETTLEMENT_STANDARD);
    assertEquals(pvFromCleanPrice.getAmount(), pvFromCurves.getAmount(), NOTIONAL * TOL);
  }

  public void test_presentValue_coherency_early() {
    CurrencyAmount pvFromCleanPrice = PRICER.presentValueFromCleanPrice(
        TRADE_EARLY, RATES_PROVIDER, ISSUER_RATES_PROVIDER, CLEAN_REAL_FROM_CURVES).multipliedBy(-1d / QUANTITY);
    CurrencyAmount pvFromCurves = PRODUCT_PRICER.presentValue(
        PRODUCT, RATES_PROVIDER, ISSUER_RATES_PROVIDER, SETTLEMENT_EARLY);
    assertEquals(pvFromCleanPrice.getAmount(), pvFromCurves.getAmount(), NOTIONAL * TOL);
  }

  public void test_presentValue_coherency_late() {
    CurrencyAmount pvFromCleanPrice = PRICER.presentValueFromCleanPrice(
        TRADE_LATE, RATES_PROVIDER, ISSUER_RATES_PROVIDER, CLEAN_REAL_FROM_CURVES).multipliedBy(-1d / QUANTITY);
    CurrencyAmount pvFromCurves = PRODUCT_PRICER.presentValue(
        PRODUCT, RATES_PROVIDER, ISSUER_RATES_PROVIDER, SETTLEMENT_LATE);
    assertEquals(pvFromCleanPrice.getAmount(), pvFromCurves.getAmount(), NOTIONAL * TOL);
  }

  public void test_presentValue_coherency_exCoupon() {
    CurrencyAmount pvFromCleanPrice = PRICER.presentValueFromCleanPrice(
        TRADE_EX_COUPON_EARLY, RATES_PROVIDER, ISSUER_RATES_PROVIDER, CLEAN_REAL_FROM_CURVES)
        .multipliedBy(-1d / QUANTITY);
    CurrencyAmount pvFromCurves = PRODUCT_PRICER.presentValue(
        PRODUCT_EX_COUPON, RATES_PROVIDER, ISSUER_RATES_PROVIDER, SETTLEMENT_EARLY);
    assertEquals(pvFromCleanPrice.getAmount(), pvFromCurves.getAmount(), NOTIONAL * TOL);
  }

  public void test_presentValueWithZSpread_coherency_standard() {
    CurrencyAmount pvFromCleanPrice = PRICER.presentValueFromCleanPriceWithZSpread(TRADE_STANDARD, RATES_PROVIDER,
        ISSUER_RATES_PROVIDER, CLEAN_REAL_FROM_CURVES_ZSPREAD, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR)
        .multipliedBy(-1d / QUANTITY);
    CurrencyAmount pvFromCurves = PRODUCT_PRICER.presentValueWithZSpread(
        PRODUCT, RATES_PROVIDER, ISSUER_RATES_PROVIDER, SETTLEMENT_STANDARD, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertEquals(pvFromCleanPrice.getAmount(), pvFromCurves.getAmount(), NOTIONAL * TOL);
  }

  public void test_presentValueWithZSpread_coherency_early() {
    CurrencyAmount pvFromCleanPrice = PRICER.presentValueFromCleanPriceWithZSpread(TRADE_EARLY, RATES_PROVIDER,
        ISSUER_RATES_PROVIDER, CLEAN_REAL_FROM_CURVES_ZSPREAD, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR)
        .multipliedBy(-1d / QUANTITY);
    CurrencyAmount pvFromCurves = PRODUCT_PRICER.presentValueWithZSpread(
        PRODUCT, RATES_PROVIDER, ISSUER_RATES_PROVIDER, SETTLEMENT_EARLY, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertEquals(pvFromCleanPrice.getAmount(), pvFromCurves.getAmount(), NOTIONAL * TOL);
  }

  public void test_presentValueWithZSpread_coherency_late() {
    CurrencyAmount pvFromCleanPrice = PRICER.presentValueFromCleanPriceWithZSpread(TRADE_LATE, RATES_PROVIDER,
        ISSUER_RATES_PROVIDER, CLEAN_REAL_FROM_CURVES_ZSPREAD, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR)
        .multipliedBy(-1d / QUANTITY);
    CurrencyAmount pvFromCurves = PRODUCT_PRICER.presentValueWithZSpread(
        PRODUCT, RATES_PROVIDER, ISSUER_RATES_PROVIDER, SETTLEMENT_LATE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertEquals(pvFromCleanPrice.getAmount(), pvFromCurves.getAmount(), NOTIONAL * TOL);
  }

  public void test_presentValueWithZSpread_coherency_exCoupon() {
    CurrencyAmount pvFromCleanPrice = PRICER.presentValueFromCleanPriceWithZSpread(TRADE_EX_COUPON_EARLY,
        RATES_PROVIDER, ISSUER_RATES_PROVIDER, CLEAN_REAL_FROM_CURVES_ZSPREAD, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR)
        .multipliedBy(-1d / QUANTITY);
    CurrencyAmount pvFromCurves = PRODUCT_PRICER.presentValueWithZSpread(
        PRODUCT_EX_COUPON, RATES_PROVIDER, ISSUER_RATES_PROVIDER, SETTLEMENT_EARLY, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertEquals(pvFromCleanPrice.getAmount(), pvFromCurves.getAmount(), NOTIONAL * TOL);
  }

}
