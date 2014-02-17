/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.result.ResultGenerator.success;

import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;

import com.opengamma.analytics.financial.schedule.HolidayDateRemovalFunction;
import com.opengamma.analytics.financial.schedule.Schedule;
import com.opengamma.analytics.financial.schedule.TimeSeriesSamplingFunction;
import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.calendar.MondayToFridayCalendar;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataRequirementFactory;
import com.opengamma.sesame.marketdata.MarketDataSeries;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultGenerator;

/**
 * Function implementation that provides an FX return series for currency pairs.
 */
public class DefaultFXReturnSeriesFn implements FXReturnSeriesFn {

  /** Removes weekends */
  private static final HolidayDateRemovalFunction HOLIDAY_REMOVER = HolidayDateRemovalFunction.getInstance();
  /** A weekend calendar */
  private static final Calendar WEEKEND_CALENDAR = new MondayToFridayCalendar("Weekend");

  /**
   * The market data function.
   */
  private final MarketDataFn _marketDataFn;
  /**
   * The time-series converter.
   */
  private final TimeSeriesReturnConverter _timeSeriesConverter;
  /**
   * The time-series sampling function.
   */
  private final TimeSeriesSamplingFunction _timeSeriesSamplingFunction;
  /**
   * The schedule.
   */
  private final Schedule _scheduleCalculator;

  public DefaultFXReturnSeriesFn(MarketDataFn marketDataFn,
                                 TimeSeriesReturnConverter timeSeriesConverter,
                                 TimeSeriesSamplingFunction timeSeriesSamplingFunction,
                                 Schedule schedule) {
    _marketDataFn = marketDataFn;
    _timeSeriesConverter = timeSeriesConverter;
    _timeSeriesSamplingFunction = timeSeriesSamplingFunction;
    _scheduleCalculator = schedule;
  }

  //-------------------------------------------------------------------------
  @Override
  public Result<LocalDateDoubleTimeSeries> calculateReturnSeries(Period seriesPeriod, CurrencyPair currencyPair) {

    Result<MarketDataSeries> result =
        _marketDataFn.requestData(MarketDataRequirementFactory.of(currencyPair), seriesPeriod);

    return result.isValueAvailable() ?
        success(calculateReturnSeries((LocalDateDoubleTimeSeries) result.getValue().getOnlySeries())) :
        propagateFailure(result);
  }

  @Override
  public LocalDateDoubleTimeSeries calculateReturnSeries(LocalDateDoubleTimeSeries timeSeries) {
    // todo - is faffing about with include start / end required?
    final LocalDate[] dates = HOLIDAY_REMOVER.getStrippedSchedule(
        _scheduleCalculator.getSchedule(timeSeries.getEarliestTime(), timeSeries.getLatestTime(), true, false),
        WEEKEND_CALENDAR);
    LocalDateDoubleTimeSeries sampledTimeSeries = _timeSeriesSamplingFunction.getSampledTimeSeries(timeSeries, dates);

    // Implementation note: to obtain the series for one unit of non-base currency expressed in base currency.
    LocalDateDoubleTimeSeries reciprocalSeries = sampledTimeSeries.reciprocal();

    // todo - clip the time-series to the range originally asked for?
    return _timeSeriesConverter.convert(reciprocalSeries);
  }

  private Result<LocalDateDoubleTimeSeries> propagateFailure(Result<MarketDataSeries> result) {
    return ResultGenerator.propagateFailure(result);
  }

}
