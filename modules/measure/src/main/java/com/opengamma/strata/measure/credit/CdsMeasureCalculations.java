/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.credit;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.scenario.CurrencyValuesArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.data.scenario.ValuesArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.pricer.credit.CdsRecoveryRate;
import com.opengamma.strata.pricer.credit.IsdaCdsPricer;
import com.opengamma.strata.pricer.credit.IsdaCreditCurveInputs;
import com.opengamma.strata.pricer.credit.IsdaIndexCreditCurveInputsId;
import com.opengamma.strata.pricer.credit.IsdaIndexRecoveryRateId;
import com.opengamma.strata.pricer.credit.IsdaSingleNameCreditCurveInputsId;
import com.opengamma.strata.pricer.credit.IsdaSingleNameRecoveryRateId;
import com.opengamma.strata.pricer.credit.IsdaYieldCurveInputs;
import com.opengamma.strata.pricer.credit.IsdaYieldCurveInputsId;
import com.opengamma.strata.product.credit.IndexReferenceInformation;
import com.opengamma.strata.product.credit.ReferenceInformation;
import com.opengamma.strata.product.credit.ResolvedCds;
import com.opengamma.strata.product.credit.ResolvedCdsTrade;
import com.opengamma.strata.product.credit.SingleNameReferenceInformation;

/**
 * Multi-scenario measure calculations for CSD trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class CdsMeasureCalculations {

  /**
   * The pricer to use.
   */
  private static final IsdaCdsPricer PRICER = IsdaCdsPricer.DEFAULT;

  // restricted constructor
  private CdsMeasureCalculations() {
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  static CurrencyValuesArray presentValue(
      ResolvedCdsTrade trade,
      ScenarioMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePresentValue(trade, marketData.scenario(i)));
  }

  // present value for one scenario
  private static CurrencyAmount calculatePresentValue(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsId.of(product.getCurrency()));
    IsdaCreditCurveInputs creditCurveInputs = creditCurveInputs(trade, marketData);
    double recoveryRate = cdsRecoveryRate(trade, marketData).getRecoveryRate();
    return PRICER.presentValue(
        product,
        yieldCurveInputs,
        creditCurveInputs,
        marketData.getValuationDate(),
        recoveryRate,
        creditCurveInputs.getScalingFactor());
  }

  //-------------------------------------------------------------------------
  // calculates IR01 for all scenarios
  static CurrencyValuesArray ir01ParallelZero(
      ResolvedCdsTrade trade,
      ScenarioMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateIr01ParallelZero(trade, marketData.scenario(i)));
  }

  // IR01 for one scenario
  private static CurrencyAmount calculateIr01ParallelZero(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsId.of(product.getCurrency()));
    IsdaCreditCurveInputs creditCurveInputs = creditCurveInputs(trade, marketData);
    double recoveryRate = cdsRecoveryRate(trade, marketData).getRecoveryRate();
    return PRICER.ir01ParallelZero(
        product,
        yieldCurveInputs,
        creditCurveInputs,
        marketData.getValuationDate(),
        recoveryRate,
        creditCurveInputs.getScalingFactor());
  }

  //-------------------------------------------------------------------------
  // calculates bucketed IR01 for all scenarios
  static ScenarioArray<CurrencyParameterSensitivities> ir01BucketedZero(
      ResolvedCdsTrade trade,
      ScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> calculateIr01BucketedZero(trade, marketData.scenario(i)));
  }

  // bucketed IR01 for one scenario
  private static CurrencyParameterSensitivities calculateIr01BucketedZero(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsId.of(product.getCurrency()));
    IsdaCreditCurveInputs creditCurveInputs = creditCurveInputs(trade, marketData);
    double recoveryRate = cdsRecoveryRate(trade, marketData).getRecoveryRate();
    return PRICER.ir01BucketedZero(
        product,
        yieldCurveInputs,
        creditCurveInputs,
        marketData.getValuationDate(),
        recoveryRate,
        creditCurveInputs.getScalingFactor());
  }

  //-------------------------------------------------------------------------
  // calculates IR01 for all scenarios
  static CurrencyValuesArray ir01ParallelPar(
      ResolvedCdsTrade trade,
      ScenarioMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateIr01ParallelPar(trade, marketData.scenario(i)));
  }

  // IR01 for one scenario
  private static CurrencyAmount calculateIr01ParallelPar(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsId.of(product.getCurrency()));
    IsdaCreditCurveInputs creditCurveInputs = creditCurveInputs(trade, marketData);
    double recoveryRate = cdsRecoveryRate(trade, marketData).getRecoveryRate();
    return PRICER.ir01ParallelPar(
        product,
        yieldCurveInputs,
        creditCurveInputs,
        marketData.getValuationDate(),
        recoveryRate,
        creditCurveInputs.getScalingFactor());
  }

  //-------------------------------------------------------------------------
  // calculates bucketed IR01 for all scenarios
  static ScenarioArray<CurrencyParameterSensitivities> ir01BucketedPar(
      ResolvedCdsTrade trade,
      ScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> calculateIr01BucketedPar(trade, marketData.scenario(i)));
  }

  // bucketed IR01 for one scenario
  private static CurrencyParameterSensitivities calculateIr01BucketedPar(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsId.of(product.getCurrency()));
    IsdaCreditCurveInputs creditCurveInputs = creditCurveInputs(trade, marketData);
    double recoveryRate = cdsRecoveryRate(trade, marketData).getRecoveryRate();
    return PRICER.ir01BucketedPar(
        product,
        yieldCurveInputs,
        creditCurveInputs,
        marketData.getValuationDate(),
        recoveryRate,
        creditCurveInputs.getScalingFactor());
  }

  //-------------------------------------------------------------------------
  // calculates CS01 for all scenarios
  static CurrencyValuesArray cs01ParallelPar(
      ResolvedCdsTrade trade,
      ScenarioMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateCs01ParallelPar(trade, marketData.scenario(i)));
  }

  // present value for one scenario
  private static CurrencyAmount calculateCs01ParallelPar(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsId.of(product.getCurrency()));
    IsdaCreditCurveInputs creditCurveInputs = creditCurveInputs(trade, marketData);
    double recoveryRate = cdsRecoveryRate(trade, marketData).getRecoveryRate();
    return PRICER.cs01ParallelPar(
        product,
        yieldCurveInputs,
        creditCurveInputs,
        marketData.getValuationDate(),
        recoveryRate,
        creditCurveInputs.getScalingFactor());
  }

  //-------------------------------------------------------------------------
  // calculates bucketed CS01 for all scenarios
  static ScenarioArray<CurrencyParameterSensitivities> cs01BucketedPar(
      ResolvedCdsTrade trade,
      ScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> calculateCs01BucketedPar(trade, marketData.scenario(i)));
  }

  // bucketed CS01 for one scenario
  private static CurrencyParameterSensitivities calculateCs01BucketedPar(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsId.of(product.getCurrency()));
    IsdaCreditCurveInputs creditCurveInputs = creditCurveInputs(trade, marketData);
    double recoveryRate = cdsRecoveryRate(trade, marketData).getRecoveryRate();
    return PRICER.cs01BucketedPar(
        product,
        yieldCurveInputs,
        creditCurveInputs,
        marketData.getValuationDate(),
        recoveryRate,
        creditCurveInputs.getScalingFactor());
  }

  //-------------------------------------------------------------------------
  // calculates CS01 for all scenarios
  static CurrencyValuesArray cs01ParallelHazard(
      ResolvedCdsTrade trade,
      ScenarioMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateCs01ParallelHazard(trade, marketData.scenario(i)));
  }

  // CS01 for one scenario
  private static CurrencyAmount calculateCs01ParallelHazard(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsId.of(product.getCurrency()));
    IsdaCreditCurveInputs creditCurveInputs = creditCurveInputs(trade, marketData);
    double recoveryRate = cdsRecoveryRate(trade, marketData).getRecoveryRate();
    return PRICER.cs01ParallelHazard(
        product,
        yieldCurveInputs,
        creditCurveInputs,
        marketData.getValuationDate(),
        recoveryRate,
        creditCurveInputs.getScalingFactor());
  }

  //-------------------------------------------------------------------------
  // calculates bucketed CS01 for all scenarios
  static ScenarioArray<CurrencyParameterSensitivities> cs01BucketedHazard(
      ResolvedCdsTrade trade,
      ScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> calculateCs01BucketedHazard(trade, marketData.scenario(i)));
  }

  // bucketed CS01 for one scenario
  private static CurrencyParameterSensitivities calculateCs01BucketedHazard(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsId.of(product.getCurrency()));
    IsdaCreditCurveInputs creditCurveInputs = creditCurveInputs(trade, marketData);
    double recoveryRate = cdsRecoveryRate(trade, marketData).getRecoveryRate();
    return PRICER.cs01BucketedHazard(
        product,
        yieldCurveInputs,
        creditCurveInputs,
        marketData.getValuationDate(),
        recoveryRate,
        creditCurveInputs.getScalingFactor());
  }

  //-------------------------------------------------------------------------
  // calculates recovery01 for all scenarios
  static CurrencyValuesArray recovery01(
      ResolvedCdsTrade trade,
      ScenarioMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateRecovery01(trade, marketData.scenario(i)));
  }

  // recovery01 for one scenario
  private static CurrencyAmount calculateRecovery01(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsId.of(product.getCurrency()));
    IsdaCreditCurveInputs creditCurveInputs = creditCurveInputs(trade, marketData);
    double recoveryRate = cdsRecoveryRate(trade, marketData).getRecoveryRate();
    return PRICER.recovery01(
        product,
        yieldCurveInputs,
        creditCurveInputs,
        marketData.getValuationDate(),
        recoveryRate,
        creditCurveInputs.getScalingFactor());
  }

  //-------------------------------------------------------------------------
  // calculates jump to default for all scenarios
  static CurrencyValuesArray jumpToDefault(
      ResolvedCdsTrade trade,
      ScenarioMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateJumpToDefault(trade, marketData.scenario(i)));
  }

  // jump to default for one scenario
  private static CurrencyAmount calculateJumpToDefault(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsId.of(product.getCurrency()));
    IsdaCreditCurveInputs creditCurveInputs = creditCurveInputs(trade, marketData);
    double recoveryRate = cdsRecoveryRate(trade, marketData).getRecoveryRate();
    return PRICER.jumpToDefault(
        product,
        yieldCurveInputs,
        creditCurveInputs,
        marketData.getValuationDate(),
        recoveryRate,
        creditCurveInputs.getScalingFactor());
  }

  //-------------------------------------------------------------------------
  // obtains the credit curve inputs
  private static IsdaCreditCurveInputs creditCurveInputs(ResolvedCdsTrade trade, MarketData marketData) {
    ReferenceInformation refInfo = trade.getProduct().getReferenceInformation();
    if (refInfo instanceof SingleNameReferenceInformation) {
      return marketData.getValue(IsdaSingleNameCreditCurveInputsId.of((SingleNameReferenceInformation) refInfo));

    } else if (refInfo instanceof IndexReferenceInformation) {
      return marketData.getValue(IsdaIndexCreditCurveInputsId.of((IndexReferenceInformation) refInfo));

    } else {
      throw new IllegalStateException("Unknown reference information type: " + refInfo.getType());
    }
  }

  // obtains the recovey rate
  private static CdsRecoveryRate cdsRecoveryRate(ResolvedCdsTrade trade, MarketData marketData) {
    ReferenceInformation refInfo = trade.getProduct().getReferenceInformation();
    if (refInfo instanceof SingleNameReferenceInformation) {
      return marketData.getValue(IsdaSingleNameRecoveryRateId.of((SingleNameReferenceInformation) refInfo));

    } else if (refInfo instanceof IndexReferenceInformation) {
      return marketData.getValue(IsdaIndexRecoveryRateId.of((IndexReferenceInformation) refInfo));

    } else {
      throw new IllegalStateException("Unknown reference information type: " + refInfo.getType());
    }
  }

  //-------------------------------------------------------------------------
  // calculates par rate for all scenarios
  static ValuesArray parRate(
      ResolvedCdsTrade trade,
      ScenarioMarketData marketData) {

    return ValuesArray.of(
        marketData.getScenarioCount(),
        index -> calculateParRate(trade, marketData.scenario(index)));
  }

  // par rate for one scenario
  private static double calculateParRate(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsId.of(product.getCurrency()));
    IsdaCreditCurveInputs creditCurveInputs = creditCurveInputs(trade, marketData);
    double recoveryRate = cdsRecoveryRate(trade, marketData).getRecoveryRate();
    return PRICER.parRate(
        product,
        yieldCurveInputs,
        creditCurveInputs,
        marketData.getValuationDate(),
        recoveryRate);
  }

}
