/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit.cds;

import java.util.List;
import java.util.stream.Collectors;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.Guavate;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.curve.node.CdsIndexIsdaCreditCurveNode;
import com.opengamma.strata.market.curve.node.CdsIsdaCreditCurveNode;
import com.opengamma.strata.market.observable.LegalEntityInformation;
import com.opengamma.strata.market.observable.LegalEntityInformationId;

/**
 * ISDA compliant index curve calibrator.
 * <p>
 * A single credit curve (index curve) is calibrated for CDS index trades.
 * <p>
 * The curve is defined using two or more {@linkplain CdsIndexIsdaCreditCurveNode nodes}.
 * Each node primarily defines enough information to produce a reference CDS index trade.
 * All of the curve nodes must be based on a common single names and currency.
 * <p>
 * Calibration involves pricing, and re-pricing, these trades to find the best fit using a root finder, 
 * where the pricing is based on {@link IsdaHomogenousCdsIndexTradePricer}, thus 
 * the calibration is completed by a calibrator for single name CDS trades, {@link IsdaCompliantCreditCurveCalibrator}.
 * <p>
 * Relevant discount curve and recovery rate curve are required to complete the calibration.
 */
public class IsdaCompliantIndexCurveCalibrator {

  /**
   * Default implementation.
   */
  public static final IsdaCompliantIndexCurveCalibrator DEFAULT =
      new IsdaCompliantIndexCurveCalibrator(FastCreditCurveCalibrator.DEFAULT);

  /**
   * The underlying credit curve calibrator.
   */
  private final IsdaCompliantCreditCurveCalibrator creditCurveCalibrator;

  /**
   * Constructor with the underlying credit curve calibrator specified. 
   * 
   * @param creditCurveCalibrator  the credit curve calibrator
   */
  public IsdaCompliantIndexCurveCalibrator(IsdaCompliantCreditCurveCalibrator creditCurveCalibrator) {
    this.creditCurveCalibrator = creditCurveCalibrator;
  }

  //-------------------------------------------------------------------------
  /**
   * Calibrates the index curve to the market data.
   * <p>
   * This creates the single credit curve for CDS index trades.
   * <p>
   * The relevant discount curve and recovery rate curve must be stored in {@code ratesProvider}. 
   * The day count convention for the resulting credit curve is the same as that of the discount curve.
   * 
   * @param curveNode  the curve node
   * @param curveDcc  the curve day count
   * @param name  the curve name
   * @param marketData  the market data
   * @param ratesProvider  the rates provider
   * @param computeJacobian  the Jacobian matrices are computed if true
   * @param refData  the reference data
   * @return the index curve
   */
  public LegalEntitySurvivalProbabilities calibrate(
      List<CdsIndexIsdaCreditCurveNode> curveNode,
      CurveName name,
      MarketData marketData,
      CreditRatesProvider ratesProvider,
      boolean computeJacobian,
      ReferenceData refData) {

    // Homogeneity of curveNode will be checked within IsdaCompliantCreditCurveCalibrator
    double indexFactor = computeIndexFactor(curveNode.get(0), marketData);
    List<CdsIsdaCreditCurveNode> cdsNode = curveNode.stream().map(i -> toCdsNode(i)).collect(Guavate.toImmutableList());
    LegalEntitySurvivalProbabilities creditCurve =
        creditCurveCalibrator.calibrate(cdsNode, name, marketData, ratesProvider, computeJacobian, refData);
    NodalCurve underlyingCurve = ((IsdaCompliantZeroRateDiscountFactors) creditCurve.getSurvivalProbabilities()).getCurve();
    NodalCurve curveWithFactor =
        underlyingCurve.withMetadata(underlyingCurve.getMetadata().withInfo(CurveInfoType.CDS_INDEX_FACTOR, indexFactor));

    return LegalEntitySurvivalProbabilities.of(
        creditCurve.getLegalEntityId(),
        IsdaCompliantZeroRateDiscountFactors.of(creditCurve.getCurrency(), creditCurve.getValuationDate(), curveWithFactor));
  }

  //-------------------------------------------------------------------------
  private CdsIsdaCreditCurveNode toCdsNode(CdsIndexIsdaCreditCurveNode index) {
    return CdsIsdaCreditCurveNode.builder()
        .label(index.getLabel())
        .legalEntityId(index.getCdsIndexId())
        .observableId(index.getObservableId())
        .quoteConvention(index.getQuoteConvention())
        .template(index.getTemplate())
        .fixedRate(index.getFixedRate().isPresent() ? index.getFixedRate().getAsDouble() : null)
        .build();
  }

  private double computeIndexFactor(CdsIndexIsdaCreditCurveNode node, MarketData marketData) {
    double numDefaulted = node.getReferenceEntityIds().stream()
        .map(s -> marketData.getValue(LegalEntityInformationId.of(s)))
        .map(LegalEntityInformation.class::cast)
        .filter(LegalEntityInformation::isIsDefaulted)
        .collect(Collectors.toList())
        .size();
    double numTotal = node.getReferenceEntityIds().size();
    return (numTotal - numDefaulted) / numTotal;
  }

}
