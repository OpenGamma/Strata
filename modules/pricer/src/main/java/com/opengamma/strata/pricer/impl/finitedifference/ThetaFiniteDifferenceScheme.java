package com.opengamma.strata.pricer.impl.finitedifference;

import java.util.function.Function;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.math.impl.FunctionUtils;

public class ThetaFiniteDifferenceScheme {

  public double solve(
      double spot,
      Function<DoublesPair, DoubleArray> coeffs,
      DoubleArray terminal,
      DoubleArray ts,
      DoubleArray xs) {

    int nX = ts.size();
    int nT = xs.size();
    double[] v = terminal.toArray();
    double[] diag = new double[nX];
    double[] upper = new double[nX - 1];
    double[] lower = new double[nX - 1];
    for (int i = 0; i < nT; ++i) {
      for (int j = 0; j < nX - 2; ++j) {
        upper[j + 1] = 0d;
        diag[j + 1] = 0d;
        lower[j] = 0d;
      }
      upper[0] = 0d;
      diag[0] = 0d;
      diag[nX - 1] = 0d;
      lower[nX - 2] = 0d;

    }

    // linear interpolation
    int index = FunctionUtils.getLowerBoundIndex(xs, spot);
    double w = (xs.get(index + 1) - spot) / (xs.get(index + 1) - xs.get(index));
    return w * v[index] + (1 - w) * v[index + 1];
  }

}
