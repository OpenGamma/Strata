/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.HashMap;
import java.util.Map;

import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.financial.analytics.curve.CurveDefinition;
import com.opengamma.financial.analytics.curve.credit.ConfigDBCurveDefinitionSource;
import com.opengamma.financial.analytics.curve.credit.CurveDefinitionSource;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * Function implementation that returns a curve definition from the configured source.
 * <p>
 * If not available, the return Result will indicate the reason why.
 */
public class DefaultCurveDefinitionFn implements CurveDefinitionFn {

  /**
   * The underlying source.
   */
  private final CurveDefinitionSource _curveDefinitionSource;

  public DefaultCurveDefinitionFn(ConfigSource configSource) {
    _curveDefinitionSource = new ConfigDBCurveDefinitionSource(ArgumentChecker.notNull(configSource, "configSource"));
  }

  //-------------------------------------------------------------------------
  @Override
  public Result<CurveDefinition> getCurveDefinition(String curveName) {

    final CurveDefinition curveDefinition = _curveDefinitionSource.getCurveDefinition(curveName);
    if (curveDefinition != null) {
      return Result.success(curveDefinition);
    } else {
      return Result.failure(FailureStatus.MISSING_DATA, "Could not get curve definition called {}", curveName);
    }
  }

  @Override
  public Result<Map<String, CurveDefinition>> getCurveDefinitions(CurveBuildingBlockBundle block) {

    Map<String, CurveDefinition> curveDefinitions = new HashMap<>();
    for (String curveName : block.getData().keySet()) {
      Result<CurveDefinition> curveDefinition = getCurveDefinition(curveName);
      if (curveDefinition.isSuccess()) {
        curveDefinitions.put(curveName, curveDefinition.getValue());
      } else {
        return Result.failure(FailureStatus.MISSING_DATA, "Could not get curve definition called {}", curveName);
      }
    }
    return Result.success(curveDefinitions);
  }

}

