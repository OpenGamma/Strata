/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.result.ResultGenerator.success;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.core.config.ConfigSource;
import com.opengamma.financial.analytics.curve.ConfigDBCurveSpecificationBuilder;
import com.opengamma.financial.analytics.curve.CurveDefinition;
import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.util.result.Result;

/**
 * Provides a curve specification.
 */
public class DefaultCurveSpecificationFn implements CurveSpecificationFn {

  private final ConfigDBCurveSpecificationBuilder _curveSpecificationBuilder;
  private final ValuationTimeFn _valuationTimeFn;

  public DefaultCurveSpecificationFn(ConfigSource configSource,
                                     ValuationTimeFn valuationTimeFn) {

    _valuationTimeFn = valuationTimeFn;
    _curveSpecificationBuilder = new ConfigDBCurveSpecificationBuilder(configSource);
  }

  @Override
  public Result<CurveSpecification> getCurveSpecification(CurveDefinition curveDefinition) {
    return buildSpecification(curveDefinition, _valuationTimeFn.getTime());
  }

  @Override
  public Result<CurveSpecification> getCurveSpecification(CurveDefinition curveDefinition, ZonedDateTime valuationTime) {
    return buildSpecification(curveDefinition, valuationTime);
  }

  private Result<CurveSpecification> buildSpecification(CurveDefinition curveDefinition,
                                                        ZonedDateTime valuationTime) {
    return success(_curveSpecificationBuilder.buildCurve(
        valuationTime.toInstant(),
        LocalDate.now(), // Want the current curves (is that what this represents)
        curveDefinition));
  }
}
