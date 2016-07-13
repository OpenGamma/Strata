/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupEntry;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.NodalCurveDefinition;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.ResolvedTrade;

/**
 * Synthetic curve calibrator.
 * <p>
 * A synthetic curve is a curve calibrated on synthetic instruments.
 * A synthetic instrument is an instrument for which a theoretical or synthetic quote
 * can be computed from a {@link RatesProvider}.
 * <p>
 * This curve transformation is often used to have a different risk view or to standardize
 * all risk to a common set of instruments, even if they are not the most liquid in a market.
 */
public final class SyntheticCurveCalibrator {

  /**
   * The standard synthetic curve calibrator.
   * <p>
   * This uses the standard {@link CurveCalibrator} and {@link CalibrationMeasures}.
   */
  private static final SyntheticCurveCalibrator STANDARD = SyntheticCurveCalibrator.of(
      CurveCalibrator.standard(), CalibrationMeasures.MARKET_QUOTE);

  /**
   * The curve calibrator.
   */
  private final CurveCalibrator calibrator;
  /**
   * The market-quotes measures used to produce the synthetic quotes.
   */
  private final CalibrationMeasures measures;

  //-------------------------------------------------------------------------
  /**
   * The standard synthetic curve calibrator.
   * <p>
   * The {@link CalibrationMeasures#MARKET_QUOTE} measures are used for calibration.
   * The underlying calibrator is {@link CurveCalibrator#standard()}.
   *
   * @return the standard synthetic curve calibrator
   */
  public static SyntheticCurveCalibrator standard() {
    return STANDARD;
  }

  /**
   * Obtains an instance, specifying market quotes measures to use and calibrator.
   * 
   * @param calibrator  the mechanism used to calibrate curves once the synthetic market quotes are known
   * @param marketQuotesMeasures  the measures used to compute the market quotes
   * @return the synthetic curve calibrator
   */
  public static SyntheticCurveCalibrator of(CurveCalibrator calibrator, CalibrationMeasures marketQuotesMeasures) {
    return new SyntheticCurveCalibrator(calibrator, marketQuotesMeasures);
  }

  // restricted constructor
  private SyntheticCurveCalibrator(CurveCalibrator calibrator, CalibrationMeasures marketQuotesMeasures) {
    this.measures = marketQuotesMeasures;
    this.calibrator = calibrator;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the market quote measures.
   * 
   * @return the measures
   */
  public CalibrationMeasures getMeasures() {
    return measures;
  }

  /**
   * Gets the curve calibrator.
   * 
   * @return the calibrator
   */
  public CurveCalibrator getCalibrator() {
    return calibrator;
  }

  //-------------------------------------------------------------------------
  /**
   * Calibrates synthetic curves from the configuration of the new curves and an existing rates provider.
   * 
   * @param group  the curve group definition for the synthetic curves and instruments
   * @param inputProvider  the input rates provider
   * @param refData  the reference data, used to resolve the trades
   * @return the rates provider
   */
  public RatesProvider calibrate(
      CurveGroupDefinition group,
      RatesProvider inputProvider,
      ReferenceData refData) {

    // Computes the synthetic market quotes
    MarketData marketQuotesSy = marketData(group, inputProvider, refData);
    // Calibrate to the synthetic instrument with the synthetic quotes
    return calibrator.calibrate(group, marketQuotesSy, refData);
  }

  /**
   * Constructs the synthetic market data from an existing rates provider and the configuration of the new curves.
   * 
   * @param group  the curve group definition for the synthetic curves and instruments
   * @param inputProvider  the input rates provider
   * @param refData  the reference data, used to resolve the trades
   * @return the market data
   */
  MarketData marketData(
      CurveGroupDefinition group,
      RatesProvider inputProvider,
      ReferenceData refData) {

    // Retrieve the required time series if present in the original provider
    Set<Index> indicesRequired = new HashSet<Index>();
    for (CurveGroupEntry entry : group.getEntries()) {
      indicesRequired.addAll(entry.getIndices());
    }
    Map<IndexQuoteId, LocalDateDoubleTimeSeries> ts = new HashMap<>();
    for (Index idx : indicesRequired) {
      ts.put(IndexQuoteId.of(idx), inputProvider.timeSeries(idx));
    }

    LocalDate valuationDate = inputProvider.getValuationDate();
    ImmutableList<NodalCurveDefinition> curveGroups = group.getCurveDefinitions();
    // Create fake market quotes of 0, only to be able to generate trades
    Map<MarketDataId<?>, Double> mapId0 = new HashMap<>();
    for (NodalCurveDefinition entry : curveGroups) {
      ImmutableList<CurveNode> nodes = entry.getNodes();
      for (int i = 0; i < nodes.size(); i++) {
        for (MarketDataId<?> key : nodes.get(i).requirements()) {
          mapId0.put(key, 0.0d);
        }
      }
    }
    ImmutableMarketData marketQuotes0 = ImmutableMarketData.of(valuationDate, mapId0);
    // Generate market quotes from the trades
    Map<MarketDataId<?>, Double> mapIdSy = new HashMap<>();
    for (NodalCurveDefinition entry : curveGroups) {
      ImmutableList<CurveNode> nodes = entry.getNodes();
      for (CurveNode node : nodes) {
        ResolvedTrade trade = node.resolvedTrade(1d, marketQuotes0, refData);
        double mq = measures.value(trade, inputProvider);
        MarketDataId<?> k = node.requirements().iterator().next();
        mapIdSy.put(k, mq);
      }
    }
    return MarketData.of(valuationDate, mapIdSy, ts);
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return Messages.format("SyntheticCurveCalibrator[{}, {}]", calibrator, measures);
  }

}
