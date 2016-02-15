/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.interpolator.CurveInterpolator;

/**
 * Loads a set of rates curves into memory by reading from CSV resources.
 * <p>
 * There are three type of CSV files.
 * <p>
 * The first file is the curve group metadata file.
 * This file has the following header row:<br />
 * {@code Group Name, Curve Type, Reference, Curve Name}.
 * <ul>
 * <li>The 'Group Name' column is the name of the group of curves.
 * <li>The 'Curve Type' column is the type of the curve, "forward" or "discount".
 * <li>The 'Reference' column is the reference the curve is used for, such as "USD" or "USD-LIBOR-3M".
 * <li>The 'Curve Name' column is the name of the curve.
 * </ul>
 * <p>
 * The second file is the curve settings metadata file.
 * This file has the following header row:<br />
 * {@code Curve Name, Value Type, Day Count, Interpolator, Left Extrapolator, Right Extrapolator}.
 * <ul>
 * <li>The 'Curve Name' column is the name of the curve.
 * <li>The 'Value Type' column is the type of data in the curve, "zero" for zero rates, or "df" for discount factors.
 * <li>The 'Day Count' column is the name of the day count, such as "Act/365F".
 * <li>The 'Interpolator' and extrapolator columns define the interpolator to use.
 * </ul>
 * <p>
 * The third file is the curve nodes file.
 * This file has the following header row:<br />
 * {@code Valuation Date, Curve Name, Date, Value, Label}.
 * <ul>
 * <li>The 'Valuation Date' column provides the valuation date, allowing data from different
 *  days to be stored in the same file
 * <li>The 'Curve Name' column is the name of the curve.
 * <li>The 'Date' column is the date associated with the node.
 * <li>The 'Value' column is value of the curve at the date.
 * <li>The 'Label' column is the label used to refer to the node.
 * </ul>
 * <p>
 * Each curve must be contained entirely within a single file, but each file may contain more than
 * one curve. The curve points do not need to be ordered.
 */
public final class RatesCurvesCsvLoader {

  // CSV column headers
  private static final String SETTINGS_CURVE_NAME = "Curve Name";
  private static final String SETTINGS_VALUE_TYPE = "Value Type";
  private static final String SETTINGS_DAY_COUNT = "Day Count";
  private static final String SETTINGS_INTERPOLATOR = "Interpolator";
  private static final String SETTINGS_LEFT_EXTRAPOLATOR = "Left Extrapolator";
  private static final String SETTINGS_RIGHT_EXTRAPOLATOR = "Right Extrapolator";

  private static final String CURVE_DATE = "Valuation Date";
  private static final String CURVE_NAME = "Curve Name";
  private static final String CURVE_POINT_DATE = "Date";
  private static final String CURVE_POINT_VALUE = "Value";
  private static final String CURVE_POINT_LABEL = "Label";

  /**
   * Names used in CSV file for value types.
   */
  private static final Map<String, ValueType> VALUE_TYPE_MAP = ImmutableMap.of(
      "zero", ValueType.ZERO_RATE,
      "df", ValueType.DISCOUNT_FACTOR,
      "forward", ValueType.FORWARD_RATE);

  //-------------------------------------------------------------------------
  /**
   * Loads one or more CSV format curve files for a specific date.
   * <p>
   * Only those quotes that match the specified date will be loaded.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   *
   * @param marketDataDate  the curve date to load
   * @param groupsResource  the curve groups CSV resource
   * @param settingsResource  the curve settings CSV resource
   * @param curvesResources  the CSV resources for curves
   * @return the loaded curves, mapped by an identifying key
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static List<CurveGroup> load(
      LocalDate marketDataDate,
      ResourceLocator groupsResource,
      ResourceLocator settingsResource,
      Collection<ResourceLocator> curvesResources) {

    List<CurveGroupDefinition> curveGroups = CurveGroupDefinitionCsvLoader.loadCurveGroups(groupsResource);
    Multimap<LocalDate, Curve> allCurves = loadCurves(settingsResource, curvesResources, marketDataDate);
    Collection<Curve> curves = allCurves.get(marketDataDate);
    Set<CurveName> curveNames = new HashSet<>();

    // Ensure curve names are unique
    for (Curve curve : curves) {
      if (!curveNames.add(curve.getName())) {
        throw new IllegalArgumentException("Multiple curves with the same name: " + curve.getName());
      }
    }
    return curveGroups.stream().map(groupDef -> CurveGroup.ofCurves(groupDef, curves)).collect(toImmutableList());
  }

  /**
   * Loads one or more CSV format curve files for all available dates.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   *
   * @param groupsResource  the curve groups CSV resource
   * @param settingsResource  the curve settings CSV resource
   * @param curvesResources  the CSV resources for curves
   * @return the loaded curves, mapped by date and identifier
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ListMultimap<LocalDate, CurveGroup> loadAllDates(
      ResourceLocator groupsResource,
      ResourceLocator settingsResource,
      Collection<ResourceLocator> curvesResources) {

    List<CurveGroupDefinition> curveGroups = CurveGroupDefinitionCsvLoader.loadCurveGroups(groupsResource);
    Multimap<LocalDate, Curve> curves = loadCurves(settingsResource, curvesResources, null);
    ImmutableListMultimap.Builder<LocalDate, CurveGroup> builder = ImmutableListMultimap.builder();

    for (CurveGroupDefinition groupDefinition : curveGroups) {
      for (Map.Entry<LocalDate, Collection<Curve>> entry : curves.asMap().entrySet()) {
        CurveGroup curveGroup = CurveGroup.ofCurves(groupDefinition, entry.getValue());
        builder.put(entry.getKey(), curveGroup);
      }
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  // loads the curves, optionally specifying a date
  private static Multimap<LocalDate, Curve> loadCurves(
      ResourceLocator settingsResource,
      Collection<ResourceLocator> curvesResources,
      LocalDate curveDate) {

    // load curve settings
    Map<CurveName, LoadedCurveSettings> settingsMap = loadCurveSettings(settingsResource);

    // load curves
    // builder ensures keys can only be seen once
    ImmutableMultimap.Builder<LocalDate, Curve> curvesBuilder = ImmutableMultimap.builder();
    curvesBuilder.orderKeysBy(Comparator.naturalOrder());

    for (ResourceLocator curvesResource : curvesResources) {
      curvesBuilder.putAll(loadSingle(curvesResource, settingsMap, curveDate));
    }
    return curvesBuilder.build();
  }

  //-------------------------------------------------------------------------
  // loads the curve settings CSV file
  static Map<CurveName, LoadedCurveSettings> loadCurveSettings(ResourceLocator settingsResource) {
    ImmutableMap.Builder<CurveName, LoadedCurveSettings> builder = ImmutableMap.builder();
    CsvFile csv = CsvFile.of(settingsResource.getCharSource(), true);
    for (CsvRow row : csv.rows()) {
      String curveNameStr = row.getField(SETTINGS_CURVE_NAME);
      String valueTypeStr = row.getField(SETTINGS_VALUE_TYPE);
      String dayCountStr = row.getField(SETTINGS_DAY_COUNT);
      String interpolatorStr = row.getField(SETTINGS_INTERPOLATOR);
      String leftExtrapolatorStr = row.getField(SETTINGS_LEFT_EXTRAPOLATOR);
      String rightExtrapolatorStr = row.getField(SETTINGS_RIGHT_EXTRAPOLATOR);

      CurveName curveName = CurveName.of(curveNameStr);
      ValueType valueType = VALUE_TYPE_MAP.get(valueTypeStr.toLowerCase(Locale.ENGLISH));
      DayCount dayCount = DayCount.of(dayCountStr);
      CurveInterpolator interpolator = CurveInterpolator.of(interpolatorStr);
      CurveExtrapolator leftExtrapolator = CurveExtrapolator.of(leftExtrapolatorStr);
      CurveExtrapolator rightExtrapolator = CurveExtrapolator.of(rightExtrapolatorStr);

      if (!VALUE_TYPE_MAP.containsKey(valueTypeStr.toLowerCase(Locale.ENGLISH))) {
        throw new IllegalArgumentException(
            Messages.format("Unsupported {} in curve settings: {}", SETTINGS_VALUE_TYPE, valueTypeStr));
      }

      LoadedCurveSettings settings = LoadedCurveSettings.of(
          curveName, valueType, dayCount, interpolator, leftExtrapolator, rightExtrapolator);
      builder.put(curveName, settings);
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  // loads a single curves CSV file
  // requestedDate can be null, meaning load all dates
  private static Multimap<LocalDate, Curve> loadSingle(
      ResourceLocator curvesResource,
      Map<CurveName, LoadedCurveSettings> settingsMap,
      LocalDate requestedDate) {

    CsvFile csv = CsvFile.of(curvesResource.getCharSource(), true);
    Map<LoadedCurveKey, List<LoadedCurveNode>> allNodes = new HashMap<>();
    for (CsvRow row : csv.rows()) {
      String valuationDateStr = row.getField(CURVE_DATE);
      String curveNameStr = row.getField(CURVE_NAME);
      String pointDateStr = row.getField(CURVE_POINT_DATE);
      String pointValueStr = row.getField(CURVE_POINT_VALUE);
      String pointLabel = row.getField(CURVE_POINT_LABEL);

      LocalDate valuationDate = LocalDate.parse(valuationDateStr);
      if (requestedDate == null || valuationDate.equals(requestedDate)) {
        LocalDate pointDate = LocalDate.parse(pointDateStr);
        double pointValue = Double.valueOf(pointValueStr);

        LoadedCurveKey key = LoadedCurveKey.of(valuationDate, CurveName.of(curveNameStr));
        List<LoadedCurveNode> curveNodes = allNodes.computeIfAbsent(key, k -> new ArrayList<>());
        curveNodes.add(LoadedCurveNode.of(pointDate, pointValue, pointLabel));
      }
    }
    return buildCurves(settingsMap, allNodes);
  }

  // build the curves
  private static Multimap<LocalDate, Curve> buildCurves(
      Map<CurveName, LoadedCurveSettings> settingsMap,
      Map<LoadedCurveKey, List<LoadedCurveNode>> allNodes) {

    ImmutableMultimap.Builder<LocalDate, Curve> results = ImmutableMultimap.builder();

    for (Map.Entry<LoadedCurveKey, List<LoadedCurveNode>> entry : allNodes.entrySet()) {
      LoadedCurveKey key = entry.getKey();
      LoadedCurveSettings settings = settingsMap.get(key.getCurveName());

      if (settings == null) {
        throw new IllegalArgumentException(Messages.format("Missing settings for curve: {}", key));
      }
      results.put(key.getCurveDate(), settings.createCurve(key.getCurveDate(), entry.getValue()));
    }
    return results.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private RatesCurvesCsvLoader() {
  }

}
