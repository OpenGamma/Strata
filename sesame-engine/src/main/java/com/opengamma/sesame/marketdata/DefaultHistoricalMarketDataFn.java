/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.LocalDate;

import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.financial.currency.CurrencyMatrixValue;
import com.opengamma.financial.currency.CurrencyMatrixValueVisitor;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.Environment;
import com.opengamma.timeseries.date.localdate.ImmutableLocalDateDoubleTimeSeries;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;
import com.opengamma.util.time.LocalDateRange;

/**
 *
 */
public class DefaultHistoricalMarketDataFn implements HistoricalMarketDataFn {

  private static final Logger s_logger = LoggerFactory.getLogger(DefaultHistoricalMarketDataFn.class);

  private static final FieldName MARKET_VALUE = FieldName.of(MarketDataRequirementNames.MARKET_VALUE);

  private final HistoricalTimeSeriesSource _timeSeriesSource;
  private final String _dataSource;
  private final String _dataProvider;
  private final CurrencyMatrix _currencyMatrix;

  public DefaultHistoricalMarketDataFn(HistoricalTimeSeriesSource timeSeriesSource,
                                       String dataSource,
                                       @Nullable String dataProvider,
                                       CurrencyMatrix currencyMatrix) {
    _timeSeriesSource = ArgumentChecker.notNull(timeSeriesSource, "timeSeriesSource");
    _dataSource = ArgumentChecker.notEmpty(dataSource, "dataSource");
    _dataProvider = dataProvider;
    _currencyMatrix = ArgumentChecker.notNull(currencyMatrix, "currencyMatrix");
  }

  @Override
  public MarketDataItem<LocalDateDoubleTimeSeries> getCurveNodeValues(Environment env,
                                                                      CurveNodeWithIdentifier node,
                                                                      LocalDateRange dateRange) {
    ExternalIdBundle id = node.getIdentifier().toBundle();
    FieldName fieldName = FieldName.of(node.getDataField());
    return get(id, fieldName, dateRange);
  }

  @Override
  public MarketDataItem<LocalDateDoubleTimeSeries> getMarketValues(Environment env,
                                                                   ExternalIdBundle id,
                                                                   LocalDateRange dateRange) {
    return get(id, MARKET_VALUE, dateRange);
  }

  @Override
  public MarketDataItem<LocalDateDoubleTimeSeries> getValues(Environment env,
                                                             ExternalIdBundle id,
                                                             FieldName fieldName,
                                                             LocalDateRange dateRange) {
    return get(id, fieldName, dateRange);
  }

  @Override
  public MarketDataItem<LocalDateDoubleTimeSeries> getFxRates(Environment env,
                                                              CurrencyPair currencyPair,
                                                              LocalDateRange dateRange) {
    return getFxRates(dateRange, currencyPair.getBase(), currencyPair.getCounter());
  }

  private MarketDataItem<LocalDateDoubleTimeSeries> get(ExternalIdBundle idBundle,
                                                        FieldName fieldName,
                                                        LocalDateRange dateRange) {
    LocalDate startDate = dateRange.getStartDateInclusive();
    LocalDate endDate = dateRange.getEndDateInclusive();
    HistoricalTimeSeries hts =
        _timeSeriesSource.getHistoricalTimeSeries(idBundle, _dataSource, _dataProvider, fieldName.getName(),
                                                  startDate, true, endDate, true);
    if (hts == null || hts.getTimeSeries().isEmpty()) {
      s_logger.info("No time-series for {}", idBundle);
      return MarketDataItem.unavailable();
    } else {
      return MarketDataItem.available(hts.getTimeSeries());
    }
  }

  private MarketDataItem<LocalDateDoubleTimeSeries> getFxRates(final LocalDateRange dateRange,
                                                               final Currency base,
                                                               final Currency counter) {
    CurrencyMatrixValue value = _currencyMatrix.getConversion(base, counter);
    if (value == null) {
      return MarketDataItem.unavailable();
    }

    CurrencyMatrixValueVisitor<MarketDataItem<LocalDateDoubleTimeSeries>> visitor =
        new CurrencyMatrixValueVisitor<MarketDataItem<LocalDateDoubleTimeSeries>>() {

      @Override
      public MarketDataItem<LocalDateDoubleTimeSeries> visitFixed(CurrencyMatrixValue.CurrencyMatrixFixed fixedValue) {
        LocalDateDoubleTimeSeriesBuilder builder = ImmutableLocalDateDoubleTimeSeries.builder();
        LocalDate start = dateRange.getStartDateInclusive();
        LocalDate end = dateRange.getEndDateInclusive();
        double fixedRate = fixedValue.getFixedValue();

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
          builder.put(date, fixedRate);
        }
        return MarketDataItem.available(builder.build());
      }

      @SuppressWarnings("unchecked")
      @Override
      public MarketDataItem<LocalDateDoubleTimeSeries> visitValueRequirement(
          CurrencyMatrixValue.CurrencyMatrixValueRequirement req) {

        ValueRequirement valueRequirement = req.getValueRequirement();
        ExternalIdBundle idBundle = valueRequirement.getTargetReference().getRequirement().getIdentifiers();
        String dataField = valueRequirement.getValueName();
        MarketDataItem item = get(idBundle, FieldName.of(dataField), dateRange);

        if (!item.isAvailable()) {
          return MarketDataItem.unavailable();
        }
        LocalDateDoubleTimeSeries spotRate = (LocalDateDoubleTimeSeries) item.getValue();

        if (req.isReciprocal()) {
          return MarketDataItem.available(spotRate.reciprocal());
        } else {
          return MarketDataItem.available(spotRate);
        }
      }

      @Override
      public MarketDataItem<LocalDateDoubleTimeSeries> visitCross(CurrencyMatrixValue.CurrencyMatrixCross cross) {
        MarketDataItem baseCrossRate = getFxRates(dateRange, base, cross.getCrossCurrency());
        MarketDataItem crossCounterRate = getFxRates(dateRange, cross.getCrossCurrency(), counter);

        if (!baseCrossRate.isAvailable() || !crossCounterRate.isAvailable()) {
          return MarketDataItem.unavailable();
        } else {
          LocalDateDoubleTimeSeries rate1 = (LocalDateDoubleTimeSeries) baseCrossRate.getValue();
          LocalDateDoubleTimeSeries rate2 = (LocalDateDoubleTimeSeries) crossCounterRate.getValue();
          return MarketDataItem.available(rate1.multiply(rate2));
        }
      }
    };
    return value.accept(visitor);
  }
}
