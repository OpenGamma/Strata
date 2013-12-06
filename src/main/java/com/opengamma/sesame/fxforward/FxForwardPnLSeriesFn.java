/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.sesame.FunctionResult;
import com.opengamma.sesame.example.OutputNames;
import com.opengamma.sesame.function.Output;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;

public interface FXForwardPnLSeriesFn {

  @Output(OutputNames.PNL_SERIES)
  FunctionResult<LocalDateDoubleTimeSeries> calculatePnlSeries(FXForwardSecurity security);
}
