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
import static com.opengamma.strata.pricer.capfloor.IborCapletFloorletSabrRateVolatilityDataSet.CONST_SHIFT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.assertj.core.data.Offset;
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
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.impl.option.NormalFormulaRepository;
import com.opengamma.strata.pricer.impl.rate.ForwardOvernightCompoundedRateComputationFn;
import com.opengamma.strata.pricer.impl.swap.DiscountingRatePaymentPeriodPricer;
import com.opengamma.strata.pricer.impl.volatility.smile.SabrFormulaData;
import com.opengamma.strata.pricer.impl.volatility.smile.SabrHaganVolatilityFunctionProvider;
import com.opengamma.strata.pricer.impl.volatility.smile.SabrInArrearsVolatilityFunction;
import com.opengamma.strata.pricer.rate.DiscountOvernightIndexRates;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.capfloor.IborCapletFloorletPeriod;
import com.opengamma.strata.product.capfloor.OvernightInArrearsCapletFloorletPeriod;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.rate.IborRateComputation;
import com.opengamma.strata.product.rate.OvernightCompoundedRateComputation;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;

/**
 * Test {@link SabrOvernightInArrearsCapletFloorletPeriodPricer}.
 */
public class SabrOvernightInArrearsCapletFloorletPeriodPricerTest {

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

  // valuation date before start date
  private static final ImmutableRatesProvider RATES = IborCapletFloorletSabrRateVolatilityDataSet.getRatesProvider(
      VALUATION.toLocalDate(), EUR_ESTRTERM_3M, LocalDateDoubleTimeSeries.empty());
  private static final SabrParametersIborCapletFloorletVolatilities VOLS = IborCapletFloorletSabrRateVolatilityDataSet
      .getVolatilities(VALUATION, EUR_ESTRTERM_3M);
  // valuation date after start date
  private static final LocalDateDoubleTimeSeries TS_ESTR_AFTER_START;
  static {
    LocalDateDoubleTimeSeriesBuilder builder = LocalDateDoubleTimeSeries.builder();
    LocalDate currentDate = START_DATE;
    while (currentDate.isBefore(VALUATION_AFTER_START.toLocalDate())) {
      builder.put(currentDate, 0.0100);
      currentDate = EUTA_IMPL.next(currentDate);
    }
    TS_ESTR_AFTER_START = builder.build();
  }
  private static final ImmutableRatesProvider RATES_AFTER_START = IborCapletFloorletSabrRateVolatilityDataSet
      .getRatesProvider(VALUATION_AFTER_START.toLocalDate(), EUR_ESTRTERM_3M, LocalDateDoubleTimeSeries.empty())
      .toBuilder().timeSeries(EUR_ESTR, TS_ESTR_AFTER_START).build();
  private static final SabrParametersIborCapletFloorletVolatilities VOLS_AFTER_START =
      IborCapletFloorletSabrRateVolatilityDataSet
          .getVolatilities(VALUATION_AFTER_START, EUR_ESTRTERM_3M);
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
  private static final ImmutableRatesProvider RATES_AFTER_END = IborCapletFloorletSabrRateVolatilityDataSet
      .getRatesProvider(VALUATION_AFTER_END.toLocalDate(), EUR_ESTRTERM_3M, LocalDateDoubleTimeSeries.empty())
      .toBuilder().timeSeries(EUR_ESTR, TS_ESTR_AFTER_END).build();
  private static final SabrParametersIborCapletFloorletVolatilities VOLS_AFTER_END =
      IborCapletFloorletSabrRateVolatilityDataSet
          .getVolatilities(VALUATION_AFTER_END, EUR_ESTRTERM_3M);
  // valuation date after payment date
  private static final ImmutableRatesProvider RATES_AFTER_PAY =
      IborCapletFloorletSabrRateVolatilityDataSet.getRatesProvider(
          VALUATION_AFTER_PAY.toLocalDate(), EUR_ESTRTERM_3M, LocalDateDoubleTimeSeries.empty());
  private static final SabrParametersIborCapletFloorletVolatilities VOLS_AFTER_PAY =
      IborCapletFloorletSabrRateVolatilityDataSet
          .getVolatilities(VALUATION_AFTER_PAY, EUR_ESTRTERM_3M);

  private static final SabrIborCapletFloorletPeriodPricer PRICER_IBOR = SabrIborCapletFloorletPeriodPricer.DEFAULT;
  private static final SabrOvernightInArrearsCapletFloorletPeriodPricer PRICER_ON_INARREARS_Q1 =
      SabrOvernightInArrearsCapletFloorletPeriodPricer.DEFAULT;
  private static final double Q_OTHER = 1.1;
  private static final SabrInArrearsVolatilityFunction FUNCTION_OTHER =
      SabrInArrearsVolatilityFunction.of(Q_OTHER);
  private static final SabrOvernightInArrearsCapletFloorletPeriodPricer PRICER_ON_INARREARS_QOTHER =
      SabrOvernightInArrearsCapletFloorletPeriodPricer.of(FUNCTION_OTHER);
  private static final SabrHaganVolatilityFunctionProvider SABR_FORMULA =
      SabrHaganVolatilityFunctionProvider.DEFAULT;
  private static final double EPS_FD = 1.0e-6;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);
  private static final DiscountingRatePaymentPeriodPricer PRICER_PERIOD =
      DiscountingRatePaymentPeriodPricer.DEFAULT;
  private static final ForwardOvernightCompoundedRateComputationFn ON_FUNCT =
      ForwardOvernightCompoundedRateComputationFn.DEFAULT;

  private static final Offset<Double> TOLERANCE_SMALL_IV = Offset.offset(1.0E-4);
  private static final Offset<Double> TOLERANCE_PV = Offset.offset(1.0E-4);
  private static final Offset<Double> TOLERANCE_VEGA = Offset.offset(1.0E-2);
  private static final Double TOLERANCE_DELTA = 2.0E+1; // 0.002 / bps on 1m

  /* The present value of the in-arrears option is higher than the in-advance. */
  @Test
  public void presentValue_higher() {
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
    CurrencyAmount inArrearsPv = PRICER_ON_INARREARS_Q1.presentValue(CAPLET_LONG, ratesOn, VOLS);
    assertThat(inAdvancePv.getAmount() < inArrearsPv.getAmount()).isTrue();
  }

  /* The implied volatility of the in-arrears option converges to the in-advance when the maturity tends to infinity. */
  @Test
  public void impliedvol_converge() {
    ZeroRateDiscountFactors onDf =
        (ZeroRateDiscountFactors) ((DiscountOvernightIndexRates) RATES.overnightIndexRates(EUR_ESTR))
            .getDiscountFactors();
    ImmutableRatesProvider ratesOn = RATES.toBuilder()
        .iborIndexCurve(EUR_ESTRTERM_3M, onDf.getCurve()).build(); // Change IBOR curve to ON for comparison
    for (int i = 50; i <= 100; i += 10) {
      LocalDate fixingDate = EUTA_IMPL.nextOrSame(START_DATE.plusYears(i));
      IborRateComputation iborComp = IborRateComputation.of(EUR_ESTRTERM_3M, fixingDate, REF_DATA);
      LocalDate startDate = iborComp.getEffectiveDate();
      LocalDate endDate = iborComp.getMaturityDate();
      OvernightCompoundedRateComputation onComp =
          OvernightCompoundedRateComputation.of(EUR_ESTR, startDate, endDate, REF_DATA);
      OvernightIndexObservation onObs =
          OvernightIndexObservation.of(EUR_ESTR, startDate, REF_DATA);
      OvernightInArrearsCapletFloorletPeriod inArrearsCap = OvernightInArrearsCapletFloorletPeriod.builder()
          .caplet(STRIKE)
          .startDate(startDate)
          .endDate(endDate)
          .yearFraction(ACCRUAL_FACTOR)
          .notional(NOTIONAL)
          .overnightRate(onComp)
          .build();
      IborCapletFloorletPeriod iborCap = IborCapletFloorletPeriod.builder()
          .caplet(STRIKE)
          .startDate(startDate)
          .endDate(endDate)
          .yearFraction(ACCRUAL_FACTOR)
          .notional(NOTIONAL)
          .iborRate(iborComp)
          .build();
      double forward = ratesOn.overnightIndexRates(EUR_ESTR).periodRate(onObs, endDate);
      double num = NOTIONAL * ACCRUAL_FACTOR * onDf.discountFactor(endDate);
      double timeToExpiry = VOLS.relativeTime(fixingDate.atStartOfDay(ZoneId.of("Europe/Brussels")));
      CurrencyAmount inArrearsPv = PRICER_ON_INARREARS_Q1.presentValue(inArrearsCap, ratesOn, VOLS);
      double inArrearsIv = NormalFormulaRepository
          .impliedVolatility(inArrearsPv.getAmount(), forward, STRIKE, timeToExpiry, 0.01, num, PutCall.CALL);
      CurrencyAmount inAdvancePv = PRICER_IBOR.presentValue(iborCap, ratesOn, VOLS);
      double inAdvanceIv = NormalFormulaRepository
          .impliedVolatility(inAdvancePv.getAmount(), forward, STRIKE, timeToExpiry, 0.01, num, PutCall.CALL);
      assertThat(inArrearsIv).isEqualTo(inAdvanceIv, TOLERANCE_SMALL_IV);
    }
  }

  /* The value of the in-arrears option converges to the in-advance when q tends to infinity. */
  @Test
  public void presentvalue_converge_pinfinity() {
    ZeroRateDiscountFactors onDf =
        (ZeroRateDiscountFactors) ((DiscountOvernightIndexRates) RATES.overnightIndexRates(EUR_ESTR))
            .getDiscountFactors();
    ImmutableRatesProvider ratesOn = RATES.toBuilder()
        .iborIndexCurve(EUR_ESTRTERM_3M, onDf.getCurve()).build(); // Change IBOR curve to ON for comparison
    LocalDate fixingDate = LocalDate.of(2022, 6, 20);
    IborRateComputation iborComp = IborRateComputation.of(EUR_ESTRTERM_3M, fixingDate, REF_DATA);
    IborCapletFloorletPeriod iborCap = IborCapletFloorletPeriod.builder()
        .caplet(STRIKE)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .paymentDate(PAYMENT_DATE)
        .yearFraction(ACCRUAL_FACTOR)
        .notional(NOTIONAL)
        .iborRate(iborComp)
        .build();
    OvernightIndexObservation onObs =
        OvernightIndexObservation.of(EUR_ESTR, START_DATE, REF_DATA);
    double forward = ratesOn.overnightIndexRates(EUR_ESTR).periodRate(onObs, END_DATE);
    double num = NOTIONAL * ACCRUAL_FACTOR * onDf.discountFactor(END_DATE);
    double timeToExpiry = VOLS.relativeTime(fixingDate.atStartOfDay(ZoneId.of("Europe/Brussels")));
    for (int i = 0; i < 10; i++) {
      double q = 100 + 10 * i; // Exaggerated q value to see the convergence
      SabrInArrearsVolatilityFunction functionQ = SabrInArrearsVolatilityFunction.of(q);
      SabrOvernightInArrearsCapletFloorletPeriodPricer pricerQ =
          SabrOvernightInArrearsCapletFloorletPeriodPricer.of(functionQ);
      double inAdvancePv = PRICER_IBOR.presentValue(iborCap, ratesOn, VOLS).getAmount();
      double inArrearsPv = pricerQ.presentValue(CAPLET_LONG, ratesOn, VOLS).getAmount();
      double inAdvanceIv = NormalFormulaRepository
          .impliedVolatility(inAdvancePv, forward, STRIKE, timeToExpiry, 0.01, num, PutCall.CALL);
      double inArrearsIv = NormalFormulaRepository
          .impliedVolatility(inArrearsPv, forward, STRIKE, timeToExpiry, 0.01, num, PutCall.CALL);
      assertThat(inArrearsIv).isEqualTo(inAdvanceIv, TOLERANCE_SMALL_IV);
    }
  }

  @Test
  public void presentValue_before_parity() {
    CurrencyAmount pvCapLongComputed = PRICER_ON_INARREARS_QOTHER.presentValue(CAPLET_LONG, RATES, VOLS);
    CurrencyAmount pvFloorShortComputed = PRICER_ON_INARREARS_QOTHER.presentValue(FLOORLET_SHORT, RATES, VOLS);
    CurrencyAmount pvCapShortComputed = PRICER_ON_INARREARS_QOTHER.presentValue(CAPLET_SHORT, RATES, VOLS);
    CurrencyAmount pvFloorLongComputed = PRICER_ON_INARREARS_QOTHER.presentValue(FLOORLET_LONG, RATES, VOLS);
    RateAccrualPeriod onAccrual = RateAccrualPeriod.builder()
        .rateComputation(RATE_COMP)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .yearFraction(ACCRUAL_FACTOR).build();
    RatePaymentPeriod onPeriod = RatePaymentPeriod.builder()
        .paymentDate(PAYMENT_DATE)
        .accrualPeriods(onAccrual)
        .dayCount(DayCounts.ACT_360)
        .currency(EUR)
        .notional(NOTIONAL).build();
    RateAccrualPeriod fixedAccrual = RateAccrualPeriod.builder()
        .rateComputation(FixedRateComputation.of(STRIKE))
        .startDate(START_DATE)
        .endDate(END_DATE)
        .yearFraction(ACCRUAL_FACTOR).build();
    RatePaymentPeriod fixedPeriod = RatePaymentPeriod.builder()
        .paymentDate(PAYMENT_DATE)
        .accrualPeriods(fixedAccrual)
        .dayCount(DayCounts.ACT_360)
        .currency(EUR)
        .notional(NOTIONAL).build();
    double pvOnPeriod = PRICER_PERIOD.presentValue(onPeriod, RATES);
    double pvFixedPeriod = PRICER_PERIOD.presentValue(fixedPeriod, RATES);
    assertThat(pvCapLongComputed.getAmount() + pvFloorShortComputed.getAmount() + pvFixedPeriod)
        .isEqualTo(pvOnPeriod, TOLERANCE_SMALL_IV);
    assertThat(pvCapShortComputed.getAmount() + pvFloorLongComputed.getAmount() - pvFixedPeriod)
        .isEqualTo(-pvOnPeriod, TOLERANCE_SMALL_IV);
  }

  @Test
  public void presentValue_beforestart_formula() {
    CurrencyAmount pvLongComputed = PRICER_ON_INARREARS_QOTHER.presentValue(CAPLET_LONG, RATES, VOLS);
    OvernightIndexObservation onObs = OvernightIndexObservation.of(EUR_ESTR, START_DATE, REF_DATA);
    double startTime = VOLS.relativeTime(START_DATE.atStartOfDay(ZoneId.of("Europe/Brussels")));
    double endTime = VOLS.relativeTime(END_DATE.atStartOfDay(ZoneId.of("Europe/Brussels")));
    double alpha = VOLS.alpha(startTime);
    double beta = VOLS.beta(startTime);
    double rho = VOLS.rho(startTime);
    double nu = VOLS.nu(startTime);
    double shift = VOLS.shift(startTime);
    SabrFormulaData sabr = SabrFormulaData.of(alpha, beta, rho, nu);
    SabrFormulaData sabrEffective = FUNCTION_OTHER.effectiveSabr(sabr, startTime, endTime);
    double forward = RATES.overnightIndexRates(EUR_ESTR).periodRate(onObs, END_DATE);
    double dfPayment = RATES.discountFactor(EUR, PAYMENT_DATE);
    double volatility = SABR_FORMULA.volatility(forward + shift, STRIKE + shift, endTime, sabrEffective);
    double pvExpected = dfPayment * ACCRUAL_FACTOR * NOTIONAL *
        BlackFormulaRepository.price(forward + shift, STRIKE + shift, endTime, volatility, true);
    assertThat(pvExpected).isEqualTo(pvLongComputed.getAmount(), TOLERANCE_PV);
    CurrencyAmount pvShortComputed = PRICER_ON_INARREARS_QOTHER.presentValue(CAPLET_SHORT, RATES, VOLS);
    assertThat(pvShortComputed.getAmount()).isEqualTo(-pvLongComputed.getAmount(), TOLERANCE_PV);
  }

  @Test
  public void presentValue_afterstart_formula() {
    double strike = 0.0115;
    OvernightInArrearsCapletFloorletPeriod capletLongAtm = CAPLET_LONG.toBuilder()
        .caplet(strike).build();
    CurrencyAmount pvLongComputed =
        PRICER_ON_INARREARS_QOTHER.presentValue(capletLongAtm, RATES_AFTER_START, VOLS_AFTER_START);
    double startTime = VOLS_AFTER_START.relativeTime(START_DATE.atStartOfDay(ZoneId.of("Europe/Brussels")));
    double endTime = VOLS_AFTER_START.relativeTime(END_DATE.atStartOfDay(ZoneId.of("Europe/Brussels")));
    double alpha = VOLS_AFTER_START.alpha(startTime);
    double beta = VOLS_AFTER_START.beta(startTime);
    double rho = VOLS_AFTER_START.rho(startTime);
    double nu = VOLS_AFTER_START.nu(startTime);
    double shift = VOLS_AFTER_START.shift(startTime);
    SabrFormulaData sabr = SabrFormulaData.of(alpha, beta, rho, nu);
    SabrFormulaData sabrEffective = FUNCTION_OTHER.effectiveSabr(sabr, startTime, endTime);
    double forward = ON_FUNCT.rate(RATE_COMP, PAYMENT_DATE, END_DATE, RATES_AFTER_START);
    double dfPayment = RATES_AFTER_START.discountFactor(EUR, PAYMENT_DATE);
    double volatility = SABR_FORMULA.volatility(forward + shift, strike + shift, endTime, sabrEffective);
    double pvExpected = dfPayment * ACCRUAL_FACTOR * NOTIONAL *
        BlackFormulaRepository.price(forward + shift, strike + shift, endTime, volatility, true);
    assertThat(pvExpected).isEqualTo(pvLongComputed.getAmount(), TOLERANCE_PV);
    CurrencyAmount pvShortComputed = PRICER_ON_INARREARS_QOTHER
        .presentValue(CAPLET_SHORT.toBuilder().caplet(strike).build(), RATES_AFTER_START, VOLS_AFTER_START);
    assertThat(pvShortComputed.getAmount()).isEqualTo(-pvLongComputed.getAmount(), TOLERANCE_PV);
  }

  @Test
  public void presentValue_afterend_formula() {
    OvernightInArrearsCapletFloorletPeriod capletLongItm = CAPLET_LONG.toBuilder()
        .caplet(0.0050).build();
    CurrencyAmount computedCaplet =
        PRICER_ON_INARREARS_QOTHER.presentValue(capletLongItm, RATES_AFTER_END, VOLS_AFTER_END);
    CurrencyAmount computedFloorlet =
        PRICER_ON_INARREARS_QOTHER.presentValue(FLOORLET_SHORT, RATES_AFTER_END, VOLS_AFTER_END);
    double forward = ON_FUNCT.rate(RATE_COMP, START_DATE, END_DATE, RATES_AFTER_END);
    CurrencyAmount payoffCapletComputed = capletLongItm.payoff(forward);
    CurrencyAmount payoffFloorletComputed = FLOORLET_SHORT.payoff(forward);
    double dfPayment = RATES_AFTER_END.discountFactor(EUR, PAYMENT_DATE);
    assertThat(computedCaplet.getCurrency()).isEqualTo(EUR);
    assertThat(computedCaplet.getAmount())
        .isCloseTo(payoffCapletComputed.getAmount() * dfPayment, TOLERANCE_PV);
    assertThat(computedFloorlet.getCurrency()).isEqualTo(EUR);
    assertThat(computedFloorlet.getAmount())
        .isCloseTo(payoffFloorletComputed.getAmount() * dfPayment, TOLERANCE_PV);
  }

  @Test
  public void presentValue_afterpay_formula() {
    CurrencyAmount computedCaplet =
        PRICER_ON_INARREARS_QOTHER.presentValue(CAPLET_LONG, RATES_AFTER_PAY, VOLS_AFTER_PAY);
    CurrencyAmount computedFloorlet =
        PRICER_ON_INARREARS_QOTHER.presentValue(FLOORLET_SHORT, RATES_AFTER_PAY, VOLS_AFTER_PAY);
    assertThat(computedCaplet.getCurrency()).isEqualTo(EUR);
    assertThat(computedCaplet.getAmount()).isCloseTo(0.0d, TOLERANCE_PV);
    assertThat(computedFloorlet.getCurrency()).isEqualTo(EUR);
    assertThat(computedFloorlet.getAmount()).isCloseTo(0.0d, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------

  @Test
  public void presentValue_beforestart_rate_sensitivity() {
    PointSensitivities ptsCapletLongComputed = PRICER_ON_INARREARS_QOTHER
        .presentValueSensitivityRatesStickyModel(CAPLET_LONG, RATES, VOLS).build();
    CurrencyParameterSensitivities psCapletLongComputed = RATES.parameterSensitivity(ptsCapletLongComputed);
    CurrencyParameterSensitivities psCapletLongExpected =
        FD_CAL.sensitivity(RATES, p -> PRICER_ON_INARREARS_QOTHER.presentValue(CAPLET_LONG, p, VOLS));
    assertThat(psCapletLongComputed.equalWithTolerance(psCapletLongExpected, TOLERANCE_DELTA)).isTrue();
    PointSensitivities ptsFloorletShortComputed = PRICER_ON_INARREARS_QOTHER
        .presentValueSensitivityRatesStickyModel(FLOORLET_SHORT, RATES, VOLS).build();
    CurrencyParameterSensitivities psFloorletShortComputed = RATES.parameterSensitivity(ptsFloorletShortComputed);
    CurrencyParameterSensitivities psFloorletShortExpected =
        FD_CAL.sensitivity(RATES, p -> PRICER_ON_INARREARS_QOTHER.presentValue(FLOORLET_SHORT, p, VOLS));
    assertThat(psFloorletShortComputed.equalWithTolerance(psFloorletShortExpected, TOLERANCE_DELTA)).isTrue();
  }

  @Test
  public void presentValue_afterstart_rate_sensitivity() {
    double strike = 0.0115;
    OvernightInArrearsCapletFloorletPeriod capletLongAtm = CAPLET_LONG.toBuilder()
        .caplet(strike).build();
    PointSensitivities ptsCapletLongComputed = PRICER_ON_INARREARS_QOTHER
        .presentValueSensitivityRatesStickyModel(capletLongAtm, RATES_AFTER_START, VOLS_AFTER_START).build();
    CurrencyParameterSensitivities psCapletLongComputed = RATES_AFTER_START.parameterSensitivity(ptsCapletLongComputed);
    CurrencyParameterSensitivities psCapletLongExpected =
        FD_CAL.sensitivity(RATES_AFTER_START,
            p -> PRICER_ON_INARREARS_QOTHER.presentValue(capletLongAtm, p, VOLS_AFTER_START));
    assertThat(psCapletLongComputed.equalWithTolerance(psCapletLongExpected, TOLERANCE_DELTA)).isTrue();
    PointSensitivities ptsFloorletShortComputed = PRICER_ON_INARREARS_QOTHER
        .presentValueSensitivityRatesStickyModel(FLOORLET_SHORT, RATES_AFTER_START, VOLS_AFTER_START).build();
    CurrencyParameterSensitivities psFloorletShortComputed =
        RATES_AFTER_START.parameterSensitivity(ptsFloorletShortComputed);
    CurrencyParameterSensitivities psFloorletShortExpected =
        FD_CAL.sensitivity(RATES_AFTER_START,
            p -> PRICER_ON_INARREARS_QOTHER.presentValue(FLOORLET_SHORT, p, VOLS_AFTER_START));
    assertThat(psFloorletShortComputed.equalWithTolerance(psFloorletShortExpected, TOLERANCE_DELTA)).isTrue();
  }

  @Test
  public void presentValue_afterend_rate_sensitivity() {
    OvernightInArrearsCapletFloorletPeriod capletLongItm = CAPLET_LONG.toBuilder()
        .caplet(0.0050).build();
    PointSensitivities ptsCapletLongComputed = PRICER_ON_INARREARS_QOTHER
        .presentValueSensitivityRatesStickyModel(capletLongItm, RATES_AFTER_END, VOLS_AFTER_END).build();
    CurrencyParameterSensitivities psCapletLongComputed = RATES_AFTER_END.parameterSensitivity(ptsCapletLongComputed);
    CurrencyParameterSensitivities psCapletLongExpected =
        FD_CAL.sensitivity(RATES_AFTER_END,
            p -> PRICER_ON_INARREARS_QOTHER.presentValue(capletLongItm, p, VOLS_AFTER_END));
    assertThat(psCapletLongComputed.equalWithTolerance(psCapletLongExpected, TOLERANCE_DELTA)).isTrue();
    PointSensitivities ptsFloorletShortComputed = PRICER_ON_INARREARS_QOTHER
        .presentValueSensitivityRatesStickyModel(FLOORLET_SHORT, RATES_AFTER_END, VOLS_AFTER_END).build();
    CurrencyParameterSensitivities psFloorletShortComputed =
        RATES_AFTER_END.parameterSensitivity(ptsFloorletShortComputed);
    CurrencyParameterSensitivities psFloorletShortExpected =
        FD_CAL.sensitivity(RATES_AFTER_END,
            p -> PRICER_ON_INARREARS_QOTHER.presentValue(FLOORLET_SHORT, p, VOLS_AFTER_END));
    assertThat(psFloorletShortComputed.equalWithTolerance(psFloorletShortExpected, TOLERANCE_DELTA)).isTrue();
  }

  @Test
  public void presentValue_afterpay_rate_sensitivity() {
    PointSensitivities ptsCapletLongComputed = PRICER_ON_INARREARS_QOTHER
        .presentValueSensitivityRatesStickyModel(CAPLET_LONG, RATES_AFTER_PAY, VOLS_AFTER_PAY).build();
    assertThat(ptsCapletLongComputed).isEqualTo(PointSensitivities.empty());
    PointSensitivities ptsFloorletShortComputed = PRICER_ON_INARREARS_QOTHER
        .presentValueSensitivityRatesStickyModel(FLOORLET_SHORT, RATES_AFTER_PAY, VOLS_AFTER_PAY).build();
    assertThat(ptsFloorletShortComputed).isEqualTo(PointSensitivities.empty());
  }

  //-------------------------------------------------------------------------

  @Test
  public void presentValue_beforestart_param_sensitivity() {
    PointSensitivities ptsCapletLongComputed = PRICER_ON_INARREARS_QOTHER
        .presentValueSensitivityModelParamsSabr(CAPLET_LONG, RATES, VOLS).build();
    CurrencyParameterSensitivities ps = VOLS.parameterSensitivity(ptsCapletLongComputed);
    CurrencyParameterSensitivity psAlpha = ps.getSensitivity(CurveName.of("Test-SABR-Alpha"), EUR);
    CurrencyParameterSensitivity psBeta = ps.getSensitivity(CurveName.of("Test-SABR-Beta"), EUR);
    CurrencyParameterSensitivity psRho = ps.getSensitivity(CurveName.of("Test-SABR-Rho"), EUR);
    CurrencyParameterSensitivity psNu = ps.getSensitivity(CurveName.of("Test-SABR-Nu"), EUR);
    double shift = 1.0E-6;
    double pvStart = PRICER_ON_INARREARS_QOTHER.presentValue(CAPLET_LONG, RATES, VOLS).getAmount();
    for (int i = 0; i < 6; i++) { // size of Alpha curve
      SabrParametersIborCapletFloorletVolatilities volsAlphaShifted = IborCapletFloorletSabrRateVolatilityDataSet
          .getVolatilitiesShiftAlpha(VALUATION, EUR_ESTRTERM_3M, i, shift);
      double pvAlphaShifted = PRICER_ON_INARREARS_QOTHER.presentValue(CAPLET_LONG, RATES, volsAlphaShifted).getAmount();
      double psAlphaExpected = (pvAlphaShifted - pvStart) / shift;
      assertThat(psAlphaExpected).isEqualTo(psAlpha.getSensitivity().get(i), TOLERANCE_VEGA);
      SabrParametersIborCapletFloorletVolatilities volsBetaShifted = IborCapletFloorletSabrRateVolatilityDataSet
          .getVolatilitiesShiftBeta(VALUATION, EUR_ESTRTERM_3M, i, shift);
      double pvBetaShifted = PRICER_ON_INARREARS_QOTHER.presentValue(CAPLET_LONG, RATES, volsBetaShifted).getAmount();
      double psBetaExpected = (pvBetaShifted - pvStart) / shift;
      assertThat(psBetaExpected).isEqualTo(psBeta.getSensitivity().get(i), TOLERANCE_VEGA);
      SabrParametersIborCapletFloorletVolatilities volsRhoShifted = IborCapletFloorletSabrRateVolatilityDataSet
          .getVolatilitiesShiftRho(VALUATION, EUR_ESTRTERM_3M, i, shift);
      double pvRhoShifted = PRICER_ON_INARREARS_QOTHER.presentValue(CAPLET_LONG, RATES, volsRhoShifted).getAmount();
      double psRhoExpected = (pvRhoShifted - pvStart) / shift;
      assertThat(psRhoExpected).isEqualTo(psRho.getSensitivity().get(i), TOLERANCE_VEGA);
      SabrParametersIborCapletFloorletVolatilities volsNuShifted = IborCapletFloorletSabrRateVolatilityDataSet
          .getVolatilitiesShiftNu(VALUATION, EUR_ESTRTERM_3M, i, shift);
      double pvNuShifted = PRICER_ON_INARREARS_QOTHER.presentValue(CAPLET_LONG, RATES, volsNuShifted).getAmount();
      double psNuExpected = (pvNuShifted - pvStart) / shift;
      assertThat(psNuExpected).isEqualTo(psNu.getSensitivity().get(i), TOLERANCE_VEGA);
    }
  }

  @Test
  public void presentValue_afterstart_param_sensitivity() {
    PointSensitivities ptsCapletLongComputed = PRICER_ON_INARREARS_QOTHER
        .presentValueSensitivityModelParamsSabr(CAPLET_LONG, RATES_AFTER_START, VOLS_AFTER_START).build();
    CurrencyParameterSensitivities ps = VOLS_AFTER_START.parameterSensitivity(ptsCapletLongComputed);
    CurrencyParameterSensitivity psAlpha = ps.getSensitivity(CurveName.of("Test-SABR-Alpha"), EUR);
    CurrencyParameterSensitivity psBeta = ps.getSensitivity(CurveName.of("Test-SABR-Beta"), EUR);
    CurrencyParameterSensitivity psRho = ps.getSensitivity(CurveName.of("Test-SABR-Rho"), EUR);
    CurrencyParameterSensitivity psNu = ps.getSensitivity(CurveName.of("Test-SABR-Nu"), EUR);
    double shift = 1.0E-6;
    double pvStart =
        PRICER_ON_INARREARS_QOTHER.presentValue(CAPLET_LONG, RATES_AFTER_START, VOLS_AFTER_START).getAmount();
    for (int i = 0; i < 6; i++) { // size of Alpha curve
      SabrParametersIborCapletFloorletVolatilities volsAlphaShifted = IborCapletFloorletSabrRateVolatilityDataSet
          .getVolatilitiesShiftAlpha(VALUATION_AFTER_START, EUR_ESTRTERM_3M, i, shift);
      double pvAlphaShifted =
          PRICER_ON_INARREARS_QOTHER.presentValue(CAPLET_LONG, RATES_AFTER_START, volsAlphaShifted).getAmount();
      double psAlphaExpected = (pvAlphaShifted - pvStart) / shift;
      assertThat(psAlphaExpected).isEqualTo(psAlpha.getSensitivity().get(i), TOLERANCE_VEGA);
      SabrParametersIborCapletFloorletVolatilities volsBetaShifted = IborCapletFloorletSabrRateVolatilityDataSet
          .getVolatilitiesShiftBeta(VALUATION_AFTER_START, EUR_ESTRTERM_3M, i, shift);
      double pvBetaShifted =
          PRICER_ON_INARREARS_QOTHER.presentValue(CAPLET_LONG, RATES_AFTER_START, volsBetaShifted).getAmount();
      double psBetaExpected = (pvBetaShifted - pvStart) / shift;
      assertThat(psBetaExpected).isEqualTo(psBeta.getSensitivity().get(i), TOLERANCE_VEGA);
      SabrParametersIborCapletFloorletVolatilities volsRhoShifted = IborCapletFloorletSabrRateVolatilityDataSet
          .getVolatilitiesShiftRho(VALUATION_AFTER_START, EUR_ESTRTERM_3M, i, shift);
      double pvRhoShifted =
          PRICER_ON_INARREARS_QOTHER.presentValue(CAPLET_LONG, RATES_AFTER_START, volsRhoShifted).getAmount();
      double psRhoExpected = (pvRhoShifted - pvStart) / shift;
      assertThat(psRhoExpected).isEqualTo(psRho.getSensitivity().get(i), TOLERANCE_VEGA);
      SabrParametersIborCapletFloorletVolatilities volsNuShifted = IborCapletFloorletSabrRateVolatilityDataSet
          .getVolatilitiesShiftNu(VALUATION_AFTER_START, EUR_ESTRTERM_3M, i, shift);
      double pvNuShifted =
          PRICER_ON_INARREARS_QOTHER.presentValue(CAPLET_LONG, RATES_AFTER_START, volsNuShifted).getAmount();
      double psNuExpected = (pvNuShifted - pvStart) / shift;
      assertThat(psNuExpected).isEqualTo(psNu.getSensitivity().get(i), TOLERANCE_VEGA);
    }
  }

  @Test
  public void presentValue_afterend_param_sensitivity() {
    PointSensitivities ptsCapletLongComputed = PRICER_ON_INARREARS_QOTHER
        .presentValueSensitivityModelParamsSabr(CAPLET_LONG, RATES_AFTER_END, VOLS_AFTER_END).build();
    PointSensitivities ptsFloorletShortComputed = PRICER_ON_INARREARS_QOTHER
        .presentValueSensitivityModelParamsSabr(FLOORLET_SHORT, RATES_AFTER_END, VOLS_AFTER_END).build();
    assertThat(ptsCapletLongComputed).isEqualTo(PointSensitivities.empty());
    assertThat(ptsFloorletShortComputed).isEqualTo(PointSensitivities.empty());
  }

  @Test
  public void presentValue_afterpay_param_sensitivity() {
    PointSensitivities ptsCapletLongComputed = PRICER_ON_INARREARS_QOTHER
        .presentValueSensitivityModelParamsSabr(CAPLET_LONG, RATES_AFTER_PAY, VOLS_AFTER_PAY).build();
    PointSensitivities ptsFloorletShortComputed = PRICER_ON_INARREARS_QOTHER
        .presentValueSensitivityModelParamsSabr(FLOORLET_SHORT, RATES_AFTER_PAY, VOLS_AFTER_PAY).build();
    assertThat(ptsCapletLongComputed).isEqualTo(PointSensitivities.empty());
    assertThat(ptsFloorletShortComputed).isEqualTo(PointSensitivities.empty());
  }
  
  //-------------------------------------------------------------------------

  @Test
  public void forwardRate_impliedVolatility_beforestart() {
    double forwardRateCapletComputed = PRICER_ON_INARREARS_Q1.forwardRate(CAPLET_LONG, RATES);
    double impliedVolCapletComputed = PRICER_ON_INARREARS_Q1.impliedVolatility(CAPLET_LONG, RATES, VOLS);
    double forwardRateFloorletComputed = PRICER_ON_INARREARS_Q1.forwardRate(FLOORLET_LONG, RATES);
    double impliedVolFloorletComputed = PRICER_ON_INARREARS_Q1.impliedVolatility(FLOORLET_LONG, RATES, VOLS);
    OvernightIndexObservation onObs = OvernightIndexObservation.of(EUR_ESTR, START_DATE, REF_DATA);
    double forwardExpected = RATES.overnightIndexRates(EUR_ESTR).periodRate(onObs, END_DATE);
    assertThat(forwardRateCapletComputed).isCloseTo(forwardExpected, TOLERANCE_SMALL_IV);
    assertThat(forwardRateFloorletComputed).isCloseTo(forwardExpected, TOLERANCE_SMALL_IV);
    double num = NOTIONAL * ACCRUAL_FACTOR * RATES.discountFactor(EUR, PAYMENT_DATE);
    double timeToExpiry = VOLS.relativeTime(END_DATE.atStartOfDay(ZoneOffset.UTC));
    CurrencyAmount inArrearsPv = PRICER_ON_INARREARS_Q1.presentValue(CAPLET_LONG, RATES, VOLS);
    double impliedVolExpected = BlackFormulaRepository.impliedVolatility(
        inArrearsPv.getAmount() / num,
        forwardExpected + CONST_SHIFT,
        STRIKE + CONST_SHIFT,
        timeToExpiry,
        true);
    assertThat(impliedVolCapletComputed).isCloseTo(impliedVolExpected, TOLERANCE_SMALL_IV);
    assertThat(impliedVolFloorletComputed).isCloseTo(impliedVolExpected, TOLERANCE_SMALL_IV);
  }

  @Test
  public void forwardRate_impliedVolatility_afterstart() {
    double strike = 0.0115; // near-ATM
    OvernightInArrearsCapletFloorletPeriod capletLong =
        OvernightInArrearsCapletFloorletPeriod.builder()
            .caplet(strike)
            .startDate(START_DATE)
            .endDate(END_DATE)
            .paymentDate(PAYMENT_DATE)
            .yearFraction(ACCRUAL_FACTOR)
            .notional(NOTIONAL)
            .overnightRate(RATE_COMP)
            .build();
    OvernightInArrearsCapletFloorletPeriod floorletLong =
        OvernightInArrearsCapletFloorletPeriod.builder()
            .floorlet(strike)
            .startDate(START_DATE)
            .endDate(END_DATE)
            .paymentDate(PAYMENT_DATE)
            .yearFraction(ACCRUAL_FACTOR)
            .notional(NOTIONAL)
            .overnightRate(RATE_COMP)
            .build();
    double forwardRateCapletComputed = PRICER_ON_INARREARS_Q1.forwardRate(capletLong, RATES_AFTER_START);
    double impliedVolCapletComputed = PRICER_ON_INARREARS_Q1.impliedVolatility(
        capletLong,
        RATES_AFTER_START,
        VOLS_AFTER_START);
    double forwardRateFloorletComputed = PRICER_ON_INARREARS_Q1.forwardRate(floorletLong, RATES_AFTER_START);
    double impliedVolFloorletComputed = PRICER_ON_INARREARS_Q1.impliedVolatility(
        floorletLong,
        RATES_AFTER_START,
        VOLS_AFTER_START);
    double forwardExpected = ForwardOvernightCompoundedRateComputationFn.DEFAULT.rate(
        capletLong.getOvernightRate(),
        START_DATE,
        END_DATE,
        RATES_AFTER_START);
    assertThat(forwardRateCapletComputed).isCloseTo(forwardExpected, TOLERANCE_SMALL_IV);
    assertThat(forwardRateFloorletComputed).isCloseTo(forwardExpected, TOLERANCE_SMALL_IV);
    double num = NOTIONAL * ACCRUAL_FACTOR * RATES_AFTER_START.discountFactor(EUR, PAYMENT_DATE);
    double timeToExpiry = VOLS_AFTER_START.relativeTime(END_DATE.atStartOfDay(ZoneOffset.UTC));
    CurrencyAmount inArrearsPv = PRICER_ON_INARREARS_Q1.presentValue(capletLong, RATES_AFTER_START, VOLS_AFTER_START);
    double impliedVolExpected = BlackFormulaRepository.impliedVolatility(
        inArrearsPv.getAmount() / num,
        forwardExpected + CONST_SHIFT,
        strike + CONST_SHIFT,
        timeToExpiry,
        true);
    assertThat(impliedVolCapletComputed).isCloseTo(impliedVolExpected, TOLERANCE_SMALL_IV);
    assertThat(impliedVolFloorletComputed).isCloseTo(impliedVolExpected, TOLERANCE_SMALL_IV);
  }

  @Test
  public void forwardRate_impliedVolatility_afterend() {
    double forwardRateCapletComputed = PRICER_ON_INARREARS_Q1.forwardRate(CAPLET_LONG, RATES_AFTER_END);
    double forwardRateFloorletComputed = PRICER_ON_INARREARS_Q1.forwardRate(FLOORLET_LONG, RATES_AFTER_END);
    double forwardExpected = ForwardOvernightCompoundedRateComputationFn.DEFAULT.rate(
        CAPLET_LONG.getOvernightRate(),
        START_DATE,
        END_DATE,
        RATES_AFTER_END);
    assertThat(forwardRateCapletComputed).isCloseTo(forwardExpected, TOLERANCE_SMALL_IV);
    assertThat(forwardRateFloorletComputed).isCloseTo(forwardExpected, TOLERANCE_SMALL_IV);
    // impliedVolatility fails after expiry
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER_ON_INARREARS_Q1.impliedVolatility(CAPLET_LONG, RATES_AFTER_END, VOLS_AFTER_END));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER_ON_INARREARS_Q1.impliedVolatility(FLOORLET_LONG, RATES_AFTER_END, VOLS_AFTER_END));
  }

}
