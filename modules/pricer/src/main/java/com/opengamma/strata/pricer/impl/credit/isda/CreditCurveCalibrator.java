/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static com.opengamma.strata.pricer.impl.credit.isda.DoublesScheduleGenerator.getIntegrationsPoints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.rootfinding.NewtonRaphsonSingleRootFinder;

/**
 * 
 */
public class CreditCurveCalibrator {

  private static final NewtonRaphsonSingleRootFinder ROOTFINDER = new NewtonRaphsonSingleRootFinder();

  private final int _nCDS;
  private final int _nCoupons;
  private final double[] _t;
  private final double _valuationDF;
  private final double[] _lgd;
  private final double[] _unitAccured;

  private final int[][] _cds2CouponsMap;
  private final int[][] _cdsCouponsUpdateMap;
  private final int[][] _knot2CouponsMap;
  private final ProtectionLegElement[] _protElems;
  private final CouponOnlyElement[] _premElems;
  private final IsdaCompliantCreditCurveBuilder.ArbitrageHandling _arbHandle;

  public CreditCurveCalibrator(MultiCdsAnalytic multiCDS, IsdaCompliantYieldCurve yieldCurve) {
    this(multiCDS, yieldCurve, AccrualOnDefaultFormulae.ORIGINAL_ISDA, IsdaCompliantCreditCurveBuilder.ArbitrageHandling.Ignore);
  }

  public CreditCurveCalibrator(MultiCdsAnalytic multiCDS, IsdaCompliantYieldCurve yieldCurve, AccrualOnDefaultFormulae formula,
      IsdaCompliantCreditCurveBuilder.ArbitrageHandling arbHandle) {
    ArgChecker.notNull(multiCDS, "multiCDS");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    _arbHandle = arbHandle;

    _nCDS = multiCDS.getNumMaturities();
    _t = new double[_nCDS];
    _lgd = new double[_nCDS];
    _unitAccured = new double[_nCDS];
    for (int i = 0; i < _nCDS; i++) {
      _t[i] = multiCDS.getProtectionEnd(i);
      _lgd[i] = multiCDS.getLGD();
      _unitAccured[i] = multiCDS.getAccruedPremiumPerUnitSpread(i);
    }
    _valuationDF = yieldCurve.getDiscountFactor(multiCDS.getCashSettleTime());

    //This is the global set of knots - it will be truncated down for the various leg elements 
    //TODO this will not match ISDA C for forward starting (i.e. accStart > tradeDate) CDS, and will give different answers 
    //if the Markit 'fix' is used in that case
    double[] knots = getIntegrationsPoints(
        multiCDS.getEffectiveProtectionStart(), _t[_nCDS - 1], yieldCurve.getKnotTimes(), _t);

    //The protection leg
    _protElems = new ProtectionLegElement[_nCDS];
    for (int i = 0; i < _nCDS; i++) {
      _protElems[i] = new ProtectionLegElement(
          i == 0 ? multiCDS.getEffectiveProtectionStart() : _t[i - 1], _t[i], yieldCurve, i, knots);
    }

    _cds2CouponsMap = new int[_nCDS][];
    _cdsCouponsUpdateMap = new int[_nCDS][];
    _knot2CouponsMap = new int[_nCDS][];

    List<CdsCoupon> allCoupons = new ArrayList<>(_nCDS + multiCDS.getTotalPayments() - 1);
    allCoupons.addAll(Arrays.asList(multiCDS.getStandardCoupons()));
    allCoupons.add(multiCDS.getTerminalCoupon(_nCDS - 1));
    int[] temp = new int[multiCDS.getTotalPayments()];
    for (int i = 0; i < multiCDS.getTotalPayments(); i++) {
      temp[i] = i;
    }
    _cds2CouponsMap[_nCDS - 1] = temp;

    //complete the list of unique coupons and fill out the cds2CouponsMap
    for (int i = 0; i < _nCDS - 1; i++) {
      CdsCoupon c = multiCDS.getTerminalCoupon(i);
      int nPayments = Math.max(0, multiCDS.getPaymentIndexForMaturity(i)) + 1;
      _cds2CouponsMap[i] = new int[nPayments];
      for (int jj = 0; jj < nPayments - 1; jj++) {
        _cds2CouponsMap[i][jj] = jj;
      }
      //because of business-day adjustment, a terminal coupon can be identical to a standard coupon,
      //in which case it is not added again 
      int index = allCoupons.indexOf(c);
      if (index == -1) {
        index = allCoupons.size();
        allCoupons.add(c);
      }
      _cds2CouponsMap[i][nPayments - 1] = index;
    }

    //loop over the coupons to populate the couponUpdateMap
    _nCoupons = allCoupons.size();
    int[] sizes = new int[_nCDS];
    int[] map = new int[_nCoupons];
    for (int i = 0; i < _nCoupons; i++) {
      CdsCoupon c = allCoupons.get(i);
      int index = Arrays.binarySearch(_t, c.getEffEnd());
      if (index < 0) {
        index = -(index + 1);
      }
      sizes[index]++;
      map[i] = index;
    }

    //make the protection leg elements 
    _premElems = new CouponOnlyElement[_nCoupons];
    if (multiCDS.isPayAccOnDefault()) {
      for (int i = 0; i < _nCoupons; i++) {
        _premElems[i] = new PremiumLegElement(multiCDS.getEffectiveProtectionStart(), allCoupons.get(i), yieldCurve, map[i],
            knots, formula);
      }
    } else {
      for (int i = 0; i < _nCoupons; i++) {
        _premElems[i] = new CouponOnlyElement(allCoupons.get(i), yieldCurve, map[i]);
      }
    }

    //sort a map from coupon to curve node, to a map from curve node to coupons 
    for (int i = 0; i < _nCDS; i++) {
      _knot2CouponsMap[i] = new int[sizes[i]];
    }
    int[] indexes = new int[_nCDS];
    for (int i = 0; i < _nCoupons; i++) {
      int index = map[i];
      _knot2CouponsMap[index][indexes[index]++] = i;
    }

    //the cdsCouponsUpdateMap is the intersection of the cds2CouponsMap and knot2CouponsMap
    for (int i = 0; i < _nCDS; i++) {
      _cdsCouponsUpdateMap[i] = intersection(_knot2CouponsMap[i], _cds2CouponsMap[i]);
    }

  }

  public CreditCurveCalibrator(CdsAnalytic[] cds, IsdaCompliantYieldCurve yieldCurve) {
    this(cds, yieldCurve, AccrualOnDefaultFormulae.ORIGINAL_ISDA, IsdaCompliantCreditCurveBuilder.ArbitrageHandling.Ignore);
  }

  public CreditCurveCalibrator(CdsAnalytic[] cds, IsdaCompliantYieldCurve yieldCurve, AccrualOnDefaultFormulae formula,
      IsdaCompliantCreditCurveBuilder.ArbitrageHandling arbHandle) {
    ArgChecker.noNulls(cds, "cds");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    _arbHandle = arbHandle;

    _nCDS = cds.length;
    boolean payAccOnDefault = cds[0].isPayAccOnDefault();
    double accStart = cds[0].getAccStart();
    double effectProtStart = cds[0].getEffectiveProtectionStart();
    double cashSettleTime = cds[0].getCashSettleTime();
    _t = new double[_nCDS];
    _t[0] = cds[0].getProtectionEnd();
    //Check all the CDSs match
    for (int i = 1; i < _nCDS; i++) {
      ArgChecker.isTrue(payAccOnDefault == cds[i].isPayAccOnDefault(), "All CDSs must have same pay-accrual on default status");
      ArgChecker.isTrue(accStart == cds[i].getAccStart(), "All CDSs must has same accrual start");
      ArgChecker.isTrue(effectProtStart == cds[i].getEffectiveProtectionStart(),
          "All CDSs must has same effective protection start");
      ArgChecker.isTrue(cashSettleTime == cds[i].getCashSettleTime(), "All CDSs must has same cash-settle time");
      _t[i] = cds[i].getProtectionEnd();
      ArgChecker.isTrue(_t[i] > _t[i - 1], "CDS maturities must be increasing");
    }

    _valuationDF = yieldCurve.getDiscountFactor(cashSettleTime);
    _lgd = new double[_nCDS];
    _unitAccured = new double[_nCDS];
    for (int i = 0; i < _nCDS; i++) {
      _lgd[i] = cds[i].getLGD();
      _unitAccured[i] = cds[i].getAccruedYearFraction();
    }

    //This is the global set of knots - it will be truncated down for the various leg elements 
    //TODO this will not match ISDA C for forward starting (i.e. accStart > tradeDate) CDS, and will give different answers 
    //if the Markit 'fix' is used in that case
    double[] knots = DoublesScheduleGenerator
        .getIntegrationsPoints(effectProtStart, _t[_nCDS - 1], yieldCurve.getKnotTimes(), _t);

    //The protection leg
    _protElems = new ProtectionLegElement[_nCDS];
    for (int i = 0; i < _nCDS; i++) {
      _protElems[i] = new ProtectionLegElement(i == 0 ? effectProtStart : _t[i - 1], _t[i], yieldCurve, i, knots);
    }

    _cds2CouponsMap = new int[_nCDS][];
    _cdsCouponsUpdateMap = new int[_nCDS][];
    _knot2CouponsMap = new int[_nCDS][];

    int nPaymentsFinalCDS = cds[_nCDS - 1].getNumPayments();
    List<CdsCoupon> allCoupons = new ArrayList<>(_nCDS + nPaymentsFinalCDS - 1);
    allCoupons.addAll(Arrays.asList(cds[_nCDS - 1].getCoupons()));
    int[] temp = new int[nPaymentsFinalCDS];
    for (int i = 0; i < nPaymentsFinalCDS; i++) {
      temp[i] = i;
    }
    _cds2CouponsMap[_nCDS - 1] = temp;

    //complete the list of unique coupons and fill out the cds2CouponsMap
    for (int i = 0; i < _nCDS - 1; i++) {
      CdsCoupon[] c = cds[i].getCoupons();
      int nPayments = c.length;
      _cds2CouponsMap[i] = new int[nPayments];
      for (int k = 0; k < nPayments; k++) {
        int index = allCoupons.indexOf(c[k]);
        if (index == -1) {
          index = allCoupons.size();
          allCoupons.add(c[k]);
        }
        _cds2CouponsMap[i][k] = index;
      }
    }

    //loop over the coupons to populate the couponUpdateMap
    _nCoupons = allCoupons.size();
    int[] sizes = new int[_nCDS];
    int[] map = new int[_nCoupons];
    for (int i = 0; i < _nCoupons; i++) {
      CdsCoupon c = allCoupons.get(i);
      int index = Arrays.binarySearch(_t, c.getEffEnd());
      if (index < 0) {
        index = -(index + 1);
      }
      sizes[index]++;
      map[i] = index;
    }

    //make the protection leg elements 
    _premElems = new CouponOnlyElement[_nCoupons];
    if (payAccOnDefault) {
      for (int i = 0; i < _nCoupons; i++) {
        _premElems[i] = new PremiumLegElement(effectProtStart, allCoupons.get(i), yieldCurve, map[i], knots, formula);
      }
    } else {
      for (int i = 0; i < _nCoupons; i++) {
        _premElems[i] = new CouponOnlyElement(allCoupons.get(i), yieldCurve, map[i]);
      }
    }

    //sort a map from coupon to curve node, to a map from curve node to coupons 
    for (int i = 0; i < _nCDS; i++) {
      _knot2CouponsMap[i] = new int[sizes[i]];
    }
    int[] indexes = new int[_nCDS];
    for (int i = 0; i < _nCoupons; i++) {
      int index = map[i];
      _knot2CouponsMap[index][indexes[index]++] = i;
    }

    //the cdsCouponsUpdateMap is the intersection of the cds2CouponsMap and knot2CouponsMap
    for (int i = 0; i < _nCDS; i++) {
      _cdsCouponsUpdateMap[i] = intersection(_knot2CouponsMap[i], _cds2CouponsMap[i]);
    }

  }

  //-------------------------------------------------------------------------
  public IsdaCompliantCreditCurve calibrate(double[] premiums) {
    ArgChecker.notEmpty(premiums, "premiums");
    ArgChecker.isTrue(_nCDS == premiums.length, "premiums wrong length");
    double[] puf = new double[_nCDS];
    CalibrationImpl imp = new CalibrationImpl();
    return imp.calibrate(premiums, puf);
  }

  public IsdaCompliantCreditCurve calibrate(double[] premiums, double[] puf) {
    ArgChecker.notEmpty(premiums, "premiums");
    ArgChecker.notEmpty(puf, "puf");
    ArgChecker.isTrue(_nCDS == premiums.length, "premiums wrong length");
    ArgChecker.isTrue(_nCDS == puf.length, "puf wrong length");

    CalibrationImpl imp = new CalibrationImpl();
    return imp.calibrate(premiums, puf);
  }

  private class CalibrationImpl {

    private double[][] _protLegElmtPV;
    private double[][] _premLegElmtPV;
    private IsdaCompliantCreditCurve _creditCurve;

    public IsdaCompliantCreditCurve calibrate(double[] premiums, double[] puf) {
      _protLegElmtPV = new double[_nCDS][2];
      _premLegElmtPV = new double[_nCoupons][2];

      // use continuous premiums as initial guess
      double[] guess = new double[_nCDS];
      for (int i = 0; i < _nCDS; i++) {
        guess[i] = (premiums[i] + puf[i] / _t[i]) / _lgd[i];
      }

      _creditCurve = new IsdaCompliantCreditCurve(_t, guess);
      for (int i = 0; i < _nCDS; i++) {
        Function1D<Double, Double> func = getPointFunction(i, premiums[i], puf[i]);
        Function1D<Double, Double> grad = getPointDerivative(i, premiums[i]);
        switch (_arbHandle) {
          case Ignore: {
            double zeroRate = ROOTFINDER.getRoot(func, grad, guess[i]);
            updateAll(zeroRate, i);
            break;
          }
          case Fail: {
            double minValue = i == 0 ? 0.0 : _creditCurve.getRTAtIndex(i - 1) / _creditCurve.getTimeAtIndex(i);
            if (i > 0 && func.evaluate(minValue) > 0.0) { //can never fail on the first spread
              StringBuilder msg = new StringBuilder();
              if (puf[i] == 0.0) {
                msg.append("The par spread of " + premiums[i] + " at index " + i);
              } else {
                msg.append("The premium of " + premiums[i] + "and points up-front of " + puf[i] + " at index " + i);
              }
              msg.append(" is an arbitrage; cannot fit a curve with positive forward hazard rate. ");
              throw new IllegalArgumentException(msg.toString());
            }
            guess[i] = Math.max(minValue, guess[i]);
            double zeroRate = ROOTFINDER.getRoot(func, grad, guess[i]);
            updateAll(zeroRate, i);
            break;
          }
          case ZeroHazardRate: {
            double minValue = i == 0 ? 0.0 : _creditCurve.getRTAtIndex(i - 1) / _creditCurve.getTimeAtIndex(i);
            if (i > 0 && func.evaluate(minValue) > 0.0) { //can never fail on the first spread
              // this is setting the forward hazard rate for this period to zero, rather than letting it go negative
              updateAll(minValue, i);
            } else {
              guess[i] = Math.max(minValue, guess[i]);
              double zeroRate = ROOTFINDER.getRoot(func, grad, guess[i]);
              updateAll(zeroRate, i);
            }
            break;
          }
        }
      }

      return _creditCurve;
    }

    private Function1D<Double, Double> getPointFunction(int index, double premium, double puf) {
      int[] iCoupons = _cds2CouponsMap[index];
      int nCoupons = iCoupons.length;
      double dirtyPV = puf - premium * _unitAccured[index];
      double lgd = _lgd[index];
      return new Function1D<Double, Double>() {
        @Override
        public Double evaluate(Double h) {
          update(h, index);
          double protLegPV = 0.0;
          for (int i = 0; i <= index; i++) {
            protLegPV += _protLegElmtPV[i][0];
          }
          double premLegPV = 0.0;
          for (int i = 0; i < nCoupons; i++) {
            int jj = iCoupons[i];
            premLegPV += _premLegElmtPV[jj][0];
          }
          double pv = (lgd * protLegPV - premium * premLegPV) / _valuationDF - dirtyPV;
          return pv;
        }
      };
    }

    private Function1D<Double, Double> getPointDerivative(int index, double premium) {
      int[] iCoupons = _cdsCouponsUpdateMap[index];
      int nCoupons = iCoupons.length;
      double lgd = _lgd[index];
      return new Function1D<Double, Double>() {
        @Override
        public Double evaluate(Double x) {
          //do not call update - all ready called for getting the value 

          double protLegPVSense = _protLegElmtPV[index][1];

          double premLegPVSense = 0.0;
          for (int i = 0; i < nCoupons; i++) {
            int jj = iCoupons[i];
            premLegPVSense += _premLegElmtPV[jj][1];
          }
          double pvSense = (lgd * protLegPVSense - premium * premLegPVSense) / _valuationDF;
          return pvSense;
        }
      };
    }

    private void update(double h, int index) {
      _creditCurve.setRate(h, index);
      _protLegElmtPV[index] = _protElems[index].pvAndSense(_creditCurve);
      int[] iCoupons = _cdsCouponsUpdateMap[index];
      int n = iCoupons.length;
      for (int i = 0; i < n; i++) {
        int jj = iCoupons[i];
        _premLegElmtPV[jj] = _premElems[jj].pvAndSense(_creditCurve);
      }
    }

    private void updateAll(double h, int index) {
      _creditCurve.setRate(h, index);
      _protLegElmtPV[index] = _protElems[index].pvAndSense(_creditCurve);
      int[] iCoupons = _knot2CouponsMap[index];
      int n = iCoupons.length;
      for (int i = 0; i < n; i++) {
        int jj = iCoupons[i];
        _premLegElmtPV[jj] = _premElems[jj].pvAndSense(_creditCurve);
      }
    }

  }

  private static int[] intersection(int[] first, int[] second) {
    int n1 = first.length;
    int n2 = second.length;
    int[] a;
    int[] b;
    int n;
    if (n1 > n2) {
      a = second;
      b = first;
      n = n2;
    } else {
      a = first;
      b = second;
      n = n1;
    }
    int[] temp = new int[n];
    int count = 0;
    for (int i = 0; i < n; i++) {
      int index = Arrays.binarySearch(b, a[i]);
      if (index >= 0) {
        temp[count++] = a[i];
      }
    }
    int[] res = new int[count];
    System.arraycopy(temp, 0, res, 0, count);
    return res;
  }

  //-------------------------------------------------------------------------
  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + ((_arbHandle == null) ? 0 : _arbHandle.hashCode());
    //    Correction made PLAT-6314
    result = prime * result + Arrays.deepHashCode(_cds2CouponsMap);
    result = prime * result + Arrays.deepHashCode(_cdsCouponsUpdateMap);
    result = prime * result + Arrays.deepHashCode(_knot2CouponsMap);
    result = prime * result + Arrays.hashCode(_lgd);
    result = prime * result + _nCDS;
    result = prime * result + _nCoupons;
    result = prime * result + Arrays.hashCode(_premElems);
    result = prime * result + Arrays.hashCode(_protElems);
    result = prime * result + Arrays.hashCode(_t);
    result = prime * result + Arrays.hashCode(_unitAccured);
    long temp;
    temp = Double.doubleToLongBits(_valuationDF);
    result = prime * result + (int) (temp ^ (temp >>> 32));
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
    CreditCurveCalibrator other = (CreditCurveCalibrator) obj;
    if (_arbHandle != other._arbHandle) {
      return false;
    }
    if (!Arrays.deepEquals(_cds2CouponsMap, other._cds2CouponsMap)) {
      return false;
    }
    if (!Arrays.deepEquals(_cdsCouponsUpdateMap, other._cdsCouponsUpdateMap)) {
      return false;
    }
    if (!Arrays.deepEquals(_knot2CouponsMap, other._knot2CouponsMap)) {
      return false;
    }
    if (!Arrays.equals(_lgd, other._lgd)) {
      return false;
    }
    if (_nCDS != other._nCDS) {
      return false;
    }
    if (_nCoupons != other._nCoupons) {
      return false;
    }
    if (!Arrays.equals(_premElems, other._premElems)) {
      return false;
    }
    if (!Arrays.equals(_protElems, other._protElems)) {
      return false;
    }
    if (!Arrays.equals(_t, other._t)) {
      return false;
    }
    if (!Arrays.equals(_unitAccured, other._unitAccured)) {
      return false;
    }
    if (Double.doubleToLongBits(_valuationDF) != Double.doubleToLongBits(other._valuationDF)) {
      return false;
    }
    return true;
  }

}
