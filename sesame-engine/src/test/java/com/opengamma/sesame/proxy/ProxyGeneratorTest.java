/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.proxy;

import static com.opengamma.util.result.FailureStatus.ERROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.fail;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.id.ExternalScheme;
import com.opengamma.sesame.example.MockResultFn;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataValues;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultStatus;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class ProxyGeneratorTest {

  private ProxyGenerator _proxyGenerator = new ProxyGenerator();

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void delegateMustNotBeNull() {
    _proxyGenerator.generate(null, Map.class);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void interfaceMustNotBeNull() {
    _proxyGenerator.generate(ImmutableMap.of("this", "that"), null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void cannotGenerateProxyForClasses() {
    _proxyGenerator.generate("S1", String.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void canGenerateProxyForInterface() {
    Map<String, String> proxy = _proxyGenerator.generate(ImmutableMap.of("this", "that"), Map.class);
    assertThat(proxy instanceof Proxy, is(true));
    assertThat(proxy.get("this"), is("that"));
  }

  @Test
  public void methodReturningFunctionResultWillHaveExceptionsIntercepted() {

    final String message = "Oops, thrown my toys out";
    MockResultFn cpf = new MockResultFn() {
      @Override
      public Result<ExternalScheme> getScheme() {
        throw new RuntimeException(message);
      }
    };

    try {
      cpf.getScheme();
      fail("Should have thrown an exception");
    } catch (Exception e) {
      assertThat(e.getMessage(), containsString(message));
    }

    MockResultFn proxy = _proxyGenerator.generate(cpf, MockResultFn.class);
    Result<ExternalScheme> result = proxy.getScheme();
    assertThat(result.getStatus(), is((ResultStatus) ERROR));
    assertThat(result.getFailureMessage(), containsString(message));
  }

  @Test
  public void methodReturningMarketDataFunctionResultWillHaveExceptionsIntercepted() {

    final String message = "Oops, thrown my toys out";
    MarketDataFn mdpf = new MarketDataFn() {
      @Override
      public Result<MarketDataValues> requestData(MarketDataRequirement requirement) {
        throw new RuntimeException(message);
      }
      
      @Override
      public Result<MarketDataValues> requestData(Set<MarketDataRequirement> requirements) {
        throw new RuntimeException(message);
      }
    };

    try {
      mdpf.requestData((MarketDataRequirement) null);
      fail("Should have thrown an exception");
    } catch (Exception e) {
      assertThat(e.getMessage(), containsString(message));
    }

    MarketDataFn proxy = _proxyGenerator.generate(mdpf, MarketDataFn.class);
    Result<MarketDataValues> result = proxy.requestData((MarketDataRequirement) null);
    assertThat(result.getStatus(), is((ResultStatus) ERROR));
    assertThat(result.getFailureMessage(), containsString(message));
  }

  @Test
  public void methodNotReturningFunctionResultThrowsExceptions() {

    final String message = "Oops, thrown my toys out";
    ActionListener al= new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        throw new RuntimeException(message);
      }
    };

    try {
      al.actionPerformed(null);
      fail("Should have thrown an exception");
    } catch (Exception e) {
      assertThat(e.getMessage(), containsString(message));
    }

    try {
      ActionListener proxy = _proxyGenerator.generate(al, ActionListener.class);
      proxy.actionPerformed(null);
      fail("Should have thrown an exception");
    } catch (Exception e) {
      assertThat(e.getMessage(), containsString(message));
    }
  }
}
