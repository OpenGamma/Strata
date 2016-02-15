/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.market.FieldName;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.NodalCurveDefinition;
import com.opengamma.strata.market.curve.node.FixedIborSwapCurveNode;
import com.opengamma.strata.market.curve.node.FixedOvernightSwapCurveNode;
import com.opengamma.strata.market.curve.node.FraCurveNode;
import com.opengamma.strata.market.curve.node.FxSwapCurveNode;
import com.opengamma.strata.market.curve.node.IborFixingDepositCurveNode;
import com.opengamma.strata.market.curve.node.IborFutureCurveNode;
import com.opengamma.strata.market.curve.node.IborIborSwapCurveNode;
import com.opengamma.strata.market.curve.node.TermDepositCurveNode;
import com.opengamma.strata.market.curve.node.ThreeLegBasisSwapCurveNode;
import com.opengamma.strata.market.curve.node.XCcyIborIborSwapCurveNode;
import com.opengamma.strata.market.key.QuoteKey;
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
import com.opengamma.strata.product.swap.type.FixedOvernightSwapConvention;
import com.opengamma.strata.product.swap.type.FixedOvernightSwapTemplate;
import com.opengamma.strata.product.swap.type.IborIborSwapConvention;
import com.opengamma.strata.product.swap.type.IborIborSwapTemplate;
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
 * {@code Curve Name,Label,Symbology,Ticker,Field Name,Type,Convention,Time,Spread}.
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
 * <li>The 'Spread' column is the spread to add to the instrument.
 * </ul>
 * <p>
 * Note that the three FX fields are only needed if specifying a cross-currency product.
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
  private static final String CURVE_SPREAD = "Spread";

  // Regex to parse FRA time string
  private static final Pattern FRA_TIME_REGEX = Pattern.compile("P?([0-9]+)M? ?X ?P?([0-9]+)M?");
  // Regex to parse FRA time string
  private static final Pattern FUT_TIME_REGEX = Pattern.compile("P?((?:[0-9]+D)?(?:[0-9]+W)?(?:[0-9]+M)?) ?[+] ?([0-9]+)");
  // Regex to parse simple time string
  private static final Pattern SIMPLE_TIME_REGEX = Pattern.compile("P?(([0-9]+M)?([0-9]+Y)?)");

  //-------------------------------------------------------------------------
  /**
   * Loads one or more CSV format curve calibration files.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   * 
   * @param groupsResource  the curve groups CSV resource
   * @param settingsResource  the curve settings CSV resource
   * @param curveResources  the CSV resources for curves
   * @return the loaded curves, mapped by an identifying key
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static List<CurveGroupDefinition> load(
      ResourceLocator groupsResource,
      ResourceLocator settingsResource,
      ResourceLocator... curveResources) {

    return load(groupsResource, settingsResource, ImmutableList.copyOf(curveResources));
  }

  /**
   * Loads one or more CSV format curve calibration files.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   * 
   * @param groupsResource  the curve groups CSV resource
   * @param settingsResource  the curve settings CSV resource
   * @param curveResources  the CSV resources for curves
   * @return the loaded curves, mapped by an identifying key
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static List<CurveGroupDefinition> load(
      ResourceLocator groupsResource,
      ResourceLocator settingsResource,
      Collection<ResourceLocator> curveResources) {

    // load curve groups and settings
    List<CurveGroupDefinition> curveGroups = CurveGroupDefinitionCsvLoader.loadCurveGroups(groupsResource);
    Map<CurveName, LoadedCurveSettings> settingsMap = RatesCurvesCsvLoader.loadCurveSettings(settingsResource);

    // load curve definitions
    List<NodalCurveDefinition> curveDefinitions = curveResources.stream()
        .flatMap(res -> loadSingle(res, settingsMap).stream())
        .collect(toImmutableList());

    // Add the curve definitions to the curve group definitions
    return curveGroups.stream()
        .map(groupDefinition -> groupDefinition.withCurveDefinitions(curveDefinitions))
        .collect(toImmutableList());
  }

  //-------------------------------------------------------------------------
  // loads a single curves CSV file
  // requestedDate can be null, meaning load all dates
  private static List<NodalCurveDefinition> loadSingle(
      ResourceLocator resource,
      Map<CurveName, LoadedCurveSettings> settingsMap) {

    CsvFile csv = CsvFile.of(resource.getCharSource(), true);
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
      String spreadStr = row.getField(CURVE_SPREAD);

      CurveName curveName = CurveName.of(curveNameStr);
      StandardId quoteId = StandardId.of(symbologyQuoteStr, tickerQuoteStr);
      FieldName quoteField = fieldQuoteStr.isEmpty() ? FieldName.MARKET_VALUE : FieldName.of(fieldQuoteStr);
      QuoteKey quoteKey = QuoteKey.of(quoteId, quoteField);
      double spread = spreadStr.isEmpty() ? 0d : Double.parseDouble(spreadStr);

      List<CurveNode> curveNodes = allNodes.computeIfAbsent(curveName, k -> new ArrayList<>());
      curveNodes.add(createCurveNode(typeStr, conventionStr, timeStr, label, quoteKey, spread));
    }
    return buildCurveDefinition(settingsMap, allNodes);
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
      QuoteKey quoteKey,
      double spread) {

    if ("DEP".equalsIgnoreCase(typeStr) || "TermDeposit".equalsIgnoreCase(typeStr)) {
      return curveTermDepositCurveNode(conventionStr, timeStr, label, quoteKey, spread);
    }
    if ("FIX".equalsIgnoreCase(typeStr) || "IborFixingDeposit".equalsIgnoreCase(typeStr)) {
      return curveIborFixingDepositCurveNode(conventionStr, label, quoteKey, spread);
    }
    if ("FRA".equalsIgnoreCase(typeStr)) {
      return curveFraCurveNode(conventionStr, timeStr, label, quoteKey, spread);
    }
    if ("IFU".equalsIgnoreCase(typeStr) || "IborFuture".equalsIgnoreCase(typeStr)) {
      return curveIborFuturesCurveNode(conventionStr, timeStr, label, quoteKey, spread);
    }
    if ("OIS".equalsIgnoreCase(typeStr) || "FixedOvernightSwap".equalsIgnoreCase(typeStr)) {
      return curveFixedOvernightCurveNode(conventionStr, timeStr, label, quoteKey, spread);
    }
    if ("IRS".equalsIgnoreCase(typeStr) || "FixedIborSwap".equalsIgnoreCase(typeStr)) {
      return curveFixedIborCurveNode(conventionStr, timeStr, label, quoteKey, spread);
    }
    if ("BAS".equalsIgnoreCase(typeStr) || "IborIborSwap".equalsIgnoreCase(typeStr)) {
      return curveIborIborCurveNode(conventionStr, timeStr, label, quoteKey, spread);
    }
    if ("BS3".equalsIgnoreCase(typeStr) || "ThreeLegBasisSwap".equalsIgnoreCase(typeStr)) {
      return curveThreeLegBasisCurveNode(conventionStr, timeStr, label, quoteKey, spread);
    }
    if ("XCS".equalsIgnoreCase(typeStr) || "XCcyIborIborSwap".equalsIgnoreCase(typeStr)) {
      return curveXCcyIborIborCurveNode(conventionStr, timeStr, label, quoteKey, spread);
    }
    if ("FXS".equalsIgnoreCase(typeStr) || "FxSwap".equalsIgnoreCase(typeStr)) {
      return curveFxSwapCurveNode(conventionStr, timeStr, label, quoteKey, spread);
    }
    throw new IllegalArgumentException(Messages.format("Invalid curve node type: {}", typeStr));
  }

  private static CurveNode curveTermDepositCurveNode(
      String conventionStr,
      String timeStr,
      String label,
      QuoteKey quoteKey,
      double spread) {

    Matcher matcher = SIMPLE_TIME_REGEX.matcher(timeStr.toUpperCase(Locale.ENGLISH));
    if (!matcher.matches()) {
      throw new IllegalArgumentException(Messages.format("Invalid time format for Term Deposit: {}", timeStr));
    }
    Period periodToEnd = Period.parse("P" + matcher.group(1));
    TermDepositConvention convention = TermDepositConvention.of(conventionStr);
    TermDepositTemplate template = TermDepositTemplate.of(periodToEnd, convention);
    return TermDepositCurveNode.of(template, quoteKey, spread, label);
  }

  private static CurveNode curveIborFixingDepositCurveNode(
      String conventionStr,
      String label,
      QuoteKey quoteKey,
      double spread) {

    IborFixingDepositConvention convention = IborFixingDepositConvention.of(conventionStr);
    IborFixingDepositTemplate template = IborFixingDepositTemplate.of(
        convention.getIndex().getTenor().getPeriod(), convention);
    return IborFixingDepositCurveNode.of(template, quoteKey, spread, label);
  }

  private static CurveNode curveFraCurveNode(
      String conventionStr,
      String timeStr,
      String label,
      QuoteKey quoteKey,
      double spread) {

    Matcher matcher = FRA_TIME_REGEX.matcher(timeStr.toUpperCase(Locale.ENGLISH));
    if (!matcher.matches()) {
      throw new IllegalArgumentException(Messages.format("Invalid time format for FRA: {}", timeStr));
    }
    Period periodToStart = Period.parse("P" + matcher.group(1) + "M");
    Period periodToEnd = Period.parse("P" + matcher.group(2) + "M");

    FraConvention convention = FraConvention.of(conventionStr);
    FraTemplate template = FraTemplate.of(periodToStart, periodToEnd, convention);
    return FraCurveNode.of(template, quoteKey, spread, label);
  }

  private static CurveNode curveIborFuturesCurveNode(
      String conventionStr,
      String timeStr,
      String label,
      QuoteKey quoteKey,
      double spread) {

    Matcher matcher = FUT_TIME_REGEX.matcher(timeStr.toUpperCase(Locale.ENGLISH));
    if (!matcher.matches()) {
      throw new IllegalArgumentException(Messages.format("Invalid time format for Ibor Futures: {}", timeStr));
    }
    Period periodToStart = Period.parse("P" + matcher.group(1));
    int sequenceNumber = Integer.parseInt(matcher.group(2));
    IborFutureConvention convention = IborFutureConvention.of(conventionStr);
    IborFutureTemplate template = IborFutureTemplate.of(periodToStart, sequenceNumber, convention);
    return IborFutureCurveNode.of(template, quoteKey, spread, label);
  }

  //-------------------------------------------------------------------------
  private static CurveNode curveFixedOvernightCurveNode(
      String conventionStr,
      String timeStr,
      String label,
      QuoteKey quoteKey,
      double spread) {

    Matcher matcher = SIMPLE_TIME_REGEX.matcher(timeStr.toUpperCase(Locale.ENGLISH));
    if (!matcher.matches()) {
      throw new IllegalArgumentException(Messages.format("Invalid time format for Fixed-Overnight swap: {}", timeStr));
    }
    Period periodToEnd = Period.parse("P" + matcher.group(1));
    FixedOvernightSwapConvention convention = FixedOvernightSwapConvention.of(conventionStr);
    FixedOvernightSwapTemplate template = FixedOvernightSwapTemplate.of(Tenor.of(periodToEnd), convention);
    return FixedOvernightSwapCurveNode.of(template, quoteKey, spread, label);
  }

  private static CurveNode curveFixedIborCurveNode(
      String conventionStr,
      String timeStr,
      String label,
      QuoteKey quoteKey,
      double spread) {

    Matcher matcher = SIMPLE_TIME_REGEX.matcher(timeStr.toUpperCase(Locale.ENGLISH));
    if (!matcher.matches()) {
      throw new IllegalArgumentException(Messages.format("Invalid time format for Fixed-Ibor swap: {}", timeStr));
    }
    Period periodToEnd = Period.parse("P" + matcher.group(1));
    FixedIborSwapConvention convention = FixedIborSwapConvention.of(conventionStr);
    FixedIborSwapTemplate template = FixedIborSwapTemplate.of(Tenor.of(periodToEnd), convention);
    return FixedIborSwapCurveNode.of(template, quoteKey, spread, label);
  }

  private static CurveNode curveIborIborCurveNode(
      String conventionStr,
      String timeStr,
      String label,
      QuoteKey quoteKey,
      double spread) {

    Matcher matcher = SIMPLE_TIME_REGEX.matcher(timeStr.toUpperCase(Locale.ENGLISH));
    if (!matcher.matches()) {
      throw new IllegalArgumentException(Messages.format("Invalid time format for Ibor-Ibor swap: {}", timeStr));
    }
    Period periodToEnd = Period.parse("P" + matcher.group(1));
    IborIborSwapConvention convention = IborIborSwapConvention.of(conventionStr);
    IborIborSwapTemplate template = IborIborSwapTemplate.of(Tenor.of(periodToEnd), convention);
    return IborIborSwapCurveNode.of(template, quoteKey, spread, label);
  }

  private static CurveNode curveThreeLegBasisCurveNode(
      String conventionStr,
      String timeStr,
      String label,
      QuoteKey quoteKey,
      double spread) {

    Matcher matcher = SIMPLE_TIME_REGEX.matcher(timeStr.toUpperCase(Locale.ENGLISH));
    if (!matcher.matches()) {
      throw new IllegalArgumentException(Messages.format("Invalid time format for Three legs basis swap: {}", timeStr));
    }
    Period periodToEnd = Period.parse("P" + matcher.group(1));
    ThreeLegBasisSwapConvention convention = ThreeLegBasisSwapConvention.of(conventionStr);
    ThreeLegBasisSwapTemplate template = ThreeLegBasisSwapTemplate.of(Tenor.of(periodToEnd), convention);
    return ThreeLegBasisSwapCurveNode.of(template, quoteKey, spread, label);
  }

  private static CurveNode curveXCcyIborIborCurveNode(
      String conventionStr,
      String timeStr,
      String label,
      QuoteKey quoteKey,
      double spread) {

    Matcher matcher = SIMPLE_TIME_REGEX.matcher(timeStr.toUpperCase(Locale.ENGLISH));
    if (!matcher.matches()) {
      throw new IllegalArgumentException(Messages.format("Invalid time format for Cross Currency Swap: {}", timeStr));
    }
    Period periodToEnd = Period.parse("P" + matcher.group(1));
    XCcyIborIborSwapConvention convention = XCcyIborIborSwapConvention.of(conventionStr);
    XCcyIborIborSwapTemplate template = XCcyIborIborSwapTemplate.of(Tenor.of(periodToEnd), convention);
    return XCcyIborIborSwapCurveNode.of(template, quoteKey, spread, label);
  }

  //-------------------------------------------------------------------------
  private static CurveNode curveFxSwapCurveNode(
      String conventionStr,
      String timeStr,
      String label,
      QuoteKey quoteKey,
      double spread) {

    if (!DoubleMath.fuzzyEquals(spread, 0d, 1e-10d)) {
      throw new IllegalArgumentException("Additional spread must be zero for FX swaps");
    }
    Matcher matcher = SIMPLE_TIME_REGEX.matcher(timeStr.toUpperCase(Locale.ENGLISH));
    if (!matcher.matches()) {
      throw new IllegalArgumentException(Messages.format("Invalid time format for FX swap: {}", timeStr));
    }
    Period periodToEnd = Period.parse("P" + matcher.group(1));
    FxSwapConvention convention = FxSwapConvention.of(conventionStr);
    FxSwapTemplate template = FxSwapTemplate.of(periodToEnd, convention);
    return FxSwapCurveNode.of(template, quoteKey, label);
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private RatesCalibrationCsvLoader() {
  }

}
