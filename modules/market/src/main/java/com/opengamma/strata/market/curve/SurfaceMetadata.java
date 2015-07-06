/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.util.List;
import java.util.Optional;

import org.joda.beans.ImmutableBean;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.Tenor;

/**
 * Metadata about a surface and surface parameters.
 * <p>
 * Implementations of this interface are used to store metadata about a surface.
 * For example, a surface may be defined based on financial instruments.
 * The parameters might represent 1 day, 1 week, 1 month, 3 month and 6 months.
 * The metadata could be used to describe each parameter in terms of a {@link Tenor}.
 * <p>
 * This metadata can be used by applications to interpret the parameters of the surface.
 * For example, the scenario framework uses the data when applying perturbations.
 */
public interface SurfaceMetadata
    extends ImmutableBean {

  /**
   * Creates a metadata instance without parameter information.
   * <p>
   * The resulting metadata will have no parameter metadata.
   * For more control, see {@link DefaultSurfaceMetadata}.
   * 
   * @param name  the surface name
   * @return the metadata
   */
  public static SurfaceMetadata of(String name) {
    return of(SurfaceName.of(name));
  }

  /**
   * Creates a metadata instance without parameter information.
   * <p>
   * The resulting metadata will have no parameter metadata.
   * For more control, see {@link DefaultSurfaceMetadata}.
   * 
   * @param name  the surface name
   * @return the metadata
   */
  public static SurfaceMetadata of(SurfaceName name) {
    return DefaultSurfaceMetadata.of(name);
  }

  /**
   * Creates a metadata instance with parameter information.
   * <p>
   * The parameter metadata must match the number of parameters on the surface.
   * An empty list is accepted and interpreted as meaning that no parameter metadata is present.
   * For more control, see {@link DefaultSurfaceMetadata}.
   * 
   * @param name  the surface name
   * @param parameters  the parameter metadata
   * @return the metadata
   */
  public static SurfaceMetadata of(String name, List<? extends SurfaceParameterMetadata> parameters) {
    return of(SurfaceName.of(name), parameters);
  }

  /**
   * Creates a metadata instance with parameter information.
   * <p>
   * The parameter metadata must match the number of parameters on the surface.
   * An empty list is accepted and interpreted as meaning that no parameter metadata is present.
   * For more control, see {@link DefaultSurfaceMetadata}.
   * 
   * @param name  the surface name
   * @param parameters  the parameter metadata
   * @return the metadata
   */
  public static SurfaceMetadata of(SurfaceName name, List<? extends SurfaceParameterMetadata> parameters) {
    return DefaultSurfaceMetadata.of(name, ImmutableList.copyOf(parameters));
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the surface name.
   * 
   * @return the surface name
   */
  public abstract SurfaceName getSurfaceName();

  /**
   * Gets the day count, optional.
   * <p>
   * If the x-value of the surface represents time as a year fraction, the day count
   * can be specified to define how the year fraction is calculated.
   * 
   * @return the day count
   */
  public abstract Optional<DayCount> getDayCount();

  /**
   * Gets metadata about each parameter underlying the surface.
   * <p>
   * If present, the parameter metadata should match the number of parameters on the surface.
   * 
   * @return the parameter metadata
   */
  public abstract Optional<List<SurfaceParameterMetadata>> getParameters();
}
