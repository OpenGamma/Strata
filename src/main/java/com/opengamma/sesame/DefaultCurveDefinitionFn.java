/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.result.ResultGenerator.failure;
import static com.opengamma.util.result.ResultGenerator.success;

import com.opengamma.core.config.ConfigSource;
import com.opengamma.financial.analytics.curve.CurveDefinition;
import com.opengamma.financial.analytics.curve.credit.ConfigDBCurveDefinitionSource;
import com.opengamma.financial.analytics.curve.credit.CurveDefinitionSource;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * Returns a curve definition from the configured source. If not available, the
 * return Result will indicate the reason why.
 */
public class DefaultCurveDefinitionFn implements CurveDefinitionFn {

  private final CurveDefinitionSource _curveDefinitionSource;

  public DefaultCurveDefinitionFn(ConfigSource configSource) {
    _curveDefinitionSource = new ConfigDBCurveDefinitionSource(ArgumentChecker.notNull(configSource, "configSource"));
  }

  @Override
  public Result<CurveDefinition> getCurveDefinition(String curveName) {

    final CurveDefinition curveDefinition = _curveDefinitionSource.getCurveDefinition(curveName);
    if (curveDefinition != null) {
      return success(curveDefinition);
    } else {
      return failure(FailureStatus.MISSING_DATA, "Could not get curve definition called {}", curveName);
    }
  }
}
