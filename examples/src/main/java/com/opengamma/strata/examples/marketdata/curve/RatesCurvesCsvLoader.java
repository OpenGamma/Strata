/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata.curve;

import static com.opengamma.strata.collect.Guavate.toImmutableSortedMap;

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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.RateIndex;
import com.opengamma.strata.basics.interpolator.CurveExtrapolator;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.examples.marketdata.LoaderUtils;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveParameterMetadata;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.SimpleCurveNodeMetadata;
import com.opengamma.strata.market.id.DiscountCurveId;
import com.opengamma.strata.market.id.RateCurveId;
import com.opengamma.strata.market.id.RateIndexCurveId;
import com.opengamma.strata.market.value.ValueType;

/**
 * Loads a set of rates curves into memory by reading from CSV resources.
 * <p>
 * There are two required metadata files:
 * <ul>
 * <li> Groups - defines named groups of curves, mapping from curves for a particular purpose
 *      to curve names. This must contain the following header row:
 *      Group Name, Curve Type, Reference, Curve Name
 *      
 * <li> Settings - contains the settings that apply to each named curve across all valuation
 *      dates. This must contain the following header row:
 *      Group Name, Curve Name, Day Count, Interpolator, Left Extrapolator, Right Extrapolator, Value Type
 * </ul>
 * There may be one or more curves files with the following header row:
 * Valuation Date, Group Name, Curve Name, Date, Value, Label
 * <p>
 * Each curve must be contained entirely within a single file, but each file may contain more than
 * one curve. The curve points do not need to be ordered.
 */
public final class RatesCurvesCsvLoader {

  private static final String SETTINGS_GROUP_NAME = "Group Name";
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
  private static final String CURVE_GROUP_NAME = "Group Name";
  private static final String CURVE_NAME = "Curve Name";
  private static final String CURVE_POINT_DATE = "Date";
  private static final String CURVE_POINT_VALUE = "Value";
  private static final String CURVE_POINT_LABEL = "Label";

  private static final Map<String, ValueType> VALUE_TYPE_MAP = ImmutableMap.of(
      "zero", ValueType.ZERO_RATE,
      "df", ValueType.DISCOUNT_FACTOR);

  /**
   * Restricted constructor.
   */
  private RatesCurvesCsvLoader() {
  }

  //-------------------------------------------------------------------------
  /**
   * Loads the available rates curves from the CSV resources for a given curve date.
   * 
   * @param groupsResource  the curve groups CSV resource
   * @param settingsResource  the curve settings CSV resource
   * @param curvesResources  the CSV resources for curves
   * @param curveDate  the curve date to load
   * @return the loaded curves, mapped by an identifying key
   */
  public static Map<RateCurveId, Curve> loadCurves(
      ResourceLocator groupsResource,
      ResourceLocator settingsResource,
      Collection<ResourceLocator> curvesResources,
      LocalDate curveDate) {

    Map<LocalDate, Map<RateCurveId, Curve>> curves = loadCurvesForDate(groupsResource, settingsResource, curvesResources, curveDate);
    return curves.getOrDefault(curveDate, ImmutableMap.of());
  }
  
  /**
   * Loads the available rates curves from the CSV resources for all dates.
   * 
   * @param groupsResource  the curve groups CSV resource
   * @param settingsResource  the curve settings CSV resource
   * @param curvesResources  the CSV resources for curves
   * @return the loaded curves, mapped by date and identifier
   */
  public static ImmutableSortedMap<LocalDate, Map<RateCurveId, Curve>> loadAllCurves(
      ResourceLocator groupsResource,
      ResourceLocator settingsResource,
      Collection<ResourceLocator> curvesResources) {
    
    return loadCurvesForDate(groupsResource, settingsResource, curvesResources, null);
  }

  //-------------------------------------------------------------------------
  private static ImmutableSortedMap<LocalDate, Map<RateCurveId, Curve>> loadCurvesForDate(
      ResourceLocator groupsResource,
      ResourceLocator settingsResource,
      Collection<ResourceLocator> curvesResources,
      LocalDate curveDate) {
    
    // load curves
    Map<LoadedCurveName, LoadedCurveSettings> settingsMap = loadCurveSettings(settingsResource);
    ImmutableMap.Builder<LoadedCurveKey, Curve> curvesBuilder = new ImmutableMap.Builder<>();
    for (ResourceLocator curvesResource : curvesResources) {
      // builder ensures keys can only be seen once
      curvesBuilder.putAll(loadCurvesFile(curvesResource, settingsMap, curveDate));
    }
    ImmutableMap<LoadedCurveKey, Curve> curves = curvesBuilder.build();

    // load curve groups
    Map<LoadedCurveName, Set<RateCurveId>> curveGroups = loadCurveGroups(groupsResource);

    return mapCurves(curveGroups, curves);
  }
  
  // uses the curve groups to form the resolved map of curve id to curve
  private static ImmutableSortedMap<LocalDate, Map<RateCurveId, Curve>> mapCurves(
      Map<LoadedCurveName, Set<RateCurveId>> curveGroupMappings,
      Map<LoadedCurveKey, Curve> curves) {
    
    Map<LocalDate, ImmutableMap.Builder<RateCurveId, Curve>> resultBuilder = new HashMap<>();
    
    for (Map.Entry<LoadedCurveKey, Curve> curveEntry : curves.entrySet()) {
      LoadedCurveKey key = curveEntry.getKey();
      Set<RateCurveId> curveUses = curveGroupMappings.get(key.toName());
      if (curveUses == null) {
        // Curve not mapped in any group
        continue;
      }
      
      ImmutableMap.Builder<RateCurveId, Curve> builder = resultBuilder.get(key.getCurveDate());
      if (builder == null) {
        builder = ImmutableMap.builder();
        resultBuilder.put(key.getCurveDate(), builder);
      }
      for (RateCurveId curveUse : curveUses) {
        builder.put(curveUse, curveEntry.getValue());
      }
    }
    
    return resultBuilder.entrySet().stream()
        .collect(toImmutableSortedMap(e -> e.getKey(), e -> e.getValue().build()));
  }

  // loads the curve settings CSV file
  private static Map<LoadedCurveName, LoadedCurveSettings> loadCurveSettings(ResourceLocator settingsResource) {
    Map<LoadedCurveName, LoadedCurveSettings> settingsMap = new HashMap<>();
    CsvFile csv = CsvFile.of(settingsResource.getCharSource(), true);
    for (int i = 0; i < csv.rowCount(); i++) {
      String curveGroupName = csv.field(i, SETTINGS_GROUP_NAME);
      String curveName = csv.field(i, SETTINGS_CURVE_NAME);
      String dayCountName = csv.field(i, SETTINGS_DAY_COUNT);
      String interpolatorName = csv.field(i, SETTINGS_INTERPOLATOR);
      String leftExtrapolatorName = csv.field(i, SETTINGS_LEFT_EXTRAPOLATOR);
      String rightExtrapolatorName = csv.field(i, SETTINGS_RIGHT_EXTRAPOLATOR);
      String valueTypeName = csv.field(i, SETTINGS_VALUE_TYPE);

      LoadedCurveName curveKey = LoadedCurveName.of(curveGroupName, curveName);
      DayCount dayCount = DayCount.of(dayCountName);
      CurveInterpolator interpolator = CurveInterpolator.of(interpolatorName);
      CurveExtrapolator leftExtrapolator = CurveExtrapolator.of(leftExtrapolatorName);
      CurveExtrapolator rightExtrapolator = CurveExtrapolator.of(rightExtrapolatorName);

      if (!VALUE_TYPE_MAP.containsKey(valueTypeName.toLowerCase(Locale.ENGLISH))) {
        throw new IllegalArgumentException(
            Messages.format("Unsupported {} in curve settings: {}", SETTINGS_VALUE_TYPE, valueTypeName));
      }

      LoadedCurveSettings settings = LoadedCurveSettings.builder()
          .dayCount(dayCount)
          .yValueType(VALUE_TYPE_MAP.get(valueTypeName.toLowerCase(Locale.ENGLISH)))
          .interpolator(interpolator)
          .leftExtrapolator(leftExtrapolator)
          .rightExtrapolator(rightExtrapolator)
          .build();
      settingsMap.put(curveKey, settings);
    }
    return settingsMap;
  }

  // loads the curve groups CSV file
  private static Map<LoadedCurveName, Set<RateCurveId>> loadCurveGroups(ResourceLocator groupsResource) {
    Map<LoadedCurveName, Set<RateCurveId>> curveGroups = new HashMap<>();
    CsvFile csv = CsvFile.of(groupsResource.getCharSource(), true);
    for (int i = 0; i < csv.rowCount(); i++) {
      String curveGroupText = csv.field(i, GROUPS_NAME);
      String curveType = csv.field(i, GROUPS_CURVE_TYPE);
      String reference = csv.field(i, GROUPS_REFERENCE);
      String curveName = csv.field(i, GROUPS_CURVE_NAME);

      // discount and forward curves are supported
      CurveGroupName curveGroup = CurveGroupName.of(curveGroupText);
      // TODO: split this to have "ibor" and "overnight" in csv file
      RateCurveId curveId;
      if ("forward".equals(curveType.toLowerCase())) {
        RateIndex index = LoaderUtils.findIndex(reference);
        curveId = RateIndexCurveId.of(index, curveGroup);
      } else if ("discount".equals(curveType.toLowerCase())) {
        Currency ccy = Currency.of(reference);
        curveId = DiscountCurveId.of(ccy, curveGroup);
      } else {
        throw new IllegalArgumentException(Messages.format("Unsupported curve type: {}", curveType));
      }

      LoadedCurveName curveKey = LoadedCurveName.of(curveGroup, CurveName.of(curveName));
      Set<RateCurveId> curveUses = curveGroups.get(curveKey);
      if (curveUses == null) {
        curveUses = new HashSet<RateCurveId>();
        curveGroups.put(curveKey, curveUses);
      }
      curveUses.add(curveId);
    }
    return curveGroups;
  }

  // loads a single curves CSV file
  private static Map<LoadedCurveKey, Curve> loadCurvesFile(
      ResourceLocator curvesResource,
      Map<LoadedCurveName, LoadedCurveSettings> settingsMap,
      LocalDate curveDate) {

    // parse the curve nodes
    CsvFile csv = CsvFile.of(curvesResource.getCharSource(), true);
    Map<LoadedCurveKey, List<LoadedCurveNode>> builders = new HashMap<>();
    for (int i = 0; i < csv.rowCount(); i++) {
      String valuationDateText = csv.field(i, CURVE_DATE);
      String curveGroup = csv.field(i, CURVE_GROUP_NAME);
      String curveName = csv.field(i, CURVE_NAME);
      String pointDateText = csv.field(i, CURVE_POINT_DATE);
      String pointValueText = csv.field(i, CURVE_POINT_VALUE);
      String pointLabel = csv.field(i, CURVE_POINT_LABEL);

      LocalDate valuationDate = LocalDate.parse(valuationDateText);
      if (curveDate != null && !valuationDate.equals(curveDate)) {
        continue;
      }

      LocalDate pointDate = LocalDate.parse(pointDateText);
      double pointValue = Double.valueOf(pointValueText);

      LoadedCurveKey key = LoadedCurveKey.of(valuationDate, curveGroup, curveName);
      List<LoadedCurveNode> curveNodes = builders.get(key);
      if (curveNodes == null) {
        curveNodes = new ArrayList<LoadedCurveNode>();
        builders.put(key, curveNodes);
      }
      LoadedCurveNode curvePoint = LoadedCurveNode.of(pointDate, pointValue, pointLabel);
      curveNodes.add(curvePoint);
    }

    // build the curves
    ImmutableMap.Builder<LoadedCurveKey, Curve> results = ImmutableMap.builder();
    for (Map.Entry<LoadedCurveKey, List<LoadedCurveNode>> builderEntry : builders.entrySet()) {
      LoadedCurveKey key = builderEntry.getKey();
      LoadedCurveSettings settings = settingsMap.get(key.toName());
      if (settings == null) {
        throw new IllegalArgumentException(Messages.format("Missing settings for curve: {}", key));
      }
      Curve curve = createCurve(key, builderEntry.getValue(), settings);
      results.put(key, curve);
    }
    return results.build();
  }

  // constructs an interpolated nodal curve
  private static Curve createCurve(
      LoadedCurveKey curveKey,
      List<LoadedCurveNode> curveNodes,
      LoadedCurveSettings curveSettings) {

    // build each node
    curveNodes.sort(Comparator.naturalOrder());
    double[] xValues = new double[curveNodes.size()];
    double[] yValues = new double[curveNodes.size()];
    List<CurveParameterMetadata> pointsMetadata = new ArrayList<CurveParameterMetadata>(curveNodes.size());
    for (int i = 0; i < curveNodes.size(); i++) {
      LoadedCurveNode point = curveNodes.get(i);
      double yearFraction = curveSettings.getDayCount().yearFraction(curveKey.getCurveDate(), point.getDate());
      xValues[i] = yearFraction;
      yValues[i] = point.getValue();
      CurveParameterMetadata pointMetadata = SimpleCurveNodeMetadata.of(point.getDate(), point.getLabel());
      pointsMetadata.add(pointMetadata);
    }

    // create metadata
    CurveMetadata curveMetadata = DefaultCurveMetadata.builder()
        .curveName(curveKey.getCurveName())
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(curveSettings.getYValueType())
        .dayCount(curveSettings.getDayCount())
        .parameterMetadata(pointsMetadata)
        .build();
    return InterpolatedNodalCurve.builder()
        .metadata(curveMetadata)
        .xValues(DoubleArray.copyOf(xValues))
        .yValues(DoubleArray.copyOf(yValues))
        .interpolator(curveSettings.getInterpolator())
        .extrapolatorLeft(curveSettings.getLeftExtrapolator())
        .extrapolatorRight(curveSettings.getRightExtrapolator())
        .build();
  }

}
