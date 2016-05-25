/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.ParameterMetadata;

/**
 * Builder for surface metadata.
 * <p>
 * This is created using {@link DefaultSurfaceMetadata#builder()}.
 */
public final class DefaultSurfaceMetadataBuilder {

  /**
   * The surface name.
   */
  private SurfaceName surfaceName;
  /**
   * The x-value type, providing meaning to the x-values of the surface.
   * <p>
   * This type provides meaning to the x-values.
   * It defaults to {@link ValueType#UNKNOWN}.
   */
  private ValueType xValueType = ValueType.UNKNOWN;
  /**
   * The y-value type, providing meaning to the y-values of the surface.
   * <p>
   * This type provides meaning to the y-values.
   * It defaults to {@link ValueType#UNKNOWN}.
   */
  private ValueType yValueType = ValueType.UNKNOWN;
  /**
   * The y-value type, providing meaning to the z-values of the surface.
   * <p>
   * This type provides meaning to the z-values.
   * It defaults to {@link ValueType#UNKNOWN}.
   */
  private ValueType zValueType = ValueType.UNKNOWN;
  /**
   * The additional surface information.
   * <p>
   * This stores additional information for the surface.
   */
  private final Map<SurfaceInfoType<?>, Object> info = new HashMap<>();
  /**
   * The metadata about the parameters.
   * <p>
   * If present, the parameter metadata will match the number of parameters on the surface.
   */
  private List<ParameterMetadata> parameterMetadata;

  /**
   * Restricted constructor.
   */
  DefaultSurfaceMetadataBuilder() {
  }

  /**
   * Restricted copy constructor.
   * 
   * @param beanToCopy  the bean to copy from
   */
  DefaultSurfaceMetadataBuilder(DefaultSurfaceMetadata beanToCopy) {
    this.surfaceName = beanToCopy.getSurfaceName();
    this.xValueType = beanToCopy.getXValueType();
    this.yValueType = beanToCopy.getYValueType();
    this.zValueType = beanToCopy.getZValueType();
    this.info.putAll(beanToCopy.getInfo());
    this.parameterMetadata = beanToCopy.getParameterMetadata().orElse(null);
  }

  //-----------------------------------------------------------------------
  /**
   * Sets the surface name.
   * 
   * @param surfaceName  the surface name
   * @return this, for chaining
   */
  public DefaultSurfaceMetadataBuilder surfaceName(String surfaceName) {
    this.surfaceName = SurfaceName.of(surfaceName);
    return this;
  }

  /**
   * Sets the surface name.
   * 
   * @param surfaceName  the surface name
   * @return this, for chaining
   */
  public DefaultSurfaceMetadataBuilder surfaceName(SurfaceName surfaceName) {
    this.surfaceName = ArgChecker.notNull(surfaceName, "surfaceName");
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Sets the x-value type, providing meaning to the x-values of the surface.
   * <p>
   * This type provides meaning to the x-values.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   * 
   * @param xValueType  the x-value type
   * @return this, for chaining
   */
  public DefaultSurfaceMetadataBuilder xValueType(ValueType xValueType) {
    this.xValueType = ArgChecker.notNull(xValueType, "xValueType");
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Sets the y-value type, providing meaning to the y-values of the surface.
   * <p>
   * This type provides meaning to the y-values.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   * 
   * @param yValueType  the y-value type
   * @return this, for chaining
   */
  public DefaultSurfaceMetadataBuilder yValueType(ValueType yValueType) {
    this.yValueType = ArgChecker.notNull(yValueType, "yValueType");
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Sets the z-value type, providing meaning to the z-values of the surface.
   * <p>
   * This type provides meaning to the z-values.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   * 
   * @param zValueType  the z-value type
   * @return this, for chaining
   */
  public DefaultSurfaceMetadataBuilder zValueType(ValueType zValueType) {
    this.zValueType = ArgChecker.notNull(zValueType, "zValueType");
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Sets the day count.
   * <p>
   * This stores the day count in the additional information map using the
   * key {@link SurfaceInfoType#DAY_COUNT}.
   * <p>
   * This is stored in the additional information map using {@code Map.put} semantics,
   * removing the key if the day count is null.
   * 
   * @param dayCount  the day count, may be null
   * @return this, for chaining
   */
  public DefaultSurfaceMetadataBuilder dayCount(DayCount dayCount) {
    return addInfo(SurfaceInfoType.DAY_COUNT, dayCount);
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a single piece of additional information.
   * <p>
   * This is stored in the additional information map using {@code Map.put} semantics,
   * removing the key if the instance is null.
   * 
   * @param <T>  the type of the info
   * @param type  the type to store under
   * @param value  the value to store, may be null
   * @return this, for chaining
   */
  public <T> DefaultSurfaceMetadataBuilder addInfo(SurfaceInfoType<T> type, T value) {
    ArgChecker.notNull(type, "type");
    if (value != null) {
      this.info.put(type, value);
    } else {
      this.info.remove(type);
    }
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Sets the parameter-level metadata.
   * <p>
   * The parameter metadata must match the number of parameters on the surface.
   * This will replace the existing parameter-level metadata.
   * 
   * @param parameterMetadata  the parameter metadata
   * @return this, for chaining
   */
  public DefaultSurfaceMetadataBuilder parameterMetadata(List<? extends ParameterMetadata> parameterMetadata) {
    this.parameterMetadata = ImmutableList.copyOf(parameterMetadata);
    return this;
  }

  /**
   * Sets the parameter-level metadata.
   * <p>
   * The parameter metadata must match the number of parameters on the surface.
   * This will replace the existing parameter-level metadata.
   * 
   * @param parameterMetadata  the parameter metadata
   * @return this, for chaining
   */
  public DefaultSurfaceMetadataBuilder parameterMetadata(ParameterMetadata... parameterMetadata) {
    this.parameterMetadata = ImmutableList.copyOf(parameterMetadata);
    return this;
  }

  /**
   * Clears the parameter-level metadata.
   * <p>
   * The existing parameter-level metadata will be removed.
   * 
   * @return this, for chaining
   */
  public DefaultSurfaceMetadataBuilder clearParameterMetadata() {
    this.parameterMetadata = null;
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Builds the metadata instance.
   * 
   * @return the instance
   */
  public DefaultSurfaceMetadata build() {
    return new DefaultSurfaceMetadata(surfaceName, xValueType, yValueType, zValueType, info, parameterMetadata);
  }

}
