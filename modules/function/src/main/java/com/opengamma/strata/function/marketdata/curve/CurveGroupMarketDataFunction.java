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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.function.MarketDataFunction;
import com.opengamma.strata.calc.marketdata.scenario.MarketDataBox;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupEntry;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveInputs;
import com.opengamma.strata.market.curve.NodalCurveDefinition;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.market.id.CurveInputsId;
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
    CurveGroupDefinition groupDefn = marketDataConfig.get(CurveGroupDefinition.class, id.getName());

    // request input data for any curves that need market data
    // no input data is requested if the curve definition contains all the market data needed to build the curve
    List<CurveInputsId> curveInputsIds = groupDefn.getEntries().stream()
        .filter(entry -> requiresMarketData(entry.getCurveDefinition()))
        .map(entry -> entry.getCurveDefinition().getName())
        .map(curveName -> CurveInputsId.of(groupDefn.getName(), curveName, id.getMarketDataFeed()))
        .collect(toImmutableList());

    return MarketDataRequirements.builder().addValues(curveInputsIds).build();
  }

  @Override
  public MarketDataBox<CurveGroup> build(
      CurveGroupId id,
      CalculationEnvironment marketData,
      MarketDataConfig marketDataConfig) {

    CurveGroupName groupName = id.getName();
    CurveGroupDefinition groupDefn = marketDataConfig.get(CurveGroupDefinition.class, groupName);
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
  MarketDataBox<CurveGroup> buildCurveGroup(
      CurveGroupDefinition groupDefn,
      CalculationEnvironment marketData,
      MarketDataFeed feed) {

    // find and combine all the input data
    CurveGroupName groupName = groupDefn.getName();

    List<MarketDataBox<CurveInputs>> inputBoxes = groupDefn.getEntries().stream()
        .map(CurveGroupEntry::getCurveDefinition)
        .map(curveDefn -> curveInputs(curveDefn, marketData, groupName, feed))
        .collect(toImmutableList());
    // If any of the inputs have values for multiple scenarios then we need to build a curve group for each scenario.
    // If all inputs contain a single value then we only need to build a single curve group.
    boolean multipleValues = inputBoxes.stream().anyMatch(MarketDataBox::isScenarioValue);

    return multipleValues ?
        buildMultipleCurveGroups(groupDefn, marketData.getValuationDate(), inputBoxes) :
        buildSingleCurveGroup(groupDefn, marketData.getValuationDate(), inputBoxes);
  }

  private MarketDataBox<CurveGroup> buildMultipleCurveGroups(
      CurveGroupDefinition groupDefn,
      MarketDataBox<LocalDate> valuationDateBox,
      List<MarketDataBox<CurveInputs>> inputBoxes) {

    int scenarioCount = scenarioCount(valuationDateBox, inputBoxes);
    ImmutableList.Builder<CurveGroup> builder = ImmutableList.builder();

    for (int i = 0; i < scenarioCount; i++) {
      List<CurveInputs> curveInputsList = inputsForScenario(inputBoxes, i);
      MarketData inputs = inputsByKey(curveInputsList);
      LocalDate valuationDate = valuationDateBox.getValue(scenarioCount);
      builder.add(buildGroup(groupDefn, valuationDate, inputs));
    }
    ImmutableList<CurveGroup> curveGroups = builder.build();
    return MarketDataBox.ofScenarioValues(curveGroups);
  }

  private static List<CurveInputs> inputsForScenario(List<MarketDataBox<CurveInputs>> boxes, int scenarioIndex) {
    return boxes.stream()
        .map(box -> box.getValue(scenarioIndex))
        .collect(toImmutableList());
  }

  private MarketDataBox<CurveGroup> buildSingleCurveGroup(
      CurveGroupDefinition groupDefn,
      MarketDataBox<LocalDate> valuationDate,
      List<MarketDataBox<CurveInputs>> inputBoxes) {

    List<CurveInputs> inputs = inputBoxes.stream().map(MarketDataBox::getSingleValue).collect(toImmutableList());
    MarketData inputValues = inputsByKey(inputs);
    CurveGroup curveGroup = buildGroup(groupDefn, valuationDate.getSingleValue(), inputValues);
    return MarketDataBox.ofSingleValue(curveGroup);
  }

  /**
   * Extracts the underlying quotes from the {@link CurveInputs} instances and returns them in a map.
   *
   * @param inputs  input data for the curve
   * @return the underlying quotes from the input data
   */
  private static MarketData inputsByKey(List<CurveInputs> inputs) {
    Map<? extends MarketDataKey<?>, ?> valueMap = inputs.stream()
        .flatMap(pr -> pr.getMarketData().entrySet().stream())
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    return MarketData.of(valueMap);
  }

  private CurveGroup buildGroup(
      CurveGroupDefinition groupDefn,
      LocalDate valuationDate,
      MarketData marketData) {

    // perform the calibration
    ImmutableRatesProvider calibratedProvider = curveCalibrator.calibrate(
        groupDefn,
        valuationDate,
        marketData,
        ImmutableMap.of());

    return CurveGroup.of(
        groupDefn.getName(),
        calibratedProvider.getDiscountCurves(),
        calibratedProvider.getIndexCurves());
  }

  private static int scenarioCount(
      MarketDataBox<LocalDate> valuationDate,
      List<MarketDataBox<CurveInputs>> curveInputBoxes) {

    int scenarioCount = 0;

    if (valuationDate.isScenarioValue()) {
      scenarioCount = valuationDate.getScenarioCount();
    }
    for (MarketDataBox<CurveInputs> box : curveInputBoxes) {
      if (box.isScenarioValue()) {
        int boxScenarioCount = box.getScenarioCount();

        if (scenarioCount == 0) {
          scenarioCount = boxScenarioCount;
        } else {
          if (scenarioCount != boxScenarioCount) {
            throw new IllegalArgumentException(
                Messages.format(
                    "All boxes must have the same number of scenarios, current count = {}, box {} has {}",
                    scenarioCount,
                    box,
                    box.getScenarioCount()));
          }
        }
      }
    }
    if (scenarioCount != 0) {
      return scenarioCount;
    }
    // This shouldn't happen, this method is only called after checking at least one of the values contains data
    // for multiple scenarios.
    throw new IllegalArgumentException("Cannot count the scenarios, all data contained single values");
  }

  /**
   * Returns the inputs required for the curve if available.
   * <p>
   * If no market data is required to build the curve an empty set of inputs is returned.
   * If the curve requires inputs which are available in {@code marketData} they are returned.
   * If the curve requires inputs which are not available in {@code marketData} an exception is thrown
   *
   * @param curveDefn  the curve definition
   * @param marketData  the market data
   * @param groupName  the name of the curve group being built
   * @param feed  the market data feed that is the source of the underlying market data
   * @return the input data required for the curve if available
   */
  private MarketDataBox<CurveInputs> curveInputs(
      NodalCurveDefinition curveDefn,
      CalculationEnvironment marketData,
      CurveGroupName groupName,
      MarketDataFeed feed) {

    // only try to get inputs from the market data if the curve needs market data
    if (requiresMarketData(curveDefn)) {
      CurveInputsId curveInputsId = CurveInputsId.of(groupName, curveDefn.getName(), feed);
      return marketData.getValue(curveInputsId);
    } else {
      return MarketDataBox.ofSingleValue(CurveInputs.builder().build());
    }
  }

  /**
   * Checks if the curve configuration requires market data.
   * <p>
   * If the curve configuration contains all the data required to build the curve it is not necessary to
   * request input data for the curve points. However if market data is required for any point on the
   * curve this function must add {@link CurveInputs} to its market data requirements.
   *
   * @param curveDefn  the curve definition
   * @return true if the curve requires market data for calibration
   */
  private boolean requiresMarketData(NodalCurveDefinition curveDefn) {
    return curveDefn.getNodes().stream().anyMatch(node -> !node.requirements().isEmpty());
  }
}
