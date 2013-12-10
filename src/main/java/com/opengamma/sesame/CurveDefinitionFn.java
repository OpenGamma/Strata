/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.financial.analytics.curve.CurveDefinition;
import com.opengamma.util.result.FunctionResult;

/**
 * Function capable of creating a curve definition.
 */
public interface CurveDefinitionFn {

  // TODO @Output
  FunctionResult<CurveDefinition> getCurveDefinition(String curveName);
}
