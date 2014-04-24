/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.pnl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.LocalDate;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.FXMatrixFn;
import com.opengamma.sesame.marketdata.HistoricalMarketDataFn;
import com.opengamma.timeseries.date.localdate.ImmutableLocalDateDoubleTimeSeries;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.LocalDateRange;

/**
 * Converts historical PnL numbers to use today's fx rate. Performs the
 * calculation TodayPnL(T) = PnL(T) * FX(T) / FX(today). PnLPeriodBound
 * parameter defines whether FX(T-1) or FX(T), i.e. start or end, respectively,
 * is used.
 */
public class DefaultHistoricalPnLFXConverterFn implements HistoricalPnLFXConverterFn {
  
  private static final Logger s_logger = LoggerFactory.getLogger(DefaultHistoricalPnLFXConverterFn.class);
  
  private final FXMatrixFn _fxMatrixFn;
  private final HistoricalMarketDataFn _historicalMarketDataFn;
  
  //specify whether to use start or end rate
  private final PnLPeriodBound _periodBound;
  
  private final boolean _rollRequired;
  
  /**
   * Constructs a new instance.
   * @param fxMatrixFn the fx matrix for sourcing current spot rate
   * @param historicalMarketDataFn the historical market data function to source the time series from
   * @param periodBound the PnL period bound to use
   */
  public DefaultHistoricalPnLFXConverterFn(FXMatrixFn fxMatrixFn, HistoricalMarketDataFn historicalMarketDataFn, PnLPeriodBound periodBound) {
    _fxMatrixFn = fxMatrixFn;
    _historicalMarketDataFn = historicalMarketDataFn;
    _periodBound = periodBound;
    _rollRequired = rollRequired(periodBound);
  }

  @Override
  public Result<LocalDateDoubleTimeSeries> convertToSpotRate(Environment env, CurrencyPair currencyPair, LocalDateDoubleTimeSeries hts) {
    
    LocalDateRange fxDateRange = getDateRange(hts); 
    
    s_logger.debug("Sourcing {} fx rates for period {}.", currencyPair, fxDateRange);

    Result<LocalDateDoubleTimeSeries> ccyPairHtsResult = _historicalMarketDataFn.getFxRates(env, currencyPair, fxDateRange);
    Result<FXMatrix> fxMatrixResult = _fxMatrixFn.getFXMatrix(env, ImmutableSet.of(currencyPair.getBase(), currencyPair.getCounter()));
    
    if (!Result.allSuccessful(fxMatrixResult, ccyPairHtsResult)) {
      return Result.failure(fxMatrixResult, ccyPairHtsResult);
    }
    
    FXMatrix fxMatrix = fxMatrixResult.getValue();
    
    LocalDateDoubleTimeSeries ccyPairHts = rollIfRequired(currencyPair, ccyPairHtsResult.getValue().reciprocal());
    
    double envFxRate = fxMatrix.getFxRate(currencyPair.getBase(), currencyPair.getCounter());
    
    LocalDateDoubleTimeSeries resultHts = hts.multiply(ccyPairHts).divide(envFxRate);
    
    return Result.success(resultHts);
  }

  private LocalDateDoubleTimeSeries rollIfRequired(CurrencyPair currencyPair, LocalDateDoubleTimeSeries ccyPairHts) {
    if (_rollRequired) {
      s_logger.debug("Rolling {} series since period bound is {}", currencyPair, _periodBound);
      return rollSeries(ccyPairHts);
    } else {
      return ccyPairHts;
    }
  }

  /**
   * Rolls series so that time[i + 1] = rate[i]. This results in the start
   * time and end rate being trimmed. Won't be an issue since the series should
   * have been padded at the beginning.
   */
  private LocalDateDoubleTimeSeries rollSeries(LocalDateDoubleTimeSeries ccyPairHts) {
    
    List<LocalDate> dates = Lists.newLinkedList();
    List<Double> rates = Lists.newLinkedList();
    
    for (int i = 0; i < ccyPairHts.size() - 1; i++) {
      LocalDate time = ccyPairHts.getTimeAtIndex(i + 1);
      Double rate = ccyPairHts.getValueAtIndex(i);
      dates.add(time);
      rates.add(rate);
    }
    
    return ImmutableLocalDateDoubleTimeSeries.of(dates, rates);
    
  }

  /**
   * Returns the appropriate date range to source fx rates for.
   */
  private LocalDateRange getDateRange(LocalDateDoubleTimeSeries hts) {
    LocalDate start = hts.getEarliestTime();
    LocalDate end = hts.getLatestTime();
    //pad start of period if we're sourcing start rates
    if (_rollRequired) {
      start = start.minusDays(7);
    }
    return LocalDateRange.of(start, end, true);
  }

  
  /**
   * Whether fx rates need to be rolled in order to
   * correspond to the correct pnl date.
   */
  private boolean rollRequired(PnLPeriodBound bound) {
    return PnLPeriodBound.START.equals(bound);
  }
  
  
}
