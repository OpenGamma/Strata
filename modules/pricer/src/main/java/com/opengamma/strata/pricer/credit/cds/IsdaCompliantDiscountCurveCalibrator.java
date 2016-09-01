package com.opengamma.strata.pricer.credit.cds;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.curve.node.FixedIborSwapCurveNode;
import com.opengamma.strata.market.curve.node.TermDepositCurveNode;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.TenorDateParameterMetadata;
import com.opengamma.strata.math.impl.rootfinding.BracketRoot;
import com.opengamma.strata.math.impl.rootfinding.NewtonRaphsonSingleRootFinder;
import com.opengamma.strata.product.deposit.type.ImmutableTermDepositConvention;
import com.opengamma.strata.product.deposit.type.TermDepositTemplate;
import com.opengamma.strata.product.swap.type.FixedIborSwapTemplate;
import com.opengamma.strata.product.swap.type.FixedRateSwapLegConvention;

public final class IsdaCompliantDiscountCurveCalibrator {

  private static final NewtonRaphsonSingleRootFinder ROOTFINDER = new NewtonRaphsonSingleRootFinder();
  private static final BracketRoot BRACKETER = new BracketRoot();

  private final DayCount curveDcc;
  private final LocalDate valuationDate;
  private final LocalDate curveSpotDate;
  private final int nNode;
  private final int nTermDeposit;
  private final ImmutableList<ParameterMetadata> parameterMetadata;
  private final DoubleArray curveNodeTime;
  private final double[] termDepositYearFraction;
  private final Currency currency;
  private final BasicFixedLeg[] swapLeg;
  private final CurveMetadata baseMetadata;

  // TODO jacobian matrix

  public IsdaCompliantDiscountCurveCalibrator(
      LocalDate valuationDate,
      List<CurveNode> curveNode,
      DayCount curveDCC,
      CurveName name,
      ReferenceData refData) {

    // TODO not null
    this.curveDcc = curveDCC;
    this.valuationDate = valuationDate;
    this.nNode = curveNode.size();
    this.swapLeg = new BasicFixedLeg[nNode];
    this.termDepositYearFraction = new double[nNode];

    Builder<ParameterMetadata> paramMetadata = ImmutableList.builder();
    int nTermDeposit = 0;
    double[] time = new double[nNode];
    Currency currency = null;
    LocalDate curveSpotDate = null;
    for (int i = 0; i < nNode; i++) {
      Currency ccyTmp;
      LocalDate cvDateTmp;
      if (curveNode.get(i) instanceof TermDepositCurveNode) {
        TermDepositTemplate template = ((TermDepositCurveNode) curveNode.get(i)).getTemplate();
        ImmutableTermDepositConvention conv = (ImmutableTermDepositConvention) template.getConvention();
        LocalDate adjMatDate = curveNode.get(i).date(valuationDate, refData);
        cvDateTmp = conv.getSpotDateOffset().adjust(valuationDate, refData);
        termDepositYearFraction[i] = conv.getDayCount().relativeYearFraction(cvDateTmp, adjMatDate);
        ccyTmp = conv.getCurrency();
        time[i] = curveDcc.relativeYearFraction(cvDateTmp, adjMatDate);
        paramMetadata.add(TenorDateParameterMetadata.of(adjMatDate, Tenor.of(template.getDepositPeriod())));
        ArgChecker.isTrue(nTermDeposit == i, "TermDepositCurveNode should be before FixedIborSwapCurveNode");
        ++nTermDeposit;
      } else if (curveNode.get(i) instanceof FixedIborSwapCurveNode) {
        FixedIborSwapTemplate template = ((FixedIborSwapCurveNode) curveNode.get(i)).getTemplate();
        FixedRateSwapLegConvention conv = template.getConvention().getFixedLeg();
        cvDateTmp = template.getConvention().getSpotDateOffset().adjust(valuationDate, refData);
        ccyTmp = conv.getCurrency();
        LocalDate adjMatDate = curveNode.get(i).date(valuationDate, refData);
        time[i] = curveDcc.relativeYearFraction(cvDateTmp, adjMatDate);

        BusinessDayAdjustment busAdj = conv.getAccrualBusinessDayAdjustment();
        ArgChecker.isTrue(
            busAdj.equals(conv.getStartDateBusinessDayAdjustment()) && busAdj.equals(conv.getEndDateBusinessDayAdjustment()),
            "The same business day adjustment should be used for start date, end date and accrual schedule");
        swapLeg[i] = new BasicFixedLeg(cvDateTmp, cvDateTmp.plus(template.getTenor()), conv.getPaymentFrequency().getPeriod(),
            conv.getDayCount(), busAdj, refData);
        paramMetadata.add(TenorDateParameterMetadata.of(adjMatDate, template.getTenor()));
      } else {
        throw new IllegalArgumentException("unsupported cuve node type");
      }
      if (i > 0) {
        ArgChecker.isTrue(time[i] - time[i - 1] > 0, "curve nodes should be ascending in terms of tenor");
        ArgChecker.isTrue(ccyTmp.equals(currency), "currency should be common for all of the curve nodes");
        ArgChecker.isTrue(cvDateTmp.equals(curveSpotDate), "spot lag should be common for all of the curve nodes");
      } else {
        ArgChecker.isTrue(time[i] >= 0d, "the first node should be after curve spot date");
        currency = ccyTmp;
        curveSpotDate = cvDateTmp;
      }
    }
    parameterMetadata = paramMetadata.build();
    baseMetadata = DefaultCurveMetadata.builder()
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .curveName(name)
        .dayCount(curveDcc)
        .build();

    this.nTermDeposit = nTermDeposit;
    this.curveNodeTime = DoubleArray.ofUnsafe(time);
    this.currency = currency;
    this.curveSpotDate = curveSpotDate;
  }

  /**
   * Builds a yield curve. 
   * 
   * @param rates The par rates of the instruments (as fractions) 
   * @param name  the curve name
   * @return a yield curve 
   */
  public IsdaCompliantZeroRateDiscountFactors build(double[] rates) {
    ArgChecker.isTrue(nNode == rates.length, "expecting " + nNode + " rates, given " + rates.length);

    double[] ratesMod = Arrays.copyOf(rates, nNode);
    for (int i = 0; i < nTermDeposit; ++i) {
      double dfInv = 1d + ratesMod[i] * termDepositYearFraction[i];
      ratesMod[i] = Math.log(dfInv) / curveNodeTime.get(i);
    }
    InterpolatedNodalCurve curve = InterpolatedNodalCurve.of(
        baseMetadata, curveNodeTime, DoubleArray.ofUnsafe(ratesMod),
        CurveInterpolators.PRODUCT_LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.PRODUCT_LINEAR);
    for (int i = nTermDeposit; i < nNode; ++i) {
      curve = fitSwap(i, swapLeg[i], curve, rates[i]);
    }

    if (valuationDate.isEqual(curveSpotDate)) {
      NodalCurve curveWithParamMetadata = curve.withMetadata(baseMetadata.withParameterMetadata(parameterMetadata));
      return IsdaCompliantZeroRateDiscountFactors.of(currency, valuationDate, curveWithParamMetadata);
    }
    double offset = curveDcc.relativeYearFraction(curveSpotDate, valuationDate);
    return IsdaCompliantZeroRateDiscountFactors.of(currency, valuationDate, withShift(curve, offset));
  }

  private InterpolatedNodalCurve fitSwap(int curveIndex, BasicFixedLeg swap, InterpolatedNodalCurve curve,
      double swapRate) {

    int nPayments = swap.getNumPayments();
    int nNodes = curve.getParameterCount();
    double t1 = curveIndex == 0 ? 0.0 : curve.getXValues().get(curveIndex - 1);
    double t2 = curveIndex == nNodes - 1 ? Double.POSITIVE_INFINITY : curve.getXValues().get(curveIndex + 1);

    double temp = 0;
    double temp2 = 0;
    int i1 = 0;
    int i2 = nPayments;
    double[] paymentAmounts = new double[nPayments];
    for (int i = 0; i < nPayments; i++) {
      double t = swap.getPaymentTime(i);
      paymentAmounts[i] = swap.getPaymentAmounts(i, swapRate);
      if (t <= t1) {
        double df = Math.exp(-curve.yValue(t) * t);
        temp += paymentAmounts[i] * df;
        temp2 += paymentAmounts[i] * t * df *
            curve.yValueParameterSensitivity(t).getSensitivity().get(curveIndex);
        i1++;
      } else if (t >= t2) {
        double df = Math.exp(-curve.yValue(t) * t);
        temp += paymentAmounts[i] * df;
        temp2 -= paymentAmounts[i] * t * df *
            curve.yValueParameterSensitivity(t).getSensitivity().get(curveIndex);
        i2--;
      }
    }
    double cachedValues = temp;
    double cachedSense = temp2;
    int index1 = i1;
    int index2 = i2;

    Function<Double, Double> func = new Function<Double, Double>() {

      @Override
      public Double apply(final Double x) {
        InterpolatedNodalCurve tempCurve = curve.withParameter(curveIndex, x);
        double sum = 1.0 - cachedValues; // Floating leg at par
        for (int i = index1; i < index2; i++) {
          double t = swap.getPaymentTime(i);
          sum -= paymentAmounts[i] * Math.exp(-tempCurve.yValue(t) * t);
        }
        return sum;
      }
    };

    Function<Double, Double> grad = new Function<Double, Double>() {

      @Override
      public Double apply(final Double x) {
        InterpolatedNodalCurve tempCurve = curve.withParameter(curveIndex, x);
        double sum = cachedSense;
        for (int i = index1; i < index2; i++) {
          double t = swap.getPaymentTime(i);
          sum += swap.getPaymentAmounts(i, swapRate) * t * Math.exp(-tempCurve.yValue(t) * t) *
              tempCurve.yValueParameterSensitivity(t).getSensitivity().get(curveIndex);
        }
        return sum;
      }

    };

    double guess = curve.getParameter(curveIndex);
    if (guess == 0.0 && func.apply(guess) == 0.0) {
      return curve;
    }
    double[] bracket = guess > 0d
        ? BRACKETER.getBracketedPoints(func, 0.8 * guess, 1.25 * guess, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
        : BRACKETER.getBracketedPoints(func, 1.25 * guess, 0.8 * guess, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    double r = ROOTFINDER.getRoot(func, grad, bracket[0], bracket[1]);
    return curve.withParameter(curveIndex, r);
  }

  /**
   * very crude swap fixed leg description.
   * So that the floating leg can be taken as having a value of 1.0, rather than the text book 1 - P(T) for LIBOR discounting,
   * we add 1.0 to the final payment, which is financially equivalent
   */
  private final class BasicFixedLeg {
    private final int nPayment;
    private final double[] swapPaymentTime;
    private final double[] yearFraction;

    public BasicFixedLeg(LocalDate curveSpotDate, LocalDate maturityDate, Period swapInterval, DayCount swapDCC,
//        BusinessDayConvention convention, HolidayCalendar calendar) {
        BusinessDayAdjustment busAdj, ReferenceData refData) {

      final List<LocalDate> list = new ArrayList<>();
      LocalDate tDate = maturityDate;
      int step = 1;
      while (tDate.isAfter(curveSpotDate)) {
        list.add(tDate);
        tDate = maturityDate.minus(swapInterval.multipliedBy(step++));
      }

      // remove spotDate from list, if it ends up there
      list.remove(curveSpotDate);

      nPayment = list.size();
      swapPaymentTime = new double[nPayment];
      yearFraction = new double[nPayment];

      LocalDate prev = curveSpotDate;
      int j = nPayment - 1;
      for (int i = 0; i < nPayment; i++, j--) {
        LocalDate current = list.get(j);
        LocalDate adjCurr = busAdj.adjust(current, refData);
        yearFraction[i] = swapDCC.relativeYearFraction(prev, adjCurr);
        swapPaymentTime[i] = curveDcc.relativeYearFraction(curveSpotDate, adjCurr); // Payment times always good business days
        prev = adjCurr;
      }
      //  _paymentAmounts[_nPayments - 1] += 1.0; // see Javadocs comment
    }

    public int getNumPayments() {
      return nPayment;
    }

    public double getPaymentAmounts(final int index, final double rate) {
      return index == nPayment - 1 ? 1 + rate * yearFraction[index] : rate * yearFraction[index];
    }

    public double getPaymentTime(final int index) {
      return swapPaymentTime[index];
    }

  }

  private NodalCurve withShift(InterpolatedNodalCurve curve, double shift) {
    if (shift < curve.getXValues().get(0)) {
      //offset less than t value of 1st knot, so no knots are not removed 
      double eta = curve.getYValues().get(0) * shift;
      DoubleArray time = DoubleArray.of(nNode, i -> curve.getXValues().get(i) - shift);
      DoubleArray rate = DoubleArray.of(nNode, i -> (curve.getYValues().get(i) * curve.getXValues().get(i) - eta) / time.get(i));
      CurveMetadata metadata = baseMetadata.withParameterMetadata(parameterMetadata);
      return curve.withValues(time, rate).withMetadata(metadata);
    }
    if (shift >= curve.getXValues().get(nNode - 1)) {
      //new base after last knot. The new 'curve' has a constant zero rate which we represent with a nominal knot at 1.0
      double time = 1d;
      double rate = (curve.getYValues().get(nNode - 1) * curve.getXValues().get(nNode - 1) -
          curve.getYValues().get(nNode - 2) * curve.getXValues().get(nNode - 2)) /
          (curve.getXValues().get(nNode - 1) - curve.getXValues().get(nNode - 2));
      return ConstantNodalCurve.of(baseMetadata, time, rate);
    }
    //offset greater than (or equal to) t value of 1st knot, so at least one knot must be removed  
    int index = Arrays.binarySearch(curve.getXValues().toArray(), shift);
    if (index < 0) {
      index = -(index + 1);
    } else {
      index++;
    }
    double eta =
        (curve.getYValues().get(index - 1) * curve.getXValues().get(index - 1) * (curve.getXValues().get(index) - shift) +
            curve.getYValues().get(index) * curve.getXValues().get(index) * (shift - curve.getXValues().get(index - 1))) /
            (curve.getXValues().get(index) - curve.getXValues().get(index - 1));
    int m = nNode - index;
    CurveMetadata metadata = baseMetadata.withParameterMetadata(parameterMetadata.subList(index, nNode));
    final int indexFinal = index;
    DoubleArray time = DoubleArray.of(m, i -> curve.getXValues().get(i + indexFinal) - shift);
    DoubleArray rate = DoubleArray.of(m,
        i -> (curve.getYValues().get(i + indexFinal) * curve.getXValues().get(i + indexFinal) - eta) / time.get(i));
    return curve.withValues(time, rate).withMetadata(metadata);
  }

}
