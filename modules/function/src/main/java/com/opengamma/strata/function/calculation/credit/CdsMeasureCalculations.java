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
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.credit.ExpandedCds;
import com.opengamma.strata.product.credit.IndexReferenceInformation;
import com.opengamma.strata.product.credit.ReferenceInformation;
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
      CdsTrade trade,
      ExpandedCds product,
      CalculationMarketData marketData) {

    return ValuesArray.of(
        marketData.getScenarioCount(),
        index -> calculateParRate(trade, product, marketData.scenario(index)));
  }

  // par rate for one scenario
  private static double calculateParRate(
      CdsTrade trade,
      ExpandedCds product,
      MarketData marketData) {

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
      CdsTrade trade,
      ExpandedCds product,
      CalculationMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePresentValue(trade, product, marketData.scenario(i)));
  }

  // present value for one scenario
  private static CurrencyAmount calculatePresentValue(
      CdsTrade trade,
      ExpandedCds product,
      MarketData marketData) {

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
      CdsTrade trade,
      ExpandedCds product,
      CalculationMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateIr01ParallelZero(trade, product, marketData.scenario(i)));
  }

  // IR01 for one scenario
  private static CurrencyAmount calculateIr01ParallelZero(
      CdsTrade trade,
      ExpandedCds product,
      MarketData marketData) {

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
      CdsTrade trade,
      ExpandedCds product,
      CalculationMarketData marketData) {

    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateIr01BucketedZero(trade, product, marketData.scenario(i)));
  }

  // bucketed IR01 for one scenario
  private static CurveCurrencyParameterSensitivities calculateIr01BucketedZero(
      CdsTrade trade,
      ExpandedCds product,
      MarketData marketData) {

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
      CdsTrade trade,
      ExpandedCds product,
      CalculationMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateIr01ParallelPar(trade, product, marketData.scenario(i)));
  }

  // IR01 for one scenario
  private static CurrencyAmount calculateIr01ParallelPar(
      CdsTrade trade,
      ExpandedCds product,
      MarketData marketData) {

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
      CdsTrade trade,
      ExpandedCds product,
      CalculationMarketData marketData) {

    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateIr01BucketedPar(trade, product, marketData.scenario(i)));
  }

  // bucketed IR01 for one scenario
  private static CurveCurrencyParameterSensitivities calculateIr01BucketedPar(
      CdsTrade trade,
      ExpandedCds product,
      MarketData marketData) {

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
      CdsTrade trade,
      ExpandedCds product,
      CalculationMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateCs01ParallelPar(trade, product, marketData.scenario(i)));
  }

  // present value for one scenario
  private static CurrencyAmount calculateCs01ParallelPar(
      CdsTrade trade,
      ExpandedCds product,
      MarketData marketData) {

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
      CdsTrade trade,
      ExpandedCds product,
      CalculationMarketData marketData) {

    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateCs01BucketedPar(trade, product, marketData.scenario(i)));
  }

  // bucketed CS01 for one scenario
  private static CurveCurrencyParameterSensitivities calculateCs01BucketedPar(
      CdsTrade trade,
      ExpandedCds product,
      MarketData marketData) {

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
      CdsTrade trade,
      ExpandedCds product,
      CalculationMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateCs01ParallelHazard(trade, product, marketData.scenario(i)));
  }

  // CS01 for one scenario
  private static CurrencyAmount calculateCs01ParallelHazard(
      CdsTrade trade,
      ExpandedCds product,
      MarketData marketData) {

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
      CdsTrade trade,
      ExpandedCds product,
      CalculationMarketData marketData) {

    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateCs01BucketedHazard(trade, product, marketData.scenario(i)));
  }

  // bucketed CS01 for one scenario
  private static CurveCurrencyParameterSensitivities calculateCs01BucketedHazard(
      CdsTrade trade,
      ExpandedCds product,
      MarketData marketData) {

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
      CdsTrade trade,
      ExpandedCds product,
      CalculationMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateRecovery01(trade, product, marketData.scenario(i)));
  }

  // recovery01 for one scenario
  private static CurrencyAmount calculateRecovery01(
      CdsTrade trade,
      ExpandedCds product,
      MarketData marketData) {

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
      CdsTrade trade,
      ExpandedCds product,
      CalculationMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateJumpToDefault(trade, product, marketData.scenario(i)));
  }

  // jump to default for one scenario
  private static CurrencyAmount calculateJumpToDefault(
      CdsTrade trade,
      ExpandedCds product,
      MarketData marketData) {

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
  private static IsdaCreditCurveInputs creditCurveInputs(CdsTrade trade, MarketData marketData) {
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
  private static CdsRecoveryRate cdsRecoveryRate(CdsTrade trade, MarketData marketData) {
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
