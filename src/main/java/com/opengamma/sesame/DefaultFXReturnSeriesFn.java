/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.result.FunctionResultGenerator.success;

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
import com.opengamma.util.result.FunctionResult;
import com.opengamma.util.result.FunctionResultGenerator;

public class DefaultFXReturnSeriesFn implements FXReturnSeriesFn {

  /** Removes weekends */
  private static final HolidayDateRemovalFunction HOLIDAY_REMOVER = HolidayDateRemovalFunction.getInstance();

  /** A weekend calendar */
  private static final Calendar WEEKEND_CALENDAR = new MondayToFridayCalendar("Weekend");

  private final MarketDataFn _marketDataFn;

  private final TimeSeriesReturnConverter _timeSeriesConverter;

  private final TimeSeriesSamplingFunction _timeSeriesSamplingFunction;

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

  @Override
  public FunctionResult<LocalDateDoubleTimeSeries> calculateReturnSeries(Period seriesPeriod, CurrencyPair currencyPair) {

    FunctionResult<MarketDataSeries> result =
        _marketDataFn.requestData(MarketDataRequirementFactory.of(currencyPair), seriesPeriod);

    return result.isResultAvailable() ?
        success(calculateReturnSeries((LocalDateDoubleTimeSeries) result.getResult().getOnlySeries())) :
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

  private FunctionResult<LocalDateDoubleTimeSeries> propagateFailure(FunctionResult<MarketDataSeries> result) {
    return FunctionResultGenerator.propagateFailure(result);
  }
}
