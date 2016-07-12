/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.Curve;

/**
 * Builder for the immutable rates provider.
 * 
 * @see ImmutableRatesProvider
 */
public final class ImmutableRatesProviderBuilder {

  /**
   * The valuation date.
   * All curves and other data items in this provider are calibrated for this date.
   */
  private final LocalDate valuationDate;
  /**
   * The provider of foreign exchange rates.
   * Conversions where both currencies are the same always succeed.
   */
  private FxRateProvider fxRateProvider = FxMatrix.empty();
  /**
   * The discount curves, defaulted to an empty map.
   * The curve data, predicting the future, associated with each currency.
   */
  private final Map<Currency, Curve> discountCurves = new HashMap<>();
  /**
   * The forward curves, defaulted to an empty map.
   * The curve data, predicting the future, associated with each index.
   * This is used for Ibor, Overnight and Price indices.
   */
  private final Map<Index, Curve> indexCurves = new HashMap<>();
  /**
   * The time-series, defaulted to an empty map.
   * The historic data associated with each index.
   */
  private final Map<Index, LocalDateDoubleTimeSeries> timeSeries = new HashMap<>();

  //-------------------------------------------------------------------------
  /**
   * Creates an instance specifying the valuation date.
   * 
   * @param valuationDate  the valuation date
   */
  ImmutableRatesProviderBuilder(LocalDate valuationDate) {
    this.valuationDate = ArgChecker.notNull(valuationDate, "valuationDate");
  }

  //-------------------------------------------------------------------------
  /**
   * Sets the FX rate provider.
   * 
   * @param fxRateProvider  the rate provider
   * @return this, for chaining
   */
  public ImmutableRatesProviderBuilder fxRateProvider(FxRateProvider fxRateProvider) {
    this.fxRateProvider = ArgChecker.notNull(fxRateProvider, "fxRateProvider");
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a discount curve to the provider.
   * <p>
   * This adds the specified discount curve to the provider.
   * This operates using {@link Map#put(Object, Object)} semantics using the currency as the key.
   * 
   * @param currency  the currency of the curve
   * @param discountCurve  the discount curve
   * @return this, for chaining
   */
  public ImmutableRatesProviderBuilder discountCurve(Currency currency, Curve discountCurve) {
    ArgChecker.notNull(currency, "currency");
    ArgChecker.notNull(discountCurve, "discountCurve");
    this.discountCurves.put(currency, discountCurve);
    return this;
  }

  /**
   * Adds discount curves to the provider.
   * <p>
   * This adds the specified discount curves to the provider.
   * This operates using {@link Map#putAll(Map)} semantics using the currency as the key.
   * 
   * @param discountCurves  the discount curves
   * @return this, for chaining
   */
  public ImmutableRatesProviderBuilder discountCurves(Map<Currency, ? extends Curve> discountCurves) {
    ArgChecker.notNull(discountCurves, "discountCurves");
    for (Entry<Currency, ? extends Curve> entry : discountCurves.entrySet()) {
      discountCurve(entry.getKey(), entry.getValue());
    }
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Adds an Ibor index forward curve to the provider.
   * <p>
   * This adds the specified forward curve to the provider.
   * This operates using {@link Map#put(Object, Object)} semantics using the index as the key.
   * 
   * @param index  the index of the curve
   * @param forwardCurve  the Ibor index forward curve
   * @return this, for chaining
   */
  public ImmutableRatesProviderBuilder iborIndexCurve(IborIndex index, Curve forwardCurve) {
    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(forwardCurve, "forwardCurve");
    this.indexCurves.put(index, forwardCurve);
    return this;
  }

  /**
   * Adds an Ibor index forward curve to the provider with associated time-series.
   * <p>
   * This adds the specified forward curve and time-series to the provider.
   * This operates using {@link Map#put(Object, Object)} semantics using the index as the key.
   * 
   * @param index  the index of the curve
   * @param forwardCurve  the index forward curve
   * @param timeSeries  the associated time-series
   * @return this, for chaining
   */
  public ImmutableRatesProviderBuilder iborIndexCurve(
      IborIndex index,
      Curve forwardCurve,
      LocalDateDoubleTimeSeries timeSeries) {

    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(forwardCurve, "forwardCurve");
    ArgChecker.notNull(timeSeries, "timeSeries");
    this.indexCurves.put(index, forwardCurve);
    this.timeSeries.put(index, timeSeries);
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Adds an Overnight index forward curve to the provider.
   * <p>
   * This adds the specified forward curve to the provider.
   * This operates using {@link Map#put(Object, Object)} semantics using the index as the key.
   * 
   * @param index  the index of the curve
   * @param forwardCurve  the Overnight index forward curve
   * @return this, for chaining
   */
  public ImmutableRatesProviderBuilder overnightIndexCurve(OvernightIndex index, Curve forwardCurve) {
    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(forwardCurve, "forwardCurve");
    this.indexCurves.put(index, forwardCurve);
    return this;
  }

  /**
   * Adds an Overnight index forward curve to the provider with associated time-series.
   * <p>
   * This adds the specified forward curve and time-series to the provider.
   * This operates using {@link Map#put(Object, Object)} semantics using the index as the key.
   * 
   * @param index  the index of the curve
   * @param forwardCurve  the index forward curve
   * @param timeSeries  the associated time-series
   * @return this, for chaining
   */
  public ImmutableRatesProviderBuilder overnightIndexCurve(
      OvernightIndex index,
      Curve forwardCurve,
      LocalDateDoubleTimeSeries timeSeries) {

    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(forwardCurve, "forwardCurve");
    ArgChecker.notNull(timeSeries, "timeSeries");
    this.indexCurves.put(index, forwardCurve);
    this.timeSeries.put(index, timeSeries);
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a Price index forward curve to the provider.
   * <p>
   * This adds the specified forward curve to the provider.
   * This operates using {@link Map#put(Object, Object)} semantics using the index as the key.
   * 
   * @param index  the index of the curve
   * @param forwardCurve  the Price index forward curve
   * @return this, for chaining
   */
  public ImmutableRatesProviderBuilder priceIndexCurve(PriceIndex index, Curve forwardCurve) {
    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(forwardCurve, "forwardCurve");
    this.indexCurves.put(index, forwardCurve);
    return this;
  }

  /**
   * Adds an index forward curve to the provider with associated time-series.
   * <p>
   * This adds the specified forward curve and time-series to the provider.
   * This operates using {@link Map#put(Object, Object)} semantics using the index as the key.
   * 
   * @param index  the index of the curve
   * @param forwardCurve  the index forward curve
   * @param timeSeries  the associated time-series
   * @return this, for chaining
   */
  public ImmutableRatesProviderBuilder priceIndexCurve(
      PriceIndex index,
      Curve forwardCurve,
      LocalDateDoubleTimeSeries timeSeries) {

    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(forwardCurve, "forwardCurve");
    ArgChecker.notNull(timeSeries, "timeSeries");
    this.indexCurves.put(index, forwardCurve);
    this.timeSeries.put(index, timeSeries);
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Adds an index forward curve to the provider.
   * <p>
   * This adds the specified forward curve to the provider.
   * This is used for Ibor, Overnight and Price indices.
   * This operates using {@link Map#put(Object, Object)} semantics using the index as the key.
   * 
   * @param index  the index of the curve
   * @param forwardCurve  the Ibor index forward curve
   * @return this, for chaining
   */
  public ImmutableRatesProviderBuilder indexCurve(Index index, Curve forwardCurve) {
    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(forwardCurve, "forwardCurve");
    if (index instanceof IborIndex || index instanceof OvernightIndex || index instanceof PriceIndex) {
      this.indexCurves.put(index, forwardCurve);
    } else {
      throw new IllegalArgumentException("Unsupported index: " + index);
    }
    return this;
  }

  /**
   * Adds an index forward curve to the provider with associated time-series.
   * <p>
   * This adds the specified forward curve to the provider.
   * This is used for Ibor, Overnight and Price indices.
   * This operates using {@link Map#put(Object, Object)} semantics using the index as the key.
   * 
   * @param index  the index of the curve
   * @param forwardCurve  the Ibor index forward curve
   * @param timeSeries  the time-series
   * @return this, for chaining
   */
  public ImmutableRatesProviderBuilder indexCurve(Index index, Curve forwardCurve, LocalDateDoubleTimeSeries timeSeries) {
    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(forwardCurve, "forwardCurve");
    if (index instanceof IborIndex || index instanceof OvernightIndex || index instanceof PriceIndex) {
      this.indexCurves.put(index, forwardCurve);
      this.timeSeries.put(index, timeSeries);
    } else {
      throw new IllegalArgumentException("Unsupported index: " + index);
    }
    return this;
  }

  /**
   * Adds index forward curves to the provider with associated time-series.
   * <p>
   * This adds the specified index forward curves to the provider.
   * This is used for Ibor, Overnight and Price indices.
   * This operates using {@link Map#putAll(Map)} semantics using the index as the key.
   * 
   * @param indexCurves  the index forward curves
   * @return this, for chaining
   */
  public ImmutableRatesProviderBuilder indexCurves(Map<? extends Index, ? extends Curve> indexCurves) {
    ArgChecker.noNulls(indexCurves, "indexCurves");
    for (Entry<? extends Index, ? extends Curve> entry : indexCurves.entrySet()) {
      indexCurve(entry.getKey(), entry.getValue());
    }
    return this;
  }

  /**
   * Adds index forward curves to the provider with associated time-series.
   * <p>
   * This adds the specified index forward curves to the provider.
   * This is used for Ibor, Overnight and Price indices.
   * This operates using {@link Map#putAll(Map)} semantics using the index as the key.
   * 
   * @param indexCurves  the index forward curves
   * @param timeSeries  the associated time-series
   * @return this, for chaining
   */
  public ImmutableRatesProviderBuilder indexCurves(
      Map<? extends Index, ? extends Curve> indexCurves,
      Map<? extends Index, LocalDateDoubleTimeSeries> timeSeries) {

    ArgChecker.noNulls(indexCurves, "indexCurves");
    for (Entry<? extends Index, ? extends Curve> entry : indexCurves.entrySet()) {
      Index index = entry.getKey();
      LocalDateDoubleTimeSeries ts = timeSeries.get(index);
      ts = (ts != null ? ts : LocalDateDoubleTimeSeries.empty());
      indexCurve(entry.getKey(), entry.getValue(), ts);
    }
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a time-series to the provider.
   * <p>
   * This adds the specified time-series to the provider.
   * This operates using {@link Map#put(Object, Object)} semantics using the index as the key.
   * 
   * @param index  the FX index
   * @param timeSeries  the FX index time-series
   * @return this, for chaining
   */
  public ImmutableRatesProviderBuilder timeSeries(Index index, LocalDateDoubleTimeSeries timeSeries) {
    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(timeSeries, "timeSeries");
    this.timeSeries.put(index, timeSeries);
    return this;
  }

  /**
   * Adds time-series to the provider.
   * <p>
   * This adds the specified time-series to the provider.
   * This operates using {@link Map#putAll(Map)} semantics using the index as the key.
   * 
   * @param timeSeries  the FX index time-series
   * @return this, for chaining
   */
  public ImmutableRatesProviderBuilder timeSeries(Map<? extends Index, LocalDateDoubleTimeSeries> timeSeries) {
    ArgChecker.noNulls(timeSeries, "timeSeries");
    this.timeSeries.putAll(timeSeries);
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Completes the builder, returning the provider.
   * 
   * @return the provider
   */
  public ImmutableRatesProvider build() {
    return new ImmutableRatesProvider(
        valuationDate,
        fxRateProvider,
        discountCurves,
        indexCurves,
        timeSeries);
  }

}
