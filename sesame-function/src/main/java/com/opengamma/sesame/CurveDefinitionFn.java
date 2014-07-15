/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Map;

import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.financial.analytics.curve.CurveDefinition;
import com.opengamma.util.result.Result;

/**
 * Function capable of creating a curve definition.
 */
public interface CurveDefinitionFn {

  // TODO @Output
  /**
   * Gets the curve definition by name.
   * 
   * @param curveName  the curve name, not null
   * @return the curve definition, a failure result if not found
   */
  Result<CurveDefinition> getCurveDefinition(String curveName);


  /**
   * Gets the curve definitions by name.
   *
   * @param block  the curve building block bundle, not null
   * @return the curve definitions keyed by curve name, a failure result if not found
   */
  Result<Map<String, CurveDefinition>> getCurveDefinitions(CurveBuildingBlockBundle block);

}
