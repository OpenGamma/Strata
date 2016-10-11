/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
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
import com.opengamma.strata.data.MarketData;
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

/**
 * ISDA compliant discount curve calibrator.
 * <p>
 * A single discounting curve is calibrated for a specified currency.
 * <p>
 * The curve is defined using two or more {@linkplain CurveNode nodes}.
 * Each node primarily defines enough information to produce a reference trade.
 * Calibration involves pricing, and re-pricing, these trades to find the best fit
 * using a root finder.
 * <p>
 * Once calibrated, the curves are then available for use.
 * Each node in the curve definition becomes a parameter in the matching output curve.
 */
public final class IsdaCompliantDiscountCurveCalibrator {

  /**
   * Default implementation.
   */
  public static final IsdaCompliantDiscountCurveCalibrator DEFAULT = new IsdaCompliantDiscountCurveCalibrator(1e-12);
  /**
   * The root bracket finder.
   */
  private static final BracketRoot BRACKETER = new BracketRoot();
  /**
   * The root finder.
   */
  private final NewtonRaphsonSingleRootFinder rootFinder;;

  /**
   * Constructor with accuracy of the root finder specified.
   * 
   * @param accuracy  the accuracy
   */
  public IsdaCompliantDiscountCurveCalibrator(double accuracy) {
    rootFinder = new NewtonRaphsonSingleRootFinder(accuracy);
  }

  //-------------------------------------------------------------------------
  /**
   * Calibrates the ISDA compliant discount curve to the market data.
   * <p>
   * This creates the single discount curve for a specified currency.
   * The curve nodes should be term deposit or fixed-for-Ibor swap, and the number of nodes should be greater than 1.
   * Typically, the term deposit nodes are used for tenor < 1Y and the swap nodes are used otherwise.
   * <p>
   * It is general that the snap date of the market data is different from the valuation date on which 
   * the resultant curve will be used for pricing CDSs. {@code valuationDate} in {@code marketData} represents 
   * the snap date, whereas {@code curveValuationDate} does the valuation date.
   * <p>
   * Note that the inverse Jacobian is not computed currently.
   * 
   * @param curveNode  the curve node
   * @param curveValuationDate  the curve valuation date
   * @param curveDcc  the curve day count
   * @param name  the curve name
   * @param currency  the currency
   * @param marketData  the market data
   * @param refData  the reference data
   * @return the ISDA compliant discount curve
   */
  public IsdaCompliantZeroRateDiscountFactors calibrate(
      List<CurveNode> curveNode,
      LocalDate curveValuationDate,
      DayCount curveDcc,
      CurveName name,
      Currency currency,
      MarketData marketData,
      ReferenceData refData) {

    ArgChecker.isTrue(curveNode.size() > 1, "the number of curve nodes must be greater than 1");
    LocalDate curveSnapDate = marketData.getValuationDate();
    int nNode = curveNode.size();
    BasicFixedLeg[] swapLeg = new BasicFixedLeg[nNode];
    double[] termDepositYearFraction = new double[nNode];
    double[] curveNodeTime = new double[nNode];
    double[] rates = new double[nNode];
    Builder<ParameterMetadata> paramMetadata = ImmutableList.builder();
    int nTermDeposit = 0;
    LocalDate curveSpotDate = null;
    for (int i = 0; i < nNode; i++) {
      LocalDate cvDateTmp;
      CurveNode node = curveNode.get(i);
      if (node instanceof TermDepositCurveNode) {
        TermDepositCurveNode termDeposit = (TermDepositCurveNode) node;
        rates[i] = marketData.getValue(termDeposit.getRateId());
        TermDepositTemplate template = termDeposit.getTemplate();
        ImmutableTermDepositConvention conv = (ImmutableTermDepositConvention) template.getConvention();
        LocalDate adjMatDate = curveNode.get(i).date(curveSnapDate, refData);
        cvDateTmp = conv.getSpotDateOffset().adjust(curveSnapDate, refData);
        termDepositYearFraction[i] = conv.getDayCount().relativeYearFraction(cvDateTmp, adjMatDate);
        curveNodeTime[i] = curveDcc.relativeYearFraction(cvDateTmp, adjMatDate);
        paramMetadata.add(TenorDateParameterMetadata.of(adjMatDate, Tenor.of(template.getDepositPeriod())));
        ArgChecker.isTrue(nTermDeposit == i, "TermDepositCurveNode should not be after FixedIborSwapCurveNode");
        ++nTermDeposit;
      } else if (node instanceof FixedIborSwapCurveNode) {
        FixedIborSwapCurveNode swap = (FixedIborSwapCurveNode) node;
        rates[i] = marketData.getValue(swap.getRateId());
        FixedIborSwapTemplate template = swap.getTemplate();
        FixedRateSwapLegConvention conv = template.getConvention().getFixedLeg();
        cvDateTmp = template.getConvention().getSpotDateOffset().adjust(curveSnapDate, refData);
        LocalDate adjMatDate = curveNode.get(i).date(curveSnapDate, refData);
        curveNodeTime[i] = curveDcc.relativeYearFraction(cvDateTmp, adjMatDate);
        BusinessDayAdjustment busAdj = conv.getAccrualBusinessDayAdjustment();
        ArgChecker.isTrue(
            busAdj.equals(conv.getStartDateBusinessDayAdjustment()) && busAdj.equals(conv.getEndDateBusinessDayAdjustment()),
            "The same business day adjustment should be used for start date, end date and accrual schedule");
        swapLeg[i] = new BasicFixedLeg(cvDateTmp, cvDateTmp.plus(template.getTenor()), conv.getPaymentFrequency().getPeriod(),
            conv.getDayCount(), curveDcc, busAdj, refData);
        paramMetadata.add(TenorDateParameterMetadata.of(adjMatDate, template.getTenor()));
      } else {
        throw new IllegalArgumentException("unsupported cuve node type");
      }
      if (i > 0) {
        ArgChecker.isTrue(curveNodeTime[i] - curveNodeTime[i - 1] > 0, "curve nodes should be ascending in terms of tenor");
        ArgChecker.isTrue(cvDateTmp.equals(curveSpotDate), "spot lag should be common for all of the curve nodes");
      } else {
        ArgChecker.isTrue(curveNodeTime[i] >= 0d, "the first node should be after curve spot date");
        curveSpotDate = cvDateTmp;
      }
    }
    ImmutableList<ParameterMetadata> parameterMetadata = paramMetadata.build();
    CurveMetadata baseMetadata = DefaultCurveMetadata.builder()
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .curveName(name)
        .dayCount(curveDcc)
        .build();

    double[] ratesMod = Arrays.copyOf(rates, nNode);
    for (int i = 0; i < nTermDeposit; ++i) {
      double dfInv = 1d + ratesMod[i] * termDepositYearFraction[i];
      ratesMod[i] = Math.log(dfInv) / curveNodeTime[i];
    }
    InterpolatedNodalCurve curve = InterpolatedNodalCurve.of(
        baseMetadata,
        DoubleArray.ofUnsafe(curveNodeTime),
        DoubleArray.ofUnsafe(ratesMod),
        CurveInterpolators.PRODUCT_LINEAR,
        CurveExtrapolators.FLAT,
        CurveExtrapolators.PRODUCT_LINEAR);
    for (int i = nTermDeposit; i < nNode; ++i) {
      curve = fitSwap(i, swapLeg[i], curve, rates[i]);
    }

    if (curveValuationDate.isEqual(curveSpotDate)) {
      NodalCurve curveWithParamMetadata = curve.withMetadata(baseMetadata.withParameterMetadata(parameterMetadata));
      return IsdaCompliantZeroRateDiscountFactors.of(currency, curveValuationDate, curveWithParamMetadata);
    }
    double offset = curveDcc.relativeYearFraction(curveSpotDate, curveValuationDate);
    return IsdaCompliantZeroRateDiscountFactors.of(currency, curveValuationDate, withShift(curve, parameterMetadata, offset));
  }

  private InterpolatedNodalCurve fitSwap(int curveIndex, BasicFixedLeg swap, InterpolatedNodalCurve curve, double swapRate) {
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
        temp2 += paymentAmounts[i] * t * df * curve.yValueParameterSensitivity(t).getSensitivity().get(curveIndex);
        i1++;
      } else if (t >= t2) {
        double df = Math.exp(-curve.yValue(t) * t);
        temp += paymentAmounts[i] * df;
        temp2 -= paymentAmounts[i] * t * df * curve.yValueParameterSensitivity(t).getSensitivity().get(curveIndex);
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
    double r = rootFinder.getRoot(func, grad, bracket[0], bracket[1]);
    return curve.withParameter(curveIndex, r);
  }

  /**
   * crude swap fixed leg description.
   */
  private final class BasicFixedLeg {
    private final int nPayment;
    private final double[] swapPaymentTime;
    private final double[] yearFraction;

    public BasicFixedLeg(LocalDate curveSpotDate, LocalDate maturityDate, Period swapInterval, DayCount swapDCC,
        DayCount curveDcc, BusinessDayAdjustment busAdj, ReferenceData refData) {

      List<LocalDate> list = new ArrayList<>();
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
    }

    public int getNumPayments() {
      return nPayment;
    }

    public double getPaymentAmounts(int index, double rate) {
      return index == nPayment - 1 ? 1 + rate * yearFraction[index] : rate * yearFraction[index];
    }

    public double getPaymentTime(int index) {
      return swapPaymentTime[index];
    }

  }

  private NodalCurve withShift(InterpolatedNodalCurve curve, List<ParameterMetadata> parameterMetadata, double shift) {
    int nNode = curve.getParameterCount();
    if (shift < curve.getXValues().get(0)) {
      //offset less than t value of 1st knot, so no knots are not removed 
      double eta = curve.getYValues().get(0) * shift;
      DoubleArray time = DoubleArray.of(nNode, i -> curve.getXValues().get(i) - shift);
      DoubleArray rate = DoubleArray.of(nNode, i -> (curve.getYValues().get(i) * curve.getXValues().get(i) - eta) / time.get(i));
      CurveMetadata metadata = curve.getMetadata().withParameterMetadata(parameterMetadata);
      return curve.withValues(time, rate).withMetadata(metadata);
    }
    if (shift >= curve.getXValues().get(nNode - 1)) {
      //new base after last knot. The new 'curve' has a constant zero rate which we represent with a nominal knot at 1.0
      double time = 1d;
      double rate = (curve.getYValues().get(nNode - 1) * curve.getXValues().get(nNode - 1) -
          curve.getYValues().get(nNode - 2) * curve.getXValues().get(nNode - 2)) /
          (curve.getXValues().get(nNode - 1) - curve.getXValues().get(nNode - 2));
      return ConstantNodalCurve.of(curve.getMetadata(), time, rate);
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
    CurveMetadata metadata = curve.getMetadata().withParameterMetadata(parameterMetadata.subList(index, nNode));
    final int indexFinal = index;
    DoubleArray time = DoubleArray.of(m, i -> curve.getXValues().get(i + indexFinal) - shift);
    DoubleArray rate = DoubleArray.of(m,
        i -> (curve.getYValues().get(i + indexFinal) * curve.getXValues().get(i + indexFinal) - eta) / time.get(i));
    return curve.withValues(time, rate).withMetadata(metadata);
  }

}
