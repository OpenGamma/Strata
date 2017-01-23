/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import java.time.LocalDate;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.math.impl.linearalgebra.LUDecompositionCommons;
import com.opengamma.strata.math.impl.linearalgebra.LUDecompositionResult;
import com.opengamma.strata.math.impl.matrix.CommonsMatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.product.credit.ResolvedCds;
import com.opengamma.strata.product.credit.ResolvedCdsTrade;

/**
 * Analytic spread sensitivity calculator.
 * <p>
 * This analytically computes the present value sensitivity to par spreads of bucketed CDSs. 
 */
public class AnalyticSpreadSensitivityCalculator
    extends SpreadSensitivityCalculator {

  /**
   * Default implementation.
   */
  public static final AnalyticSpreadSensitivityCalculator DEFAULT =
      new AnalyticSpreadSensitivityCalculator(AccrualOnDefaultFormula.ORIGINAL_ISDA);

  /**
   * The matrix algebra used for matrix inversion.
   */
  private static final MatrixAlgebra MATRIX_ALGEBRA = new CommonsMatrixAlgebra();
  /**
   * LU decomposition.
   */
  private static final LUDecompositionCommons DECOMPOSITION = new LUDecompositionCommons();

  /**
   * Constructor with the accrual-on-default formula specified.
   * 
   * @param formula  the formula
   */
  public AnalyticSpreadSensitivityCalculator(AccrualOnDefaultFormula formula) {
    super(formula);
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyAmount parallelCs01(
      ResolvedCdsTrade trade,
      List<ResolvedCdsTrade> bucketCds,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    DoubleArray temp = computedBucketedCs01(trade, bucketCds, ratesProvider, refData);
    return CurrencyAmount.of(trade.getProduct().getCurrency(), temp.sum());
  }

  @Override
  DoubleArray computedBucketedCs01(
      ResolvedCdsTrade trade,
      List<ResolvedCdsTrade> bucketCds,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    checkCdsBucket(trade, bucketCds);
    ResolvedCds product = trade.getProduct();
    Currency currency = product.getCurrency();
    StandardId legalEntityId = product.getLegalEntityId();
    LocalDate valuationDate = ratesProvider.getValuationDate();

    int nBucket = bucketCds.size();
    DoubleArray impSp = impliedSpread(bucketCds, ratesProvider, refData);
    NodalCurve creditCurveBase = getCalibrator().calibrate(
        bucketCds,
        impSp,
        DoubleArray.filled(nBucket),
        CurveName.of("baseImpliedCreditCurve"),
        valuationDate,
        ratesProvider.discountFactors(currency),
        ratesProvider.recoveryRates(legalEntityId),
        refData);
    IsdaCreditDiscountFactors df = IsdaCreditDiscountFactors.of(currency, valuationDate, creditCurveBase);
    CreditRatesProvider ratesProviderBase = ratesProvider.toImmutableCreditRatesProvider().toBuilder()
        .creditCurves(ImmutableMap.of(Pair.of(legalEntityId, currency), LegalEntitySurvivalProbabilities.of(legalEntityId, df)))
        .build();

    double[][] res = new double[nBucket][];
    PointSensitivities pointPv = getPricer().presentValueOnSettleSensitivity(trade, ratesProviderBase, refData);
    DoubleArray vLambda =
        ratesProviderBase.singleCreditCurveParameterSensitivity(pointPv, legalEntityId, currency).getSensitivity();
    for (int i = 0; i < nBucket; i++) {
      PointSensitivities pointSp = getPricer().parSpreadSensitivity(bucketCds.get(i), ratesProviderBase, refData);
      res[i] = ratesProviderBase.singleCreditCurveParameterSensitivity(pointSp, legalEntityId, currency)
          .getSensitivity().toArray();
    }
    DoubleMatrix jacT = MATRIX_ALGEBRA.getTranspose(DoubleMatrix.ofUnsafe(res));
    LUDecompositionResult luRes = DECOMPOSITION.apply(jacT);
    DoubleArray vS = luRes.solve(vLambda);
    return vS;
  }

}
