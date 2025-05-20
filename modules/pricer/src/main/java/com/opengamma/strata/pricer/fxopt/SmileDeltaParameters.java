/*
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutablePreBuild;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.option.DeltaStrike;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.ParameterizedData;
import com.opengamma.strata.market.param.TenoredParameterMetadata;
import com.opengamma.strata.pricer.common.GenericVolatilitySurfaceYearFractionParameterMetadata;
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
   * The array is typically {@code [0.1, 0.25]}. The at-the-money value of 0.5 is not included.
   */
  @PropertyDefinition
  private final DoubleArray delta;
  /**
   * The volatilities associated with the strikes.
   * This will be of size {@code (delta.size() * 2) + 1} with the put with lower delta (in absolute value) first,
   * at-the-money and call with larger delta first.
   */
  @PropertyDefinition
  private final DoubleArray volatility;
  /**
   * The associated metadata.
   * This will be of size {@code (delta.size() * 2) + 1} with the put with lower delta (in absolute value) first,
   * at-the-money and call with larger delta first.
   */
  @PropertyDefinition
  private final ImmutableList<ParameterMetadata> parameterMetadata;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from volatility.
   * <p>
   * {@code GenericVolatilitySurfaceYearFractionParameterMetadata} is used for parameter metadata.
   * 
   * @param expiry  the time to expiry associated to the data
   * @param delta  the delta of the different data points, must be positive and sorted in ascending order,
   *   the put will have as delta the opposite of the numbers
   * @param volatility  the volatilities
   * @return the smile definition
   */
  public static SmileDeltaParameters of(double expiry, DoubleArray delta, DoubleArray volatility) {
    return of(expiry, delta, volatility, createParameterMetadata(expiry, null, delta));
  }

  /**
   * Obtains an instance from volatility.
   * <p>
   * {@code GenericVolatilitySurfaceYearFractionParameterMetadata} is used for parameter metadata.
   * 
   * @param expiry  the time to expiry associated to the data
   * @param expiryTenor  the tenor associated with the expiry
   * @param delta  the delta of the different data points, must be positive and sorted in ascending order,
   *   the put will have as delta the opposite of the numbers
   * @param volatility  the volatilities
   * @return the smile definition
   */
  public static SmileDeltaParameters of(double expiry, Tenor expiryTenor, DoubleArray delta, DoubleArray volatility) {
    return of(expiry, delta, volatility, createParameterMetadata(expiry, expiryTenor, delta));
  }

  /**
   * Obtains an instance from volatility.
   * 
   * @param expiry  the time to expiry associated to the data
   * @param delta  the delta of the different data points, must be positive and sorted in ascending order,
   *   the put will have as delta the opposite of the numbers
   * @param volatility  the volatilities
   * @param parameterMetadata  the parameter metadata
   * @return the smile definition
   */
  public static SmileDeltaParameters of(
      double expiry,
      DoubleArray delta,
      DoubleArray volatility,
      List<ParameterMetadata> parameterMetadata) {

    ArgChecker.notNull(delta, "delta");
    ArgChecker.notNull(volatility, "volatility");
    return new SmileDeltaParameters(expiry, delta, volatility, parameterMetadata);
  }

  /**
   * Obtains an instance from market data at-the-money, delta, risk-reversal and strangle.
   * <p>
   * {@code GenericVolatilitySurfaceYearFractionParameterMetadata} is used for parameter metadata.
   * 
   * @param expiry  the time to expiry associated to the data
   * @param atmVolatility  the at-the-money volatility
   * @param delta  the delta of the different data points, must be positive and sorted in ascending order,
   *   the put will have as delta the opposite of the numbers
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

    return of(expiry, atmVolatility, delta, riskReversal, strangle, createParameterMetadata(expiry, null, delta));
  }

  /**
   * Obtains an instance from market data at-the-money, delta, risk-reversal and strangle.
   * <p>
   * This factory allows the tenor to be included in the parameter metadata.
   * 
   * @param expiry  the time to expiry associated to the data
   * @param expiryTenor  the tenor associated with the expiry
   * @param atmVolatility  the at-the-money volatility
   * @param delta  the delta of the different data points, must be positive and sorted in ascending order,
   *   the put will have as delta the opposite of the numbers
   * @param riskReversal  the risk reversal volatility figures, in the same order as the delta
   * @param strangle  the strangle volatility figures, in the same order as the delta
   * @return the smile definition
   */
  public static SmileDeltaParameters of(
      double expiry,
      Tenor expiryTenor,
      double atmVolatility,
      DoubleArray delta,
      DoubleArray riskReversal,
      DoubleArray strangle) {

    return of(expiry, atmVolatility, delta, riskReversal, strangle, createParameterMetadata(expiry, expiryTenor, delta));
  }

  /**
   * Obtains an instance from market data at-the-money, delta, risk-reversal and strangle.
   * 
   * @param expiry  the time to expiry associated to the data
   * @param atmVolatility  the at-the-money volatility
   * @param delta  the delta of the different data points, must be positive and sorted in ascending order,
   *   the put will have as delta the opposite of the numbers
   * @param riskReversal  the risk reversal volatility figures, in the same order as the delta
   * @param strangle  the strangle volatility figures, in the same order as the delta
   * @param parameterMetadata  the parameter metadata
   * @return the smile definition
   */
  public static SmileDeltaParameters of(
      double expiry,
      double atmVolatility,
      DoubleArray delta,
      DoubleArray riskReversal,
      DoubleArray strangle,
      List<ParameterMetadata> parameterMetadata) {

    ArgChecker.notNull(delta, "delta");
    ArgChecker.notNull(riskReversal, "riskReversal");
    ArgChecker.notNull(strangle, "strangle");
    int nbDelta = delta.size();
    ArgChecker.isTrue(nbDelta == riskReversal.size(),
        "Length of delta {} should be equal to length of riskReversal {}", delta.size(), riskReversal.size());
    ArgChecker.isTrue(nbDelta == strangle.size(),
        "Length of delta {} should be equal to length of strangle {} ", delta.size(), strangle.size());

    double[] volatility = new double[2 * nbDelta + 1];
    volatility[nbDelta] = atmVolatility;
    for (int i = 0; i < nbDelta; i++) {
      volatility[i] = strangle.get(i) + atmVolatility - riskReversal.get(i) / 2.0; // Put
      volatility[2 * nbDelta - i] = strangle.get(i) + atmVolatility + riskReversal.get(i) / 2.0; // Call
    }
    return of(expiry, delta, DoubleArray.ofUnsafe(volatility), parameterMetadata);
  }

  //-------------------------------------------------------------------------
  // creates the metadata, tenor may be null
  private static ImmutableList<ParameterMetadata> createParameterMetadata(
      double expiry,
      Tenor expiryTenor,
      DoubleArray delta) {

    ArgChecker.notNull(delta, "delta");
    int nbDelta = delta.size();
    ParameterMetadata[] paramMetadata = new ParameterMetadata[2 * nbDelta + 1];
    // at the money, 0.5
    DeltaStrike strikeAtm = DeltaStrike.of(0.5d);
    paramMetadata[nbDelta] = GenericVolatilitySurfaceYearFractionParameterMetadata.of(expiry, expiryTenor, strikeAtm);
    for (int i = 0; i < nbDelta; i++) {
      // put, such as 0.1 and 0.25
      DeltaStrike strikePut = DeltaStrike.of(1d - delta.get(i));
      paramMetadata[i] = GenericVolatilitySurfaceYearFractionParameterMetadata.of(expiry, expiryTenor, strikePut);
      // call, such as 0.75 and 0.9
      DeltaStrike strikeCall = DeltaStrike.of(delta.get(i));
      paramMetadata[2 * nbDelta - i] = GenericVolatilitySurfaceYearFractionParameterMetadata.of(expiry, expiryTenor, strikeCall);
    }
    return ImmutableList.copyOf(paramMetadata);
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.parameterMetadata == null) {
      if (builder.delta != null) {
        builder.parameterMetadata = createParameterMetadata(builder.expiry, null, builder.delta);
      }
    }
  }

  @ImmutableValidator
  private void validate() {
    int nbDelta = delta.size();
    ArgChecker.isTrue(2 * nbDelta + 1 == volatility.size(),
        "Length of delta {} should be coherent with volatility length {}", 2 * delta.size() + 1, volatility.size());
    ArgChecker.isTrue(2 * nbDelta + 1 == parameterMetadata.size(),
        "Length of delta {} should be coherent with parameterMetadata length {}", 2 * delta.size() + 1, parameterMetadata.size());
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
    return parameterMetadata.get(parameterIndex);
  }

  @Override
  public SmileDeltaParameters withParameter(int parameterIndex, double newValue) {
    return new SmileDeltaParameters(expiry, delta, volatility.with(parameterIndex, newValue), parameterMetadata);
  }

  @Override
  public SmileDeltaParameters withPerturbation(ParameterPerturbation perturbation) {
    int size = volatility.size();
    DoubleArray perturbedValues = DoubleArray.of(
        size, i -> perturbation.perturbParameter(i, volatility.get(i), getParameterMetadata(i)));
    return new SmileDeltaParameters(expiry, delta, perturbedValues, parameterMetadata);
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

  //-------------------------------------------------------------------------
  /**
   * Calculates the derivatives of the implied strikes to expiry.
   *
   * @param forward  the forward
   * @return the strikes
   */
  public DoubleArray impliedStrikesDerivativeToExpiry(double forward) {
    int nbDelta = delta.size();
    double[] dStrikedTime = new double[2 * nbDelta + 1];
    double atmVol = volatility.get(nbDelta);
    dStrikedTime[nbDelta] = forward * atmVol * atmVol * Math.exp(atmVol * atmVol * expiry / 2.0) / 2.0;
    for (int loopdelta = 0; loopdelta < nbDelta; loopdelta++) {
      double[] valueDerivatives = new double[4];
      BlackFormulaRepository.impliedStrike(
          -delta.get(loopdelta), false, forward, expiry, volatility.get(loopdelta), valueDerivatives); // Put
      dStrikedTime[loopdelta] = valueDerivatives[2];
      BlackFormulaRepository.impliedStrike(
          delta.get(loopdelta), true, forward, expiry, volatility.get(2 * nbDelta - loopdelta), valueDerivatives); // Call
      dStrikedTime[2 * nbDelta - loopdelta] = valueDerivatives[2];
    }
    return DoubleArray.ofUnsafe(dStrikedTime);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the derivatives of the implied strikes to forward.
   *
   * @param forward  the forward
   * @return the derivatives of the implied strikes
   */
  public DoubleArray impliedStrikesDerivativeToForward(double forward) {
    int nbDelta = delta.size();
    double[] dStrikedForward = new double[2 * nbDelta + 1];
    double atmVol = volatility.get(nbDelta);
    dStrikedForward[nbDelta] = Math.exp(atmVol * atmVol * expiry / 2.0);
    for (int loopdelta = 0; loopdelta < nbDelta; loopdelta++) {
      double[] valueDerivatives = new double[4];
      BlackFormulaRepository.impliedStrike(
          -delta.get(loopdelta),
          false,
          forward,
          expiry,
          volatility.get(loopdelta),
          valueDerivatives); // Put
      dStrikedForward[loopdelta] = valueDerivatives[1];
      BlackFormulaRepository.impliedStrike(
          delta.get(loopdelta),
          true,
          forward,
          expiry,
          volatility.get(2 * nbDelta - loopdelta),
          valueDerivatives); // Call
      dStrikedForward[2 * nbDelta - loopdelta] = valueDerivatives[1];
    }
    return DoubleArray.ofUnsafe(dStrikedForward);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the derivatives of the implied strikes to volatility.
   *
   * @param forward  the forward
   * @return the strikes
   */
  public DoubleArray impliedStrikesDerivativeToSmileVols(double forward) {
    int nbDelta = delta.size();
    double[] dStrikedVol = new double[2 * nbDelta + 1];
    double[] valueDerivatives = new double[4];
    double atmVol = volatility.get(nbDelta);
    dStrikedVol[nbDelta] = atmVol * expiry * forward * Math.exp(atmVol * atmVol * expiry / 2.0);
    for (int loopdelta = 0; loopdelta < nbDelta; loopdelta++) {
      BlackFormulaRepository.impliedStrike(
          -delta.get(loopdelta), false, forward, expiry, volatility.get(loopdelta), valueDerivatives); // Put
      dStrikedVol[loopdelta] = valueDerivatives[3];
      BlackFormulaRepository.impliedStrike(
          delta.get(loopdelta), true, forward, expiry, volatility.get(2 * nbDelta - loopdelta), valueDerivatives); // Call
      dStrikedVol[2 * nbDelta - loopdelta] = valueDerivatives[3];
    }
    return DoubleArray.ofUnsafe(dStrikedVol);
  }

  /**
   * Gets the tenor associated with the time to expiry, optional.
   * @return the optional value of the property, not null
   */
  public Optional<Tenor> getExpiryTenor() {
    ParameterMetadata meta = parameterMetadata.get(0);
    if (meta instanceof TenoredParameterMetadata) {
      return Optional.of(((TenoredParameterMetadata) meta).getTenor());
    } else if (meta instanceof GenericVolatilitySurfaceYearFractionParameterMetadata) {
      return ((GenericVolatilitySurfaceYearFractionParameterMetadata) meta).getYearFractionTenor();
    } else {
      return Optional.empty();
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code SmileDeltaParameters}.
   * @return the meta-bean, not null
   */
  public static SmileDeltaParameters.Meta meta() {
    return SmileDeltaParameters.Meta.INSTANCE;
  }

  static {
    MetaBean.register(SmileDeltaParameters.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private SmileDeltaParameters(
      double expiry,
      DoubleArray delta,
      DoubleArray volatility,
      List<ParameterMetadata> parameterMetadata) {
    this.expiry = expiry;
    this.delta = delta;
    this.volatility = volatility;
    this.parameterMetadata = (parameterMetadata != null ? ImmutableList.copyOf(parameterMetadata) : null);
    validate();
  }

  @Override
  public SmileDeltaParameters.Meta metaBean() {
    return SmileDeltaParameters.Meta.INSTANCE;
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
   * The array is typically {@code [0.1, 0.25]}. The at-the-money value of 0.5 is not included.
   * @return the value of the property
   */
  public DoubleArray getDelta() {
    return delta;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the volatilities associated with the strikes.
   * This will be of size {@code (delta.size() * 2) + 1} with the put with lower delta (in absolute value) first,
   * at-the-money and call with larger delta first.
   * @return the value of the property
   */
  public DoubleArray getVolatility() {
    return volatility;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the associated metadata.
   * This will be of size {@code (delta.size() * 2) + 1} with the put with lower delta (in absolute value) first,
   * at-the-money and call with larger delta first.
   * @return the value of the property
   */
  public ImmutableList<ParameterMetadata> getParameterMetadata() {
    return parameterMetadata;
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
          JodaBeanUtils.equal(volatility, other.volatility) &&
          JodaBeanUtils.equal(parameterMetadata, other.parameterMetadata);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(expiry);
    hash = hash * 31 + JodaBeanUtils.hashCode(delta);
    hash = hash * 31 + JodaBeanUtils.hashCode(volatility);
    hash = hash * 31 + JodaBeanUtils.hashCode(parameterMetadata);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("SmileDeltaParameters{");
    buf.append("expiry").append('=').append(JodaBeanUtils.toString(expiry)).append(',').append(' ');
    buf.append("delta").append('=').append(JodaBeanUtils.toString(delta)).append(',').append(' ');
    buf.append("volatility").append('=').append(JodaBeanUtils.toString(volatility)).append(',').append(' ');
    buf.append("parameterMetadata").append('=').append(JodaBeanUtils.toString(parameterMetadata));
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
     * The meta-property for the {@code parameterMetadata} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<ParameterMetadata>> parameterMetadata = DirectMetaProperty.ofImmutable(
        this, "parameterMetadata", SmileDeltaParameters.class, (Class) ImmutableList.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "expiry",
        "delta",
        "volatility",
        "parameterMetadata");

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
        case -1169106440:  // parameterMetadata
          return parameterMetadata;
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

    /**
     * The meta-property for the {@code parameterMetadata} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<ParameterMetadata>> parameterMetadata() {
      return parameterMetadata;
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
        case -1169106440:  // parameterMetadata
          return ((SmileDeltaParameters) bean).getParameterMetadata();
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
  private static final class Builder extends DirectPrivateBeanBuilder<SmileDeltaParameters> {

    private double expiry;
    private DoubleArray delta;
    private DoubleArray volatility;
    private List<ParameterMetadata> parameterMetadata;

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
        case -1169106440:  // parameterMetadata
          return parameterMetadata;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
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
        case -1169106440:  // parameterMetadata
          this.parameterMetadata = (List<ParameterMetadata>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public SmileDeltaParameters build() {
      preBuild(this);
      return new SmileDeltaParameters(
          expiry,
          delta,
          volatility,
          parameterMetadata);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("SmileDeltaParameters.Builder{");
      buf.append("expiry").append('=').append(JodaBeanUtils.toString(expiry)).append(',').append(' ');
      buf.append("delta").append('=').append(JodaBeanUtils.toString(delta)).append(',').append(' ');
      buf.append("volatility").append('=').append(JodaBeanUtils.toString(volatility)).append(',').append(' ');
      buf.append("parameterMetadata").append('=').append(JodaBeanUtils.toString(parameterMetadata));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
