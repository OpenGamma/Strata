/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import com.google.common.collect.Lists;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.finance.credit.ExpandedCds;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.IsdaCreditCurveParRates;
import com.opengamma.strata.market.curve.IsdaYieldCurveParRates;
import com.opengamma.strata.market.curve.TenorCurveNodeMetadata;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivity;

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
   * @param yieldCurveParRates  par rate curve points of the ISDA discount curve to use
   * @param creditCurveParRates  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @return present value of fee leg and any up front fee
   */
  public CurrencyAmount presentValue(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate) {

    double recoveryRate = creditCurveParRates.getRecoveryRate();
    return IsdaCdsHelper.price(valuationDate, product, yieldCurveParRates, creditCurveParRates, recoveryRate);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the scalar PV change to a 1 basis point shift in par interest rates.
   *
   * @param product  expanded CDS product
   * @param yieldCurveParRates  par rate curve points of the ISDA discount curve to use
   * @param creditCurveParRates  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @return present value of fee leg and any up front fee
   */
  public CurrencyAmount ir01ParallelPar(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate) {

    CurrencyAmount basePrice = presentValue(product, yieldCurveParRates, creditCurveParRates, valuationDate);
    IsdaYieldCurveParRates bumpedCurve = yieldCurveParRates.parallelShiftParRatesinBps(ONE_BPS);
    CurrencyAmount bumpedPrice = presentValue(product, bumpedCurve, creditCurveParRates, valuationDate);
    return bumpedPrice.minus(basePrice);
  }

  /**
   * Calculates the vector PV change to a series of 1 basis point shifts in par interest rates at each curve node.
   *
   * @param product  expanded CDS product
   * @param yieldCurveParRates  par rate curve points of the ISDA discount curve to use
   * @param creditCurveParRates  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @return present value of fee leg and any up front fee
   */
  public CurveCurrencyParameterSensitivities ir01BucketedPar(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate) {

    CurrencyAmount basePrice = presentValue(product, yieldCurveParRates, creditCurveParRates, valuationDate);
    int points = yieldCurveParRates.getNumberOfPoints();
    double[] paramSensitivities = new double[points];
    List<TenorCurveNodeMetadata> metaData = Lists.newArrayList();
    for (int i = 0; i < points; i++) {
      IsdaYieldCurveParRates bumpedCurve = yieldCurveParRates.bucketedShiftParRatesinBps(i, ONE_BPS);
      CurrencyAmount bumpedPrice = presentValue(product, bumpedCurve, creditCurveParRates, valuationDate);
      CurrencyAmount sensitivity = bumpedPrice.minus(basePrice);
      paramSensitivities[i] = sensitivity.getAmount();
      Period period = yieldCurveParRates.getYieldCurvePoints()[i];
      LocalDate pointDate = valuationDate.plus(period);
      metaData.add(TenorCurveNodeMetadata.of(pointDate, Tenor.of(period)));
    }
    // ISDA model uses ACT_365F
    CurveMetadata curveMetadata = Curves.zeroRates(yieldCurveParRates.getName(), ACT_365F, metaData);
    return CurveCurrencyParameterSensitivities.of(
        CurveCurrencyParameterSensitivity.of(curveMetadata, product.getCurrency(), paramSensitivities));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the scalar PV change to a 1 basis point shift in par credit spread rates.
   *
   * @param product  expanded CDS product
   * @param yieldCurveParRates  par rate curve points of the ISDA discount curve to use
   * @param creditCurveParRates  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @return present value of fee leg and any up front fee
   */
  public CurrencyAmount cs01ParallelPar(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate) {

    CurrencyAmount basePrice = presentValue(product, yieldCurveParRates, creditCurveParRates, valuationDate);
    IsdaCreditCurveParRates bumpedCurve = creditCurveParRates.parallelShiftParRatesinBps(ONE_BPS);
    CurrencyAmount bumpedPrice = presentValue(product, yieldCurveParRates, bumpedCurve, valuationDate);
    return bumpedPrice.minus(basePrice);
  }

  /**
   * Calculates the vector PV change to a series of 1 basis point shifts in par credit spread rates at each curve node.
   *
   * @param product  expanded CDS product
   * @param yieldCurveParRates  par rate curve points of the ISDA discount curve to use
   * @param creditCurveParRates  par spread rate curve points of the ISDA spread curve to use
   * @param valuationDate  date to use when calibrating curves and calculating the result
   * @return present value of fee leg and any up front fee
   */
  public CurveCurrencyParameterSensitivities cs01BucketedPar(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate) {

    CurrencyAmount basePrice = presentValue(product, yieldCurveParRates, creditCurveParRates, valuationDate);
    int points = creditCurveParRates.getNumberOfPoints();
    double[] paramSensitivities = new double[points];
    List<TenorCurveNodeMetadata> metaData = Lists.newArrayList();
    for (int i = 0; i < points; i++) {
      IsdaCreditCurveParRates bumpedCurve = creditCurveParRates.bucketedShiftParRatesinBps(i, ONE_BPS);
      CurrencyAmount bumpedPrice = presentValue(product, yieldCurveParRates, bumpedCurve, valuationDate);
      CurrencyAmount sensitivity = bumpedPrice.minus(basePrice);
      paramSensitivities[i] = sensitivity.getAmount();
      Period period = creditCurveParRates.getCreditCurvePoints()[i];
      LocalDate pointDate = valuationDate.plus(period);
      metaData.add(TenorCurveNodeMetadata.of(pointDate, Tenor.of(period)));
    }
    CurveMetadata curveMetadata = Curves.isdaCredit(yieldCurveParRates.getName(), metaData);
    return CurveCurrencyParameterSensitivities.of(
        CurveCurrencyParameterSensitivity.of(curveMetadata, product.getCurrency(), paramSensitivities));
  }

}
