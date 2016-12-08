/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.option;

import java.io.Serializable;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.ValueType;

/**
 * Raw data from the volatility market.
 */
@BeanDefinition(style = "light")
public final class RawOptionData
    implements ImmutableBean, Serializable {

  /**
   * The expiry values.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<Period> expiries;
  /**
   * The strike values. Can be directly strike or moneyness (simple or log)
   */
  @PropertyDefinition(validate = "notNull")
  private final DoubleArray strikes;
  /**
   * The value type of the strike-like dimension.
   */
  @PropertyDefinition(validate = "notNull")
  private final ValueType strikeType;
  /**
   * The data. The values can be model parameters (like Black or normal volatilities) or direct 
   * option prices. The first (outer) dimension is the expiry, the second dimension is the strike.
   * A 'NaN' value indicates that the data is not available.
   */
  @PropertyDefinition(validate = "notNull")
  private final DoubleMatrix data;
  /**
   * The measurement error of the option data.
   * <p>
   * These will be used if the option data is calibrated by a least square method. 
   * {@code data} and {@code error} must have the same number of elements.
   */
  @PropertyDefinition(get = "optional")
  private final DoubleMatrix error;
  /**
   * The type of the raw data.
   */
  @PropertyDefinition(validate = "notNull")
  private final ValueType dataType;
  /**
   * The shift for which the raw data is valid. Used only if the dataType is 'BlackVolatility'.
   */
  @PropertyDefinition(get = "optional")
  private final Double shift;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance of the raw volatility.
   * <p>
   * The data values can be model parameters (like Black or normal volatilities) or direct option prices.
   * 
   * @param expiries  the expiries
   * @param strikes  the strikes-like data
   * @param strikeType  the value type of the strike-like dimension
   * @param data  the data
   * @param dataType  the data type
   * @return the instance
   */
  public static RawOptionData of(
      List<Period> expiries,
      DoubleArray strikes,
      ValueType strikeType,
      DoubleMatrix data,
      ValueType dataType) {

    ArgChecker.isTrue(expiries.size() == data.rowCount(),
        "expiries list should be of the same size as the external data dimension");
    for (int i = 0; i < expiries.size(); i++) {
      ArgChecker.isTrue(strikes.size() == data.columnCount(),
          "strikes should be of the same size as the inner data dimension");
    }
    return new RawOptionData(expiries, strikes, strikeType, data, null, dataType, 0.0);
  }

  /**
   * Obtains an instance of the raw volatility for shifted Black (log-normal) volatility.
   * 
   * @param expiries  the expiries
   * @param strikes  the strikes-like data
   * @param strikeType  the value type of the strike-like dimension
   * @param data  the data
   * @param shift  the shift
   * @return the instance
   */
  public static RawOptionData ofBlackVolatility(
      List<Period> expiries,
      DoubleArray strikes,
      ValueType strikeType,
      DoubleMatrix data,
      Double shift) {

    ArgChecker.isTrue(expiries.size() == data.rowCount(),
        "expiries list should be of the same size as the external data dimension");
    for (int i = 0; i < expiries.size(); i++) {
      ArgChecker.isTrue(strikes.size() == data.columnCount(),
          "strikes should be of the same size as the inner data dimension");
    }
    return new RawOptionData(expiries, strikes, strikeType, data, null, ValueType.BLACK_VOLATILITY, shift);
  }

  /**
   * Obtains an instance of the raw data with error.
   * <p>
   * The data values can be model parameters (like Black or normal volatilities) or direct option prices.
   * 
   * @param expiries  the expiries
   * @param strikes  the strikes-like data
   * @param strikeType  the value type of the strike-like dimension
   * @param data  the data
   * @param error  the error
   * @param dataType  the data type
   * @return the instance
   */
  public static RawOptionData of(
      List<Period> expiries,
      DoubleArray strikes,
      ValueType strikeType,
      DoubleMatrix data,
      DoubleMatrix error,
      ValueType dataType) {

    ArgChecker.isTrue(expiries.size() == data.rowCount(),
        "expiries list should be of the same size as the external data dimension");
    ArgChecker.isTrue(error.rowCount() == data.rowCount(),
        "the error row count should be the same as the data raw count");
    ArgChecker.isTrue(error.columnCount() == data.columnCount(),
        "the error column count should the same as the data column count");
    for (int i = 0; i < expiries.size(); i++) {
      ArgChecker.isTrue(strikes.size() == data.columnCount(),
          "strikes should be of the same size as the inner data dimension");
    }
    return new RawOptionData(expiries, strikes, strikeType, data, error, dataType, 0.0);
  }

  /**
   * Obtains an instance of the raw data with error for shifted Black (log-normal) volatility.
   * 
   * @param expiries  the expiries
   * @param strikes  the strikes-like data
   * @param strikeType  the value type of the strike-like dimension
   * @param data  the data
   * @param error  the error
   * @param shift  the shift
   * @return the instance
   */
  public static RawOptionData ofBlackVolatility(
      List<Period> expiries,
      DoubleArray strikes,
      ValueType strikeType,
      DoubleMatrix data,
      DoubleMatrix error,
      Double shift) {

    ArgChecker.isTrue(expiries.size() == data.rowCount(),
        "expiries list should be of the same size as the external data dimension");
    for (int i = 0; i < expiries.size(); i++) {
      ArgChecker.isTrue(strikes.size() == data.columnCount(),
          "strikes should be of the same size as the inner data dimension");
    }
    return new RawOptionData(expiries, strikes, strikeType, data, error, ValueType.BLACK_VOLATILITY, shift);
  }

  //-------------------------------------------------------------------------
  /**
   * For a given expiration returns all the data available.
   * 
   * @param expiry  the expiration
   * @return the strikes and related volatilities for all available data at the given expiration
   */
  public Pair<DoubleArray, DoubleArray> availableSmileAtExpiry(Period expiry) {
    int index = expiries.indexOf(expiry);
    ArgChecker.isTrue(index >= 0, "expiry not available");
    List<Double> strikesAvailable = new ArrayList<>();
    List<Double> volatilitiesAvailable = new ArrayList<>();
    for (int i = 0; i < strikes.size(); i++) {
      if (!Double.isNaN(data.get(index, i))) {
        strikesAvailable.add(strikes.get(i));
        volatilitiesAvailable.add(data.get(index, i));
      }
    }
    return Pair.of(DoubleArray.copyOf(strikesAvailable), DoubleArray.copyOf(volatilitiesAvailable));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code RawOptionData}.
   */
  private static MetaBean META_BEAN = LightMetaBean.of(RawOptionData.class);

  /**
   * The meta-bean for {@code RawOptionData}.
   * @return the meta-bean, not null
   */
  public static MetaBean meta() {
    return META_BEAN;
  }

  static {
    JodaBeanUtils.registerMetaBean(META_BEAN);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private RawOptionData(
      List<Period> expiries,
      DoubleArray strikes,
      ValueType strikeType,
      DoubleMatrix data,
      DoubleMatrix error,
      ValueType dataType,
      Double shift) {
    JodaBeanUtils.notNull(expiries, "expiries");
    JodaBeanUtils.notNull(strikes, "strikes");
    JodaBeanUtils.notNull(strikeType, "strikeType");
    JodaBeanUtils.notNull(data, "data");
    JodaBeanUtils.notNull(dataType, "dataType");
    this.expiries = ImmutableList.copyOf(expiries);
    this.strikes = strikes;
    this.strikeType = strikeType;
    this.data = data;
    this.error = error;
    this.dataType = dataType;
    this.shift = shift;
  }

  @Override
  public MetaBean metaBean() {
    return META_BEAN;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the expiry values.
   * @return the value of the property, not null
   */
  public ImmutableList<Period> getExpiries() {
    return expiries;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the strike values. Can be directly strike or moneyness (simple or log)
   * @return the value of the property, not null
   */
  public DoubleArray getStrikes() {
    return strikes;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the value type of the strike-like dimension.
   * @return the value of the property, not null
   */
  public ValueType getStrikeType() {
    return strikeType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the data. The values can be model parameters (like Black or normal volatilities) or direct
   * option prices. The first (outer) dimension is the expiry, the second dimension is the strike.
   * A 'NaN' value indicates that the data is not available.
   * @return the value of the property, not null
   */
  public DoubleMatrix getData() {
    return data;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the measurement error of the option data.
   * <p>
   * These will be used if the option data is calibrated by a least square method.
   * {@code data} and {@code error} must have the same number of elements.
   * @return the optional value of the property, not null
   */
  public Optional<DoubleMatrix> getError() {
    return Optional.ofNullable(error);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the type of the raw data.
   * @return the value of the property, not null
   */
  public ValueType getDataType() {
    return dataType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the shift for which the raw data is valid. Used only if the dataType is 'BlackVolatility'.
   * @return the optional value of the property, not null
   */
  public OptionalDouble getShift() {
    return shift != null ? OptionalDouble.of(shift) : OptionalDouble.empty();
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      RawOptionData other = (RawOptionData) obj;
      return JodaBeanUtils.equal(expiries, other.expiries) &&
          JodaBeanUtils.equal(strikes, other.strikes) &&
          JodaBeanUtils.equal(strikeType, other.strikeType) &&
          JodaBeanUtils.equal(data, other.data) &&
          JodaBeanUtils.equal(error, other.error) &&
          JodaBeanUtils.equal(dataType, other.dataType) &&
          JodaBeanUtils.equal(shift, other.shift);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(expiries);
    hash = hash * 31 + JodaBeanUtils.hashCode(strikes);
    hash = hash * 31 + JodaBeanUtils.hashCode(strikeType);
    hash = hash * 31 + JodaBeanUtils.hashCode(data);
    hash = hash * 31 + JodaBeanUtils.hashCode(error);
    hash = hash * 31 + JodaBeanUtils.hashCode(dataType);
    hash = hash * 31 + JodaBeanUtils.hashCode(shift);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("RawOptionData{");
    buf.append("expiries").append('=').append(expiries).append(',').append(' ');
    buf.append("strikes").append('=').append(strikes).append(',').append(' ');
    buf.append("strikeType").append('=').append(strikeType).append(',').append(' ');
    buf.append("data").append('=').append(data).append(',').append(' ');
    buf.append("error").append('=').append(error).append(',').append(' ');
    buf.append("dataType").append('=').append(dataType).append(',').append(' ');
    buf.append("shift").append('=').append(JodaBeanUtils.toString(shift));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
