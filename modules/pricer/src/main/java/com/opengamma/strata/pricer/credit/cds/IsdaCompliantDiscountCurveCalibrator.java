package com.opengamma.strata.pricer.credit.cds;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.curve.node.TermDepositCurveNode;
import com.opengamma.strata.math.impl.rootfinding.BracketRoot;
import com.opengamma.strata.math.impl.rootfinding.NewtonRaphsonSingleRootFinder;

public final class IsdaCompliantDiscountCurveCalibrator {

  private static final NewtonRaphsonSingleRootFinder ROOTFINDER = new NewtonRaphsonSingleRootFinder(); // new BrentSingleRootFinder(); // TODO get gradient and use Newton
  private static final BracketRoot BRACKETER = new BracketRoot();
  
  public static final IsdaCompliantDiscountCurveCalibrator DEFAULT = new IsdaCompliantDiscountCurveCalibrator();

  public IsdaCompliantDiscountCurveCalibrator() {
    // TODO set variable for root finders??
  }

  public IsdaCompliantZeroRateDiscountFactors build(LocalDate cdsSpotDate, LocalDate curveSpotDate,
      CurveNode[] curveNode,
      final Period[] tenors,
      final DayCount moneyMarketDCC,
      final DayCount swapDCC, final Period swapInterval, final DayCount curveDCC, final BusinessDayConvention convention,
      final HolidayCalendarId calendar, ReferenceData refData, double[] rates) {

    InnerCurveCalibrator calibrator = new InnerCurveCalibrator(cdsSpotDate, curveSpotDate, curveNode, tenors, moneyMarketDCC,
        swapDCC, swapInterval, curveDCC, convention, calendar, refData);
    return calibrator.build(rates);
  }

  private class InnerCurveCalibrator {

  // TODO create inner calibrator

  private final double _offset; //if curve spot date is not the same as CDS trade date
    private final DoubleArray time; //yieldCurve nodes
    private final double[] _mmYF; //money market year fractions // TODO not need to be stored
    private final BasicFixedLeg[] _swaps; // TODO not need to be stored, derived from node
  private final CurveNode[] _curveNode;
    private final DayCount _curveDcc;  // TODO stored in node
    private final LocalDate _cdsSpotDate;
    private final Currency _currency;  // TODO stored in node

  // TODO tenors must be in CurveNode

    private InnerCurveCalibrator(
        LocalDate cdsSpotDate,
        LocalDate curveSpotDate,
      CurveNode[] curveNode,
        final Period[] tenors, // TODO available in node
        final DayCount moneyMarketDCC, // TODO available in node
        final DayCount swapDCC,  // TODO available in node
        final Period swapInterval, // TODO available in node 
        final DayCount curveDCC,
        final BusinessDayConvention convention, // TODO available in node 
        final HolidayCalendarId calendar, // TODO available in node
        ReferenceData refData) {
//    ArgumentChecker.notNull(spotDate, "spotDate");
//    ArgumentChecker.noNulls(instrumentTypes, "instrumentTypes");
//    ArgumentChecker.noNulls(tenors, "tenors");
//    ArgumentChecker.notNull(moneyMarketDCC, "moneyMarketDCC");
//    ArgumentChecker.notNull(swapDCC, "swapDCC");
//    ArgumentChecker.notNull(swapInterval, "swapInterval");
//    ArgumentChecker.notNull(curveDCC, "curveDCC");
//    ArgumentChecker.notNull(convention, "convention");
    final int n = curveNode.length;
//    ArgumentChecker.isTrue(n == instrumentTypes.length, "{} tenors given, but {} instrumentTypes", n, instrumentTypes.length);

    HolidayCalendar holidayCalendar = calendar.resolve(refData);

    final LocalDate[] matDates = new LocalDate[n];
    final LocalDate[] adjMatDates = new LocalDate[n];
    for (int i = 0; i < n; i++) {
      matDates[i] = curveSpotDate.plus(tenors[i]);
      if (i == 0) {
//        ArgumentChecker.isTrue(matDates[0].isAfter(spotDate), "first tenor zero");
      } else {
//        ArgumentChecker.isTrue(matDates[i].isAfter(matDates[i - 1]), "tenors are not assending");
      }
//        adjMatDates[i] = convention.adjust(matDates[i], holidayCalendar);
//        System.out.println(convention.adjust(matDates[i], holidayCalendar) + "\t" + curveNode[i].date(cdsSpotDate, refData));
        adjMatDates[i] = curveNode[i].date(cdsSpotDate, refData);
    }

      double[] t = new double[n];
    _curveNode = curveNode;
    int nMM = 0;
    for (int i = 0; i < n; i++) {
        t[i] = curveDCC.relativeYearFraction(curveSpotDate, adjMatDates[i]);
      if (_curveNode[i] instanceof TermDepositCurveNode) {
        nMM++;
      }
    }
    final int nSwap = n - nMM;
    _mmYF = new double[nMM];
    _swaps = new BasicFixedLeg[nSwap];
    int mmCount = 0;
    int swapCount = 0;
    for (int i = 0; i < n; i++) {
      if (_curveNode[i] instanceof TermDepositCurveNode) {
        // TODO in ISDA code money market instruments of less than 21 days have special treatment
        _mmYF[mmCount++] = moneyMarketDCC.relativeYearFraction(curveSpotDate, adjMatDates[i]);
      } else {
        _swaps[swapCount++] =
            new BasicFixedLeg(curveSpotDate, matDates[i], swapInterval, swapDCC, curveDCC, convention, holidayCalendar);
      }
    }
    _offset = cdsSpotDate.isAfter(curveSpotDate) ? curveDCC.relativeYearFraction(curveSpotDate, cdsSpotDate)
        : -curveDCC.relativeYearFraction(cdsSpotDate, curveSpotDate);

    _curveDcc = curveDCC;
    _cdsSpotDate = cdsSpotDate;
    _currency = Currency.USD; // TODO
      time = DoubleArray.ofUnsafe(t);
  }

  /**
   * build a yield curve 
   * @param rates The par rates of the instruments (as fractions) 
   * @return a yield curve 
   */
    private IsdaCompliantZeroRateDiscountFactors build(final double[] rates) {
//    ArgumentChecker.notEmpty(rates, "rates");
    final int n = _curveNode.length;
//    ArgumentChecker.isTrue(n == rates.length, "expecting " + n + " rates, given " + rates.length);

    // set up curve with best guess rates
    DefaultCurveMetadata metadata = DefaultCurveMetadata.builder()
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .curveName("yield")                   // TODO relevant name, taken from index? -> can be final
        .dayCount(_curveDcc)
        .build();
    // TODO parameter meta data

    InterpolatedNodalCurve curve = InterpolatedNodalCurve.of(
          metadata, time, DoubleArray.ofUnsafe(rates), // TODO DoubleArray.ofUnsafe(_t) can be finalised
        CurveInterpolators.PRODUCT_LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.PRODUCT_LINEAR); // TODO rates.length > 1 is assumed
    // loop over the instruments and adjust the curve to price each in turn
    int mmCount = 0;
    int swapCount = 0;
    for (int i = 0; i < n; i++) {
      if (_curveNode[i] instanceof TermDepositCurveNode) {
        // TODO in ISDA code money market instruments of less than 21 days have special treatment
        double dfInv = 1d + rates[i] * _mmYF[mmCount++];
          double zr = Math.log(dfInv) / time.get(i);
        curve = curve.withParameter(i, zr);
      } else {
        curve = fitSwap(i, _swaps[swapCount++], curve, rates[i]);
      }
    }

//    final ISDACompliantYieldCurve baseCurve = new ISDACompliantYieldCurve(curve); // TODO offset
    if (_offset == 0.0) {
      return IsdaCompliantZeroRateDiscountFactors.of(_currency, _cdsSpotDate, curve);
    }
    return IsdaCompliantZeroRateDiscountFactors.of(_currency, _cdsSpotDate, withShift(curve, _offset));
  }

  private InterpolatedNodalCurve fitSwap(final int curveIndex, final BasicFixedLeg swap, final InterpolatedNodalCurve curve,
      final double swapRate) {

    final int nPayments = swap.getNumPayments();
    final int nNodes = curve.getParameterCount();
    final double t1 = curveIndex == 0 ? 0.0 : curve.getXValues().get(curveIndex - 1);
    final double t2 = curveIndex == nNodes - 1 ? Double.POSITIVE_INFINITY : curve.getXValues().get(curveIndex + 1);

    double temp = 0;
    double temp2 = 0;
    int i1 = 0;
    int i2 = nPayments;
    final double[] paymentAmounts = new double[nPayments];
    for (int i = 0; i < nPayments; i++) {
      final double t = swap.getPaymentTime(i);
      paymentAmounts[i] = swap.getPaymentAmounts(i, swapRate);
      if (t <= t1) {
        final double df = Math.exp(-curve.yValue(t) * t);
        temp += paymentAmounts[i] * df;
        temp2 += paymentAmounts[i] * t * df *
            curve.yValueParameterSensitivity(t).getSensitivity().get(curveIndex);
        i1++;
      } else if (t >= t2) {
        final double df = Math.exp(-curve.yValue(t) * t);
        temp += paymentAmounts[i] * df;
        temp2 -= paymentAmounts[i] * t * df *
            curve.yValueParameterSensitivity(t).getSensitivity().get(curveIndex);
        i2--;
      }
    }
    final double cachedValues = temp;
    final double cachedSense = temp2;
    final int index1 = i1;
    final int index2 = i2;

    final Function<Double, Double> func = new Function<Double, Double>() {

      @Override
      public Double apply(final Double x) {
        final InterpolatedNodalCurve tempCurve = curve.withParameter(curveIndex, x);
        double sum = 1.0 - cachedValues; // Floating leg at par
        for (int i = index1; i < index2; i++) {
          final double t = swap.getPaymentTime(i);
          sum -= paymentAmounts[i] * Math.exp(-tempCurve.yValue(t) * t);
        }
        return sum;
      }
    };

    final Function<Double, Double> grad = new Function<Double, Double>() {

      @Override
      public Double apply(final Double x) {
        final InterpolatedNodalCurve tempCurve = curve.withParameter(curveIndex, x);
        double sum = cachedSense;
        for (int i = index1; i < index2; i++) {
          final double t = swap.getPaymentTime(i);
          // TODO have two looks ups for the same time - could have a specialist function in ISDACompliantCurve
          sum += swap.getPaymentAmounts(i, swapRate) * t * Math.exp(-tempCurve.yValue(t) * t) *
              tempCurve.yValueParameterSensitivity(t).getSensitivity().get(curveIndex);
        }
        return sum;
      }

    };

    final double guess = curve.getParameter(curveIndex);
    if (guess == 0.0 && func.apply(guess) == 0.0) {
      return curve;
    }
    final double[] bracket = guess > 0d
        ? BRACKETER.getBracketedPoints(func, 0.8 * guess, 1.25 * guess, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
        : BRACKETER.getBracketedPoints(func, 1.25 * guess, 0.8 * guess, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    // final double r = ROOTFINDER.getRoot(func, bracket[0], bracket[1]);
    final double r = ROOTFINDER.getRoot(func, grad, bracket[0], bracket[1]);
    return curve.withParameter(curveIndex, r);
  }

  /**
   * very crude swap fixed leg description. TODO modify to match ISDA <p>
   * So that the floating leg can be taken as having a value of 1.0, rather than the text book 1 - P(T) for LIBOR discounting,
   * we add 1.0 to the final payment, which is financially equivalent
   */
  private class BasicFixedLeg {
    private final int _nPayments;
    private final double[] _swapPaymentTimes;
    private final double[] _yearFraction;

    public BasicFixedLeg(final LocalDate curveSpotDate, final LocalDate mat, final Period swapInterval, final DayCount swapDCC,
        final DayCount curveDCC, final BusinessDayConvention convention,
        final HolidayCalendar calendar) {
//      ArgumentChecker.isFalse(swapInterval.getDays() > 0, "swap interval must be in months or years");

      final List<LocalDate> list = new ArrayList<>();
      LocalDate tDate = mat;
      int step = 1;
      while (tDate.isAfter(curveSpotDate)) {
        list.add(tDate);
        tDate = mat.minus(swapInterval.multipliedBy(step++));
      }

      // remove spotDate from list, if it ends up there
      list.remove(curveSpotDate);

      _nPayments = list.size();
      _swapPaymentTimes = new double[_nPayments];
      _yearFraction = new double[_nPayments];

      LocalDate prev = curveSpotDate;
      int j = _nPayments - 1;
      for (int i = 0; i < _nPayments; i++, j--) {
        final LocalDate current = list.get(j);
        final LocalDate adjCurr = convention.adjust(current, calendar);
        _yearFraction[i] = swapDCC.relativeYearFraction(prev, adjCurr);
        _swapPaymentTimes[i] = curveDCC.relativeYearFraction(curveSpotDate, adjCurr); // Payment times always good business days
        prev = adjCurr;
      }
      //  _paymentAmounts[_nPayments - 1] += 1.0; // see Javadocs comment
    }

    public int getNumPayments() {
      return _nPayments;
    }

    public double getPaymentAmounts(final int index, final double rate) {
      return index == _nPayments - 1 ? 1 + rate * _yearFraction[index] : rate * _yearFraction[index];
    }

    public double getPaymentTime(final int index) {
      return _swapPaymentTimes[index];
    }

  }

  private InterpolatedNodalCurve withShift(InterpolatedNodalCurve curve,
      final double newBaseFromOriginalBase) {
    final int n = curve.getParameterCount();
//    ArgumentChecker.isTrue(n == r.length, "times and rates different lengths");
//    ArgumentChecker.isTrue(timesFromBaseDate[0] >= 0.0, "timesFromBaseDate must be >= 0");

    for (int i = 1; i < n; i++) {
//      ArgumentChecker.isTrue(timesFromBaseDate[i] > timesFromBaseDate[i - 1], "Times must be ascending");
    }


    if (newBaseFromOriginalBase == 0) { //no offset 
      return curve;
    }
//    final double[] timesFromBaseDate = curve.getXValues(); 
//    final double[] r = curve.getYValues();

    double[] time;
    double[] rate;
    if (newBaseFromOriginalBase < curve.getXValues().get(0)) {
      //offset less than t value of 1st knot, so no knots are not removed 
      time = new double[n];
      rate = new double[n];
      final double eta = curve.getYValues().get(0) * newBaseFromOriginalBase;
      for (int i = 0; i < n; i++) {
        time[i] = curve.getXValues().get(i) - newBaseFromOriginalBase;
        rate[i] = (curve.getYValues().get(i) * curve.getXValues().get(i) - eta) / time[i];
      }
    } else if (newBaseFromOriginalBase >= curve.getXValues().get(n - 1)) {
      //new base after last knot. The new 'curve' has a constant zero rate which we represent with a nominal knot at 1.0
      time = new double[1];
      rate = new double[1];
      time[0] = 1.0;
      rate[0] = (curve.getYValues().get(n - 1) * curve.getXValues().get(n - 1) -
          curve.getYValues().get(n - 2) * curve.getXValues().get(n - 2)) /
          (curve.getXValues().get(n - 1) - curve.getXValues().get(n - 2));
    } else {
      //offset greater than (or equal to) t value of 1st knot, so at least one knot must be removed  
      int index = Arrays.binarySearch(curve.getXValues().toArray(), newBaseFromOriginalBase);
      if (index < 0) {
        index = -(index + 1);
      } else {
        index++;
      }
      final double eta = (curve.getYValues().get(index - 1) * curve.getXValues().get(index - 1) *
          (curve.getXValues().get(index) - newBaseFromOriginalBase) +
          curve.getYValues().get(index) * curve.getXValues().get(index) *
              (newBaseFromOriginalBase - curve.getXValues().get(index - 1))) /
          (curve.getXValues().get(index) - curve.getXValues().get(index - 1));
      final int m = n - index;
      time = new double[m];
      rate = new double[m];
      for (int i = 0; i < m; i++) {
        time[i] = curve.getXValues().get(i + index) - newBaseFromOriginalBase;
        rate[i] = (curve.getYValues().get(i + index) * curve.getXValues().get(i + index) - eta) / time[i];
      }
    }
    return curve.withValues(DoubleArray.ofUnsafe(time), DoubleArray.ofUnsafe(rate)); // parameter metadata if # of nodes changed?

  }
  }
}
