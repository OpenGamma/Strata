/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.result.ResultGenerator.failure;
import static com.opengamma.util.result.ResultGenerator.success;

import com.opengamma.core.config.ConfigSource;
import com.opengamma.financial.analytics.curve.ConfigDBCurveSpecificationBuilder;
import com.opengamma.financial.analytics.curve.CurveDefinition;
import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.analytics.curve.credit.ConfigDBCurveDefinitionSource;
import com.opengamma.financial.analytics.curve.credit.CurveDefinitionSource;
import com.opengamma.id.VersionCorrection;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * Provides a curve specification.
 */
public class DefaultCurveSpecificationFn implements CurveSpecificationFn {

  private final CurveDefinitionSource _curveDefinitionSource;
  private final ConfigDBCurveSpecificationBuilder _curveSpecificationBuilder;
  private final ValuationTimeFn _valuationTimeFn;

  public DefaultCurveSpecificationFn(ConfigSource configSource,
                                     ValuationTimeFn valuationTimeFn) {

    _valuationTimeFn = valuationTimeFn;
    _curveDefinitionSource = new ConfigDBCurveDefinitionSource(configSource);
    _curveSpecificationBuilder = new ConfigDBCurveSpecificationBuilder(configSource);
  }

  @Override
  public Result<CurveSpecification> getCurveSpecification(String curveName) {

    final CurveDefinition curveDefinition =
        _curveDefinitionSource.getCurveDefinition(curveName, VersionCorrection.LATEST);

    if (curveDefinition == null) {
      return failure(FailureStatus.MISSING_DATA, "Could not get curve definition called: {}", curveName);
    } else {
      return success(_curveSpecificationBuilder.buildCurve(
          _valuationTimeFn.getTime().toInstant(),
          _valuationTimeFn.getDate(),
          curveDefinition));
    }
  }
}
