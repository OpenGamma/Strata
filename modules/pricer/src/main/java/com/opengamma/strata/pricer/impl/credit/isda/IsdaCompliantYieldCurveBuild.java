/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.rootfinding.BracketRoot;
import com.opengamma.strata.math.impl.rootfinding.NewtonRaphsonSingleRootFinder;

/**
 *
 */
public class IsdaCompliantYieldCurveBuild {
  private static final DayCount ACT_365 = DayCounts.ACT_365F;
  private static final HolidayCalendar DEFAULT_CALENDAR = HolidayCalendars.SAT_SUN;

  private static final NewtonRaphsonSingleRootFinder ROOTFINDER = new NewtonRaphsonSingleRootFinder(); // new BrentSingleRootFinder(); // TODO get gradient and use Newton
  private static final BracketRoot BRACKETER = new BracketRoot();

  private final double _offset; //if curve spot date is not the same as CDS trade date
  private final double[] _t; //yieldCurve nodes
  private final double[] _mmYF; //money market year fractions
  private final BasicFixedLeg[] _swaps;
  private final IsdaInstrumentTypes[] _instrumentTypes;

  //************************************************************************************************************************
  // static curve builders 
  //************************************************************************************************************************

  /**
   * Build a ISDA-Compliant yield curve (i.e. one with piecewise flat forward rate) from money market rates and par swap rates.
   * Note if cdsTradeDate (today) is different from spotDate, the curve is adjusted accordingly
   * @param cdsTradeDate The 'observation' date
   * @param spotDate The spot date of the instruments
   * @param instrumentTypes List of instruments - these are  MoneyMarket or Swap
   * @param tenors The length of the instruments (e.g. a 5y swap would be  Period.ofYears(5))
   * @param rates the par rates (as fractions) of the instruments
   * @param moneyMarketDCC The day count convention for money market instruments
   * @param swapDCC The day count convention for swap fixed payments
   * @param swapInterval Time between fixed payments (e.g. 3M fixed is Period.ofMonths(3))
   * @param curveDCC The day count convention used by the yield/discount curve - normally this is ACT/365
   * @param convention Specification for the handling of  non-business days
   * @return A yield curve observed from today
   */
  public static IsdaCompliantYieldCurve build(
      LocalDate cdsTradeDate,
      LocalDate spotDate,
      IsdaInstrumentTypes[] instrumentTypes,
      Period[] tenors,
      double[] rates,
      DayCount moneyMarketDCC,
      DayCount swapDCC,
      Period swapInterval,
      DayCount curveDCC,
      BusinessDayConvention convention) {

    IsdaCompliantYieldCurveBuild builder = new IsdaCompliantYieldCurveBuild(cdsTradeDate, spotDate, instrumentTypes,
        tenors, moneyMarketDCC, swapDCC, swapInterval, curveDCC, convention);
    return builder.build(rates);
  }

  /**
   * Build a ISDA-Compliant yield curve (i.e. one with piecewise flat forward rate) from money market rates and par swap rates.
   * @param spotDate The spot date of the instruments (note is curve is assumed to be observed from this date)
   * @param instrumentTypes List of instruments - these are  MoneyMarket or Swap
   * @param tenors The length of the instruments (e.g. a 5y swap would be  Period.ofYears(5))
   * @param rates the par rates (as fractions) of the instruments
   * @param moneyMarketDCC The day count convention for money market instruments
   * @param swapDCC The day count convention for swap fixed payments
   * @param swapInterval Time between fixed payments (e.g. 3M fixed is Period.ofMonths(3))
   * @param convention Specification for the handling of  non-business days
   * @return A yield curve
   */
  public static IsdaCompliantYieldCurve build(LocalDate spotDate,
      IsdaInstrumentTypes[] instrumentTypes,
      Period[] tenors,
      double[] rates,
      DayCount moneyMarketDCC,
      DayCount swapDCC,
      Period swapInterval,
      BusinessDayConvention convention) {

    IsdaCompliantYieldCurveBuild builder = new IsdaCompliantYieldCurveBuild(spotDate, instrumentTypes, tenors,
        moneyMarketDCC, swapDCC, swapInterval, convention);
    return builder.build(rates);
  }

  //************************************************************************************************************************
  // constructors 
  //************************************************************************************************************************

  /**
   * Set up a yield curve builder (run the method build, to build the curve). Note, the cds trade date is taken as the spot date;
   * a weekend only calendar is used.
   * @param spotDate The spot date for the instruments used to build the yield curve 
   * @param instrumentTypes  The instrument type for each curve node 
   * @param tenors The tenors of the instruments 
   * @param moneyMarketDCC The day-count-convention for money market (spot libor) instruments (accrual)
   * @param swapDCC The day-count-convention for swaps (accrual)  
   * @param swapInterval The payment interval for the swaps
   * @param convention Specification for the handling of  non-business days
   */
  public IsdaCompliantYieldCurveBuild(
      LocalDate spotDate,
      IsdaInstrumentTypes[] instrumentTypes,
      Period[] tenors,
      DayCount moneyMarketDCC,
      DayCount swapDCC,
      Period swapInterval,
      BusinessDayConvention convention) {

    this(spotDate, spotDate, instrumentTypes, tenors, moneyMarketDCC, swapDCC, swapInterval, ACT_365, convention);
  }

  /**
   * Set up a yield curve builder (run the method build, to build the curve). Note, a weekend only calendar is used.
   * @param cdsTradeDate The trade date (aka today)
   * @param spotDate The spot date for the instruments used to build the yield curve 
   * @param instrumentTypes  The instrument type for each curve node 
   * @param tenors The tenors of the instruments 
   * @param moneyMarketDCC The day-count-convention for money market (spot libor) instruments (accrual)
   * @param swapDCC The day-count-convention for swaps (accrual)  
   * @param swapInterval The payment interval for the swaps
   * @param curveDCC The day-count-convention used for the curve 
   * @param convention Specification for the handling of  non-business days
   */
  public IsdaCompliantYieldCurveBuild(
      LocalDate cdsTradeDate,
      LocalDate spotDate,
      IsdaInstrumentTypes[] instrumentTypes,
      Period[] tenors, DayCount moneyMarketDCC,
      DayCount swapDCC,
      Period swapInterval,
      DayCount curveDCC,
      BusinessDayConvention convention) {

    this(cdsTradeDate, spotDate, instrumentTypes, tenors, moneyMarketDCC, swapDCC, swapInterval, curveDCC, convention,
        DEFAULT_CALENDAR);
  }

  /**
   * Set up a yield curve builder (run the method build, to build the curve). 
   * @param cdsTradeDate The trade date (aka today)
   * @param spotDate The spot date for the instruments used to build the yield curve 
   * @param instrumentTypes  The instrument type for each curve node 
   * @param tenors The tenors of the instruments 
   * @param moneyMarketDCC The day-count-convention for money market (spot libor) instruments (accrual)
   * @param swapDCC The day-count-convention for swaps (accrual)  
   * @param swapInterval The payment interval for the swaps
   * @param curveDCC The day-count-convention used for the curve 
   * @param convention Specification for the handling of  non-business days
   * @param calendar HolidayCalendar defining what is a non-business day
   */
  public IsdaCompliantYieldCurveBuild(
      LocalDate cdsTradeDate,
      LocalDate spotDate,
      IsdaInstrumentTypes[] instrumentTypes,
      Period[] tenors,
      DayCount moneyMarketDCC,
      DayCount swapDCC,
      Period swapInterval,
      DayCount curveDCC,
      BusinessDayConvention convention,
      HolidayCalendar calendar) {

    ArgChecker.notNull(spotDate, "spotDate");
    ArgChecker.noNulls(instrumentTypes, "instrumentTypes");
    ArgChecker.noNulls(tenors, "tenors");
    ArgChecker.notNull(moneyMarketDCC, "moneyMarketDCC");
    ArgChecker.notNull(swapDCC, "swapDCC");
    ArgChecker.notNull(swapInterval, "swapInterval");
    ArgChecker.notNull(curveDCC, "curveDCC");
    ArgChecker.notNull(convention, "convention");
    int n = tenors.length;
    ArgChecker.isTrue(n == instrumentTypes.length, "{} tenors given, but {} instrumentTypes", n, instrumentTypes.length);

    LocalDate[] matDates = new LocalDate[n];
    LocalDate[] adjMatDates = new LocalDate[n];
    for (int i = 0; i < n; i++) {
      matDates[i] = spotDate.plus(tenors[i]);
      if (i == 0) {
        ArgChecker.isTrue(matDates[0].isAfter(spotDate), "first tenor zero");
      } else {
        ArgChecker.isTrue(matDates[i].isAfter(matDates[i - 1]), "tenors are not assending");
      }
      adjMatDates[i] = convention.adjust(matDates[i], calendar);
    }

    _t = new double[n];
    _instrumentTypes = instrumentTypes;
    int nMM = 0;
    for (int i = 0; i < n; i++) {
      _t[i] = curveDCC.yearFraction(spotDate, adjMatDates[i]);
      if (_instrumentTypes[i] == IsdaInstrumentTypes.MONEY_MARKET) {
        nMM++;
      }
    }
    int nSwap = n - nMM;
    _mmYF = new double[nMM];
    _swaps = new BasicFixedLeg[nSwap];
    int mmCount = 0;
    int swapCount = 0;
    for (int i = 0; i < n; i++) {
      if (instrumentTypes[i] == IsdaInstrumentTypes.MONEY_MARKET) {
        // TODO in ISDA code money market instruments of less than 21 days have special treatment
        _mmYF[mmCount++] = moneyMarketDCC.yearFraction(spotDate, adjMatDates[i]);
      } else {
        _swaps[swapCount++] = new BasicFixedLeg(spotDate, matDates[i], swapInterval, swapDCC, curveDCC, convention, calendar);
      }
    }
    _offset = cdsTradeDate.isAfter(spotDate) ? curveDCC.yearFraction(spotDate, cdsTradeDate) : -curveDCC.yearFraction(
        cdsTradeDate, spotDate);
  }

  /**
   * build a yield curve 
   * @param rates The par rates of the instruments (as fractions) 
   * @return a yield curve 
   */
  public IsdaCompliantYieldCurve build(double[] rates) {
    ArgChecker.notEmpty(rates, "rates");
    int n = _instrumentTypes.length;
    ArgChecker.isTrue(n == rates.length, "expecting " + n + " rates, given " + rates.length);

    // set up curve with best guess rates
    IsdaCompliantCurve curve = new IsdaCompliantCurve(_t, rates);
    // loop over the instruments and adjust the curve to price each in turn
    int mmCount = 0;
    int swapCount = 0;
    for (int i = 0; i < n; i++) {
      if (_instrumentTypes[i] == IsdaInstrumentTypes.MONEY_MARKET) {
        // TODO in ISDA code money market instruments of less than 21 days have special treatment
        double z = 1.0 / (1 + rates[i] * _mmYF[mmCount++]);
        curve = curve.withDiscountFactor(z, i);
      } else {
        curve = fitSwap(i, _swaps[swapCount++], curve, rates[i]);
      }
    }

    IsdaCompliantYieldCurve baseCurve = new IsdaCompliantYieldCurve(curve);
    if (_offset == 0.0) {
      return baseCurve;
    }
    return baseCurve.withOffset(_offset);
  }

  private IsdaCompliantCurve fitSwap(int curveIndex, BasicFixedLeg swap, IsdaCompliantCurve curve, double swapRate) {

    int nPayments = swap.getNumPayments();
    int nNodes = curve.getNumberOfKnots();
    double t1 = curveIndex == 0 ? 0.0 : curve.getTimeAtIndex(curveIndex - 1);
    double t2 = curveIndex == nNodes - 1 ? Double.POSITIVE_INFINITY : curve.getTimeAtIndex(curveIndex + 1);

    double temp = 0;
    double temp2 = 0;
    int i1 = 0;
    int i2 = nPayments;
    double[] paymentAmounts = new double[nPayments];
    for (int i = 0; i < nPayments; i++) {
      double t = swap.getPaymentTime(i);
      paymentAmounts[i] = swap.getPaymentAmounts(i, swapRate);
      if (t <= t1) {
        double df = curve.getDiscountFactor(t);
        temp += paymentAmounts[i] * df;
        temp2 -= paymentAmounts[i] * curve.getSingleNodeDiscountFactorSensitivity(t, curveIndex);
        i1++;
      } else if (t >= t2) {
        double df = curve.getDiscountFactor(t);
        temp += paymentAmounts[i] * df;
        temp2 += paymentAmounts[i] * curve.getSingleNodeDiscountFactorSensitivity(t, curveIndex);
        i2--;
      }
    }
    double cachedValues = temp;
    double cachedSense = temp2;
    int index1 = i1;
    int index2 = i2;

    Function<Double, Double> func = new Function<Double, Double>() {

      @Override
      public Double apply(Double x) {
        IsdaCompliantCurve tempCurve = curve.withRate(x, curveIndex);
        double sum = 1.0 - cachedValues; // Floating leg at par
        for (int i = index1; i < index2; i++) {
          double t = swap.getPaymentTime(i);
          sum -= paymentAmounts[i] * tempCurve.getDiscountFactor(t);
        }
        return sum;
      }
    };

    Function<Double, Double> grad = new Function<Double, Double>() {

      @Override
      public Double apply(Double x) {
        IsdaCompliantCurve tempCurve = curve.withRate(x, curveIndex);
        double sum = cachedSense;
        for (int i = index1; i < index2; i++) {
          double t = swap.getPaymentTime(i);
          // TODO have two looks ups for the same time - could have a specialist function in ISDACompliantCurve
          sum -= swap.getPaymentAmounts(i, swapRate) * tempCurve.getSingleNodeDiscountFactorSensitivity(t, curveIndex);
        }
        return sum;
      }

    };

    double guess = curve.getZeroRateAtIndex(curveIndex);
    if (guess == 0.0 && func.apply(guess) == 0.0) {
      return curve;
    }
    double[] bracket = BRACKETER.getBracketedPoints(func, 0.8 * guess, 1.25 * guess, 0, Double.POSITIVE_INFINITY);
    double r = ROOTFINDER.getRoot(func, grad, bracket[0], bracket[1]);
    return curve.withRate(r, curveIndex);
  }

  /**
   * very crude swap fixed leg description. TODO modify to match ISDA <p>
   * So that the floating leg can be taken as having a value of 1.0, rather than the text book 1 - P(T) for LIBOR discounting,
   * we add 1.0 to the payment, which is financially equivalent
   */
  private class BasicFixedLeg {
    private final int _nPayments;
    private final double[] _swapPaymentTimes;
    private final double[] _yearFraction;

    public BasicFixedLeg(
        LocalDate spotDate,
        LocalDate mat,
        Period swapInterval,
        DayCount swapDCC,
        DayCount curveDCC,
        BusinessDayConvention convention,
        HolidayCalendar calendar) {

      ArgChecker.isFalse(swapInterval.getDays() > 0, "swap interval must be in months or years");

      List<LocalDate> list = new ArrayList<>();
      LocalDate tDate = mat;
      int step = 1;
      while (tDate.isAfter(spotDate)) {
        list.add(tDate);
        tDate = mat.minus(swapInterval.multipliedBy(step++));
      }

      // remove spotDate from list, if it ends up there
      list.remove(spotDate);

      _nPayments = list.size();
      _swapPaymentTimes = new double[_nPayments];
      _yearFraction = new double[_nPayments];

      LocalDate prev = spotDate;
      int j = _nPayments - 1;
      for (int i = 0; i < _nPayments; i++, j--) {
        LocalDate current = list.get(j);
        LocalDate adjCurr = convention.adjust(current, calendar);
        _yearFraction[i] = swapDCC.yearFraction(prev, adjCurr);
        _swapPaymentTimes[i] = curveDCC.yearFraction(spotDate, adjCurr); // Payment times always good business days
        prev = adjCurr;
      }
      //  _paymentAmounts[_nPayments - 1] += 1.0; // see Javadocs comment
    }

    public int getNumPayments() {
      return _nPayments;
    }

    public double getPaymentAmounts(int index, double rate) {
      return index == _nPayments - 1 ? 1 + rate * _yearFraction[index] : rate * _yearFraction[index];
    }

    public double getPaymentTime(int index) {
      return _swapPaymentTimes[index];
    }

  }

}
