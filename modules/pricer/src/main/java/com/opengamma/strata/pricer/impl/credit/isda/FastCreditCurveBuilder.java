/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static com.opengamma.strata.math.impl.util.Epsilon.epsilon;
import static com.opengamma.strata.math.impl.util.Epsilon.epsilonP;
import static com.opengamma.strata.pricer.impl.credit.isda.DoublesScheduleGenerator.getIntegrationsPoints;
import static com.opengamma.strata.pricer.impl.credit.isda.DoublesScheduleGenerator.truncateSetInclusive;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.MathException;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.rootfinding.BracketRoot;
import com.opengamma.strata.math.impl.rootfinding.BrentSingleRootFinder;
import com.opengamma.strata.math.impl.rootfinding.RealSingleRootFinder;

/**
 * This is a fast bootstrapper for the credit curve that is consistent with ISDA in that it will produce the same curve from
 * the same inputs (up to numerical round-off)
 */

public class FastCreditCurveBuilder extends IsdaCompliantCreditCurveBuilder {
  private static final double HALFDAY = 1 / 730.;
  private static final BracketRoot BRACKER = new BracketRoot();
  private static final RealSingleRootFinder ROOTFINDER = new BrentSingleRootFinder();

  private final double _omega;

  /**
   *Construct a credit curve builder that uses the Original ISDA accrual-on-default formula (version 1.8.2 and lower)
   */
  public FastCreditCurveBuilder() {
    super();
    _omega = HALFDAY;
  }

  /**
   *Construct a credit curve builder that uses the specified accrual-on-default formula
   * @param formula The accrual on default formulae. <b>Note</b> The MarkitFix is erroneous
   */
  public FastCreditCurveBuilder(final AccrualOnDefaultFormulae formula) {
    super(formula);
    if (formula == AccrualOnDefaultFormulae.ORIGINAL_ISDA) {
      _omega = HALFDAY;
    } else {
      _omega = 0.0;
    }
  }

  /**
   * Construct a credit curve builder that uses the specified accrual-on-default formula and arbitrage handing 
  * @param formula The accrual on default formulae. <b>Note</b> The MarkitFix is erroneous
   * @param arbHandling How should any arbitrage in the input date be handled
   */
  public FastCreditCurveBuilder(AccrualOnDefaultFormulae formula, ArbitrageHandling arbHandling) {
    super(formula, arbHandling);
    if (formula == AccrualOnDefaultFormulae.ORIGINAL_ISDA) {
      _omega = HALFDAY;
    } else {
      _omega = 0.0;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IsdaCompliantCreditCurve calibrateCreditCurve(
      CdsAnalytic[] cds,
      double[] premiums,
      IsdaCompliantYieldCurve yieldCurve,
      double[] pointsUpfront) {

    ArgChecker.noNulls(cds, "null CDSs");
    ArgChecker.notEmpty(premiums, "empty fractionalSpreads");
    ArgChecker.notEmpty(pointsUpfront, "empty pointsUpfront");
    ArgChecker.notNull(yieldCurve, "null yieldCurve");
    int n = cds.length;
    ArgChecker.isTrue(n == premiums.length, "Number of CDSs does not match number of spreads");
    ArgChecker.isTrue(n == pointsUpfront.length, "Number of CDSs does not match number of pointsUpfront");
    double proStart = cds[0].getEffectiveProtectionStart();
    for (int i = 1; i < n; i++) {
      ArgChecker.isTrue(proStart == cds[i].getEffectiveProtectionStart(), "all CDSs must has same protection start");
      ArgChecker.isTrue(cds[i].getProtectionEnd() > cds[i - 1].getProtectionEnd(), "protection end must be ascending");
    }

    // use continuous premiums as initial guess
    double[] guess = new double[n];
    double[] t = new double[n];
    for (int i = 0; i < n; i++) {
      t[i] = cds[i].getProtectionEnd();
      guess[i] = (premiums[i] + pointsUpfront[i] / t[i]) / cds[i].getLGD();
    }

    IsdaCompliantCreditCurve creditCurve = new IsdaCompliantCreditCurve(t, guess);
    for (int i = 0; i < n; i++) {
      Pricer pricer = new Pricer(cds[i], yieldCurve, t, premiums[i], pointsUpfront[i]);
      Function1D<Double, Double> func = pricer.getPointFunction(i, creditCurve);

      switch (getArbHanding()) {
        case Ignore: {
          try {
            double[] bracket = BRACKER.getBracketedPoints(func, 0.8 * guess[i], 1.25 * guess[i], Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY);
            double zeroRate = bracket[0] > bracket[1] ?
                ROOTFINDER.getRoot(func, bracket[1], bracket[0]) :
                ROOTFINDER.getRoot(func, bracket[0], bracket[1]); //Negative guess handled
            creditCurve = creditCurve.withRate(zeroRate, i);
          } catch (MathException e) { //handling bracketing failure due to small survival probability
            if (Math.abs(func.evaluate(creditCurve.getZeroRateAtIndex(i - 1))) < 1.e-12) {
              creditCurve = creditCurve.withRate(creditCurve.getZeroRateAtIndex(i - 1), i);
            } else {
              throw new MathException(e);
            }
          }
          break;
        }
        case Fail: {
          double minValue = i == 0 ? 0.0 : creditCurve.getRTAtIndex(i - 1) / creditCurve.getTimeAtIndex(i);
          if (i > 0 && func.evaluate(minValue) > 0.0) { //can never fail on the first spread
            StringBuilder msg = new StringBuilder();
            if (pointsUpfront[i] == 0.0) {
              msg.append("The par spread of " + premiums[i] + " at index " + i);
            } else {
              msg.append("The premium of " + premiums[i] + "and points up-front of " + pointsUpfront[i] + " at index " + i);
            }
            msg.append(" is an arbitrage; cannot fit a curve with positive forward hazard rate. ");
            throw new IllegalArgumentException(msg.toString());
          }
          guess[i] = Math.max(minValue, guess[i]);
          double[] bracket = BRACKER.getBracketedPoints(func, guess[i], 1.2 * guess[i], minValue, Double.POSITIVE_INFINITY);
          double zeroRate = ROOTFINDER.getRoot(func, bracket[0], bracket[1]);
          creditCurve = creditCurve.withRate(zeroRate, i);
          break;
        }
        case ZeroHazardRate: {
          double minValue = i == 0 ? 0.0 : creditCurve.getRTAtIndex(i - 1) / creditCurve.getTimeAtIndex(i);
          if (i > 0 && func.evaluate(minValue) > 0.0) { //can never fail on the first spread
            creditCurve = creditCurve.withRate(minValue, i);
          } else {
            guess[i] = Math.max(minValue, guess[i]);
            double[] bracket = BRACKER.getBracketedPoints(func, guess[i], 1.2 * guess[i], minValue, Double.POSITIVE_INFINITY);
            double zeroRate = ROOTFINDER.getRoot(func, bracket[0], bracket[1]);
            creditCurve = creditCurve.withRate(zeroRate, i);
          }
          break;
        }
        default:
          throw new IllegalArgumentException("unknow case " + getArbHanding());
      }

    }
    return creditCurve;
  }

  /**
   * Prices the CDS
   */
  protected class Pricer {

    private final CdsAnalytic _cds;
    private final double _lgdDF;
    private final double _valuationDF;
    private final double _fracSpread;
    private final double _pointsUpfront;

    // protection leg
    private final int _nProPoints;
    private final double[] _proLegIntPoints;
    private final double[] _proYieldCurveRT;
    private final double[] _proDF;

    // premium leg
    private final int _nPayments;
    private final double[] _paymentDF;
    private final double[][] _premLegIntPoints;
    private final double[][] _premDF;
    private final double[][] _rt;
    private final double[][] _premDt;
    private final double[] _accRate;
    private final double[] _offsetAccStart;

    public Pricer(
        CdsAnalytic cds,
        IsdaCompliantYieldCurve yieldCurve,
        double[] creditCurveKnots,
        double fractionalSpread,
        double pointsUpfront) {

      _cds = cds;
      _fracSpread = fractionalSpread;
      _pointsUpfront = pointsUpfront;

      // protection leg
      _proLegIntPoints = getIntegrationsPoints(
          cds.getEffectiveProtectionStart(),
          cds.getProtectionEnd(),
          yieldCurve.getKnotTimes(),
          creditCurveKnots);
      _nProPoints = _proLegIntPoints.length;
      double lgd = cds.getLGD();
      _valuationDF = yieldCurve.getDiscountFactor(cds.getCashSettleTime());
      _lgdDF = lgd / _valuationDF;
      _proYieldCurveRT = new double[_nProPoints];
      _proDF = new double[_nProPoints];
      for (int i = 0; i < _nProPoints; i++) {
        _proYieldCurveRT[i] = yieldCurve.getRT(_proLegIntPoints[i]);
        _proDF[i] = Math.exp(-_proYieldCurveRT[i]);
      }

      // premium leg
      _nPayments = cds.getNumPayments();
      _paymentDF = new double[_nPayments];
      for (int i = 0; i < _nPayments; i++) {
        _paymentDF[i] = yieldCurve.getDiscountFactor(cds.getCoupon(i).getPaymentTime());
      }

      if (cds.isPayAccOnDefault()) {
        double tmp = cds.getNumPayments() == 1 ? cds.getEffectiveProtectionStart() : cds.getAccStart();
        double[] integrationSchedule = getIntegrationsPoints(
            tmp, cds.getProtectionEnd(), yieldCurve.getKnotTimes(), creditCurveKnots);

        _accRate = new double[_nPayments];
        _offsetAccStart = new double[_nPayments];
        _premLegIntPoints = new double[_nPayments][];
        _premDF = new double[_nPayments][];
        _rt = new double[_nPayments][];
        _premDt = new double[_nPayments][];
        for (int i = 0; i < _nPayments; i++) {
          CdsCoupon c = cds.getCoupon(i);
          _offsetAccStart[i] = c.getEffStart();
          double offsetAccEnd = c.getEffEnd();
          _accRate[i] = c.getYFRatio();
          double start = Math.max(_offsetAccStart[i], cds.getEffectiveProtectionStart());
          if (start >= offsetAccEnd) {
            continue;
          }
          _premLegIntPoints[i] = truncateSetInclusive(start, offsetAccEnd, integrationSchedule);
          int n = _premLegIntPoints[i].length;
          _rt[i] = new double[n];
          _premDF[i] = new double[n];
          for (int k = 0; k < n; k++) {
            _rt[i][k] = yieldCurve.getRT(_premLegIntPoints[i][k]);
            _premDF[i][k] = Math.exp(-_rt[i][k]);
          }
          _premDt[i] = new double[n - 1];

          for (int k = 1; k < n; k++) {
            double dt = _premLegIntPoints[i][k] - _premLegIntPoints[i][k - 1];
            _premDt[i][k - 1] = dt;
          }

        }
      } else {
        _accRate = null;
        _offsetAccStart = null;
        _premDF = null;
        _premDt = null;
        _rt = null;
        _premLegIntPoints = null;
      }

    }

    public Function1D<Double, Double> getPointFunction(int index, IsdaCompliantCreditCurve creditCurve) {
      return new Function1D<Double, Double>() {
        @Override
        public Double evaluate(Double x) {
          IsdaCompliantCreditCurve cc = creditCurve.withRate(x, index);
          double rpv01 = rpv01(cc, CdsPriceType.CLEAN);
          double pro = protectionLeg(cc);
          return pro - _fracSpread * rpv01 - _pointsUpfront;
        }
      };

    }

    public double rpv01(IsdaCompliantCreditCurve creditCurve, CdsPriceType cleanOrDirty) {

      double pv = 0.0;
      for (int i = 0; i < _nPayments; i++) {
        CdsCoupon c = _cds.getCoupon(i);
        double q = creditCurve.getDiscountFactor(c.getEffEnd());
        pv += c.getYearFrac() * _paymentDF[i] * q;
      }

      if (_cds.isPayAccOnDefault()) {
        double accPV = 0.0;
        for (int i = 0; i < _nPayments; i++) {
          accPV += calculateSinglePeriodAccrualOnDefault(i, creditCurve);
        }
        pv += accPV;
      }

      pv /= _valuationDF;

      if (cleanOrDirty == CdsPriceType.CLEAN) {
        pv -= _cds.getAccruedYearFraction();
      }
      return pv;
    }

    private double calculateSinglePeriodAccrualOnDefault(int paymentIndex, IsdaCompliantCreditCurve creditCurve) {

      double[] knots = _premLegIntPoints[paymentIndex];
      if (knots == null) {
        return 0.0;
      }
      double[] df = _premDF[paymentIndex];
      double[] deltaT = _premDt[paymentIndex];
      double[] rt = _rt[paymentIndex];
      double accRate = _accRate[paymentIndex];
      double accStart = _offsetAccStart[paymentIndex];

      double t = knots[0];
      double ht0 = creditCurve.getRT(t);
      double rt0 = rt[0];
      double b0 = df[0] * Math.exp(-ht0);

      double t0 = t - accStart + _omega;
      double pv = 0.0;
      int nItems = knots.length;
      for (int j = 1; j < nItems; ++j) {
        t = knots[j];
        double ht1 = creditCurve.getRT(t);
        double rt1 = rt[j];
        double b1 = df[j] * Math.exp(-ht1);
        double dt = deltaT[j - 1];

        double dht = ht1 - ht0;
        double drt = rt1 - rt0;
        double dhrt = dht + drt + 1e-50; // to keep consistent with ISDA c code

        double tPV;
        if (getAccOnDefaultFormula() == AccrualOnDefaultFormulae.MARKIT_FIX) {
          if (Math.abs(dhrt) < 1e-5) {
            tPV = dht * dt * b0 * epsilonP(-dhrt);
          } else {
            tPV = dht * dt / dhrt * ((b0 - b1) / dhrt - b1);
          }
        } else {
          double t1 = t - accStart + _omega;
          if (Math.abs(dhrt) < 1e-5) {
            tPV = dht * b0 * (t0 * epsilon(-dhrt) + dt * epsilonP(-dhrt));
          } else {
            tPV = dht / dhrt * (t0 * b0 - t1 * b1 + dt / dhrt * (b0 - b1));
          }
          t0 = t1;
        }
        pv += tPV;
        ht0 = ht1;
        rt0 = rt1;
        b0 = b1;

      }
      return accRate * pv;
    }

    public double protectionLeg(IsdaCompliantCreditCurve creditCurve) {

      double ht0 = creditCurve.getRT(_proLegIntPoints[0]);
      double rt0 = _proYieldCurveRT[0];
      double b0 = _proDF[0] * Math.exp(-ht0);

      double pv = 0.0;

      for (int i = 1; i < _nProPoints; ++i) {
        double ht1 = creditCurve.getRT(_proLegIntPoints[i]);
        double rt1 = _proYieldCurveRT[i];
        double b1 = _proDF[i] * Math.exp(-ht1);
        double dht = ht1 - ht0;
        double drt = rt1 - rt0;
        double dhrt = dht + drt;

        // this is equivalent to the ISDA code without explicitly calculating the time step - it also handles the limit
        double dPV;
        if (Math.abs(dhrt) < 1e-5) {
          dPV = dht * b0 * epsilon(-dhrt);
        } else {
          dPV = (b0 - b1) * dht / dhrt;
        }
        pv += dPV;
        ht0 = ht1;
        rt0 = rt1;
        b0 = b1;
      }
      pv *= _lgdDF; // multiply by LGD and adjust to valuation date

      return pv;
    }

  }

}
