/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDate;

import org.joda.beans.ser.JodaBeanSer;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.strata.collect.id.StandardIdentifiable;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.config.MarketDataRules;
import com.opengamma.strata.engine.config.SimpleMarketDataRules;
import com.opengamma.strata.engine.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.examples.finance.SwapPricingExample;
import com.opengamma.strata.finance.rate.swap.SwapTrade;

/**
 * Contains utilities for using example market data from JSON resources.
 */
public final class ExampleMarketData {

  private static final MarketDataRules RULES = SimpleMarketDataRules.builder()
      .addMappings(SwapTrade.class, MarketDataMappings.builder().curveGroup("Default").build())
      .build();
  
  /**
   * Restricted constructor.
   */
  private ExampleMarketData() {
  }
  
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
    return loadFromJson(resourceName, LocalDateDoubleTimeSeries.class);
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
    InterpolatedCurve curve = loadFromJson(resourceName, InterpolatedCurve.class);
    return curve.toYieldCurve();
  }
  
  private static <T> T loadFromJson(String resourceName, Class<T> clazz) {
    InputStream tsResource = SwapPricingExample.class.getResourceAsStream(resourceName);
    Reader tsReader = new InputStreamReader(tsResource);
    return JodaBeanSer.COMPACT.jsonReader().read(tsReader, clazz);
  }
  
}
