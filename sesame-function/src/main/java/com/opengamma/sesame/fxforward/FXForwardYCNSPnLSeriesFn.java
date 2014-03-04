/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import com.opengamma.financial.analytics.TenorLabelledLocalDateDoubleTimeSeriesMatrix1D;
import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.function.Output;
import com.opengamma.util.result.Result;

/**
 */
public interface FXForwardYCNSPnLSeriesFn {

  @Output(OutputNames.YCNS_PNL_SERIES)
  Result<TenorLabelledLocalDateDoubleTimeSeriesMatrix1D> calculateYCNSPnlSeries(Environment env, FXForwardSecurity security);
}
