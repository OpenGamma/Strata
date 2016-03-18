/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

/**
 * Testing surface implementation.
 * <p>
 * Does not implement {@link NodalSurface}.
 */
public class TestingSurface implements Surface {

  private final SurfaceMetadata metadata;

  public TestingSurface() {
    this(DefaultSurfaceMetadata.of("Test"));
  }

  public TestingSurface(SurfaceMetadata metadata) {
    this.metadata = metadata;
  }

  //-------------------------------------------------------------------------
  @Override
  public SurfaceMetadata getMetadata() {
    return metadata;
  }

  @Override
  public int getParameterCount() {
    throw new IllegalStateException();
  }

  @Override
  public double zValue(double x, double y) {
    throw new IllegalStateException();
  }

  @Override
  public SurfaceUnitParameterSensitivity zValueParameterSensitivity(double x, double y) {
    throw new IllegalStateException();
  }

}
