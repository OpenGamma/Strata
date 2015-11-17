/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.Guavate.toImmutableSortedMap;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.RateIndex;
import com.opengamma.strata.basics.interpolator.CurveExtrapolator;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.id.DiscountCurveId;
import com.opengamma.strata.market.id.RateCurveId;
import com.opengamma.strata.market.id.RateIndexCurveId;

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

  private static final String GROUPS_NAME = "Group Name";
  private static final String GROUPS_CURVE_TYPE = "Curve Type";
  private static final String GROUPS_REFERENCE = "Reference";
  private static final String GROUPS_CURVE_NAME = "Curve Name";

  private static final String CURVE_DATE = "Valuation Date";
  private static final String CURVE_NAME = "Curve Name";
  private static final String CURVE_POINT_DATE = "Date";
  private static final String CURVE_POINT_VALUE = "Value";
  private static final String CURVE_POINT_LABEL = "Label";

  /**
   * Name used in CSV file for discount curves.
   */
  private static final String DISCOUNT = "discount";
  /**
   * Name used in CSV file for forward curves.
   */
  private static final String FORWARD = "forward";
  /**
   * Names used in CSV file for value types.
   */
  private static final Map<String, ValueType> VALUE_TYPE_MAP = ImmutableMap.of(
      "zero", ValueType.ZERO_RATE,
      "df", ValueType.DISCOUNT_FACTOR);

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
  public static ImmutableMap<RateCurveId, Curve> load(
      LocalDate marketDataDate,
      ResourceLocator groupsResource,
      ResourceLocator settingsResource,
      Collection<ResourceLocator> curvesResources) {

    ImmutableMap<LocalDate, Map<RateCurveId, Curve>> curves =
        loadCurves(groupsResource, settingsResource, curvesResources, marketDataDate);
    return ImmutableMap.copyOf(curves.getOrDefault(marketDataDate, ImmutableMap.of()));
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
  public static ImmutableSortedMap<LocalDate, Map<RateCurveId, Curve>> loadAllDates(
      ResourceLocator groupsResource,
      ResourceLocator settingsResource,
      Collection<ResourceLocator> curvesResources) {

    return loadCurves(groupsResource, settingsResource, curvesResources, null);
  }

  //-------------------------------------------------------------------------
  // loads the curves, optionally specifying a date
  private static ImmutableSortedMap<LocalDate, Map<RateCurveId, Curve>> loadCurves(
      ResourceLocator groupsResource,
      ResourceLocator settingsResource,
      Collection<ResourceLocator> curvesResources,
      LocalDate curveDate) {

    // load curve groups and settings
    Map<CurveName, Set<RateCurveId>> curveGroups = loadCurveGroups(groupsResource);
    Map<CurveName, LoadedCurveSettings> settingsMap = loadCurveSettings(settingsResource);

    // load curves
    // builder ensures keys can only be seen once
    ImmutableMap.Builder<LoadedCurveKey, Curve> curvesBuilder = new ImmutableMap.Builder<>();
    for (ResourceLocator curvesResource : curvesResources) {
      curvesBuilder.putAll(loadSingle(curvesResource, settingsMap, curveDate));
    }
    ImmutableMap<LoadedCurveKey, Curve> curves = curvesBuilder.build();

    return mapCurves(curveGroups, curves);
  }

  // uses the curve groups to form the resolved map of curve id to curve
  private static ImmutableSortedMap<LocalDate, Map<RateCurveId, Curve>> mapCurves(
      Map<CurveName, Set<RateCurveId>> curveGroupMappings,
      Map<LoadedCurveKey, Curve> curves) {

    Map<LocalDate, ImmutableMap.Builder<RateCurveId, Curve>> resultBuilder = new HashMap<>();
    for (Map.Entry<LoadedCurveKey, Curve> curveEntry : curves.entrySet()) {
      LoadedCurveKey key = curveEntry.getKey();
      Set<RateCurveId> curveUses = curveGroupMappings.get(key.getCurveName());
      // ignore if curve not mapped in any group
      if (curveUses != null) {
        ImmutableMap.Builder<RateCurveId, Curve> builder =
            resultBuilder.computeIfAbsent(key.getCurveDate(), k -> ImmutableMap.builder());
        for (RateCurveId curveUse : curveUses) {
          builder.put(curveUse, curveEntry.getValue());
        }
      }
    }

    return resultBuilder.entrySet().stream()
        .collect(toImmutableSortedMap(e -> e.getKey(), e -> e.getValue().build()));
  }

  //-------------------------------------------------------------------------
  // loads the curve settings CSV file
  static Map<CurveName, LoadedCurveSettings> loadCurveSettings(ResourceLocator settingsResource) {
    ImmutableMap.Builder<CurveName, LoadedCurveSettings> builder = ImmutableMap.builder();
    CsvFile csv = CsvFile.of(settingsResource.getCharSource(), true);
    for (int i = 0; i < csv.rowCount(); i++) {
      String curveNameStr = csv.field(i, SETTINGS_CURVE_NAME);
      String valueTypeStr = csv.field(i, SETTINGS_VALUE_TYPE);
      String dayCountStr = csv.field(i, SETTINGS_DAY_COUNT);
      String interpolatorStr = csv.field(i, SETTINGS_INTERPOLATOR);
      String leftExtrapolatorStr = csv.field(i, SETTINGS_LEFT_EXTRAPOLATOR);
      String rightExtrapolatorStr = csv.field(i, SETTINGS_RIGHT_EXTRAPOLATOR);

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

  // loads the curve groups CSV file
  static Map<CurveName, Set<RateCurveId>> loadCurveGroups(ResourceLocator groupsResource) {
    Map<CurveName, Set<RateCurveId>> curveGroups = new HashMap<>();
    CsvFile csv = CsvFile.of(groupsResource.getCharSource(), true);
    for (int i = 0; i < csv.rowCount(); i++) {
      String curveGroupStr = csv.field(i, GROUPS_NAME);
      String curveTypeStr = csv.field(i, GROUPS_CURVE_TYPE);
      String referenceStr = csv.field(i, GROUPS_REFERENCE);
      String curveNameStr = csv.field(i, GROUPS_CURVE_NAME);

      RateCurveId curveId = createRateCurveId(CurveGroupName.of(curveGroupStr), curveTypeStr, referenceStr);
      CurveName curveName = CurveName.of(curveNameStr);
      Set<RateCurveId> curveUses = curveGroups.computeIfAbsent(curveName, k -> new HashSet<RateCurveId>());
      curveUses.add(curveId);
    }
    return curveGroups;
  }

  // parses the identifier
  private static RateCurveId createRateCurveId(CurveGroupName curveGroup, String curveTypeStr, String referenceStr) {
    // discount and forward curves are supported
    if (FORWARD.equals(curveTypeStr.toLowerCase())) {
      RateIndex index = (RateIndex) LoaderUtils.findIndex(referenceStr);
      return RateIndexCurveId.of(index, curveGroup);

    } else if (DISCOUNT.equals(curveTypeStr.toLowerCase())) {
      Currency ccy = Currency.of(referenceStr);
      return DiscountCurveId.of(ccy, curveGroup);

    } else {
      throw new IllegalArgumentException(Messages.format("Unsupported curve type: {}", curveTypeStr));
    }
  }

  //-------------------------------------------------------------------------
  // loads a single curves CSV file
  // requestedDate can be null, meaning load all dates
  private static Map<LoadedCurveKey, Curve> loadSingle(
      ResourceLocator curvesResource,
      Map<CurveName, LoadedCurveSettings> settingsMap,
      LocalDate requestedDate) {

    CsvFile csv = CsvFile.of(curvesResource.getCharSource(), true);
    Map<LoadedCurveKey, List<LoadedCurveNode>> allNodes = new HashMap<>();
    for (int i = 0; i < csv.rowCount(); i++) {
      String valuationDateStr = csv.field(i, CURVE_DATE);
      String curveNameStr = csv.field(i, CURVE_NAME);
      String pointDateStr = csv.field(i, CURVE_POINT_DATE);
      String pointValueStr = csv.field(i, CURVE_POINT_VALUE);
      String pointLabel = csv.field(i, CURVE_POINT_LABEL);

      LocalDate valuationDate = LocalDate.parse(valuationDateStr);
      if (requestedDate == null || valuationDate.equals(requestedDate)) {
        LocalDate pointDate = LocalDate.parse(pointDateStr);
        double pointValue = Double.valueOf(pointValueStr);

        LoadedCurveKey key = LoadedCurveKey.of(valuationDate, CurveName.of(curveNameStr));
        List<LoadedCurveNode> curveNodes = allNodes.computeIfAbsent(key, k -> new ArrayList<LoadedCurveNode>());
        curveNodes.add(LoadedCurveNode.of(pointDate, pointValue, pointLabel));
      }
    }
    return buildCurves(settingsMap, allNodes);
  }

  // build the curves
  private static Map<LoadedCurveKey, Curve> buildCurves(
      Map<CurveName, LoadedCurveSettings> settingsMap,
      Map<LoadedCurveKey, List<LoadedCurveNode>> allNodes) {

    ImmutableMap.Builder<LoadedCurveKey, Curve> results = ImmutableMap.builder();
    for (Map.Entry<LoadedCurveKey, List<LoadedCurveNode>> entry : allNodes.entrySet()) {
      LoadedCurveKey key = entry.getKey();
      LoadedCurveSettings settings = settingsMap.get(key.getCurveName());
      if (settings == null) {
        throw new IllegalArgumentException(Messages.format("Missing settings for curve: {}", key));
      }
      results.put(key, settings.createCurve(key.getCurveDate(), entry.getValue()));
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
