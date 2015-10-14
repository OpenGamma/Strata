/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.value.ValueType;

/**
 * Builder for curve metadata.
 * <p>
 * This is created using {@link DefaultCurveMetadata#builder()}.
 */
public final class DefaultCurveMetadataBuilder {

  /**
   * The curve name.
   */
  private CurveName curveName;
  /**
   * The x-value type, providing meaning to the x-values of the curve.
   * <p>
   * This type provides meaning to the x-values. For example, the x-value might
   * represent a year fraction, as represented using {@link ValueType#YEAR_FRACTION}.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   */
  private ValueType xValueType = ValueType.UNKNOWN;
  /**
   * The y-value type, providing meaning to the y-values of the curve.
   * <p>
   * This type provides meaning to the y-values. For example, the y-value might
   * represent a zero rate, as represented using {@link ValueType#ZERO_RATE}.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   */
  private ValueType yValueType = ValueType.UNKNOWN;
  /**
   * The additional curve information.
   * <p>
   * This stores additional information for the curve.
   * <p>
   * The most common information is the {@linkplain CurveInfoType#DAY_COUNT day count}
   * and {@linkplain CurveInfoType#JACOBIAN curve calibration Jacobian}.
   */
  private final Map<CurveInfoType<?>, Object> info = new HashMap<>();
  /**
   * The metadata about the parameters.
   * <p>
   * If present, the parameter metadata will match the number of parameters on the curve.
   */
  private List<CurveParameterMetadata> parameterMetadata;

  /**
   * Restricted constructor.
   */
  DefaultCurveMetadataBuilder() {
  }

  /**
   * Restricted copy constructor.
   * 
   * @param beanToCopy  the bean to copy from
   */
  DefaultCurveMetadataBuilder(DefaultCurveMetadata beanToCopy) {
    this.curveName = beanToCopy.getCurveName();
    this.xValueType = beanToCopy.getXValueType();
    this.yValueType = beanToCopy.getYValueType();
    this.info.putAll(beanToCopy.getInfo());
    this.parameterMetadata = beanToCopy.getParameterMetadata().map(m -> new ArrayList<>(m)).orElse(null);
  }

  //-----------------------------------------------------------------------
  /**
   * Sets the curve name.
   * 
   * @param curveName  the curve name
   * @return this, for chaining
   */
  public DefaultCurveMetadataBuilder curveName(CurveName curveName) {
    ArgChecker.notNull(curveName, "curveName");
    this.curveName = curveName;
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Sets the x-value type, providing meaning to the x-values of the curve.
   * <p>
   * This type provides meaning to the x-values. For example, the x-value might
   * represent a year fraction, as represented using {@link ValueType#YEAR_FRACTION}.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   * 
   * @param xValueType  the x-value type
   * @return this, for chaining
   */
  public DefaultCurveMetadataBuilder xValueType(ValueType xValueType) {
    ArgChecker.notNull(xValueType, "xValueType");
    this.xValueType = xValueType;
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Sets the y-value type, providing meaning to the y-values of the curve.
   * <p>
   * This type provides meaning to the y-values. For example, the y-value might
   * represent a zero rate, as represented using {@link ValueType#ZERO_RATE}.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   * 
   * @param yValueType  the y-value type
   * @return this, for chaining
   */
  public DefaultCurveMetadataBuilder yValueType(ValueType yValueType) {
    ArgChecker.notNull(yValueType, "yValueType");
    this.yValueType = yValueType;
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Sets the day count.
   * <p>
   * This stores the day count in the additional information map using the
   * key {@link CurveInfoType#DAY_COUNT}.
   * <p>
   * This is stored in the additional information map using {@code Map.put} semantics,
   * removing the key if the day count is null.
   * 
   * @param dayCount  the day count, may be null
   * @return this, for chaining
   */
  public DefaultCurveMetadataBuilder dayCount(DayCount dayCount) {
    return addInfo(CurveInfoType.DAY_COUNT, dayCount);
  }

  //-------------------------------------------------------------------------
  /**
   * Sets the calibration information.
   * <p>
   * This stores the calibration information in the additional information map
   * using the key {@link CurveInfoType#JACOBIAN}.
   * <p>
   * This is stored in the additional information map using {@code Map.put} semantics,
   * removing the key if the jacobian is null.
   * 
   * @param jacobian  the calibration information, may be null
   * @return this, for chaining
   */
  public DefaultCurveMetadataBuilder jacobian(JacobianCalibrationMatrix jacobian) {
    return addInfo(CurveInfoType.JACOBIAN, jacobian);
  }

  /**
   * Adds a single piece of additional information.
   * <p>
   * This is stored in the additional information map using {@code Map.put} semantics,
   * removing the key if the instance is null.
   * 
   * @param type  the type to store under
   * @param instance  the instance to store, may be null
   * @return this, for chaining
   */
  public <T> DefaultCurveMetadataBuilder addInfo(CurveInfoType<T> type, T instance) {
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
  public <T> DefaultCurveMetadataBuilder addInfo(Map<CurveInfoType<?>, Object> info) {
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
  public <T> DefaultCurveMetadataBuilder addParameterMetadata(CurveParameterMetadata parameterMetadata) {
    if (this.parameterMetadata == null) {
      this.parameterMetadata = new ArrayList<>();
    }
    this.parameterMetadata.add(parameterMetadata);
    return this;
  }

  /**
   * Sets the metadata about the parameters.
   * <p>
   * If present, the parameter metadata will match the number of parameters on the curve.
   * <p>
   * This will replace all existing data in the metadata list.
   * 
   * @param parameterMetadata  the parameter metadata, may be null
   * @return this, for chaining
   */
  public DefaultCurveMetadataBuilder parameterMetadata(List<? extends CurveParameterMetadata> parameterMetadata) {
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
  public DefaultCurveMetadataBuilder parameterMetadata(CurveParameterMetadata... parameterMetadata) {
    return parameterMetadata(ImmutableList.copyOf(parameterMetadata));
  }

  //-------------------------------------------------------------------------
  /**
   * Builds the metadata instance.
   * 
   * @return the instance
   */
  public DefaultCurveMetadata build() {
    return new DefaultCurveMetadata(curveName, xValueType, yValueType, info, parameterMetadata);
  }

}
