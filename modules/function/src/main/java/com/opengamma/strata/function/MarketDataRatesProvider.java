/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function;

import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.OptionalDouble;

import org.joda.beans.JodaBeanUtils;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.marketdata.SingleCalculationMarketData;
import com.opengamma.strata.marketdata.key.DiscountingCurveKey;
import com.opengamma.strata.marketdata.key.FxRateKey;
import com.opengamma.strata.marketdata.key.IndexCurveKey;
import com.opengamma.strata.marketdata.key.IndexRateKey;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.PricingException;
import com.opengamma.strata.pricer.sensitivity.CurveParameterSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

/**
 * A rates provider based on market data from the engine.
 * <p>
 * This provides the environmental information against which pricing occurs.
 * This includes FX rates, discount factors and forward curves.
 */
public final class MarketDataRatesProvider
    implements RatesProvider, Serializable {

  /**
   * Serialization version.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The day count applicable to the models.
   */
  private final DayCount dayCount = ACT_ACT_ISDA;
  /**
   * The set of market data for the calculations.
   */
  private final SingleCalculationMarketData marketData;

  public MarketDataRatesProvider(SingleCalculationMarketData marketData) {
    JodaBeanUtils.notNull(marketData, "marketData");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    this.marketData = marketData;
  }

  @Override
  public LocalDate getValuationDate() {
    return marketData.getValuationDate();
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDateDoubleTimeSeries timeSeries(Index index) {
    ArgChecker.notNull(index, "index");
    LocalDateDoubleTimeSeries series = marketData.getTimeSeries(IndexRateKey.of(index));
    if (series == null) {
      throw new IllegalArgumentException("Unknown index: " + index.getName());
    }
    return series;
  }

  //-------------------------------------------------------------------------
  @Override
  public double fxRate(Currency baseCurrency, Currency counterCurrency) {
    ArgChecker.notNull(baseCurrency, "baseCurrency");
    ArgChecker.notNull(counterCurrency, "counterCurrency");
    if (baseCurrency.equals(counterCurrency)) {
      return 1d;
    }
    return marketData.getValue(FxRateKey.of(baseCurrency, counterCurrency));
  }

  //-------------------------------------------------------------------------
  @Override
  public double discountFactor(Currency currency, LocalDate date) {
    ArgChecker.notNull(currency, "currency");
    ArgChecker.notNull(date, "date");
    YieldCurve curve = marketData.getValue(DiscountingCurveKey.of(currency));
    return curve.getDiscountFactor(relativeTime(date));
  }

  @Override
  public PointSensitivityBuilder discountFactorZeroRateSensitivity(
      Currency currency, LocalDate date) {
    // TODO implement MarketDataRatesProvider.discountFactorZeroRateSensitivity
    throw new UnsupportedOperationException("discountFactorZeroRateSensitivity not implemented");
  }

  //-------------------------------------------------------------------------
  @Override
  public double fxIndexRate(FxIndex index, Currency baseCurrency, LocalDate fixingDate) {
    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(baseCurrency, "baseCurrency");
    ArgChecker.notNull(fixingDate, "fixingDate");
    ArgChecker.isTrue(
        index.getCurrencyPair().contains(baseCurrency),
        "Currency {} invalid for FxIndex {}", baseCurrency, index);
    // historic rate
    boolean inverse = baseCurrency.equals(index.getCurrencyPair().getCounter());
    if (!fixingDate.isAfter(marketData.getValuationDate())) {
      OptionalDouble fixedRate = timeSeries(index).get(fixingDate);
      if (fixedRate.isPresent()) {
        // if the index is the inverse of the desired pair, then invert it
        double fxIndexRate = fixedRate.getAsDouble();
        return (inverse ? 1d / fxIndexRate : fxIndexRate);
      } else if (fixingDate.isBefore(marketData.getValuationDate())) { // the fixing is required
        throw new PricingException(Messages.format("Unable to get fixing for {} on date {}", index, fixingDate));
      }
    }
    // forward rate
    // use the specified base currency to determine the desired currency pair
    // then derive rate from discount factors based off desired currency pair, not that of the index
    CurrencyPair pair = inverse ? index.getCurrencyPair().inverse() : index.getCurrencyPair();
    double maturity = relativeTime(index.calculateMaturityFromFixing(fixingDate));
    YieldCurve baseDiscountingCurve = marketData.getValue(DiscountingCurveKey.of(pair.getBase()));
    YieldCurve counterDiscountingCurve = marketData.getValue(DiscountingCurveKey.of(pair.getCounter()));
    double dfCcyBaseAtMaturity = baseDiscountingCurve.getDiscountFactor(maturity);
    double dfCcyCounterAtMaturity = counterDiscountingCurve.getDiscountFactor(maturity);
    return fxRate(pair) * (dfCcyBaseAtMaturity / dfCcyCounterAtMaturity);
  }

  //-------------------------------------------------------------------------
  @Override
  public double iborIndexRate(IborIndex index, LocalDate fixingDate) {
    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(fixingDate, "fixingDate");
    // historic rate
    if (!fixingDate.isAfter(marketData.getValuationDate())) {
      OptionalDouble fixedRate = timeSeries(index).get(fixingDate);
      if (fixedRate.isPresent()) {
        return fixedRate.getAsDouble();
      } else if (fixingDate.isBefore(marketData.getValuationDate())) { // the fixing is required
        throw new PricingException(Messages.format("Unable to get fixing for {} on date {}", index, fixingDate));
      }
    }
    // forward rate
    LocalDate fixingStartDate = index.calculateEffectiveFromFixing(fixingDate);
    LocalDate fixingEndDate = index.calculateMaturityFromEffective(fixingStartDate);
    YieldCurve curve = marketData.getValue(IndexCurveKey.of(index));
    double fixingYearFraction = index.getDayCount().yearFraction(fixingStartDate, fixingEndDate);
    double startTime = relativeTime(fixingStartDate);
    double endTime = relativeTime(fixingEndDate);
    return (curve.getDiscountFactor(startTime) / curve.getDiscountFactor(endTime) - 1) / fixingYearFraction;
  }

  @Override
  public PointSensitivityBuilder iborIndexRateSensitivity(IborIndex index, LocalDate fixingDate) {
    // TODO implement MarketDataRatesProvider.iborIndexRateSensitivity
    throw new UnsupportedOperationException("iborIndexRateSensitivity not implemented");
  }

  //-------------------------------------------------------------------------
  @Override
  public double overnightIndexRate(OvernightIndex index, LocalDate fixingDate) {
    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(fixingDate, "fixingDate");
    LocalDate publicationDate = index.calculatePublicationFromFixing(fixingDate);
    if (!publicationDate.isAfter(marketData.getValuationDate())) {
      OptionalDouble fixedRate = timeSeries(index).get(fixingDate);
      if (fixedRate.isPresent()) {
        return fixedRate.getAsDouble();
      } else if (publicationDate.isBefore(marketData.getValuationDate())) { // the fixing is required
        throw new PricingException(Messages.format("Unable to get fixing for {} on date {}", index, fixingDate));
      }
    }
    // forward rate
    LocalDate fixingStartDate = index.calculateEffectiveFromFixing(fixingDate);
    LocalDate fixingEndDate = index.calculateMaturityFromEffective(fixingStartDate);
    YieldCurve curve = marketData.getValue(IndexCurveKey.of(index));
    double fixingYearFraction = index.getDayCount().yearFraction(fixingStartDate, fixingEndDate);
    double startTime = relativeTime(fixingStartDate);
    double endTime = relativeTime(fixingEndDate);
    return (curve.getDiscountFactor(startTime) / curve.getDiscountFactor(endTime) - 1) / fixingYearFraction;
  }

  @Override
  public PointSensitivityBuilder overnightIndexRateSensitivity(OvernightIndex index, LocalDate fixingDate) {
    // TODO implement MarketDataRatesProvider.overnightIndexRateSensitivity
    throw new UnsupportedOperationException("overnightIndexRateSensitivity not implemented");
  }

  //-------------------------------------------------------------------------
  @Override
  public double overnightIndexRatePeriod(OvernightIndex index, LocalDate startDate, LocalDate endDate) {
    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(startDate, "startDate");
    ArgChecker.notNull(endDate, "endDate");
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate");
    ArgChecker.inOrderOrEqual(marketData.getValuationDate(), startDate, "valuationDate", "startDate");
    YieldCurve curve = marketData.getValue(IndexCurveKey.of(index));
    double fixingYearFraction = index.getDayCount().yearFraction(startDate, endDate);
    double startTime = relativeTime(startDate);
    double endTime = relativeTime(endDate);
    return (curve.getDiscountFactor(startTime) / curve.getDiscountFactor(endTime) - 1) / fixingYearFraction;
  }

  @Override
  public PointSensitivityBuilder overnightIndexRatePeriodSensitivity(
      OvernightIndex index, LocalDate startDate, LocalDate endDate) {
    // TODO implement MarketDataRatesProvider.overnightIndexRatePeriodSensitivity
    throw new UnsupportedOperationException("overnightIndexRatePeriodSensitivity not implemented");
  }

  //-------------------------------------------------------------------------

  @Override
  public CurveParameterSensitivity parameterSensitivity(PointSensitivities pointSensitivities) {
    // TODO: Implement before creating PV01 results
    throw new UnsupportedOperationException();
  }

  //-------------------------------------------------------------------------
  @Override
  public double relativeTime(LocalDate date) {
    ArgChecker.notNull(date, "date");
    return (date.isBefore(marketData.getValuationDate()) ?
        -dayCount.yearFraction(date, marketData.getValuationDate()) :
        dayCount.yearFraction(marketData.getValuationDate(), date));
  }
}
