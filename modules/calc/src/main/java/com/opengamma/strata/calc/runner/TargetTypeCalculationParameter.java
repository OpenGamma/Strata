/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;

/**
 * A calculation parameter that selects the parameter based on the type of the target.
 * <p>
 * This can be used where a {@link CalculationParameter} is required, and will
 * select an underlying parameter based on the target type.
 */
@BeanDefinition(style = "light")
public final class TargetTypeCalculationParameter
    implements CalculationParameter, ImmutableBean, Serializable {

  /**
   * The parameter query type.
   */
  @PropertyDefinition(validate = "notNull")
  private final Class<? extends CalculationParameter> queryType;
  /**
   * The underlying parameters, keyed by target type.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Class<?>, CalculationParameter> parameters;
  /**
   * The default underlying parameter.
   */
  @PropertyDefinition(validate = "notNull")
  private final CalculationParameter defaultParameter;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified parameters.
   * <p>
   * The map provides a lookup from the {@link CalculationTarget} implementation type
   * to the appropriate parameter to use for that target. If a target is requested that
   * is not in the map, the default parameter is used.
   * 
   * @param parameters  the parameters, keyed by target type
   * @param defaultParameter  the default parameter
   * @return the target aware parameter
   */
  public static <T extends CalculationParameter> TargetTypeCalculationParameter of(
      Map<Class<?>, CalculationParameter> parameters,
      CalculationParameter defaultParameter) {

    ArgChecker.notEmpty(parameters, "values");
    ArgChecker.notNull(defaultParameter, "defaultValue");
    Class<? extends CalculationParameter> queryType = defaultParameter.queryType();
    for (CalculationParameter value : parameters.values()) {
      if (value.queryType() != queryType) {
        throw new IllegalArgumentException(Messages.format(
            "Map contained a parameter '{}' that did not match the expected query type '{}'",
            value,
            queryType.getClass().getSimpleName()));
      }
    }
    return new TargetTypeCalculationParameter(queryType, ImmutableMap.copyOf(parameters), defaultParameter);
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<? extends CalculationParameter> queryType() {
    return queryType;
  }

  @Override
  public Optional<CalculationParameter> filter(CalculationTarget target, Measure measure) {
    CalculationParameter value = parameters.getOrDefault(target.getClass(), defaultParameter);
    return value.filter(target, measure);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code TargetTypeCalculationParameter}.
   */
  private static MetaBean META_BEAN = LightMetaBean.of(TargetTypeCalculationParameter.class);

  /**
   * The meta-bean for {@code TargetTypeCalculationParameter}.
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

  private TargetTypeCalculationParameter(
      Class<? extends CalculationParameter> queryType,
      Map<Class<?>, CalculationParameter> parameters,
      CalculationParameter defaultParameter) {
    JodaBeanUtils.notNull(queryType, "queryType");
    JodaBeanUtils.notNull(parameters, "parameters");
    JodaBeanUtils.notNull(defaultParameter, "defaultParameter");
    this.queryType = queryType;
    this.parameters = ImmutableMap.copyOf(parameters);
    this.defaultParameter = defaultParameter;
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
   * Gets the parameter query type.
   * @return the value of the property, not null
   */
  public Class<? extends CalculationParameter> getQueryType() {
    return queryType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying parameters, keyed by target type.
   * @return the value of the property, not null
   */
  public ImmutableMap<Class<?>, CalculationParameter> getParameters() {
    return parameters;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the default underlying parameter.
   * @return the value of the property, not null
   */
  public CalculationParameter getDefaultParameter() {
    return defaultParameter;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      TargetTypeCalculationParameter other = (TargetTypeCalculationParameter) obj;
      return JodaBeanUtils.equal(queryType, other.queryType) &&
          JodaBeanUtils.equal(parameters, other.parameters) &&
          JodaBeanUtils.equal(defaultParameter, other.defaultParameter);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(queryType);
    hash = hash * 31 + JodaBeanUtils.hashCode(parameters);
    hash = hash * 31 + JodaBeanUtils.hashCode(defaultParameter);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("TargetTypeCalculationParameter{");
    buf.append("queryType").append('=').append(queryType).append(',').append(' ');
    buf.append("parameters").append('=').append(parameters).append(',').append(' ');
    buf.append("defaultParameter").append('=').append(JodaBeanUtils.toString(defaultParameter));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
