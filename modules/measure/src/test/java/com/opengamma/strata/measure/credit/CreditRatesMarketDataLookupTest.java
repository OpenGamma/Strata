/**
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.credit;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.assertThrowsRuntime;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.joda.beans.ImmutableBean;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.pricer.credit.ConstantRecoveryRates;
import com.opengamma.strata.pricer.credit.CreditRatesProvider;
import com.opengamma.strata.pricer.credit.IsdaCreditDiscountFactors;
import com.opengamma.strata.pricer.credit.LegalEntitySurvivalProbabilities;

/**
 * Test {@link CreditRatesMarketDataLookup}.
 */
@Test
public class CreditRatesMarketDataLookupTest {

  private static final StandardId ISSUER_A = StandardId.of("OG-LegEnt", "A");
  private static final StandardId ISSUER_B = StandardId.of("OG-LegEnt", "B");
  private static final StandardId ISSUER_C = StandardId.of("OG-LegEnt", "C");

  private static final CurveId CC_A_USD = CurveId.of("Group", "Credit-A-USD");
  private static final CurveId CC_B_GBP = CurveId.of("Group", "Credit-B-GBP");
  private static final CurveId CC_A_GBP = CurveId.of("Group", "Credit-A-GBP");
  private static final CurveId DC_USD = CurveId.of("Group", "Dsc-USD");
  private static final CurveId DC_GBP = CurveId.of("Group", "Dsc-GBP");
  private static final CurveId RC_A = CurveId.of("Group", "Recovery-A");
  private static final CurveId RC_B = CurveId.of("Group", "Recovery-B");
  private static final ObservableSource OBS_SOURCE = ObservableSource.of("Vendor");
  private static final MarketData MOCK_MARKET_DATA = mock(MarketData.class);
  private static final ScenarioMarketData MOCK_CALC_MARKET_DATA = mock(ScenarioMarketData.class);

  private static final CreditRatesMarketDataLookup LOOKUP;
  private static final CreditRatesMarketDataLookup LOOKUP_WITH_SOURCE;
  static {
    ImmutableMap<Pair<StandardId, Currency>, CurveId> creditCurve = ImmutableMap.of(
        Pair.of(ISSUER_A, USD), CC_A_USD, Pair.of(ISSUER_B, GBP), CC_B_GBP, Pair.of(ISSUER_A, GBP), CC_A_GBP);
    ImmutableMap<Currency, CurveId> discoutCurve = ImmutableMap.of(USD, DC_USD, GBP, DC_GBP);
    ImmutableMap<StandardId, CurveId> recoveryCurve = ImmutableMap.of(ISSUER_A, RC_A, ISSUER_B, RC_B);
    LOOKUP_WITH_SOURCE = CreditRatesMarketDataLookup.of(creditCurve, discoutCurve, recoveryCurve, OBS_SOURCE);
    LOOKUP = CreditRatesMarketDataLookup.of(creditCurve, discoutCurve, recoveryCurve);
  }

  public void test_map() {
    assertEquals(LOOKUP.queryType(), CreditRatesMarketDataLookup.class);
    assertEquals(
        LOOKUP_WITH_SOURCE.requirements(ISSUER_A, USD),
        FunctionRequirements.builder()
            .observableSource(OBS_SOURCE)
            .valueRequirements(CC_A_USD, DC_USD, RC_A)
            .outputCurrencies(USD)
            .build());
    assertEquals(
        LOOKUP_WITH_SOURCE.requirements(ISSUER_A, GBP),
        FunctionRequirements.builder()
            .observableSource(OBS_SOURCE)
            .valueRequirements(CC_A_GBP, DC_GBP, RC_A)
            .outputCurrencies(GBP)
            .build());
    assertEquals(
        LOOKUP_WITH_SOURCE.requirements(ISSUER_B, GBP),
        FunctionRequirements.builder()
            .observableSource(OBS_SOURCE)
            .valueRequirements(CC_B_GBP, DC_GBP, RC_B)
            .outputCurrencies(GBP)
            .build());
    assertEquals(
        LOOKUP.requirements(ISSUER_A, USD),
        FunctionRequirements.builder()
            .valueRequirements(CC_A_USD, DC_USD, RC_A)
            .outputCurrencies(USD)
            .build());
    assertEquals(
        LOOKUP.requirements(ISSUER_A, GBP),
        FunctionRequirements.builder()
            .valueRequirements(CC_A_GBP, DC_GBP, RC_A)
            .outputCurrencies(GBP)
            .build());
    assertEquals(
        LOOKUP.requirements(ISSUER_B, GBP),
        FunctionRequirements.builder()
            .valueRequirements(CC_B_GBP, DC_GBP, RC_B)
            .outputCurrencies(GBP)
            .build());
    assertThrowsIllegalArg(() -> LOOKUP.requirements(ISSUER_A, EUR));
    assertThrowsIllegalArg(() -> LOOKUP.requirements(ISSUER_C, USD));
    assertEquals(
        LOOKUP.creditRatesProvider(MOCK_MARKET_DATA),
        DefaultLookupCreditRatesProvider.of((DefaultCreditRatesMarketDataLookup) LOOKUP, MOCK_MARKET_DATA));
  }

  //-------------------------------------------------------------------------
  public void test_marketDataView() {
    LocalDate valDate = LocalDate.of(2015, 6, 30);
    ScenarioMarketData md = new TestMarketDataMap(valDate, ImmutableMap.of(), ImmutableMap.of());
    CreditRatesScenarioMarketData multiScenario = LOOKUP_WITH_SOURCE.marketDataView(md);
    assertEquals(multiScenario.getLookup(), LOOKUP_WITH_SOURCE);
    assertEquals(multiScenario.getMarketData(), md);
    assertEquals(multiScenario.getScenarioCount(), 1);
    CreditRatesMarketData scenario = multiScenario.scenario(0);
    assertEquals(scenario.getLookup(), LOOKUP_WITH_SOURCE);
    assertEquals(scenario.getMarketData(), md.scenario(0));
    assertEquals(scenario.getValuationDate(), valDate);
  }

  public void test_bondDiscountingProvider() {
    LocalDate valDate = LocalDate.of(2015, 6, 30);
    Curve ccAUsd = ConstantNodalCurve.of(Curves.zeroRates(CC_A_USD.getCurveName(), ACT_365F), 0.5d, 1.5d);
    Curve ccBGbp = ConstantNodalCurve.of(Curves.zeroRates(CC_B_GBP.getCurveName(), ACT_365F), 0.5d, 2d);
    Curve ccAGbp = ConstantNodalCurve.of(Curves.zeroRates(CC_A_GBP.getCurveName(), ACT_365F), 0.5d, 3d);
    Curve dcGbp = ConstantNodalCurve.of(Curves.zeroRates(DC_GBP.getCurveName(), ACT_365F), 0.5d, 0.1d);
    Curve dcUsd = ConstantNodalCurve.of(Curves.zeroRates(DC_USD.getCurveName(), ACT_365F), 0.5d, 0.05d);
    Curve rcA = ConstantCurve.of(Curves.recoveryRates(RC_A.getCurveName(), ACT_365F), 0.5d);
    Curve rcB = ConstantCurve.of(Curves.recoveryRates(RC_B.getCurveName(), ACT_365F), 0.4234d);
    Map<CurveId, Curve> curveMap = new HashMap<CurveId, Curve>();
    curveMap.put(CC_A_USD, ccAUsd);
    curveMap.put(CC_B_GBP, ccBGbp);
    curveMap.put(CC_A_GBP, ccAGbp);
    curveMap.put(DC_USD, dcUsd);
    curveMap.put(DC_GBP, dcGbp);
    curveMap.put(RC_A, rcA);
    curveMap.put(RC_B, rcB);
    MarketData md = ImmutableMarketData.of(valDate, ImmutableMap.copyOf(curveMap));
    CreditRatesProvider provider = LOOKUP_WITH_SOURCE.creditRatesProvider(md);

    assertEquals(provider.getValuationDate(), valDate);
    assertEquals(provider.findData(CC_A_USD.getCurveName()), Optional.of(ccAUsd));
    assertEquals(provider.findData(DC_USD.getCurveName()), Optional.of(dcUsd));
    assertEquals(provider.findData(RC_B.getCurveName()), Optional.of(rcB));
    assertEquals(provider.findData(CurveName.of("Rubbish")), Optional.empty());
    // check credit curve
    LegalEntitySurvivalProbabilities cc = provider.survivalProbabilities(ISSUER_A, GBP);
    IsdaCreditDiscountFactors ccUnder = (IsdaCreditDiscountFactors) cc.getSurvivalProbabilities();
    assertEquals(ccUnder.getCurve().getName(), ccAGbp.getName());
    assertThrowsRuntime(() -> provider.survivalProbabilities(ISSUER_B, USD));
    assertThrowsRuntime(() -> provider.survivalProbabilities(ISSUER_C, USD));
    // check discount curve
    IsdaCreditDiscountFactors dc = (IsdaCreditDiscountFactors) provider.discountFactors(USD);
    assertEquals(dc.getCurve().getName(), dcUsd.getName());
    assertThrowsRuntime(() -> provider.discountFactors(EUR));
    // check recovery rate curve
    ConstantRecoveryRates rc = (ConstantRecoveryRates) provider.recoveryRates(ISSUER_B);
    assertEquals(rc.getRecoveryRate(), rcB.getParameter(0));
    assertThrowsRuntime(() -> provider.recoveryRates(ISSUER_C));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean((ImmutableBean) LOOKUP_WITH_SOURCE);
    ImmutableMap<Pair<StandardId, Currency>, CurveId> ccMap = ImmutableMap.of(
        Pair.of(ISSUER_A, USD), CC_A_USD);
    ImmutableMap<Currency, CurveId> dcMap = ImmutableMap.of(USD, DC_USD);
    ImmutableMap<StandardId, CurveId> rcMap = ImmutableMap.of(ISSUER_A, RC_A);
    CreditRatesMarketDataLookup test2 = CreditRatesMarketDataLookup.of(ccMap, dcMap, rcMap);
    coverBeanEquals((ImmutableBean) LOOKUP_WITH_SOURCE, (ImmutableBean) test2);

    // related coverage
    coverImmutableBean((ImmutableBean) LOOKUP_WITH_SOURCE.marketDataView(MOCK_CALC_MARKET_DATA));
    DefaultCreditRatesScenarioMarketData.meta();
    coverImmutableBean((ImmutableBean) LOOKUP_WITH_SOURCE.marketDataView(MOCK_MARKET_DATA));
    DefaultCreditRatesMarketData.meta();
    coverImmutableBean((ImmutableBean) LOOKUP_WITH_SOURCE.marketDataView(MOCK_MARKET_DATA).creditRatesProvider());
    DefaultLookupCreditRatesProvider.meta();
  }

  public void test_serialization() {
    assertSerialization(LOOKUP_WITH_SOURCE);
  }

}
