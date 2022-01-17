/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.index.OvernightIndices.EUR_ESTR;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.PeriodAdditionConventions;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.date.TenorAdjustment;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.ImmutableIborIndex;
import com.opengamma.strata.basics.index.OvernightIndexObservation;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.impl.rate.ForwardOvernightCompoundedRateComputationFn;
import com.opengamma.strata.pricer.impl.swap.DiscountingRatePaymentPeriodPricer;
import com.opengamma.strata.pricer.rate.DiscountOvernightIndexRates;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.capfloor.IborCapletFloorletPeriod;
import com.opengamma.strata.product.capfloor.OvernightInArrearsCapletFloorletPeriod;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.rate.IborRateComputation;
import com.opengamma.strata.product.rate.OvernightCompoundedRateComputation;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;

/**
 * Test {@link VolatilityOvernightInArrearsCapletFloorletPeriodPricer}.
 */
public class BlackOvernightInArrearsCapletFloorletPeriodPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final IborIndex EUR_ESTRTERM_3M = ImmutableIborIndex.builder()
      .name("EUR-ESTRTERM-3M")
      .currency(EUR)
      .dayCount(DayCounts.ACT_360)
      .fixingCalendar(EUTA)
      .fixingTime(LocalTime.of(11, 0))
      .fixingZone(ZoneId.of("Europe/Brussels"))
      .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, EUTA))
      .effectiveDateOffset(DaysAdjustment.ofBusinessDays(2, EUTA))
      .maturityDateOffset(TenorAdjustment.of(
          Tenor.TENOR_3M,
          PeriodAdditionConventions.LAST_BUSINESS_DAY,
          BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, EUTA)))
      .build();
  private static final HolidayCalendar EUTA_IMPL = REF_DATA.getValue(EUTA);
  private static final ZonedDateTime VALUATION = dateUtc(2021, 12, 20);
  private static final ZonedDateTime VALUATION_AFTER_START = dateUtc(2022, 8, 18);
  private static final ZonedDateTime VALUATION_AFTER_PAY = dateUtc(2022, 11, 18);
  private static final ZonedDateTime VALUATION_AFTER_END = dateUtc(2022, 9, 29);
  private static final LocalDate START_DATE = LocalDate.of(2022, 6, 22);
  private static final LocalDate END_DATE = LocalDate.of(2022, 9, 22);
  private static final LocalDate PAYMENT_DATE = END_DATE.plusMonths(1);
  private static final double NOTIONAL = 1_000_000.0d;
  private static final double STRIKE = 0.0155;
  private static final double ACCRUAL_FACTOR = 0.30;
  private static final OvernightCompoundedRateComputation RATE_COMP =
      OvernightCompoundedRateComputation.of(EUR_ESTR, START_DATE, END_DATE, REF_DATA);
  private static final OvernightIndexObservation ON_OBS =
      OvernightIndexObservation.of(EUR_ESTR, START_DATE, REF_DATA);

  private static final OvernightInArrearsCapletFloorletPeriod CAPLET_LONG =
      OvernightInArrearsCapletFloorletPeriod.builder()
          .caplet(STRIKE)
          .startDate(START_DATE)
          .endDate(END_DATE)
          .paymentDate(PAYMENT_DATE)
          .yearFraction(ACCRUAL_FACTOR)
          .notional(NOTIONAL)
          .overnightRate(RATE_COMP)
          .build();
  private static final OvernightInArrearsCapletFloorletPeriod CAPLET_SHORT =
      OvernightInArrearsCapletFloorletPeriod.builder()
          .caplet(STRIKE)
          .startDate(START_DATE)
          .endDate(END_DATE)
          .paymentDate(PAYMENT_DATE)
          .yearFraction(ACCRUAL_FACTOR)
          .notional(-NOTIONAL)
          .overnightRate(RATE_COMP)
          .build();
  private static final OvernightInArrearsCapletFloorletPeriod FLOORLET_LONG =
      OvernightInArrearsCapletFloorletPeriod.builder()
          .floorlet(STRIKE)
          .startDate(START_DATE)
          .endDate(END_DATE)
          .paymentDate(PAYMENT_DATE)
          .yearFraction(ACCRUAL_FACTOR)
          .notional(NOTIONAL)
          .overnightRate(RATE_COMP)
          .build();
  private static final OvernightInArrearsCapletFloorletPeriod FLOORLET_SHORT =
      OvernightInArrearsCapletFloorletPeriod.builder()
          .floorlet(STRIKE)
          .startDate(START_DATE)
          .endDate(END_DATE)
          .paymentDate(PAYMENT_DATE)
          .yearFraction(ACCRUAL_FACTOR)
          .notional(-NOTIONAL)
          .overnightRate(RATE_COMP)
          .build();

  private static final RateAccrualPeriod ON_PERIOD = RateAccrualPeriod.builder()
      .startDate(CAPLET_LONG.getStartDate())
      .endDate(CAPLET_LONG.getEndDate())
      .yearFraction(CAPLET_LONG.getYearFraction())
      .rateComputation(RATE_COMP)
      .build();
  private static final RatePaymentPeriod ON_COUPON = RatePaymentPeriod.builder()
      .accrualPeriods(ON_PERIOD)
      .paymentDate(CAPLET_LONG.getPaymentDate())
      .dayCount(EUR_ESTRTERM_3M.getDayCount())
      .notional(NOTIONAL)
      .currency(EUR)
      .build();

  private static final RateAccrualPeriod FIXED_PERIOD = RateAccrualPeriod.builder()
      .startDate(CAPLET_LONG.getStartDate())
      .endDate(CAPLET_LONG.getEndDate())
      .rateComputation(FixedRateComputation.of(STRIKE))
      .yearFraction(CAPLET_LONG.getYearFraction())
      .build();
  private static final RatePaymentPeriod FIXED_COUPON = RatePaymentPeriod.builder()
      .accrualPeriods(FIXED_PERIOD)
      .paymentDate(CAPLET_LONG.getPaymentDate())
      .dayCount(EUR_ESTRTERM_3M.getDayCount())
      .notional(NOTIONAL)
      .currency(EUR)
      .build();

  // valuation date before start date
  private static final ImmutableRatesProvider RATES =
      IborCapletFloorletDataSet.createRatesProvider(VALUATION.toLocalDate());
  private static final BlackIborCapletFloorletExpiryStrikeVolatilities VOLS = IborCapletFloorletDataSet
      .createBlackVolatilities(VALUATION, EUR_ESTRTERM_3M);
  private static final BlackIborCapletFloorletExpiryStrikeVolatilities VOLS_FLAT = IborCapletFloorletDataSet
      .createBlackVolatilitiesFlat(VALUATION, EUR_ESTRTERM_3M);
  private static final ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities VOLS_SHIFTED = IborCapletFloorletDataSet
      .createShiftedBlackVolatilities(VALUATION, EUR_ESTRTERM_3M);
  private static final ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities VOLS_SHIFTED_AFTER_START =
      IborCapletFloorletDataSet
          .createShiftedBlackVolatilities(VALUATION_AFTER_START, EUR_ESTRTERM_3M);
  // valuation date after start date
  private static final LocalDateDoubleTimeSeries TS_ESTR;
  static {
    LocalDateDoubleTimeSeriesBuilder builder = LocalDateDoubleTimeSeries.builder();
    LocalDate currentDate = START_DATE;
    while (currentDate.isBefore(VALUATION_AFTER_START.toLocalDate())) {
      builder.put(currentDate, 0.0010);
      currentDate = EUTA_IMPL.next(currentDate);
    }
    TS_ESTR = builder.build();
  }
  private static final ImmutableRatesProvider RATES_AFTER_START =
      IborCapletFloorletDataSet.createRatesProvider(VALUATION_AFTER_START.toLocalDate())
          .toBuilder().timeSeries(EUR_ESTR, TS_ESTR).build();
  private static final BlackIborCapletFloorletExpiryStrikeVolatilities VOLS_AFTER_START = IborCapletFloorletDataSet
      .createBlackVolatilities(VALUATION_AFTER_START, EUR_ESTRTERM_3M);
  private static final BlackIborCapletFloorletExpiryStrikeVolatilities VOLS_FLAT_AFTER_START = IborCapletFloorletDataSet
      .createBlackVolatilitiesFlat(VALUATION_AFTER_START, EUR_ESTRTERM_3M);
  // valuation date after end date
  private static final LocalDateDoubleTimeSeries TS_ESTR_AFTER_END;
  static {
    LocalDateDoubleTimeSeriesBuilder builder = LocalDateDoubleTimeSeries.builder();
    LocalDate currentDate = START_DATE;
    while (currentDate.isBefore(VALUATION_AFTER_END.toLocalDate())) {
      builder.put(currentDate, 0.0100);
      currentDate = EUTA_IMPL.next(currentDate);
    }
    TS_ESTR_AFTER_END = builder.build();
  }
  private static final ImmutableRatesProvider RATES_AFTER_END =
      IborCapletFloorletDataSet.createRatesProvider(VALUATION_AFTER_END.toLocalDate())
          .toBuilder().timeSeries(EUR_ESTR, TS_ESTR_AFTER_END).build();
  private static final BlackIborCapletFloorletExpiryStrikeVolatilities VOLS_AFTER_END =
      IborCapletFloorletDataSet.createBlackVolatilities(VALUATION_AFTER_END, EUR_ESTRTERM_3M);
  // valuation date after payment date
  private static final ImmutableRatesProvider RATES_AFTER_PAY =
      IborCapletFloorletDataSet.createRatesProvider(VALUATION_AFTER_PAY.toLocalDate());
  private static final BlackIborCapletFloorletExpiryStrikeVolatilities VOLS_AFTER_PAY =
      IborCapletFloorletDataSet.createBlackVolatilities(VALUATION_AFTER_PAY, EUR_ESTRTERM_3M);

  private static final double TOL = 1.0e-14;
  private static final double EPS_FD = 1.0e-6;
  private static final Double TOLERANCE_DELTA = 5.0E+1; // 0.005 / bps on 1m
  private static final VolatilityOvernightInArrearsCapletFloorletPeriodPricer PRICER_BASE =
      VolatilityOvernightInArrearsCapletFloorletPeriodPricer.DEFAULT;
  private static final DiscountingRatePaymentPeriodPricer PRICER_COUPON =
      DiscountingRatePaymentPeriodPricer.DEFAULT;
  private static final VolatilityIborCapletFloorletPeriodPricer PRICER_IBOR =
      VolatilityIborCapletFloorletPeriodPricer.DEFAULT;
  private static final ForwardOvernightCompoundedRateComputationFn ON_FUNCT =
      ForwardOvernightCompoundedRateComputationFn.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);

  //-------------------------------------------------------------------------
  @Test
  public void presentValue_beforestart_formula() {
    CurrencyAmount computedCaplet = PRICER_BASE.presentValue(CAPLET_LONG, RATES, VOLS);
    CurrencyAmount computedFloorlet = PRICER_BASE.presentValue(FLOORLET_SHORT, RATES, VOLS);
    double forward = RATES.overnightIndexRates(EUR_ESTR).periodRate(ON_OBS, END_DATE);
    double startTime = VOLS.relativeTime(START_DATE.atStartOfDay(ZoneOffset.UTC));
    double endTime = VOLS.relativeTime(END_DATE.atStartOfDay(ZoneOffset.UTC));
    double volatility = VOLS.volatility(endTime, STRIKE, forward);
    double volatilityAdjusted = volatility * Math.sqrt((1.0d + 2 * startTime / endTime) / 3.0d);
    double df = RATES.discountFactor(EUR, CAPLET_LONG.getPaymentDate());
    double expectedCaplet = NOTIONAL * df * CAPLET_LONG.getYearFraction() *
        BlackFormulaRepository.price(forward, STRIKE, endTime, volatilityAdjusted, true);
    double expectedFloorlet = -NOTIONAL * df * FLOORLET_SHORT.getYearFraction() *
        BlackFormulaRepository.price(forward, STRIKE, endTime, volatilityAdjusted, false);
    assertThat(computedCaplet.getCurrency()).isEqualTo(EUR);
    assertThat(computedCaplet.getAmount()).isCloseTo(expectedCaplet, offset(NOTIONAL * TOL));
    assertThat(computedFloorlet.getCurrency()).isEqualTo(EUR);
    assertThat(computedFloorlet.getAmount()).isCloseTo(expectedFloorlet, offset(NOTIONAL * TOL));
  }

  @Test
  public void presentValue_beforestart_parity() {
    double capletLong = PRICER_BASE.presentValue(CAPLET_LONG, RATES, VOLS).getAmount();
    double capletShort = PRICER_BASE.presentValue(CAPLET_SHORT, RATES, VOLS).getAmount();
    double floorletLong = PRICER_BASE.presentValue(FLOORLET_LONG, RATES, VOLS).getAmount();
    double floorletShort = PRICER_BASE.presentValue(FLOORLET_SHORT, RATES, VOLS).getAmount();
    double iborCoupon = PRICER_COUPON.presentValue(ON_COUPON, RATES);
    double fixedCoupon = PRICER_COUPON.presentValue(FIXED_COUPON, RATES);
    assertThat(capletLong).isCloseTo(-capletShort, offset(NOTIONAL * TOL));
    assertThat(floorletLong).isCloseTo(-floorletShort, offset(NOTIONAL * TOL));
    assertThat(capletLong - floorletLong).isCloseTo(iborCoupon - fixedCoupon, offset(NOTIONAL * TOL));
    assertThat(capletShort - floorletShort).isCloseTo(-iborCoupon + fixedCoupon, offset(NOTIONAL * TOL));
  }

  @Test
  public void presentValue_afterstart_formula() {
    CurrencyAmount computedCaplet =
        PRICER_BASE.presentValue(CAPLET_LONG, RATES_AFTER_START, VOLS_AFTER_START);
    CurrencyAmount computedFloorlet =
        PRICER_BASE.presentValue(FLOORLET_SHORT, RATES_AFTER_START, VOLS_AFTER_START);
    double forward = ON_FUNCT.rate(RATE_COMP, START_DATE, END_DATE, RATES_AFTER_START);
    double startTime = VOLS_AFTER_START.relativeTime(START_DATE.atStartOfDay(ZoneOffset.UTC));
    double endTime = VOLS_AFTER_START.relativeTime(END_DATE.atStartOfDay(ZoneOffset.UTC));
    double volatility = VOLS_AFTER_START.volatility(endTime, STRIKE, forward);
    double volatilityAdjusted = volatility * endTime / (endTime - startTime) / Math.sqrt(3.0d);
    double df = RATES_AFTER_START.discountFactor(EUR, CAPLET_LONG.getPaymentDate());
    double expectedCaplet = NOTIONAL * df * CAPLET_LONG.getYearFraction() *
        BlackFormulaRepository.price(forward, STRIKE, endTime, volatilityAdjusted, true);
    double expectedFloorlet = -NOTIONAL * df * FLOORLET_SHORT.getYearFraction() *
        BlackFormulaRepository.price(forward, STRIKE, endTime, volatilityAdjusted, false);
    assertThat(computedCaplet.getCurrency()).isEqualTo(EUR);
    assertThat(computedCaplet.getAmount()).isCloseTo(expectedCaplet, offset(NOTIONAL * TOL));
    assertThat(computedFloorlet.getCurrency()).isEqualTo(EUR);
    assertThat(computedFloorlet.getAmount()).isCloseTo(expectedFloorlet, offset(NOTIONAL * TOL));
  }

  @Test
  public void presentValue_afterend_formula() {
    OvernightInArrearsCapletFloorletPeriod capletLongItm = CAPLET_LONG.toBuilder()
        .caplet(0.0050).build();
    CurrencyAmount computedCaplet =
        PRICER_BASE.presentValue(capletLongItm, RATES_AFTER_END, VOLS_AFTER_END);
    CurrencyAmount computedFloorlet =
        PRICER_BASE.presentValue(FLOORLET_SHORT, RATES_AFTER_END, VOLS_AFTER_END);
    double forward = ON_FUNCT.rate(RATE_COMP, START_DATE, END_DATE, RATES_AFTER_END);
    CurrencyAmount payoffCapletComputed = capletLongItm.payoff(forward);
    CurrencyAmount payoffFloorletComputed = FLOORLET_SHORT.payoff(forward);
    double dfPayment = RATES_AFTER_END.discountFactor(EUR, PAYMENT_DATE);
    assertThat(computedCaplet.getCurrency()).isEqualTo(EUR);
    assertThat(computedCaplet.getAmount())
        .isCloseTo(payoffCapletComputed.getAmount() * dfPayment, offset(NOTIONAL * TOL));
    assertThat(computedFloorlet.getCurrency()).isEqualTo(EUR);
    assertThat(computedFloorlet.getAmount())
        .isCloseTo(payoffFloorletComputed.getAmount() * dfPayment, offset(NOTIONAL * TOL));
  }

  @Test
  public void presentValue_afterpay_formula() {
    CurrencyAmount computedCaplet =
        PRICER_BASE.presentValue(CAPLET_LONG, RATES_AFTER_PAY, VOLS_AFTER_PAY);
    CurrencyAmount computedFloorlet =
        PRICER_BASE.presentValue(FLOORLET_SHORT, RATES_AFTER_PAY, VOLS_AFTER_PAY);
    assertThat(computedCaplet.getCurrency()).isEqualTo(EUR);
    assertThat(computedCaplet.getAmount()).isCloseTo(0.0d, offset(NOTIONAL * TOL));
    assertThat(computedFloorlet.getCurrency()).isEqualTo(EUR);
    assertThat(computedFloorlet.getAmount()).isCloseTo(0.0d, offset(NOTIONAL * TOL));
  }

  /* The present value of the in-arrears option is higher than the in-advance. */
  @Test
  public void presentValue_higher_than_European() {
    ZeroRateDiscountFactors onDf =
        (ZeroRateDiscountFactors) ((DiscountOvernightIndexRates) RATES.overnightIndexRates(EUR_ESTR))
            .getDiscountFactors();
    ImmutableRatesProvider ratesOn = RATES.toBuilder()
        .iborIndexCurve(EUR_ESTRTERM_3M, onDf.getCurve()).build(); // Change IBOR curve to ON for comparison
    IborRateComputation iborComp = IborRateComputation.of(EUR_ESTRTERM_3M, LocalDate.of(2022, 6, 20), REF_DATA);
    IborCapletFloorletPeriod iborCap = IborCapletFloorletPeriod.builder()
        .caplet(STRIKE)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .paymentDate(PAYMENT_DATE)
        .yearFraction(ACCRUAL_FACTOR)
        .notional(NOTIONAL)
        .iborRate(iborComp)
        .build();
    CurrencyAmount inAdvancePv = PRICER_IBOR.presentValue(iborCap, ratesOn, VOLS);
    CurrencyAmount inArrearsPv = PRICER_BASE.presentValue(CAPLET_LONG, ratesOn, VOLS);
    assertThat(inAdvancePv.getAmount() < inArrearsPv.getAmount()).isTrue();
  }

  /* The present value of the in-arrears option and the in-advance are close for a short composition period. */
  @Test
  public void presentValue_close_inadvance() {
    LocalDate startDate = LocalDate.of(2042, 6, 20);
    LocalDate endDate = LocalDate.of(2042, 6, 27);
    IborIndex eurEstrTerm1W = ImmutableIborIndex.builder()
        .name("EUR-ESTRTERM-1W")
        .currency(EUR)
        .dayCount(DayCounts.ACT_360)
        .fixingCalendar(EUTA)
        .fixingTime(LocalTime.of(11, 0))
        .fixingZone(ZoneId.of("Europe/Brussels"))
        .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, EUTA))
        .effectiveDateOffset(DaysAdjustment.ofBusinessDays(2, EUTA))
        .maturityDateOffset(TenorAdjustment.of(
            Tenor.TENOR_1W,
            PeriodAdditionConventions.NONE,
            BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, EUTA)))
        .build();
    ZeroRateDiscountFactors onDf =
        (ZeroRateDiscountFactors) ((DiscountOvernightIndexRates) RATES.overnightIndexRates(EUR_ESTR))
            .getDiscountFactors();
    ImmutableRatesProvider ratesOn = RATES.toBuilder()
        .iborIndexCurve(eurEstrTerm1W, onDf.getCurve()).build();
    IborRateComputation iborComp = IborRateComputation.of(eurEstrTerm1W, LocalDate.of(2042, 6, 18), REF_DATA);
    IborCapletFloorletPeriod inAdvanceCaplet = IborCapletFloorletPeriod.builder()
        .caplet(STRIKE)
        .startDate(startDate)
        .endDate(endDate)
        .paymentDate(endDate)
        .yearFraction(ACCRUAL_FACTOR)
        .notional(NOTIONAL)
        .iborRate(iborComp)
        .build();
    OvernightCompoundedRateComputation rateComp =
        OvernightCompoundedRateComputation.of(EUR_ESTR, startDate, endDate, REF_DATA);
    OvernightInArrearsCapletFloorletPeriod inArrearsCaplet =
        OvernightInArrearsCapletFloorletPeriod.builder()
            .caplet(STRIKE)
            .startDate(startDate)
            .endDate(endDate)
            .paymentDate(endDate)
            .yearFraction(ACCRUAL_FACTOR)
            .notional(NOTIONAL)
            .overnightRate(rateComp)
            .build();
    CurrencyAmount inAdvancePv = PRICER_IBOR.presentValue(inAdvanceCaplet, ratesOn, VOLS);
    CurrencyAmount inArrearsPv = PRICER_BASE.presentValue(inArrearsCaplet, ratesOn, VOLS);
    assertThat(inAdvancePv.getAmount() < inArrearsPv.getAmount()).isTrue();
    assertThat(inAdvancePv.getAmount()).isCloseTo(inArrearsPv.getAmount(), offset(1.0E+0));
  }

  //-------------------------------------------------------------------------

  @Test
  public void presentValue_before_rate_sensitivity() {
    PointSensitivities ptsCapletLongComputed = PRICER_BASE
        .presentValueSensitivityRatesStickyStrike(CAPLET_LONG, RATES, VOLS_FLAT).build();
    CurrencyParameterSensitivities psCapletLongComputed = RATES.parameterSensitivity(ptsCapletLongComputed);
    CurrencyParameterSensitivities psCapletLongExpected =
        FD_CAL.sensitivity(RATES, p -> PRICER_BASE.presentValue(CAPLET_LONG, p, VOLS_FLAT));
    assertThat(psCapletLongComputed.equalWithTolerance(psCapletLongExpected, TOLERANCE_DELTA)).isTrue();
    PointSensitivities ptsFloorletShortComputed = PRICER_BASE
        .presentValueSensitivityRatesStickyStrike(FLOORLET_SHORT, RATES, VOLS_FLAT).build();
    CurrencyParameterSensitivities psFloorletShortComputed = RATES.parameterSensitivity(ptsFloorletShortComputed);
    CurrencyParameterSensitivities psFloorletShortExpected =
        FD_CAL.sensitivity(RATES, p -> PRICER_BASE.presentValue(FLOORLET_SHORT, p, VOLS_FLAT));
    assertThat(psFloorletShortComputed.equalWithTolerance(psFloorletShortExpected, TOLERANCE_DELTA)).isTrue();
  }

  @Test
  public void presentValue_afterstart_rate_sensitivity() {
    OvernightInArrearsCapletFloorletPeriod capLongAtm = CAPLET_LONG.toBuilder().caplet(0.0060).build();
    PointSensitivities ptsCapletLongComputed = PRICER_BASE
        .presentValueSensitivityRatesStickyStrike(capLongAtm, RATES_AFTER_START, VOLS_FLAT_AFTER_START).build();
    CurrencyParameterSensitivities psCapletLongComputed = RATES_AFTER_START.parameterSensitivity(ptsCapletLongComputed);
    CurrencyParameterSensitivities psCapletLongExpected =
        FD_CAL.sensitivity(RATES_AFTER_START, p -> PRICER_BASE.presentValue(capLongAtm, p, VOLS_FLAT_AFTER_START));
    assertThat(psCapletLongComputed.equalWithTolerance(psCapletLongExpected, TOLERANCE_DELTA)).isTrue();
    PointSensitivities ptsFloorletShortComputed = PRICER_BASE
        .presentValueSensitivityRatesStickyStrike(FLOORLET_SHORT, RATES_AFTER_START, VOLS_FLAT_AFTER_START).build();
    CurrencyParameterSensitivities psFloorletShortComputed =
        RATES_AFTER_START.parameterSensitivity(ptsFloorletShortComputed);
    CurrencyParameterSensitivities psFloorletShortExpected =
        FD_CAL.sensitivity(RATES_AFTER_START, p -> PRICER_BASE.presentValue(FLOORLET_SHORT, p, VOLS_FLAT_AFTER_START));
    assertThat(psFloorletShortComputed.equalWithTolerance(psFloorletShortExpected, TOLERANCE_DELTA)).isTrue();
  }

  @Test
  public void presentValue_afterend_rate_sensitivity() {
    OvernightInArrearsCapletFloorletPeriod capletLongItm = CAPLET_LONG.toBuilder()
        .caplet(0.0050).build();
    PointSensitivities ptsCapletLongComputed = PRICER_BASE
        .presentValueSensitivityRatesStickyStrike(capletLongItm, RATES_AFTER_END, VOLS_AFTER_END).build();
    CurrencyParameterSensitivities psCapletLongComputed = RATES_AFTER_END.parameterSensitivity(ptsCapletLongComputed);
    CurrencyParameterSensitivities psCapletLongExpected =
        FD_CAL.sensitivity(RATES_AFTER_END, p -> PRICER_BASE.presentValue(capletLongItm, p, VOLS_AFTER_END));
    assertThat(psCapletLongComputed.equalWithTolerance(psCapletLongExpected, TOLERANCE_DELTA)).isTrue();
    PointSensitivities ptsFloorletShortComputed = PRICER_BASE
        .presentValueSensitivityRatesStickyStrike(FLOORLET_SHORT, RATES_AFTER_END, VOLS_AFTER_END).build();
    CurrencyParameterSensitivities psFloorletShortComputed =
        RATES_AFTER_END.parameterSensitivity(ptsFloorletShortComputed);
    CurrencyParameterSensitivities psFloorletShortExpected =
        FD_CAL.sensitivity(RATES_AFTER_END, p -> PRICER_BASE.presentValue(FLOORLET_SHORT, p, VOLS_AFTER_END));
    assertThat(psFloorletShortComputed.equalWithTolerance(psFloorletShortExpected, TOLERANCE_DELTA)).isTrue();
  }

  @Test
  public void presentValue_afterpay_rate_sensitivity() {
    PointSensitivities ptsCapletLongComputed = PRICER_BASE
        .presentValueSensitivityRatesStickyStrike(CAPLET_LONG, RATES_AFTER_PAY, VOLS_AFTER_PAY).build();
    assertThat(ptsCapletLongComputed).isEqualTo(PointSensitivities.empty());
    PointSensitivities ptsFloorletShortComputed = PRICER_BASE
        .presentValueSensitivityRatesStickyStrike(FLOORLET_SHORT, RATES_AFTER_PAY, VOLS_AFTER_PAY).build();
    assertThat(ptsFloorletShortComputed).isEqualTo(PointSensitivities.empty());
  }

  //-------------------------------------------------------------------------

  @Test
  public void presentValue_beforestart_SensitivityVolatility() {
    PointSensitivityBuilder pointCaplet =
        PRICER_BASE.presentValueSensitivityModelParamsVolatility(CAPLET_LONG, RATES, VOLS_SHIFTED);
    CurrencyParameterSensitivity computedCaplet =
        VOLS_SHIFTED.parameterSensitivity(pointCaplet.build()).getSensitivities().get(0);
    PointSensitivityBuilder pointFloorlet =
        PRICER_BASE.presentValueSensitivityModelParamsVolatility(FLOORLET_SHORT, RATES, VOLS_SHIFTED);
    CurrencyParameterSensitivity computedFloorlet =
        VOLS_SHIFTED.parameterSensitivity(pointFloorlet.build()).getSensitivities().get(0);
    testSurfaceSensitivity(computedCaplet, VOLS_SHIFTED, v -> PRICER_BASE.presentValue(CAPLET_LONG, RATES, v));
    testSurfaceSensitivity(computedFloorlet, VOLS_SHIFTED, v -> PRICER_BASE.presentValue(FLOORLET_SHORT, RATES, v));
  }

  @Test
  public void presentValue_afterstart_SensitivityVolatility() {
    PointSensitivityBuilder pointCaplet =
        PRICER_BASE.presentValueSensitivityModelParamsVolatility(CAPLET_LONG, RATES_AFTER_START,
            VOLS_SHIFTED_AFTER_START);
    CurrencyParameterSensitivity computedCaplet =
        VOLS_SHIFTED_AFTER_START.parameterSensitivity(pointCaplet.build()).getSensitivities().get(0);
    PointSensitivityBuilder pointFloorlet =
        PRICER_BASE.presentValueSensitivityModelParamsVolatility(FLOORLET_SHORT, RATES_AFTER_START,
            VOLS_SHIFTED_AFTER_START);
    CurrencyParameterSensitivity computedFloorlet =
        VOLS_SHIFTED_AFTER_START.parameterSensitivity(pointFloorlet.build()).getSensitivities().get(0);
    testSurfaceSensitivity(computedCaplet, VOLS_SHIFTED_AFTER_START,
        v -> PRICER_BASE.presentValue(CAPLET_LONG, RATES_AFTER_START, v));
    testSurfaceSensitivity(computedFloorlet, VOLS_SHIFTED_AFTER_START,
        v -> PRICER_BASE.presentValue(FLOORLET_SHORT, RATES_AFTER_START, v));
  }

  @Test
  public void presentValue_afterend_SensitivityVolatility() {
    PointSensitivities ptsCapletLongComputed =
        PRICER_BASE.presentValueSensitivityModelParamsVolatility(CAPLET_LONG, RATES_AFTER_END, VOLS_AFTER_END).build();
    PointSensitivities ptsFloorletShortComputed =
        PRICER_BASE.presentValueSensitivityModelParamsVolatility(FLOORLET_SHORT, RATES_AFTER_END, VOLS_AFTER_END)
            .build();
    assertThat(ptsCapletLongComputed).isEqualTo(PointSensitivities.empty());
    assertThat(ptsFloorletShortComputed).isEqualTo(PointSensitivities.empty());
  }

  @Test
  public void presentValue_afterpay_SensitivityVolatility() {
    PointSensitivities ptsCapletLongComputed =
        PRICER_BASE.presentValueSensitivityModelParamsVolatility(CAPLET_LONG, RATES_AFTER_PAY, VOLS_AFTER_PAY).build();
    PointSensitivities ptsFloorletShortComputed =
        PRICER_BASE.presentValueSensitivityModelParamsVolatility(FLOORLET_SHORT, RATES_AFTER_PAY, VOLS_AFTER_PAY)
            .build();
    assertThat(ptsCapletLongComputed).isEqualTo(PointSensitivities.empty());
    assertThat(ptsFloorletShortComputed).isEqualTo(PointSensitivities.empty());
  }

  private void testSurfaceSensitivity(
      CurrencyParameterSensitivity computed,
      ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities vols,
      Function<IborCapletFloorletVolatilities, CurrencyAmount> valueFn) {

    double pvBase = valueFn.apply(vols).getAmount();
    InterpolatedNodalSurface surfaceBase = (InterpolatedNodalSurface) vols.getSurface();
    int nParams = surfaceBase.getParameterCount();
    for (int i = 0; i < nParams; i++) {
      DoubleArray zBumped = surfaceBase.getZValues().with(i, surfaceBase.getZValues().get(i) + EPS_FD);
      InterpolatedNodalSurface surfaceBumped = surfaceBase.withZValues(zBumped);
      ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities volsBumped =
          ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities.of(
              vols.getIndex(),
              vols.getValuationDateTime(),
              surfaceBumped,
              vols.getShiftCurve());
      double fd = (valueFn.apply(volsBumped).getAmount() - pvBase) / EPS_FD;
      assertThat(computed.getSensitivity().get(i)).isCloseTo(fd, offset(NOTIONAL * EPS_FD));
    }
  }

}
