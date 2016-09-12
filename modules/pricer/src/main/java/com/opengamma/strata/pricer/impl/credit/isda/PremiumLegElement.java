/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static com.opengamma.strata.math.impl.util.Epsilon.epsilon;
import static com.opengamma.strata.math.impl.util.Epsilon.epsilonP;
import static com.opengamma.strata.math.impl.util.Epsilon.epsilonPP;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;

/**
 * 
 */
public class PremiumLegElement extends CouponOnlyElement {

  private final CdsCoupon _coupon;

  private final AccrualOnDefaultFormulae _formula;
  private final double _omega;

  private final int _creditCurveKnot;

  private final double[] _knots;
  private final double[] _rt;
  private final double[] _p;
  private final int _n;

  public PremiumLegElement(
      double protectionStart,
      CdsCoupon coupon,
      IsdaCompliantYieldCurve yieldCurve,
      int creditCurveKnot,
      double[] knots,
      AccrualOnDefaultFormulae formula) {

    super(coupon, yieldCurve, creditCurveKnot);
    ArgChecker.notNull(coupon, "coupon");
    _coupon = coupon;

    _creditCurveKnot = creditCurveKnot;
    _formula = formula;
    if (formula == AccrualOnDefaultFormulae.ORIGINAL_ISDA) {
      _omega = 1. / 730;
    } else {
      _omega = 0.0;
    }

    _knots = DoublesScheduleGenerator.truncateSetInclusive(
        Math.max(_coupon.getEffStart(), protectionStart), _coupon.getEffEnd(), knots);
    _n = _knots.length;
    _rt = new double[_n];
    _p = new double[_n];
    for (int i = 0; i < _n; i++) {
      _rt[i] = yieldCurve.getRT(_knots[i]);
      _p[i] = Math.exp(-_rt[i]);
    }
  }

  @Override
  public double[] pvAndSense(IsdaCompliantCreditCurve creditCurve) {
    double[] pv = super.pvAndSense(creditCurve);

    double[] aod = new double[2];
    if (_formula == AccrualOnDefaultFormulae.MARKIT_FIX) {
      aod = accOnDefaultMarkitFix(creditCurve);
    } else {
      aod = accOnDefault(creditCurve);
    }
    return new double[] {pv[0] + aod[0], pv[1] + aod[1]};
  }

  private double[] accOnDefault(IsdaCompliantCreditCurve creditCurve) {

    double t = _knots[0];
    double[] htAndSense = creditCurve.getRTandSensitivity(t, _creditCurveKnot);
    double ht0 = htAndSense[0];
    double rt0 = _rt[0];
    double p0 = _p[0];
    double q0 = Math.exp(-ht0);
    double b0 = p0 * q0; // this is the risky discount factor
    double dqdr0 = -htAndSense[1] * q0;

    double t0 = t - _coupon.getEffStart() + _omega;
    double pv = 0.0;
    double pvSense = 0.0;

    for (int j = 1; j < _n; ++j) {
      t = _knots[j];
      htAndSense = creditCurve.getRTandSensitivity(t, _creditCurveKnot);
      double ht1 = htAndSense[0];
      double rt1 = _rt[j];
      double p1 = _p[j];
      double q1 = Math.exp(-ht1);
      double b1 = p1 * q1;
      double dqdr1 = -htAndSense[1] * q1;

      double dt = _knots[j] - _knots[j - 1];

      double dht = ht1 - ht0;
      double drt = rt1 - rt0;
      double dhrt = dht + drt;

      double tPV;
      double tPvSense;

      double t1 = t - _coupon.getEffStart() + _omega;
      if (Math.abs(dhrt) < 1e-5) {
        double e = epsilon(-dhrt);
        double eP = epsilonP(-dhrt);
        double ePP = epsilonPP(-dhrt);
        double w1 = t0 * e + dt * eP;
        double w2 = t0 * eP + dt * ePP;
        double dPVdq0 = p0 * ((1 + dhrt) * w1 - dht * w2);
        double dPVdq1 = b0 / q1 * (-w1 + dht * w2);
        tPV = dht * b0 * w1;
        tPvSense = dPVdq0 * dqdr0 + dPVdq1 * dqdr1;
      } else {
        double w1 = dt / dhrt;
        double w2 = dht / dhrt;
        double w3 = (t0 + w1) * b0 - (t1 + w1) * b1;
        double w4 = (1 - w2) / dhrt;
        double w5 = w1 / dhrt * (b0 - b1);
        double dPVdq0 = w4 * w3 / q0 + w2 * ((t0 + w1) * p0 - w5 / q0);
        double dPVdq1 = w4 * w3 / q1 + w2 * ((t1 + w1) * p1 - w5 / q1);
        tPV = dht / dhrt * (t0 * b0 - t1 * b1 + dt / dhrt * (b0 - b1));
        tPvSense = dPVdq0 * dqdr0 - dPVdq1 * dqdr1;
      }
      t0 = t1;

      pv += tPV;
      pvSense += tPvSense;
      ht0 = ht1;
      rt0 = rt1;
      p0 = p1;
      q0 = q1;
      b0 = b1;
      dqdr0 = dqdr1;
    }
    return new double[] {_coupon.getYFRatio() * pv, _coupon.getYFRatio() * pvSense};
  }

  private double[] accOnDefaultMarkitFix(IsdaCompliantCreditCurve creditCurve) {

    double t = _knots[0];
    double[] htAndSense = creditCurve.getRTandSensitivity(t, _creditCurveKnot);
    double ht0 = htAndSense[0];
    double rt0 = _rt[0];
    double p0 = _p[0];
    double q0 = Math.exp(-ht0);
    double b0 = p0 * q0; // this is the risky discount factor
    double dqdr0 = -htAndSense[1] * q0;

    double pv = 0.0;
    double pvSense = 0.0;

    for (int j = 1; j < _n; ++j) {
      t = _knots[j];
      htAndSense = creditCurve.getRTandSensitivity(t, _creditCurveKnot);
      double ht1 = htAndSense[0];
      double rt1 = _rt[j];
      double p1 = _p[j];
      double q1 = Math.exp(-ht1);
      double b1 = p1 * q1;
      double dqdr1 = -htAndSense[1] * q1;

      double dt = _knots[j] - _knots[j - 1];

      double dht = ht1 - ht0;
      double drt = rt1 - rt0;
      double dhrt = dht + drt;

      double tPV;
      double tPvSense;

      if (Math.abs(dhrt) < 1e-5) {
        double eP = epsilonP(-dhrt);
        double ePP = epsilonPP(-dhrt);
        tPV = dht * dt * b0 * eP;
        double dPVdq0 = p0 * dt * ((1 + dht) * eP - dht * ePP);
        double dPVdq1 = b0 * dt / q1 * (-eP + dht * ePP);
        tPvSense = dPVdq0 * dqdr0 + dPVdq1 * dqdr1;
      } else {
        double w1 = (b0 - b1) / dhrt;
        double w2 = w1 - b1;
        double w3 = dht / dhrt;
        double w4 = dt / dhrt;
        double w5 = (1 - w3) * w2;
        double dPVdq0 = w4 / q0 * (w5 + w3 * (b0 - w1));
        double dPVdq1 = w4 / q1 * (w5 + w3 * (b1 * (1 + dhrt) - w1));
        tPV = dt * w3 * w2;
        tPvSense = dPVdq0 * dqdr0 - dPVdq1 * dqdr1;

      }
      pv += tPV;
      pvSense += tPvSense;
      ht0 = ht1;
      rt0 = rt1;
      p0 = p1;
      q0 = q1;
      b0 = b1;
      dqdr0 = dqdr1;
    }
    return new double[] {_coupon.getYFRatio() * pv, _coupon.getYFRatio() * pvSense};
  }

  //-------------------------------------------------------------------------
  @Override
  public int hashCode() {
    int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((_coupon == null) ? 0 : _coupon.hashCode());
    result = prime * result + _creditCurveKnot;
    result = prime * result + ((_formula == null) ? 0 : _formula.hashCode());
    result = prime * result + Arrays.hashCode(_knots);
    result = prime * result + _n;
    long temp;
    temp = Double.doubleToLongBits(_omega);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + Arrays.hashCode(_p);
    result = prime * result + Arrays.hashCode(_rt);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    PremiumLegElement other = (PremiumLegElement) obj;
    if (_coupon == null) {
      if (other._coupon != null) {
        return false;
      }
    } else if (!_coupon.equals(other._coupon)) {
      return false;
    }
    if (_creditCurveKnot != other._creditCurveKnot) {
      return false;
    }
    if (_formula != other._formula) {
      return false;
    }
    if (!Arrays.equals(_knots, other._knots)) {
      return false;
    }
    if (_n != other._n) {
      return false;
    }
    if (Double.doubleToLongBits(_omega) != Double.doubleToLongBits(other._omega)) {
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
