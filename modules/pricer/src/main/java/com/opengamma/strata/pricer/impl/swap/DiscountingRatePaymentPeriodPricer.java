/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.swap;

import static java.time.temporal.ChronoUnit.DAYS;

import java.time.LocalDate;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.fx.FxIndexRates;
import com.opengamma.strata.pricer.rate.RateComputationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.SwapPaymentPeriodPricer;
import com.opengamma.strata.product.rate.RateComputation;
import com.opengamma.strata.product.swap.CompoundingMethod;
import com.opengamma.strata.product.swap.FxReset;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;

/**
 * Pricer implementation for swap payment periods based on a rate.
 * <p>
 * The value of a payment period is calculated by combining the value of each accrual period.
 * Where necessary, the accrual periods are compounded.
 */
public class DiscountingRatePaymentPeriodPricer
    implements SwapPaymentPeriodPricer<RatePaymentPeriod> {

  /**
   * Default implementation.
   */
  public static final DiscountingRatePaymentPeriodPricer DEFAULT = new DiscountingRatePaymentPeriodPricer(
      RateComputationFn.standard());

  /**
   * Rate computation.
   */
  private final RateComputationFn<RateComputation> rateComputationFn;

  /**
   * Creates an instance.
   * 
   * @param rateComputationFn  the rate computation function
   */
  public DiscountingRatePaymentPeriodPricer(
      RateComputationFn<RateComputation> rateComputationFn) {
    this.rateComputationFn = ArgChecker.notNull(rateComputationFn, "rateComputationFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(RatePaymentPeriod period, RatesProvider provider) {
    // forecastValue * discountFactor
    double df = provider.discountFactor(period.getCurrency(), period.getPaymentDate());
    return forecastValue(period, provider) * df;
  }

  @Override
  public double forecastValue(RatePaymentPeriod period, RatesProvider provider) {
    // notional * fxRate
    // fxRate is 1 if no FX conversion
    double notional = period.getNotional() * fxRate(period, provider);
    return accrualWithNotional(period, notional, provider);
  }

  @Override
  public double pvbp(RatePaymentPeriod paymentPeriod, RatesProvider provider) {
    ArgChecker.isTrue(!paymentPeriod.getFxReset().isPresent(), "FX reset is not supported");
    int accPeriodCount = paymentPeriod.getAccrualPeriods().size();
    ArgChecker.isTrue(accPeriodCount == 1 || paymentPeriod.getCompoundingMethod().equals(CompoundingMethod.FLAT),
        "Only one accrued period or Flat compounding supported");
    // no compounding
    if (accPeriodCount == 1) {
      RateAccrualPeriod accrualPeriod = paymentPeriod.getAccrualPeriods().get(0);
      double df = provider.discountFactor(paymentPeriod.getCurrency(), paymentPeriod.getPaymentDate());
      return df * accrualPeriod.getYearFraction() * paymentPeriod.getNotional();
    } else {
      // Flat compounding
      switch (paymentPeriod.getCompoundingMethod()) {
        case FLAT:
          return pvbpCompoundedFlat(paymentPeriod, provider);
        default:
          throw new UnsupportedOperationException("PVBP not implemented yet for non FLAT compounding");
      }
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public double accruedInterest(RatePaymentPeriod period, RatesProvider provider) {
    LocalDate valDate = provider.getValuationDate();
    if (valDate.compareTo(period.getStartDate()) <= 0 || valDate.compareTo(period.getEndDate()) > 0) {
      return 0d;
    }
    ImmutableList.Builder<RateAccrualPeriod> truncated = ImmutableList.builder();
    for (RateAccrualPeriod rap : period.getAccrualPeriods()) {
      if (valDate.compareTo(rap.getEndDate()) > 0) {
        truncated.add(rap);
      } else {
        truncated.add(rap.toBuilder()
            .endDate(provider.getValuationDate())
            .unadjustedEndDate(provider.getValuationDate())
            .yearFraction(period.getDayCount().yearFraction(rap.getStartDate(), provider.getValuationDate()))
            .build());
        break;
      }
    }
    RatePaymentPeriod adjustedPaymentPeriod = period.toBuilder().accrualPeriods(truncated.build()).build();
    return forecastValue(adjustedPaymentPeriod, provider);
  }

  //-------------------------------------------------------------------------
  // resolve the FX rate from the FX reset, returning an FX rate of 1 if not applicable
  private double fxRate(RatePaymentPeriod paymentPeriod, RatesProvider provider) {
    // inefficient to use Optional.orElse because double primitive type would be boxed
    if (paymentPeriod.getFxReset().isPresent()) {
      FxReset fxReset = paymentPeriod.getFxReset().get();
      FxIndexRates rates = provider.fxIndexRates(fxReset.getObservation().getIndex());
      return rates.rate(fxReset.getObservation(), fxReset.getReferenceCurrency());
    } else {
      return 1d;
    }
  }

  private double accrualWithNotional(RatePaymentPeriod period, double notional, RatesProvider provider) {
    // handle simple case and more complex compounding for whole payment period
    if (period.getAccrualPeriods().size() == 1) {
      RateAccrualPeriod accrualPeriod = period.getAccrualPeriods().get(0);
      return unitNotionalAccrual(accrualPeriod, accrualPeriod.getSpread(), provider) * notional;
    }
    return accrueCompounded(period, notional, provider);
  }

  // calculate the accrual for a unit notional
  private double unitNotionalAccrual(RateAccrualPeriod accrualPeriod, double spread, RatesProvider provider) {
    double rawRate = rawRate(accrualPeriod, provider);
    return unitNotionalAccrualRaw(accrualPeriod, rawRate, spread);
  }

  // calculate the accrual for a unit notional from the raw rate
  private double unitNotionalAccrualRaw(RateAccrualPeriod accrualPeriod, double rawRate, double spread) {
    double treatedRate = rawRate * accrualPeriod.getGearing() + spread;
    return accrualPeriod.getNegativeRateMethod().adjust(treatedRate * accrualPeriod.getYearFraction());
  }

  // finds the raw rate for the accrual period
  // the raw rate is the rate before gearing, spread and negative checks are applied
  private double rawRate(RateAccrualPeriod accrualPeriod, RatesProvider provider) {
    return rateComputationFn.rate(
        accrualPeriod.getRateComputation(),
        accrualPeriod.getStartDate(),
        accrualPeriod.getEndDate(),
        provider);
  }

  //-------------------------------------------------------------------------
  // apply compounding
  private double accrueCompounded(RatePaymentPeriod paymentPeriod, double notional, RatesProvider provider) {
    switch (paymentPeriod.getCompoundingMethod()) {
      case STRAIGHT:
        return compoundedStraight(paymentPeriod, notional, provider);
      case FLAT:
        return compoundedFlat(paymentPeriod, notional, provider);
      case SPREAD_EXCLUSIVE:
        return compoundedSpreadExclusive(paymentPeriod, notional, provider);
      case NONE:
      default:
        return compoundingNone(paymentPeriod, notional, provider);
    }
  }

  // straight compounding
  private double compoundedStraight(RatePaymentPeriod paymentPeriod, double notional, RatesProvider provider) {
    double notionalAccrued = notional;
    for (RateAccrualPeriod accrualPeriod : paymentPeriod.getAccrualPeriods()) {
      double investFactor = 1 + unitNotionalAccrual(accrualPeriod, accrualPeriod.getSpread(), provider);
      notionalAccrued *= investFactor;
    }
    return (notionalAccrued - notional);
  }

  // flat compounding
  private double compoundedFlat(RatePaymentPeriod paymentPeriod, double notional, RatesProvider provider) {
    double cpaAccumulated = 0d;
    for (RateAccrualPeriod accrualPeriod : paymentPeriod.getAccrualPeriods()) {
      double rate = rawRate(accrualPeriod, provider);
      cpaAccumulated += cpaAccumulated * unitNotionalAccrualRaw(accrualPeriod, rate, 0) +
          unitNotionalAccrualRaw(accrualPeriod, rate, accrualPeriod.getSpread());
    }
    return cpaAccumulated * notional;
  }

  // spread exclusive compounding
  private double compoundedSpreadExclusive(RatePaymentPeriod paymentPeriod, double notional, RatesProvider provider) {
    double notionalAccrued = notional;
    double spreadAccrued = 0;
    for (RateAccrualPeriod accrualPeriod : paymentPeriod.getAccrualPeriods()) {
      double investFactor = 1 + unitNotionalAccrual(accrualPeriod, 0, provider);
      notionalAccrued *= investFactor;
      spreadAccrued += notional * accrualPeriod.getSpread() * accrualPeriod.getYearFraction();
    }
    return (notionalAccrued - notional + spreadAccrued);
  }

  // no compounding, just sum each accrual period
  private double compoundingNone(RatePaymentPeriod paymentPeriod, double notional, RatesProvider provider) {
    return paymentPeriod.getAccrualPeriods().stream()
        .mapToDouble(accrualPeriod -> unitNotionalAccrual(accrualPeriod, accrualPeriod.getSpread(), provider) * notional)
        .sum();
  }

  //-------------------------------------------------------------------------
  @Override
  public PointSensitivityBuilder presentValueSensitivity(RatePaymentPeriod period, RatesProvider provider) {
    Currency ccy = period.getCurrency();
    DiscountFactors discountFactors = provider.discountFactors(ccy);
    LocalDate paymentDate = period.getPaymentDate();
    double df = discountFactors.discountFactor(paymentDate);
    PointSensitivityBuilder forecastSensitivity = forecastValueSensitivity(period, provider);
    forecastSensitivity = forecastSensitivity.multipliedBy(df);
    double forecastValue = forecastValue(period, provider);
    PointSensitivityBuilder dscSensitivity = discountFactors.zeroRatePointSensitivity(paymentDate);
    dscSensitivity = dscSensitivity.multipliedBy(forecastValue);
    return forecastSensitivity.combinedWith(dscSensitivity);
  }

  @Override
  public PointSensitivityBuilder forecastValueSensitivity(RatePaymentPeriod period, RatesProvider provider) {
    // historic payments have zero sensi
    if (period.getPaymentDate().isBefore(provider.getValuationDate())) {
      return PointSensitivityBuilder.none();
    }
    PointSensitivityBuilder sensiFx = fxRateSensitivity(period, provider);
    double accrual = accrualWithNotional(period, period.getNotional(), provider);
    sensiFx = sensiFx.multipliedBy(accrual);
    PointSensitivityBuilder sensiAccrual = PointSensitivityBuilder.none();
    if (period.isCompoundingApplicable()) {
      sensiAccrual = accrueCompoundedSensitivity(period, provider);
    } else {
      sensiAccrual = unitNotionalSensitivityNoCompounding(period, provider);
    }
    double notional = period.getNotional() * fxRate(period, provider);
    sensiAccrual = sensiAccrual.multipliedBy(notional);
    return sensiFx.combinedWith(sensiAccrual);
  }

  @Override
  public PointSensitivityBuilder pvbpSensitivity(RatePaymentPeriod paymentPeriod, RatesProvider provider) {
    ArgChecker.isTrue(!paymentPeriod.getFxReset().isPresent(), "FX reset is not supported");
    int accPeriodCount = paymentPeriod.getAccrualPeriods().size();
    ArgChecker.isTrue(accPeriodCount == 1 || paymentPeriod.getCompoundingMethod().equals(CompoundingMethod.FLAT),
        "Only one accrued period or Flat compounding supported");
    // no compounding
    if (accPeriodCount == 1) {
      RateAccrualPeriod accrualPeriod = paymentPeriod.getAccrualPeriods().get(0);
      DiscountFactors discountFactors = provider.discountFactors(paymentPeriod.getCurrency());
      return discountFactors.zeroRatePointSensitivity(paymentPeriod.getPaymentDate())
          .multipliedBy(accrualPeriod.getYearFraction() * paymentPeriod.getNotional());
    } else {
      // Flat compounding
      switch (paymentPeriod.getCompoundingMethod()) {
        case FLAT:
          return pvbpSensitivtyCompoundedFlat(paymentPeriod, provider);
        default:
          throw new UnsupportedOperationException("PVBP not implemented yet for non FLAT compounding");
      }
    }
  }

  // resolve the FX rate sensitivity from the FX reset
  private PointSensitivityBuilder fxRateSensitivity(RatePaymentPeriod paymentPeriod, RatesProvider provider) {
    if (paymentPeriod.getFxReset().isPresent()) {
      FxReset fxReset = paymentPeriod.getFxReset().get();
      FxIndexRates rates = provider.fxIndexRates(fxReset.getObservation().getIndex());
      return rates.ratePointSensitivity(fxReset.getObservation(), fxReset.getReferenceCurrency());
    }
    return PointSensitivityBuilder.none();
  }

  // computes the sensitivity of the payment period to the rate observations (not to the discount factors)
  private PointSensitivityBuilder unitNotionalSensitivityNoCompounding(RatePaymentPeriod period, RatesProvider provider) {
    Currency ccy = period.getCurrency();
    PointSensitivityBuilder sensi = PointSensitivityBuilder.none();
    for (RateAccrualPeriod accrualPeriod : period.getAccrualPeriods()) {
      sensi = sensi.combinedWith(unitNotionalSensitivityAccrual(accrualPeriod, ccy, provider));
    }
    return sensi;
  }

  // computes the sensitivity of the accrual period to the rate observations (not to discount factors)
  private PointSensitivityBuilder unitNotionalSensitivityAccrual(
      RateAccrualPeriod period,
      Currency ccy,
      RatesProvider provider) {

    PointSensitivityBuilder sensi = rateComputationFn.rateSensitivity(
        period.getRateComputation(), period.getStartDate(), period.getEndDate(), provider);
    return sensi.multipliedBy(period.getGearing() * period.getYearFraction());
  }

  //-------------------------------------------------------------------------
  // apply compounding - sensitivity
  private PointSensitivityBuilder accrueCompoundedSensitivity(
      RatePaymentPeriod paymentPeriod,
      RatesProvider provider) {

    switch (paymentPeriod.getCompoundingMethod()) {
      case STRAIGHT:
        return compoundedStraightSensitivity(paymentPeriod, provider);
      case FLAT:
        return compoundedFlatSensitivity(paymentPeriod, provider);
      case SPREAD_EXCLUSIVE:
        return compoundedSpreadExclusiveSensitivity(paymentPeriod, provider);
      default:
        return unitNotionalSensitivityNoCompounding(paymentPeriod, provider);
    }
  }

  // straight compounding
  private PointSensitivityBuilder compoundedStraightSensitivity(
      RatePaymentPeriod paymentPeriod,
      RatesProvider provider) {

    double notionalAccrued = 1d;
    Currency ccy = paymentPeriod.getCurrency();
    PointSensitivityBuilder sensi = PointSensitivityBuilder.none();
    for (RateAccrualPeriod accrualPeriod : paymentPeriod.getAccrualPeriods()) {
      double investFactor = 1d + unitNotionalAccrual(accrualPeriod, accrualPeriod.getSpread(), provider);
      notionalAccrued *= investFactor;
      PointSensitivityBuilder investFactorSensi =
          unitNotionalSensitivityAccrual(accrualPeriod, ccy, provider).multipliedBy(1d / investFactor);
      sensi = sensi.combinedWith(investFactorSensi);
    }
    return sensi.multipliedBy(notionalAccrued);
  }

  // flat compounding
  private PointSensitivityBuilder compoundedFlatSensitivity(
      RatePaymentPeriod paymentPeriod,
      RatesProvider provider) {

    double cpaAccumulated = 0d;
    Currency ccy = paymentPeriod.getCurrency();
    PointSensitivityBuilder sensiAccumulated = PointSensitivityBuilder.none();
    for (RateAccrualPeriod accrualPeriod : paymentPeriod.getAccrualPeriods()) {
      double rate = rawRate(accrualPeriod, provider);
      double accrualZeroSpread = unitNotionalAccrualRaw(accrualPeriod, rate, 0);
      PointSensitivityBuilder sensiCp = sensiAccumulated.cloned();
      sensiCp = sensiCp.multipliedBy(accrualZeroSpread);
      PointSensitivityBuilder sensi2 =
          unitNotionalSensitivityAccrual(accrualPeriod, ccy, provider).multipliedBy(1d + cpaAccumulated);
      cpaAccumulated += cpaAccumulated * accrualZeroSpread +
          unitNotionalAccrualRaw(accrualPeriod, rate, accrualPeriod.getSpread());
      sensiCp = sensiCp.combinedWith(sensi2);
      sensiAccumulated = sensiAccumulated.combinedWith(sensiCp).normalize();
    }
    return sensiAccumulated;
  }

  // spread exclusive compounding
  private PointSensitivityBuilder compoundedSpreadExclusiveSensitivity(
      RatePaymentPeriod paymentPeriod,
      RatesProvider provider) {

    double notionalAccrued = 1d;
    Currency ccy = paymentPeriod.getCurrency();
    PointSensitivityBuilder sensi = PointSensitivityBuilder.none();
    for (RateAccrualPeriod accrualPeriod : paymentPeriod.getAccrualPeriods()) {
      double investFactor = 1 + unitNotionalAccrual(accrualPeriod, 0, provider);
      notionalAccrued *= investFactor;
      PointSensitivityBuilder investFactorSensi =
          unitNotionalSensitivityAccrual(accrualPeriod, ccy, provider).multipliedBy(1d / investFactor);
      sensi = sensi.combinedWith(investFactorSensi);
    }
    return sensi.multipliedBy(notionalAccrued);
  }

  //-------------------------------------------------------------------------
  @Override
  public void explainPresentValue(RatePaymentPeriod paymentPeriod, RatesProvider provider, ExplainMapBuilder builder) {
    Currency currency = paymentPeriod.getCurrency();
    LocalDate paymentDate = paymentPeriod.getPaymentDate();

    double fxRate = fxRate(paymentPeriod, provider);
    double notional = paymentPeriod.getNotional() * fxRate;
    builder.put(ExplainKey.ENTRY_TYPE, "RatePaymentPeriod");
    builder.put(ExplainKey.PAYMENT_DATE, paymentDate);
    builder.put(ExplainKey.PAYMENT_CURRENCY, currency);
    builder.put(ExplainKey.NOTIONAL, CurrencyAmount.of(currency, notional));
    builder.put(ExplainKey.TRADE_NOTIONAL, paymentPeriod.getNotionalAmount());
    if (paymentDate.isBefore(provider.getValuationDate())) {
      builder.put(ExplainKey.COMPLETED, Boolean.TRUE);
      builder.put(ExplainKey.FORECAST_VALUE, CurrencyAmount.zero(currency));
      builder.put(ExplainKey.PRESENT_VALUE, CurrencyAmount.zero(currency));
    } else {
      paymentPeriod.getFxReset().ifPresent(fxReset -> {
        builder.addListEntry(ExplainKey.OBSERVATIONS, child -> {
          child.put(ExplainKey.ENTRY_TYPE, "FxObservation");
          child.put(ExplainKey.INDEX, fxReset.getObservation().getIndex());
          child.put(ExplainKey.FIXING_DATE, fxReset.getObservation().getFixingDate());
          child.put(ExplainKey.INDEX_VALUE, fxRate);
        });
      });
      for (RateAccrualPeriod accrualPeriod : paymentPeriod.getAccrualPeriods()) {
        builder.addListEntry(
            ExplainKey.ACCRUAL_PERIODS,
            child -> explainPresentValue(accrualPeriod, paymentPeriod.getDayCount(), currency, notional, provider, child));
      }
      builder.put(ExplainKey.COMPOUNDING, paymentPeriod.getCompoundingMethod());
      builder.put(ExplainKey.DISCOUNT_FACTOR, provider.discountFactor(currency, paymentDate));
      builder.put(ExplainKey.FORECAST_VALUE, CurrencyAmount.of(currency, forecastValue(paymentPeriod, provider)));
      builder.put(ExplainKey.PRESENT_VALUE, CurrencyAmount.of(currency, presentValue(paymentPeriod, provider)));
    }
  }

  // explain PV for an accrual period, ignoring compounding
  private void explainPresentValue(
      RateAccrualPeriod accrualPeriod,
      DayCount dayCount,
      Currency currency,
      double notional,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    double rawRate = rateComputationFn.explainRate(
        accrualPeriod.getRateComputation(), accrualPeriod.getStartDate(), accrualPeriod.getEndDate(), provider, builder);
    double payOffRate = rawRate * accrualPeriod.getGearing() + accrualPeriod.getSpread();
    double ua = unitNotionalAccrual(accrualPeriod, accrualPeriod.getSpread(), provider);

    // Note that the forecast value is not published since this is potentially misleading when
    // compounding is being applied, and when it isn't then it's the same as the forecast
    // value of the payment period.

    builder.put(ExplainKey.ENTRY_TYPE, "AccrualPeriod");
    builder.put(ExplainKey.START_DATE, accrualPeriod.getStartDate());
    builder.put(ExplainKey.UNADJUSTED_START_DATE, accrualPeriod.getUnadjustedStartDate());
    builder.put(ExplainKey.END_DATE, accrualPeriod.getEndDate());
    builder.put(ExplainKey.UNADJUSTED_END_DATE, accrualPeriod.getUnadjustedEndDate());
    builder.put(ExplainKey.ACCRUAL_YEAR_FRACTION, accrualPeriod.getYearFraction());
    builder.put(ExplainKey.ACCRUAL_DAYS, dayCount.days(accrualPeriod.getStartDate(), accrualPeriod.getEndDate()));
    builder.put(ExplainKey.DAYS, (int) DAYS.between(accrualPeriod.getStartDate(), accrualPeriod.getEndDate()));
    builder.put(ExplainKey.GEARING, accrualPeriod.getGearing());
    builder.put(ExplainKey.SPREAD, accrualPeriod.getSpread());
    builder.put(ExplainKey.PAY_OFF_RATE, accrualPeriod.getNegativeRateMethod().adjust(payOffRate));
    builder.put(ExplainKey.UNIT_AMOUNT, ua);
  }

  //-------------------------------------------------------------------------
  @Override
  public MultiCurrencyAmount currencyExposure(RatePaymentPeriod period, RatesProvider provider) {
    double df = provider.discountFactor(period.getCurrency(), period.getPaymentDate());
    if (period.getFxReset().isPresent()) {
      FxReset fxReset = period.getFxReset().get();
      LocalDate fixingDate = fxReset.getObservation().getFixingDate();
      FxIndexRates rates = provider.fxIndexRates(fxReset.getObservation().getIndex());
      if (!fixingDate.isAfter(provider.getValuationDate()) &&
          rates.getFixings().get(fixingDate).isPresent()) {
        double fxRate = rates.rate(fxReset.getObservation(), fxReset.getReferenceCurrency());
        return MultiCurrencyAmount.of(period.getCurrency(),
            accrualWithNotional(period, period.getNotional() * fxRate * df, provider));
      }
      double fxRateSpotSensitivity = rates.getFxForwardRates()
          .rateFxSpotSensitivity(fxReset.getReferenceCurrency(), fxReset.getObservation().getMaturityDate());
      return MultiCurrencyAmount.of(fxReset.getReferenceCurrency(),
          accrualWithNotional(period, period.getNotional() * fxRateSpotSensitivity * df, provider));
    }
    return MultiCurrencyAmount.of(period.getCurrency(), accrualWithNotional(period, period.getNotional() * df, provider));
  }

  @Override
  public double currentCash(RatePaymentPeriod period, RatesProvider provider) {
    if (provider.getValuationDate().isEqual(period.getPaymentDate())) {
      return forecastValue(period, provider);
    }
    return 0d;
  }

  //-------------------------------------------------------------------------
  // sensitivity to the spread for a payment period with FLAT compounding type
  private double pvbpCompoundedFlat(RatePaymentPeriod paymentPeriod, RatesProvider provider) {
    int nbCmp = paymentPeriod.getAccrualPeriods().size();
    double[] rate = paymentPeriod.getAccrualPeriods().stream()
        .mapToDouble(ap -> rawRate(ap, provider))
        .toArray();
    double df = provider.discountFactor(paymentPeriod.getCurrency(), paymentPeriod.getPaymentDate());
    double rBar = 1.0;
    double[] cpaAccumulatedBar = new double[nbCmp + 1];
    cpaAccumulatedBar[nbCmp] = paymentPeriod.getNotional() * df * rBar;
    double spreadBar = 0.0d;
    for (int j = nbCmp - 1; j >= 0; j--) {
      cpaAccumulatedBar[j] = (1.0d + paymentPeriod.getAccrualPeriods().get(j).getYearFraction() * rate[j] *
          paymentPeriod.getAccrualPeriods().get(j).getGearing()) * cpaAccumulatedBar[j + 1];
      spreadBar += paymentPeriod.getAccrualPeriods().get(j).getYearFraction() * cpaAccumulatedBar[j + 1];
    }
    return spreadBar;
  }

  // sensitivity to the spread for a payment period with FLAT compounding type
  private PointSensitivityBuilder pvbpSensitivtyCompoundedFlat(RatePaymentPeriod paymentPeriod, RatesProvider provider) {
    Currency ccy = paymentPeriod.getCurrency();
    int nbCmp = paymentPeriod.getAccrualPeriods().size();
    double[] rate = paymentPeriod.getAccrualPeriods().stream()
        .mapToDouble(ap -> rawRate(ap, provider))
        .toArray();
    double df = provider.discountFactor(ccy, paymentPeriod.getPaymentDate());
    double rB1 = 1.0;
    double[] cpaAccumulatedB1 = new double[nbCmp + 1];
    cpaAccumulatedB1[nbCmp] = paymentPeriod.getNotional() * df * rB1;
    for (int j = nbCmp - 1; j >= 0; j--) {
      RateAccrualPeriod accrualPeriod = paymentPeriod.getAccrualPeriods().get(j);
      cpaAccumulatedB1[j] =
          (1.0d + accrualPeriod.getYearFraction() * rate[j] * accrualPeriod.getGearing()) * cpaAccumulatedB1[j + 1];
    }
    // backward sweep
    double pvbpB2 = 1.0d;
    double[] cpaAccumulatedB1B2 = new double[nbCmp + 1];
    double[] rateB2 = new double[nbCmp];
    for (int j = 0; j < nbCmp; j++) {
      RateAccrualPeriod accrualPeriod = paymentPeriod.getAccrualPeriods().get(j);
      cpaAccumulatedB1B2[j + 1] += accrualPeriod.getYearFraction() * pvbpB2;
      cpaAccumulatedB1B2[j + 1] +=
          (1.0d + accrualPeriod.getYearFraction() * rate[j] * accrualPeriod.getGearing()) * cpaAccumulatedB1B2[j];
      rateB2[j] += accrualPeriod.getYearFraction() * accrualPeriod.getGearing() *
          cpaAccumulatedB1[j + 1] * cpaAccumulatedB1B2[j];
    }
    double dfB2 = paymentPeriod.getNotional() * rB1 * cpaAccumulatedB1B2[nbCmp];
    PointSensitivityBuilder dfdr = provider.discountFactors(ccy).zeroRatePointSensitivity(paymentPeriod.getPaymentDate());
    PointSensitivityBuilder pvbpdr = dfdr.multipliedBy(dfB2);
    for (int j = 0; j < nbCmp; j++) {
      RateAccrualPeriod accrualPeriod = paymentPeriod.getAccrualPeriods().get(j);
      pvbpdr = pvbpdr.combinedWith(rateComputationFn.rateSensitivity(accrualPeriod.getRateComputation(),
          accrualPeriod.getStartDate(), accrualPeriod.getEndDate(), provider).multipliedBy(rateB2[j]));
    }
    return pvbpdr;
  }

}
