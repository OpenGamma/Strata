/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.FxRateId;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.market.curve.CurveDefinition;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.RatesCurveGroupDefinition;
import com.opengamma.strata.market.curve.RatesCurveGroupEntry;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
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
public final class SyntheticRatesCurveCalibrator {

  /**
   * The standard synthetic curve calibrator.
   * <p>
   * This uses the standard {@link RatesCurveCalibrator} and {@link CalibrationMeasures}.
   */
  private static final SyntheticRatesCurveCalibrator STANDARD = SyntheticRatesCurveCalibrator.of(
      RatesCurveCalibrator.standard(), CalibrationMeasures.MARKET_QUOTE);

  /**
   * The curve calibrator.
   */
  private final RatesCurveCalibrator calibrator;
  /**
   * The market-quotes measures used to produce the synthetic quotes.
   */
  private final CalibrationMeasures measures;

  //-------------------------------------------------------------------------
  /**
   * The standard synthetic curve calibrator.
   * <p>
   * The {@link CalibrationMeasures#MARKET_QUOTE} measures are used for calibration.
   * The underlying calibrator is {@link RatesCurveCalibrator#standard()}.
   *
   * @return the standard synthetic curve calibrator
   */
  public static SyntheticRatesCurveCalibrator standard() {
    return STANDARD;
  }

  /**
   * Obtains an instance, specifying market quotes measures to use and calibrator.
   * 
   * @param calibrator  the mechanism used to calibrate curves once the synthetic market quotes are known
   * @param marketQuotesMeasures  the measures used to compute the market quotes
   * @return the synthetic curve calibrator
   */
  public static SyntheticRatesCurveCalibrator of(RatesCurveCalibrator calibrator, CalibrationMeasures marketQuotesMeasures) {
    return new SyntheticRatesCurveCalibrator(calibrator, marketQuotesMeasures);
  }

  // restricted constructor
  private SyntheticRatesCurveCalibrator(RatesCurveCalibrator calibrator, CalibrationMeasures marketQuotesMeasures) {
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
  public RatesCurveCalibrator getCalibrator() {
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
  public ImmutableRatesProvider calibrate(
      RatesCurveGroupDefinition group,
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
  public ImmutableMarketData marketData(
      RatesCurveGroupDefinition group,
      RatesProvider inputProvider,
      ReferenceData refData) {

    // Retrieve the set of required indices and the list of required currencies
    Set<Index> indicesRequired = new HashSet<>();
    List<Currency> ccyRequired = new ArrayList<>();
    for (RatesCurveGroupEntry entry : group.getEntries()) {
      indicesRequired.addAll(entry.getIndices());
      ccyRequired.addAll(entry.getDiscountCurrencies());
    }
    // Retrieve the required time series if present in the original provider
    Map<IndexQuoteId, LocalDateDoubleTimeSeries> ts = new HashMap<>();
    for (Index idx : Sets.intersection(inputProvider.getTimeSeriesIndices(), indicesRequired)) {
      ts.put(IndexQuoteId.of(idx), inputProvider.timeSeries(idx));
    }
    LocalDate valuationDate = inputProvider.getValuationDate();
    ImmutableList<CurveDefinition> curveGroups = group.getCurveDefinitions();
    // Generate market quotes from the trades
    Map<MarketDataId<?>, Object> mapIdSy = new HashMap<>();
    // Generate quotes for FX pairs. The first currency is arbitrarily selected as starting point. 
    // The crosses are automatically generated by the MarketDataFxRateProvider used in calibration.
    for (int loopccy = 1; loopccy < ccyRequired.size(); loopccy++) {
      CurrencyPair ccyPair = CurrencyPair.of(ccyRequired.get(0), ccyRequired.get(loopccy));
      FxRateId fxId = FxRateId.of(ccyPair);
      mapIdSy.put(fxId, FxRate.of(ccyPair, inputProvider.fxRate(ccyPair)));
    }
    // create a synthetic value for each node
    for (CurveDefinition entry : curveGroups) {
      ImmutableList<CurveNode> nodes = entry.getNodes();
      for (CurveNode node : nodes) {
        ResolvedTrade trade = node.sampleResolvedTrade(valuationDate, inputProvider, refData);
        double mq = measures.value(trade, inputProvider);
        for (MarketDataId<?> key : node.requirements()) {
          if (key instanceof QuoteId) {
            mapIdSy.put(key, mq);
          }
        }
      }
    }
    return ImmutableMarketData.builder(valuationDate)
        .addValueMap(mapIdSy)
        .addTimeSeriesMap(ts).build();
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return Messages.format("SyntheticCurveCalibrator[{}, {}]", calibrator, measures);
  }

}
