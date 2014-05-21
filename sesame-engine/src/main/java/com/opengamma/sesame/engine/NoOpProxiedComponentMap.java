/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

/**
 * A proxied component map that does nothing with any calls made to it.
 */
public class NoOpProxiedComponentMap implements ProxiedComponentMap {

  /**
   * The static instance.
   */
  public static final NoOpProxiedComponentMap INSTANCE = new NoOpProxiedComponentMap();

  private NoOpProxiedComponentMap() {
  }

  @Override
  public void addListener(ComponentListener listener) {
    // Do nothing
  }

  @Override
  public void removeListener(ComponentListener listener) {
    // Do nothing
  }

  @Override
  public void receivedCall(Class<?> componentType, Object result) {
    // Do nothing
  }
}
