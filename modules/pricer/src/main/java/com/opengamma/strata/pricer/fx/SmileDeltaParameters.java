/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;

/**
 * Defines a delta dependent smile as used in Forex market.
 * <p>
 * This contains the data for delta dependent smile from at-the-money, risk reversal and strangle.
 * The delta used is the delta with respect to forward.
 */
@BeanDefinition(style = "light")
final class SmileDeltaParameters
    implements ImmutableBean {
  // NOTE: This class is package scoped, as the Smile data provider API is effectively still in Beta

  /**
   * The time to expiry associated to the data.
   */
  @PropertyDefinition
  private final double timeToExpiry;
  /**
   * The delta of the different data points.
   * Must be positive and sorted in ascending order.
   * The put will have as delta the opposite of the numbers.
   */
  @PropertyDefinition
  private final DoubleArray delta;
  /**
   * The volatilities associated with the strikes.
   */
  @PropertyDefinition
  private final DoubleArray volatility;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from volatility.
   * 
   * @param timeToExpiry  the time to expiry associated to the data
   * @param delta  the delta of the different data points, must be positive and sorted in ascending order,
   *  the put will have as delta the opposite of the numbers
   * @param volatility  the volatilities
   * @return the smile definition
   */
  public static SmileDeltaParameters of(double timeToExpiry, DoubleArray delta, DoubleArray volatility) {
    return new SmileDeltaParameters(timeToExpiry, delta, volatility);
  }

  /**
   * Obtains an instance from market data at-the-money, delta, risk-reversal and strangle.
   * 
   * @param timeToExpiry  the time to expiry associated to the data
   * @param atm  the at-the-money volatility
   * @param delta  the delta of the different data points, must be positive and sorted in ascending order,
   *  the put will have as delta the opposite of the numbers
   * @param riskReversal  the risk reversal volatility figures, in the same order as the delta
   * @param strangle  the strangle volatility figures, in the same order as the delta
   * @return the smile definition
   */
  public static SmileDeltaParameters of(
      double timeToExpiry,
      double atm,
      DoubleArray delta,
      DoubleArray riskReversal,
      DoubleArray strangle) {

    ArgChecker.notNull(delta, "Delta");
    ArgChecker.notNull(riskReversal, "Risk Reversal");
    ArgChecker.notNull(strangle, "Strangle");
    ArgChecker.isTrue(delta.size() == riskReversal.size(),
        "Length of delta {} should be equal to length of risk reversal {}", delta.size(), riskReversal.size());
    ArgChecker.isTrue(delta.size() == strangle.size(),
        "Length of delta {} should be equal to length of strangle {} ", delta.size(), strangle.size());
    //TODO: check that delta is sorted (ascending).
    int nbDelta = delta.size();
    double[] volatility = new double[2 * nbDelta + 1];
    volatility[nbDelta] = atm;
    for (int i = 0; i < nbDelta; i++) {
      volatility[i] = strangle.get(i) + atm - riskReversal.get(i) / 2.0; // Put
      volatility[2 * nbDelta - i] = strangle.get(i) + atm + riskReversal.get(i) / 2.0; // Call
    }
    return new SmileDeltaParameters(timeToExpiry, delta, DoubleArray.ofUnsafe(volatility));
  }

  @ImmutableValidator
  private void validate() {
    ArgChecker.isTrue(2 * delta.size() + 1 == volatility.size(),
        "Length of delta {} should be coherent with volatility length {}", 2 * delta.size() + 1, volatility.size());
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the strikes in ascending order.
   * Put with lower delta (in absolute value) first, at-the-money and call with larger delta first.
   * 
   * @param forward  the forward
   * @return the strikes
   */
  public double[] getStrike(double forward) {
    int nbDelta = delta.size();
    double[] strike = new double[2 * nbDelta + 1];
    strike[nbDelta] = forward * Math.exp(volatility.get(nbDelta) * volatility.get(nbDelta) * timeToExpiry / 2.0);
    for (int loopdelta = 0; loopdelta < nbDelta; loopdelta++) {
      strike[loopdelta] = BlackFormulaRepository.impliedStrike(
          -delta.get(loopdelta), false, forward, timeToExpiry, volatility.get(loopdelta)); // Put
      strike[2 * nbDelta - loopdelta] = BlackFormulaRepository.impliedStrike(
          delta.get(loopdelta), true, forward, timeToExpiry, volatility.get(2 * nbDelta - loopdelta)); // Call
    }
    return strike;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SmileDeltaParameters}.
   */
  private static MetaBean META_BEAN = LightMetaBean.of(SmileDeltaParameters.class);

  /**
   * The meta-bean for {@code SmileDeltaParameters}.
   * @return the meta-bean, not null
   */
  public static MetaBean meta() {
    return META_BEAN;
  }

  static {
    JodaBeanUtils.registerMetaBean(META_BEAN);
  }

  private SmileDeltaParameters(
      double timeToExpiry,
      DoubleArray delta,
      DoubleArray volatility) {
    this.timeToExpiry = timeToExpiry;
    this.delta = delta;
    this.volatility = volatility;
    validate();
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
   * Gets the time to expiry associated to the data.
   * @return the value of the property
   */
  public double getTimeToExpiry() {
    return timeToExpiry;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the delta of the different data points.
   * Must be positive and sorted in ascending order.
   * The put will have as delta the opposite of the numbers.
   * @return the value of the property
   */
  public DoubleArray getDelta() {
    return delta;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the volatilities associated with the strikes.
   * @return the value of the property
   */
  public DoubleArray getVolatility() {
    return volatility;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SmileDeltaParameters other = (SmileDeltaParameters) obj;
      return JodaBeanUtils.equal(timeToExpiry, other.timeToExpiry) &&
          JodaBeanUtils.equal(delta, other.delta) &&
          JodaBeanUtils.equal(volatility, other.volatility);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(timeToExpiry);
    hash = hash * 31 + JodaBeanUtils.hashCode(delta);
    hash = hash * 31 + JodaBeanUtils.hashCode(volatility);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("SmileDeltaParameters{");
    buf.append("timeToExpiry").append('=').append(timeToExpiry).append(',').append(' ');
    buf.append("delta").append('=').append(delta).append(',').append(' ');
    buf.append("volatility").append('=').append(JodaBeanUtils.toString(volatility));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
