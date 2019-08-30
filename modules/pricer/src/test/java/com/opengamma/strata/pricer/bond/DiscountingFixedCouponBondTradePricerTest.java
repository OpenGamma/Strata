/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.pricer.CompoundedRateType.CONTINUOUS;
import static com.opengamma.strata.pricer.CompoundedRateType.PERIODIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.LegalEntityGroup;
import com.opengamma.strata.market.curve.RepoGroup;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.bond.FixedCouponBond;
import com.opengamma.strata.product.bond.FixedCouponBondPaymentPeriod;
import com.opengamma.strata.product.bond.FixedCouponBondYieldConvention;
import com.opengamma.strata.product.bond.ResolvedFixedCouponBond;
import com.opengamma.strata.product.bond.ResolvedFixedCouponBondSettlement;
import com.opengamma.strata.product.bond.ResolvedFixedCouponBondTrade;

/**
 * Test {@link DiscountingFixedCouponBondTradePricer}.
 */
public class DiscountingFixedCouponBondTradePricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  // dates
  private static final LocalDate SETTLEMENT = date(2016, 4, 29); // after coupon date
  private static final LocalDate VAL_DATE = date(2016, 4, 25);
  private static final LocalDate TRADE_BEFORE = date(2016, 3, 18);
  private static final LocalDate SETTLE_BEFORE = date(2016, 3, 22); // before coupon date
  private static final LocalDate SETTLE_ON_COUPON = date(2016, 4, 12); // coupon date
  private static final LocalDate SETTLE_BTWN_DETACHMENT_COUPON = date(2016, 4, 8); // between detachment date and coupon date
  private static final LocalDate SETTLE_ON_DETACHMENT = date(2016, 4, 7); // detachment date

  // pricers
  private static final double TOL = 1.0e-12;
  private static final double EPS = 1.0e-6;
  private static final DiscountingFixedCouponBondTradePricer TRADE_PRICER = DiscountingFixedCouponBondTradePricer.DEFAULT;
  // when refactoring, existing tests needed a pricer that has zero upfront payment,
  // and where the settlement date is artificially set to always be SETTLEMENT
  private static final DiscountingFixedCouponBondTradePricer TRADE_PRICER_NO_UPFRONT =
      new DiscountingFixedCouponBondTradePricer(
          DiscountingFixedCouponBondProductPricer.DEFAULT,
          DiscountingPaymentPricer.DEFAULT) {
        @Override
        public Payment upfrontPayment(ResolvedFixedCouponBondTrade trade) {
          return Payment.of(CurrencyAmount.zero(trade.getProduct().getCurrency()), SETTLEMENT);
        }
      };
  private static final DiscountingFixedCouponBondProductPricer PRODUCT_PRICER =
      DiscountingFixedCouponBondProductPricer.DEFAULT;
  private static final DiscountingPaymentPricer PRICER_NOMINAL = DiscountingPaymentPricer.DEFAULT;
  private static final DiscountingFixedCouponBondPaymentPeriodPricer COUPON_PRICER =
      DiscountingFixedCouponBondPaymentPeriodPricer.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL = new RatesFiniteDifferenceSensitivityCalculator(EPS);

  // fixed coupon bond
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Ticker", "GOVT1-BOND1");
  private static final LegalEntityId ISSUER_ID = LegalEntityId.of("OG-Ticker", "GOVT1");
  private static final long QUANTITY = 15L;
  private static final FixedCouponBondYieldConvention YIELD_CONVENTION = FixedCouponBondYieldConvention.DE_BONDS;
  private static final double NOTIONAL = 1.0e7;
  private static final double FIXED_RATE = 0.015;
  private static final HolidayCalendarId EUR_CALENDAR = HolidayCalendarIds.EUTA;
  private static final DaysAdjustment DATE_OFFSET = DaysAdjustment.ofBusinessDays(3, EUR_CALENDAR);
  private static final DayCount DAY_COUNT = DayCounts.ACT_365F;
  private static final LocalDate START_DATE = LocalDate.of(2015, 4, 12);
  private static final LocalDate END_DATE = LocalDate.of(2025, 4, 12);
  private static final BusinessDayAdjustment BUSINESS_ADJUST =
      BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, EUR_CALENDAR);
  private static final PeriodicSchedule PERIOD_SCHEDULE = PeriodicSchedule.of(
      START_DATE, END_DATE, Frequency.P6M, BUSINESS_ADJUST, StubConvention.SHORT_INITIAL, false);
  private static final DaysAdjustment EX_COUPON = DaysAdjustment.ofCalendarDays(-5, BUSINESS_ADJUST);
  private static final ResolvedFixedCouponBond PRODUCT = FixedCouponBond.builder()
      .securityId(SECURITY_ID)
      .dayCount(DAY_COUNT)
      .fixedRate(FIXED_RATE)
      .legalEntityId(ISSUER_ID)
      .currency(EUR)
      .notional(NOTIONAL)
      .accrualSchedule(PERIOD_SCHEDULE)
      .settlementDateOffset(DATE_OFFSET)
      .yieldConvention(YIELD_CONVENTION)
      .exCouponPeriod(EX_COUPON)
      .build()
      .resolve(REF_DATA);
  private static final double CLEAN_PRICE = 0.98;
  private static final double DIRTY_PRICE = PRODUCT_PRICER.dirtyPriceFromCleanPrice(PRODUCT, SETTLEMENT, CLEAN_PRICE);
  private static final Payment UPFRONT_PAYMENT = Payment.of(
      CurrencyAmount.of(EUR, -QUANTITY * NOTIONAL * DIRTY_PRICE), SETTLEMENT);

  /** nonzero ex-coupon period */
  private static final ResolvedFixedCouponBondTrade TRADE = ResolvedFixedCouponBondTrade.builder()
      .product(PRODUCT)
      .quantity(QUANTITY)
      .settlement(ResolvedFixedCouponBondSettlement.of(SETTLEMENT, CLEAN_PRICE))
      .build();
  private static final ResolvedFixedCouponBond PRODUCT_NO_EXCOUPON = FixedCouponBond.builder()
      .securityId(SECURITY_ID)
      .dayCount(DAY_COUNT)
      .fixedRate(FIXED_RATE)
      .legalEntityId(ISSUER_ID)
      .currency(EUR)
      .notional(NOTIONAL)
      .accrualSchedule(PERIOD_SCHEDULE)
      .settlementDateOffset(DATE_OFFSET)
      .yieldConvention(YIELD_CONVENTION)
      .build()
      .resolve(REF_DATA);
  /** no ex-coupon period */
  private static final ResolvedFixedCouponBondTrade TRADE_NO_EXCOUPON = ResolvedFixedCouponBondTrade.builder()
      .product(PRODUCT_NO_EXCOUPON)
      .quantity(QUANTITY)
      .settlement(ResolvedFixedCouponBondSettlement.of(SETTLEMENT, CLEAN_PRICE))
      .build();
  private static final ResolvedFixedCouponBondTrade POSITION = ResolvedFixedCouponBondTrade.builder()
      .product(PRODUCT)
      .quantity(QUANTITY)
      .build();

  // rates provider
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveName NAME_REPO = CurveName.of("TestRepoCurve");
  private static final CurveMetadata METADATA_REPO = Curves.zeroRates(NAME_REPO, ACT_365F);
  private static final InterpolatedNodalCurve CURVE_REPO = InterpolatedNodalCurve.of(
      METADATA_REPO, DoubleArray.of(0.1, 2.0, 10.0), DoubleArray.of(0.05, 0.06, 0.09), INTERPOLATOR);
  private static final RepoGroup GROUP_REPO = RepoGroup.of("GOVT1 BOND1");
  private static final CurveName NAME_ISSUER = CurveName.of("TestIssuerCurve");
  private static final CurveMetadata METADATA_ISSUER = Curves.zeroRates(NAME_ISSUER, ACT_365F);
  private static final InterpolatedNodalCurve CURVE_ISSUER = InterpolatedNodalCurve.of(
      METADATA_ISSUER, DoubleArray.of(0.2, 9.0, 15.0), DoubleArray.of(0.03, 0.05, 0.13), INTERPOLATOR);
  private static final LegalEntityGroup GROUP_ISSUER = LegalEntityGroup.of("GOVT1");
  private static final LegalEntityDiscountingProvider PROVIDER = createRatesProvider(VAL_DATE);
  private static final LegalEntityDiscountingProvider PROVIDER_BEFORE = createRatesProvider(TRADE_BEFORE);

  private static final double Z_SPREAD = 0.035;
  private static final int PERIOD_PER_YEAR = 4;

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValue() {
    CurrencyAmount computedTrade = TRADE_PRICER.presentValue(TRADE, PROVIDER);
    CurrencyAmount computedProduct = PRODUCT_PRICER.presentValue(PRODUCT, PROVIDER, SETTLEMENT);
    CurrencyAmount pvPayment =
        PRICER_NOMINAL.presentValue(UPFRONT_PAYMENT, ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_REPO));
    assertThat(computedTrade.getAmount()).isCloseTo(computedProduct.multipliedBy(QUANTITY).plus(pvPayment).getAmount(), offset(NOTIONAL * QUANTITY * TOL));
  }

  @Test
  public void test_presentValueWithZSpread_continuous() {
    CurrencyAmount computedTrade = TRADE_PRICER.presentValueWithZSpread(
        TRADE, PROVIDER, Z_SPREAD, CONTINUOUS, 0);
    CurrencyAmount computedProduct =
        PRODUCT_PRICER.presentValueWithZSpread(PRODUCT, PROVIDER, Z_SPREAD, CONTINUOUS, 0, SETTLEMENT);
    CurrencyAmount pvPayment =
        PRICER_NOMINAL.presentValue(UPFRONT_PAYMENT, ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_REPO));
    assertThat(computedTrade.getAmount()).isCloseTo(computedProduct.multipliedBy(QUANTITY).plus(pvPayment).getAmount(), offset(NOTIONAL * QUANTITY * TOL));
  }

  @Test
  public void test_presentValueWithZSpread_periodic() {
    CurrencyAmount computedTrade =
        TRADE_PRICER.presentValueWithZSpread(TRADE, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurrencyAmount computedProduct = PRODUCT_PRICER.presentValueWithZSpread(
        PRODUCT, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR, SETTLEMENT);
    CurrencyAmount pvPayment =
        PRICER_NOMINAL.presentValue(UPFRONT_PAYMENT, ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_REPO));
    assertThat(computedTrade.getAmount()).isCloseTo(computedProduct.multipliedBy(QUANTITY).plus(pvPayment).getAmount(), offset(NOTIONAL * QUANTITY * TOL));
  }

  @Test
  public void test_presentValue_noExcoupon() {
    CurrencyAmount computedTrade = TRADE_PRICER.presentValue(TRADE_NO_EXCOUPON, PROVIDER);
    CurrencyAmount computedProduct = PRODUCT_PRICER.presentValue(PRODUCT_NO_EXCOUPON, PROVIDER, SETTLEMENT);
    CurrencyAmount pvPayment =
        PRICER_NOMINAL.presentValue(UPFRONT_PAYMENT, ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_REPO));
    assertThat(computedTrade.getAmount()).isCloseTo(computedProduct.multipliedBy(QUANTITY).plus(pvPayment).getAmount(), offset(NOTIONAL * QUANTITY * TOL));
  }

  @Test
  public void test_presentValueWithZSpread_continuous_noExcoupon() {
    CurrencyAmount computedTrade =
        TRADE_PRICER.presentValueWithZSpread(TRADE_NO_EXCOUPON, PROVIDER, Z_SPREAD, CONTINUOUS, 0);
    CurrencyAmount computedProduct = PRODUCT_PRICER.presentValueWithZSpread(
        PRODUCT_NO_EXCOUPON, PROVIDER, Z_SPREAD, CONTINUOUS, 0, SETTLEMENT);
    CurrencyAmount pvPayment =
        PRICER_NOMINAL.presentValue(UPFRONT_PAYMENT, ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_REPO));
    assertThat(computedTrade.getAmount()).isCloseTo(computedProduct.multipliedBy(QUANTITY).plus(pvPayment).getAmount(), offset(NOTIONAL * QUANTITY * TOL));
  }

  @Test
  public void test_presentValueWithZSpread_periodic_noExcoupon() {
    CurrencyAmount computedTrade = TRADE_PRICER.presentValueWithZSpread(
        TRADE_NO_EXCOUPON, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurrencyAmount computedProduct = PRODUCT_PRICER.presentValueWithZSpread(
        PRODUCT_NO_EXCOUPON, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR, SETTLEMENT);
    CurrencyAmount pvPayment =
        PRICER_NOMINAL.presentValue(UPFRONT_PAYMENT, ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_REPO));
    assertThat(computedTrade.getAmount()).isCloseTo(computedProduct.multipliedBy(QUANTITY).plus(pvPayment).getAmount(), offset(NOTIONAL * QUANTITY * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValue_dateLogic() {
    ResolvedFixedCouponBondTrade tradeAfter = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLEMENT, CLEAN_PRICE))
        .build();
    CurrencyAmount computedTradeAfter = TRADE_PRICER_NO_UPFRONT.presentValue(tradeAfter, PROVIDER_BEFORE);
    // settle before detachment date
    ResolvedFixedCouponBondTrade tradeBefore = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_BEFORE, CLEAN_PRICE))
        .build();
    CurrencyAmount computedTradeBefore = TRADE_PRICER_NO_UPFRONT.presentValue(tradeBefore, PROVIDER_BEFORE);
    FixedCouponBondPaymentPeriod periodExtra = findPeriod(PRODUCT, SETTLE_BEFORE, SETTLEMENT);
    double pvExtra = COUPON_PRICER.presentValue(periodExtra, PROVIDER_BEFORE.issuerCurveDiscountFactors(ISSUER_ID, EUR));
    assertThat(computedTradeBefore.getAmount()).isCloseTo(computedTradeAfter.plus(pvExtra * QUANTITY).getAmount(), offset(NOTIONAL * QUANTITY * TOL));
    // settle on detachment date
    ResolvedFixedCouponBondTrade tradeOnDetachment = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_ON_DETACHMENT, CLEAN_PRICE))
        .build();
    CurrencyAmount computedTradeOnDetachment = TRADE_PRICER_NO_UPFRONT.presentValue(tradeOnDetachment, PROVIDER_BEFORE);
    assertThat(computedTradeOnDetachment.getAmount()).isCloseTo(computedTradeAfter.getAmount(), offset(NOTIONAL * QUANTITY * TOL));
    // settle between detachment date and coupon date
    ResolvedFixedCouponBondTrade tradeBtwnDetachmentCoupon = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_BTWN_DETACHMENT_COUPON, CLEAN_PRICE))
        .build();
    CurrencyAmount computedTradeBtwnDetachmentCoupon =
        TRADE_PRICER_NO_UPFRONT.presentValue(tradeBtwnDetachmentCoupon, PROVIDER_BEFORE);
    assertThat(computedTradeBtwnDetachmentCoupon.getAmount()).isCloseTo(computedTradeAfter.getAmount(), offset(NOTIONAL * QUANTITY * TOL));
  }

  @Test
  public void test_presentValue_dateLogic_pastSettle() {
    ResolvedFixedCouponBondTrade tradeAfter = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLEMENT, CLEAN_PRICE))
        .build();
    CurrencyAmount computedTradeAfter = TRADE_PRICER_NO_UPFRONT.presentValue(tradeAfter, PROVIDER);
    // settle before detachment date
    ResolvedFixedCouponBondTrade tradeBefore = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_BEFORE, CLEAN_PRICE))
        .build();
    CurrencyAmount computedTradeBefore = TRADE_PRICER_NO_UPFRONT.presentValue(tradeBefore, PROVIDER);
    assertThat(computedTradeBefore.getAmount()).isCloseTo(computedTradeAfter.getAmount(), offset(NOTIONAL * QUANTITY * TOL));
    // settle on detachment date
    ResolvedFixedCouponBondTrade tradeOnDetachment = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_ON_DETACHMENT, CLEAN_PRICE))
        .build();
    CurrencyAmount computedTradeOnDetachment = TRADE_PRICER_NO_UPFRONT.presentValue(tradeOnDetachment, PROVIDER);
    assertThat(computedTradeOnDetachment.getAmount()).isCloseTo(computedTradeAfter.getAmount(), offset(NOTIONAL * QUANTITY * TOL));
    // settle between detachment date and coupon date
    ResolvedFixedCouponBondTrade tradeBtwnDetachmentCoupon = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_BTWN_DETACHMENT_COUPON, CLEAN_PRICE))
        .build();
    CurrencyAmount computedTradeBtwnDetachmentCoupon =
        TRADE_PRICER_NO_UPFRONT.presentValue(tradeBtwnDetachmentCoupon, PROVIDER);
    assertThat(computedTradeBtwnDetachmentCoupon.getAmount()).isCloseTo(computedTradeAfter.getAmount(), offset(NOTIONAL * QUANTITY * TOL));
  }

  @Test
  public void test_presentValue_dateLogic_noExcoupon() {
    ResolvedFixedCouponBondTrade tradeAfter = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT_NO_EXCOUPON)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLEMENT, CLEAN_PRICE))
        .build();
    CurrencyAmount computedTradeAfter = TRADE_PRICER_NO_UPFRONT.presentValue(tradeAfter, PROVIDER_BEFORE);
    // settle before coupon date
    ResolvedFixedCouponBondTrade tradeBefore = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT_NO_EXCOUPON)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_BEFORE, CLEAN_PRICE))
        .build();
    CurrencyAmount computedTradeBefore = TRADE_PRICER_NO_UPFRONT.presentValue(tradeBefore, PROVIDER_BEFORE);
    FixedCouponBondPaymentPeriod periodExtra = findPeriod(PRODUCT_NO_EXCOUPON, SETTLE_BEFORE, SETTLEMENT);
    double pvExtra = COUPON_PRICER.presentValue(periodExtra, PROVIDER_BEFORE.issuerCurveDiscountFactors(ISSUER_ID, EUR));
    assertThat(computedTradeBefore.getAmount()).isCloseTo(computedTradeAfter.plus(pvExtra * QUANTITY).getAmount(), offset(NOTIONAL * QUANTITY * TOL));
    // settle on coupon date
    ResolvedFixedCouponBondTrade tradeOnCoupon = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT_NO_EXCOUPON)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_ON_COUPON, CLEAN_PRICE))
        .build();
    CurrencyAmount computedTradeOnCoupon = TRADE_PRICER_NO_UPFRONT.presentValue(tradeOnCoupon, PROVIDER_BEFORE);
    assertThat(computedTradeOnCoupon.getAmount()).isCloseTo(computedTradeAfter.getAmount(), offset(NOTIONAL * QUANTITY * TOL));
  }

  @Test
  public void test_presentValue_dateLogic_pastSettle_noExcoupon() {
    ResolvedFixedCouponBondTrade tradeAfter = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT_NO_EXCOUPON)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLEMENT, CLEAN_PRICE))
        .build();
    CurrencyAmount computedTradeAfter = TRADE_PRICER_NO_UPFRONT.presentValue(tradeAfter, PROVIDER);
    // settle before coupon date
    ResolvedFixedCouponBondTrade tradeBefore = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT_NO_EXCOUPON)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_BEFORE, CLEAN_PRICE))
        .build();
    CurrencyAmount computedTradeBefore = TRADE_PRICER_NO_UPFRONT.presentValue(tradeBefore, PROVIDER);
    assertThat(computedTradeBefore.getAmount()).isCloseTo(computedTradeAfter.getAmount(), offset(NOTIONAL * QUANTITY * TOL));
    // settle on coupon date
    ResolvedFixedCouponBondTrade tradeOnCoupon = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT_NO_EXCOUPON)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_ON_COUPON, CLEAN_PRICE))
        .build();
    CurrencyAmount computedTradeOnCoupon = TRADE_PRICER_NO_UPFRONT.presentValue(tradeOnCoupon, PROVIDER);
    assertThat(computedTradeOnCoupon.getAmount()).isCloseTo(computedTradeAfter.getAmount(), offset(NOTIONAL * QUANTITY * TOL));
  }

  @Test
  public void test_presentValue_position() {
    CurrencyAmount computedTrade = TRADE_PRICER.presentValue(POSITION, PROVIDER);
    CurrencyAmount computedProduct = PRODUCT_PRICER.presentValue(PRODUCT, PROVIDER, VAL_DATE);
    assertThat(computedTrade.getAmount()).isCloseTo(computedProduct.multipliedBy(QUANTITY).getAmount(), offset(NOTIONAL * QUANTITY * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueFromCleanPrice() {
    double cleanPrice = 0.985;
    CurrencyAmount computed = TRADE_PRICER.presentValueFromCleanPrice(TRADE, PROVIDER, REF_DATA, cleanPrice);
    LocalDate standardSettlement = PRODUCT.getSettlementDateOffset().adjust(VAL_DATE, REF_DATA);
    double df = ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_REPO).discountFactor(standardSettlement);
    double accruedInterest = PRODUCT_PRICER.accruedInterest(PRODUCT, standardSettlement);
    double pvPayment = PRICER_NOMINAL
        .presentValue(UPFRONT_PAYMENT, ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_REPO)).getAmount();
    double expected = QUANTITY * (cleanPrice * df * NOTIONAL + accruedInterest * df) + pvPayment;
    assertThat(computed.getCurrency()).isEqualTo(EUR);
    assertThat(computed.getAmount()).isCloseTo(expected, offset(NOTIONAL * QUANTITY * TOL));
  }

  @Test
  public void test_presentValueFromCleanPriceWithZSpread_continuous() {
    double cleanPrice = 0.985;
    CurrencyAmount computed = TRADE_PRICER.presentValueFromCleanPriceWithZSpread(
        TRADE, PROVIDER, REF_DATA, cleanPrice, Z_SPREAD, CONTINUOUS, 0);
    LocalDate standardSettlement = PRODUCT.getSettlementDateOffset().adjust(VAL_DATE, REF_DATA);
    double df = ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_REPO).discountFactor(standardSettlement);
    double accruedInterest = PRODUCT_PRICER.accruedInterest(PRODUCT, standardSettlement);
    double pvPayment = PRICER_NOMINAL
        .presentValue(UPFRONT_PAYMENT, ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_REPO)).getAmount();
    double expected = QUANTITY * (cleanPrice * df * NOTIONAL + accruedInterest * df) + pvPayment;
    assertThat(computed.getCurrency()).isEqualTo(EUR);
    assertThat(computed.getAmount()).isCloseTo(expected, offset(NOTIONAL * QUANTITY * TOL));
  }

  @Test
  public void test_presentValueFromCleanPriceWithZSpread_periodic() {
    double cleanPrice = 0.985;
    CurrencyAmount computed = TRADE_PRICER.presentValueFromCleanPriceWithZSpread(
        TRADE, PROVIDER, REF_DATA, cleanPrice, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    LocalDate standardSettlement = PRODUCT.getSettlementDateOffset().adjust(VAL_DATE, REF_DATA);
    double df = ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_REPO).discountFactor(standardSettlement);
    double accruedInterest = PRODUCT_PRICER.accruedInterest(PRODUCT, standardSettlement);
    double pvPayment = PRICER_NOMINAL
        .presentValue(UPFRONT_PAYMENT, ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_REPO)).getAmount();
    double expected = QUANTITY * (cleanPrice * df * NOTIONAL + accruedInterest * df) + pvPayment;
    assertThat(computed.getCurrency()).isEqualTo(EUR);
    assertThat(computed.getAmount()).isCloseTo(expected, offset(NOTIONAL * QUANTITY * TOL));
  }

  @Test
  public void test_presentValueFromCleanPrice_noExcoupon() {
    double cleanPrice = 0.985;
    CurrencyAmount computed = TRADE_PRICER.presentValueFromCleanPrice(TRADE_NO_EXCOUPON, PROVIDER, REF_DATA, cleanPrice);
    LocalDate standardSettlement = PRODUCT_NO_EXCOUPON.getSettlementDateOffset().adjust(VAL_DATE, REF_DATA);
    double df = ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_REPO).discountFactor(standardSettlement);
    double accruedInterest = PRODUCT_PRICER.accruedInterest(PRODUCT_NO_EXCOUPON, standardSettlement);
    double pvPayment = PRICER_NOMINAL
        .presentValue(UPFRONT_PAYMENT, ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_REPO)).getAmount();
    double expected = QUANTITY * (cleanPrice * df * NOTIONAL + accruedInterest * df) + pvPayment;
    assertThat(computed.getCurrency()).isEqualTo(EUR);
    assertThat(computed.getAmount()).isCloseTo(expected, offset(NOTIONAL * QUANTITY * TOL));
  }

  @Test
  public void test_presentValueFromCleanPriceWithZSpread_continuous_noExcoupon() {
    double cleanPrice = 0.985;
    CurrencyAmount computed = TRADE_PRICER.presentValueFromCleanPriceWithZSpread(
        TRADE_NO_EXCOUPON, PROVIDER, REF_DATA, cleanPrice, Z_SPREAD, CONTINUOUS, 0);
    LocalDate standardSettlement = PRODUCT_NO_EXCOUPON.getSettlementDateOffset().adjust(VAL_DATE, REF_DATA);
    double df = ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_REPO).discountFactor(standardSettlement);
    double accruedInterest = PRODUCT_PRICER.accruedInterest(PRODUCT_NO_EXCOUPON, standardSettlement);
    double pvPayment = PRICER_NOMINAL
        .presentValue(UPFRONT_PAYMENT, ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_REPO)).getAmount();
    double expected = QUANTITY * (cleanPrice * df * NOTIONAL + accruedInterest * df) + pvPayment;
    assertThat(computed.getCurrency()).isEqualTo(EUR);
    assertThat(computed.getAmount()).isCloseTo(expected, offset(NOTIONAL * QUANTITY * TOL));
  }

  @Test
  public void test_presentValueFromCleanPriceWithZSpread_periodic_noExcoupon() {
    double cleanPrice = 0.985;
    CurrencyAmount computed = TRADE_PRICER.presentValueFromCleanPriceWithZSpread(
        TRADE_NO_EXCOUPON, PROVIDER, REF_DATA, cleanPrice, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    LocalDate standardSettlement = PRODUCT_NO_EXCOUPON.getSettlementDateOffset().adjust(VAL_DATE, REF_DATA);
    double df = ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_REPO).discountFactor(standardSettlement);
    double accruedInterest = PRODUCT_PRICER.accruedInterest(PRODUCT_NO_EXCOUPON, standardSettlement);
    double pvPayment = PRICER_NOMINAL
        .presentValue(UPFRONT_PAYMENT, ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_REPO)).getAmount();
    double expected = QUANTITY * (cleanPrice * df * NOTIONAL + accruedInterest * df) + pvPayment;
    assertThat(computed.getCurrency()).isEqualTo(EUR);
    assertThat(computed.getAmount()).isCloseTo(expected, offset(NOTIONAL * QUANTITY * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueFromCleanPrice_dateLogic() {
    double cleanPrice = 0.985;
    FixedCouponBondPaymentPeriod periodExtra = findPeriod(PRODUCT, SETTLE_BEFORE, SETTLEMENT);
    // trade settlement < detachment date < standard settlement (tradeDate = valuation1)
    LocalDate valuation1 = SETTLE_ON_DETACHMENT.minusDays(1);
    ResolvedFixedCouponBondTrade trade1 = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(valuation1, CLEAN_PRICE))
        .build();
    LegalEntityDiscountingProvider provider1 = createRatesProvider(valuation1);
    LocalDate standardSettlement1 = PRODUCT.getSettlementDateOffset().adjust(valuation1, REF_DATA);
    double df1 = ZeroRateDiscountFactors.of(EUR, valuation1, CURVE_REPO).discountFactor(standardSettlement1);
    double accruedInterest1 = PRODUCT_PRICER.accruedInterest(PRODUCT, standardSettlement1);
    double basePv1 = cleanPrice * df1 * NOTIONAL + accruedInterest1 * df1;
    double pvExtra1 = COUPON_PRICER.presentValue(periodExtra, provider1.issuerCurveDiscountFactors(ISSUER_ID, EUR));
    double pvExtra1Continuous = COUPON_PRICER.presentValueWithSpread(
        periodExtra, provider1.issuerCurveDiscountFactors(ISSUER_ID, EUR), Z_SPREAD, CONTINUOUS, 0);
    double pvExtra1Periodic = COUPON_PRICER.presentValueWithSpread(
        periodExtra, provider1.issuerCurveDiscountFactors(ISSUER_ID, EUR), Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurrencyAmount computed1 = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPrice(trade1, provider1, REF_DATA, cleanPrice);
    CurrencyAmount computed1Continuous = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade1, provider1, REF_DATA, cleanPrice, Z_SPREAD, CONTINUOUS, 0);
    CurrencyAmount computed1Periodic = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade1, provider1, REF_DATA, cleanPrice, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertThat(computed1.getAmount()).isCloseTo(QUANTITY * (basePv1 + pvExtra1), offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed1Continuous.getAmount()).isCloseTo(QUANTITY * (basePv1 + pvExtra1Continuous), offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed1Periodic.getAmount()).isCloseTo(QUANTITY * (basePv1 + pvExtra1Periodic), offset(NOTIONAL * QUANTITY * TOL));
    // detachment date < trade settlement < standard settlement (tradeDate = valuation1)
    ResolvedFixedCouponBondTrade trade2 = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_ON_DETACHMENT.plusDays(2), CLEAN_PRICE))
        .build();
    CurrencyAmount computed2 = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPrice(trade2, provider1, REF_DATA, cleanPrice);
    CurrencyAmount computed2Continuous = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade2, provider1, REF_DATA, cleanPrice, Z_SPREAD, CONTINUOUS, 0);
    CurrencyAmount computed2Periodic = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade2, provider1, REF_DATA, cleanPrice, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertThat(computed2.getAmount()).isCloseTo(QUANTITY * basePv1, offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed2Continuous.getAmount()).isCloseTo(QUANTITY * basePv1, offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed2Periodic.getAmount()).isCloseTo(QUANTITY * basePv1, offset(NOTIONAL * QUANTITY * TOL));
    // detachment date < standard settlement < trade sinfo (tradeDate = valuation1)
    ResolvedFixedCouponBondTrade trade3 = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_ON_DETACHMENT.plusDays(7), CLEAN_PRICE))
        .build();
    CurrencyAmount computed3 = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPrice(trade3, provider1, REF_DATA, cleanPrice);
    CurrencyAmount computed3Continuous = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade3, provider1, REF_DATA, cleanPrice, Z_SPREAD, CONTINUOUS, 0);
    CurrencyAmount computed3Periodic = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade3, provider1, REF_DATA, cleanPrice, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertThat(computed3.getAmount()).isCloseTo(QUANTITY * basePv1, offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed3Continuous.getAmount()).isCloseTo(QUANTITY * basePv1, offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed3Periodic.getAmount()).isCloseTo(QUANTITY * basePv1, offset(NOTIONAL * QUANTITY * TOL));

    // standard settlement < detachment date < trade settlement (tradeDate = TRADE_BEFORE)
    LocalDate settlement4 = SETTLE_ON_DETACHMENT.plusDays(1);
    ResolvedFixedCouponBondTrade trade4 = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(settlement4, CLEAN_PRICE))
        .build();
    LocalDate standardSettlement4 = PRODUCT.getSettlementDateOffset().adjust(TRADE_BEFORE, REF_DATA);
    double df4 = ZeroRateDiscountFactors.of(EUR, TRADE_BEFORE, CURVE_REPO).discountFactor(standardSettlement4);
    double accruedInterest4 = PRODUCT_PRICER.accruedInterest(PRODUCT, standardSettlement4);
    double basePv4 = cleanPrice * df4 * NOTIONAL + accruedInterest4 * df4;
    double pvExtra4 = COUPON_PRICER.presentValue(periodExtra, PROVIDER_BEFORE.issuerCurveDiscountFactors(ISSUER_ID, EUR));
    double pvExtra4Continuous = COUPON_PRICER.presentValueWithSpread(
        periodExtra, PROVIDER_BEFORE.issuerCurveDiscountFactors(ISSUER_ID, EUR), Z_SPREAD, CONTINUOUS, 0);
    double pvExtra4Periodic = COUPON_PRICER.presentValueWithSpread(periodExtra, 
        PROVIDER_BEFORE.issuerCurveDiscountFactors(ISSUER_ID, EUR), Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurrencyAmount computed4 = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPrice(trade4, PROVIDER_BEFORE, REF_DATA, cleanPrice);
    CurrencyAmount computed4Continuous = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade4, PROVIDER_BEFORE, REF_DATA, cleanPrice, Z_SPREAD, CONTINUOUS, 0);
    CurrencyAmount computed4Periodic = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade4, PROVIDER_BEFORE, REF_DATA, cleanPrice, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);

    assertThat(computed4.getAmount()).isCloseTo(QUANTITY * (basePv4 - pvExtra4), offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed4Continuous.getAmount()).isCloseTo(QUANTITY * (basePv4 - pvExtra4Continuous), offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed4Periodic.getAmount()).isCloseTo(QUANTITY * (basePv4 - pvExtra4Periodic), offset(NOTIONAL * QUANTITY * TOL));
    // standard settlement < trade settlement < detachment date (tradeDate = TRADE_BEFORE)
    LocalDate settlement5 = TRADE_BEFORE.plusDays(7);
    ResolvedFixedCouponBondTrade trade5 = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(settlement5, CLEAN_PRICE))
        .build();
    CurrencyAmount computed5 = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPrice(trade5, PROVIDER_BEFORE, REF_DATA, cleanPrice);
    CurrencyAmount computed5Continuous = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade5, PROVIDER_BEFORE, REF_DATA, cleanPrice, Z_SPREAD, CONTINUOUS, 0);
    CurrencyAmount computed5Periodic = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade5, PROVIDER_BEFORE, REF_DATA, cleanPrice, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertThat(computed5.getAmount()).isCloseTo(QUANTITY * basePv4, offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed5Continuous.getAmount()).isCloseTo(QUANTITY * basePv4, offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed5Periodic.getAmount()).isCloseTo(QUANTITY * basePv4, offset(NOTIONAL * QUANTITY * TOL));
    // trade settlement < standard settlement < detachment date
    ResolvedFixedCouponBondTrade trade6 = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_BEFORE, CLEAN_PRICE))
        .build();
    CurrencyAmount computed6 = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPrice(trade6, PROVIDER_BEFORE, REF_DATA, cleanPrice);
    CurrencyAmount computed6Continuous = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade6, PROVIDER_BEFORE, REF_DATA, cleanPrice, Z_SPREAD, CONTINUOUS, 0);
    CurrencyAmount computed6Periodic = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade6, PROVIDER_BEFORE, REF_DATA, cleanPrice, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertThat(computed6.getAmount()).isCloseTo(QUANTITY * basePv4, offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed6Continuous.getAmount()).isCloseTo(QUANTITY * basePv4, offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed6Periodic.getAmount()).isCloseTo(QUANTITY * basePv4, offset(NOTIONAL * QUANTITY * TOL));
  }

  @Test
  public void test_presentValueFromCleanPrice_dateLogic_noExcoupon() {
    double cleanPrice = 0.985;
    FixedCouponBondPaymentPeriod periodExtra = findPeriod(PRODUCT_NO_EXCOUPON, SETTLE_BEFORE, SETTLEMENT);
    // trade settlement < coupon date < standard settlement (tradeDate = valuation1)
    LocalDate valuation1 = SETTLE_ON_COUPON.minusDays(1);
    ResolvedFixedCouponBondTrade trade1 = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT_NO_EXCOUPON)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(valuation1, CLEAN_PRICE))
        .build();
    LegalEntityDiscountingProvider provider1 = createRatesProvider(valuation1);
    LocalDate standardSettlement1 = PRODUCT_NO_EXCOUPON.getSettlementDateOffset().adjust(valuation1, REF_DATA);
    double df1 = ZeroRateDiscountFactors.of(EUR, valuation1, CURVE_REPO).discountFactor(standardSettlement1);
    double accruedInterest1 = PRODUCT_PRICER.accruedInterest(PRODUCT_NO_EXCOUPON, standardSettlement1);
    double basePv1 = cleanPrice * df1 * NOTIONAL + accruedInterest1 * df1;
    double pvExtra1 = COUPON_PRICER.presentValue(periodExtra, provider1.issuerCurveDiscountFactors(ISSUER_ID, EUR));
    double pvExtra1Continuous = COUPON_PRICER.presentValueWithSpread(
        periodExtra, provider1.issuerCurveDiscountFactors(ISSUER_ID, EUR), Z_SPREAD, CONTINUOUS, 0);
    double pvExtra1Periodic = COUPON_PRICER.presentValueWithSpread(
        periodExtra, provider1.issuerCurveDiscountFactors(ISSUER_ID, EUR), Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurrencyAmount computed1 = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPrice(trade1, provider1, REF_DATA, cleanPrice);
    CurrencyAmount computed1Continuous = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade1, provider1, REF_DATA, cleanPrice, Z_SPREAD, CONTINUOUS, 0);
    CurrencyAmount computed1Periodic = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade1, provider1, REF_DATA, cleanPrice, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertThat(computed1.getAmount()).isCloseTo(QUANTITY * (basePv1 + pvExtra1), offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed1Continuous.getAmount()).isCloseTo(QUANTITY * (basePv1 + pvExtra1Continuous), offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed1Periodic.getAmount()).isCloseTo(QUANTITY * (basePv1 + pvExtra1Periodic), offset(NOTIONAL * QUANTITY * TOL));
    // coupon date < trade settlement < standard settlement (tradeDate = valuation1)
    LocalDate settlement2 = SETTLE_ON_COUPON.plusDays(2);
    ResolvedFixedCouponBondTrade trade2 = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT_NO_EXCOUPON)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(settlement2, CLEAN_PRICE))
        .build();
    CurrencyAmount computed2 = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPrice(trade2, provider1, REF_DATA, cleanPrice);
    CurrencyAmount computed2Continuous = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade2, provider1, REF_DATA, cleanPrice, Z_SPREAD, CONTINUOUS, 0);
    CurrencyAmount computed2Periodic = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade2, provider1, REF_DATA, cleanPrice, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertThat(computed2.getAmount()).isCloseTo(QUANTITY * basePv1, offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed2Continuous.getAmount()).isCloseTo(QUANTITY * basePv1, offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed2Periodic.getAmount()).isCloseTo(QUANTITY * basePv1, offset(NOTIONAL * QUANTITY * TOL));
    // coupon date < standard settlement < trade settlement (tradeDate = valuation1)
    LocalDate settlement3 = SETTLE_ON_COUPON.plusDays(7);
    ResolvedFixedCouponBondTrade trade3 = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT_NO_EXCOUPON)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(settlement3, CLEAN_PRICE))
        .build();
    CurrencyAmount computed3 = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPrice(trade3, provider1, REF_DATA, cleanPrice);
    CurrencyAmount computed3Continuous = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade3, provider1, REF_DATA, cleanPrice, Z_SPREAD, CONTINUOUS, 0);
    CurrencyAmount computed3Periodic = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade3, provider1, REF_DATA, cleanPrice, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertThat(computed3.getAmount()).isCloseTo(QUANTITY * basePv1, offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed3Continuous.getAmount()).isCloseTo(QUANTITY * basePv1, offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed3Periodic.getAmount()).isCloseTo(QUANTITY * basePv1, offset(NOTIONAL * QUANTITY * TOL));

    // standard settlement < coupon date < trade settlement (tradeDate = TRADE_BEFORE)
    LocalDate settlement4 = SETTLE_ON_COUPON.plusDays(1);
    ResolvedFixedCouponBondTrade trade4 = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT_NO_EXCOUPON)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(settlement4, CLEAN_PRICE))
        .build();
    LocalDate standardSettlement4 = PRODUCT_NO_EXCOUPON.getSettlementDateOffset().adjust(TRADE_BEFORE, REF_DATA);
    double df4 = ZeroRateDiscountFactors.of(EUR, TRADE_BEFORE, CURVE_REPO).discountFactor(standardSettlement4);
    double accruedInterest4 = PRODUCT_PRICER.accruedInterest(PRODUCT_NO_EXCOUPON, standardSettlement4);
    double basePv4 = cleanPrice * df4 * NOTIONAL + accruedInterest4 * df4;
    double pvExtra4 = COUPON_PRICER.presentValue(periodExtra,
        PROVIDER_BEFORE.issuerCurveDiscountFactors(ISSUER_ID, EUR));
    double pvExtra4Continuous = COUPON_PRICER.presentValueWithSpread(periodExtra,
        PROVIDER_BEFORE.issuerCurveDiscountFactors(ISSUER_ID, EUR), Z_SPREAD, CONTINUOUS, 0);
    double pvExtra4Periodic = COUPON_PRICER.presentValueWithSpread(periodExtra,
        PROVIDER_BEFORE.issuerCurveDiscountFactors(ISSUER_ID, EUR), Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurrencyAmount computed4 = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPrice(trade4, PROVIDER_BEFORE, REF_DATA, cleanPrice);
    CurrencyAmount computed4Continuous = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade4, PROVIDER_BEFORE, REF_DATA, cleanPrice, Z_SPREAD, CONTINUOUS, 0);
    CurrencyAmount computed4Periodic = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade4, PROVIDER_BEFORE, REF_DATA, cleanPrice, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertThat(computed4.getAmount()).isCloseTo(QUANTITY * (basePv4 - pvExtra4), offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed4Continuous.getAmount()).isCloseTo(QUANTITY * (basePv4 - pvExtra4Continuous), offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed4Periodic.getAmount()).isCloseTo(QUANTITY * (basePv4 - pvExtra4Periodic), offset(NOTIONAL * QUANTITY * TOL));
    // standard settlement < trade settlement < coupon date (tradeDate = TRADE_BEFORE)
    LocalDate settlement5 = TRADE_BEFORE.plusDays(7);
    ResolvedFixedCouponBondTrade trade5 = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT_NO_EXCOUPON)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(settlement5, CLEAN_PRICE))
        .build();
    CurrencyAmount computed5 = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPrice(trade5, PROVIDER_BEFORE, REF_DATA, cleanPrice);
    CurrencyAmount computed5Continuous = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade5, PROVIDER_BEFORE, REF_DATA, cleanPrice, Z_SPREAD, CONTINUOUS, 0);
    CurrencyAmount computed5Periodic = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade5, PROVIDER_BEFORE, REF_DATA, cleanPrice, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertThat(computed5.getAmount()).isCloseTo(QUANTITY * basePv4, offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed5Continuous.getAmount()).isCloseTo(QUANTITY * basePv4, offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed5Periodic.getAmount()).isCloseTo(QUANTITY * basePv4, offset(NOTIONAL * QUANTITY * TOL));
    // trade settlement < standard settlement < coupon date
    ResolvedFixedCouponBondTrade trade6 = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT_NO_EXCOUPON)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_BEFORE, CLEAN_PRICE))
        .build();
    CurrencyAmount computed6 = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPrice(trade6, PROVIDER_BEFORE, REF_DATA, cleanPrice);
    CurrencyAmount computed6Continuous = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade6, PROVIDER_BEFORE, REF_DATA, cleanPrice, Z_SPREAD, CONTINUOUS, 0);
    CurrencyAmount computed6Periodic = TRADE_PRICER_NO_UPFRONT.presentValueFromCleanPriceWithZSpread(
        trade6, PROVIDER_BEFORE, REF_DATA, cleanPrice, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertThat(computed6.getAmount()).isCloseTo(QUANTITY * basePv4, offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed6Continuous.getAmount()).isCloseTo(QUANTITY * basePv4, offset(NOTIONAL * QUANTITY * TOL));
    assertThat(computed6Periodic.getAmount()).isCloseTo(QUANTITY * basePv4, offset(NOTIONAL * QUANTITY * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueFromCleanPrice_coherency() {
    double priceDirty = PRODUCT_PRICER.dirtyPriceFromCurves(PRODUCT, PROVIDER, REF_DATA);
    LocalDate standardSettlementDate = PRODUCT.getSettlementDateOffset().adjust(PROVIDER.getValuationDate(), REF_DATA);
    double priceCleanComputed = PRODUCT_PRICER.cleanPriceFromDirtyPrice(PRODUCT, standardSettlementDate, priceDirty);
    CurrencyAmount pvCleanPrice = TRADE_PRICER.presentValueFromCleanPrice(TRADE, PROVIDER, REF_DATA, priceCleanComputed);
    CurrencyAmount pvCurves = TRADE_PRICER.presentValue(TRADE, PROVIDER);
    assertThat(pvCleanPrice.getAmount()).isCloseTo(pvCurves.getAmount(), offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValueFromCleanPriceWithZSpread_continuous_coherency() {
    double priceDirty = PRODUCT_PRICER
        .dirtyPriceFromCurvesWithZSpread(PRODUCT, PROVIDER, REF_DATA, Z_SPREAD, CONTINUOUS, 0);
    LocalDate standardSettlementDate = PRODUCT.getSettlementDateOffset().adjust(PROVIDER.getValuationDate(), REF_DATA);
    double priceCleanComputed = PRODUCT_PRICER.cleanPriceFromDirtyPrice(PRODUCT, standardSettlementDate, priceDirty);
    CurrencyAmount pvCleanPrice = TRADE_PRICER.presentValueFromCleanPriceWithZSpread(
        TRADE, PROVIDER, REF_DATA, priceCleanComputed, Z_SPREAD, CONTINUOUS, 0);
    CurrencyAmount pvCurves = TRADE_PRICER.presentValueWithZSpread(TRADE, PROVIDER, Z_SPREAD, CONTINUOUS, 0);
    assertThat(pvCleanPrice.getAmount()).isCloseTo(pvCurves.getAmount(), offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValueFromCleanPriceWithZSpread_periodic_coherency() {
    double priceDirty = PRODUCT_PRICER.dirtyPriceFromCurvesWithZSpread(
        PRODUCT, PROVIDER, REF_DATA, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    LocalDate standardSettlementDate = PRODUCT.getSettlementDateOffset().adjust(PROVIDER.getValuationDate(), REF_DATA);
    double priceCleanComputed = PRODUCT_PRICER.cleanPriceFromDirtyPrice(PRODUCT, standardSettlementDate, priceDirty);
    CurrencyAmount pvCleanPrice = TRADE_PRICER.presentValueFromCleanPriceWithZSpread(
        TRADE, PROVIDER, REF_DATA, priceCleanComputed, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurrencyAmount pvCurves = TRADE_PRICER
        .presentValueWithZSpread(TRADE, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertThat(pvCleanPrice.getAmount()).isCloseTo(pvCurves.getAmount(), offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValueFromCleanPrice_noExcoupon_coherency() {
    double priceDirty = PRODUCT_PRICER.dirtyPriceFromCurves(PRODUCT_NO_EXCOUPON, PROVIDER, REF_DATA);
    LocalDate standardSettlementDate = PRODUCT.getSettlementDateOffset().adjust(PROVIDER.getValuationDate(), REF_DATA);
    double priceCleanComputed = PRODUCT_PRICER.cleanPriceFromDirtyPrice(PRODUCT, standardSettlementDate, priceDirty);
    CurrencyAmount pvCleanPrice = TRADE_PRICER.presentValueFromCleanPrice(
        TRADE_NO_EXCOUPON, PROVIDER, REF_DATA, priceCleanComputed);
    CurrencyAmount pvCurves = TRADE_PRICER.presentValue(TRADE_NO_EXCOUPON, PROVIDER);
    assertThat(pvCleanPrice.getAmount()).isCloseTo(pvCurves.getAmount(), offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValueFromCleanPriceWithZSpread_continuous_noExcoupon_coherency() {
    double priceDirty = PRODUCT_PRICER.dirtyPriceFromCurvesWithZSpread(
        PRODUCT_NO_EXCOUPON, PROVIDER, REF_DATA, Z_SPREAD, CONTINUOUS, 0);
    LocalDate standardSettlementDate = PRODUCT.getSettlementDateOffset().adjust(PROVIDER.getValuationDate(), REF_DATA);
    double priceCleanComputed = PRODUCT_PRICER.cleanPriceFromDirtyPrice(PRODUCT, standardSettlementDate, priceDirty);
    CurrencyAmount pvCleanPrice = TRADE_PRICER.presentValueFromCleanPriceWithZSpread(
        TRADE_NO_EXCOUPON, PROVIDER, REF_DATA, priceCleanComputed, Z_SPREAD, CONTINUOUS, 0);
    CurrencyAmount pvCurves = TRADE_PRICER
        .presentValueWithZSpread(TRADE_NO_EXCOUPON, PROVIDER, Z_SPREAD, CONTINUOUS, 0);
    assertThat(pvCleanPrice.getAmount()).isCloseTo(pvCurves.getAmount(), offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValueFromCleanPriceWithZSpread_periodic_noExcoupon_coherency() {
    double priceDirty = PRODUCT_PRICER.dirtyPriceFromCurvesWithZSpread(
        PRODUCT_NO_EXCOUPON, PROVIDER, REF_DATA, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    LocalDate standardSettlementDate = PRODUCT.getSettlementDateOffset().adjust(PROVIDER.getValuationDate(), REF_DATA);
    double priceCleanComputed = PRODUCT_PRICER.cleanPriceFromDirtyPrice(PRODUCT, standardSettlementDate, priceDirty);
    CurrencyAmount pvCleanPrice = TRADE_PRICER.presentValueFromCleanPriceWithZSpread(
        TRADE_NO_EXCOUPON, PROVIDER, REF_DATA, priceCleanComputed, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurrencyAmount pvCurves = TRADE_PRICER.presentValueWithZSpread(
        TRADE_NO_EXCOUPON, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertThat(pvCleanPrice.getAmount()).isCloseTo(pvCurves.getAmount(), offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivity() {
    PointSensitivities pointTrade = TRADE_PRICER.presentValueSensitivity(TRADE, PROVIDER);
    CurrencyParameterSensitivities computedTrade = PROVIDER.parameterSensitivity(pointTrade);
    CurrencyParameterSensitivities expectedTrade = FD_CAL.sensitivity(PROVIDER,
        (p) -> TRADE_PRICER.presentValue(TRADE, (p)));
    assertThat(computedTrade.equalWithTolerance(expectedTrade, 30d * NOTIONAL * QUANTITY * EPS)).isTrue();
  }

  @Test
  public void test_presentValueSensitivityWithZSpread_continuous() {
    PointSensitivities pointTrade =
        TRADE_PRICER.presentValueSensitivityWithZSpread(TRADE, PROVIDER, Z_SPREAD, CONTINUOUS, 0);
    CurrencyParameterSensitivities computedTrade = PROVIDER.parameterSensitivity(pointTrade);
    CurrencyParameterSensitivities expectedTrade = FD_CAL.sensitivity(
        PROVIDER, (p) -> TRADE_PRICER.presentValueWithZSpread(TRADE, (p), Z_SPREAD, CONTINUOUS, 0));
    assertThat(computedTrade.equalWithTolerance(expectedTrade, 20d * NOTIONAL * QUANTITY * EPS)).isTrue();
  }

  @Test
  public void test_presentValueSensitivityWithZSpread_periodic() {
    PointSensitivities pointTrade =
        TRADE_PRICER.presentValueSensitivityWithZSpread(TRADE, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurrencyParameterSensitivities computedTrade = PROVIDER.parameterSensitivity(pointTrade);
    CurrencyParameterSensitivities expectedTrade = FD_CAL.sensitivity(PROVIDER,
        (p) -> TRADE_PRICER.presentValueWithZSpread(TRADE, (p), Z_SPREAD, PERIODIC, PERIOD_PER_YEAR));
    assertThat(computedTrade.equalWithTolerance(expectedTrade, 20d * NOTIONAL * QUANTITY * EPS)).isTrue();
  }

  @Test
  public void test_presentValueProductSensitivity_noExcoupon() {
    PointSensitivities pointTrade = TRADE_PRICER.presentValueSensitivity(TRADE_NO_EXCOUPON, PROVIDER);
    CurrencyParameterSensitivities computedTrade = PROVIDER.parameterSensitivity(pointTrade);
    CurrencyParameterSensitivities expectedTrade = FD_CAL.sensitivity(
        PROVIDER, (p) -> TRADE_PRICER.presentValue(TRADE_NO_EXCOUPON, (p)));
    assertThat(computedTrade.equalWithTolerance(expectedTrade, 30d * NOTIONAL * QUANTITY * EPS)).isTrue();
  }

  @Test
  public void test_presentValueSensitivityWithZSpread_continuous_noExcoupon() {
    PointSensitivities pointTrade =
        TRADE_PRICER.presentValueSensitivityWithZSpread(TRADE_NO_EXCOUPON, PROVIDER, Z_SPREAD, CONTINUOUS, 0);
    CurrencyParameterSensitivities computedTrade = PROVIDER.parameterSensitivity(pointTrade);
    CurrencyParameterSensitivities expectedTrade = FD_CAL.sensitivity(PROVIDER, (p) ->
        TRADE_PRICER.presentValueWithZSpread(TRADE_NO_EXCOUPON, (p), Z_SPREAD, CONTINUOUS, 0));
    assertThat(computedTrade.equalWithTolerance(expectedTrade, 20d * NOTIONAL * QUANTITY * EPS)).isTrue();
  }

  @Test
  public void test_presentValueSensitivityWithZSpread_periodic_noExcoupon() {
    PointSensitivities pointTrade = TRADE_PRICER.presentValueSensitivityWithZSpread(
        TRADE_NO_EXCOUPON, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurrencyParameterSensitivities computedTrade = PROVIDER.parameterSensitivity(pointTrade);
    CurrencyParameterSensitivities expectedTrade = FD_CAL.sensitivity(PROVIDER, (p) ->
        TRADE_PRICER.presentValueWithZSpread(TRADE_NO_EXCOUPON, (p), Z_SPREAD, PERIODIC, PERIOD_PER_YEAR));
    assertThat(computedTrade.equalWithTolerance(expectedTrade, 20d * NOTIONAL * QUANTITY * EPS)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivity_dateLogic() {
    ResolvedFixedCouponBondTrade tradeAfter = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLEMENT, CLEAN_PRICE))
        .build();
    PointSensitivities computedTradeAfter = TRADE_PRICER_NO_UPFRONT.presentValueSensitivity(tradeAfter, PROVIDER_BEFORE);
    // settle before detachment date
    ResolvedFixedCouponBondTrade tradeBefore = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_BEFORE, CLEAN_PRICE))
        .build();
    PointSensitivities computedTradeBefore =
        TRADE_PRICER_NO_UPFRONT.presentValueSensitivity(tradeBefore, PROVIDER_BEFORE);
    FixedCouponBondPaymentPeriod periodExtra = findPeriod(PRODUCT, SETTLE_BEFORE, SETTLEMENT);
    PointSensitivities sensiExtra = COUPON_PRICER
        .presentValueSensitivity(periodExtra, PROVIDER_BEFORE.issuerCurveDiscountFactors(ISSUER_ID, EUR)).build();
    assertThat(computedTradeBefore.normalized().equalWithTolerance(
        computedTradeAfter.combinedWith(sensiExtra.multipliedBy(QUANTITY)).normalized(), NOTIONAL * QUANTITY * TOL)).isTrue();
    // settle on detachment date
    ResolvedFixedCouponBondTrade tradeOnDetachment = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_ON_DETACHMENT, CLEAN_PRICE))
        .build();
    PointSensitivities computedTradeOnDetachment =
        TRADE_PRICER_NO_UPFRONT.presentValueSensitivity(tradeOnDetachment, PROVIDER_BEFORE);
    assertThat(computedTradeOnDetachment.equalWithTolerance(computedTradeAfter, NOTIONAL * QUANTITY * TOL)).isTrue();
    // settle between detachment date and coupon date
    ResolvedFixedCouponBondTrade tradeBtwnDetachmentCoupon = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_BTWN_DETACHMENT_COUPON, CLEAN_PRICE))
        .build();
    PointSensitivities computedTradeBtwnDetachmentCoupon =
        TRADE_PRICER_NO_UPFRONT.presentValueSensitivity(tradeBtwnDetachmentCoupon, PROVIDER_BEFORE);
    assertThat(computedTradeBtwnDetachmentCoupon.equalWithTolerance(computedTradeAfter, NOTIONAL * QUANTITY * TOL)).isTrue();
  }

  @Test
  public void test_presentValueSensitivity_dateLogic_pastSettle() {
    ResolvedFixedCouponBondTrade tradeAfter = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLEMENT, CLEAN_PRICE))
        .build();
    PointSensitivities computedTradeAfter = TRADE_PRICER_NO_UPFRONT.presentValueSensitivity(tradeAfter, PROVIDER);
    // settle before detachment date
    ResolvedFixedCouponBondTrade tradeBefore = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_BEFORE, CLEAN_PRICE))
        .build();
    PointSensitivities computedTradeBefore = TRADE_PRICER_NO_UPFRONT.presentValueSensitivity(tradeBefore, PROVIDER);
    assertThat(computedTradeBefore.equalWithTolerance(computedTradeAfter, NOTIONAL * QUANTITY * TOL)).isTrue();
    // settle on detachment date
    ResolvedFixedCouponBondTrade tradeOnDetachment = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_ON_DETACHMENT, CLEAN_PRICE))
        .build();
    PointSensitivities computedTradeOnDetachment =
        TRADE_PRICER_NO_UPFRONT.presentValueSensitivity(tradeOnDetachment, PROVIDER);
    assertThat(computedTradeOnDetachment.equalWithTolerance(computedTradeAfter, NOTIONAL * QUANTITY * TOL)).isTrue();
    // settle between detachment date and coupon date
    ResolvedFixedCouponBondTrade tradeBtwnDetachmentCoupon = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_BTWN_DETACHMENT_COUPON, CLEAN_PRICE))
        .build();
    PointSensitivities computedTradeBtwnDetachmentCoupon =
        TRADE_PRICER_NO_UPFRONT.presentValueSensitivity(tradeBtwnDetachmentCoupon, PROVIDER);
    assertThat(computedTradeBtwnDetachmentCoupon.equalWithTolerance(computedTradeAfter, NOTIONAL * QUANTITY * TOL)).isTrue();
  }

  @Test
  public void test_presentValueSensitivity_dateLogic_noExcoupon() {
    ResolvedFixedCouponBondTrade tradeAfter = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT_NO_EXCOUPON)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLEMENT, CLEAN_PRICE))
        .build();
    PointSensitivities computedTradeAfter = TRADE_PRICER_NO_UPFRONT.presentValueSensitivity(tradeAfter, PROVIDER_BEFORE);
    // settle before coupon date
    ResolvedFixedCouponBondTrade tradeBefore = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT_NO_EXCOUPON)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_BEFORE, CLEAN_PRICE))
        .build();
    PointSensitivities computedTradeBefore =
        TRADE_PRICER_NO_UPFRONT.presentValueSensitivity(tradeBefore, PROVIDER_BEFORE);
    FixedCouponBondPaymentPeriod periodExtra = findPeriod(PRODUCT_NO_EXCOUPON, SETTLE_BEFORE, SETTLEMENT);
    PointSensitivities sensiExtra = COUPON_PRICER
        .presentValueSensitivity(periodExtra, PROVIDER_BEFORE.issuerCurveDiscountFactors(ISSUER_ID, EUR)).build();
    assertThat(computedTradeBefore.normalized().equalWithTolerance(
        computedTradeAfter.combinedWith(sensiExtra.multipliedBy(QUANTITY)).normalized(), NOTIONAL * QUANTITY * TOL)).isTrue();
    // settle on coupon date
    ResolvedFixedCouponBondTrade tradeOnCoupon = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT_NO_EXCOUPON)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_ON_COUPON, CLEAN_PRICE))
        .build();
    PointSensitivities computedTradeOnCoupon = TRADE_PRICER_NO_UPFRONT.presentValueSensitivity(tradeOnCoupon, PROVIDER_BEFORE);
    assertThat(computedTradeOnCoupon.equalWithTolerance(computedTradeAfter, NOTIONAL * QUANTITY * TOL)).isTrue();
  }

  @Test
  public void test_presentValueSensitivity_dateLogic_pastSettle_noExcoupon() {
    ResolvedFixedCouponBondTrade tradeAfter = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT_NO_EXCOUPON)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLEMENT, CLEAN_PRICE))
        .build();
    PointSensitivities computedTradeAfter = TRADE_PRICER_NO_UPFRONT.presentValueSensitivity(tradeAfter, PROVIDER);
    // settle before coupon date
    ResolvedFixedCouponBondTrade tradeBefore = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT_NO_EXCOUPON)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_BEFORE, CLEAN_PRICE))
        .build();
    PointSensitivities computedTradeBefore = TRADE_PRICER_NO_UPFRONT.presentValueSensitivity(tradeBefore, PROVIDER);
    assertThat(computedTradeBefore.equalWithTolerance(computedTradeAfter, NOTIONAL * QUANTITY * TOL)).isTrue();
    // settle on coupon date
    ResolvedFixedCouponBondTrade tradeOnCoupon = ResolvedFixedCouponBondTrade.builder()
        .product(PRODUCT_NO_EXCOUPON)
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLE_ON_COUPON, CLEAN_PRICE))
        .build();
    PointSensitivities computedTradeOnCoupon = TRADE_PRICER_NO_UPFRONT.presentValueSensitivity(tradeOnCoupon, PROVIDER);
    assertThat(computedTradeOnCoupon.equalWithTolerance(computedTradeAfter, NOTIONAL * QUANTITY * TOL)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_currencyExposure() {
    MultiCurrencyAmount ceComputed = TRADE_PRICER.currencyExposure(TRADE, PROVIDER);
    CurrencyAmount pv = TRADE_PRICER.presentValue(TRADE, PROVIDER);
    assertThat(ceComputed).isEqualTo(MultiCurrencyAmount.of(pv));
  }

  @Test
  public void test_currencyExposureWithZSpread() {
    MultiCurrencyAmount ceComputed = TRADE_PRICER.currencyExposureWithZSpread(
        TRADE, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurrencyAmount pv = TRADE_PRICER.presentValueWithZSpread(TRADE, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertThat(ceComputed).isEqualTo(MultiCurrencyAmount.of(pv));
  }

  @Test
  public void test_currentCash_zero() {
    CurrencyAmount ccComputed = TRADE_PRICER.currentCash(TRADE, VAL_DATE);
    assertThat(ccComputed).isEqualTo(CurrencyAmount.zero(EUR));
  }

  @Test
  public void test_currentCash_valuationAtSettlement() {
    CurrencyAmount ccComputed = TRADE_PRICER.currentCash(TRADE, SETTLEMENT);
    assertThat(ccComputed).isEqualTo(UPFRONT_PAYMENT.getValue());
  }

  @Test
  public void test_currentCash_valuationAtPayment() {
    LocalDate paymentDate = LocalDate.of(2016, 10, 12);
    CurrencyAmount ccComputed = TRADE_PRICER.currentCash(TRADE, paymentDate);
    assertThat(ccComputed).isEqualTo(CurrencyAmount.zero(EUR));
  }

  @Test
  public void test_currentCash_valuationAtPayment_noExcoupon() {
    LocalDate startDate = LocalDate.of(2016, 4, 12);
    LocalDate paymentDate = LocalDate.of(2016, 10, 12);
    double yc = DAY_COUNT.relativeYearFraction(startDate, paymentDate);
    CurrencyAmount ccComputed = TRADE_PRICER.currentCash(TRADE_NO_EXCOUPON, paymentDate);
    assertThat(ccComputed).isEqualTo(CurrencyAmount.of(EUR, FIXED_RATE * NOTIONAL * yc * QUANTITY));
  }

  @Test
  public void test_currentCash_valuationAtMaturity() {
    LocalDate paymentDate = LocalDate.of(2025, 4, 14);
    CurrencyAmount ccComputed = TRADE_PRICER.currentCash(TRADE, paymentDate);
    assertThat(ccComputed).isEqualTo(CurrencyAmount.of(EUR, NOTIONAL * QUANTITY));
  }

  @Test
  public void test_currentCash_valuationAtMaturity_noExcoupon() {
    LocalDate startDate = LocalDate.of(2024, 10, 14);
    LocalDate paymentDate = LocalDate.of(2025, 4, 14);
    double yc = DAY_COUNT.relativeYearFraction(startDate, paymentDate);
    CurrencyAmount ccComputed = TRADE_PRICER.currentCash(TRADE_NO_EXCOUPON, paymentDate);
    assertThat(ccComputed).isEqualTo(CurrencyAmount.of(EUR, NOTIONAL * (1d + yc * FIXED_RATE) * QUANTITY));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_upfrontPayment() {
    Payment payment = TRADE_PRICER.upfrontPayment(TRADE);
    assertThat(payment.getCurrency()).isEqualTo(EUR);
    assertThat(payment.getAmount()).isCloseTo(-NOTIONAL * QUANTITY * DIRTY_PRICE, offset(TOL));
    assertThat(payment.getDate()).isEqualTo(SETTLEMENT);
  }

  @Test
  public void test_upfrontPayment_position() {
    Payment payment = TRADE_PRICER.upfrontPayment(POSITION);
    assertThat(payment.getCurrency()).isEqualTo(EUR);
    assertThat(payment.getAmount()).isCloseTo(0, offset(TOL));
    assertThat(payment.getDate()).isEqualTo(POSITION.getProduct().getStartDate());
  }

  //-------------------------------------------------------------------------
  private static LegalEntityDiscountingProvider createRatesProvider(LocalDate valuationDate) {
    DiscountFactors dscRepo = ZeroRateDiscountFactors.of(EUR, valuationDate, CURVE_REPO);
    DiscountFactors dscIssuer = ZeroRateDiscountFactors.of(EUR, valuationDate, CURVE_ISSUER);
    LegalEntityDiscountingProvider provider = ImmutableLegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.of(Pair.of(GROUP_ISSUER, EUR), dscIssuer))
        .issuerCurveGroups(ImmutableMap.of(ISSUER_ID, GROUP_ISSUER))
        .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO, EUR), dscRepo))
        .repoCurveSecurityGroups(ImmutableMap.of(SECURITY_ID, GROUP_REPO))
        .valuationDate(valuationDate)
        .build();
    return provider;
  }

  private FixedCouponBondPaymentPeriod findPeriod(ResolvedFixedCouponBond bond, LocalDate date1, LocalDate date2) {
    ImmutableList<FixedCouponBondPaymentPeriod> list = bond.getPeriodicPayments();
    for (FixedCouponBondPaymentPeriod period : list) {
      if (period.getDetachmentDate().equals(period.getPaymentDate())) {
        if (period.getPaymentDate().isAfter(date1) && period.getPaymentDate().isBefore(date2)) {
          return period;
        }
      } else {
        if (period.getDetachmentDate().isAfter(date1) && period.getDetachmentDate().isBefore(date2)) {
          return period;
        }
      }
    }
    return null;
  }
}
