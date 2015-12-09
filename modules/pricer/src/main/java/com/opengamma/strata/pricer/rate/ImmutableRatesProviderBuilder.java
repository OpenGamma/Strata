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
import com.opengamma.strata.market.value.PriceIndexValues;

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
   */
  private final Map<Index, Curve> indexCurves = new HashMap<>();
  /**
   * The price index values, defaulted to an empty map.
   * The curve data, predicting the future, associated with each index.
   */
  private final Map<PriceIndex, PriceIndexValues> priceIndexValues = new HashMap<>();
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
  public ImmutableRatesProviderBuilder discountCurves(Map<Currency, Curve> discountCurves) {
    ArgChecker.notNull(discountCurves, "discountCurves");
    for (Entry<Currency, Curve> entry : discountCurves.entrySet()) {
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
    ArgChecker.notNull(index, "currency");
    ArgChecker.notNull(forwardCurve, "forwardCurve");
    this.indexCurves.put(index, forwardCurve);
    return this;
  }

  /**
   * Adds a index forward curve to the provider with associated time-series.
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

    ArgChecker.notNull(index, "currency");
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
    ArgChecker.notNull(index, "currency");
    ArgChecker.notNull(forwardCurve, "forwardCurve");
    this.indexCurves.put(index, forwardCurve);
    return this;
  }

  /**
   * Adds a index forward curve to the provider with associated time-series.
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

    ArgChecker.notNull(index, "currency");
    ArgChecker.notNull(forwardCurve, "forwardCurve");
    ArgChecker.notNull(timeSeries, "timeSeries");
    this.indexCurves.put(index, forwardCurve);
    this.timeSeries.put(index, timeSeries);
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Adds index forward curves to the provider.
   * <p>
   * This adds the specified index forward curves to the provider.
   * This operates using {@link Map#putAll(Map)} semantics using the index as the key.
   * 
   * @param indexCurves  the index forward curves
   * @return this, for chaining
   */
  public ImmutableRatesProviderBuilder indexCurves(Map<Index, Curve> indexCurves) {
    ArgChecker.noNulls(indexCurves, "indexCurves");
    for (Entry<Index, Curve> entry : indexCurves.entrySet()) {
      Index index = entry.getKey();
      if (index instanceof IborIndex) {
        iborIndexCurve((IborIndex) index, entry.getValue());
      } else if (index instanceof OvernightIndex) {
        overnightIndexCurve((OvernightIndex) index, entry.getValue());
      } else {
        throw new IllegalArgumentException("Unknown index type: " + index);
      }
    }
    return this;
  }

  /**
   * Adds index forward curves to the provider with associated time-series.
   * <p>
   * This adds the specified index forward curves to the provider.
   * This operates using {@link Map#putAll(Map)} semantics using the index as the key.
   * 
   * @param indexCurves  the index forward curves
   * @param timeSeries  the associated time-series
   * @return this, for chaining
   */
  public ImmutableRatesProviderBuilder indexCurves(
      Map<Index, Curve> indexCurves,
      Map<Index, LocalDateDoubleTimeSeries> timeSeries) {

    ArgChecker.noNulls(indexCurves, "indexCurves");
    for (Entry<Index, Curve> entry : indexCurves.entrySet()) {
      Index index = entry.getKey();
      LocalDateDoubleTimeSeries ts = timeSeries.get(index);
      ts = (ts != null ? ts : LocalDateDoubleTimeSeries.empty());
      if (index instanceof IborIndex) {
        iborIndexCurve((IborIndex) index, entry.getValue(), ts);
      } else if (index instanceof OvernightIndex) {
        overnightIndexCurve((OvernightIndex) index, entry.getValue(), ts);
      } else {
        throw new IllegalArgumentException("Unknown index type: " + index);
      }
    }
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Adds price index values to the provider.
   * <p>
   * This adds the specified price index values to the provider.
   * The valuation date of the price index values must match the valuation date of the builder.
   * This operates using {@link Map#put(Object, Object)} semantics using the index as the key.
   * 
   * @param priceIndexValues  the price index values
   * @return this, for chaining
   * @throws IllegalArgumentException if the valuation date does not match
   */
  public ImmutableRatesProviderBuilder priceIndexValues(PriceIndexValues... priceIndexValues) {
    ArgChecker.notNull(priceIndexValues, "priceIndexValues");
    for (PriceIndexValues piv : priceIndexValues) {
      checkValuationDate(piv.getValuationDate());
      this.priceIndexValues.put(piv.getIndex(), piv);
    }
    return this;
  }

  /**
   * Adds price index values to the provider.
   * <p>
   * This adds the specified price index values to the provider.
   * The valuation date of the price index values must match the valuation date of the builder.
   * This operates using {@link Map#putAll(Map)} semantics using the index as the key.
   * 
   * @param priceIndexValues  the price index values
   * @return this, for chaining
   * @throws IllegalArgumentException if the valuation date does not match
   */
  public ImmutableRatesProviderBuilder priceIndexValues(Map<PriceIndex, PriceIndexValues> priceIndexValues) {
    ArgChecker.notNull(priceIndexValues, "priceIndexValues");
    for (PriceIndexValues piv : priceIndexValues.values()) {
      checkValuationDate(piv.getValuationDate());
      this.priceIndexValues.put(piv.getIndex(), piv);
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
  public ImmutableRatesProviderBuilder timeSeries(Map<Index, LocalDateDoubleTimeSeries> timeSeries) {
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
        priceIndexValues,
        timeSeries);
  }

  //-------------------------------------------------------------------------
  private void checkValuationDate(LocalDate inputValuationDate) {
    ArgChecker.isTrue(
        valuationDate.equals(inputValuationDate),
        "Valuation date differs, {} and {}", valuationDate, inputValuationDate);
  }

}
