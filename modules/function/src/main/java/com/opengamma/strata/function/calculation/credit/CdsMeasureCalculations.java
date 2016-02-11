/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.credit;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.CurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.calc.runner.function.result.ValuesArray;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.IsdaCreditCurveInputs;
import com.opengamma.strata.market.curve.IsdaYieldCurveInputs;
import com.opengamma.strata.market.key.IsdaIndexCreditCurveInputsKey;
import com.opengamma.strata.market.key.IsdaIndexRecoveryRateKey;
import com.opengamma.strata.market.key.IsdaSingleNameCreditCurveInputsKey;
import com.opengamma.strata.market.key.IsdaSingleNameRecoveryRateKey;
import com.opengamma.strata.market.key.IsdaYieldCurveInputsKey;
import com.opengamma.strata.market.value.CdsRecoveryRate;
import com.opengamma.strata.pricer.credit.IsdaCdsPricer;
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
class CdsMeasureCalculations {

  /**
   * The pricer to use.
   */
  private static final IsdaCdsPricer PRICER = IsdaCdsPricer.DEFAULT;

  // restricted constructor
  private CdsMeasureCalculations() {
  }

  //-------------------------------------------------------------------------
  // calculates par rate for all scenarios
  static ValuesArray parRate(
      ResolvedCdsTrade trade,
      CalculationMarketData marketData) {

    return ValuesArray.of(
        marketData.getScenarioCount(),
        index -> calculateParRate(trade, marketData.scenario(index)));
  }

  // par rate for one scenario
  private static double calculateParRate(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsKey.of(product.getCurrency()));
    IsdaCreditCurveInputs creditCurveInputs = creditCurveInputs(trade, marketData);
    double recoveryRate = cdsRecoveryRate(trade, marketData).getRecoveryRate();
    return PRICER.parRate(
        product,
        yieldCurveInputs,
        creditCurveInputs,
        marketData.getValuationDate(),
        recoveryRate);
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  static CurrencyValuesArray presentValue(
      ResolvedCdsTrade trade,
      CalculationMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePresentValue(trade, marketData.scenario(i)));
  }

  // present value for one scenario
  private static CurrencyAmount calculatePresentValue(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsKey.of(product.getCurrency()));
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
      CalculationMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateIr01ParallelZero(trade, marketData.scenario(i)));
  }

  // IR01 for one scenario
  private static CurrencyAmount calculateIr01ParallelZero(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsKey.of(product.getCurrency()));
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
  static ScenarioResult<CurveCurrencyParameterSensitivities> ir01BucketedZero(
      ResolvedCdsTrade trade,
      CalculationMarketData marketData) {

    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateIr01BucketedZero(trade, marketData.scenario(i)));
  }

  // bucketed IR01 for one scenario
  private static CurveCurrencyParameterSensitivities calculateIr01BucketedZero(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsKey.of(product.getCurrency()));
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
      CalculationMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateIr01ParallelPar(trade, marketData.scenario(i)));
  }

  // IR01 for one scenario
  private static CurrencyAmount calculateIr01ParallelPar(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsKey.of(product.getCurrency()));
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
  static ScenarioResult<CurveCurrencyParameterSensitivities> ir01BucketedPar(
      ResolvedCdsTrade trade,
      CalculationMarketData marketData) {

    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateIr01BucketedPar(trade, marketData.scenario(i)));
  }

  // bucketed IR01 for one scenario
  private static CurveCurrencyParameterSensitivities calculateIr01BucketedPar(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsKey.of(product.getCurrency()));
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
      CalculationMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateCs01ParallelPar(trade, marketData.scenario(i)));
  }

  // present value for one scenario
  private static CurrencyAmount calculateCs01ParallelPar(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsKey.of(product.getCurrency()));
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
  static ScenarioResult<CurveCurrencyParameterSensitivities> cs01BucketedPar(
      ResolvedCdsTrade trade,
      CalculationMarketData marketData) {

    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateCs01BucketedPar(trade, marketData.scenario(i)));
  }

  // bucketed CS01 for one scenario
  private static CurveCurrencyParameterSensitivities calculateCs01BucketedPar(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsKey.of(product.getCurrency()));
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
      CalculationMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateCs01ParallelHazard(trade, marketData.scenario(i)));
  }

  // CS01 for one scenario
  private static CurrencyAmount calculateCs01ParallelHazard(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsKey.of(product.getCurrency()));
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
  static ScenarioResult<CurveCurrencyParameterSensitivities> cs01BucketedHazard(
      ResolvedCdsTrade trade,
      CalculationMarketData marketData) {

    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateCs01BucketedHazard(trade, marketData.scenario(i)));
  }

  // bucketed CS01 for one scenario
  private static CurveCurrencyParameterSensitivities calculateCs01BucketedHazard(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsKey.of(product.getCurrency()));
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
      CalculationMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateRecovery01(trade, marketData.scenario(i)));
  }

  // recovery01 for one scenario
  private static CurrencyAmount calculateRecovery01(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsKey.of(product.getCurrency()));
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
      CalculationMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateJumpToDefault(trade, marketData.scenario(i)));
  }

  // jump to default for one scenario
  private static CurrencyAmount calculateJumpToDefault(
      ResolvedCdsTrade trade,
      MarketData marketData) {

    ResolvedCds product = trade.getProduct();
    IsdaYieldCurveInputs yieldCurveInputs = marketData.getValue(IsdaYieldCurveInputsKey.of(product.getCurrency()));
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
      return marketData.getValue(IsdaSingleNameCreditCurveInputsKey.of((SingleNameReferenceInformation) refInfo));

    } else if (refInfo instanceof IndexReferenceInformation) {
      return marketData.getValue(IsdaIndexCreditCurveInputsKey.of((IndexReferenceInformation) refInfo));

    } else {
      throw new IllegalStateException("Unknown reference information type: " + refInfo.getType());
    }
  }

  // obtains the recovey rate
  private static CdsRecoveryRate cdsRecoveryRate(ResolvedCdsTrade trade, MarketData marketData) {
    ReferenceInformation refInfo = trade.getProduct().getReferenceInformation();
    if (refInfo instanceof SingleNameReferenceInformation) {
      return marketData.getValue(IsdaSingleNameRecoveryRateKey.of((SingleNameReferenceInformation) refInfo));

    } else if (refInfo instanceof IndexReferenceInformation) {
      return marketData.getValue(IsdaIndexRecoveryRateKey.of((IndexReferenceInformation) refInfo));

    } else {
      throw new IllegalStateException("Unknown reference information type: " + refInfo.getType());
    }
  }

}
