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
 */
public class CurveDataShiftDecorator implements CurveSpecificationMarketDataFn {

  /** The underlying function that this function decorates. */
  private final CurveSpecificationMarketDataFn _delegate;

  /**
   * @param delegate the function to decorate
   */
  public CurveDataShiftDecorator(CurveSpecificationMarketDataFn delegate) {
    _delegate = ArgumentChecker.notNull(delegate, "delegate");
  }

  @Override
  public Result<Map<ExternalIdBundle, Double>> requestData(Environment env, CurveSpecification curveSpecification) {
    Result<Map<ExternalIdBundle, Double>> result = _delegate.requestData(env, curveSpecification);

    if (!result.isSuccess()) {
      return result;
    }
    Shifts shifts = (Shifts) env.getScenarioArgument(this);

    if (shifts == null) {
      return result;
    }
    Map<ExternalIdBundle, Double> results = result.getValue();

    for (CurveDataShift shift : shifts._shifts) {
      results = shift.apply(curveSpecification, results);
    }
    return Result.success(results);
  }

  /**
   * Creates an instance of {@link Shifts} wrapping some {@link CurveDataShift} instances.
   *
   * @param shifts some shifts
   * @return an instance of {@link Shifts} wrapping the shifts
   */
  public static Shifts shifts(CurveDataShift... shifts) {
    List<CurveDataShift> shiftList = new ArrayList<>(shifts.length);

    for (CurveDataShift shift : shifts) {
      if (shift == null) {
        throw new IllegalArgumentException("Null shifts not allowed");
      }
      shiftList.add(shift);
    }
    return new Shifts(Collections.unmodifiableList(shiftList));
  }

  /**
   * Wraps a list of {@link CurveDataShift} instances.
   */
  public static final class Shifts {

    private final List<CurveDataShift> _shifts;

    private Shifts(List<CurveDataShift> shifts) {
      _shifts = shifts;
    }
  }
}
