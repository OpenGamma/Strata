/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata;

import java.time.LocalDate;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.strata.collect.id.StandardIdentifiable;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.config.MarketDataRule;
import com.opengamma.strata.engine.config.MarketDataRules;
import com.opengamma.strata.examples.data.ExampleData;
import com.opengamma.strata.function.marketdata.mapping.MarketDataMappingsBuilder;

/**
 * Contains utilities for using example market data from JSON resources.
 */
public final class ExampleMarketData {

  /**
   * Standard set of market data rules.
   */
  private static final MarketDataRules RULES =
      MarketDataRules.of(
          MarketDataRule.anyTarget(MarketDataMappingsBuilder.create().curveGroup("Default").build()));

  /**
   * Restricted constructor.
   */
  private ExampleMarketData() {
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the market data rules to be used with the example market data.
   * <p>
   * These rules identify a single curve group that all curves loaded from the
   * example data belong to.
   * 
   * @return the market data rules
   */
  public static MarketDataRules rules() {
    return RULES;
  }

  /**
   * Loads a time-series from a JSON resource.
   * <p>
   * The JSON resource must contain a Joda-bean serialized {@link LocalDateDoubleTimeSeries}
   * instance, and is expected to be available using a resource name of the form
   * <code>/timeseries/[id].json</code> where <code>[id]</code> is the string representation
   * of the identifier used to load the resource.
   * 
   * @param identifiable  provides the identifier of the time-series to load
   * @return the loaded time-series
   */
  public static LocalDateDoubleTimeSeries loadTimeSeries(StandardIdentifiable identifiable) {
    String resourceName = String.format("/timeseries/%s.json", identifiable.getStandardId().toString().toLowerCase());
    return ExampleData.loadFromJson(resourceName, LocalDateDoubleTimeSeries.class);
  }

  /**
   * Loads a yield curve from a JSON resource.
   * <p>
   * The JSON resource must contain a Joda-bean serialized {@link InterpolatedCurve} instance,
   * and is expected to be available using a resource name of the form
   * <code>/yieldcurve/[name]_[yyyy-mm-dd].json</code>, where <code>[name]</code> is the
   * curve name and <code>[yyyy-mm-dd]</code> is derived from the curve date.
   * 
   * @param curveDate  the curve date
   * @param curveName  the curve name
   * @return the loaded yield curve
   */
  public static YieldCurve loadYieldCurve(LocalDate curveDate, String curveName) {
    String resourceName = String.format("/yieldcurve/%s_%s.json", curveName.toLowerCase(), curveDate);
    InterpolatedCurve curve = ExampleData.loadFromJson(resourceName, InterpolatedCurve.class);
    return curve.toYieldCurve();
  }

}
