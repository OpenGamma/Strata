/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config.pricing;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.config.FunctionConfig;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A rule which specifies the function group and parameters that should be used to calculate the value
 * of a measure for a target.
 * <p>
 * A rule matches a calculation if
 * <ul>
 *   <li>The calculation target is an instance of the target type</li>
 *   <li>The set of measures in the rule contains the measure, or the rule has an empty set of measures,
 *   meaning it handles all measures</li>
 *   <li>The function group has a function to calculate the measure for the target</li>
 * </ul>
 * 
 * @param <T>  the type of the calculation target
 */
@BeanDefinition(builderScope = "private", constructorScope = "package")
public final class PricingRule<T extends CalculationTarget>
    implements ImmutableBean {

  // Fields that define whether the rule applies to a calculation for a target and measure -----------------------------

  /** The type of target this rule applies to. */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final Class<T> targetType;

  /** The measures this rule applies to. An empty set means the rule applies to all measures. */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final ImmutableSet<Measure> measures;

  // TODO Expression to validate against arbitrary properties of the target

  // Fields that define the behaviour when the rule matches ------------------------------------------------------------

  /** The the function group with configuration parameters used for calculations that satisfy this rule. */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final FunctionGroup<T> functionGroup;

  /**
   * The arguments used by the function group when creating functions. These arguments are specified by
   * the pricing rule.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final ImmutableMap<String, Object> arguments;

  /** The function group and arguments bundled up together. */
  private final ConfiguredFunctionGroup configuredFunctionGroup;

  /**
   * Returns a builder for building pricing rules.
   *
   * @param targetType  the type of the calculation target
   * @param <T>  the type of the calculation target
   * @return a builder for building pricing rules
   */
  public static <T extends CalculationTarget> PricingRuleBuilder<T> builder(Class<T> targetType) {
    return new PricingRuleBuilder<>(targetType);
  }

  // package-private constructor used by PricingRuleBuilder
  @ImmutableConstructor
  PricingRule(Class<T> targetType, Set<Measure> measures, FunctionGroup<T> functionGroup, Map<String, Object> arguments) {
    this.targetType = ArgChecker.notNull(targetType, "targetType");
    this.measures = ImmutableSet.copyOf(measures);
    this.functionGroup = ArgChecker.notNull(functionGroup, "functionGroup");
    this.arguments = ImmutableMap.copyOf(arguments);
    this.configuredFunctionGroup = ConfiguredFunctionGroup.of(this.functionGroup, this.arguments);
  }

  /**
   * Returns a function group to calculate a value of the measure for the target if this rule applies to the target.
   *
   * @param target  a target
   * @param measure  a measure
   * @return a function group to calculate a value of the measure for the target if this rule applies to the target
   */
  public Optional<ConfiguredFunctionGroup> functionGroup(CalculationTarget target, Measure measure) {
    Optional<FunctionConfig<T>> functionConfig = functionGroup.functionConfig(target, measure);

    return targetType.isInstance(target) && handlesMeasure(measure) && functionConfig.isPresent() ?
        Optional.of(configuredFunctionGroup) :
        Optional.empty();
  }

  /**
   * Returns the set of measures configured for a calculation target.
   * 
   * @param target  a target
   * @return the set of measures configured by this rule
   */
  public ImmutableSet<Measure> configuredMeasures(CalculationTarget target) {
    if (!targetType.isInstance(target)) {
      return ImmutableSet.of();
    }
    Set<Measure> measuresConfigured = functionGroup.configuredMeasures(target);
    if (!measures.isEmpty()) {
      measuresConfigured = Sets.intersection(measures, functionGroup.configuredMeasures(target));
    }
    return ImmutableSet.copyOf(measuresConfigured);
  }

  /**
   * Returns true if this rule handles the measure.
   * <p>
   * A rule handles a measure if {@link #measures} is empty or if it contains the measure.
   *
   * @param measure  a measure
   * @return true if this rule handles the measure
   */
  private boolean handlesMeasure(Measure measure) {
    return measures.isEmpty() || measures.contains(measure);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code PricingRule}.
   * @return the meta-bean, not null
   */
  @SuppressWarnings("rawtypes")
  public static PricingRule.Meta meta() {
    return PricingRule.Meta.INSTANCE;
  }

  /**
   * The meta-bean for {@code PricingRule}.
   * @param <R>  the bean's generic type
   * @param cls  the bean's generic type
   * @return the meta-bean, not null
   */
  @SuppressWarnings("unchecked")
  public static <R extends CalculationTarget> PricingRule.Meta<R> metaPricingRule(Class<R> cls) {
    return PricingRule.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(PricingRule.Meta.INSTANCE);
  }

  @SuppressWarnings("unchecked")
  @Override
  public PricingRule.Meta<T> metaBean() {
    return PricingRule.Meta.INSTANCE;
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
   * Gets the type of target this rule applies to.
   * @return the value of the property, not null
   */
  private Class<T> getTargetType() {
    return targetType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the measures this rule applies to. An empty set means the rule applies to all measures.
   * @return the value of the property, not null
   */
  private ImmutableSet<Measure> getMeasures() {
    return measures;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the the function group with configuration parameters used for calculations that satisfy this rule.
   * @return the value of the property, not null
   */
  private FunctionGroup<T> getFunctionGroup() {
    return functionGroup;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the arguments used by the function group when creating functions. These arguments are specified by
   * the pricing rule.
   * @return the value of the property, not null
   */
  private ImmutableMap<String, Object> getArguments() {
    return arguments;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      PricingRule<?> other = (PricingRule<?>) obj;
      return JodaBeanUtils.equal(targetType, other.targetType) &&
          JodaBeanUtils.equal(measures, other.measures) &&
          JodaBeanUtils.equal(functionGroup, other.functionGroup) &&
          JodaBeanUtils.equal(arguments, other.arguments);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(targetType);
    hash = hash * 31 + JodaBeanUtils.hashCode(measures);
    hash = hash * 31 + JodaBeanUtils.hashCode(functionGroup);
    hash = hash * 31 + JodaBeanUtils.hashCode(arguments);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("PricingRule{");
    buf.append("targetType").append('=').append(targetType).append(',').append(' ');
    buf.append("measures").append('=').append(measures).append(',').append(' ');
    buf.append("functionGroup").append('=').append(functionGroup).append(',').append(' ');
    buf.append("arguments").append('=').append(JodaBeanUtils.toString(arguments));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code PricingRule}.
   * @param <T>  the type
   */
  public static final class Meta<T extends CalculationTarget> extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    @SuppressWarnings("rawtypes")
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code targetType} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<Class<T>> targetType = DirectMetaProperty.ofImmutable(
        this, "targetType", PricingRule.class, (Class) Class.class);
    /**
     * The meta-property for the {@code measures} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableSet<Measure>> measures = DirectMetaProperty.ofImmutable(
        this, "measures", PricingRule.class, (Class) ImmutableSet.class);
    /**
     * The meta-property for the {@code functionGroup} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<FunctionGroup<T>> functionGroup = DirectMetaProperty.ofImmutable(
        this, "functionGroup", PricingRule.class, (Class) FunctionGroup.class);
    /**
     * The meta-property for the {@code arguments} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<String, Object>> arguments = DirectMetaProperty.ofImmutable(
        this, "arguments", PricingRule.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "targetType",
        "measures",
        "functionGroup",
        "arguments");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 486622315:  // targetType
          return targetType;
        case -976812331:  // measures
          return measures;
        case 2031434119:  // functionGroup
          return functionGroup;
        case -2035517098:  // arguments
          return arguments;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends PricingRule<T>> builder() {
      return new PricingRule.Builder<T>();
    }

    @SuppressWarnings({"unchecked", "rawtypes" })
    @Override
    public Class<? extends PricingRule<T>> beanType() {
      return (Class) PricingRule.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code targetType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Class<T>> targetType() {
      return targetType;
    }

    /**
     * The meta-property for the {@code measures} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableSet<Measure>> measures() {
      return measures;
    }

    /**
     * The meta-property for the {@code functionGroup} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FunctionGroup<T>> functionGroup() {
      return functionGroup;
    }

    /**
     * The meta-property for the {@code arguments} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<String, Object>> arguments() {
      return arguments;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 486622315:  // targetType
          return ((PricingRule<?>) bean).getTargetType();
        case -976812331:  // measures
          return ((PricingRule<?>) bean).getMeasures();
        case 2031434119:  // functionGroup
          return ((PricingRule<?>) bean).getFunctionGroup();
        case -2035517098:  // arguments
          return ((PricingRule<?>) bean).getArguments();
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
   * The bean-builder for {@code PricingRule}.
   * @param <T>  the type
   */
  private static final class Builder<T extends CalculationTarget> extends DirectFieldsBeanBuilder<PricingRule<T>> {

    private Class<T> targetType;
    private Set<Measure> measures = ImmutableSet.of();
    private FunctionGroup<T> functionGroup;
    private Map<String, Object> arguments = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 486622315:  // targetType
          return targetType;
        case -976812331:  // measures
          return measures;
        case 2031434119:  // functionGroup
          return functionGroup;
        case -2035517098:  // arguments
          return arguments;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder<T> set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 486622315:  // targetType
          this.targetType = (Class<T>) newValue;
          break;
        case -976812331:  // measures
          this.measures = (Set<Measure>) newValue;
          break;
        case 2031434119:  // functionGroup
          this.functionGroup = (FunctionGroup<T>) newValue;
          break;
        case -2035517098:  // arguments
          this.arguments = (Map<String, Object>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder<T> set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder<T> setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder<T> setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder<T> setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public PricingRule<T> build() {
      return new PricingRule<T>(
          targetType,
          measures,
          functionGroup,
          arguments);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("PricingRule.Builder{");
      buf.append("targetType").append('=').append(JodaBeanUtils.toString(targetType)).append(',').append(' ');
      buf.append("measures").append('=').append(JodaBeanUtils.toString(measures)).append(',').append(' ');
      buf.append("functionGroup").append('=').append(JodaBeanUtils.toString(functionGroup)).append(',').append(' ');
      buf.append("arguments").append('=').append(JodaBeanUtils.toString(arguments));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
