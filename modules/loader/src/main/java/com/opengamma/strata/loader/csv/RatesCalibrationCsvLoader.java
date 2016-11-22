/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.toImmutableMap;
import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.google.common.math.DoubleMath;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.CurveNodeClashAction;
import com.opengamma.strata.market.curve.CurveNodeDate;
import com.opengamma.strata.market.curve.CurveNodeDateOrder;
import com.opengamma.strata.market.curve.NodalCurveDefinition;
import com.opengamma.strata.market.curve.node.FixedIborSwapCurveNode;
import com.opengamma.strata.market.curve.node.FixedInflationSwapCurveNode;
import com.opengamma.strata.market.curve.node.FixedOvernightSwapCurveNode;
import com.opengamma.strata.market.curve.node.FraCurveNode;
import com.opengamma.strata.market.curve.node.FxSwapCurveNode;
import com.opengamma.strata.market.curve.node.IborFixingDepositCurveNode;
import com.opengamma.strata.market.curve.node.IborFutureCurveNode;
import com.opengamma.strata.market.curve.node.IborIborSwapCurveNode;
import com.opengamma.strata.market.curve.node.OvernightIborSwapCurveNode;
import com.opengamma.strata.market.curve.node.TermDepositCurveNode;
import com.opengamma.strata.market.curve.node.ThreeLegBasisSwapCurveNode;
import com.opengamma.strata.market.curve.node.XCcyIborIborSwapCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.product.deposit.type.IborFixingDepositConvention;
import com.opengamma.strata.product.deposit.type.IborFixingDepositTemplate;
import com.opengamma.strata.product.deposit.type.TermDepositConvention;
import com.opengamma.strata.product.deposit.type.TermDepositTemplate;
import com.opengamma.strata.product.fra.type.FraConvention;
import com.opengamma.strata.product.fra.type.FraTemplate;
import com.opengamma.strata.product.fx.type.FxSwapConvention;
import com.opengamma.strata.product.fx.type.FxSwapTemplate;
import com.opengamma.strata.product.index.type.IborFutureConvention;
import com.opengamma.strata.product.index.type.IborFutureTemplate;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.product.swap.type.FixedIborSwapTemplate;
import com.opengamma.strata.product.swap.type.FixedInflationSwapConvention;
import com.opengamma.strata.product.swap.type.FixedInflationSwapTemplate;
import com.opengamma.strata.product.swap.type.FixedOvernightSwapConvention;
import com.opengamma.strata.product.swap.type.FixedOvernightSwapTemplate;
import com.opengamma.strata.product.swap.type.IborIborSwapConvention;
import com.opengamma.strata.product.swap.type.IborIborSwapTemplate;
import com.opengamma.strata.product.swap.type.OvernightIborSwapConvention;
import com.opengamma.strata.product.swap.type.OvernightIborSwapTemplate;
import com.opengamma.strata.product.swap.type.ThreeLegBasisSwapConvention;
import com.opengamma.strata.product.swap.type.ThreeLegBasisSwapTemplate;
import com.opengamma.strata.product.swap.type.XCcyIborIborSwapConvention;
import com.opengamma.strata.product.swap.type.XCcyIborIborSwapTemplate;

/**
 * Loads a set of definitions to calibrate rates curves by reading from CSV resources.
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
 * The third file is the curve calibration nodes file.
 * This file has the following header row:<br />
 * {@code Curve Name,Label,Symbology,Ticker,Field Name,Type,Convention,Time,Date,Min Gap,Clash Action,Spread}.
 * <ul>
 * <li>The 'Curve Name' column is the name of the curve.
 * <li>The 'Label' column is the label used to refer to the node.
 * <li>The 'Symbology' column is the symbology scheme applicable to the ticker used for the market price.
 * <li>The 'Ticker' column is the identifier within the symbology used for the market price.
 * <li>The 'Field Name' column is the field name used for the market price, defaulted to "MarketValue", allowing
 *  fields such as 'Bid' or 'Ask' to be specified.
 * <li>The 'Type' column is the type of the instrument, such as "FRA" or "OIS".
 * <li>The 'Convention' column is the name of the convention to use.
 * <li>The 'Time' column is the description of the time, such as "1Y" for a 1 year swap, or "3Mx6M" for a FRA.
 * <li>The optional 'Date' column is the date to use for the node, defaults to "End", but can be
 *  set to "LastFixing" or a yyyy-MM-dd date.
 * <li>The optional 'Min Gap' column is the minimum gap between this node and the adjacent nodes.
 * <li>The optional 'Clash Action' column is the action to perform if the nodes are closer than the minimum gap
 *  or in the wrong order, defaults to "Exception", but can be set to "DropThis" or "DropOther".
 * <li>The optional 'Spread' column is the spread to add to the instrument.
 * </ul>
 * <p>
 * Each curve must be contained entirely within a single file, but each file may contain more than
 * one curve. The curve points do not need to be ordered.
 */
public final class RatesCalibrationCsvLoader {

  // CSV column headers
  private static final String CURVE_NAME = "Curve Name";
  private static final String CURVE_LABEL = "Label";
  private static final String CURVE_SYMBOLOGY_QUOTE = "Symbology";
  private static final String CURVE_TICKER_QUOTE = "Ticker";
  private static final String CURVE_FIELD_QUOTE = "Field Name";
  private static final String CURVE_TYPE = "Type";
  private static final String CURVE_CONVENTION = "Convention";
  private static final String CURVE_TIME = "Time";
  private static final String CURVE_DATE = "Date";
  private static final String CURVE_SPREAD = "Spread";
  private static final String CURVE_MIN_GAP = "Min Gap";
  private static final String CURVE_CLASH_ACTION = "Clash Action";

  // Regex to parse FRA time string
  private static final Pattern FRA_TIME_REGEX = Pattern.compile("P?([0-9]+)M? ?X ?P?([0-9]+)M?");
  // Regex to parse future time string
  private static final Pattern FUT_TIME_REGEX = Pattern.compile("P?((?:[0-9]+D)?(?:[0-9]+W)?(?:[0-9]+M)?) ?[+] ?([0-9]+)");
  // Regex to parse future month string
  private static final Pattern FUT_MONTH_REGEX = Pattern.compile("([A-Z][A-Z][A-Z][0-9][0-9])");
  // Regex to parse simple time string with years, months and days
  private static final Pattern SIMPLE_YMD_TIME_REGEX = Pattern.compile("P?(([0-9]+Y)?([0-9]+M)?([0-9]+W)?([0-9]+D)?)");
  // Regex to parse simple time string with years and months
  private static final Pattern SIMPLE_YM_TIME_REGEX = Pattern.compile("P?(([0-9]+Y)?([0-9]+M)?)");
  // Regex to parse simple time string with days
  private static final Pattern SIMPLE_DAYS_REGEX = Pattern.compile("P?([0-9]+D)?");
  // parse year-month
  private static final DateTimeFormatter YM_FORMATTER = new DateTimeFormatterBuilder()
      .parseCaseInsensitive().appendPattern("MMMuu").toFormatter(Locale.ENGLISH);

  //-------------------------------------------------------------------------
  /**
   * Loads one or more CSV format curve calibration files.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   * 
   * @param groupsResource  the curve groups CSV resource
   * @param settingsResource  the curve settings CSV resource
   * @param curveNodeResources  the CSV resources for curve nodes
   * @return the group definitions, mapped by name
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableMap<CurveGroupName, CurveGroupDefinition> load(
      ResourceLocator groupsResource,
      ResourceLocator settingsResource,
      ResourceLocator... curveNodeResources) {

    return load(groupsResource, settingsResource, ImmutableList.copyOf(curveNodeResources));
  }

  /**
   * Loads one or more CSV format curve calibration files.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   * 
   * @param groupsResource  the curve groups CSV resource
   * @param settingsResource  the curve settings CSV resource
   * @param curveNodeResources  the CSV resources for curve nodes
   * @return the group definitions, mapped by name
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableMap<CurveGroupName, CurveGroupDefinition> load(
      ResourceLocator groupsResource,
      ResourceLocator settingsResource,
      Collection<ResourceLocator> curveNodeResources) {

    Collection<CharSource> curveNodeCharSources = curveNodeResources.stream().map(r -> r.getCharSource()).collect(toList());
    return parse(groupsResource.getCharSource(), settingsResource.getCharSource(), curveNodeCharSources);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses one or more CSV format curve calibration files.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   * 
   * @param groupsCharSource  the curve groups CSV character source
   * @param settingsCharSource  the curve settings CSV character source
   * @param curveNodeCharSources  the CSV character sources for curve nodes
   * @return the group definitions, mapped by name
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableMap<CurveGroupName, CurveGroupDefinition> parse(
      CharSource groupsCharSource,
      CharSource settingsCharSource,
      Collection<CharSource> curveNodeCharSources) {

    // load curve groups and settings
    List<CurveGroupDefinition> curveGroups = CurveGroupDefinitionCsvLoader.parseCurveGroupDefinitions(groupsCharSource);
    Map<CurveName, LoadedCurveSettings> settingsMap = RatesCurvesCsvLoader.parseCurveSettings(settingsCharSource);

    // load curve definitions
    List<NodalCurveDefinition> curveDefinitions = curveNodeCharSources.stream()
        .flatMap(res -> parseSingle(res, settingsMap).stream())
        .collect(toImmutableList());

    // Add the curve definitions to the curve group definitions
    return curveGroups.stream()
        .map(groupDefinition -> groupDefinition.withCurveDefinitions(curveDefinitions))
        .collect(toImmutableMap(groupDefinition -> groupDefinition.getName()));
  }

  //-------------------------------------------------------------------------
  // loads a single curves CSV file
  // requestedDate can be null, meaning load all dates
  private static List<NodalCurveDefinition> parseSingle(
      CharSource resource,
      Map<CurveName, LoadedCurveSettings> settingsMap) {

    CsvFile csv = CsvFile.of(resource, true);
    Map<CurveName, List<CurveNode>> allNodes = new HashMap<>();
    for (CsvRow row : csv.rows()) {
      String curveNameStr = row.getField(CURVE_NAME);
      String label = row.getField(CURVE_LABEL);
      String symbologyQuoteStr = row.getField(CURVE_SYMBOLOGY_QUOTE);
      String tickerQuoteStr = row.getField(CURVE_TICKER_QUOTE);
      String fieldQuoteStr = row.getField(CURVE_FIELD_QUOTE);
      String typeStr = row.getField(CURVE_TYPE);
      String conventionStr = row.getField(CURVE_CONVENTION);
      String timeStr = row.getField(CURVE_TIME);
      String dateStr = row.findField(CURVE_DATE).orElse("");
      String minGapStr = row.findField(CURVE_MIN_GAP).orElse("");
      String clashActionStr = row.findField(CURVE_CLASH_ACTION).orElse("");
      String spreadStr = row.findField(CURVE_SPREAD).orElse("");

      CurveName curveName = CurveName.of(curveNameStr);
      StandardId quoteStandardId = StandardId.of(symbologyQuoteStr, tickerQuoteStr);
      FieldName quoteField = fieldQuoteStr.isEmpty() ? FieldName.MARKET_VALUE : FieldName.of(fieldQuoteStr);
      QuoteId quoteId = QuoteId.of(quoteStandardId, quoteField);
      double spread = spreadStr.isEmpty() ? 0d : Double.parseDouble(spreadStr);
      CurveNodeDate date = parseDate(dateStr);
      CurveNodeDateOrder order = parseDateOrder(minGapStr, clashActionStr);

      List<CurveNode> curveNodes = allNodes.computeIfAbsent(curveName, k -> new ArrayList<>());
      curveNodes.add(createCurveNode(typeStr, conventionStr, timeStr, label, quoteId, spread, date, order));
    }
    return buildCurveDefinition(settingsMap, allNodes);
  }

  // parse date order
  private static CurveNodeDate parseDate(String dateStr) {
    if (dateStr.isEmpty()) {
      return CurveNodeDate.END;
    }
    if (dateStr.length() == 10 && dateStr.charAt(4) == '-' && dateStr.charAt(7) == '-') {
      return CurveNodeDate.of(LocalDate.parse(dateStr));
    }
    String dateUpper = dateStr.toUpperCase(Locale.ENGLISH);
    if (dateUpper.equals("END")) {
      return CurveNodeDate.END;
    }
    if (dateUpper.equals("LASTFIXING")) {
      return CurveNodeDate.LAST_FIXING;
    }
    throw new IllegalArgumentException(Messages.format(
        "Invalid format for node date, should be date in 'yyyy-MM-dd' format, 'End' or 'LastFixing': {}", dateUpper));
  }

  // parse date order
  private static CurveNodeDateOrder parseDateOrder(String minGapStr, String clashActionStr) {
    CurveNodeClashAction clashAction =
        clashActionStr.isEmpty() ? CurveNodeClashAction.EXCEPTION : CurveNodeClashAction.of(clashActionStr);
    if (minGapStr.isEmpty()) {
      return CurveNodeDateOrder.of(1, clashAction);
    }
    Matcher matcher = SIMPLE_DAYS_REGEX.matcher(minGapStr.toUpperCase(Locale.ENGLISH));
    if (!matcher.matches()) {
      throw new IllegalArgumentException(Messages.format(
          "Invalid days format for minimum gap, should be 2D or P2D: {}", minGapStr));
    }
    Period minGap = Period.parse("P" + matcher.group(1));
    return CurveNodeDateOrder.of(minGap.getDays(), clashAction);
  }

  // build the curves
  private static List<NodalCurveDefinition> buildCurveDefinition(
      Map<CurveName, LoadedCurveSettings> settingsMap,
      Map<CurveName, List<CurveNode>> allNodes) {

    ImmutableList.Builder<NodalCurveDefinition> results = ImmutableList.builder();

    for (Map.Entry<CurveName, List<CurveNode>> entry : allNodes.entrySet()) {
      CurveName name = entry.getKey();
      LoadedCurveSettings settings = settingsMap.get(name);

      if (settings == null) {
        throw new IllegalArgumentException(Messages.format("Missing settings for curve: {}", name));
      }
      results.add(settings.createCurveDefinition(entry.getValue()));
    }
    return results.build();
  }

  //-------------------------------------------------------------------------
  // create the curve node
  private static CurveNode createCurveNode(
      String typeStr,
      String conventionStr,
      String timeStr,
      String label,
      QuoteId quoteId,
      double spread,
      CurveNodeDate date,
      CurveNodeDateOrder order) {

    if ("DEP".equalsIgnoreCase(typeStr) || "TermDeposit".equalsIgnoreCase(typeStr)) {
      return curveTermDepositCurveNode(conventionStr, timeStr, label, quoteId, spread, date, order);
    }
    if ("FIX".equalsIgnoreCase(typeStr) || "IborFixingDeposit".equalsIgnoreCase(typeStr)) {
      return curveIborFixingDepositCurveNode(conventionStr, label, quoteId, spread, date, order);
    }
    if ("FRA".equalsIgnoreCase(typeStr)) {
      return curveFraCurveNode(conventionStr, timeStr, label, quoteId, spread, date, order);
    }
    if ("IFU".equalsIgnoreCase(typeStr) || "IborFuture".equalsIgnoreCase(typeStr)) {
      return curveIborFutureCurveNode(conventionStr, timeStr, label, quoteId, spread, date, order);
    }
    if ("OIS".equalsIgnoreCase(typeStr) || "FixedOvernightSwap".equalsIgnoreCase(typeStr)) {
      return curveFixedOvernightCurveNode(conventionStr, timeStr, label, quoteId, spread, date, order);
    }
    if ("IRS".equalsIgnoreCase(typeStr) || "FixedIborSwap".equalsIgnoreCase(typeStr)) {
      return curveFixedIborCurveNode(conventionStr, timeStr, label, quoteId, spread, date, order);
    }
    if ("BAS".equalsIgnoreCase(typeStr) || "IborIborSwap".equalsIgnoreCase(typeStr)) {
      return curveIborIborCurveNode(conventionStr, timeStr, label, quoteId, spread, date, order);
    }
    if ("BS3".equalsIgnoreCase(typeStr) || "ThreeLegBasisSwap".equalsIgnoreCase(typeStr)) {
      return curveThreeLegBasisCurveNode(conventionStr, timeStr, label, quoteId, spread, date, order);
    }
    if ("ONI".equalsIgnoreCase(typeStr) || "OvernightIborBasisSwap".equalsIgnoreCase(typeStr)) {
      return curveOvernightIborCurveNode(conventionStr, timeStr, label, quoteId, spread, date, order);
    }
    if ("XCS".equalsIgnoreCase(typeStr) || "XCcyIborIborSwap".equalsIgnoreCase(typeStr)) {
      return curveXCcyIborIborCurveNode(conventionStr, timeStr, label, quoteId, spread, date, order);
    }
    if ("FXS".equalsIgnoreCase(typeStr) || "FxSwap".equalsIgnoreCase(typeStr)) {
      return curveFxSwapCurveNode(conventionStr, timeStr, label, quoteId, spread, date, order);
    }
    if ("INF".equalsIgnoreCase(typeStr) || "FixedInflationSwap".equalsIgnoreCase(typeStr)) {
      return curveFixedInflationCurveNode(conventionStr, timeStr, label, quoteId, spread, date, order);
    }
    throw new IllegalArgumentException(Messages.format("Invalid curve node type: {}", typeStr));
  }

  private static CurveNode curveTermDepositCurveNode(
      String conventionStr,
      String timeStr,
      String label,
      QuoteId quoteId,
      double spread,
      CurveNodeDate date,
      CurveNodeDateOrder order) {

    Matcher matcher = SIMPLE_YMD_TIME_REGEX.matcher(timeStr.toUpperCase(Locale.ENGLISH));
    if (!matcher.matches()) {
      throw new IllegalArgumentException(Messages.format("Invalid time format for Term Deposit: {}", timeStr));
    }
    Period periodToEnd = Period.parse("P" + matcher.group(1));
    TermDepositConvention convention = TermDepositConvention.of(conventionStr);
    TermDepositTemplate template = TermDepositTemplate.of(periodToEnd, convention);
    return TermDepositCurveNode.builder()
        .template(template)
        .rateId(quoteId)
        .additionalSpread(spread)
        .label(label)
        .date(date)
        .dateOrder(order)
        .build();
  }

  private static CurveNode curveIborFixingDepositCurveNode(
      String conventionStr,
      String label,
      QuoteId quoteId,
      double spread,
      CurveNodeDate date,
      CurveNodeDateOrder order) {

    IborFixingDepositConvention convention = IborFixingDepositConvention.of(conventionStr);
    IborFixingDepositTemplate template = IborFixingDepositTemplate.of(
        convention.getIndex().getTenor().getPeriod(), convention);
    return IborFixingDepositCurveNode.builder()
        .template(template)
        .rateId(quoteId)
        .additionalSpread(spread)
        .label(label)
        .date(date)
        .dateOrder(order)
        .build();
  }

  private static CurveNode curveFraCurveNode(
      String conventionStr,
      String timeStr,
      String label,
      QuoteId quoteId,
      double spread,
      CurveNodeDate date,
      CurveNodeDateOrder order) {

    Matcher matcher = FRA_TIME_REGEX.matcher(timeStr.toUpperCase(Locale.ENGLISH));
    if (!matcher.matches()) {
      throw new IllegalArgumentException(Messages.format("Invalid time format for FRA: {}", timeStr));
    }
    Period periodToStart = Period.parse("P" + matcher.group(1) + "M");
    Period periodToEnd = Period.parse("P" + matcher.group(2) + "M");

    FraConvention convention = FraConvention.of(conventionStr);
    FraTemplate template = FraTemplate.of(periodToStart, periodToEnd, convention);
    return FraCurveNode.builder()
        .template(template)
        .rateId(quoteId)
        .additionalSpread(spread)
        .label(label)
        .date(date)
        .dateOrder(order)
        .build();
  }

  private static CurveNode curveIborFutureCurveNode(
      String conventionStr,
      String timeStr,
      String label,
      QuoteId quoteId,
      double spread,
      CurveNodeDate date,
      CurveNodeDateOrder order) {

    Matcher matcher = FUT_TIME_REGEX.matcher(timeStr.toUpperCase(Locale.ENGLISH));
    if (matcher.matches()) {
      Period periodToStart = Period.parse("P" + matcher.group(1));
      int sequenceNumber = Integer.parseInt(matcher.group(2));
      IborFutureConvention convention = IborFutureConvention.of(conventionStr);
      IborFutureTemplate template = IborFutureTemplate.of(periodToStart, sequenceNumber, convention);
      return IborFutureCurveNode.builder()
          .template(template)
          .rateId(quoteId)
          .additionalSpread(spread)
          .label(label)
          .date(date)
          .dateOrder(order)
          .build();
    }
    Matcher matcher2 = FUT_MONTH_REGEX.matcher(timeStr.toUpperCase(Locale.ENGLISH));
    if (matcher2.matches()) {
      YearMonth yearMonth = YearMonth.parse(matcher2.group(1), YM_FORMATTER);
      IborFutureConvention convention = IborFutureConvention.of(conventionStr);
      IborFutureTemplate template = IborFutureTemplate.of(yearMonth, convention);
      return IborFutureCurveNode.builder()
          .template(template)
          .rateId(quoteId)
          .additionalSpread(spread)
          .label(label)
          .date(date)
          .dateOrder(order)
          .build();
    }
    throw new IllegalArgumentException(Messages.format("Invalid time format for Ibor Future: {}", timeStr));
  }

  //-------------------------------------------------------------------------
  private static CurveNode curveFixedOvernightCurveNode(
      String conventionStr,
      String timeStr,
      String label,
      QuoteId quoteId,
      double spread,
      CurveNodeDate date,
      CurveNodeDateOrder order) {

    Matcher matcher = SIMPLE_YMD_TIME_REGEX.matcher(timeStr.toUpperCase(Locale.ENGLISH));
    if (!matcher.matches()) {
      throw new IllegalArgumentException(Messages.format("Invalid time format for Fixed-Overnight swap: {}", timeStr));
    }
    Period periodToEnd = Period.parse("P" + matcher.group(1));
    FixedOvernightSwapConvention convention = FixedOvernightSwapConvention.of(conventionStr);
    FixedOvernightSwapTemplate template = FixedOvernightSwapTemplate.of(Tenor.of(periodToEnd), convention);
    return FixedOvernightSwapCurveNode.builder()
        .template(template)
        .rateId(quoteId)
        .additionalSpread(spread)
        .label(label)
        .date(date)
        .dateOrder(order)
        .build();
  }

  private static CurveNode curveFixedIborCurveNode(
      String conventionStr,
      String timeStr,
      String label,
      QuoteId quoteId,
      double spread,
      CurveNodeDate date,
      CurveNodeDateOrder order) {

    Matcher matcher = SIMPLE_YM_TIME_REGEX.matcher(timeStr.toUpperCase(Locale.ENGLISH));
    if (!matcher.matches()) {
      throw new IllegalArgumentException(Messages.format("Invalid time format for Fixed-Ibor swap: {}", timeStr));
    }
    Period periodToEnd = Period.parse("P" + matcher.group(1));
    FixedIborSwapConvention convention = FixedIborSwapConvention.of(conventionStr);
    FixedIborSwapTemplate template = FixedIborSwapTemplate.of(Tenor.of(periodToEnd), convention);
    return FixedIborSwapCurveNode.builder()
        .template(template)
        .rateId(quoteId)
        .additionalSpread(spread)
        .label(label)
        .date(date)
        .dateOrder(order)
        .build();
  }

  private static CurveNode curveIborIborCurveNode(
      String conventionStr,
      String timeStr,
      String label,
      QuoteId quoteId,
      double spread,
      CurveNodeDate date,
      CurveNodeDateOrder order) {

    Matcher matcher = SIMPLE_YM_TIME_REGEX.matcher(timeStr.toUpperCase(Locale.ENGLISH));
    if (!matcher.matches()) {
      throw new IllegalArgumentException(Messages.format("Invalid time format for Ibor-Ibor swap: {}", timeStr));
    }
    Period periodToEnd = Period.parse("P" + matcher.group(1));
    IborIborSwapConvention convention = IborIborSwapConvention.of(conventionStr);
    IborIborSwapTemplate template = IborIborSwapTemplate.of(Tenor.of(periodToEnd), convention);
    return IborIborSwapCurveNode.builder()
        .template(template)
        .rateId(quoteId)
        .additionalSpread(spread)
        .label(label)
        .date(date)
        .dateOrder(order)
        .build();
  }

  private static CurveNode curveThreeLegBasisCurveNode(
      String conventionStr,
      String timeStr,
      String label,
      QuoteId quoteId,
      double spread,
      CurveNodeDate date,
      CurveNodeDateOrder order) {

    Matcher matcher = SIMPLE_YM_TIME_REGEX.matcher(timeStr.toUpperCase(Locale.ENGLISH));
    if (!matcher.matches()) {
      throw new IllegalArgumentException(Messages.format("Invalid time format for Three legs basis swap: {}", timeStr));
    }
    Period periodToEnd = Period.parse("P" + matcher.group(1));
    ThreeLegBasisSwapConvention convention = ThreeLegBasisSwapConvention.of(conventionStr);
    ThreeLegBasisSwapTemplate template = ThreeLegBasisSwapTemplate.of(Tenor.of(periodToEnd), convention);
    return ThreeLegBasisSwapCurveNode.builder()
        .template(template)
        .rateId(quoteId)
        .additionalSpread(spread)
        .label(label)
        .date(date)
        .dateOrder(order)
        .build();
  }

  private static CurveNode curveXCcyIborIborCurveNode(
      String conventionStr,
      String timeStr,
      String label,
      QuoteId quoteId,
      double spread,
      CurveNodeDate date,
      CurveNodeDateOrder order) {

    Matcher matcher = SIMPLE_YM_TIME_REGEX.matcher(timeStr.toUpperCase(Locale.ENGLISH));
    if (!matcher.matches()) {
      throw new IllegalArgumentException(Messages.format("Invalid time format for Cross Currency Swap: {}", timeStr));
    }
    Period periodToEnd = Period.parse("P" + matcher.group(1));
    XCcyIborIborSwapConvention convention = XCcyIborIborSwapConvention.of(conventionStr);
    XCcyIborIborSwapTemplate template = XCcyIborIborSwapTemplate.of(Tenor.of(periodToEnd), convention);
    return XCcyIborIborSwapCurveNode.builder()
        .template(template)
        .spreadId(quoteId)
        .additionalSpread(spread)
        .label(label)
        .date(date)
        .dateOrder(order)
        .build();
  }

  private static CurveNode curveOvernightIborCurveNode(
      String conventionStr,
      String timeStr,
      String label,
      QuoteId quoteId,
      double spread,
      CurveNodeDate date,
      CurveNodeDateOrder order) {

    Matcher matcher = SIMPLE_YMD_TIME_REGEX.matcher(timeStr.toUpperCase(Locale.ENGLISH));
    if (!matcher.matches()) {
      throw new IllegalArgumentException(Messages.format("Invalid time format for Overnight-Ibor swap: {}", timeStr));
    }
    Period periodToEnd = Period.parse("P" + matcher.group(1));
    OvernightIborSwapConvention convention = OvernightIborSwapConvention.of(conventionStr);
    OvernightIborSwapTemplate template = OvernightIborSwapTemplate.of(Tenor.of(periodToEnd), convention);
    return OvernightIborSwapCurveNode.builder()
        .template(template)
        .rateId(quoteId)
        .additionalSpread(spread)
        .label(label)
        .date(date)
        .dateOrder(order)
        .build();
  }

  private static CurveNode curveFxSwapCurveNode(
      String conventionStr,
      String timeStr,
      String label,
      QuoteId quoteId,
      double spread,
      CurveNodeDate date,
      CurveNodeDateOrder order) {

    if (!DoubleMath.fuzzyEquals(spread, 0d, 1e-10d)) {
      throw new IllegalArgumentException("Additional spread must be zero for FX swaps");
    }
    Matcher matcher = SIMPLE_YMD_TIME_REGEX.matcher(timeStr.toUpperCase(Locale.ENGLISH));
    if (!matcher.matches()) {
      throw new IllegalArgumentException(Messages.format("Invalid time format for FX swap: {}", timeStr));
    }
    Period periodToEnd = Period.parse("P" + matcher.group(1));
    FxSwapConvention convention = FxSwapConvention.of(conventionStr);
    FxSwapTemplate template = FxSwapTemplate.of(periodToEnd, convention);
    return FxSwapCurveNode.builder()
        .template(template)
        .farForwardPointsId(quoteId)
        .label(label)
        .date(date)
        .dateOrder(order)
        .build();
  }

  private static CurveNode curveFixedInflationCurveNode(
      String conventionStr,
      String timeStr,
      String label,
      QuoteId quoteId,
      double spread,
      CurveNodeDate date,
      CurveNodeDateOrder order) {

    Matcher matcher = SIMPLE_YM_TIME_REGEX.matcher(timeStr.toUpperCase(Locale.ENGLISH));
    if (!matcher.matches()) {
      throw new IllegalArgumentException(Messages.format("Invalid time format for Fixed-Inflation swap: {}", timeStr));
    }
    Period periodToEnd = Period.parse("P" + matcher.group(1));
    FixedInflationSwapConvention convention = FixedInflationSwapConvention.of(conventionStr);
    FixedInflationSwapTemplate template = FixedInflationSwapTemplate.of(Tenor.of(periodToEnd), convention);
    return FixedInflationSwapCurveNode.builder()
        .template(template)
        .rateId(quoteId)
        .additionalSpread(spread)
        .label(label)
        .date(date)
        .dateOrder(order)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private RatesCalibrationCsvLoader() {
  }

}
