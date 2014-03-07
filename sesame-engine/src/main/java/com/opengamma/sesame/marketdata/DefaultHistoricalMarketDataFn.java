/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import javax.annotation.Nullable;

import org.threeten.bp.LocalDate;

import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.PointsCurveNodeWithIdentifier;
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
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultGenerator;
import com.opengamma.util.time.LocalDateRange;

/**
 *
 */
public class DefaultHistoricalMarketDataFn implements HistoricalMarketDataFn {

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
  public Result<LocalDateDoubleTimeSeries> getCurveNodeValues(Environment env,
                                                              CurveNodeWithIdentifier node,
                                                              LocalDateRange dateRange) {
    ExternalIdBundle id = node.getIdentifier().toBundle();
    FieldName fieldName = FieldName.of(node.getDataField());
    return get(id, fieldName, dateRange);
  }

  @Override
  public Result<LocalDateDoubleTimeSeries> getCurveNodeUnderlyingValue(Environment env,
                                                                       PointsCurveNodeWithIdentifier node,
                                                                       LocalDateRange dateRange) {
    ExternalIdBundle id = node.getUnderlyingIdentifier().toBundle();
    FieldName fieldName = FieldName.of(node.getUnderlyingDataField());
    return get(id, fieldName, dateRange);
  }

  @Override
  public Result<LocalDateDoubleTimeSeries> getMarketValues(Environment env, ExternalIdBundle id, LocalDateRange dateRange) {
    return get(id, MARKET_VALUE, dateRange);
  }

  @Override
  public Result<LocalDateDoubleTimeSeries> getValues(Environment env,
                                                     ExternalIdBundle id,
                                                     FieldName fieldName,
                                                     LocalDateRange dateRange) {
    return get(id, fieldName, dateRange);
  }

  @Override
  public Result<LocalDateDoubleTimeSeries> getFxRates(Environment env, CurrencyPair currencyPair, LocalDateRange dateRange) {
    return getFxRates(dateRange, currencyPair.getBase(), currencyPair.getCounter());
  }

  private Result<LocalDateDoubleTimeSeries> get(ExternalIdBundle id, FieldName fieldName, LocalDateRange dateRange) {
    LocalDate startDate = dateRange.getStartDateInclusive();
    LocalDate endDate = dateRange.getEndDateInclusive();
    HistoricalTimeSeries hts =
        _timeSeriesSource.getHistoricalTimeSeries(id, _dataSource, _dataProvider, fieldName.getName(),
                                                  startDate, true, endDate, true);
    if (hts == null || hts.getTimeSeries().isEmpty()) {
      return ResultGenerator.failure(FailureStatus.MISSING_DATA, "No data found for {}/{}", id, fieldName);

    } else {
      return ResultGenerator.success(hts.getTimeSeries());
    }
  }

  private Result<LocalDateDoubleTimeSeries> getFxRates(final LocalDateRange dateRange,
                                                       final Currency base,
                                                       final Currency counter) {
    CurrencyMatrixValue value = _currencyMatrix.getConversion(base, counter);
    if (value == null) {
      return ResultGenerator.failure(FailureStatus.MISSING_DATA,
                                     "No conversion found for {}",
                                     CurrencyPair.of(base, counter));
    }
    CurrencyMatrixValueVisitor<Result<LocalDateDoubleTimeSeries>> visitor =
        new CurrencyMatrixValueVisitor<Result<LocalDateDoubleTimeSeries>>() {

      @Override
      public Result<LocalDateDoubleTimeSeries> visitFixed(CurrencyMatrixValue.CurrencyMatrixFixed fixedValue) {
        LocalDateDoubleTimeSeriesBuilder builder = ImmutableLocalDateDoubleTimeSeries.builder();
        LocalDate start = dateRange.getStartDateInclusive();
        LocalDate end = dateRange.getEndDateInclusive();
        double fixedRate = fixedValue.getFixedValue();

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
          builder.put(date, fixedRate);
        }
        return ResultGenerator.success(builder.build());
      }

      @SuppressWarnings("unchecked")
      @Override
      public Result<LocalDateDoubleTimeSeries> visitValueRequirement(
          CurrencyMatrixValue.CurrencyMatrixValueRequirement req) {

        ValueRequirement valueRequirement = req.getValueRequirement();
        ExternalIdBundle idBundle = valueRequirement.getTargetReference().getRequirement().getIdentifiers();
        String dataField = valueRequirement.getValueName();
        Result<LocalDateDoubleTimeSeries> result = get(idBundle, FieldName.of(dataField), dateRange);

        if (!result.isValueAvailable()) {
          return result;
        }
        LocalDateDoubleTimeSeries spotRate = result.getValue();

        if (req.isReciprocal()) {
          return ResultGenerator.success(spotRate.reciprocal());
        } else {
          return ResultGenerator.success(spotRate);
        }
      }

      @Override
      public Result<LocalDateDoubleTimeSeries> visitCross(CurrencyMatrixValue.CurrencyMatrixCross cross) {
        Result<LocalDateDoubleTimeSeries> baseCrossRate = getFxRates(dateRange, base, cross.getCrossCurrency());
        Result<LocalDateDoubleTimeSeries> crossCounterRate = getFxRates(dateRange, cross.getCrossCurrency(), counter);

        if (ResultGenerator.anyFailures(baseCrossRate, crossCounterRate)) {
          return ResultGenerator.propagateFailures(baseCrossRate, crossCounterRate);
        } else {
          LocalDateDoubleTimeSeries rate1 = baseCrossRate.getValue();
          LocalDateDoubleTimeSeries rate2 = crossCounterRate.getValue();
          return ResultGenerator.success(rate1.multiply(rate2));
        }
      }
    };
    return value.accept(visitor);
  }
}
