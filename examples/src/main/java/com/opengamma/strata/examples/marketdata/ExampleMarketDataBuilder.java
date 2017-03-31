/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.FxRateId;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.ImmutableMarketDataBuilder;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.loader.csv.FixingSeriesCsvLoader;
import com.opengamma.strata.loader.csv.QuotesCsvLoader;
import com.opengamma.strata.loader.csv.RatesCurvesCsvLoader;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;

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
public abstract class ExampleMarketDataBuilder {

  private static final Logger log = LoggerFactory.getLogger(ExampleMarketDataBuilder.class);

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
  public static ExampleMarketDataBuilder ofResource(String resourceRoot) {
    return ofResource(resourceRoot, ExampleMarketDataBuilder.class.getClassLoader());
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
  public static ExampleMarketDataBuilder ofResource(String resourceRoot, ClassLoader classLoader) {
    // classpath resources are forward-slash separated
    String qualifiedRoot = resourceRoot;
    qualifiedRoot = qualifiedRoot.startsWith("/") ? qualifiedRoot.substring(1) : qualifiedRoot;
    qualifiedRoot = qualifiedRoot.startsWith("\\") ? qualifiedRoot.substring(1) : qualifiedRoot;
    qualifiedRoot = qualifiedRoot.endsWith("/") ? qualifiedRoot : qualifiedRoot + "/";
    URL url = classLoader.getResource(qualifiedRoot);
    if (url == null) {
      throw new IllegalArgumentException(Messages.format("Classpath resource not found: {}", qualifiedRoot));
    }
    if (url.getProtocol() != null && "jar".equals(url.getProtocol().toLowerCase(Locale.ENGLISH))) {
      // Inside a JAR
      int classSeparatorIdx = url.getFile().indexOf("!");
      if (classSeparatorIdx == -1) {
        throw new IllegalArgumentException(Messages.format("Unexpected JAR file URL: {}", url));
      }
      String jarPath = url.getFile().substring("file:".length(), classSeparatorIdx);
      File jarFile;
      try {
        jarFile = new File(jarPath);
      } catch (Exception e) {
        throw new IllegalArgumentException(Messages.format("Unable to create file for JAR: {}", jarPath), e);
      }
      return new JarMarketDataBuilder(jarFile, resourceRoot);
    } else {
      // Resource is on disk
      File file;
      try {
        file = new File(url.toURI());
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException(Messages.format("Unexpected file location: {}", url), e);
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
  public static ExampleMarketDataBuilder ofPath(Path rootPath) {
    return new DirectoryMarketDataBuilder(rootPath);
  }

  //-------------------------------------------------------------------------
  /**
   * Builds a market data snapshot from this environment.
   * 
   * @param marketDataDate  the date of the market data
   * @return the snapshot
   */
  public ImmutableMarketData buildSnapshot(LocalDate marketDataDate) {
    ImmutableMarketDataBuilder builder = ImmutableMarketData.builder(marketDataDate);
    loadFixingSeries(builder);
    loadRatesCurves(builder, marketDataDate);
    loadQuotes(builder, marketDataDate);
    loadFxRates(builder);
    return builder.build();
  }

  /**
   * Gets the rates market lookup to use with this environment.
   * 
   * @param marketDataDate  the date of the market data
   * @return the rates lookup
   */
  public RatesMarketDataLookup ratesLookup(LocalDate marketDataDate) {
    SortedMap<LocalDate, CurveGroup> curves = loadAllRatesCurves();
    return RatesMarketDataLookup.of(curves.get(marketDataDate));
  }

  /**
   * Gets all rates curves.
   * 
   * @return the map of all rates curves
   */
  public SortedMap<LocalDate, CurveGroup> loadAllRatesCurves() {
    if (!subdirectoryExists(CURVES_DIR)) {
      throw new IllegalArgumentException("No rates curves directory found");
    }
    ResourceLocator curveGroupsResource = getResource(CURVES_DIR, CURVES_GROUPS_FILE);
    if (curveGroupsResource == null) {
      throw new IllegalArgumentException(Messages.format(
          "Unable to load rates curves: curve groups file not found at {}/{}", CURVES_DIR, CURVES_GROUPS_FILE));
    }
    ResourceLocator curveSettingsResource = getResource(CURVES_DIR, CURVES_SETTINGS_FILE);
    if (curveSettingsResource == null) {
      throw new IllegalArgumentException(Messages.format(
          "Unable to load rates curves: curve settings file not found at {}/{}", CURVES_DIR, CURVES_SETTINGS_FILE));
    }
    ListMultimap<LocalDate, CurveGroup> curveGroups =
        RatesCurvesCsvLoader.loadAllDates(curveGroupsResource, curveSettingsResource, getRatesCurvesResources());

    // There is only one curve group in the market data file so this will always succeed
    Map<LocalDate, CurveGroup> curveGroupMap = Maps.transformValues(curveGroups.asMap(), groups -> groups.iterator().next());
    return new TreeMap<>(curveGroupMap);
  }

  //-------------------------------------------------------------------------
  private void loadFixingSeries(ImmutableMarketDataBuilder builder) {
    if (!subdirectoryExists(HISTORICAL_FIXINGS_DIR)) {
      log.debug("No historical fixings directory found");
      return;
    }
    try {
      Collection<ResourceLocator> fixingSeriesResources = getAllResources(HISTORICAL_FIXINGS_DIR);
      Map<ObservableId, LocalDateDoubleTimeSeries> fixingSeries = FixingSeriesCsvLoader.load(fixingSeriesResources);
      builder.addTimeSeriesMap(fixingSeries);
    } catch (Exception e) {
      log.error("Error loading fixing series", e);
    }
  }

  private void loadRatesCurves(ImmutableMarketDataBuilder builder, LocalDate marketDataDate) {
    if (!subdirectoryExists(CURVES_DIR)) {
      log.debug("No rates curves directory found");
      return;
    }

    ResourceLocator curveGroupsResource = getResource(CURVES_DIR, CURVES_GROUPS_FILE);
    if (curveGroupsResource == null) {
      log.error("Unable to load rates curves: curve groups file not found at {}/{}", CURVES_DIR, CURVES_GROUPS_FILE);
      return;
    }

    ResourceLocator curveSettingsResource = getResource(CURVES_DIR, CURVES_SETTINGS_FILE);
    if (curveSettingsResource == null) {
      log.error("Unable to load rates curves: curve settings file not found at {}/{}", CURVES_DIR, CURVES_SETTINGS_FILE);
      return;
    }
    try {
      Collection<ResourceLocator> curvesResources = getRatesCurvesResources();
      List<CurveGroup> ratesCurves =
          RatesCurvesCsvLoader.load(marketDataDate, curveGroupsResource, curveSettingsResource, curvesResources);

      for (CurveGroup group : ratesCurves) {
        // add entry for higher level discount curve name
        group.getDiscountCurves().forEach(
            (ccy, curve) -> builder.addValue(CurveId.of(group.getName(), curve.getName()), curve));
        // add entry for higher level forward curve name
        group.getForwardCurves().forEach(
            (idx, curve) -> builder.addValue(CurveId.of(group.getName(), curve.getName()), curve));
      }

    } catch (Exception e) {
      log.error("Error loading rates curves", e);
    }
  }

  // load quotes
  private void loadQuotes(ImmutableMarketDataBuilder builder, LocalDate marketDataDate) {
    if (!subdirectoryExists(QUOTES_DIR)) {
      log.debug("No quotes directory found");
      return;
    }

    ResourceLocator quotesResource = getResource(QUOTES_DIR, QUOTES_FILE);
    if (quotesResource == null) {
      log.error("Unable to load quotes: quotes file not found at {}/{}", QUOTES_DIR, QUOTES_FILE);
      return;
    }

    try {
      Map<QuoteId, Double> quotes = QuotesCsvLoader.load(marketDataDate, quotesResource);
      builder.addValueMap(quotes);

    } catch (Exception ex) {
      log.error("Error loading quotes", ex);
    }
  }

  private void loadFxRates(ImmutableMarketDataBuilder builder) {
    // TODO - load from CSV file - format to be defined
    builder.addValue(FxRateId.of(Currency.GBP, Currency.USD), FxRate.of(Currency.GBP, Currency.USD, 1.61));
  }

  //-------------------------------------------------------------------------
  private Collection<ResourceLocator> getRatesCurvesResources() {
    return getAllResources(CURVES_DIR).stream()
        .filter(res -> !res.getLocator().endsWith(CURVES_GROUPS_FILE))
        .filter(res -> !res.getLocator().endsWith(CURVES_SETTINGS_FILE))
        .collect(toImmutableList());
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
