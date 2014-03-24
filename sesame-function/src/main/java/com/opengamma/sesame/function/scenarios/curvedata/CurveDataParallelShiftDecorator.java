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
 * Function that decorates {@link CurveSpecificationMarketDataFn} and applies parallel shifts to the underlying data.
 */
public class CurveDataParallelShiftDecorator implements CurveSpecificationMarketDataFn {

  /** The underlying function that this function decorates. */
  private final CurveSpecificationMarketDataFn _delegate;

  /**
   * @param delegate the function to decorate
   */
  public CurveDataParallelShiftDecorator(CurveSpecificationMarketDataFn delegate) {
    _delegate = ArgumentChecker.notNull(delegate, "delegate");
  }

  @Override
  public Result<Map<ExternalIdBundle, Double>> requestData(Environment env, CurveSpecification curveSpecification) {
    Result<Map<ExternalIdBundle, Double>> result = _delegate.requestData(env, curveSpecification);

    if (!result.isSuccess()) {
      return result;
    }
    Shifts shifts = (Shifts) env.getScenarioArgument(getClass());

    if (shifts == null) {
      return result;
    }
    Map<ExternalIdBundle, Double> results = result.getValue();

    for (CurveDataParallelShift shift : shifts._shifts) {
      results = shift.apply(curveSpecification, results);
    }
    return Result.success(results);
  }

  /**
   * Creates an instance of {@link Shifts} wrapping some {@link CurveDataParallelShift} instances.
   *
   * @param shifts some shifts
   * @return an instance of {@link Shifts} wrapping the shifts
   */
  public static Shifts shifts(CurveDataParallelShift... shifts) {
    List<CurveDataParallelShift> shiftList = new ArrayList<>(shifts.length);

    for (CurveDataParallelShift shift : shifts) {
      if (shift == null) {
        throw new IllegalArgumentException("Null shifts not allowed");
      }
      shiftList.add(shift);
    }
    return new Shifts(Collections.unmodifiableList(shiftList));
  }

  /**
   * Wraps a list of {@link CurveDataParallelShift} instances.
   */
  public static final class Shifts {

    private final List<CurveDataParallelShift> _shifts;

    private Shifts(List<CurveDataParallelShift> shifts) {
      _shifts = shifts;
    }
  }
}
