/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static java.util.stream.Collectors.toMap;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.market.ImmutableObservableValues;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.basics.market.ObservableValues;
import com.opengamma.strata.calc.marketdata.MarketDataLookup;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.function.MarketDataFunction;
import com.opengamma.strata.calc.marketdata.scenario.MarketDataBox;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.ParRates;
import com.opengamma.strata.market.curve.definition.CurveGroupDefinition;
import com.opengamma.strata.market.curve.definition.CurveGroupEntry;
import com.opengamma.strata.market.curve.definition.NodalCurveDefinition;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.market.id.ParRatesId;
import com.opengamma.strata.pricer.calibration.CalibrationMeasures;
import com.opengamma.strata.pricer.calibration.CurveCalibrator;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * Market data function that builds a {@link CurveGroup}.
 */
public class CurveGroupMarketDataFunction implements MarketDataFunction<CurveGroup, CurveGroupId> {

  /** The analytics object that performs the curve calibration. */
  private final CurveCalibrator curveCalibrator;

  // TODO Where should the root finder config come from?
  //   Should it be possible to override it for each call? Put it in MarketDataConfig?
  //   Is it a system-wide setting?
  /**
   * Creates a new function for building curve groups that delegates to {@code curveBuilder} to perform calibration.
   *
   * @param rootFinderConfig  the configuration for the root finder used when calibrating curves
   * @param measures  the measures used for calibration
   */
  public CurveGroupMarketDataFunction(RootFinderConfig rootFinderConfig, CalibrationMeasures measures) {
    this.curveCalibrator = CurveCalibrator.of(
        rootFinderConfig.getAbsoluteTolerance(),
        rootFinderConfig.getRelativeTolerance(),
        rootFinderConfig.getMaximumSteps(),
        measures);
  }

  //-------------------------------------------------------------------------
  @Override
  public MarketDataRequirements requirements(CurveGroupId id, MarketDataConfig marketDataConfig) {
    Optional<CurveGroupDefinition> optionalGroup = marketDataConfig.get(CurveGroupDefinition.class, id.getName());

    if (!optionalGroup.isPresent()) {
      return MarketDataRequirements.empty();
    }
    CurveGroupDefinition groupDefn = optionalGroup.get();

    // request par rates for any curves that need market data
    // no par rates are requested if the curve definition contains all the market data needed to build the curve
    List<ParRatesId> parRatesIds = groupDefn.getEntries().stream()
        .filter(entry -> requiresMarketData(entry.getCurveDefinition()))
        .map(entry -> entry.getCurveDefinition().getName())
        .map(curveName -> ParRatesId.of(groupDefn.getName(), curveName, id.getMarketDataFeed()))
        .collect(toImmutableList());

    return MarketDataRequirements.builder().addValues(parRatesIds).build();
  }

  @Override
  public Result<MarketDataBox<CurveGroup>> build(
      CurveGroupId id,
      MarketDataLookup marketData,
      MarketDataConfig marketDataConfig) {

    CurveGroupName groupName = id.getName();
    Optional<CurveGroupDefinition> optionalGroup = marketDataConfig.get(CurveGroupDefinition.class, groupName);

    if (!optionalGroup.isPresent()) {
      return Result.failure(FailureReason.MISSING_DATA, "No configuration found for curve group '{}'", groupName);
    }
    CurveGroupDefinition groupDefn = optionalGroup.get();
    return buildCurveGroup(groupDefn, marketData, id.getMarketDataFeed());
  }

  @Override
  public Class<CurveGroupId> getMarketDataIdType() {
    return CurveGroupId.class;
  }

  //-------------------------------------------------------------------------
  /**
   * Builds a curve group given the configuration for the group and a set of market data.
   *
   * @param groupDefn  the definition of the curve group
   * @param marketData  the market data containing any values required to build the curve group
   * @param feed  the market data feed that is the source of the observable data
   * @return a result containing the curve group or details of why it couldn't be built
   */
  Result<MarketDataBox<CurveGroup>> buildCurveGroup(
      CurveGroupDefinition groupDefn,
      MarketDataLookup marketData,
      MarketDataFeed feed) {

    // find and combine all the par rates
    CurveGroupName groupName = groupDefn.getName();

    List<Result<MarketDataBox<ParRates>>> parRatesResults = groupDefn.getEntries().stream()
        .map(CurveGroupEntry::getCurveDefinition)
        .map(curveDefn -> parRates(curveDefn, marketData, groupName, feed))
        .collect(toImmutableList());

    if (Result.anyFailures(parRatesResults)) {
      return Result.failure(parRatesResults);
    }
    List<MarketDataBox<ParRates>> parRateBoxes = parRatesResults.stream().map(Result::getValue).collect(toImmutableList());
    // If any of the rates have values for multiple scenarios then we need to build a curve group for each scenario.
    // If all rates contain a single value then we only need to build a single curve group.
    boolean multipleValues = parRateBoxes.stream().anyMatch(MarketDataBox::isScenarioValue);

    return multipleValues ?
        buildMultipleCurveGroups(groupDefn, marketData.getValuationDate(), parRateBoxes) :
        buildSingleCurveGroup(groupDefn, marketData.getValuationDate(), parRateBoxes);
  }

  private Result<MarketDataBox<CurveGroup>> buildMultipleCurveGroups(
      CurveGroupDefinition groupDefn,
      MarketDataBox<LocalDate> valuationDateBox,
      List<MarketDataBox<ParRates>> parRateBoxes) {

    Result<Integer> scenarioCountResult = scenarioCount(valuationDateBox, parRateBoxes);

    if (scenarioCountResult.isFailure()) {
      return Result.failure(scenarioCountResult);
    }
    int scenarioCount = scenarioCountResult.getValue();
    ImmutableList.Builder<Result<CurveGroup>> builder = ImmutableList.builder();

    for (int i = 0; i < scenarioCount; i++) {
      List<ParRates> parRatesList = parRatesForScenario(parRateBoxes, i);
      ObservableValues ratesByKey = ratesByKey(parRatesList);
      LocalDate valuationDate = valuationDateBox.getValue(scenarioCount);
      builder.add(buildGroup(groupDefn, valuationDate, ratesByKey));
    }
    ImmutableList<Result<CurveGroup>> results = builder.build();

    if (Result.anyFailures(results)) {
      return Result.failure(results);
    }
    List<CurveGroup> curveGroups = results.stream()
        .map(Result::getValue)
        .collect(toImmutableList());

    return Result.success(MarketDataBox.ofScenarioValues(curveGroups));
  }

  private static List<ParRates> parRatesForScenario(List<MarketDataBox<ParRates>> boxes, int scenarioIndex) {
    return boxes.stream()
        .map(box -> box.getValue(scenarioIndex))
        .collect(toImmutableList());
  }

  private Result<MarketDataBox<CurveGroup>> buildSingleCurveGroup(
      CurveGroupDefinition groupDefn,
      MarketDataBox<LocalDate> valuationDate,
      List<MarketDataBox<ParRates>> parRateBoxes) {

    List<ParRates> parRates = parRateBoxes.stream().map(MarketDataBox::getSingleValue).collect(toImmutableList());
    ObservableValues parRateValuesByKey = ratesByKey(parRates);
    Result<CurveGroup> result = buildGroup(groupDefn, valuationDate.getSingleValue(), parRateValuesByKey);

    return result.isFailure() ?
        Result.failure(result) :
        Result.success(MarketDataBox.ofSingleValue(result.getValue()));
  }

  /**
   * Extracts the underlying quotes from the {@link ParRates} instances and returns them in a map.
   *
   * @param parRates  par rates objects
   * @return the underlying quotes from the par rates
   */
  private static ObservableValues ratesByKey(List<ParRates> parRates) {
    Map<ObservableKey, Double> valueMap = parRates.stream()
        .flatMap(pr -> pr.toRatesByKey().entrySet().stream())
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    return ImmutableObservableValues.of(valueMap);
  }

  private Result<CurveGroup> buildGroup(
      CurveGroupDefinition groupDefn,
      LocalDate valuationDate,
      ObservableValues parRateValuesByKey) {

    // perform the calibration
    ImmutableRatesProvider calibratedProvider = curveCalibrator.calibrate(
        groupDefn,
        valuationDate,
        parRateValuesByKey,
        ImmutableMap.of(),
        FxMatrix.empty());

    // extract the result
    CurveGroup curveGroup = CurveGroup.of(
        groupDefn.getName(),
        calibratedProvider.getDiscountCurves(),
        calibratedProvider.getIndexCurves());

    return Result.success(curveGroup);
  }

  private static Result<Integer> scenarioCount(
      MarketDataBox<LocalDate> valuationDate,
      List<MarketDataBox<ParRates>> parRateBoxes) {

    Integer scenarioCount = null;

    if (valuationDate.isScenarioValue()) {
      scenarioCount = valuationDate.getScenarioCount();
    }
    for (MarketDataBox<ParRates> box : parRateBoxes) {
      if (box.isScenarioValue()) {
        int boxScenarioCount = box.getScenarioCount();

        if (scenarioCount == null) {
          scenarioCount = boxScenarioCount;
        } else {
          if (scenarioCount != boxScenarioCount) {
            return Result.failure(
                FailureReason.INVALID_INPUT,
                "There are {} scenarios for par rates {} which does not match the previous scenario count {}",
                boxScenarioCount,
                box,
                scenarioCount);
          }
        }
      }
    }
    if (scenarioCount != null) {
      return Result.success(scenarioCount);
    }
    // This shouldn't happen, this method is only called after checking at least one of the values contains data
    // for multiple scenarios.
    return Result.failure(FailureReason.INVALID_INPUT, "Cannot count the scenarios, all data contained single values");
  }

  /**
   * Returns the par rates required for the curve if available.
   * <p>
   * If no market data is required to build the curve an empty set of par rates is returned.
   * If the curve requires par rates which are available in {@code marketData} they are returned.
   * If the curve requires par rates which are not available in {@code marketData} a failure is returned.
   *
   * @param curveDefn  the curve definition
   * @param marketData  the market data
   * @param groupName  the name of the curve group being built
   * @param feed  the market data feed that is the source of the underlying market data
   * @return the par rates required for the curve if available.
   */
  private Result<MarketDataBox<ParRates>> parRates(
      NodalCurveDefinition curveDefn,
      MarketDataLookup marketData,
      CurveGroupName groupName,
      MarketDataFeed feed) {

    // only try to get par rates from the market data if the curve needs market data
    if (requiresMarketData(curveDefn)) {
      ParRatesId parRatesId = ParRatesId.of(groupName, curveDefn.getName(), feed);
      if (!marketData.containsValue(parRatesId)) {
        return Result.failure(FailureReason.MISSING_DATA, "No par rates for {}", parRatesId);
      }
      return Result.success(marketData.getValue(parRatesId));
    } else {
      return Result.success(MarketDataBox.ofSingleValue(ParRates.builder().build()));
    }
  }

  /**
   * Checks if the curve configuration requires market data.
   * <p>
   * If the curve configuration contains all the data required to build the curve it is not necessary to
   * request par rates for the curve points. However if market data is required for any point on the
   * curve this function must add {@link ParRates} to its market data requirements.
   *
   * @param curveDefn  the curve definition
   * @return true if the curve requires market data for calibration
   */
  private boolean requiresMarketData(NodalCurveDefinition curveDefn) {
    return curveDefn.getNodes().stream().anyMatch(node -> !node.requirements().isEmpty());
  }
}
