/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.core.historicaltimeseries2;

import com.opengamma.core.change.ChangeProvider;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.util.PublicSPI;

/**
 * A source of daily historical time-series as accessed by the engine.
 * <p>
 * The interface provides access to historical time-series data on a daily basis.
 * There may be other uses of time-series within the application, but this interface is
 * specifically focused on the requirement for daily data.
 * <p>
 * This interface provides a simple view of the time-series as needed by the engine.
 * This may be backed by a full-featured time-series master, or by a much simpler data structure.
 * <p>
 * This interface is read-only.
 * Implementations must be thread-safe.
 */
@PublicSPI
public interface HistoricalTimeSeriesSource extends ChangeProvider {

  /**
   * Returns the time-series specified by HistoricalDataRequest object
   * @param request historical data request object
   * @return the historical time series, null if not found
   */
  HistoricalTimeSeries getHistoricalTimeSeries(HistoricalDataRequest request);

}

