package com.opengamma.strata.examples.exampleccp;

import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;

public class MyCurves {

  public static Curve oisCurveDiscount() {
    return ois("ois-discount");
  }

  public static Curve oisCurve() {
    return ois("ois");
  }

  private static Curve ois(String name) {
    double nodePoints[] = {
        0.0027397260273972603,
        0.09863013698630137,
        0.5178082191780822,
        1.0182872969533647,
        2.021917808219178,
        5.018287296953365,
        10.01917808219178
    };
    double zeroRates[] = {
        0.0202772145268651,
        0.0221765975781118,
        0.0241315208703011,
        0.0260012770821695,
        0.0280403369214972,
        0.0300564184949819,
        0.0321912791614938
    };
    return InterpolatedNodalCurve.of(
        name,
        nodePoints,
        zeroRates,
        Interpolator1DFactory.LINEAR_INSTANCE
    );
  }
}
