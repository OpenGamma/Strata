/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.util.result.Result;

/**
 * Converts curve nodes into instruments, in particular for curve calibration.
 */
public interface CurveNodeConverterFn {

  /**
   * Converts a time independent instrument definition into a time dependent instrument derivative.
   *
   * @param node The curve node
   * @param definition The definition
   * @param valuationTime The valuation time
   * @return A derivative instrument
   */
  Result<InstrumentDerivative> getDerivative(Environment env,
                                             CurveNodeWithIdentifier node,
                                             InstrumentDefinition<?> definition,
                                             ZonedDateTime valuationTime);
}
