/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.curve;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.calc.marketdata.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.MarketDataFunction;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.CurveDefinition;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupId;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveInputs;
import com.opengamma.strata.market.curve.CurveInputsId;
import com.opengamma.strata.pricer.curve.CalibrationMeasures;
import com.opengamma.strata.pricer.curve.CurveCalibrator;
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
    CurveGroupDefinition groupDefn = marketDataConfig.get(CurveGroupDefinition.class, id.getCurveGroupName());

    // request input data for any curves that need market data
    // no input data is requested if the curve definition contains all the market data needed to build the curve
    List<CurveInputsId> curveInputsIds = groupDefn.getCurveDefinitions().stream()
        .filter(defn -> requiresMarketData(defn))
        .map(defn -> defn.getName())
        .map(curveName -> CurveInputsId.of(groupDefn.getName(), curveName, id.getObservableSource()))
        .collect(toImmutableList());

    return MarketDataRequirements.builder().addValues(curveInputsIds).build();
  }

  @Override
  public MarketDataBox<CurveGroup> build(
      CurveGroupId id,
      MarketDataConfig marketDataConfig,
      ScenarioMarketData marketData,
      ReferenceData refData) {

    // create the calibrator, using the configured RootFinderConfig if found
    RootFinderConfig rfc = marketDataConfig.find(RootFinderConfig.class).orElse(RootFinderConfig.standard());
    CurveCalibrator calibrator = CurveCalibrator.of(
        rfc.getAbsoluteTolerance(), rfc.getRelativeTolerance(), rfc.getMaximumSteps(), calibrationMeasures);

    // calibrate
    CurveGroupName groupName = id.getCurveGroupName();
    CurveGroupDefinition configuredDefn = marketDataConfig.get(CurveGroupDefinition.class, groupName);
    return buildCurveGroup(configuredDefn, calibrator, marketData, refData, id.getObservableSource());
  }

  @Override
  public Class<CurveGroupId> getMarketDataIdType() {
    return CurveGroupId.class;
  }

  //-------------------------------------------------------------------------
  /**
   * Builds a curve group given the configuration for the group and a set of market data.
   *
   * @param configuredGroup  the definition of the curve group
   * @param calibrator  the calibrator
   * @param marketData  the market data containing any values required to build the curve group
   * @param refData  the reference data, used for resolving trades
   * @param obsSource  the source of observable market data
   * @return a result containing the curve group or details of why it couldn't be built
   */
  MarketDataBox<CurveGroup> buildCurveGroup(
      CurveGroupDefinition configuredGroup,
      CurveCalibrator calibrator,
      ScenarioMarketData marketData,
      ReferenceData refData,
      ObservableSource obsSource) {

    // find and combine all the input data
    CurveGroupName groupName = configuredGroup.getName();

    List<MarketDataBox<CurveInputs>> inputBoxes = configuredGroup.getCurveDefinitions().stream()
        .map(curveDefn -> curveInputs(curveDefn, marketData, groupName, obsSource))
        .collect(toImmutableList());
    MarketDataBox<LocalDate> valuationDates = marketData.getValuationDate();
    // If any of the inputs have values for multiple scenarios then we need to build a curve group for each scenario.
    // If all inputs contain a single value then we only need to build a single curve group.
    boolean multipleValuationDates = valuationDates.isScenarioValue();
    boolean multipleValues = inputBoxes.stream().anyMatch(MarketDataBox::isScenarioValue);
    Map<ObservableId, LocalDateDoubleTimeSeries> fixings = extractFixings(marketData);

    return multipleValues || multipleValuationDates ?
        buildMultipleCurveGroups(configuredGroup, calibrator, valuationDates, inputBoxes, fixings, refData) :
        buildSingleCurveGroup(configuredGroup, calibrator, valuationDates.getSingleValue(), inputBoxes, fixings, refData);
  }

  // extract the fixings from the input data
  private Map<ObservableId, LocalDateDoubleTimeSeries> extractFixings(ScenarioMarketData marketData) {
    Map<ObservableId, LocalDateDoubleTimeSeries> fixings = new HashMap<>();
    for (ObservableId id : marketData.getTimeSeriesIds()) {
      fixings.put(id, marketData.getTimeSeries(id));
    }
    return fixings;
  }

  // calibrates when there are multiple groups
  private MarketDataBox<CurveGroup> buildMultipleCurveGroups(
      CurveGroupDefinition configuredGroup,
      CurveCalibrator calibrator,
      MarketDataBox<LocalDate> valuationDateBox,
      List<MarketDataBox<CurveInputs>> inputBoxes,
      Map<ObservableId, LocalDateDoubleTimeSeries> fixings,
      ReferenceData refData) {

    int scenarioCount = scenarioCount(valuationDateBox, inputBoxes);
    ImmutableList.Builder<CurveGroup> builder = ImmutableList.builder();

    for (int i = 0; i < scenarioCount; i++) {
      LocalDate valuationDate = valuationDateBox.getValue(scenarioCount);
      CurveGroupDefinition filteredGroup = configuredGroup.filtered(valuationDate, refData);
      List<CurveInputs> curveInputsList = inputsForScenario(inputBoxes, i);
      MarketData inputs = inputsByKey(valuationDate, curveInputsList, fixings);
      builder.add(buildGroup(filteredGroup, calibrator, inputs, refData));
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
      CurveGroupDefinition configuredGroup,
      CurveCalibrator calibrator,
      LocalDate valuationDate,
      List<MarketDataBox<CurveInputs>> inputBoxes,
      Map<ObservableId, LocalDateDoubleTimeSeries> fixings,
      ReferenceData refData) {

    CurveGroupDefinition filteredGroup = configuredGroup.filtered(valuationDate, refData);
    List<CurveInputs> inputs = inputBoxes.stream().map(MarketDataBox::getSingleValue).collect(toImmutableList());
    MarketData inputValues = inputsByKey(valuationDate, inputs, fixings);
    CurveGroup curveGroup = buildGroup(filteredGroup, calibrator, inputValues, refData);
    return MarketDataBox.ofSingleValue(curveGroup);
  }

  /**
   * Extracts the underlying quotes from the {@link CurveInputs} instances and returns them in a map.
   *
   * @param valuationDate  the valuation date
   * @param inputs  input data for the curve
   * @param fixings  the fixings
   * @return the underlying quotes from the input data
   */
  private static MarketData inputsByKey(
      LocalDate valuationDate,
      List<CurveInputs> inputs,
      Map<ObservableId, LocalDateDoubleTimeSeries> fixings) {

    Map<MarketDataId<?>, Object> marketDataMap = new HashMap<>();

    for (CurveInputs input : inputs) {
      Map<? extends MarketDataId<?>, ?> inputMarketData = input.getMarketData();

      for (Map.Entry<? extends MarketDataId<?>, ?> entry : inputMarketData.entrySet()) {
        Object existingValue = marketDataMap.get(entry.getKey());

        // If the same identifier is used by multiple different curves the corresponding market data value must be equal
        if (existingValue == null) {
          marketDataMap.put(entry.getKey(), entry.getValue());
        } else if (!existingValue.equals(entry.getValue())) {
          throw new IllegalArgumentException(
              Messages.format(
                  "Multiple unequal values found for identifier {}. Values: {} and {}",
                  entry.getKey(),
                  existingValue,
                  entry.getValue()));
        }
      }
    }
    return ImmutableMarketData.builder(valuationDate).values(marketDataMap).timeSeries(fixings).build();
  }

  private CurveGroup buildGroup(
      CurveGroupDefinition groupDefn,
      CurveCalibrator calibrator,
      MarketData marketData,
      ReferenceData refData) {

    // perform the calibration
    ImmutableRatesProvider calibratedProvider = calibrator.calibrate(
        groupDefn,
        marketData,
        refData);

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
   * @param obsSource  the source of the observable market data
   * @return the input data required for the curve if available
   */
  private MarketDataBox<CurveInputs> curveInputs(
      CurveDefinition curveDefn,
      ScenarioMarketData marketData,
      CurveGroupName groupName,
      ObservableSource obsSource) {

    // only try to get inputs from the market data if the curve needs market data
    if (requiresMarketData(curveDefn)) {
      CurveInputsId curveInputsId = CurveInputsId.of(groupName, curveDefn.getName(), obsSource);
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
  private boolean requiresMarketData(CurveDefinition curveDefn) {
    return curveDefn.getNodes().stream().anyMatch(node -> !node.requirements().isEmpty());
  }
}
