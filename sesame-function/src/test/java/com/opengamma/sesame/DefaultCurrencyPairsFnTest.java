/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.money.Currency.AUD;
import static com.opengamma.util.money.Currency.EUR;
import static com.opengamma.util.money.Currency.GBP;
import static com.opengamma.util.money.Currency.USD;
import static com.opengamma.util.result.FailureStatus.MISSING_DATA;
import static com.opengamma.util.result.SuccessStatus.SUCCESS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.sesame.component.CurrencyPairSet;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultStatus;
import com.opengamma.util.test.TestGroup;


@Test(groups = TestGroup.UNIT)
public class DefaultCurrencyPairsFnTest {

  private CurrencyPairsFn _pairsFunction;

  @BeforeMethod
  public void setUp() {
    _pairsFunction = new DefaultCurrencyPairsFn(
        CurrencyPairSet.of(CurrencyPair.of(EUR, USD), CurrencyPair.of(GBP, USD)));
  }

  @Test
  public void testRetrieval() {
    Result<CurrencyPair> result = _pairsFunction.getCurrencyPair(EUR, USD);
    assertThat(result.getStatus(), is((ResultStatus) SUCCESS));
    assertThat(result.getValue(), is(CurrencyPair.of(EUR, USD)));
  }

  @Test
  public void testInverseRetrieval() {
    Result<CurrencyPair> result = _pairsFunction.getCurrencyPair(USD, GBP);
    assertThat(result.getStatus(), is((ResultStatus) SUCCESS));
    assertThat(result.getValue(), is(CurrencyPair.of(GBP, USD)));
  }

  @Test
  public void testUnknownPair() {
    Result<CurrencyPair> result = _pairsFunction.getCurrencyPair(AUD, GBP);
    assertThat(result.getStatus(), is((ResultStatus) MISSING_DATA));
  }

}
