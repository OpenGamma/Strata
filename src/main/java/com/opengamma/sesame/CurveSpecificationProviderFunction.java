/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.financial.analytics.curve.CurveSpecification;

public interface CurveSpecificationProviderFunction {

  FunctionResult<CurveSpecification> getCurveSpecification(String curveName);
}
