/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.basics.market.ObservableValues;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.MarketDataLookup;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.function.MarketDataFunction;
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
  public Result<CurveGroup> build(CurveGroupId id, MarketDataLookup marketData, MarketDataConfig marketDataConfig) {
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
  Result<CurveGroup> buildCurveGroup(
      CurveGroupDefinition groupDefn,
      MarketDataLookup marketData,
      MarketDataFeed feed) {

    // find and combine all the par rates
    CurveGroupName groupName = groupDefn.getName();
    Map<ObservableKey, Double> parRateValuesByKey = new HashMap<>();
    for (CurveGroupEntry curveEntry : groupDefn.getEntries()) {
      NodalCurveDefinition curveDefn = curveEntry.getCurveDefinition();
      Result<ParRates> parRatesResult = parRates(curveDefn, marketData, groupName, feed);
      if (!parRatesResult.isSuccess()) {
        return Result.failure(parRatesResult);
      }
      parRateValuesByKey.putAll(parRatesResult.getValue().toRatesByKey());
    }

    // perform the calibration
    ImmutableRatesProvider calibratedProvider = curveCalibrator.calibrate(
        groupDefn,
        marketData.getValuationDate(),
        ObservableValues.of(parRateValuesByKey),
        ImmutableMap.of(),
        FxMatrix.empty());

    // extract the result
    CurveGroup curveGroup = CurveGroup.of(
        groupDefn.getName(),
        calibratedProvider.getDiscountCurves(),
        calibratedProvider.getIndexCurves());
    return Result.success(curveGroup);
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
  private Result<ParRates> parRates(
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
      return Result.success(ParRates.builder().build());
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
