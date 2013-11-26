/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;

import com.opengamma.analytics.financial.schedule.HolidayDateRemovalFunction;
import com.opengamma.analytics.financial.schedule.Schedule;
import com.opengamma.analytics.financial.schedule.TimeSeriesSamplingFunction;
import com.opengamma.analytics.financial.timeseries.util.TimeSeriesDifferenceOperator;
import com.opengamma.analytics.financial.timeseries.util.TimeSeriesPercentageChangeOperator;
import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.calendar.MondayToFridayCalendar;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.sesame.marketdata.MarketDataProviderFunction;
import com.opengamma.sesame.marketdata.MarketDataRequirementFactory;
import com.opengamma.sesame.marketdata.MarketDataSeries;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;

public class FxReturnSeriesProvider implements FxReturnSeriesProviderFunction {

  /**
   * The calculation type used for a return series
   */
  public enum ReturnSeriesType {
    /**
     * Calculate the return series relative to each preceding value.
     */
    RELATIVE,
    /**
     * Calculate the return series in absolute terms from each preceding value.
     */
    ABSOLUTE
  }

  /**
   * The weighting used for a return series.
   */
  public enum ReturnSeriesWeighting {
    /**
     * Volatility weighting applied to the return series.
     */
    VOLATILITY_WEIGHTED,
    /**
     * No weighting applied to the return series.
     */
    NONE
  }

  // todo - unclear if these attributes can be set independently of each other

  /** Removes weekends */
  private static final HolidayDateRemovalFunction HOLIDAY_REMOVER = HolidayDateRemovalFunction.getInstance();

  /** A weekend calendar */
  private static final Calendar WEEKEND_CALENDAR = new MondayToFridayCalendar("Weekend");

  private final ReturnSeriesType _returnSeriesType;

  private final ReturnSeriesWeighting _returnSeriesWeighting;

  private final MarketDataProviderFunction _marketDataProviderFunction;

  // TimeSeriesSamplingFunctionFactory.getFunction(samplingFunctionName);
  private final TimeSeriesSamplingFunction _timeSeriesSamplingFunction;


  // ScheduleCalculatorFactory.getScheduleCalculator(scheduleCalculatorName)
  private final Schedule _scheduleCalculator;

  // todo - the appropriate calculator should perhaps be passed in directly

  /** Calculates an absolute return time series */
  private static final TimeSeriesDifferenceOperator DIFFERENCE = new TimeSeriesDifferenceOperator();
  /** Calculates a relative return time series */
  private static final TimeSeriesPercentageChangeOperator PERCENTAGE_CHANGE = new TimeSeriesPercentageChangeOperator();

  public FxReturnSeriesProvider(ReturnSeriesType returnSeriesType,
                                ReturnSeriesWeighting returnSeriesWeighting,
                                MarketDataProviderFunction marketDataProviderFunction,
                                TimeSeriesSamplingFunction timeSeriesSamplingFunction, Schedule schedule) {
    _returnSeriesType = returnSeriesType;
    _returnSeriesWeighting = returnSeriesWeighting;
    _marketDataProviderFunction = marketDataProviderFunction;
    _timeSeriesSamplingFunction = timeSeriesSamplingFunction;
    _scheduleCalculator = schedule;
  }

  @Override
  public FunctionResult<LocalDateDoubleTimeSeries> getReturnSeries(Period seriesPeriod, CurrencyPair currencyPair) {

    FunctionResult<MarketDataSeries> result =
        _marketDataProviderFunction.requestData(MarketDataRequirementFactory.of(currencyPair), seriesPeriod);

    if (result.isResultAvailable()) {

      // todo - is faffing abount with include start / end required?
      LocalDateDoubleTimeSeries timeSeries = (LocalDateDoubleTimeSeries) result.getResult().getOnlySeries();

      final LocalDate[] dates = HOLIDAY_REMOVER.getStrippedSchedule(_scheduleCalculator.getSchedule(timeSeries.getEarliestTime(), timeSeries.getLatestTime(), true, false), WEEKEND_CALENDAR);
      LocalDateDoubleTimeSeries sampledTimeSeries = _timeSeriesSamplingFunction.getSampledTimeSeries(timeSeries, dates);

      // Implementation note: to obtain the series for one unit of non-base currency expressed in base currency.
      LocalDateDoubleTimeSeries reciprocalSeries = sampledTimeSeries.reciprocal();

      LocalDateDoubleTimeSeries returnSeries = calculateReturnSeries(reciprocalSeries);

      // todo - clip the time-series to the range originally asked for?

      return StandardResultGenerator.success(returnSeries);


    } else {
      return StandardResultGenerator.propagateFailure(result);
    }
  }

  private LocalDateDoubleTimeSeries calculateReturnSeries(LocalDateDoubleTimeSeries reciprocalSeries) {

    // todo - no account taken of vol weighting yet
    return (LocalDateDoubleTimeSeries) (_returnSeriesType == ReturnSeriesType.RELATIVE ?
            PERCENTAGE_CHANGE.evaluate(reciprocalSeries) :
            DIFFERENCE.evaluate(reciprocalSeries));
  }
}
