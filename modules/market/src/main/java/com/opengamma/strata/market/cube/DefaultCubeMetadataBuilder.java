/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.cube;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.ParameterMetadata;

/**
 * Builder for cube metadata.
 * <p>
 * This is created using {@link DefaultCubeMetadata#builder()}.
 */
public final class DefaultCubeMetadataBuilder {

  /**
   * The cube name.
   */
  private CubeName cubeName;
  /**
   * The x-value type, providing meaning to the x-values of the cube.
   * <p>
   * This type provides meaning to the x-values.
   * It defaults to {@link ValueType#UNKNOWN}.
   */
  private ValueType xValueType = ValueType.UNKNOWN;
  /**
   * The y-value type, providing meaning to the y-values of the cube.
   * <p>
   * This type provides meaning to the y-values.
   * It defaults to {@link ValueType#UNKNOWN}.
   */
  private ValueType yValueType = ValueType.UNKNOWN;
  /**
   * The z-value type, providing meaning to the z-values of the cube.
   * <p>
   * This type provides meaning to the z-values.
   * It defaults to {@link ValueType#UNKNOWN}.
   */
  private ValueType zValueType = ValueType.UNKNOWN;
  /**
   * The w-value type, providing meaning to the w-values of the cube.
   * <p>
   * This type provides meaning to the w-values.
   * It defaults to {@link ValueType#UNKNOWN}.
   */
  private ValueType wValueType = ValueType.UNKNOWN;
  /**
   * The additional cube information.
   * <p>
   * This stores additional information for the cube.
   */
  private final Map<CubeInfoType<?>, Object> info = new HashMap<>();
  /**
   * The metadata about the parameters.
   * <p>
   * If present, the parameter metadata will match the number of parameters on the cube.
   */
  private List<ParameterMetadata> parameterMetadata;

  /**
   * Restricted constructor.
   */
  DefaultCubeMetadataBuilder() {
  }

  /**
   * Restricted copy constructor.
   *
   * @param beanToCopy  the bean to copy from
   */
  DefaultCubeMetadataBuilder(DefaultCubeMetadata beanToCopy) {
    this.cubeName = beanToCopy.getCubeName();
    this.xValueType = beanToCopy.getXValueType();
    this.yValueType = beanToCopy.getYValueType();
    this.zValueType = beanToCopy.getZValueType();
    this.wValueType = beanToCopy.getWValueType();
    this.info.putAll(beanToCopy.getInfo());
    this.parameterMetadata = beanToCopy.getParameterMetadata().orElse(null);
  }

  //-----------------------------------------------------------------------
  /**
   * Sets the cube name.
   *
   * @param cubeName  the cube name
   * @return this, for chaining
   */
  public DefaultCubeMetadataBuilder cubeName(String cubeName) {
    this.cubeName = CubeName.of(cubeName);
    return this;
  }

  /**
   * Sets the cube name.
   *
   * @param cubeName  the cube name
   * @return this, for chaining
   */
  public DefaultCubeMetadataBuilder cubeName(CubeName cubeName) {
    this.cubeName = ArgChecker.notNull(cubeName, "cubeName");
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Sets the x-value type, providing meaning to the x-values of the cube.
   * <p>
   * This type provides meaning to the x-values.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   *
   * @param xValueType  the x-value type
   * @return this, for chaining
   */
  public DefaultCubeMetadataBuilder xValueType(ValueType xValueType) {
    this.xValueType = ArgChecker.notNull(xValueType, "xValueType");
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Sets the y-value type, providing meaning to the y-values of the cube.
   * <p>
   * This type provides meaning to the y-values.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   *
   * @param yValueType  the y-value type
   * @return this, for chaining
   */
  public DefaultCubeMetadataBuilder yValueType(ValueType yValueType) {
    this.yValueType = ArgChecker.notNull(yValueType, "yValueType");
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Sets the z-value type, providing meaning to the z-values of the cube.
   * <p>
   * This type provides meaning to the z-values.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   *
   * @param zValueType  the z-value type
   * @return this, for chaining
   */
  public DefaultCubeMetadataBuilder zValueType(ValueType zValueType) {
    this.zValueType = ArgChecker.notNull(zValueType, "zValueType");
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Sets the w-value type, providing meaning to the w-values of the cube.
   * <p>
   * This type provides meaning to the w-values.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   *
   * @param wValueType  the w-value type
   * @return this, for chaining
   */
  public DefaultCubeMetadataBuilder wValueType(ValueType wValueType) {
    this.wValueType = ArgChecker.notNull(wValueType, "wValueType");
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Sets the day count.
   * <p>
   * This stores the day count in the additional information map using the
   * key {@link CubeInfoType#DAY_COUNT}.
   * <p>
   * This is stored in the additional information map using {@code Map.put} semantics,
   * removing the key if the day count is null.
   *
   * @param dayCount  the day count, may be null
   * @return this, for chaining
   */
  public DefaultCubeMetadataBuilder dayCount(DayCount dayCount) {
    return addInfo(CubeInfoType.DAY_COUNT, dayCount);
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
  public <T> DefaultCubeMetadataBuilder addInfo(CubeInfoType<T> type, T value) {
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
   * The parameter metadata must match the number of parameters on the cube.
   * This will replace the existing parameter-level metadata.
   *
   * @param parameterMetadata  the parameter metadata
   * @return this, for chaining
   */
  public DefaultCubeMetadataBuilder parameterMetadata(List<? extends ParameterMetadata> parameterMetadata) {
    this.parameterMetadata = ImmutableList.copyOf(parameterMetadata);
    return this;
  }

  /**
   * Sets the parameter-level metadata.
   * <p>
   * The parameter metadata must match the number of parameters on the cube.
   * This will replace the existing parameter-level metadata.
   *
   * @param parameterMetadata  the parameter metadata
   * @return this, for chaining
   */
  public DefaultCubeMetadataBuilder parameterMetadata(ParameterMetadata... parameterMetadata) {
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
  public DefaultCubeMetadataBuilder clearParameterMetadata() {
    this.parameterMetadata = null;
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Builds the metadata instance.
   *
   * @return the instance
   */
  public DefaultCubeMetadata build() {
    return new DefaultCubeMetadata(cubeName, xValueType, yValueType, zValueType, wValueType, info, parameterMetadata);
  }

}
