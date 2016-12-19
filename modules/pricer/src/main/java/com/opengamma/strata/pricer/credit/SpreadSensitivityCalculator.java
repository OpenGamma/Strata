/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.product.credit.ResolvedCds;
import com.opengamma.strata.product.credit.ResolvedCdsIndexTrade;
import com.opengamma.strata.product.credit.ResolvedCdsTrade;

/**
 * The spread sensitivity calculator. 
 * <p>
 * The spread sensitivity, also called CS01, is the sensitivity of the CDS product present value to par spreads of the bucket CDSs. 
 * The bucket CDSs do not necessarily correspond to the node point of the input credit curve.
 */
abstract class SpreadSensitivityCalculator {

  /**
   * The trade pricer.
   */
  protected final IsdaCdsTradePricer pricer;
  /**
   * The credit curve calibrator.
   */
  protected final IsdaCompliantCreditCurveCalibrator calibrator;

  public SpreadSensitivityCalculator(AccrualOnDefaultFormula formula) {
    this.pricer = new IsdaCdsTradePricer(formula);
    this.calibrator = new FastCreditCurveCalibrator(formula);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes parallel CS01 for CDS. 
   * <p>
   * The relevant credit curve must be stored in {@code RatesProvider}.
   * 
   * @param trade  the trade
   * @param bucketCds  the CDS bucket
   * @param ratesProvider  the rates provider
   * @param refData  the reference data
   * @return the parallel CS01
   */
  public abstract CurrencyAmount parallelCs01(
      ResolvedCdsTrade trade,
      List<ResolvedCdsTrade> bucketCds,
      CreditRatesProvider ratesProvider,
      ReferenceData refData);

  /**
   * Computes bucketed CS01 for CDS.
   * <p>
   * The relevant credit curve must be stored in {@code RatesProvider}.
   * 
   * @param trade  the trade
   * @param bucketCds  the CDS bucket
   * @param ratesProvider  the rates provider
   * @param refData  the reference data
   * @return the bucketed CS01
   */
  public CurrencyParameterSensitivity bucketedCs01(
      ResolvedCdsTrade trade,
      List<ResolvedCdsTrade> bucketCds,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    DoubleArray sensiValue = computedBucketedCs01(trade, bucketCds, ratesProvider, refData);
    return CurrencyParameterSensitivity.of(CurveName.of("impliedSpreads"), trade.getProduct().getCurrency(), sensiValue);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes parallel CS01 for CDS index using a single credit curve. 
   * <p>
   * This is coherent to the pricer {@link IsdaHomogenousCdsIndexTradePricer}.
   * The relevant credit curve must be stored in {@code RatesProvider}.
   * 
   * @param trade  the trade
   * @param bucketCdsIndex  the CDS index bucket
   * @param ratesProvider  the rates provider
   * @param refData  the reference data
   * @return the parallel CS01
   */
  public CurrencyAmount parallelCs01(
      ResolvedCdsIndexTrade trade,
      List<ResolvedCdsIndexTrade> bucketCdsIndex,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    ResolvedCdsTrade cdsTrade = trade.toSingleNameCds();
    List<ResolvedCdsTrade> bucketCds = bucketCdsIndex.stream()
        .map(ResolvedCdsIndexTrade::toSingleNameCds)
        .collect(Collectors.toList());
    CurrencyAmount cs01Cds = parallelCs01(cdsTrade, bucketCds, ratesProvider, refData);
    double indexFactor = getIndexFactor(cdsTrade.getProduct(), ratesProvider);
    return cs01Cds.multipliedBy(indexFactor);
  }

  /**
   * Computes bucketed CS01 for CDS index using a single credit curve.
   * <p>
   * This is coherent to the pricer {@link IsdaHomogenousCdsIndexTradePricer}.
   * The relevant credit curve must be stored in {@code RatesProvider}.
   * 
   * @param trade  the trade
   * @param bucketCdsIndex  the CDS index bucket
   * @param ratesProvider  the rates provider
   * @param refData  the reference data
   * @return the bucketed CS01
   */
  public CurrencyParameterSensitivity bucketedCs01(
      ResolvedCdsIndexTrade trade,
      List<ResolvedCdsIndexTrade> bucketCdsIndex,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    ResolvedCdsTrade cdsTrade = trade.toSingleNameCds();
    List<ResolvedCdsTrade> bucketCds = bucketCdsIndex.stream()
        .map(ResolvedCdsIndexTrade::toSingleNameCds)
        .collect(Collectors.toList());
    CurrencyParameterSensitivity bucketedCs01 = bucketedCs01(cdsTrade, bucketCds, ratesProvider, refData);
    double indexFactor = getIndexFactor(cdsTrade.getProduct(), ratesProvider);
    return bucketedCs01.multipliedBy(indexFactor);
  }

  //-------------------------------------------------------------------------
  // internal bucketed CS01 computation
  abstract DoubleArray computedBucketedCs01(
      ResolvedCdsTrade trade,
      List<ResolvedCdsTrade> bucketCds,
      CreditRatesProvider ratesProvider,
      ReferenceData refData);

  // check legal entity and currency are common for all of the CDSs
  protected void checkCdsBucket(ResolvedCdsTrade trade, List<ResolvedCdsTrade> bucketCds) {
    Iterator<StandardId> legalEntities =
        bucketCds.stream().map(t -> t.getProduct().getLegalEntityId()).collect(Collectors.toSet()).iterator();
    ArgChecker.isTrue(legalEntities.next().equals(trade.getProduct().getLegalEntityId()), "legal entity must be common");
    ArgChecker.isFalse(legalEntities.hasNext(), "legal entity must be common");
    Iterator<Currency> currencies =
        bucketCds.stream().map(t -> t.getProduct().getCurrency()).collect(Collectors.toSet()).iterator();
    ArgChecker.isTrue(currencies.next().equals(trade.getProduct().getCurrency()), "currency must be common");
    ArgChecker.isFalse(currencies.hasNext(), "currency must be common");
  }

  protected double[] impliedSpread(List<ResolvedCdsTrade> bucketCds, CreditRatesProvider ratesProvider, ReferenceData refData) {
    int n = bucketCds.size();
    double[] impSp = new double[n];
    for (int i = 0; i < n; ++i) {
      impSp[i] = pricer.parSpread(bucketCds.get(i), ratesProvider, refData);
    }
    return impSp;
  }

  private double getIndexFactor(ResolvedCds cds, CreditRatesProvider ratesProvider) {
    LegalEntitySurvivalProbabilities survivalProbabilities =
        ratesProvider.survivalProbabilities(cds.getLegalEntityId(), cds.getCurrency());
    // instance is checked in pricer
    double indexFactor = ((IsdaCompliantZeroRateDiscountFactors) survivalProbabilities.getSurvivalProbabilities())
        .getCurve().getMetadata().getInfo(CurveInfoType.CDS_INDEX_FACTOR);
    return indexFactor;
  }

}
