/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static com.opengamma.strata.math.impl.util.Epsilon.epsilon;
import static com.opengamma.strata.math.impl.util.Epsilon.epsilonP;

import java.util.Arrays;

/**
 * 
 */
public class ProtectionLegElement {

  private final double[] _knots;
  private final double[] _rt;
  private final double[] _p;
  private final int _n;
  private final int _creditCurveKnot;

  public ProtectionLegElement(
      double start,
      double end,
      IsdaCompliantYieldCurve yieldCurve,
      int creditCurveKnot,
      double[] knots) {

    _knots = DoublesScheduleGenerator.truncateSetInclusive(start, end, knots);
    _n = _knots.length;
    _rt = new double[_n];
    _p = new double[_n];
    for (int i = 0; i < _n; i++) {
      _rt[i] = yieldCurve.getRT(_knots[i]);
      _p[i] = Math.exp(-_rt[i]);
    }
    _creditCurveKnot = creditCurveKnot;
  }

  //-------------------------------------------------------------------------
  public double[] pvAndSense(IsdaCompliantCreditCurve creditCurve) {
    double t = _knots[0];
    double[] htAndSense = creditCurve.getRTandSensitivity(t, _creditCurveKnot);
    double ht0 = htAndSense[0];
    double rt0 = _rt[0];
    double q0 = Math.exp(-ht0);
    double dqdh0 = -htAndSense[1] * q0;

    double p0 = _p[0];
    double b0 = p0 * q0; // risky discount factor

    double pv = 0.0;
    double pvSense = 0.0;
    for (int i = 1; i < _n; ++i) {
      t = _knots[i];
      htAndSense = creditCurve.getRTandSensitivity(t, _creditCurveKnot);
      double ht1 = htAndSense[0];
      double rt1 = _rt[i];
      double q1 = Math.exp(-ht1);
      double p1 = _p[i];
      double b1 = p1 * q1;
      double dqdh1 = -htAndSense[1] * q1;
      double dht = ht1 - ht0;
      double drt = rt1 - rt0;
      double dhrt = dht + drt;

      // The formula has been modified from ISDA (but is equivalent) to avoid log(exp(x)) and explicitly calculating the time
      // step - it also handles the limit
      double dPV;
      double dPVSense;
      if (Math.abs(dhrt) < 1e-5) {
        double e = epsilon(-dhrt);
        double eP = epsilonP(-dhrt);
        dPV = dht * b0 * e;
        double dPVdq0 = p0 * ((1 + dht) * e - dht * eP);
        double dPVdq1 = -p0 * q0 / q1 * (e - dht * eP);
        dPVSense = dPVdq0 * dqdh0 + dPVdq1 * dqdh1;
      } else {
        double w1 = (b0 - b1) / dhrt;
        dPV = dht * w1;
        double w = drt * w1;
        dPVSense = ((w / q0 + dht * p0) / dhrt) * dqdh0 - ((w / q1 + dht * p1) / dhrt) * dqdh1;
      }

      pv += dPV;
      pvSense += dPVSense;
      ht0 = ht1;
      dqdh0 = dqdh1;
      rt0 = rt1;
      p0 = p1;
      q0 = q1;
      b0 = b1;
    }
    return new double[] {pv, pvSense};
  }

  //-------------------------------------------------------------------------
  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + _creditCurveKnot;
    result = prime * result + Arrays.hashCode(_knots);
    result = prime * result + _n;
    result = prime * result + Arrays.hashCode(_p);
    result = prime * result + Arrays.hashCode(_rt);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ProtectionLegElement other = (ProtectionLegElement) obj;
    if (_creditCurveKnot != other._creditCurveKnot) {
      return false;
    }
    if (!Arrays.equals(_knots, other._knots)) {
      return false;
    }
    if (_n != other._n) {
      return false;
    }
    if (!Arrays.equals(_p, other._p)) {
      return false;
    }
    if (!Arrays.equals(_rt, other._rt)) {
      return false;
    }
    return true;
  }

}
