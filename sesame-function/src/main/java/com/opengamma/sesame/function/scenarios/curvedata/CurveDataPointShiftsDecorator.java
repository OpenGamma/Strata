/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function.scenarios.curvedata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.CurveSpecificationMarketDataFn;
import com.opengamma.sesame.Environment;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;

/**
 * Function that decorates {@link CurveSpecificationMarketDataFn} and applies shifts to the underlying data.
 * The points to shift are specified by tenor.
 */
public class CurveDataPointShiftsDecorator implements CurveSpecificationMarketDataFn {

  /** The underlying function that this function decorates. */
  private final CurveSpecificationMarketDataFn _delegate;

  /**
   * @param delegate the function to decorate
   */
  public CurveDataPointShiftsDecorator(CurveSpecificationMarketDataFn delegate) {
    _delegate = ArgumentChecker.notNull(delegate, "delegate");
  }

  @Override
  public Result<Map<ExternalIdBundle, Double>> requestData(Environment env, final CurveSpecification curveSpecification) {
    Result<Map<ExternalIdBundle, Double>> underlyingData = _delegate.requestData(env, curveSpecification);

    if (!underlyingData.isSuccess()) {
      return underlyingData;
    }
    Shifts shifts = (Shifts) env.getScenarioArgument(this);

    if (shifts == null) {
      return underlyingData;
    }
    Result<Map<ExternalIdBundle, Double>> results = underlyingData;
    List<Result<?>> failures = new ArrayList<>();

    for (final CurveDataPointShifts shift : shifts._shifts) {
      results = shift.apply(curveSpecification, results.getValue());

      if (!results.isSuccess()) {
        failures.add(results);
      }
    }
    if (Result.anyFailures(failures)) {
      return Result.failure(failures);
    } else {
      return results;
    }
  }

  /**
   * Creates an instance of {@link Shifts} wrapping some {@link CurveDataPointShifts} instances.
   *
   * @param shifts some shifts
   * @return an instance of {@link Shifts} wrapping the shifts
   */
  public static Shifts shifts(CurveDataPointShifts... shifts) {
    List<CurveDataPointShifts> shiftList = new ArrayList<>(shifts.length);

    for (CurveDataPointShifts shift : shifts) {
      if (shift == null) {
        throw new IllegalArgumentException("Null shifts not allowed");
      }
      shiftList.add(shift);
    }
    return new Shifts(Collections.unmodifiableList(shiftList));
  }

  /**
   * Wraps a list of {@link CurveDataPointShifts} instances.
   */
  public static final class Shifts {

    private final List<CurveDataPointShifts> _shifts;

    private Shifts(List<CurveDataPointShifts> shifts) {
      _shifts = shifts;
    }
  }
}
