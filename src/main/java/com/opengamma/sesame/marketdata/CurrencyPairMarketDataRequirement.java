/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.financial.currency.CurrencyMatrixValue;
import com.opengamma.financial.currency.CurrencyMatrixValueVisitor;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.timeseries.DoubleTimeSeries;
import com.opengamma.util.money.Currency;
import com.opengamma.util.time.LocalDateRange;

/**
 * A market data requirement for a currency pair spot rate.
 */
public class CurrencyPairMarketDataRequirement implements MarketDataRequirement {

  /**
   * The currency pair to get the spot rate for.
   */
  private final CurrencyPair _currencyPair;

  /* package */ CurrencyPairMarketDataRequirement(CurrencyPair currencyPair) {
    _currencyPair = currencyPair;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    return _currencyPair.equals(((CurrencyPairMarketDataRequirement) o)._currencyPair);
  }

  @Override
  public int hashCode() {
    return _currencyPair.hashCode();
  }

  @Override
  public String toString() {
    return "CurrencyPairMarketDataRequirement [" + _currencyPair + "]";
  }

  /* package */ MarketDataItem getFxRate(CurrencyMatrix currencyMatrix, RawMarketDataSource dataSource) {
    return getFxRate(currencyMatrix, dataSource, _currencyPair.getBase(), _currencyPair.getCounter());
  }

  // TODO does this logic belong in this class? maybe not. move it if there turns out to be a better place
  private MarketDataItem getFxRate(final CurrencyMatrix currencyMatrix,
                                   final RawMarketDataSource dataSource,
                                   final Currency base,
                                   final Currency counter) {
    CurrencyMatrixValue value = currencyMatrix.getConversion(base, counter);
    if (value == null) {
      return MarketDataItem.missing(MarketDataStatus.UNAVAILABLE);
    }
    CurrencyMatrixValueVisitor<MarketDataItem> visitor = new CurrencyMatrixValueVisitor<MarketDataItem>() {
      @Override
      public MarketDataItem visitFixed(CurrencyMatrixValue.CurrencyMatrixFixed fixedValue) {
        return MarketDataItem.available(fixedValue.getFixedValue());
      }

      @SuppressWarnings("unchecked")
      @Override
      public MarketDataItem visitValueRequirement(CurrencyMatrixValue.CurrencyMatrixValueRequirement req) {
        ValueRequirement valueRequirement = req.getValueRequirement();
        ExternalIdBundle idBundle = valueRequirement.getTargetReference().getRequirement().getIdentifiers();
        String dataField = valueRequirement.getValueName();
        MarketDataItem item = dataSource.get(idBundle, dataField);
        if (!item.isAvailable()) {
          return MarketDataItem.missing(MarketDataStatus.UNAVAILABLE);
        }
        Double spotRate = (Double) item.getValue();
        if (req.isReciprocal()) {
          return MarketDataItem.available(1 / spotRate);
        } else {
          return MarketDataItem.available(spotRate);
        }
      }

      @Override
      public MarketDataItem visitCross(CurrencyMatrixValue.CurrencyMatrixCross cross) {
        MarketDataItem baseCrossRate = getFxRate(currencyMatrix, dataSource, base, cross.getCrossCurrency());
        MarketDataItem crossCounterRate = getFxRate(currencyMatrix, dataSource, cross.getCrossCurrency(), counter);
        if (!baseCrossRate.isAvailable() || !crossCounterRate.isAvailable()) {
          // TODO should this be pending?
          return MarketDataItem.missing(MarketDataStatus.UNAVAILABLE);
        } else {
          Double rate1 = (Double) baseCrossRate.getValue();
          Double rate2 = (Double) crossCounterRate.getValue();
          return MarketDataItem.available(rate1 * rate2);
        }
      }
    };
    return value.accept(visitor);
  }

  /* package */ MarketDataItem getFxRateSeries(LocalDateRange dateRange,
                                               CurrencyMatrix currencyMatrix,
                                               RawMarketDataSource rawDataSource) {
    return getFxRateSeries(dateRange,
                           currencyMatrix,
                           rawDataSource,
                           _currencyPair.getBase(),
                           _currencyPair.getCounter());
  }

  private MarketDataItem getFxRateSeries(final LocalDateRange dateRange,
                                         final CurrencyMatrix currencyMatrix,
                                         final RawMarketDataSource dataSource,
                                         final Currency base,
                                         final Currency counter) {
    // TODO needs to look a lot like getFxRate. see CurrencyMatrixSeriesSourcingFunction.getFxRate
    CurrencyMatrixValue value = currencyMatrix.getConversion(base, counter);
    if (value == null) {
      return MarketDataItem.missing(MarketDataStatus.UNAVAILABLE);
    }
    CurrencyMatrixValueVisitor<MarketDataItem> visitor = new CurrencyMatrixValueVisitor<MarketDataItem>() {
      @Override
      public MarketDataItem visitFixed(CurrencyMatrixValue.CurrencyMatrixFixed fixedValue) {
        // TODO is this right? don't I need a constant time series?
        // the existing code returns a double but that smells like an undiscovered bug to me
        return MarketDataItem.available(fixedValue.getFixedValue());
      }

      @SuppressWarnings("unchecked")
      @Override
      public MarketDataItem visitValueRequirement(CurrencyMatrixValue.CurrencyMatrixValueRequirement req) {
        ValueRequirement valueRequirement = req.getValueRequirement();
        ExternalIdBundle idBundle = valueRequirement.getTargetReference().getRequirement().getIdentifiers();
        String dataField = valueRequirement.getValueName();
        MarketDataItem item = dataSource.get(idBundle, dataField, dateRange);
        if (!item.isAvailable()) {
          return MarketDataItem.missing(MarketDataStatus.UNAVAILABLE);
        }
        DoubleTimeSeries<?> spotRate = (DoubleTimeSeries<?>) item.getValue();
        if (req.isReciprocal()) {
          return MarketDataItem.available(spotRate.reciprocal());
        } else {
          return MarketDataItem.available(spotRate);
        }
      }

      @Override
      public MarketDataItem visitCross(CurrencyMatrixValue.CurrencyMatrixCross cross) {
        MarketDataItem baseCrossRate = getFxRateSeries(dateRange,
                                                       currencyMatrix,
                                                       dataSource,
                                                       base,
                                                       cross.getCrossCurrency());
        MarketDataItem crossCounterRate = getFxRateSeries(dateRange,
                                                          currencyMatrix,
                                                          dataSource,
                                                          cross.getCrossCurrency(),
                                                          counter);
        if (!baseCrossRate.isAvailable() || !crossCounterRate.isAvailable()) {
          // TODO should this ever be pending?
          return MarketDataItem.missing(MarketDataStatus.UNAVAILABLE);
        } else {
          DoubleTimeSeries<?> rate1 = (DoubleTimeSeries<?>) baseCrossRate.getValue();
          DoubleTimeSeries<?> rate2 = (DoubleTimeSeries<?>) crossCounterRate.getValue();
          return MarketDataItem.available(rate1.multiply(rate2));
        }
      }
    };
    return value.accept(visitor);
  }
}
