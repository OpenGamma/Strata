/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.ValueType;

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
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   */
  private ValueType xValueType = ValueType.UNKNOWN;
  /**
   * The y-value type, providing meaning to the y-values of the surface.
   * <p>
   * This type provides meaning to the y-values.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   */
  private ValueType yValueType = ValueType.UNKNOWN;
  /**
   * The y-value type, providing meaning to the z-values of the surface.
   * <p>
   * This type provides meaning to the z-values.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
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
  private List<SurfaceParameterMetadata> parameterMetadata;

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
    this.parameterMetadata = beanToCopy.getParameterMetadata().map(m -> new ArrayList<>(m)).orElse(null);
  }

  //-----------------------------------------------------------------------
  /**
   * Sets the surface name.
   * 
   * @param surfaceName  the surface name
   * @return this, for chaining
   */
  public DefaultSurfaceMetadataBuilder surfaceName(String surfaceName) {
    ArgChecker.notNull(surfaceName, "surfaceName");
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
    ArgChecker.notNull(surfaceName, "surfaceName");
    this.surfaceName = surfaceName;
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
    ArgChecker.notNull(xValueType, "xValueType");
    this.xValueType = xValueType;
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
    ArgChecker.notNull(yValueType, "yValueType");
    this.yValueType = yValueType;
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
    ArgChecker.notNull(zValueType, "zValueType");
    this.zValueType = zValueType;
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
   * @param instance  the instance to store, may be null
   * @return this, for chaining
   */
  public <T> DefaultSurfaceMetadataBuilder addInfo(SurfaceInfoType<T> type, T instance) {
    ArgChecker.notNull(type, "type");
    if (instance != null) {
      this.info.put(type, instance);
    } else {
      this.info.remove(type);
    }
    return this;
  }

  /**
   * Adds additional information.
   * <p>
   * This is stored in the additional information map using {@code Map.putAll} semantics
   * 
   * @param info  the information to add
   * @return this, for chaining
   */
  public DefaultSurfaceMetadataBuilder addInfo(Map<SurfaceInfoType<?>, Object> info) {
    ArgChecker.notNull(info, "infoMap");
    this.info.putAll(info);
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a single piece of parameter metadata.
   * <p>
   * This is stored in the parameter metadata list.
   * 
   * @param parameterMetadata  the parameter metadata
   * @return this, for chaining
   */
  public DefaultSurfaceMetadataBuilder addParameterMetadata(SurfaceParameterMetadata parameterMetadata) {
    if (this.parameterMetadata == null) {
      this.parameterMetadata = new ArrayList<>();
    }
    this.parameterMetadata.add(parameterMetadata);
    return this;
  }

  /**
   * Sets the metadata about the parameters.
   * <p>
   * If present, the parameter metadata will match the number of parameters on the surface.
   * <p>
   * This will replace all existing data in the metadata list.
   * 
   * @param parameterMetadata  the parameter metadata, may be null
   * @return this, for chaining
   */
  public DefaultSurfaceMetadataBuilder parameterMetadata(List<? extends SurfaceParameterMetadata> parameterMetadata) {
    if (parameterMetadata == null) {
      this.parameterMetadata = null;
    } else {
      if (this.parameterMetadata == null) {
        this.parameterMetadata = new ArrayList<>();
      }
      this.parameterMetadata.clear();
      this.parameterMetadata.addAll(parameterMetadata);
    }
    return this;
  }

  /**
   * Sets the {@code parameterMetadata} property in the builder from an array of objects.
   * <p>
   * This will replace all existing data in the metadata list.
   * 
   * @param parameterMetadata  the new value
   * @return this, for chaining
   */
  public DefaultSurfaceMetadataBuilder parameterMetadata(SurfaceParameterMetadata... parameterMetadata) {
    return parameterMetadata(ImmutableList.copyOf(parameterMetadata));
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
