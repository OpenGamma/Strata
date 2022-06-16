/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
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
import com.opengamma.strata.market.ValueType;
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
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.bond.FixedCouponBond;
import com.opengamma.strata.product.bond.FixedCouponBondOption;
import com.opengamma.strata.product.bond.FixedCouponBondYieldConvention;
import com.opengamma.strata.product.bond.ResolvedFixedCouponBond;
import com.opengamma.strata.product.bond.ResolvedFixedCouponBondOption;
import com.opengamma.strata.product.common.LongShort;

/**
 * Test {@link BlackFixedCouponBondOptionPricer}
 */
class BlackFixedCouponBondOptionPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  // fixed coupon bond
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Ticker", "GOVT1-BOND1");
  private static final LegalEntityId ISSUER_ID = LegalEntityId.of("OG-Ticker", "GOVT1");
  private static final LocalDate VAL_DATE = LocalDate.of(2022, 4, 22);
  private static final FixedCouponBondYieldConvention YIELD_CONVENTION = FixedCouponBondYieldConvention.DE_BONDS;
  private static final double NOTIONAL = 1.0;
  private static final double FIXED_RATE = 0.0450;
  private static final HolidayCalendarId EUR_CALENDAR = HolidayCalendarIds.EUTA;
  private static final DaysAdjustment DATE_OFFSET = DaysAdjustment.ofBusinessDays(3, EUR_CALENDAR);
  private static final DayCount DAY_COUNT = DayCounts.ACT_365F;
  private static final LocalDate START_DATE = LocalDate.of(2021, 4, 12);
  private static final LocalDate END_DATE = LocalDate.of(2031, 4, 12);
  private static final BusinessDayAdjustment BUSINESS_ADJUST =
      BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, EUR_CALENDAR);
  private static final PeriodicSchedule PERIOD_SCHEDULE = PeriodicSchedule.of(
      START_DATE, END_DATE, Frequency.P6M, BUSINESS_ADJUST, StubConvention.SHORT_INITIAL, false);
  private static final FixedCouponBond UNDERLYING = FixedCouponBond.builder()
      .securityId(SECURITY_ID)
      .dayCount(DAY_COUNT)
      .fixedRate(FIXED_RATE)
      .legalEntityId(ISSUER_ID)
      .currency(EUR)
      .notional(NOTIONAL)
      .accrualSchedule(PERIOD_SCHEDULE)
      .settlementDateOffset(DATE_OFFSET)
      .yieldConvention(YIELD_CONVENTION)
      .build();
  private static final ResolvedFixedCouponBond UNDERLYING_RESOLVED = UNDERLYING.resolve(REF_DATA);
  private static final double CLEAN_STRIKE = 0.98;
  private static final AdjustableDate EXPIRY_DATE = AdjustableDate.of(LocalDate.of(2022, 10, 24));
  private static final AdjustableDate SETTLE_DATE = AdjustableDate.of(LocalDate.of(2022, 10, 26));
  private static final double QUANTITY = 100_000_000;
  private static final FixedCouponBondOption BOND_OPTION_1 = FixedCouponBondOption.builder()
      .longShort(LongShort.LONG)
      .underlying(UNDERLYING)
      .expiryDate(EXPIRY_DATE)
      .expiryTime(LocalTime.of(11, 0))
      .expiryZone(ZoneId.of("Europe/Brussels"))
      .quantity(QUANTITY)
      .cleanStrikePrice(CLEAN_STRIKE)
      .settlementDate(SETTLE_DATE).build();
  private static final ResolvedFixedCouponBondOption BOND_OPTION =
      BOND_OPTION_1.resolve(REF_DATA);

  /* Rates provider */
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveName NAME_REPO = CurveName.of("TestRepoCurve");
  private static final CurveMetadata METADATA_REPO = Curves.zeroRates(NAME_REPO, ACT_365F);
  private static final InterpolatedNodalCurve CURVE_REPO = InterpolatedNodalCurve.of(
      METADATA_REPO, DoubleArray.of(0.1, 2.0, 10.0), DoubleArray.of(0.05, 0.06, 0.09), INTERPOLATOR);
  private static final DiscountFactors DSC_FACTORS_REPO = ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_REPO);
  private static final RepoGroup GROUP_REPO = RepoGroup.of("GOVT1 BOND1");
  private static final CurveName NAME_ISSUER = CurveName.of("TestIssuerCurve");
  private static final CurveMetadata METADATA_ISSUER = Curves.zeroRates(NAME_ISSUER, ACT_365F);
  private static final InterpolatedNodalCurve CURVE_ISSUER = InterpolatedNodalCurve.of(
      METADATA_ISSUER, DoubleArray.of(0.2, 9.0, 15.0), DoubleArray.of(0.03, 0.05, 0.06), INTERPOLATOR);
  private static final DiscountFactors DSC_FACTORS_ISSUER = ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_ISSUER);
  private static final LegalEntityGroup GROUP_ISSUER = LegalEntityGroup.of("GOVT1");
  private static final LegalEntityDiscountingProvider PROVIDER = ImmutableLegalEntityDiscountingProvider.builder()
      .issuerCurves(ImmutableMap.of(Pair.of(GROUP_ISSUER, EUR), DSC_FACTORS_ISSUER))
      .issuerCurveGroups(ImmutableMap.of(ISSUER_ID, GROUP_ISSUER))
      .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO, EUR), DSC_FACTORS_REPO))
      .repoCurveSecurityGroups(ImmutableMap.of(SECURITY_ID, GROUP_REPO))
      .valuationDate(VAL_DATE)
      .build();

  /* Volatilities */
  private static final LocalTime VALUATION_TIME = LocalTime.of(14, 30);
  private static final ZoneId VALUATION_ZONE = ZoneId.of("Europe/Brussels");
  private static final SurfaceName SURFACE_NAME = SurfaceName.of("EUR Govt Vol");
  private static final SurfaceMetadata SURFACE_METADATA = DefaultSurfaceMetadata.builder()
      .surfaceName(SURFACE_NAME)
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.YEAR_FRACTION)
      .zValueType(ValueType.NORMAL_VOLATILITY)
      .dayCount(ACT_365F).build();
  private static final double YIELD_VOL = 0.0050;
  private static final Surface VOL_SURFACE = ConstantSurface.of(SURFACE_METADATA, YIELD_VOL);
  private static final BondYieldVolatilities VOLATILITIES_FLAT = NormalBondYieldExpiryDurationVolatilities
      .of(EUR, VAL_DATE.atTime(VALUATION_TIME).atZone(VALUATION_ZONE), VOL_SURFACE);

  /* Pricers */
  private static final BlackFixedCouponBondOptionPricer PRICER_BOND_OPT = BlackFixedCouponBondOptionPricer.DEFAULT;
  private static final DiscountingFixedCouponBondProductPricer PRICER_BOND =
      DiscountingFixedCouponBondProductPricer.DEFAULT;
  private static final double FD_SHIFT = 1.0e-6;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(FD_SHIFT);

  /* Test */
  private static final double TOLERANCE_PV = 1.0E-2;
  private static final double TOLERANCE_RATES_SENSI = 1.0E+5; // 10 ccy per bp on 100m
  private static final double TOLERANCE_VOL_SENSI = 1.0E+4;

  @Test
  void presentValue_local_impl() {
    CurrencyAmount pvComputed = PRICER_BOND_OPT.presentValue(BOND_OPTION, PROVIDER, VOLATILITIES_FLAT);
    ZonedDateTime expiryDate = BOND_OPTION.getExpiry();
    LocalDate settlementDate = SETTLE_DATE.adjusted(REF_DATA);
    double expiry = VOLATILITIES_FLAT.relativeTime(expiryDate);
    double dirtyPriceStrike = PRICER_BOND.dirtyPriceFromCleanPrice(UNDERLYING_RESOLVED, settlementDate, CLEAN_STRIKE);
    double yieldStrike = PRICER_BOND.yieldFromDirtyPrice(UNDERLYING_RESOLVED, settlementDate, dirtyPriceStrike);
    double dirtyPriceForward =
        PRICER_BOND.dirtyPriceFromCurves(UNDERLYING_RESOLVED, PROVIDER, settlementDate);
    double yieldForward =
        PRICER_BOND.yieldFromDirtyPrice(UNDERLYING_RESOLVED, settlementDate, dirtyPriceForward);
    double modifiedDurationForward =
        PRICER_BOND.modifiedDurationFromYield(UNDERLYING_RESOLVED, settlementDate, yieldForward);
    double priceVolatility = VOLATILITIES_FLAT
        .priceVolatilityEquivalent(expiry, modifiedDurationForward, yieldStrike, yieldForward);
    double dfSettle = PROVIDER.repoCurveDiscountFactors(SECURITY_ID, ISSUER_ID, EUR)
        .discountFactor(settlementDate);
    double priceExpected = dfSettle *
        BlackFormulaRepository.price(dirtyPriceForward, dirtyPriceStrike, expiry, priceVolatility, true);
    double pvExpected = priceExpected * QUANTITY;
    assertThat(pvComputed.getAmount()).isCloseTo(pvExpected, offset(TOLERANCE_PV));
  }

  @Test
  void presentValue_put_call_parity() {
    ResolvedFixedCouponBondOption call = BOND_OPTION;
    ResolvedFixedCouponBondOption put = call.toBuilder().quantity(-QUANTITY).build();
    CurrencyAmount pvCall = PRICER_BOND_OPT.presentValue(call, PROVIDER, VOLATILITIES_FLAT);
    CurrencyAmount pvPut = PRICER_BOND_OPT.presentValue(put, PROVIDER, VOLATILITIES_FLAT);
    LocalDate settlementDate = SETTLE_DATE.adjusted(REF_DATA);
    double dirtyPriceForward =
        PRICER_BOND.dirtyPriceFromCurves(UNDERLYING_RESOLVED, PROVIDER, settlementDate);
    double dirtyPriceStrike = PRICER_BOND.dirtyPriceFromCleanPrice(UNDERLYING_RESOLVED, settlementDate, CLEAN_STRIKE);
    double dfSettle = PROVIDER.repoCurveDiscountFactors(SECURITY_ID, ISSUER_ID, EUR)
        .discountFactor(settlementDate);
    assertThat(pvCall.minus(pvPut).getAmount())
        .isCloseTo((dirtyPriceForward - dirtyPriceStrike) * dfSettle * QUANTITY, offset(TOLERANCE_PV));
  }

  @Test
  void presentValue_long_short_parity() {
    ResolvedFixedCouponBondOption callLong = BOND_OPTION;
    ResolvedFixedCouponBondOption callShort = BOND_OPTION.toBuilder().longShort(LongShort.SHORT).build();
    CurrencyAmount pvCallLong = PRICER_BOND_OPT.presentValue(callLong, PROVIDER, VOLATILITIES_FLAT);
    CurrencyAmount pvCallShort = PRICER_BOND_OPT.presentValue(callShort, PROVIDER, VOLATILITIES_FLAT);
    assertThat(pvCallLong.getAmount()).isCloseTo(-pvCallShort.getAmount(), offset(TOLERANCE_PV));
  }

  @Test
  void presentValue_rate_sensi() {
    PointSensitivities ptsComputed = PRICER_BOND_OPT
        .presentValueSensitivityRatesStickyStrike(BOND_OPTION, PROVIDER, VOLATILITIES_FLAT);
    CurrencyParameterSensitivities psComputed = PROVIDER.parameterSensitivity(ptsComputed);
    CurrencyParameterSensitivities psExpected = FD_CAL.sensitivity(PROVIDER,
        (p) -> PRICER_BOND_OPT.presentValue(BOND_OPTION, p, VOLATILITIES_FLAT));
    assertThat(psComputed.equalWithTolerance(psExpected, TOLERANCE_RATES_SENSI)).isTrue();
  }

  @Test
  void presentValue_vol_sensi() {
    BondYieldSensitivity ptsComputed = PRICER_BOND_OPT
        .presentValueSensitivityModelParamsVolatility(BOND_OPTION, PROVIDER, VOLATILITIES_FLAT);
    double shift = 1.0E-6;
    Surface surfaceShifted = ConstantSurface.of(SURFACE_METADATA, YIELD_VOL + shift);
    BondYieldVolatilities volatilitiesShifted = NormalBondYieldExpiryDurationVolatilities
        .of(EUR, VAL_DATE.atTime(VALUATION_TIME).atZone(VALUATION_ZONE), surfaceShifted);
    CurrencyAmount pv = PRICER_BOND_OPT.presentValue(BOND_OPTION, PROVIDER, VOLATILITIES_FLAT);
    CurrencyAmount pvShifted = PRICER_BOND_OPT.presentValue(BOND_OPTION, PROVIDER, volatilitiesShifted);
    double yieldSensitivity = (pvShifted.getAmount() - pv.getAmount()) / shift;
    assertThat(ptsComputed.getSensitivity()).isCloseTo(yieldSensitivity, offset(TOLERANCE_VOL_SENSI));
  }
  
  // TODO

}
