/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.UnitParameterSensitivity;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCreditCurve;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCurve;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantYieldCurve;
import com.opengamma.strata.product.credit.ResolvedCds;

/**
 * Pricer for for CDS products using the ISDA methodology.
 * <p>
 * This function provides the ability to price a {@link ResolvedCds}.
 * Both single name and index swaps can be priced.
 */
public class IsdaCdsPricer {

  /**
   * Default implementation
   */
  public static final IsdaCdsPricer DEFAULT = new IsdaCdsPricer();

  /**
   * Standard one basis point for applying shifts
   */
  private static final double ONE_BPS = 0.0001d;

  //-------------------------------------------------------------------------

  /**
   * Calculates the present value of the expanded CDS product.
   * <p>
   * The present value of the CDS is the present value of all cashflows as of the valuation date.
   *
   * @param product  expanded CDS product
   * @param yieldCurve  calibrated curve points of the ISDA discount curve to use
   * @param creditCurve  calibrated curve points of the ISDA spread curve to use
   * @param valuationDate date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associate with underlying issue or index
   * @param scalingFactor linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurrencyAmount presentValue(
      ResolvedCds product,
      NodalCurve yieldCurve,
      NodalCurve creditCurve,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    return IsdaCdsHelper.price(valuationDate, product, yieldCurve, creditCurve, recoveryRate, scalingFactor);
  }

  /**
   * Calculates the present value of the expanded CDS product.
   * <p>
   * The present value of the CDS is the present value of all cashflows as of the valuation date.
   *
   * @param product  expanded CDS product
   * @param yieldCurveInputs  par rate curve points of the ISDA discount curve to use
   * @param creditCurveInputs  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associate with underlying issue or index
   * @param scalingFactor  linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurrencyAmount presentValue(
      ResolvedCds product,
      IsdaYieldCurveInputs yieldCurveInputs,
      IsdaCreditCurveInputs creditCurveInputs,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    NodalCurve yieldCurve = IsdaNodalCurve.of(valuationDate, yieldCurveInputs);
    NodalCurve creditCurve = IsdaNodalCurve.of(valuationDate, creditCurveInputs, yieldCurve, recoveryRate);

    return IsdaCdsHelper.price(valuationDate, product, yieldCurve, creditCurve, recoveryRate, scalingFactor);
  }

  /**
   * Calculates the par rate of the expanded CDS product.
   * <p>
   * The par rate of the CDS is the coupon rate that will make present value of all cashflows
   * equal zero as of the valuation date.
   *
   * @param product  expanded CDS product
   * @param yieldCurveInputs  par rate curve points of the ISDA discount curve to use
   * @param creditCurveInputs  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associate with underlying issue or index
   * @return par rate for the credit default swap
   */
  public double parRate(
      ResolvedCds product,
      IsdaYieldCurveInputs yieldCurveInputs,
      IsdaCreditCurveInputs creditCurveInputs,
      LocalDate valuationDate,
      double recoveryRate) {

    NodalCurve yieldCurve = IsdaNodalCurve.of(valuationDate, yieldCurveInputs);
    NodalCurve creditCurve = IsdaNodalCurve.of(valuationDate, creditCurveInputs, yieldCurve, recoveryRate);

    return IsdaCdsHelper.parSpread(valuationDate, product, yieldCurve, creditCurve, recoveryRate);
  }

  /**
   * Local class that implements ISDANodalCurve
   * This is a further step towards supporting regular Strata ParRates and zero curves for CDS
   * This class wraps a {@link IsdaCompliantCurve} as well as {@link CurveMetadata}
   * from the input par rates information.
   * <p>
   * The static factory methods either bootstrap and calibrate a curve or they replace the
   * calibrated values with a supplied vector of zeroes or hazards (the y axis of the calibrated curve)
   */
  static final class IsdaNodalCurve implements NodalCurve {
    private final IsdaCompliantCurve underlyingCurve;
    private final CurveMetadata curveMetadata;

    private IsdaNodalCurve(IsdaCompliantCurve underlyingCurve, CurveMetadata curveMetadata) {
      this.underlyingCurve = underlyingCurve;
      this.curveMetadata = curveMetadata;
    }

    @Override
    public DoubleArray getXValues() {
      return underlyingCurve.getXValues();
    }

    @Override
    public DoubleArray getYValues() {
      return DoubleArray.copyOf(underlyingCurve.getRt());
    }

    @Override
    public NodalCurve withYValues(DoubleArray values) {
      return new IsdaNodalCurve(IsdaCompliantCurve.makeFromRT(getXValues(), values), curveMetadata);
    }

    @Override
    public NodalCurve withValues(DoubleArray xValues, DoubleArray yValues) {
      return new IsdaNodalCurve(IsdaCompliantCurve.makeFromRT(xValues, yValues), curveMetadata);
    }

    @Override
    public CurveMetadata getMetadata() {
      return curveMetadata;
    }

    @Override
    public IsdaNodalCurve withMetadata(CurveMetadata metadata) {
      return new IsdaNodalCurve(underlyingCurve, metadata);
    }

    //-------------------------------------------------------------------------
    @Override
    public int getParameterCount() {
      return underlyingCurve.getParameterCount();
    }

    @Override
    public double getParameter(int parameterIndex) {
      return underlyingCurve.getParameter(parameterIndex);
    }

    @Override
    public ParameterMetadata getParameterMetadata(int parameterIndex) {
      return underlyingCurve.getParameterMetadata(parameterIndex);
    }

    @Override
    public IsdaNodalCurve withParameter(int parameterIndex, double newValue) {
      return new IsdaNodalCurve(underlyingCurve.withParameter(parameterIndex, newValue), curveMetadata);
    }

    @Override
    public IsdaNodalCurve withNode(double x, double y, ParameterMetadata paramMetadata) {
      throw new UnsupportedOperationException("ISDA credit curve does not allow node to be inserted");
    }

    //-------------------------------------------------------------------------
    @Override
    public double yValue(double x) {
      return underlyingCurve.yValue(x);
    }

    @Override
    public UnitParameterSensitivity yValueParameterSensitivity(double x) {
      return createParameterSensitivity(DoubleArray.filled(getParameterCount()));
    }

    @Override
    public double firstDerivative(double x) {
      return 0;
    }

    // bootstraps a yield curve from par rates
    public static NodalCurve of(LocalDate valuationDate, IsdaYieldCurveInputs yieldCurveInputs) {
      IsdaCompliantYieldCurve yieldCurve = IsdaCdsHelper.createIsdaDiscountCurve(valuationDate, yieldCurveInputs);
      IsdaCompliantCurve underlying = yieldCurve;
      return new IsdaNodalCurve(underlying, yieldCurveInputs.getCurveMetaData());
    }

    // bootstraps a credit curve from par rates
    public static NodalCurve of(
        LocalDate valuationDate,
        IsdaCreditCurveInputs creditCurveInputs,
        NodalCurve yieldCurve,
        double recoveryRate) {

      IsdaCompliantCreditCurve creditCurve = IsdaCdsHelper.createIsdaCreditCurve(valuationDate, creditCurveInputs, yieldCurve,
          recoveryRate);
      IsdaCompliantCurve underlying = creditCurve;
      return new IsdaNodalCurve(underlying, creditCurveInputs.getCurveMetaData());
    }

    // overwrites the x and y values of a calibrated curve, but copy the curve metadata to the new instance
    public static NodalCurve of(IsdaYieldCurveInputs yieldCurveInputs, double[] t, double[] rt) {
      IsdaCompliantYieldCurve yieldCurve = IsdaCompliantYieldCurve.makeFromRT(t, rt);
      IsdaCompliantCurve underlying = yieldCurve;
      return new IsdaNodalCurve(underlying, yieldCurveInputs.getCurveMetaData());
    }

    public static NodalCurve of(IsdaYieldCurveInputs yieldCurveInputs, DoubleArray t, DoubleArray rt) {
      IsdaCompliantYieldCurve yieldCurve = IsdaCompliantYieldCurve.makeFromRT(t, rt);
      IsdaCompliantCurve underlying = yieldCurve;
      return new IsdaNodalCurve(underlying, yieldCurveInputs.getCurveMetaData());
    }

    // overwrites the x and y values of a calibrated curve, but copy the curve metadata to the new instance
    public static NodalCurve of(IsdaCreditCurveInputs creditCurveInputs, double[] t, double[] ht) {
      IsdaCompliantCreditCurve creditCurve = IsdaCompliantCreditCurve.makeFromRT(t, ht);
      IsdaCompliantCurve underlying = creditCurve;
      return new IsdaNodalCurve(underlying, creditCurveInputs.getCurveMetaData());
    }

    public static NodalCurve of(IsdaCreditCurveInputs creditCurveInputs, DoubleArray t, DoubleArray ht) {
      IsdaCompliantCreditCurve creditCurve = IsdaCompliantCreditCurve.makeFromRT(t, ht);
      IsdaCompliantCurve underlying = creditCurve;
      return new IsdaNodalCurve(underlying, creditCurveInputs.getCurveMetaData());
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the scalar PV change to a 1 basis point shift in par interest rates.
   *
   * @param product  expanded CDS product
   * @param yieldCurveInputs  par rate curve points of the ISDA discount curve to use
   * @param creditCurveInputs  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associated with underlying issue or index
   * @param scalingFactor  linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurrencyAmount ir01ParallelPar(
      ResolvedCds product,
      IsdaYieldCurveInputs yieldCurveInputs,
      IsdaCreditCurveInputs creditCurveInputs,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    NodalCurve yieldCurve = IsdaNodalCurve.of(valuationDate, yieldCurveInputs);
    NodalCurve creditCurve = IsdaNodalCurve.of(valuationDate, creditCurveInputs, yieldCurve, recoveryRate);

    NodalCurve bumpedYieldCurve = IsdaNodalCurve.of(valuationDate, yieldCurveInputs.parallelShiftParRatesinBps(ONE_BPS));
    NodalCurve bumpedCreditCurve = IsdaNodalCurve.of(valuationDate, creditCurveInputs, bumpedYieldCurve, recoveryRate);

    CurrencyAmount basePrice = presentValue(
        product, yieldCurve, creditCurve, valuationDate, recoveryRate, scalingFactor);
    CurrencyAmount bumpedPrice = presentValue(
        product, bumpedYieldCurve, bumpedCreditCurve, valuationDate, recoveryRate, scalingFactor);
    return bumpedPrice.minus(basePrice);
  }

  /**
   * Calculates the scalar PV change to a 1 basis point shift in zero rates.
   *
   * @param product  expanded CDS product
   * @param yieldCurveInputs  par rate curve points of the ISDA discount curve to use
   * @param creditCurveInputs  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associated with underlying issue or index
   * @param scalingFactor  linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurrencyAmount ir01ParallelZero(
      ResolvedCds product,
      IsdaYieldCurveInputs yieldCurveInputs,
      IsdaCreditCurveInputs creditCurveInputs,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    NodalCurve yieldCurve = IsdaNodalCurve.of(valuationDate, yieldCurveInputs);
    NodalCurve creditCurve = IsdaNodalCurve.of(valuationDate, creditCurveInputs, yieldCurve, recoveryRate);

    NodalCurve bumpedYieldCurve = yieldCurve.withPerturbation((i, value, meta) -> value + ONE_BPS);
    NodalCurve bumpedCreditCurve = IsdaNodalCurve.of(valuationDate, creditCurveInputs, bumpedYieldCurve, recoveryRate);

    CurrencyAmount basePrice = presentValue(
        product, yieldCurve, creditCurve, valuationDate, recoveryRate, scalingFactor);
    CurrencyAmount bumpedPrice = presentValue(
        product, bumpedYieldCurve, bumpedCreditCurve, valuationDate, recoveryRate, scalingFactor);
    return bumpedPrice.minus(basePrice);
  }

  /**
   * Calculates the vector PV change to a series of 1 basis point shifts in par interest rates at each curve node.
   *
   * @param product  expanded CDS product
   * @param yieldCurveInputs  par rate curve points of the ISDA discount curve to use
   * @param creditCurveInputs  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associated with underlying issue or index
   * @param scalingFactor  linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurrencyParameterSensitivities ir01BucketedPar(
      ResolvedCds product,
      IsdaYieldCurveInputs yieldCurveInputs,
      IsdaCreditCurveInputs creditCurveInputs,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    NodalCurve yieldCurve = IsdaNodalCurve.of(valuationDate, yieldCurveInputs);
    NodalCurve creditCurve = IsdaNodalCurve.of(valuationDate, creditCurveInputs, yieldCurve, recoveryRate);

    int points = yieldCurveInputs.getNumberOfPoints();
    DoubleArray paramSens = DoubleArray.of(points, i -> {
      NodalCurve bumpedYieldCurve = IsdaNodalCurve.of(
          valuationDate, yieldCurveInputs.bucketedShiftParRatesinBps(i, ONE_BPS));
      NodalCurve bumpedCreditCurve = IsdaNodalCurve.of(
          valuationDate, creditCurveInputs, bumpedYieldCurve, recoveryRate);
      CurrencyAmount basePrice = presentValue(
          product, yieldCurve, creditCurve, valuationDate, recoveryRate, scalingFactor);
      CurrencyAmount bumpedPrice = presentValue(
          product, bumpedYieldCurve, bumpedCreditCurve, valuationDate, recoveryRate, scalingFactor);
      CurrencyAmount sensitivity = bumpedPrice.minus(basePrice);
      return sensitivity.getAmount();
    });
    return CurrencyParameterSensitivities.of(
        CurrencyParameterSensitivity.of(yieldCurveInputs.getName(), product.getCurrency(), paramSens));
  }

  /**
   * Calculates the vector PV change to a series of 1 basis point shifts in par interest rates at each curve node.
   *
   * @param product  expanded CDS product
   * @param yieldCurveInputs  par rate curve points of the ISDA discount curve to use
   * @param creditCurveInputs  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associated with underlying issue or index
   * @param scalingFactor  linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurrencyParameterSensitivities ir01BucketedZero(
      ResolvedCds product,
      IsdaYieldCurveInputs yieldCurveInputs,
      IsdaCreditCurveInputs creditCurveInputs,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    NodalCurve yieldCurve = IsdaNodalCurve.of(valuationDate, yieldCurveInputs);
    NodalCurve creditCurve = IsdaNodalCurve.of(valuationDate, creditCurveInputs, yieldCurve, recoveryRate);

    int points = yieldCurveInputs.getNumberOfPoints();
    DoubleArray paramSens = DoubleArray.of(points, i -> {
      DoubleArray shiftVector = yieldCurve.getYValues();
      shiftVector = shiftVector.with(i, shiftVector.get(i) + ONE_BPS);
      NodalCurve bumpedYieldCurve = IsdaNodalCurve.of(yieldCurveInputs, yieldCurve.getXValues(), shiftVector);
      NodalCurve bumpedCreditCurve = IsdaNodalCurve.of(valuationDate, creditCurveInputs, bumpedYieldCurve, recoveryRate);
      CurrencyAmount basePrice = presentValue(
          product, yieldCurve, creditCurve, valuationDate, recoveryRate, scalingFactor);
      CurrencyAmount bumpedPrice = presentValue(
          product, bumpedYieldCurve, bumpedCreditCurve, valuationDate, recoveryRate, scalingFactor);
      CurrencyAmount sensitivity = bumpedPrice.minus(basePrice);
      return sensitivity.getAmount();
    });
    return CurrencyParameterSensitivities.of(
        CurrencyParameterSensitivity.of(yieldCurveInputs.getName(), product.getCurrency(), paramSens));
  }

  //-------------------------------------------------------------------------

  /**
   * Calculates the scalar PV change to a 1 basis point shift in par credit spread rates.
   *
   * @param product  expanded CDS product
   * @param yieldCurveInputs  par rate curve points of the ISDA discount curve to use
   * @param creditCurveInputs  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associate with underlying issue or index
   * @param scalingFactor  linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurrencyAmount cs01ParallelPar(
      ResolvedCds product,
      IsdaYieldCurveInputs yieldCurveInputs,
      IsdaCreditCurveInputs creditCurveInputs,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    NodalCurve yieldCurve = IsdaNodalCurve.of(valuationDate, yieldCurveInputs);
    NodalCurve creditCurve = IsdaNodalCurve.of(valuationDate, creditCurveInputs, yieldCurve, recoveryRate);

    NodalCurve bumpedYieldCurve = yieldCurve;
    NodalCurve bumpedCreditCurve = IsdaNodalCurve.of(
        valuationDate, creditCurveInputs.parallelShiftParRatesinBps(ONE_BPS), bumpedYieldCurve, recoveryRate);

    CurrencyAmount basePrice = presentValue(
        product, yieldCurve, creditCurve, valuationDate, recoveryRate, scalingFactor);
    CurrencyAmount bumpedPrice = presentValue(
        product, bumpedYieldCurve, bumpedCreditCurve, valuationDate, recoveryRate, scalingFactor);
    return bumpedPrice.minus(basePrice);
  }

  /**
   * Calculates the scalar PV change to a 1 basis point shift in hazard rates.
   *
   * @param product  expanded CDS product
   * @param yieldCurveInputs  par rate curve points of the ISDA discount curve to use
   * @param creditCurveInputs  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associated with underlying issue or index
   * @param scalingFactor  linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurrencyAmount cs01ParallelHazard(
      ResolvedCds product,
      IsdaYieldCurveInputs yieldCurveInputs,
      IsdaCreditCurveInputs creditCurveInputs,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    NodalCurve yieldCurve = IsdaNodalCurve.of(valuationDate, yieldCurveInputs);
    NodalCurve creditCurve = IsdaNodalCurve.of(valuationDate, creditCurveInputs, yieldCurve, recoveryRate);

    NodalCurve bumpedYieldCurve = yieldCurve;
    NodalCurve bumpedCreditCurve = creditCurve.withPerturbation((i, value, meta) -> value + ONE_BPS);

    CurrencyAmount basePrice = presentValue(
        product, yieldCurve, creditCurve, valuationDate, recoveryRate, scalingFactor);
    CurrencyAmount bumpedPrice = presentValue(
        product, bumpedYieldCurve, bumpedCreditCurve, valuationDate, recoveryRate, scalingFactor);
    return bumpedPrice.minus(basePrice);
  }

  /**
   * Calculates the vector PV change to a series of 1 basis point shifts in par credit spread rates at each curve node.
   *
   * @param product  expanded CDS product
   * @param yieldCurveInputs  par rate curve points of the ISDA discount curve to use
   * @param creditCurveInputs  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associated with underlying issue or index
   * @param scalingFactor  linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurrencyParameterSensitivities cs01BucketedPar(
      ResolvedCds product,
      IsdaYieldCurveInputs yieldCurveInputs,
      IsdaCreditCurveInputs creditCurveInputs,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    NodalCurve yieldCurve = IsdaNodalCurve.of(valuationDate, yieldCurveInputs);
    NodalCurve creditCurve = IsdaNodalCurve.of(valuationDate, creditCurveInputs, yieldCurve, recoveryRate);

    int points = creditCurveInputs.getNumberOfPoints();
    DoubleArray paramSens = DoubleArray.of(points, i -> {
      NodalCurve bumpedYieldCurve = yieldCurve;
      NodalCurve bumpedCreditCurve = IsdaNodalCurve.of(
          valuationDate, creditCurveInputs.bucketedShiftParRatesinBps(i, ONE_BPS), yieldCurve, recoveryRate);
      CurrencyAmount basePrice = presentValue(
          product, yieldCurve, creditCurve, valuationDate, recoveryRate, scalingFactor);
      CurrencyAmount bumpedPrice = presentValue(
          product, bumpedYieldCurve, bumpedCreditCurve, valuationDate, recoveryRate, scalingFactor);
      CurrencyAmount sensitivity = bumpedPrice.minus(basePrice);
      return sensitivity.getAmount();
    });
    return CurrencyParameterSensitivities.of(
        CurrencyParameterSensitivity.of(creditCurveInputs.getName(), product.getCurrency(), paramSens));
  }

  /**
   * Calculates the vector PV change to a series of 1 basis point shifts in par credit spread rates at each curve node.
   *
   * @param product  expanded CDS product
   * @param yieldCurveInputs  par rate curve points of the ISDA discount curve to use
   * @param creditCurveInputs  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associated with underlying issue or index
   * @param scalingFactor  linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurrencyParameterSensitivities cs01BucketedHazard(
      ResolvedCds product,
      IsdaYieldCurveInputs yieldCurveInputs,
      IsdaCreditCurveInputs creditCurveInputs,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    NodalCurve yieldCurve = IsdaNodalCurve.of(valuationDate, yieldCurveInputs);
    NodalCurve creditCurve = IsdaNodalCurve.of(valuationDate, creditCurveInputs, yieldCurve, recoveryRate);

    int points = creditCurveInputs.getNumberOfPoints();
    DoubleArray paramSens = DoubleArray.of(points, i -> {
      DoubleArray shiftVector = creditCurve.getYValues();
      shiftVector = shiftVector.with(i, shiftVector.get(i) + ONE_BPS);
      NodalCurve bumpedYieldCurve = yieldCurve;
      NodalCurve bumpedCreditCurve = IsdaNodalCurve.of(creditCurveInputs, creditCurve.getXValues(), shiftVector);
      CurrencyAmount basePrice = presentValue(
          product, yieldCurve, creditCurve, valuationDate, recoveryRate, scalingFactor);
      CurrencyAmount bumpedPrice = presentValue(
          product, bumpedYieldCurve, bumpedCreditCurve, valuationDate, recoveryRate, scalingFactor);
      CurrencyAmount sensitivity = bumpedPrice.minus(basePrice);
      return sensitivity.getAmount();
    });
    return CurrencyParameterSensitivities.of(
        CurrencyParameterSensitivity.of(creditCurveInputs.getName(), product.getCurrency(), paramSens));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the scalar PV change to a 1 basis point shift in recovery rate.
   *
   * @param product  expanded CDS product
   * @param yieldCurveInputs  par rate curve points of the ISDA discount curve to use
   * @param creditCurveInputs  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associated with underlying issue or index
   * @param scalingFactor  linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurrencyAmount recovery01(
      ResolvedCds product,
      IsdaYieldCurveInputs yieldCurveInputs,
      IsdaCreditCurveInputs creditCurveInputs,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    CurrencyAmount basePrice = presentValue(product, yieldCurveInputs, creditCurveInputs, valuationDate, recoveryRate,
        scalingFactor);
    CurrencyAmount bumpedPrice = presentValue(product, yieldCurveInputs, creditCurveInputs, valuationDate, recoveryRate +
        ONE_BPS, scalingFactor);
    return bumpedPrice.minus(basePrice);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the risk of default by subtracting from current MTM the Notional amount times Recovery Rate - 1.
   *
   * @param product  expanded CDS product
   * @param yieldCurveInputs  par rate curve points of the ISDA discount curve to use
   * @param creditCurveInputs  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associated with underlying issue or index
   * @param scalingFactor  linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurrencyAmount jumpToDefault(
      ResolvedCds product,
      IsdaYieldCurveInputs yieldCurveInputs,
      IsdaCreditCurveInputs creditCurveInputs,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    CurrencyAmount basePrice = presentValue(product, yieldCurveInputs, creditCurveInputs, valuationDate, recoveryRate,
        scalingFactor);
    CurrencyAmount expectedLoss = CurrencyAmount.of(product.getCurrency(), product.getNotional() * (recoveryRate - 1));
    return expectedLoss.minus(basePrice);
  }

}
