/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import org.threeten.bp.LocalDate;

import com.google.common.base.Function;
import com.opengamma.analytics.financial.schedule.HolidayDateRemovalFunction;
import com.opengamma.analytics.financial.schedule.Schedule;
import com.opengamma.analytics.financial.schedule.TimeSeriesSamplingFunction;
import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.calendar.MondayToFridayCalendar;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.sesame.marketdata.HistoricalMarketDataFn;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.LocalDateRange;

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
  private final HistoricalMarketDataFn _historicalMarketDataFn;
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

  public DefaultFXReturnSeriesFn(HistoricalMarketDataFn historicalMarketDataFn,
                                 TimeSeriesReturnConverter timeSeriesConverter,
                                 TimeSeriesSamplingFunction timeSeriesSamplingFunction,
                                 Schedule schedule) {
    _historicalMarketDataFn = historicalMarketDataFn;
    _timeSeriesConverter = timeSeriesConverter;
    _timeSeriesSamplingFunction = timeSeriesSamplingFunction;
    _scheduleCalculator = schedule;
  }

  //-------------------------------------------------------------------------
  @Override
  public Result<LocalDateDoubleTimeSeries> calculateReturnSeries(Environment env,
                                                                 LocalDateRange dateRange,
                                                                 CurrencyPair currencyPair) {
    return _historicalMarketDataFn.getFxRates(env, currencyPair, dateRange).flatMap(
        new Function<LocalDateDoubleTimeSeries, Result<LocalDateDoubleTimeSeries>>() {
          @Override
          public Result<LocalDateDoubleTimeSeries> apply(LocalDateDoubleTimeSeries input) {
            return Result.success(_timeSeriesConverter.convert(input));
          }
        });
  }

  @Override
  //TODO [SSM-243] this doesn't really apply specifically to FX. move elsewhere?
  public LocalDateDoubleTimeSeries calculateReturnSeries(Environment env, LocalDateDoubleTimeSeries timeSeries) {
    // todo - is faffing about with include start / end required?
    final LocalDate[] dates = HOLIDAY_REMOVER.getStrippedSchedule(
        _scheduleCalculator.getSchedule(timeSeries.getEarliestTime(), timeSeries.getLatestTime(), true, false),
        WEEKEND_CALENDAR);
    LocalDateDoubleTimeSeries sampledTimeSeries = _timeSeriesSamplingFunction.getSampledTimeSeries(timeSeries, dates);

    // todo - clip the time-series to the range originally asked for?
    return _timeSeriesConverter.convert(sampledTimeSeries);
  }
}
