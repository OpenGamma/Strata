/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.proxy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

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
}
