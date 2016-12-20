/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import static com.opengamma.strata.math.impl.util.Epsilon.epsilon;
import static com.opengamma.strata.math.impl.util.Epsilon.epsilonP;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.math.MathException;
import com.opengamma.strata.math.impl.rootfinding.BracketRoot;
import com.opengamma.strata.math.impl.rootfinding.BrentSingleRootFinder;
import com.opengamma.strata.math.impl.rootfinding.RealSingleRootFinder;
import com.opengamma.strata.pricer.common.PriceType;
import com.opengamma.strata.product.credit.CreditCouponPaymentPeriod;
import com.opengamma.strata.product.credit.ResolvedCds;
import com.opengamma.strata.product.credit.ResolvedCdsTrade;

/**
 * Fast credit curve calibrator.
 * <p>
 * This is a fast bootstrapper for the credit curve that is consistent with ISDA 
 * in that it will produce the same curve from the same inputs (up to numerical round-off).
 * <p>
 * The CDS pricer is internally implemented for fast calibration.
 */
public final class FastCreditCurveCalibrator extends IsdaCompliantCreditCurveCalibrator {

  /**
   * The default implementation.
   */
  public static final FastCreditCurveCalibrator DEFAULT = new FastCreditCurveCalibrator();

  /**
   * The root bracket finder.
   */
  private static final BracketRoot BRACKETER = new BracketRoot();
  /**
   * The root finder.
   */
  private static final RealSingleRootFinder ROOTFINDER = new BrentSingleRootFinder();

  //-------------------------------------------------------------------------
  /**
   * Constructs a default credit curve builder. 
   * <p>
   * The original ISDA accrual-on-default formula (version 1.8.2 and lower) and the arbitrage handling 'ignore' are used.
   */
  public FastCreditCurveCalibrator() {
    super();
  }

  /**
   * Constructs a credit curve builder with the accrual-on-default formula specified.
   * <p>
   * The arbitrage handling 'ignore' is used. 
   * 
   * @param formula  the accrual-on-default formula
   */
  public FastCreditCurveCalibrator(AccrualOnDefaultFormula formula) {
    super(formula);
  }

  /**
   * Constructs a credit curve builder with accrual-on-default formula and arbitrage handing specified.
   * 
   * @param formula  the accrual on default formulae
   * @param arbHandling  the arbitrage handling
   */
  public FastCreditCurveCalibrator(AccrualOnDefaultFormula formula, ArbitrageHandling arbHandling) {
    super(formula, arbHandling);
  }

  @Override
  NodalCurve calibrate(
      List<ResolvedCdsTrade> calibrationCDSs,
      DoubleArray flactionalSpreads,
      DoubleArray pointsUpfront,
      CurveName name,
      LocalDate valuationDate,
      CreditDiscountFactors discountFactors,
      RecoveryRates recoveryRates,
      ReferenceData refData) {

    int n = calibrationCDSs.size();
    double[] guess = new double[n];
    double[] t = new double[n];
    double[] lgd = new double[n];
    for (int i = 0; i < n; i++) {
      LocalDate endDate = calibrationCDSs.get(i).getProduct().getProtectionEndDate();
      t[i] = discountFactors.relativeYearFraction(endDate);
      lgd[i] = 1d - recoveryRates.recoveryRate(endDate);
      guess[i] = (flactionalSpreads.get(i) + pointsUpfront.get(i) / t[i]) / lgd[i];
    }
    DoubleArray times = DoubleArray.ofUnsafe(t);
    CurveMetadata baseMetadata = DefaultCurveMetadata.builder()
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .curveName(name)
        .dayCount(discountFactors.getDayCount())
        .build();
    NodalCurve creditCurve = n == 1 ? ConstantNodalCurve.of(baseMetadata, t[0], guess[0])
        : InterpolatedNodalCurve.of(
            baseMetadata,
            times,
            DoubleArray.ofUnsafe(guess),
            CurveInterpolators.PRODUCT_LINEAR,
            CurveExtrapolators.FLAT,
            CurveExtrapolators.PRODUCT_LINEAR);

    for (int i = 0; i < n; i++) {
      ResolvedCds cds = calibrationCDSs.get(i).getProduct();
      LocalDate stepinDate = cds.getStepinDateOffset().adjust(valuationDate, refData);
      LocalDate effectiveStartDate = cds.calculateEffectiveStartDate(stepinDate);
      LocalDate settlementDate = calibrationCDSs.get(i).getInfo().getSettlementDate()
          .orElse(cds.getSettlementDateOffset().adjust(valuationDate, refData));
      double accrued = cds.accruedYearFraction(stepinDate);

      Pricer pricer = new Pricer(cds, discountFactors, times, flactionalSpreads.get(i), pointsUpfront.get(i), lgd[i], stepinDate,
          effectiveStartDate, settlementDate, accrued);
      Function<Double, Double> func = pricer.getPointFunction(i, creditCurve);

      switch (getArbHanding()) {
        case IGNORE: {
          try {
            double[] bracket = BRACKETER.getBracketedPoints(
                func, 0.8 * guess[i], 1.25 * guess[i], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            double zeroRate = bracket[0] > bracket[1] ? ROOTFINDER.getRoot(func, bracket[1], bracket[0])
                : ROOTFINDER.getRoot(func, bracket[0], bracket[1]); //Negative guess handled
            creditCurve = creditCurve.withParameter(i, zeroRate);
          } catch (final MathException e) { //handling bracketing failure due to small survival probability
            if (Math.abs(func.apply(creditCurve.getYValues().get(i - 1))) < 1.e-12) {
              creditCurve = creditCurve.withParameter(i, creditCurve.getYValues().get(i - 1));
            } else {
              throw new MathException(e);
            }
          }
          break;
        }
        case FAIL: {
          final double minValue = i == 0 ? 0d
              : creditCurve.getYValues().get(i - 1) * creditCurve.getXValues().get(i - 1) / creditCurve.getXValues().get(i);
          if (i > 0 && func.apply(minValue) > 0.0) { //can never fail on the first spread
            final StringBuilder msg = new StringBuilder();
            if (pointsUpfront.get(i) == 0.0) {
              msg.append("The par spread of " + flactionalSpreads.get(i) + " at index " + i);
            } else {
              msg.append(
                  "The premium of " + flactionalSpreads.get(i) +
                      "and points up-front of " + pointsUpfront.get(i) + " at index " + i);
            }
            msg.append(" is an arbitrage; cannot fit a curve with positive forward hazard rate. ");
            throw new IllegalArgumentException(msg.toString());
          }
          guess[i] = Math.max(minValue, guess[i]);
          double[] bracket = BRACKETER.getBracketedPoints(func, guess[i], 1.2 * guess[i], minValue, Double.POSITIVE_INFINITY);
          double zeroRate = ROOTFINDER.getRoot(func, bracket[0], bracket[1]);
          creditCurve = creditCurve.withParameter(i, zeroRate);
          break;
        }
        case ZERO_HAZARD_RATE: {
          final double minValue = i == 0 ? 0.0
              : creditCurve.getYValues().get(i - 1) * creditCurve.getXValues().get(i - 1) / creditCurve.getXValues().get(i);
          if (i > 0 && func.apply(minValue) > 0.0) { //can never fail on the first spread
            creditCurve = creditCurve.withParameter(i, minValue);
          } else {
            guess[i] = Math.max(minValue, guess[i]);
            final double[] bracket =
                BRACKETER.getBracketedPoints(func, guess[i], 1.2 * guess[i], minValue, Double.POSITIVE_INFINITY);
            final double zeroRate = ROOTFINDER.getRoot(func, bracket[0], bracket[1]);
            creditCurve = creditCurve.withParameter(i, zeroRate);
          }
          break;
        }
        default:
          throw new IllegalArgumentException("unknown case " + getArbHanding());
      }
    }
    return creditCurve;
  }

  /* Prices the CDS */
  protected class Pricer {

    private final ResolvedCds cds;
    private final double lgdDF;
    private final double valuationDF;
    private final double fracSpread;
    private final double puf;
    // protection leg
    private final int nProPoints;
    private final double[] proLegIntPoints;
    private final double[] proYieldCurveRT;
    private final double[] proDF;
    // premium leg
    private final int nPayments;
    private final double[] paymentDF;
    private final double[][] premLegIntPoints;
    private final double[][] premDF;
    private final double[][] rt;
    private final double[][] premDt;
    private final double[] accRate;
    private final double[] offsetAccStart;
    private final double[] offsetAccEnd;

    private final double accYearFraction;
    private final double productEffectiveStart;
    private final int startPeriodIndex;

    public Pricer(ResolvedCds nodeCds, CreditDiscountFactors yieldCurve, DoubleArray creditCurveKnots, double fractionalSpread,
        double pointsUpfront, double lgd, LocalDate stepinDate, LocalDate effectiveStartDate, LocalDate settlementDate,
        double accruedYearFraction) {

      accYearFraction = accruedYearFraction;
      cds = nodeCds;
      fracSpread = fractionalSpread;
      puf = pointsUpfront;
      productEffectiveStart = yieldCurve.relativeYearFraction(effectiveStartDate);
      double protectionEnd = yieldCurve.relativeYearFraction(cds.getProtectionEndDate());
      // protection leg
      proLegIntPoints = DoublesScheduleGenerator.getIntegrationsPoints(
          productEffectiveStart,
          protectionEnd,
          yieldCurve.getParameterKeys(), creditCurveKnots).toArray();
      nProPoints = proLegIntPoints.length;
      valuationDF = yieldCurve.discountFactor(settlementDate);
      lgdDF = lgd / valuationDF;
      proYieldCurveRT = new double[nProPoints];
      proDF = new double[nProPoints];
      for (int i = 0; i < nProPoints; i++) {
        proYieldCurveRT[i] = yieldCurve.zeroRate(proLegIntPoints[i]) * proLegIntPoints[i];
        proDF[i] = Math.exp(-proYieldCurveRT[i]);
      }
      // premium leg
      nPayments = cds.getPaymentPeriods().size();
      paymentDF = new double[nPayments];
      int indexTmp = -1;
      for (int i = 0; i < nPayments; i++) {
        if (stepinDate.isBefore(cds.getPaymentPeriods().get(i).getEndDate())) {
          paymentDF[i] = yieldCurve.discountFactor(cds.getPaymentPeriods().get(i).getPaymentDate());
        } else {
          indexTmp = i;
        }
      }
      startPeriodIndex = indexTmp + 1;
      // accrual on default
      if (cds.getPaymentOnDefault().isAccruedInterest()) {
        LocalDate tmp = nPayments == 1 ? effectiveStartDate : cds.getAccrualStartDate();
        DoubleArray integrationSchedule =
            DoublesScheduleGenerator.getIntegrationsPoints(
                yieldCurve.relativeYearFraction(tmp),
                protectionEnd,
                yieldCurve.getParameterKeys(),
                creditCurveKnots);
        accRate = new double[nPayments];
        offsetAccStart = new double[nPayments];
        offsetAccEnd = new double[nPayments];
        premLegIntPoints = new double[nPayments][];
        premDF = new double[nPayments][];
        rt = new double[nPayments][];
        premDt = new double[nPayments][];
        for (int i = startPeriodIndex; i < nPayments; i++) {
          CreditCouponPaymentPeriod coupon = cds.getPaymentPeriods().get(i);
          offsetAccStart[i] = yieldCurve.relativeYearFraction(coupon.getEffectiveStartDate());
          offsetAccEnd[i] = yieldCurve.relativeYearFraction(coupon.getEffectiveEndDate());
          accRate[i] = coupon.getYearFraction() /
              yieldCurve.getDayCount().relativeYearFraction(coupon.getStartDate(), coupon.getEndDate());
          double start = Math.max(productEffectiveStart, offsetAccStart[i]);
          if (start >= offsetAccEnd[i]) {
            continue;
          }
          premLegIntPoints[i] = DoublesScheduleGenerator.truncateSetInclusive(
              start,
              offsetAccEnd[i],
              integrationSchedule).toArray();
          int n = premLegIntPoints[i].length;
          rt[i] = new double[n];
          premDF[i] = new double[n];
          for (int k = 0; k < n; k++) {
            rt[i][k] = yieldCurve.zeroRate(premLegIntPoints[i][k]) * premLegIntPoints[i][k];
            premDF[i][k] = Math.exp(-rt[i][k]);
          }
          premDt[i] = new double[n - 1];

          for (int k = 1; k < n; k++) {
            final double dt = premLegIntPoints[i][k] - premLegIntPoints[i][k - 1];
            premDt[i][k - 1] = dt;
          }
        }
      } else {
        accRate = null;
        offsetAccStart = null;
        offsetAccEnd = null;
        premDF = null;
        premDt = null;
        rt = null;
        premLegIntPoints = null;
      }
    }

    public Function<Double, Double> getPointFunction(int index, NodalCurve creditCurve) {
      return new Function<Double, Double>() {
        @Override
        public Double apply(Double x) {
          NodalCurve cc = creditCurve.withParameter(index, x);
          double rpv01 = rpv01(cc, PriceType.CLEAN);
          double pro = protectionLeg(cc);
          return pro - fracSpread * rpv01 - puf;
        }
      };
    }

    public double rpv01(NodalCurve creditCurve, PriceType cleanOrDirty) {
      double pv = 0.0;
      for (int i = startPeriodIndex; i < nPayments; i++) {
        CreditCouponPaymentPeriod coupon = cds.getPaymentPeriods().get(i);
        double yc = offsetAccEnd[i];
        double q = Math.exp(-creditCurve.yValue(yc) * yc);
        pv += coupon.getYearFraction() * paymentDF[i] * q;
      }

      if (cds.getPaymentOnDefault().isAccruedInterest()) {
        double accPV = 0.0;
        for (int i = startPeriodIndex; i < nPayments; i++) {
          accPV += calculateSinglePeriodAccrualOnDefault(i, creditCurve);
        }
        pv += accPV;
      }
      pv /= valuationDF;
      if (cleanOrDirty == PriceType.CLEAN) {
        pv -= accYearFraction;
      }
      return pv;
    }

    private double calculateSinglePeriodAccrualOnDefault(int paymentIndex, NodalCurve creditCurve) {
      double[] knots = premLegIntPoints[paymentIndex];
      if (knots == null) {
        return 0d;
      }
      double[] df = premDF[paymentIndex];
      double[] deltaT = premDt[paymentIndex];
      double[] rtCurrent = rt[paymentIndex];
      double accRateCurrent = accRate[paymentIndex];
      double accStart = offsetAccStart[paymentIndex];
      double t = knots[0];
      double ht0 = creditCurve.yValue(t) * t;
      double rt0 = rtCurrent[0];
      double b0 = df[0] * Math.exp(-ht0);
      double t0 = t - accStart + getAccOnDefaultFormula().getOmega();
      double pv = 0d;
      int nItems = knots.length;
      for (int j = 1; j < nItems; ++j) {
        t = knots[j];
        double ht1 = creditCurve.yValue(t) * t;
        double rt1 = rtCurrent[j];
        double b1 = df[j] * Math.exp(-ht1);
        double dt = deltaT[j - 1];
        double dht = ht1 - ht0;
        double drt = rt1 - rt0;
        double dhrt = dht + drt + 1e-50; // to keep consistent with ISDA c code
        double tPV;
        if (getAccOnDefaultFormula() == AccrualOnDefaultFormula.MARKIT_FIX) {
          if (Math.abs(dhrt) < 1e-5) {
            tPV = dht * dt * b0 * epsilonP(-dhrt);
          } else {
            tPV = dht * dt / dhrt * ((b0 - b1) / dhrt - b1);
          }
        } else {
          double t1 = t - accStart + getAccOnDefaultFormula().getOmega();
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
      return accRateCurrent * pv;
    }

    public double protectionLeg(NodalCurve creditCurve) {
      double ht0 = creditCurve.yValue(proLegIntPoints[0]) * proLegIntPoints[0];
      double rt0 = proYieldCurveRT[0];
      double b0 = proDF[0] * Math.exp(-ht0);
      double pv = 0d;
      for (int i = 1; i < nProPoints; ++i) {
        double ht1 = creditCurve.yValue(proLegIntPoints[i]) * proLegIntPoints[i];
        double rt1 = proYieldCurveRT[i];
        double b1 = proDF[i] * Math.exp(-ht1);
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
      pv *= lgdDF; // multiply by LGD and adjust to valuation date
      return pv;
    }
  }

}
