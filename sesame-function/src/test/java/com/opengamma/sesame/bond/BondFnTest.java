/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bond;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.Instant;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.financial.analytics.model.fixedincome.BucketedCurveSensitivities;
import com.opengamma.service.ServiceContext;
import com.opengamma.service.ThreadLocalServiceContext;
import com.opengamma.service.VersionCorrectionProvider;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.engine.FixedInstantVersionCorrectionProvider;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.sources.BondMockSources;
import com.opengamma.sesame.trade.BondTrade;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.tuple.ObjectsPair;
import com.opengamma.util.tuple.Pair;

/**
 * Tests for bond future functions using the discounting calculator.
 */
@Test(groups = TestGroup.UNIT)
public class BondFnTest {

  private static BondFn _bondFn;
  private static final Environment ENV = BondMockSources.ENV;
  private static final BondTrade GOV_TRADE = BondMockSources.GOVERNMENT_BOND_TRADE;
  private static final BondTrade CORP_TRADE = BondMockSources.CORPORATE_BOND_TRADE;

  private static final double STD_TOLERANCE_PV = 1.0E-3;
  private static final double STD_TOLERANCE_RATE = 1.0E-8;
  private static final double STD_TOLERANCE_PV01 = 1.0E-4;

  @BeforeClass
  public void setUp() {
    ImmutableMap<Class<?>, Object> components = BondMockSources.generateBaseComponents();
    VersionCorrectionProvider vcProvider = new FixedInstantVersionCorrectionProvider(Instant.now());
    ServiceContext serviceContext = ServiceContext.of(components).with(VersionCorrectionProvider.class, vcProvider);
    ThreadLocalServiceContext.init(serviceContext);
    _bondFn = FunctionModel.build(BondFn.class, BondMockSources.getConfig(), ComponentMap.of(components));
  }

  /* Corporate bond tests */

  @Test
  public void testCorporateBondPresentValueFromCurves() {
    Result<MultipleCurrencyAmount> computed = _bondFn.calculatePresentValueFromCurves(ENV, CORP_TRADE);
    assertThat(computed.isSuccess(), is(true));
    assertThat(computed.getValue().getCurrencyAmount(Currency.GBP).getAmount(),
               is(closeTo(12014.470297433974, STD_TOLERANCE_PV)));
  }

  @Test
  public void testCorporateBondPresentValueFromMarketCleanPrice() {
    Result<MultipleCurrencyAmount> computed = _bondFn.calculatePresentValueFromClean(ENV, CORP_TRADE);
    assertThat(computed.isSuccess(), is(true));
    assertThat(computed.getValue().getCurrencyAmount(Currency.GBP).getAmount(),
               is(closeTo(10920.525899532251, STD_TOLERANCE_PV)));
  }

  @Test
  public void testCorporateBondPresentValueFromYield() {
    Result<MultipleCurrencyAmount> computed = _bondFn.calculatePresentValueFromYield(ENV, CORP_TRADE);
    assertThat(computed.isSuccess(), is(true));
    assertThat(computed.getValue().getCurrencyAmount(Currency.GBP).getAmount(),
               is(closeTo(10920.525899532251, STD_TOLERANCE_PV)));
  }

  @Test
  public void testCorporateBondPV01() {
    Result<ReferenceAmount<Pair<String, Currency>>> computed = _bondFn.calculatePV01(ENV, CORP_TRADE);
    assertThat(computed.isSuccess(), is(true));
    ObjectsPair key = ObjectsPair.of(BondMockSources.BOND_GBP_CURVE_NAME, Currency.GBP);
    assertThat(computed.getValue().getMap().get(key), is(closeTo(-5.185676590681165, STD_TOLERANCE_PV01)));
  }

  @Test
  public void testCorporateBondBucketedPV01() {
    Result<BucketedCurveSensitivities> computed = _bondFn.calculateBucketedPV01(ENV, CORP_TRADE);
    assertThat(computed.isSuccess(), is(true));

    BucketedCurveSensitivities sensitivities = computed.getValue();
    ObjectsPair key = ObjectsPair.of(BondMockSources.BOND_GBP_CURVE_NAME, Currency.GBP);
    assertThat(sensitivities.getSensitivities().containsKey(key), is(true));

    double[] actual = sensitivities.getSensitivities().get(key).getValues();
    double[] expected = {-0.06685986286417701,
                         -0.1215256080769927,
                         -0.17909597621767445,
                         -0.5639057484608532,
                         -4.254289395061467,
                          0.0,
                          0.0,
                          0.0,
                          0.0,
                          0.0,
                          0.0};

    int i = 0;
    for(double amount : actual) {
      assertThat(amount, is(closeTo(expected[i], STD_TOLERANCE_PV01)));
      i++;
    }
  }

  @Test
  public void testCorporateBondZSpread() {
    Result<Double> computed = _bondFn.calculateZSpread(ENV, CORP_TRADE);
    assertThat(computed.isSuccess(), is(true));
    assertThat(computed.getValue(), is(closeTo(217.28428993879345, STD_TOLERANCE_RATE)));
  }

  @Test
  public void testCorporateBondMarketCleanPrice() {
    Result<Double> computed = _bondFn.calculateMarketCleanPrice(ENV, CORP_TRADE);
    assertThat(computed.isSuccess(), is(true));
    assertThat(computed.getValue(), is(108.672));
  }

  @Test
  public void testCorporateBondYTM() {
    Result<Double> computed =  _bondFn.calculateYieldToMaturity(ENV, CORP_TRADE);
    assertThat(computed.isSuccess(), is(true));
    assertThat(computed.getValue(), is(closeTo(0.04373607286006458, STD_TOLERANCE_RATE)));
  }


  /* Government bond tests */

  @Test
  public void testGovernmentBondPresentValueFromCurves() {
    Result<MultipleCurrencyAmount> computed = _bondFn.calculatePresentValueFromCurves(ENV, GOV_TRADE);
    assertThat(computed.isSuccess(), is(true));
    assertThat(computed.getValue().getCurrencyAmount(Currency.GBP).getAmount(),
               is(closeTo(13552.455116034153, STD_TOLERANCE_PV)));
  }

  @Test
  public void testGovernmentBondPresentValueFromMarketCleanPrice() {
    Result<MultipleCurrencyAmount> computed = _bondFn.calculatePresentValueFromClean(ENV, CORP_TRADE);
    assertThat(computed.isSuccess(), is(true));
    assertThat(computed.getValue().getCurrencyAmount(Currency.GBP).getAmount(),
               is(closeTo(10920.525899532251, STD_TOLERANCE_PV)));
  }

  @Test
  public void testGovernmentBondPresentValueFromYield() {
    Result<MultipleCurrencyAmount> computed = _bondFn.calculatePresentValueFromYield(ENV, CORP_TRADE);
    assertThat(computed.isSuccess(), is(true));
    assertThat(computed.getValue().getCurrencyAmount(Currency.GBP).getAmount(),
               is(closeTo(10920.525899532246, STD_TOLERANCE_PV)));
  }

  @Test
  public void testGovernmentBondPV01() {
    Result<ReferenceAmount<Pair<String, Currency>>> computed = _bondFn.calculatePV01(ENV, GOV_TRADE);
    assertThat(computed.isSuccess(), is(true));
    ObjectsPair key = ObjectsPair.of(BondMockSources.BOND_GBP_CURVE_NAME, Currency.GBP);
    assertThat(computed.getValue().getMap().get(key), is(closeTo(-7.464961746079804, STD_TOLERANCE_PV01)));
  }

  @Test
  public void testGovernmentBondBucketedPV01() {
    Result<BucketedCurveSensitivities> computed = _bondFn.calculateBucketedPV01(ENV, GOV_TRADE);
    assertThat(computed.isSuccess(), is(true));
    BucketedCurveSensitivities sensitivities = computed.getValue();
    ObjectsPair key = ObjectsPair.of(BondMockSources.BOND_GBP_CURVE_NAME, Currency.GBP);
    assertThat(sensitivities.getSensitivities().containsKey(key), is(true));

    double[] actual = sensitivities.getSensitivities().get(key).getValues();
    double[] expected = { -0.09192722152866753,
                          -0.15343169933881246,
                          -0.22372407292040475,
                          -0.28953442042880745,
                          -0.34923213343519427,
                          -1.108880636097358,
                          -5.248231562330559,
                          0.0,
                          0.0,
                          0.0};

    int i = 0;
    for(double amount : actual) {
      assertThat(amount, is(closeTo(expected[i], STD_TOLERANCE_PV01)));
      i++;
    }
  }

  @Test
  public void testGovernmentBondZSpread() {
    Result<Double> computed = _bondFn.calculateZSpread(ENV, GOV_TRADE);
    assertThat(computed.isSuccess(), is(true));
    assertThat(computed.getValue(), is(closeTo(-10.80482378769219, STD_TOLERANCE_RATE)));
  }

  @Test
  public void testGovernmentBondMarketCleanPrice() {
    Result<Double> computed = _bondFn.calculateMarketCleanPrice(ENV, GOV_TRADE);
    assertThat(computed.isSuccess(), is(true));
    assertThat(computed.getValue(), is(136.375));
  }

  @Test
  public void testGovernmentBondYTM() {
    Result<Double> computed = _bondFn.calculateYieldToMaturity(ENV, GOV_TRADE);
    assertThat(computed.isSuccess(), is(true));
    assertThat(computed.getValue(), is(closeTo(0.022588052760789467, STD_TOLERANCE_RATE)));
  }
  
}
