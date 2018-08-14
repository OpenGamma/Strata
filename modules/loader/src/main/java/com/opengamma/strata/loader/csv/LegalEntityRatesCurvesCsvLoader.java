/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.CharSource;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.LegalEntityCurveGroup;
import com.opengamma.strata.market.curve.LegalEntityGroup;
import com.opengamma.strata.market.curve.RepoGroup;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;

/**
 * Loads a set of legal entity rates curves into memory by reading from CSV resources.
 * <p>
 * There are three type of CSV files.
 * <p>
 * The first file is the legal entity curve group metadata file.
 * This file has the following header row:<br />
 * {@code Group Name, Curve Type, Reference, Currency, Curve Name}.
 * <ul>
 * <li>The 'Group Name' column is the name of the group of curves.
 * <li>The 'Curve Type' column is the type of the curve, "repo" or "issuer".
 * <li>The 'Reference' column is the reference group for which the curve is used, legal entity group or repo group.
 * <li>The 'Currency' column is the reference currency for which the curve is used.
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
 * <li>The 'Interpolator' column defines the interpolator to use.
 * <li>The 'Left Extrapolator' and 'Right Extrapolator' columns define the extrapolators to use.
 * </ul>
 * <p>
 * The third file is the curve values file.
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
 * The files must contain at least one repo curve and one issuer curve.
 */
public class LegalEntityRatesCurvesCsvLoader {

  // Column headers for legal entity curve group
  private static final String GROUPS_NAME = "Group Name";
  private static final String GROUPS_CURVE_TYPE = "Curve Type";
  private static final String GROUPS_REFERENCE = "Reference";
  private static final String GROUPS_CURRENCY = "Currency";
  private static final String GROUPS_CURVE_NAME = "Curve Name";

  // Names used in the curve type column in the legal entity curve group
  private static final String REPO = "repo";
  private static final String ISSUER = "issuer";

  // Column headers for curve setting 
  private static final String SETTINGS_CURVE_NAME = "Curve Name";
  private static final String SETTINGS_VALUE_TYPE = "Value Type";
  private static final String SETTINGS_DAY_COUNT = "Day Count";
  private static final String SETTINGS_INTERPOLATOR = "Interpolator";
  private static final String SETTINGS_LEFT_EXTRAPOLATOR = "Left Extrapolator";
  private static final String SETTINGS_RIGHT_EXTRAPOLATOR = "Right Extrapolator";

  // Column headers for curve nodes
  private static final String CURVE_DATE = "Valuation Date";
  private static final String CURVE_NAME = "Curve Name";
  private static final String CURVE_POINT_DATE = "Date";
  private static final String CURVE_POINT_VALUE = "Value";
  private static final String CURVE_POINT_LABEL = "Label";

  /**
   * Names used in CSV file for value types.
   */
  private static final BiMap<String, ValueType> VALUE_TYPE_MAP = ImmutableBiMap.of(
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
   * @param curveValueResources  the CSV resources for curves
   * @return the loaded curves, mapped by an identifying key
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableList<LegalEntityCurveGroup> load(
      LocalDate marketDataDate,
      ResourceLocator groupsResource,
      ResourceLocator settingsResource,
      Collection<ResourceLocator> curveValueResources) {

    Collection<CharSource> curveCharSources = curveValueResources.stream().map(r -> r.getCharSource()).collect(toList());
    ListMultimap<LocalDate, LegalEntityCurveGroup> map = parse(
        d -> marketDataDate.equals(d),
        groupsResource.getCharSource(),
        settingsResource.getCharSource(),
        curveCharSources);
    return ImmutableList.copyOf(map.get(marketDataDate));
  }

  /**
   * Loads one or more CSV format curve files for all available dates.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   *
   * @param groupsResource  the curve groups CSV resource
   * @param settingsResource  the curve settings CSV resource
   * @param curveValueResources  the CSV resources for curves
   * @return the loaded curves, mapped by date and identifier
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableListMultimap<LocalDate, LegalEntityCurveGroup> loadAllDates(
      ResourceLocator groupsResource,
      ResourceLocator settingsResource,
      Collection<ResourceLocator> curveValueResources) {

    Collection<CharSource> curveCharSources = curveValueResources.stream().map(r -> r.getCharSource()).collect(toList());
    return parse(d -> true, groupsResource.getCharSource(), settingsResource.getCharSource(), curveCharSources);
  }

  /**
   * Parses one or more CSV format curve files for all available dates.
   * <p>
   * A predicate is specified that is used to filter the dates that are returned.
   * This could match a single date, a set of dates or all dates.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   *
   * @param datePredicate  the predicate used to select the dates
   * @param groupsCharSource  the curve groups CSV character source
   * @param settingsCharSource  the curve settings CSV character source
   * @param curveValueCharSources  the CSV character sources for curves
   * @return the loaded curves, mapped by date and identifier
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableListMultimap<LocalDate, LegalEntityCurveGroup> parse(
      Predicate<LocalDate> datePredicate,
      CharSource groupsCharSource,
      CharSource settingsCharSource,
      Collection<CharSource> curveValueCharSources) {

    Map<CurveGroupName, Map<Pair<RepoGroup, Currency>, CurveName>> repoGroups = new LinkedHashMap<>();
    Map<CurveGroupName, Map<Pair<LegalEntityGroup, Currency>, CurveName>> legalEntityGroups = new LinkedHashMap<>();
    parseCurveMaps(groupsCharSource, repoGroups, legalEntityGroups);
    Map<LocalDate, Map<CurveName, Curve>> allCurves =
        parseCurves(datePredicate, settingsCharSource, curveValueCharSources);
    ImmutableListMultimap.Builder<LocalDate, LegalEntityCurveGroup> builder = ImmutableListMultimap.builder();

    for (Map.Entry<LocalDate, Map<CurveName, Curve>> curveEntry : allCurves.entrySet()) {
      LocalDate date = curveEntry.getKey();
      Map<CurveName, Curve> curves = curveEntry.getValue();
      for (Map.Entry<CurveGroupName, Map<Pair<RepoGroup, Currency>, CurveName>> repoEntry : repoGroups.entrySet()) {
        CurveGroupName groupName = repoEntry.getKey();
        Map<Pair<RepoGroup, Currency>, Curve> repoCurves = MapStream.of(repoEntry.getValue())
            .mapValues(name -> queryCurve(name, curves, date, groupName, "Repo"))
            .toMap();
        Map<Pair<LegalEntityGroup, Currency>, Curve> issuerCurves = MapStream.of(legalEntityGroups.get(groupName))
            .mapValues(name -> queryCurve(name, curves, date, groupName, "Issuer"))
            .toMap();
        builder.put(date, LegalEntityCurveGroup.of(groupName, repoCurves, issuerCurves));
      }
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  private static Map<LocalDate, Map<CurveName, Curve>> parseCurves(
      Predicate<LocalDate> datePredicate,
      CharSource settingsResource,
      Collection<CharSource> curvesResources) {

    // load curve settings
    Map<CurveName, LoadedCurveSettings> settingsMap = parseCurveSettings(settingsResource);

    // load curves, ensuring curves only be seen once within a date
    Map<LocalDate, Map<CurveName, Curve>> resultMap = new TreeMap<>();
    for (CharSource curvesResource : curvesResources) {
      Multimap<LocalDate, Curve> fileCurvesByDate = parseSingle(datePredicate, curvesResource, settingsMap);
      // Ensure curve names are unique, with a good error message
      for (LocalDate date : fileCurvesByDate.keySet()) {
        Collection<Curve> fileCurves = fileCurvesByDate.get(date);
        Map<CurveName, Curve> resultCurves = resultMap.computeIfAbsent(date, d -> new HashMap<>());
        for (Curve fileCurve : fileCurves) {
          if (resultCurves.put(fileCurve.getName(), fileCurve) != null) {
            throw new IllegalArgumentException(
                "Rates curve loader found multiple curves with the same name: " + fileCurve.getName());
          }
        }
      }
    }
    return resultMap;
  }

  private static Map<CurveName, LoadedCurveSettings> parseCurveSettings(CharSource settingsResource) {
    ImmutableMap.Builder<CurveName, LoadedCurveSettings> builder = ImmutableMap.builder();
    CsvFile csv = CsvFile.of(settingsResource, true);
    for (CsvRow row : csv.rows()) {
      String curveNameStr = row.getField(SETTINGS_CURVE_NAME);
      String valueTypeStr = row.getField(SETTINGS_VALUE_TYPE);
      String dayCountStr = row.getField(SETTINGS_DAY_COUNT);
      String interpolatorStr = row.getField(SETTINGS_INTERPOLATOR);
      String leftExtrapolatorStr = row.getField(SETTINGS_LEFT_EXTRAPOLATOR);
      String rightExtrapolatorStr = row.getField(SETTINGS_RIGHT_EXTRAPOLATOR);

      if (!VALUE_TYPE_MAP.containsKey(valueTypeStr.toLowerCase(Locale.ENGLISH))) {
        throw new IllegalArgumentException(
            Messages.format("Unsupported {} in curve settings: {}", SETTINGS_VALUE_TYPE, valueTypeStr));
      }

      CurveName curveName = CurveName.of(curveNameStr);
      ValueType valueType = VALUE_TYPE_MAP.get(valueTypeStr.toLowerCase(Locale.ENGLISH));
      CurveInterpolator interpolator = CurveInterpolator.of(interpolatorStr);
      CurveExtrapolator leftExtrap = CurveExtrapolator.of(leftExtrapolatorStr);
      CurveExtrapolator rightExtrap = CurveExtrapolator.of(rightExtrapolatorStr);
      // ONE_ONE day count is not used
      DayCount dayCount = LoaderUtils.parseDayCount(dayCountStr);
      LoadedCurveSettings settings = LoadedCurveSettings.of(
          curveName, ValueType.YEAR_FRACTION, valueType, dayCount, interpolator, leftExtrap, rightExtrap);
      builder.put(curveName, settings);
    }
    return builder.build();
  }

  private static Multimap<LocalDate, Curve> parseSingle(
      Predicate<LocalDate> datePredicate,
      CharSource curvesResource,
      Map<CurveName, LoadedCurveSettings> settingsMap) {

    CsvFile csv = CsvFile.of(curvesResource, true);
    Map<LoadedCurveKey, List<LoadedCurveNode>> allNodes = new HashMap<>();
    for (CsvRow row : csv.rows()) {
      String dateStr = row.getField(CURVE_DATE);
      String curveNameStr = row.getField(CURVE_NAME);
      String pointDateStr = row.getField(CURVE_POINT_DATE);
      String pointValueStr = row.getField(CURVE_POINT_VALUE);
      String pointLabel = row.getField(CURVE_POINT_LABEL);

      LocalDate date = LoaderUtils.parseDate(dateStr);
      if (datePredicate.test(date)) {
        LocalDate pointDate = LoaderUtils.parseDate(pointDateStr);
        double pointValue = Double.valueOf(pointValueStr);

        LoadedCurveKey key = LoadedCurveKey.of(date, CurveName.of(curveNameStr));
        List<LoadedCurveNode> curveNodes = allNodes.computeIfAbsent(key, k -> new ArrayList<>());
        curveNodes.add(LoadedCurveNode.of(pointDate, pointValue, pointLabel));
      }
    }
    return buildCurves(settingsMap, allNodes);
  }

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
  private static void parseCurveMaps(
      CharSource groupsCharSource,
      Map<CurveGroupName, Map<Pair<RepoGroup, Currency>, CurveName>> repoGroups,
      Map<CurveGroupName, Map<Pair<LegalEntityGroup, Currency>, CurveName>> legalEntityGroups) {
    CsvFile csv = CsvFile.of(groupsCharSource, true);
    for (CsvRow row : csv.rows()) {
      String curveGroupStr = row.getField(GROUPS_NAME);
      String curveTypeStr = row.getField(GROUPS_CURVE_TYPE);
      String referenceStr = row.getField(GROUPS_REFERENCE);
      String currencyStr = row.getField(GROUPS_CURRENCY);
      String curveNameStr = row.getField(GROUPS_CURVE_NAME);
      CurveName curveName = CurveName.of(curveNameStr);
      createKey(
          curveName,
          CurveGroupName.of(curveGroupStr), curveTypeStr, referenceStr, currencyStr, repoGroups, legalEntityGroups);
    }
  }

  private static void createKey(
      CurveName curveName,
      CurveGroupName curveGroup,
      String curveTypeStr,
      String referenceStr,
      String currencyStr,
      Map<CurveGroupName, Map<Pair<RepoGroup, Currency>, CurveName>> repoGroups,
      Map<CurveGroupName, Map<Pair<LegalEntityGroup, Currency>, CurveName>> legalEntityGroups) {

    Currency currency = Currency.of(currencyStr);
    if (REPO.equalsIgnoreCase(curveTypeStr.toLowerCase(Locale.ENGLISH))) {
      RepoGroup repoGroup = RepoGroup.of(referenceStr);
      repoGroups.computeIfAbsent(curveGroup, k -> new LinkedHashMap<>()).put(Pair.of(repoGroup, currency), curveName);
    } else if (ISSUER.equalsIgnoreCase(curveTypeStr.toLowerCase(Locale.ENGLISH))) {
      LegalEntityGroup legalEntiryGroup = LegalEntityGroup.of(referenceStr);
      legalEntityGroups.computeIfAbsent(
          curveGroup, k -> new LinkedHashMap<>()).put(Pair.of(legalEntiryGroup, currency), curveName);
    } else {
      throw new IllegalArgumentException(Messages.format("Unsupported curve type: {}", curveTypeStr));
    }
  }

  //-------------------------------------------------------------------------
  private static Curve queryCurve(
      CurveName name,
      Map<CurveName, Curve> curves,
      LocalDate date,
      CurveGroupName groupName,
      String curveType) {

    Curve curve = curves.get(name);
    if (curve == null) {
      throw new IllegalArgumentException(
          curveType + " curve values for " + name.toString() + " in group " + groupName.getName() +
              " are missing on " + date.toString());
    }
    return curve;
  }

}
