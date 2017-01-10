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
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.pricer.common.PriceType;
import com.opengamma.strata.product.credit.ResolvedCds;
import com.opengamma.strata.product.credit.ResolvedCdsTrade;

/**
 * Finite difference spread sensitivity calculator. 
 * <p>
 * This computes the present value sensitivity to par spreads of bucketed CDSs by bump-and-reprice, i.e., 
 * finite difference method. 
 */
public class FiniteDifferenceSpreadSensitivityCalculator extends SpreadSensitivityCalculator {

  /**
   * Default implementation.
   * <p>
   * The bump amount is one basis point.
   */
  public static final FiniteDifferenceSpreadSensitivityCalculator DEFAULT =
      new FiniteDifferenceSpreadSensitivityCalculator(AccrualOnDefaultFormula.ORIGINAL_ISDA, 1.0e-4);
  /**
   * The bump amount for the finite difference method.
   * <p>
   * The magnitude of the bump amount must be greater than 1e-10. 
   * However, this bound does not guarantee that the finite difference calculation produces reliable numbers.
   */
  private final double bumpAmount;

  /**
   * Constructor with accrual-on-default formula and bump amount specified.
   * 
   * @param formula  the formula
   * @param bumpAmount  the bump amount
   */
  public FiniteDifferenceSpreadSensitivityCalculator(AccrualOnDefaultFormula formula, double bumpAmount) {
    super(formula);
    this.bumpAmount = ArgChecker.notZero(bumpAmount, 1.0e-10, "bumpAmount");
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyAmount parallelCs01(
      ResolvedCdsTrade trade,
      List<ResolvedCdsTrade> bucketCds,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    checkCdsBucket(trade, bucketCds);
    ResolvedCds product = trade.getProduct();
    Currency currency = product.getCurrency();
    StandardId legalEntityId = product.getLegalEntityId();
    LocalDate valuationDate = ratesProvider.getValuationDate();
    ImmutableCreditRatesProvider immutableRatesProvider = ratesProvider.toImmutableCreditRatesProvider();

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
    Pair<StandardId, Currency> lePair = Pair.of(legalEntityId, currency);

    IsdaCreditDiscountFactors df = IsdaCreditDiscountFactors.of(currency, valuationDate, creditCurveBase);
    CreditRatesProvider ratesProviderBase = immutableRatesProvider.toBuilder()
        .creditCurves(ImmutableMap.of(lePair, LegalEntitySurvivalProbabilities.of(legalEntityId, df)))
        .build();
    CurrencyAmount pvBase = getPricer().presentValueOnSettle(trade, ratesProviderBase, PriceType.DIRTY, refData);

    DoubleArray bumpedSp = DoubleArray.of(nBucket, i -> impSp.get(i) + bumpAmount);
    NodalCurve creditCurveBump = getCalibrator().calibrate(
        bucketCds,
        bumpedSp,
        DoubleArray.filled(nBucket),
        CurveName.of("bumpedImpliedCreditCurve"),
        valuationDate,
        ratesProvider.discountFactors(currency),
        ratesProvider.recoveryRates(legalEntityId),
        refData);
    IsdaCreditDiscountFactors dfBump = IsdaCreditDiscountFactors.of(currency, valuationDate, creditCurveBump);
    CreditRatesProvider ratesProviderBump = immutableRatesProvider.toBuilder()
        .creditCurves(
            ImmutableMap.of(lePair, LegalEntitySurvivalProbabilities.of(legalEntityId, dfBump)))
        .build();
    CurrencyAmount pvBumped = getPricer().presentValueOnSettle(trade, ratesProviderBump, PriceType.DIRTY, refData);

    return CurrencyAmount.of(currency, (pvBumped.getAmount() - pvBase.getAmount()) / bumpAmount);
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
    ImmutableCreditRatesProvider immutableRatesProvider = ratesProvider.toImmutableCreditRatesProvider();

    int nBucket = bucketCds.size();
    double[] res = new double[nBucket];
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
    Pair<StandardId, Currency> lePair = Pair.of(legalEntityId, currency);

    IsdaCreditDiscountFactors df = IsdaCreditDiscountFactors.of(currency, valuationDate, creditCurveBase);
    CreditRatesProvider ratesProviderBase = immutableRatesProvider.toBuilder()
        .creditCurves(ImmutableMap.of(lePair, LegalEntitySurvivalProbabilities.of(legalEntityId, df)))
        .build();
    double pvBase = getPricer().presentValueOnSettle(trade, ratesProviderBase, PriceType.DIRTY, refData).getAmount();
    for (int i = 0; i < nBucket; ++i) {
      double[] bumpedSp = impSp.toArray();
      bumpedSp[i] += bumpAmount;
      NodalCurve creditCurveBump = getCalibrator().calibrate(
          bucketCds,
          DoubleArray.ofUnsafe(bumpedSp),
          DoubleArray.filled(nBucket),
          CurveName.of("bumpedImpliedCreditCurve"),
          valuationDate,
          ratesProvider.discountFactors(currency),
          ratesProvider.recoveryRates(legalEntityId),
          refData);
      IsdaCreditDiscountFactors dfBump = IsdaCreditDiscountFactors.of(currency, valuationDate, creditCurveBump);
      CreditRatesProvider ratesProviderBump = immutableRatesProvider.toBuilder()
          .creditCurves(ImmutableMap.of(lePair, LegalEntitySurvivalProbabilities.of(legalEntityId, dfBump)))
          .build();
      double pvBumped = getPricer().presentValueOnSettle(trade, ratesProviderBump, PriceType.DIRTY, refData).getAmount();
      res[i] = (pvBumped - pvBase) / bumpAmount;
    }
    return DoubleArray.ofUnsafe(res);
  }

}
