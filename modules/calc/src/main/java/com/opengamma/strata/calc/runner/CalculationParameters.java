/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.ReportingCurrency;

/**
 * The calculation parameters.
 * <p>
 * This provides a set of parameters that will be used in a calculation.
 * Each parameter defines a {@linkplain CalculationParameter#queryType() query type},
 * thus the functions are keyed in a {@code Map} by the query type {@code Class}.
 * <p>
 * Parameters exist to provide control over the calculation.
 * For example, {@link ReportingCurrency} is a parameter that controls currency conversion.
 * If specified, on a {@link Column}, or in {@link CalculationRules}, then the output will
 * be converted to the specified currency.
 */
@BeanDefinition(style = "light")
public final class CalculationParameters implements ImmutableBean, Serializable {

  /**
   * An empty instance.
   */
  private static final CalculationParameters EMPTY = new CalculationParameters(ImmutableMap.of());

  /**
   * The parameters, keyed by query type.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Class<? extends CalculationParameter>, CalculationParameter> parameters;

  //-------------------------------------------------------------------------
  /**
   * Obtains an empty instance with no parameters.
   * 
   * @return the empty instance
   */
  public static CalculationParameters empty() {
    return EMPTY;
  }

  /**
   * Obtains an instance from the specified parameters.
   * <p>
   * The list will be converted to a {@code Map} using {@link CalculationParameter#queryType()}.
   * Each parameter must refer to a different query type.
   * 
   * @param parameters  the parameters
   * @return the calculation parameters
   * @throws IllegalArgumentException if two parameters have same query type
   */
  public static CalculationParameters of(CalculationParameter... parameters) {
    if (parameters.length == 0) {
      return EMPTY;
    }
    return new CalculationParameters(Stream.of(parameters).collect(toImmutableMap(p -> p.queryType())));
  }

  /**
   * Obtains an instance from the specified parameters.
   * <p>
   * The list will be converted to a {@code Map} using {@link CalculationParameter#queryType()}.
   * Each parameter must refer to a different query type.
   * 
   * @param parameters  the parameters
   * @return the calculation parameters
   * @throws IllegalArgumentException if two parameters have same query type
   */
  public static CalculationParameters of(List<? extends CalculationParameter> parameters) {
    if (parameters.isEmpty()) {
      return EMPTY;
    }
    return new CalculationParameters(parameters.stream().collect(toImmutableMap(p -> p.queryType())));
  }

  // create checking for empty
  private CalculationParameters of(Map<Class<? extends CalculationParameter>, CalculationParameter> map) {
    if (map.isEmpty()) {
      return EMPTY;
    }
    return new CalculationParameters(map);
  }

  //-------------------------------------------------------------------------
  /**
   * Combines this set of parameters with the specified set.
   * <p>
   * This set of parameters takes priority.
   * 
   * @param other  the other parameters
   * @return the combined calculation parameters
   */
  public CalculationParameters combinedWith(CalculationParameters other) {
    if (other.parameters.isEmpty()) {
      return this;
    }
    if (parameters.isEmpty()) {
      return other;
    }
    Map<Class<? extends CalculationParameter>, CalculationParameter> map = new HashMap<>(other.getParameters());
    map.putAll(parameters);
    return of(map);
  }

  /**
   * Returns a copy of this instance with the specified parameter added.
   * <p>
   * If this instance already has a parameter with the query type, it will be replaced.
   * 
   * @param parameter  the parameter to add
   * @return the new instance based on this with the parameter added
   */
  public CalculationParameters with(CalculationParameter parameter) {
    Map<Class<? extends CalculationParameter>, CalculationParameter> map = new HashMap<>(parameters);
    map.put(parameter.queryType(), parameter);
    return of(map);
  }

  /**
   * Filters the parameters, returning a set without the specified type.
   * 
   * @param type  the type to remove
   * @return the filtered calculation parameters
   */
  public CalculationParameters without(Class<? extends CalculationParameter> type) {
    if (!parameters.containsKey(type)) {
      return this;
    }
    Map<Class<? extends CalculationParameter>, CalculationParameter> map = new HashMap<>(parameters);
    map.remove(type);
    return of(map);
  }

  /**
   * Filters the parameters, matching only those that are applicable for the target and measure.
   * <p>
   * The resulting parameters are filtered to the target and measure.
   * The implementation of each parameter may be changed by this process.
   * If two parameters are filtered to the same {@linkplain CalculationParameter#queryType() query type}
   * then an exception will be thrown
   * 
   * @param target  the calculation target, such as a trade
   * @param measure  the measure to be calculated
   * @return the filtered calculation parameters
   * @throws IllegalArgumentException if two parameters are filtered to the same query type
   */
  public CalculationParameters filter(CalculationTarget target, Measure measure) {
    ImmutableList<CalculationParameter> filtered = parameters.values().stream()
        .map(cp -> cp.filter(target, measure))
        .filter(opt -> opt.isPresent())
        .map(opt -> opt.get())
        .collect(toImmutableList());
    return of(filtered);
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the parameter that matches the specified query type.
   * 
   * @param <T>  the type of the parameter
   * @param type  the query type to find
   * @return the parameter
   */
  @SuppressWarnings("unchecked")
  public <T extends CalculationParameter> Optional<T> findParameter(Class<T> type) {
    return Optional.ofNullable((T) parameters.get(type));
  }

  /**
   * Returns the parameter that matches the specified query type throwing an exception if not available.
   *
   * @param <T>  the type of the parameter
   * @param type  the query type to return
   * @return the parameter
   * @throws IllegalArgumentException if no parameter if found for the type
   */
  @SuppressWarnings("unchecked")
  public <T extends CalculationParameter> T getParameter(Class<T> type) {
    T calculationParameter = (T) parameters.get(type);

    if (calculationParameter == null) {
      throw new IllegalArgumentException("No parameter found for query type " + type.getName());
    }
    return calculationParameter;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CalculationParameters}.
   */
  private static final MetaBean META_BEAN = LightMetaBean.of(CalculationParameters.class);

  /**
   * The meta-bean for {@code CalculationParameters}.
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

  private CalculationParameters(
      Map<Class<? extends CalculationParameter>, CalculationParameter> parameters) {
    JodaBeanUtils.notNull(parameters, "parameters");
    this.parameters = ImmutableMap.copyOf(parameters);
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
   * Gets the parameters, keyed by query type.
   * @return the value of the property, not null
   */
  public ImmutableMap<Class<? extends CalculationParameter>, CalculationParameter> getParameters() {
    return parameters;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CalculationParameters other = (CalculationParameters) obj;
      return JodaBeanUtils.equal(parameters, other.parameters);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(parameters);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("CalculationParameters{");
    buf.append("parameters").append('=').append(JodaBeanUtils.toString(parameters));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
