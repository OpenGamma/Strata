/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import java.time.LocalDate;
import java.time.Period;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.rootfinding.BracketRoot;
import com.opengamma.strata.math.impl.rootfinding.BrentSingleRootFinder;
import com.opengamma.strata.math.impl.rootfinding.RealSingleRootFinder;

/**
 * This should be viewed as "proof of concept" code, since it used the code that has date logic mixed with the analytics (this was to
 * mimic the structure of the ISDA c code). This should <b>not</b> be used for production credit (hazard) curve calibration/bootstrapping,
 * ISDACompliantCreditCurveCalibrator should be used.
 *
 */
public class IsdaCompliantCurveCalibrator {

  private static final DayCount ACT_365 = DayCounts.ACT_365F;

  private static final BracketRoot BRACKER = new BracketRoot();
  private static final RealSingleRootFinder ROOTFINDER = new BrentSingleRootFinder();
  private static final IsdaCompliantPresentValueCreditDefaultSwap PRICER = new IsdaCompliantPresentValueCreditDefaultSwap();

  public IsdaCompliantDateCreditCurve calibrateHazardCurve(
      LocalDate today,
      LocalDate stepinDate,
      LocalDate valueDate,
      LocalDate startDate,
      LocalDate[] endDates,
      double[] couponRates,
      boolean payAccOnDefault,
      Period tenor,
      CdsStubType stubType,
      boolean protectStart,
      IsdaCompliantDateYieldCurve yieldCurve,
      double recoveryRate) {

    ArgChecker.notNull(today, "null today");
    ArgChecker.notNull(stepinDate, "null stepinDate");
    ArgChecker.notNull(valueDate, "null valueDate");
    ArgChecker.notNull(startDate, "null startDate");
    ArgChecker.noNulls(endDates, "null endDates");
    ArgChecker.notEmpty(couponRates, "no or null couponRates");
    ArgChecker.notNull(tenor, "null tenor");
    ArgChecker.notNull(stubType, "null stubType");
    ArgChecker.notNull(yieldCurve, "null yieldCurve");
    ArgChecker.inRange(recoveryRate, 0d, 1d, "recoveryRate");
    ArgChecker.isFalse(valueDate.isBefore(today), "Require valueDate >= today");
    ArgChecker.isFalse(stepinDate.isBefore(today), "Require stepin >= today");

    int n = endDates.length;
    ArgChecker.isTrue(n == couponRates.length, "length of couponRates does not match endDates");

    // use continuous premiums as initial guess
    double[] guess = new double[n];
    double[] t = new double[n];
    double lgd = 1 - recoveryRate;
    for (int i = 0; i < n; i++) {
      guess[i] = couponRates[i] / lgd;
      t[i] = ACT_365.yearFraction(today, endDates[i]);
    }

    IsdaCompliantDateCreditCurve hazardCurve = new IsdaCompliantDateCreditCurve(today, endDates, guess);
    for (int i = 0; i < n; i++) {
      CDSPricer func = new CDSPricer(i, today, stepinDate, valueDate, startDate, endDates[i], couponRates[i], protectStart,
          payAccOnDefault,
          tenor, stubType, recoveryRate, yieldCurve, hazardCurve);
      double[] bracket = BRACKER.getBracketedPoints(func, 0.9 * guess[i], 1.1 * guess[i], 0.0, Double.POSITIVE_INFINITY);
      double zeroRate = ROOTFINDER.getRoot(func, bracket[0], bracket[1]);
      hazardCurve = hazardCurve.withRate(zeroRate, i);
    }

    return hazardCurve;
  }

  private class CDSPricer extends Function1D<Double, Double> {

    private final int _index;
    private final LocalDate _today;
    private final LocalDate _stepinDate;
    private final LocalDate _valueDate;
    private final LocalDate _startDate;
    private final LocalDate _endDate;
    private final double _couponRate;
    private final boolean _protectStart;

    private final boolean _payAccOnDefault;
    private final Period _tenor;
    private final CdsStubType _stubType;
    private final double _rr;

    private IsdaCompliantDateYieldCurve _yieldCurve;
    private IsdaCompliantDateCreditCurve _hazardCurve;

    public CDSPricer(
        int index,
        LocalDate today,
        LocalDate stepinDate,
        LocalDate valueDate,
        LocalDate startDate,
        LocalDate endDate,
        double couponRate,
        boolean protectStart,
        boolean payAccOnDefault,
        Period tenor,
        CdsStubType stubType,
        double rr,
        IsdaCompliantDateYieldCurve yieldCurve,
        IsdaCompliantDateCreditCurve hazardCurve) {

      _index = index;
      _today = today;
      _stepinDate = stepinDate;
      _valueDate = valueDate;
      _startDate = startDate;
      _endDate = endDate;
      _couponRate = couponRate;
      _protectStart = protectStart;
      _payAccOnDefault = payAccOnDefault;
      _tenor = tenor;
      _stubType = stubType;
      _rr = rr;
      _yieldCurve = yieldCurve;
      _hazardCurve = hazardCurve;

    }

    @Override
    public Double evaluate(Double x) {
      // TODO this direct access is unpleasant
      IsdaCompliantDateCreditCurve hazardCurve = _hazardCurve.withRate(x, _index);
      double rpv01 = PRICER.pvPremiumLegPerUnitSpread(_today, _stepinDate, _valueDate, _startDate, _endDate,
          _payAccOnDefault, _tenor, _stubType, _yieldCurve, hazardCurve, _protectStart,
          CdsPriceType.CLEAN);
      double protectLeg = PRICER.calculateProtectionLeg(_today, _stepinDate, _valueDate, _startDate, _endDate, _yieldCurve,
          hazardCurve, _rr, _protectStart);
      double pv = protectLeg - _couponRate * rpv01;
      return pv;
    }
  }

}
