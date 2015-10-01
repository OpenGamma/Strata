/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.credit.ExpandedCds;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.IsdaCreditCurveParRates;
import com.opengamma.strata.market.curve.IsdaYieldCurveParRates;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.CurveUnitParameterSensitivity;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCreditCurve;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCurve;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantYieldCurve;

/**
 * Pricer for for CDS products using the ISDA methodology.
 * <p>
 * This function provides the ability to price a {@link ExpandedCds}.
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
      ExpandedCds product,
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
   * @param yieldCurveParRates  par rate curve points of the ISDA discount curve to use
   * @param creditCurveParRates  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associate with underlying issue or index
   * @param scalingFactor  linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurrencyAmount presentValue(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    NodalCurve yieldCurve = ISDANodalCurve.of(valuationDate, yieldCurveParRates);
    NodalCurve creditCurve = ISDANodalCurve.of(valuationDate, creditCurveParRates, yieldCurve, recoveryRate);

    return IsdaCdsHelper.price(valuationDate, product, yieldCurve, creditCurve, recoveryRate, scalingFactor);
  }

  /**
   * Calculates the par rate of the expanded CDS product.
   * <p>
   * The par rate of the CDS is the coupon rate that will make present value of all cashflows
   * equal zero as of the valuation date.
   *
   * @param product  expanded CDS product
   * @param yieldCurveParRates  par rate curve points of the ISDA discount curve to use
   * @param creditCurveParRates  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associate with underlying issue or index
   * @return par rate for the credit default swap
   */
  public double parRate(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate,
      double recoveryRate) {

    NodalCurve yieldCurve = ISDANodalCurve.of(valuationDate, yieldCurveParRates);
    NodalCurve creditCurve = ISDANodalCurve.of(valuationDate, creditCurveParRates, yieldCurve, recoveryRate);

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
  static class ISDANodalCurve implements NodalCurve {
    private final IsdaCompliantCurve underlyingCurve;
    private final CurveMetadata curveMetadata;

    private ISDANodalCurve(IsdaCompliantCurve underlyingCurve, CurveMetadata curveMetadata) {
      this.underlyingCurve = underlyingCurve;
      this.curveMetadata = curveMetadata;
    }

    @Override
    public double[] getXValues() {
      return underlyingCurve.getT();
    }

    @Override
    public double[] getYValues() {
      return underlyingCurve.getRt();
    }

    @Override
    public NodalCurve withYValues(double[] values) {
      return new ISDANodalCurve(IsdaCompliantCurve.makeFromRT(getXValues().clone(), values.clone()), curveMetadata);
    }

    @Override
    public CurveMetadata getMetadata() {
      return curveMetadata;
    }

    @Override
    public int getParameterCount() {
      return underlyingCurve.getNumberOfKnots();
    }

    @Override
    public double yValue(double x) {
      return underlyingCurve.yValue(x);
    }

    @Override
    public CurveUnitParameterSensitivity yValueParameterSensitivity(double x) {
      return CurveUnitParameterSensitivity.of(curveMetadata, new double[0]);
    }

    @Override
    public double firstDerivative(double x) {
      return 0;
    }

    // bootstraps a yield curve from par rates
    public static NodalCurve of(LocalDate valuationDate, IsdaYieldCurveParRates yieldCurveParRates) {
      IsdaCompliantYieldCurve yieldCurve = IsdaCdsHelper.createIsdaDiscountCurve(valuationDate, yieldCurveParRates);
      IsdaCompliantCurve underlying = yieldCurve;
      return new ISDANodalCurve(underlying, yieldCurveParRates.getCurveMetaData());
    }

    // bootstraps a credit curve from par rates
    public static NodalCurve of(LocalDate valuationDate, IsdaCreditCurveParRates creditCurveParRates, NodalCurve yieldCurve, double recoveryRate) {
      IsdaCompliantCreditCurve creditCurve = IsdaCdsHelper.createIsdaCreditCurve(valuationDate, creditCurveParRates, yieldCurve,
          recoveryRate);
      IsdaCompliantCurve underlying = creditCurve;
      return new ISDANodalCurve(underlying, creditCurveParRates.getCurveMetaData());
    }

    // overwrites the x and y values of a calibrated curve, but copy the curve metadata to the new instance
    public static NodalCurve of(IsdaYieldCurveParRates yieldCurveParRates, double[] t, double[] rt) {
      IsdaCompliantYieldCurve yieldCurve = IsdaCompliantYieldCurve.makeFromRT(t, rt);
      IsdaCompliantCurve underlying = yieldCurve;
      return new ISDANodalCurve(underlying, yieldCurveParRates.getCurveMetaData());
    }

    // overwrites the x and y values of a calibrated curve, but copy the curve metadata to the new instance
    public static NodalCurve of(IsdaCreditCurveParRates creditCurveParRates, double[] t, double[] ht) {
      IsdaCompliantCreditCurve creditCurve = IsdaCompliantCreditCurve.makeFromRT(t, ht);
      IsdaCompliantCurve underlying = creditCurve;
      return new ISDANodalCurve(underlying, creditCurveParRates.getCurveMetaData());
    }

  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the scalar PV change to a 1 basis point shift in par interest rates.
   *
   * @param product  expanded CDS product
   * @param yieldCurveParRates  par rate curve points of the ISDA discount curve to use
   * @param creditCurveParRates  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associated with underlying issue or index
   * @param scalingFactor  linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurrencyAmount ir01ParallelPar(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    NodalCurve yieldCurve = ISDANodalCurve.of(valuationDate, yieldCurveParRates);
    NodalCurve creditCurve = ISDANodalCurve.of(valuationDate, creditCurveParRates, yieldCurve, recoveryRate);

    NodalCurve bumpedYieldCurve = ISDANodalCurve.of(valuationDate, yieldCurveParRates.parallelShiftParRatesinBps(ONE_BPS));
    NodalCurve bumpedCreditCurve = ISDANodalCurve.of(valuationDate, creditCurveParRates, bumpedYieldCurve, recoveryRate);

    CurrencyAmount basePrice = presentValue(product, yieldCurve, creditCurve, valuationDate, recoveryRate, scalingFactor);
    CurrencyAmount bumpedPrice =
        presentValue(product, bumpedYieldCurve, bumpedCreditCurve, valuationDate, recoveryRate, scalingFactor);
    return bumpedPrice.minus(basePrice);
  }

  /**
   * Calculates the scalar PV change to a 1 basis point shift in zero rates.
   *
   * @param product  expanded CDS product
   * @param yieldCurveParRates  par rate curve points of the ISDA discount curve to use
   * @param creditCurveParRates  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associated with underlying issue or index
   * @param scalingFactor  linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurrencyAmount ir01ParallelZero(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    NodalCurve yieldCurve = ISDANodalCurve.of(valuationDate, yieldCurveParRates);
    NodalCurve creditCurve = ISDANodalCurve.of(valuationDate, creditCurveParRates, yieldCurve, recoveryRate);

    NodalCurve bumpedYieldCurve = yieldCurve.shiftedBy((x, y) -> y + ONE_BPS);
    NodalCurve bumpedCreditCurve = ISDANodalCurve.of(valuationDate, creditCurveParRates, bumpedYieldCurve, recoveryRate);

    CurrencyAmount basePrice = presentValue(product, yieldCurve, creditCurve, valuationDate, recoveryRate, scalingFactor);
    CurrencyAmount bumpedPrice =
        presentValue(product, bumpedYieldCurve, bumpedCreditCurve, valuationDate, recoveryRate, scalingFactor);
    return bumpedPrice.minus(basePrice);
  }

  /**
   * Calculates the vector PV change to a series of 1 basis point shifts in par interest rates at each curve node.
   *
   * @param product  expanded CDS product
   * @param yieldCurveParRates  par rate curve points of the ISDA discount curve to use
   * @param creditCurveParRates  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associated with underlying issue or index
   * @param scalingFactor  linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurveCurrencyParameterSensitivities ir01BucketedPar(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    NodalCurve yieldCurve = ISDANodalCurve.of(valuationDate, yieldCurveParRates);
    NodalCurve creditCurve = ISDANodalCurve.of(valuationDate, creditCurveParRates, yieldCurve, recoveryRate);

    int points = yieldCurveParRates.getNumberOfPoints();
    double[] paramSensitivities = new double[points];
    for (int i = 0; i < points; i++) {
      NodalCurve bumpedYieldCurve = ISDANodalCurve.of(valuationDate, yieldCurveParRates.bucketedShiftParRatesinBps(i, ONE_BPS));
      NodalCurve bumpedCreditCurve = ISDANodalCurve.of(valuationDate, creditCurveParRates, bumpedYieldCurve, recoveryRate);
      CurrencyAmount basePrice = presentValue(product, yieldCurve, creditCurve, valuationDate, recoveryRate, scalingFactor);
      CurrencyAmount bumpedPrice =
          presentValue(product, bumpedYieldCurve, bumpedCreditCurve, valuationDate, recoveryRate, scalingFactor);
      CurrencyAmount sensitivity = bumpedPrice.minus(basePrice);
      paramSensitivities[i] = sensitivity.getAmount();
    }
    return CurveCurrencyParameterSensitivities.of(
        CurveCurrencyParameterSensitivity.of(yieldCurveParRates.getCurveMetaData(), product.getCurrency(), paramSensitivities));
  }

  /**
   * Calculates the vector PV change to a series of 1 basis point shifts in par interest rates at each curve node.
   *
   * @param product  expanded CDS product
   * @param yieldCurveParRates  par rate curve points of the ISDA discount curve to use
   * @param creditCurveParRates  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associated with underlying issue or index
   * @param scalingFactor  linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurveCurrencyParameterSensitivities ir01BucketedZero(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    NodalCurve yieldCurve = ISDANodalCurve.of(valuationDate, yieldCurveParRates);
    NodalCurve creditCurve = ISDANodalCurve.of(valuationDate, creditCurveParRates, yieldCurve, recoveryRate);

    int points = yieldCurveParRates.getNumberOfPoints();
    double[] paramSensitivities = new double[points];
    for (int i = 0; i < points; i++) {
      double[] shiftVector = yieldCurve.getYValues().clone();
      shiftVector[i] = shiftVector[i] + ONE_BPS;
      NodalCurve bumpedYieldCurve = ISDANodalCurve.of(yieldCurveParRates, yieldCurve.getXValues(), shiftVector);
      NodalCurve bumpedCreditCurve = ISDANodalCurve.of(valuationDate, creditCurveParRates, bumpedYieldCurve, recoveryRate);
      CurrencyAmount basePrice = presentValue(product, yieldCurve, creditCurve, valuationDate, recoveryRate, scalingFactor);
      CurrencyAmount bumpedPrice = presentValue(product, bumpedYieldCurve, bumpedCreditCurve, valuationDate, recoveryRate,
          scalingFactor);
      CurrencyAmount sensitivity = bumpedPrice.minus(basePrice);
      paramSensitivities[i] = sensitivity.getAmount();
    }
    return CurveCurrencyParameterSensitivities.of(
        CurveCurrencyParameterSensitivity.of(yieldCurveParRates.getCurveMetaData(), product.getCurrency(), paramSensitivities));
  }

  //-------------------------------------------------------------------------

  /**
   * Calculates the scalar PV change to a 1 basis point shift in par credit spread rates.
   *
   * @param product  expanded CDS product
   * @param yieldCurveParRates  par rate curve points of the ISDA discount curve to use
   * @param creditCurveParRates  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associate with underlying issue or index
   * @param scalingFactor  linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurrencyAmount cs01ParallelPar(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    NodalCurve yieldCurve = ISDANodalCurve.of(valuationDate, yieldCurveParRates);
    NodalCurve creditCurve = ISDANodalCurve.of(valuationDate, creditCurveParRates, yieldCurve, recoveryRate);

    NodalCurve bumpedYieldCurve = yieldCurve;
    NodalCurve bumpedCreditCurve = ISDANodalCurve.of(valuationDate, creditCurveParRates.parallelShiftParRatesinBps(ONE_BPS),
        bumpedYieldCurve, recoveryRate);

    CurrencyAmount basePrice = presentValue(product, yieldCurve, creditCurve, valuationDate, recoveryRate, scalingFactor);
    CurrencyAmount bumpedPrice = presentValue(product, bumpedYieldCurve, bumpedCreditCurve, valuationDate, recoveryRate,
        scalingFactor);
    return bumpedPrice.minus(basePrice);
  }

  /**
   * Calculates the scalar PV change to a 1 basis point shift in hazard rates.
   *
   * @param product  expanded CDS product
   * @param yieldCurveParRates  par rate curve points of the ISDA discount curve to use
   * @param creditCurveParRates  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associated with underlying issue or index
   * @param scalingFactor  linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurrencyAmount cs01ParallelHazard(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    NodalCurve yieldCurve = ISDANodalCurve.of(valuationDate, yieldCurveParRates);
    NodalCurve creditCurve = ISDANodalCurve.of(valuationDate, creditCurveParRates, yieldCurve, recoveryRate);

    NodalCurve bumpedYieldCurve = yieldCurve;
    NodalCurve bumpedCreditCurve = creditCurve.shiftedBy((x, y) -> y + ONE_BPS);

    CurrencyAmount basePrice = presentValue(product, yieldCurve, creditCurve, valuationDate, recoveryRate, scalingFactor);
    CurrencyAmount bumpedPrice = presentValue(product, bumpedYieldCurve, bumpedCreditCurve, valuationDate, recoveryRate,
        scalingFactor);
    return bumpedPrice.minus(basePrice);
  }

  /**
   * Calculates the vector PV change to a series of 1 basis point shifts in par credit spread rates at each curve node.
   *
   * @param product  expanded CDS product
   * @param yieldCurveParRates  par rate curve points of the ISDA discount curve to use
   * @param creditCurveParRates  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associated with underlying issue or index
   * @param scalingFactor  linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurveCurrencyParameterSensitivities cs01BucketedPar(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    NodalCurve yieldCurve = ISDANodalCurve.of(valuationDate, yieldCurveParRates);
    NodalCurve creditCurve = ISDANodalCurve.of(valuationDate, creditCurveParRates, yieldCurve, recoveryRate);

    int points = creditCurveParRates.getNumberOfPoints();
    double[] paramSensitivities = new double[points];
    for (int i = 0; i < points; i++) {
      NodalCurve bumpedYieldCurve = yieldCurve;
      NodalCurve bumpedCreditCurve = ISDANodalCurve.of(valuationDate, creditCurveParRates.bucketedShiftParRatesinBps(i, ONE_BPS),
          yieldCurve, recoveryRate);
      CurrencyAmount basePrice = presentValue(product, yieldCurve, creditCurve, valuationDate, recoveryRate, scalingFactor);
      CurrencyAmount bumpedPrice = presentValue(product, bumpedYieldCurve, bumpedCreditCurve, valuationDate, recoveryRate,
          scalingFactor);
      CurrencyAmount sensitivity = bumpedPrice.minus(basePrice);
      paramSensitivities[i] = sensitivity.getAmount();
    }
    return CurveCurrencyParameterSensitivities.of(
        CurveCurrencyParameterSensitivity.of(creditCurveParRates.getCurveMetaData(), product.getCurrency(), paramSensitivities));
  }

  /**
   * Calculates the vector PV change to a series of 1 basis point shifts in par credit spread rates at each curve node.
   *
   * @param product  expanded CDS product
   * @param yieldCurveParRates  par rate curve points of the ISDA discount curve to use
   * @param creditCurveParRates  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associated with underlying issue or index
   * @param scalingFactor  linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurveCurrencyParameterSensitivities cs01BucketedHazard(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    NodalCurve yieldCurve = ISDANodalCurve.of(valuationDate, yieldCurveParRates);
    NodalCurve creditCurve = ISDANodalCurve.of(valuationDate, creditCurveParRates, yieldCurve, recoveryRate);

    int points = creditCurveParRates.getNumberOfPoints();
    double[] paramSensitivities = new double[points];
    for (int i = 0; i < points; i++) {
      double[] shiftVector = creditCurve.getYValues().clone();
      shiftVector[i] = shiftVector[i] + ONE_BPS;
      NodalCurve bumpedYieldCurve = yieldCurve;
      NodalCurve bumpedCreditCurve = ISDANodalCurve.of(creditCurveParRates, creditCurve.getXValues(), shiftVector);
      CurrencyAmount basePrice = presentValue(product, yieldCurve, creditCurve, valuationDate, recoveryRate, scalingFactor);
      CurrencyAmount bumpedPrice = presentValue(product, bumpedYieldCurve, bumpedCreditCurve, valuationDate, recoveryRate,
          scalingFactor);
      CurrencyAmount sensitivity = bumpedPrice.minus(basePrice);
      paramSensitivities[i] = sensitivity.getAmount();
    }
    return CurveCurrencyParameterSensitivities.of(
        CurveCurrencyParameterSensitivity.of(creditCurveParRates.getCurveMetaData(), product.getCurrency(), paramSensitivities));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the scalar PV change to a 1 basis point shift in recovery rate.
   *
   * @param product  expanded CDS product
   * @param yieldCurveParRates  par rate curve points of the ISDA discount curve to use
   * @param creditCurveParRates  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associated with underlying issue or index
   * @param scalingFactor  linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurrencyAmount recovery01(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    CurrencyAmount basePrice = presentValue(product, yieldCurveParRates, creditCurveParRates, valuationDate, recoveryRate,
        scalingFactor);
    CurrencyAmount bumpedPrice = presentValue(product, yieldCurveParRates, creditCurveParRates, valuationDate, recoveryRate +
        ONE_BPS, scalingFactor);
    return bumpedPrice.minus(basePrice);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the risk of default by subtracting from current MTM the Notional amount times Recovery Rate - 1.
   *
   * @param product  expanded CDS product
   * @param yieldCurveParRates  par rate curve points of the ISDA discount curve to use
   * @param creditCurveParRates  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @param recoveryRate  recovery rate associated with underlying issue or index
   * @param scalingFactor  linear scaling factor associated with underlying index, or 1 in case of CDS
   * @return present value of fee leg and any up front fee
   */
  public CurrencyAmount jumpToDefault(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    CurrencyAmount basePrice = presentValue(product, yieldCurveParRates, creditCurveParRates, valuationDate, recoveryRate,
        scalingFactor);
    CurrencyAmount expectedLoss = CurrencyAmount.of(product.getCurrency(), product.getNotional() * (recoveryRate - 1));
    return expectedLoss.minus(basePrice);
  }

}
