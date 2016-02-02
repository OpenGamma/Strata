/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.calibration;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.ImmutableMarketData;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.SimpleMarketDataKey;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupEntry;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.NodalCurveDefinition;
import com.opengamma.strata.pricer.calibration.CalibrationMeasures;
import com.opengamma.strata.pricer.calibration.CurveCalibrator;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.MarketDataRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Synthetic curve calibrator.
 * <p>
 * A synthetic curve is a curve calibrated on synthetic instruments. A synthetic instrument is an instrument for which
 * a theoretical or synthetic quote can be computed from a {@link RatesProvider}.
 * <p>
 * This curve transformation is often use to have a different risk view or to standardize all risk to a common set
 * of instruments, even if they are not the most liquid in a market.
 */
public class SyntheticCurveCalibrator {

  /**
   * The default synthetic curve calibrator.
   * <p>
   * This uses the default StandardCurveCalibrator and the measures from {@link MarketQuoteMeasure}}.
   */
  static final SyntheticCurveCalibrator DEFAULT =
      SyntheticCurveCalibrator.of(
          CalibrationMeasures.of(
              MarketQuoteMeasure.FRA_MQ,
              MarketQuoteMeasure.IBOR_FIXING_DEPOSIT_MQ,
              MarketQuoteMeasure.IBOR_FUTURE_MQ,
              MarketQuoteMeasure.SWAP_MQ,
              MarketQuoteMeasure.TERM_DEPOSIT_MQ),
          CurveCalibrator.defaultCurveCalibrator());

  /** The market-quotes measures used to produce the synthetic quotes. */
  private final CalibrationMeasures marketQuotesMeasures;
  /** The curve calibrator. */
  private final CurveCalibrator calibrator;
  
  /**
   * Obtains an instance, specifying market quotes measures to use and calibrator.
   *
   * @param marketQuotesMeasures  the measures used to compute the market quotes
   * @param calibrator  the mechanism used to calibrate curves once the synthetic market quotes are known
   * @return the synthetic curve calibrator
   */
  public static SyntheticCurveCalibrator of(
      CalibrationMeasures marketQuotesMeasures,
      CurveCalibrator calibrator) {
    return new SyntheticCurveCalibrator(marketQuotesMeasures, calibrator);
  }
  
  /**
   * Returns the market quote measures.
   * 
   * @return  the measures
   */
  public CalibrationMeasures getMarketQuotesMeasures() {
    return marketQuotesMeasures;
  }

  /**
   * Returns the curve calibrator.
   * 
   * @return  the calibrator
   */
  public CurveCalibrator getCalibrator() {
    return calibrator;
  }



  //-------------------------------------------------------------------------
  // restricted constructor
  private SyntheticCurveCalibrator(
      CalibrationMeasures marketQuotesMeasures, 
      CurveCalibrator calibrator) {
    this.marketQuotesMeasures = marketQuotesMeasures;
    this.calibrator = calibrator;
  }

  /**
   * Calibrates synthetic curves from the configuration of the new curves and an existing {@link MarketData}.
   * 
   * @param group  the curve group definition for the synthetic curves and instruments
   * @param marketData  the market data
   * @return the rates provider
   */
  public ImmutableRatesProvider calibrate(
      CurveGroupDefinition group,
      MarketData marketData) {
    return calibrate(group, MarketDataRatesProvider.of(marketData));
  }

  /**
   * Calibrates synthetic curves from the configuration of the new curves and an existing RatesProvider.
   * 
   * @param group  the curve group definition for the synthetic curves and instruments
   * @param inputMulticurve  the input rates provider
   * @return the rates provider
   */
  public ImmutableRatesProvider calibrate(
      CurveGroupDefinition group,
      RatesProvider inputMulticurve) {
    LocalDate valuationDate = inputMulticurve.getValuationDate();
    // Computes the synthetic market quotes
    MarketData marketQuotesSy = marketData(inputMulticurve, group);
    // Retrieve the required time series if present in the original provider
    Set<Index> indicesRequired = new HashSet<Index>();
    for (CurveGroupEntry entry : group.getEntries()) {
      indicesRequired.addAll(entry.getIndices());
    }
    Map<Index, LocalDateDoubleTimeSeries> ts = new HashMap<>();
    for (Index i : indicesRequired) {
      LocalDateDoubleTimeSeries tsi = inputMulticurve.timeSeries(i);
      if (tsi != null) {
        ts.put(i, tsi);
      }
    }
    // Calibrate to the synthetic instrument with the synthetic quotes
    return calibrator.calibrate(group, valuationDate, marketQuotesSy, ts);
  }
  
  /**
   * Constructs the synthetic market data from an existing RatesProvider and the configuration of the new curves.
   * 
   * @param inputMulticurve  the input rates provider
   * @param group  the curve group definition for the synthetic curves and instruments
   * @return the market data
   */
  public MarketData marketData(
      RatesProvider inputMulticurve,
      CurveGroupDefinition group) {
    LocalDate valuationDate = inputMulticurve.getValuationDate();
    ImmutableList<NodalCurveDefinition> curveGroups = group.getCurveDefinitions();
    // Create fake market quotes of 0, only to be able to generate trades
    Map<MarketDataKey<?>, Double> mapKey0 = new HashMap<>();
    for (NodalCurveDefinition entry : curveGroups) {
      ImmutableList<CurveNode> nodes = entry.getNodes();
      for(int i=0; i<nodes.size(); i++) {
        for(SimpleMarketDataKey<?> key: nodes.get(i).requirements()) {
          mapKey0.put(key, 0.0d); 
        }
      }
    }
    ImmutableMarketData marketQuotes0 = ImmutableMarketData.of(valuationDate, mapKey0);    
    // Generate market quotes from the trades
    Map<MarketDataKey<?>, Double> mapKeySy = new HashMap<>();
    for (NodalCurveDefinition entry : curveGroups) {
      ImmutableList<CurveNode> nodes = entry.getNodes();
      for (CurveNode node : nodes) {
        Trade trade = node.trade(valuationDate, marketQuotes0);
        double mq = marketQuotesMeasures.value(trade, inputMulticurve);
        MarketDataKey<?> k = node.requirements().iterator().next();
        mapKeySy.put(k, mq);
      }
    }
    return ImmutableMarketData.of(valuationDate, mapKeySy);
  }

}
