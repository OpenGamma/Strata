/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.ParameterMetadata;

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
   * It defaults to {@link ValueType#UNKNOWN}.
   */
  private ValueType xValueType = ValueType.UNKNOWN;
  /**
   * The y-value type, providing meaning to the y-values of the curve.
   * <p>
   * This type provides meaning to the y-values. For example, the y-value might
   * represent a zero rate, as represented using {@link ValueType#ZERO_RATE}.
   * It defaults to {@link ValueType#UNKNOWN}.
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
  private List<ParameterMetadata> parameterMetadata;

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
    this.parameterMetadata = beanToCopy.getParameterMetadata().orElse(null);
  }

  //-----------------------------------------------------------------------
  /**
   * Sets the curve name.
   * 
   * @param curveName  the curve name
   * @return this, for chaining
   */
  public DefaultCurveMetadataBuilder curveName(String curveName) {
    this.curveName = CurveName.of(curveName);
    return this;
  }

  /**
   * Sets the curve name.
   * 
   * @param curveName  the curve name
   * @return this, for chaining
   */
  public DefaultCurveMetadataBuilder curveName(CurveName curveName) {
    this.curveName = ArgChecker.notNull(curveName, "curveName");
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
    this.xValueType = ArgChecker.notNull(xValueType, "xValueType");
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
    this.yValueType = ArgChecker.notNull(yValueType, "yValueType");
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
  public <T> DefaultCurveMetadataBuilder addInfo(CurveInfoType<T> type, T value) {
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
   * The parameter metadata must match the number of parameters on the curve.
   * This will replace the existing parameter-level metadata.
   * 
   * @param parameterMetadata  the parameter metadata
   * @return this, for chaining
   */
  public DefaultCurveMetadataBuilder parameterMetadata(List<? extends ParameterMetadata> parameterMetadata) {
    this.parameterMetadata = ImmutableList.copyOf(parameterMetadata);
    return this;
  }

  /**
   * Sets the parameter-level metadata.
   * <p>
   * The parameter metadata must match the number of parameters on the curve.
   * This will replace the existing parameter-level metadata.
   * 
   * @param parameterMetadata  the parameter metadata
   * @return this, for chaining
   */
  public DefaultCurveMetadataBuilder parameterMetadata(ParameterMetadata... parameterMetadata) {
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
  public DefaultCurveMetadataBuilder clearParameterMetadata() {
    this.parameterMetadata = null;
    return this;
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
