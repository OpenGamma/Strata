package com.opengamma.strata.examples.exampleccp;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;

public class MyCurves {

  public static YieldCurve oisCurveDiscount() {
    return ois("ois-discount");
  }

  public static YieldCurve oisCurve() {
    return ois("ois");
  }

  private static YieldCurve ois(String name) {
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
    return YieldCurve.from(
        InterpolatedDoublesCurve.from(
            nodePoints,
            zeroRates,
            Interpolator1DFactory.LINEAR_INSTANCE,
            name
        )
    );
  }
}
