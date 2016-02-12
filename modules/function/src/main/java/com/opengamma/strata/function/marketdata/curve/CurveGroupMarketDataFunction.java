/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.market.ImmutableMarketData;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.function.MarketDataFunction;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveInputs;
import com.opengamma.strata.market.curve.NodalCurveDefinition;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.market.id.CurveInputsId;
import com.opengamma.strata.pricer.calibration.CalibrationMeasures;
import com.opengamma.strata.pricer.calibration.CurveCalibrator;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * Market data function that builds a curve group.
 * <p>
 * This function calibrates curves, turning a {@link CurveGroupDefinition} into a {@link CurveGroup}.
 */
public class CurveGroupMarketDataFunction implements MarketDataFunction<CurveGroup, CurveGroupId> {

  /**
   * The default analytics object that performs the curve calibration.
   */
  private final CalibrationMeasures calibrationMeasures;

  //-------------------------------------------------------------------------
  /**
   * Creates a new function for building curve groups using the standard measures.
   * <p>
   * This will use the standard {@linkplain CalibrationMeasures#PAR_SPREAD par spread} measures
   * for calibration. The {@link MarketDataConfig} may contain a {@link RootFinderConfig}
   * to define the tolerances.
   */
  public CurveGroupMarketDataFunction() {
    this(CalibrationMeasures.PAR_SPREAD);
  }

  /**
   * Creates a new function for building curve groups.
   * <p>
   * The default calibrator is specified. The {@link MarketDataConfig} may contain a
   * {@link RootFinderConfig} that alters the tolerances used in calibration.
   *
   * @param calibrationMeasures  the calibration measures to be used in the calibrator
   */
  public CurveGroupMarketDataFunction(CalibrationMeasures calibrationMeasures) {
    this.calibrationMeasures = ArgChecker.notNull(calibrationMeasures, "calibrationMeasures");
  }

  //-------------------------------------------------------------------------
  @Override
  public MarketDataRequirements requirements(CurveGroupId id, MarketDataConfig marketDataConfig) {
    CurveGroupDefinition groupDefn = marketDataConfig.get(CurveGroupDefinition.class, id.getName());

    // request input data for any curves that need market data
    // no input data is requested if the curve definition contains all the market data needed to build the curve
    List<CurveInputsId> curveInputsIds = groupDefn.getCurveDefinitions().stream()
        .filter(defn -> requiresMarketData(defn))
        .map(defn -> defn.getName())
        .map(curveName -> CurveInputsId.of(groupDefn.getName(), curveName, id.getMarketDataFeed()))
        .collect(toImmutableList());

    return MarketDataRequirements.builder().addValues(curveInputsIds).build();
  }

  @Override
  public MarketDataBox<CurveGroup> build(
      CurveGroupId id,
      MarketDataConfig marketDataConfig,
      CalculationEnvironment marketData,
      ReferenceData refData) {

    // create the calibrator, using the configured RootFinderConfig if found
    RootFinderConfig rfc = marketDataConfig.find(RootFinderConfig.class).orElse(RootFinderConfig.standard());
    CurveCalibrator calibrator = CurveCalibrator.of(
        rfc.getAbsoluteTolerance(), rfc.getRelativeTolerance(), rfc.getMaximumSteps(), calibrationMeasures);

    // calibrate
    CurveGroupName groupName = id.getName();
    CurveGroupDefinition groupDefn = marketDataConfig.get(CurveGroupDefinition.class, groupName);
    return buildCurveGroup(groupDefn, calibrator, marketData, refData, id.getMarketDataFeed());
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
   * @param calibrator  the calibrator
   * @param marketData  the market data containing any values required to build the curve group
   * @param refData  the reference data, used for resolving trades
   * @param feed  the market data feed that is the source of the observable data
   * @return a result containing the curve group or details of why it couldn't be built
   */
  MarketDataBox<CurveGroup> buildCurveGroup(
      CurveGroupDefinition groupDefn,
      CurveCalibrator calibrator,
      CalculationEnvironment marketData,
      ReferenceData refData,
      MarketDataFeed feed) {

    // find and combine all the input data
    CurveGroupName groupName = groupDefn.getName();

    List<MarketDataBox<CurveInputs>> inputBoxes = groupDefn.getCurveDefinitions().stream()
        .map(curveDefn -> curveInputs(curveDefn, marketData, groupName, feed))
        .collect(toImmutableList());
    // If any of the inputs have values for multiple scenarios then we need to build a curve group for each scenario.
    // If all inputs contain a single value then we only need to build a single curve group.
    boolean multipleValues = inputBoxes.stream().anyMatch(MarketDataBox::isScenarioValue);

    return multipleValues ?
        buildMultipleCurveGroups(groupDefn, calibrator, marketData.getValuationDate(), inputBoxes, refData) :
        buildSingleCurveGroup(groupDefn, calibrator, marketData.getValuationDate(), inputBoxes, refData);
  }

  // calibrates when there are multiple groups
  private MarketDataBox<CurveGroup> buildMultipleCurveGroups(
      CurveGroupDefinition groupDefn,
      CurveCalibrator calibrator,
      MarketDataBox<LocalDate> valuationDateBox,
      List<MarketDataBox<CurveInputs>> inputBoxes,
      ReferenceData refData) {

    int scenarioCount = scenarioCount(valuationDateBox, inputBoxes);
    ImmutableList.Builder<CurveGroup> builder = ImmutableList.builder();

    for (int i = 0; i < scenarioCount; i++) {
      List<CurveInputs> curveInputsList = inputsForScenario(inputBoxes, i);
      LocalDate valuationDate = valuationDateBox.getValue(scenarioCount);
      MarketData inputs = inputsByKey(valuationDate, curveInputsList);
      builder.add(buildGroup(groupDefn, calibrator, valuationDate, inputs, refData));
    }
    ImmutableList<CurveGroup> curveGroups = builder.build();
    return MarketDataBox.ofScenarioValues(curveGroups);
  }

  private static List<CurveInputs> inputsForScenario(List<MarketDataBox<CurveInputs>> boxes, int scenarioIndex) {
    return boxes.stream()
        .map(box -> box.getValue(scenarioIndex))
        .collect(toImmutableList());
  }

  // calibrates when there is a single group
  private MarketDataBox<CurveGroup> buildSingleCurveGroup(
      CurveGroupDefinition groupDefn,
      CurveCalibrator calibrator,
      MarketDataBox<LocalDate> valuationDateBox,
      List<MarketDataBox<CurveInputs>> inputBoxes,
      ReferenceData refData) {

    List<CurveInputs> inputs = inputBoxes.stream().map(MarketDataBox::getSingleValue).collect(toImmutableList());
    LocalDate valuationDate = valuationDateBox.getValue(0);
    MarketData inputValues = inputsByKey(valuationDate, inputs);
    CurveGroup curveGroup = buildGroup(groupDefn, calibrator, valuationDateBox.getSingleValue(), inputValues, refData);
    return MarketDataBox.ofSingleValue(curveGroup);
  }

  /**
   * Extracts the underlying quotes from the {@link CurveInputs} instances and returns them in a map.
   *
   * @param valuationDate  the valuation date
   * @param inputs  input data for the curve
   * @return the underlying quotes from the input data
   */
  private static MarketData inputsByKey(LocalDate valuationDate, List<CurveInputs> inputs) {
    Map<MarketDataKey<?>, Object> marketDataMap = new HashMap<>();

    for (CurveInputs input : inputs) {
      Map<? extends MarketDataKey<?>, ?> inputMarketData = input.getMarketData();

      for (Map.Entry<? extends MarketDataKey<?>, ?> entry : inputMarketData.entrySet()) {
        Object existingValue = marketDataMap.get(entry.getKey());

        // If the same key is used by multiple different curves the corresponding market data value must be equal
        if (existingValue == null) {
          marketDataMap.put(entry.getKey(), entry.getValue());
        } else if (!existingValue.equals(entry.getValue())) {
          throw new IllegalArgumentException(
              Messages.format(
                  "Multiple unequal values found for key {}. Values: {} and {}",
                  entry.getKey(),
                  existingValue,
                  entry.getValue()));
        }
      }
    }
    return ImmutableMarketData.of(valuationDate, marketDataMap);
  }

  private CurveGroup buildGroup(
      CurveGroupDefinition groupDefn,
      CurveCalibrator calibrator,
      LocalDate valuationDate,
      MarketData marketData,
      ReferenceData refData) {

    // perform the calibration
    ImmutableRatesProvider calibratedProvider = calibrator.calibrate(
        groupDefn,
        valuationDate,
        marketData,
        refData,
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
