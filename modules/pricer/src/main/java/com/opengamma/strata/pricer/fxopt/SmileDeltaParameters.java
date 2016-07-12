/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.ParameterizedData;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;

/**
 * A delta dependent smile as used in Forex market.
 * <p>
 * This contains the data for delta dependent smile from at-the-money, risk reversal and strangle.
 * The delta used is the delta with respect to forward.
 */
@BeanDefinition(builderScope = "private")
public final class SmileDeltaParameters
    implements ParameterizedData, ImmutableBean, Serializable {

  /**
   * The time to expiry associated with the data.
   */
  @PropertyDefinition
  private final double expiry;
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
   * @param expiry  the time to expiry associated to the data
   * @param delta  the delta of the different data points, must be positive and sorted in ascending order,
   *  the put will have as delta the opposite of the numbers
   * @param volatility  the volatilities
   * @return the smile definition
   */
  public static SmileDeltaParameters of(double expiry, DoubleArray delta, DoubleArray volatility) {
    ArgChecker.notNull(delta, "delta");
    ArgChecker.notNull(volatility, "volatility");
    return new SmileDeltaParameters(expiry, delta, volatility);
  }

  /**
   * Obtains an instance from market data at-the-money, delta, risk-reversal and strangle.
   * 
   * @param expiry  the time to expiry associated to the data
   * @param atmVolatility  the at-the-money volatility
   * @param delta  the delta of the different data points, must be positive and sorted in ascending order,
   *  the put will have as delta the opposite of the numbers
   * @param riskReversal  the risk reversal volatility figures, in the same order as the delta
   * @param strangle  the strangle volatility figures, in the same order as the delta
   * @return the smile definition
   */
  public static SmileDeltaParameters of(
      double expiry,
      double atmVolatility,
      DoubleArray delta,
      DoubleArray riskReversal,
      DoubleArray strangle) {

    ArgChecker.notNull(delta, "delta");
    ArgChecker.notNull(riskReversal, "riskReversal");
    ArgChecker.notNull(strangle, "strangle");
    ArgChecker.isTrue(delta.size() == riskReversal.size(),
        "Length of delta {} should be equal to length of riskReversal {}", delta.size(), riskReversal.size());
    ArgChecker.isTrue(delta.size() == strangle.size(),
        "Length of delta {} should be equal to length of strangle {} ", delta.size(), strangle.size());
    int nbDelta = delta.size();
    double[] volatility = new double[2 * nbDelta + 1];
    volatility[nbDelta] = atmVolatility;
    for (int i = 0; i < nbDelta; i++) {
      volatility[i] = strangle.get(i) + atmVolatility - riskReversal.get(i) / 2.0; // Put
      volatility[2 * nbDelta - i] = strangle.get(i) + atmVolatility + riskReversal.get(i) / 2.0; // Call
    }
    return new SmileDeltaParameters(expiry, delta, DoubleArray.ofUnsafe(volatility));
  }

  @ImmutableValidator
  private void validate() {
    int nbDelta = delta.size();
    ArgChecker.isTrue(2 * nbDelta + 1 == volatility.size(),
        "Length of delta {} should be coherent with volatility length {}", 2 * delta.size() + 1, volatility.size());
    if (nbDelta > 1) {
      for (int i = 1; i < nbDelta; ++i) {
        ArgChecker.isTrue(delta.get(i - 1) < delta.get(i), "delta should be sorted in ascending order");
      }
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public int getParameterCount() {
    return volatility.size();
  }

  @Override
  public double getParameter(int parameterIndex) {
    return volatility.get(parameterIndex);
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    return ParameterMetadata.empty();
  }

  @Override
  public SmileDeltaParameters withParameter(int parameterIndex, double newValue) {
    return new SmileDeltaParameters(expiry, delta, volatility.with(parameterIndex, newValue));
  }

  @Override
  public SmileDeltaParameters withPerturbation(ParameterPerturbation perturbation) {
    int size = volatility.size();
    DoubleArray perturbedValues = DoubleArray.of(
        size, i -> perturbation.perturbParameter(i, volatility.get(i), getParameterMetadata(i)));
    return new SmileDeltaParameters(expiry, delta, perturbedValues);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the strikes in ascending order.
   * <p>
   * The result has twice the number of values plus one as the delta/volatility.
   * The put with lower delta (in absolute value) first, at-the-money and call with larger delta first.
   * 
   * @param forward  the forward
   * @return the strikes
   */
  public DoubleArray strike(double forward) {
    int nbDelta = delta.size();
    double[] strike = new double[2 * nbDelta + 1];
    strike[nbDelta] = forward * Math.exp(volatility.get(nbDelta) * volatility.get(nbDelta) * expiry / 2.0);
    for (int loopdelta = 0; loopdelta < nbDelta; loopdelta++) {
      strike[loopdelta] = BlackFormulaRepository.impliedStrike(
          -delta.get(loopdelta), false, forward, expiry, volatility.get(loopdelta)); // Put
      strike[2 * nbDelta - loopdelta] = BlackFormulaRepository.impliedStrike(
          delta.get(loopdelta), true, forward, expiry, volatility.get(2 * nbDelta - loopdelta)); // Call
    }
    return DoubleArray.ofUnsafe(strike);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SmileDeltaParameters}.
   * @return the meta-bean, not null
   */
  public static SmileDeltaParameters.Meta meta() {
    return SmileDeltaParameters.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SmileDeltaParameters.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private SmileDeltaParameters(
      double expiry,
      DoubleArray delta,
      DoubleArray volatility) {
    this.expiry = expiry;
    this.delta = delta;
    this.volatility = volatility;
    validate();
  }

  @Override
  public SmileDeltaParameters.Meta metaBean() {
    return SmileDeltaParameters.Meta.INSTANCE;
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
   * Gets the time to expiry associated with the data.
   * @return the value of the property
   */
  public double getExpiry() {
    return expiry;
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
      return JodaBeanUtils.equal(expiry, other.expiry) &&
          JodaBeanUtils.equal(delta, other.delta) &&
          JodaBeanUtils.equal(volatility, other.volatility);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(expiry);
    hash = hash * 31 + JodaBeanUtils.hashCode(delta);
    hash = hash * 31 + JodaBeanUtils.hashCode(volatility);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("SmileDeltaParameters{");
    buf.append("expiry").append('=').append(expiry).append(',').append(' ');
    buf.append("delta").append('=').append(delta).append(',').append(' ');
    buf.append("volatility").append('=').append(JodaBeanUtils.toString(volatility));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SmileDeltaParameters}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code expiry} property.
     */
    private final MetaProperty<Double> expiry = DirectMetaProperty.ofImmutable(
        this, "expiry", SmileDeltaParameters.class, Double.TYPE);
    /**
     * The meta-property for the {@code delta} property.
     */
    private final MetaProperty<DoubleArray> delta = DirectMetaProperty.ofImmutable(
        this, "delta", SmileDeltaParameters.class, DoubleArray.class);
    /**
     * The meta-property for the {@code volatility} property.
     */
    private final MetaProperty<DoubleArray> volatility = DirectMetaProperty.ofImmutable(
        this, "volatility", SmileDeltaParameters.class, DoubleArray.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "expiry",
        "delta",
        "volatility");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1289159373:  // expiry
          return expiry;
        case 95468472:  // delta
          return delta;
        case -1917967323:  // volatility
          return volatility;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends SmileDeltaParameters> builder() {
      return new SmileDeltaParameters.Builder();
    }

    @Override
    public Class<? extends SmileDeltaParameters> beanType() {
      return SmileDeltaParameters.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code expiry} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> expiry() {
      return expiry;
    }

    /**
     * The meta-property for the {@code delta} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleArray> delta() {
      return delta;
    }

    /**
     * The meta-property for the {@code volatility} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleArray> volatility() {
      return volatility;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1289159373:  // expiry
          return ((SmileDeltaParameters) bean).getExpiry();
        case 95468472:  // delta
          return ((SmileDeltaParameters) bean).getDelta();
        case -1917967323:  // volatility
          return ((SmileDeltaParameters) bean).getVolatility();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code SmileDeltaParameters}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<SmileDeltaParameters> {

    private double expiry;
    private DoubleArray delta;
    private DoubleArray volatility;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1289159373:  // expiry
          return expiry;
        case 95468472:  // delta
          return delta;
        case -1917967323:  // volatility
          return volatility;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1289159373:  // expiry
          this.expiry = (Double) newValue;
          break;
        case 95468472:  // delta
          this.delta = (DoubleArray) newValue;
          break;
        case -1917967323:  // volatility
          this.volatility = (DoubleArray) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public SmileDeltaParameters build() {
      return new SmileDeltaParameters(
          expiry,
          delta,
          volatility);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("SmileDeltaParameters.Builder{");
      buf.append("expiry").append('=').append(JodaBeanUtils.toString(expiry)).append(',').append(' ');
      buf.append("delta").append('=').append(JodaBeanUtils.toString(delta)).append(',').append(' ');
      buf.append("volatility").append('=').append(JodaBeanUtils.toString(volatility));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
