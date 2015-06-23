/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.market.FxRateId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.config.MarketDataRule;
import com.opengamma.strata.engine.config.MarketDataRules;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.engine.marketdata.BaseMarketDataBuilder;
import com.opengamma.strata.examples.marketdata.curve.RatesCurvesCsvLoader;
import com.opengamma.strata.examples.marketdata.timeseries.FixingSeriesCsvLoader;
import com.opengamma.strata.function.marketdata.mapping.MarketDataMappingsBuilder;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.id.QuoteId;
import com.opengamma.strata.market.id.RateCurveId;

/**
 * Builds a market data snapshot from user-editable files in a prescribed directory structure.
 * <p>
 * Descendants of this class provide the ability to source this directory structure from any
 * location.
 * <p>
 * The directory structure must look like:
 * <ul>
 *   <li>root
 *   <ul>
 *     <li>curves
 *     <ul>
 *       <li>groups.csv
 *       <li>settings.csv
 *       <li>one or more curve CSV files
 *     </ul>
 *     <li>historical-fixings
 *     <ul>
 *       <li>one or more time-series CSV files
 *     </ul>
 *   </ul>
 * </ul>
 */
public abstract class MarketDataBuilder {

  private static final Logger s_logger = LoggerFactory.getLogger(MarketDataBuilder.class);

  /** The name of the subdirectory containing historical fixings. */
  private static final String HISTORICAL_FIXINGS_DIR = "historical-fixings";

  /** The name of the subdirectory containing calibrated rates curves. */
  private static final String CURVES_DIR = "curves";
  /** The name of the curve groups file. */
  private static final String CURVES_GROUPS_FILE = "groups.csv";
  /** The name of the curve settings file. */
  private static final String CURVES_SETTINGS_FILE = "settings.csv";

  /** The name of the subdirectory containing simple market quotes. */
  private static final String QUOTES_DIR = "quotes";
  /** The name of the quotes file. */
  private static final String QUOTES_FILE = "quotes.csv";

  //-------------------------------------------------------------------------
  /**
   * Creates an instance from a given classpath resource root location using the class loader
   * which created this class.
   * <p>
   * This is designed to handle resource roots which may physically correspond to a directory on
   * disk, or be located within a jar file.
   * 
   * @param resourceRoot  the resource root path
   * @return the market data builder
   */
  public static MarketDataBuilder ofResource(String resourceRoot) {
    return ofResource(resourceRoot, MarketDataBuilder.class.getClassLoader());
  }

  /**
   * Creates an instance from a given classpath resource root location, using the given class loader
   * to find the resource.
   * <p>
   * This is designed to handle resource roots which may physically correspond to a directory on
   * disk, or be located within a jar file.
   * 
   * @param resourceRoot  the resource root path
   * @param classLoader  the class loader with which to find the resource
   * @return the market data builder
   */
  public static MarketDataBuilder ofResource(String resourceRoot, ClassLoader classLoader) {
    String qualifiedResourceRoot = resourceRoot.startsWith(File.separator) ? resourceRoot.substring(1) : resourceRoot;
    if (!qualifiedResourceRoot.endsWith(File.separator)) {
      qualifiedResourceRoot += File.separator;
    }
    URL url = classLoader.getResource(qualifiedResourceRoot);
    if (url == null) {
      throw new IllegalArgumentException(
          Messages.format("Resource not found: {}", resourceRoot));
    }
    if (url.getProtocol() != null && "jar".equals(url.getProtocol().toLowerCase())) {
      // Inside a JAR
      int classSeparatorIdx = url.getFile().indexOf("!");
      if (classSeparatorIdx == -1) {
        throw new IllegalArgumentException(
            Messages.format("Unexpected JAR file URL: {}", url));
      }
      String jarPath = url.getFile().substring("file:".length(), classSeparatorIdx);
      File jarFile;
      try {
        jarFile = new File(jarPath);
      } catch (Exception e) {
        throw new IllegalArgumentException(
            Messages.format("Unable to create file for JAR: {}", jarPath), e);
      }
      return new JarMarketDataBuilder(jarFile, resourceRoot);
    } else {
      // Resource is on disk
      File file;
      try {
        file = new File(url.toURI());
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException(
            Messages.format("Unexpected file location: {}", url), e);
      }
      return new DirectoryMarketDataBuilder(file.toPath());
    }
  }

  /**
   * Creates an instance from a given directory root.
   * 
   * @param rootPath  the root directory
   * @return the market data builder
   */
  public static MarketDataBuilder ofPath(Path rootPath) {
    return new DirectoryMarketDataBuilder(rootPath);
  }

  //-------------------------------------------------------------------------
  /**
   * Builds a market data snapshot from this environment.
   * 
   * @param marketDataDate  the date of the market data
   * @return the snapshot
   */
  public BaseMarketData buildSnapshot(LocalDate marketDataDate) {
    BaseMarketDataBuilder builder = BaseMarketData.builder(marketDataDate);
    loadFixingSeries(builder);
    loadRatesCurves(builder, marketDataDate);
    loadQuotes(builder, marketDataDate);
    loadFxRates(builder);
    return builder.build();
  }

  /**
   * Gets the market data rules to use with this environment.
   * 
   * @return the market data rules
   */
  public MarketDataRules rules() {
    // TODO - should be loaded from a CSV file - format to be defined
    return MarketDataRules.of(
        MarketDataRule.anyTarget(
            MarketDataMappingsBuilder.create()
                .curveGroup(CurveGroupName.of("Default"))
                .build()));
  }
  
  /**
   * Gets all rates curves.
   * 
   * @return the map of all rates curves
   */
  public Map<LocalDate, Map<RateCurveId, Curve>> loadAllRatesCurves() {
    if (!subdirectoryExists(CURVES_DIR)) {
      throw new IllegalArgumentException(
          Messages.format("No rates curves directory found"));
    }

    ResourceLocator curveGroupsResource = getResource(CURVES_DIR, CURVES_GROUPS_FILE);
    if (curveGroupsResource == null) {
      throw new IllegalArgumentException(
          Messages.format("Unable to load rates curves: curve groups file not found at {}/{}", CURVES_DIR, CURVES_GROUPS_FILE));
    }

    ResourceLocator curveSettingsResource = getResource(CURVES_DIR, CURVES_SETTINGS_FILE);
    if (curveSettingsResource == null) {
      throw new IllegalArgumentException(
          Messages.format("Unable to load rates curves: curve settings file not found at {}/{}", CURVES_DIR, CURVES_SETTINGS_FILE));
    }
    
    return RatesCurvesCsvLoader.loadAllCurves(curveGroupsResource, curveSettingsResource, getRatesCurvesResources());
  }

  //-------------------------------------------------------------------------
  private void loadFixingSeries(BaseMarketDataBuilder builder) {
    if (!subdirectoryExists(HISTORICAL_FIXINGS_DIR)) {
      s_logger.debug("No historical fixings directory found");
      return;
    }
    try {
      Collection<ResourceLocator> fixingSeriesResources = getAllResources(HISTORICAL_FIXINGS_DIR);
      Map<ObservableId, LocalDateDoubleTimeSeries> fixingSeries = FixingSeriesCsvLoader.loadFixingSeries(fixingSeriesResources);
      builder.addAllTimeSeries(fixingSeries);
    } catch (Exception e) {
      s_logger.error("Error loading fixing series", e);
    }
  }

  private void loadRatesCurves(BaseMarketDataBuilder builder, LocalDate marketDataDate) {
    if (!subdirectoryExists(CURVES_DIR)) {
      s_logger.debug("No rates curves directory found");
      return;
    }

    ResourceLocator curveGroupsResource = getResource(CURVES_DIR, CURVES_GROUPS_FILE);
    if (curveGroupsResource == null) {
      s_logger.error("Unable to load rates curves: curve groups file not found at {}/{}", CURVES_DIR, CURVES_GROUPS_FILE);
      return;
    }

    ResourceLocator curveSettingsResource = getResource(CURVES_DIR, CURVES_SETTINGS_FILE);
    if (curveSettingsResource == null) {
      s_logger.error("Unable to load rates curves: curve settings file not found at {}/{}", CURVES_DIR, CURVES_SETTINGS_FILE);
      return;
    }

    try {
      Collection<ResourceLocator> curvesResources = getRatesCurvesResources();
      Map<RateCurveId, Curve> ratesCurves = RatesCurvesCsvLoader
          .loadCurves(curveGroupsResource, curveSettingsResource, curvesResources, marketDataDate);
      builder.addAllValues(ratesCurves);
    } catch (Exception e) {
      s_logger.error("Error loading rates curves", e);
    }
  }

  // load quotes
  private void loadQuotes(BaseMarketDataBuilder builder, LocalDate marketDataDate) {
    if (!subdirectoryExists(QUOTES_DIR)) {
      s_logger.debug("No quotes directory found");
      return;
    }

    ResourceLocator quotesResource = getResource(QUOTES_DIR, QUOTES_FILE);
    if (quotesResource == null) {
      s_logger.error("Unable to load quotes: quotes file not found at {}/{}", QUOTES_DIR, QUOTES_FILE);
      return;
    }

    try {
      Map<QuoteId, Double> quotes = QuotesCsvLoader.loadQuotes(quotesResource, marketDataDate);
      builder.addAllValues(quotes);

    } catch (Exception ex) {
      s_logger.error("Error loading quotes", ex);
    }
  }

  private void loadFxRates(BaseMarketDataBuilder builder) {
    // TODO - load from CSV file - format to be defined
    builder.addValue(FxRateId.of(Currency.GBP, Currency.USD), FxRate.of(Currency.GBP, Currency.USD, 1.61));
  }
  
  //-------------------------------------------------------------------------
  private Collection<ResourceLocator> getRatesCurvesResources() {
    return getAllResources(CURVES_DIR).stream()
        .filter(res ->
            !res.getLocator().endsWith(CURVES_GROUPS_FILE) && !res.getLocator().endsWith(CURVES_SETTINGS_FILE))
        .collect(Collectors.toList());
  }

  //-------------------------------------------------------------------------
  /**
   * Gets all available resources from a given subdirectory.
   * 
   * @param subdirectoryName  the name of the subdirectory
   * @return a collection of locators for the resources in the subdirectory
   */
  protected abstract Collection<ResourceLocator> getAllResources(String subdirectoryName);

  /**
   * Gets a specific resource from a given subdirectory.
   * 
   * @param subdirectoryName  the name of the subdirectory
   * @param resourceName  the name of the resource
   * @return a locator for the requested resource
   */
  protected abstract ResourceLocator getResource(String subdirectoryName, String resourceName);

  /**
   * Checks whether a specific subdirectory exists.
   * 
   * @param subdirectoryName  the name of the subdirectory
   * @return whether the subdirectory exists
   */
  protected abstract boolean subdirectoryExists(String subdirectoryName);

}
