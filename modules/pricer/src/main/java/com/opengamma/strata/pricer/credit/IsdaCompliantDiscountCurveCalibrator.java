/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

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
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.CurveParameterSize;
import com.opengamma.strata.market.curve.DepositIsdaCreditCurveNode;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.IsdaCreditCurveDefinition;
import com.opengamma.strata.market.curve.IsdaCreditCurveNode;
import com.opengamma.strata.market.curve.JacobianCalibrationMatrix;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.curve.SwapIsdaCreditCurveNode;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.UnitParameterSensitivities;
import com.opengamma.strata.math.impl.matrix.CommonsMatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.rootfinding.BracketRoot;
import com.opengamma.strata.math.impl.rootfinding.NewtonRaphsonSingleRootFinder;

/**
 * ISDA compliant discount curve calibrator.
 * <p>
 * A single discounting curve is calibrated for a specified currency.
 * <p>
 * The curve is defined using two or more {@linkplain CurveNode nodes}.
 * Each node primarily defines enough information to produce a reference trade.
 * Calibration involves pricing, and re-pricing, these trades to find the best fit using a root finder.
 * <p>
 * Once calibrated, the curves are then available for use.
 * Each node in the curve definition becomes a parameter in the matching output curve.
 */
public final class IsdaCompliantDiscountCurveCalibrator {

  /**
   * Default implementation.
   */
  public static final IsdaCompliantDiscountCurveCalibrator STANDARD = IsdaCompliantDiscountCurveCalibrator.of(1.0e-12);

  /**
   * The matrix algebra used for matrix inversion.
   */
  private static final MatrixAlgebra MATRIX_ALGEBRA = new CommonsMatrixAlgebra();
  /**
   * The root bracket finder.
   */
  private static final BracketRoot BRACKETER = new BracketRoot();
  /**
   * The root finder.
   */
  private final NewtonRaphsonSingleRootFinder rootFinder;

  //-------------------------------------------------------------------------
  /**
   * Obtains the standard curve calibrator.
   * <p>
   * The accuracy of the root finder is set to be its default, 1.0e-12;
   * 
   * @return the standard curve calibrator
   */
  public static IsdaCompliantDiscountCurveCalibrator standard() {
    return IsdaCompliantDiscountCurveCalibrator.STANDARD;
  }

  /**
   * Obtains the curve calibrator with the accuracy of the root finder specified. 
   * 
   * @param accuracy  the accuracy
   * @return the curve calibrator
   */
  public static IsdaCompliantDiscountCurveCalibrator of(double accuracy) {
    return new IsdaCompliantDiscountCurveCalibrator(accuracy);
  }

  // private constructor
  private IsdaCompliantDiscountCurveCalibrator(double accuracy) {
    this.rootFinder = new NewtonRaphsonSingleRootFinder(accuracy);
  }

  //-------------------------------------------------------------------------
  /**
   * Calibrates the ISDA compliant discount curve to the market data.
   * <p>
   * This creates the single discount curve for a specified currency.
   * The curve nodes in {@code IsdaCreditCurveDefinition} should be term deposit or fixed-for-Ibor swap, 
   * and the number of nodes should be greater than 1.
   * 
   * @param curveDefinition  the curve definition
   * @param marketData  the market data
   * @param refData  the reference data
   * @return the ISDA compliant discount curve
   */
  public IsdaCompliantZeroRateDiscountFactors calibrate(
      IsdaCreditCurveDefinition curveDefinition,
      MarketData marketData,
      ReferenceData refData) {

    List<? extends IsdaCreditCurveNode> curveNodes = curveDefinition.getCurveNodes();
    int nNodes = curveNodes.size();
    ArgChecker.isTrue(nNodes > 1, "the number of curve nodes must be greater than 1");
    LocalDate curveSnapDate = marketData.getValuationDate();
    LocalDate curveValuationDate = curveDefinition.getCurveValuationDate();
    DayCount curveDayCount = curveDefinition.getDayCount();
    BasicFixedLeg[] swapLeg = new BasicFixedLeg[nNodes];
    double[] termDepositYearFraction = new double[nNodes];
    double[] curveNodeTime = new double[nNodes];
    double[] rates = new double[nNodes];
    Builder<ParameterMetadata> paramMetadata = ImmutableList.builder();
    int nTermDeposit = 0;
    LocalDate curveSpotDate = null;
    for (int i = 0; i < nNodes; i++) {
      LocalDate cvDateTmp;
      IsdaCreditCurveNode node = curveNodes.get(i);
      rates[i] = marketData.getValue(node.getObservableId());
      LocalDate adjMatDate = node.date(curveSnapDate, refData);
      paramMetadata.add(node.metadata(adjMatDate));
      if (node instanceof DepositIsdaCreditCurveNode) {
        DepositIsdaCreditCurveNode termDeposit = (DepositIsdaCreditCurveNode) node;
        cvDateTmp = termDeposit.getSpotDateOffset().adjust(curveSnapDate, refData);
        curveNodeTime[i] = curveDayCount.relativeYearFraction(cvDateTmp, adjMatDate);
        termDepositYearFraction[i] = termDeposit.getDayCount().relativeYearFraction(cvDateTmp, adjMatDate);
        ArgChecker.isTrue(nTermDeposit == i, "TermDepositCurveNode should not be after FixedIborSwapCurveNode");
        ++nTermDeposit;
      } else if (node instanceof SwapIsdaCreditCurveNode) {
        SwapIsdaCreditCurveNode swap = (SwapIsdaCreditCurveNode) node;
        cvDateTmp = swap.getSpotDateOffset().adjust(curveSnapDate, refData);
        curveNodeTime[i] = curveDayCount.relativeYearFraction(cvDateTmp, adjMatDate);
        BusinessDayAdjustment busAdj = swap.getBusinessDayAdjustment();
        swapLeg[i] = new BasicFixedLeg(
            cvDateTmp,
            cvDateTmp.plus(swap.getTenor()),
            swap.getPaymentFrequency().getPeriod(),
            swap.getDayCount(),
            curveDayCount,
            busAdj,
            refData);
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
    double[] ratesMod = Arrays.copyOf(rates, nNodes);
    for (int i = 0; i < nTermDeposit; ++i) {
      double dfInv = 1d + ratesMod[i] * termDepositYearFraction[i];
      ratesMod[i] = Math.log(dfInv) / curveNodeTime[i];
    }
    InterpolatedNodalCurve curve = curveDefinition.curve(DoubleArray.ofUnsafe(curveNodeTime), DoubleArray.ofUnsafe(ratesMod));
    for (int i = nTermDeposit; i < nNodes; ++i) {
      curve = fitSwap(i, swapLeg[i], curve, rates[i]);
    }

    Currency currency = curveDefinition.getCurrency();
    DoubleMatrix sensi = quoteValueSensitivity(
        nTermDeposit, termDepositYearFraction, swapLeg, ratesMod, curve, curveDefinition.isComputeJacobian());
    if (curveValuationDate.isEqual(curveSpotDate)) {
      if (curveDefinition.isComputeJacobian()) {
        JacobianCalibrationMatrix jacobian = JacobianCalibrationMatrix.of(
            ImmutableList.of(CurveParameterSize.of(curveDefinition.getName(), nNodes)), MATRIX_ALGEBRA.getInverse(sensi));
        NodalCurve curveWithParamMetadata = curve.withMetadata(
            curve.getMetadata().withInfo(CurveInfoType.JACOBIAN, jacobian).withParameterMetadata(parameterMetadata));
        return IsdaCompliantZeroRateDiscountFactors.of(currency, curveValuationDate, curveWithParamMetadata);
      }
      NodalCurve curveWithParamMetadata = curve.withMetadata(curve.getMetadata().withParameterMetadata(parameterMetadata));
      return IsdaCompliantZeroRateDiscountFactors.of(currency, curveValuationDate, curveWithParamMetadata);
    }
    double offset = curveDayCount.relativeYearFraction(curveSpotDate, curveValuationDate);
    return IsdaCompliantZeroRateDiscountFactors.of(
        currency, curveValuationDate, withShift(curve, parameterMetadata, sensi, curveDefinition.isComputeJacobian(), offset));
  }

  //-------------------------------------------------------------------------
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

  //-------------------------------------------------------------------------
  // market quote sensitivity calculators
  private DoubleMatrix quoteValueSensitivity(
      int nTermDeposit,
      double[] termDepositYearFraction,
      BasicFixedLeg[] swapLeg,
      double[] rates,
      InterpolatedNodalCurve curve,
      boolean computejacobian) {

    if (computejacobian) {
      int nNode = curve.getParameterCount();
      DoubleMatrix sensiDeposit = DoubleMatrix.ofArrayObjects(nTermDeposit, nNode,
          i -> sensitivityDeposit(curve, termDepositYearFraction[i], i, rates[i]));
      DoubleMatrix sensiSwap = DoubleMatrix.ofArrayObjects(nNode - nTermDeposit, nNode,
          i -> sensitivitySwap(swapLeg[i + nTermDeposit], curve, rates[i + nTermDeposit]));
      double[][] sensiTotal = new double[nNode][];
      for (int i = 0; i < nTermDeposit; ++i) {
        sensiTotal[i] = sensiDeposit.rowArray(i);
      }
      for (int i = nTermDeposit; i < nNode; ++i) {
        sensiTotal[i] = sensiSwap.rowArray(i - nTermDeposit);
      }
      return DoubleMatrix.ofUnsafe(sensiTotal);
    }
    return DoubleMatrix.EMPTY;
  }

  private DoubleArray sensitivityDeposit(
      InterpolatedNodalCurve curve,
      double termDepositYearFraction,
      int index,
      double fixedRate) {

    int nNode = curve.getParameterCount();
    double[] sensi = new double[nNode];
    sensi[index] = curve.getXValues().get(index) * (1d + fixedRate * termDepositYearFraction) / termDepositYearFraction;
    return DoubleArray.ofUnsafe(sensi);
  }

  private DoubleArray sensitivitySwap(BasicFixedLeg swap, NodalCurve curve, double swapRate) {
    int nPayments = swap.getNumPayments();
    double annuity = 0d;
    UnitParameterSensitivities sensi = UnitParameterSensitivities.empty();
    for (int i = 0; i < nPayments - 1; i++) {
      double t = swap.getPaymentTime(i);
      double df = Math.exp(-curve.yValue(t) * t);
      annuity += swap.getYearFraction(i) * df;
      sensi = sensi.combinedWith(curve.yValueParameterSensitivity(t).multipliedBy(-df * t * swap.getYearFraction(i) * swapRate));
    }
    int lastIndex = nPayments - 1;
    double t = swap.getPaymentTime(lastIndex);
    double df = Math.exp(-curve.yValue(t) * t);
    annuity += swap.getYearFraction(lastIndex) * df;
    sensi = sensi.combinedWith(
        curve.yValueParameterSensitivity(t).multipliedBy(-df * t * (1d + swap.getYearFraction(lastIndex) * swapRate)));
    sensi = sensi.multipliedBy(-1d / annuity);
    ArgChecker.isTrue(sensi.size() == 1);
    return sensi.getSensitivities().get(0).getSensitivity();
  }

  //-------------------------------------------------------------------------
  /* crude swap fixed leg description */
  private final class BasicFixedLeg {
    private final int nPayment;
    private final double[] swapPaymentTime;
    private final double[] yearFraction;

    public BasicFixedLeg(
        LocalDate curveSpotDate,
        LocalDate maturityDate,
        Period swapInterval,
        DayCount swapDCC,
        DayCount curveDcc,
        BusinessDayAdjustment busAdj,
        ReferenceData refData) {

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

    public double getYearFraction(int index) {
      return yearFraction[index];
    }

  }

  //-------------------------------------------------------------------------
  // shift the curve 
  private NodalCurve withShift(
      InterpolatedNodalCurve curve,
      List<ParameterMetadata> parameterMetadata,
      DoubleMatrix sensitivity,
      boolean computeJacobian,
      double shift) {

    int nNode = curve.getParameterCount();
    if (shift < curve.getXValues().get(0)) {
      //offset less than t value of 1st knot, so no knots are not removed 
      double eta = curve.getYValues().get(0) * shift;
      DoubleArray time = DoubleArray.of(nNode, i -> curve.getXValues().get(i) - shift);
      DoubleArray rate = DoubleArray.of(nNode, i -> (curve.getYValues().get(i) * curve.getXValues().get(i) - eta) / time.get(i));
      CurveMetadata metadata = curve.getMetadata().withParameterMetadata(parameterMetadata);
      if (computeJacobian) {
        double[][] transf = new double[nNode][nNode];
        for (int i = 0; i < nNode; ++i) {
          transf[i][0] = -shift / time.get(i);
          transf[i][i] += curve.getXValues().get(i) / time.get(i);
        }
        DoubleMatrix jacobianMatrix =
            (DoubleMatrix) MATRIX_ALGEBRA.multiply(DoubleMatrix.ofUnsafe(transf), MATRIX_ALGEBRA.getInverse(sensitivity));
        JacobianCalibrationMatrix jacobian = JacobianCalibrationMatrix.of(
            ImmutableList.of(CurveParameterSize.of(curve.getName(), nNode)), jacobianMatrix);
        return curve.withValues(time, rate).withMetadata(metadata.withInfo(CurveInfoType.JACOBIAN, jacobian));
      }
      return curve.withValues(time, rate).withMetadata(metadata);
    }
    if (shift >= curve.getXValues().get(nNode - 1)) {
      //new base after last knot. The new 'curve' has a constant zero rate which we represent with a nominal knot at 1.0
      double time = 1d;
      double interval = curve.getXValues().get(nNode - 1) - curve.getXValues().get(nNode - 2);
      double rate = (curve.getYValues().get(nNode - 1) * curve.getXValues().get(nNode - 1) -
          curve.getYValues().get(nNode - 2) * curve.getXValues().get(nNode - 2)) / interval;
      if (computeJacobian) {
        double[][] transf = new double[1][nNode];
        transf[0][nNode - 2] = -curve.getXValues().get(nNode - 2) / interval;
        transf[0][nNode - 1] = curve.getXValues().get(nNode - 1) / interval;
        DoubleMatrix jacobianMatrix =
            (DoubleMatrix) MATRIX_ALGEBRA.multiply(DoubleMatrix.ofUnsafe(transf), MATRIX_ALGEBRA.getInverse(sensitivity));
        JacobianCalibrationMatrix jacobian = JacobianCalibrationMatrix.of(
            ImmutableList.of(CurveParameterSize.of(curve.getName(), nNode)), jacobianMatrix);
        return ConstantNodalCurve.of(curve.getMetadata().withInfo(CurveInfoType.JACOBIAN, jacobian), time, rate);
      }
      return ConstantNodalCurve.of(curve.getMetadata(), time, rate);
    }
    //offset greater than (or equal to) t value of 1st knot, so at least one knot must be removed  
    int index = Arrays.binarySearch(curve.getXValues().toArray(), shift);
    if (index < 0) {
      index = -(index + 1);
    } else {
      index++;
    }
    double interval = curve.getXValues().get(index) - curve.getXValues().get(index - 1);
    double tt1 = curve.getXValues().get(index - 1) * (curve.getXValues().get(index) - shift);
    double tt2 = curve.getXValues().get(index) * (shift - curve.getXValues().get(index - 1));
    double eta = (curve.getYValues().get(index - 1) * tt1 + curve.getYValues().get(index) * tt2) / interval;
    int m = nNode - index;
    CurveMetadata metadata = curve.getMetadata().withParameterMetadata(parameterMetadata.subList(index, nNode));
    final int indexFinal = index;
    DoubleArray time = DoubleArray.of(m, i -> curve.getXValues().get(i + indexFinal) - shift);
    DoubleArray rate = DoubleArray.of(m,
        i -> (curve.getYValues().get(i + indexFinal) * curve.getXValues().get(i + indexFinal) - eta) / time.get(i));
    if (computeJacobian) {
      double[][] transf = new double[m][nNode];
      for (int i = 0; i < m; ++i) {
        transf[i][index - 1] -= tt1 / (time.get(i) * interval);
        transf[i][index] -= tt2 / (time.get(i) * interval);
        transf[i][i + index] += curve.getXValues().get(i + index) / time.get(i);
      }
      DoubleMatrix jacobianMatrix =
          (DoubleMatrix) MATRIX_ALGEBRA.multiply(DoubleMatrix.ofUnsafe(transf), MATRIX_ALGEBRA.getInverse(sensitivity));
      JacobianCalibrationMatrix jacobian = JacobianCalibrationMatrix.of(
          ImmutableList.of(CurveParameterSize.of(curve.getName(), nNode)), jacobianMatrix);
      return curve.withValues(time, rate).withMetadata(metadata.withInfo(CurveInfoType.JACOBIAN, jacobian));
    }
    return curve.withValues(time, rate).withMetadata(metadata);
  }

}
