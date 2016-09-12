/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

/**
 * An adjustment to a value, describing how to change one value into another.
 * <p>
 * A base value, represented as a {@code double}, can be transformed into another value
 * by specifying the result (absolute) or the calculation (relative).
 * <p>
 * <table class="border 1px solid black;border-collapse:collapse">
 * <tr>
 * <th>Type</th><th>baseValue</th><th>modifyingValue</th><th>Calculation</th>
 * </tr><tr>
 * <td>Replace</td><td>200</td><td>220</td><td>{@code result = modifyingValue = 220}</td>
 * </tr><tr>
 * <td>DeltaAmount</td><td>200</td><td>20</td><td>{@code result = baseValue + modifyingValue = (200 + 20) = 220}</td>
 * </tr><tr>
 * <td>DeltaMultiplier</td><td>200</td><td>0.1</td>
 * <td>{@code result = baseValue + baseValue * modifyingValue = (200 + 200 * 0.1) = 220}</td>
 * </tr><tr>
 * <td>Multiplier</td><td>200</td><td>1.1</td><td>{@code result = baseValue * modifyingValue = (200 * 1.1) = 220}</td>
 * </tr>
 * </table>
 */
@BeanDefinition(builderScope = "private")
public final class ValueAdjustment
    implements ImmutableBean, Serializable {

  /**
   * An instance that makes no adjustment to the value.
   */
  public static final ValueAdjustment NONE = ValueAdjustment.ofDeltaAmount(0);

  /**
   * The value used to modify the base value.
   * This value is given meaning by the associated type.
   */
  @PropertyDefinition
  private final double modifyingValue;
  /**
   * The type of adjustment to make.
   */
  @PropertyDefinition(validate = "notNull")
  private final ValueAdjustmentType type;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that replaces the base value.
   * <p>
   * The base value is ignored when calculating the result.
   * 
   * @param replacementValue  the replacement value to use as the result of the adjustment
   * @return the adjustment, capturing the replacement value
   */
  public static ValueAdjustment ofReplace(double replacementValue) {
    return new ValueAdjustment(replacementValue, ValueAdjustmentType.REPLACE);
  }

  /**
   * Obtains an instance specifying an amount to add to the base value.
   * <p>
   * The result will be {@code (baseValue + deltaAmount)}.
   * 
   * @param deltaAmount  the amount to be added to the base value
   * @return the adjustment, capturing the delta amount
   */
  public static ValueAdjustment ofDeltaAmount(double deltaAmount) {
    return new ValueAdjustment(deltaAmount, ValueAdjustmentType.DELTA_AMOUNT);
  }

  /**
   * Obtains an instance specifying a multiplication factor, adding it to the base value.
   * <p>
   * The result will be {@code (baseValue + baseValue * modifyingValue)}.
   * 
   * @param deltaMultiplier  the multiplication factor to apply to the base amount
   *  with the result added to the base amount
   * @return the adjustment, capturing the delta multiplier
   */
  public static ValueAdjustment ofDeltaMultiplier(double deltaMultiplier) {
    return new ValueAdjustment(deltaMultiplier, ValueAdjustmentType.DELTA_MULTIPLIER);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance specifying a multiplication factor to apply to the base value.
   * <p>
   * The result will be {@code (baseValue * modifyingValue)}.
   * 
   * @param multiplier  the multiplication factor to apply to the base amount
   * @return the adjustment
   */
  public static ValueAdjustment ofMultiplier(double multiplier) {
    return new ValueAdjustment(multiplier, ValueAdjustmentType.MULTIPLIER);
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("ValueAdjustment[result = ");
    switch (type) {
      case DELTA_AMOUNT:
        if (this == NONE) {
          buf.append("input");
        } else {
          buf.append("input + ").append(modifyingValue);
        }
        break;
      case DELTA_MULTIPLIER:
        buf.append("input + input * ").append(modifyingValue);
        break;
      case MULTIPLIER:
        buf.append("input * ").append(modifyingValue);
        break;
      case REPLACE:
      default:
        buf.append(modifyingValue);
        break;
    }
    buf.append(']');
    return buf.toString();
  }

  //-------------------------------------------------------------------------
  /**
   * Adjusts the base value based on the criteria of this adjustment.
   * <p>
   * For example, if this adjustment represents a 10% decrease, then the
   * result will be the base value minus 10%.
   * 
   * @param baseValue  the base, or previous, value to be adjusted
   * @return the calculated result
   */
  public double adjust(double baseValue) {
    return type.adjust(baseValue, modifyingValue);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ValueAdjustment}.
   * @return the meta-bean, not null
   */
  public static ValueAdjustment.Meta meta() {
    return ValueAdjustment.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ValueAdjustment.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private ValueAdjustment(
      double modifyingValue,
      ValueAdjustmentType type) {
    JodaBeanUtils.notNull(type, "type");
    this.modifyingValue = modifyingValue;
    this.type = type;
  }

  @Override
  public ValueAdjustment.Meta metaBean() {
    return ValueAdjustment.Meta.INSTANCE;
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
   * Gets the value used to modify the base value.
   * This value is given meaning by the associated type.
   * @return the value of the property
   */
  public double getModifyingValue() {
    return modifyingValue;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the type of adjustment to make.
   * @return the value of the property, not null
   */
  public ValueAdjustmentType getType() {
    return type;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ValueAdjustment other = (ValueAdjustment) obj;
      return JodaBeanUtils.equal(modifyingValue, other.modifyingValue) &&
          JodaBeanUtils.equal(type, other.type);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(modifyingValue);
    hash = hash * 31 + JodaBeanUtils.hashCode(type);
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ValueAdjustment}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code modifyingValue} property.
     */
    private final MetaProperty<Double> modifyingValue = DirectMetaProperty.ofImmutable(
        this, "modifyingValue", ValueAdjustment.class, Double.TYPE);
    /**
     * The meta-property for the {@code type} property.
     */
    private final MetaProperty<ValueAdjustmentType> type = DirectMetaProperty.ofImmutable(
        this, "type", ValueAdjustment.class, ValueAdjustmentType.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "modifyingValue",
        "type");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 503432553:  // modifyingValue
          return modifyingValue;
        case 3575610:  // type
          return type;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ValueAdjustment> builder() {
      return new ValueAdjustment.Builder();
    }

    @Override
    public Class<? extends ValueAdjustment> beanType() {
      return ValueAdjustment.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code modifyingValue} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> modifyingValue() {
      return modifyingValue;
    }

    /**
     * The meta-property for the {@code type} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ValueAdjustmentType> type() {
      return type;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 503432553:  // modifyingValue
          return ((ValueAdjustment) bean).getModifyingValue();
        case 3575610:  // type
          return ((ValueAdjustment) bean).getType();
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
   * The bean-builder for {@code ValueAdjustment}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<ValueAdjustment> {

    private double modifyingValue;
    private ValueAdjustmentType type;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 503432553:  // modifyingValue
          return modifyingValue;
        case 3575610:  // type
          return type;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 503432553:  // modifyingValue
          this.modifyingValue = (Double) newValue;
          break;
        case 3575610:  // type
          this.type = (ValueAdjustmentType) newValue;
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
    public ValueAdjustment build() {
      return new ValueAdjustment(
          modifyingValue,
          type);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("ValueAdjustment.Builder{");
      buf.append("modifyingValue").append('=').append(JodaBeanUtils.toString(modifyingValue)).append(',').append(' ');
      buf.append("type").append('=').append(JodaBeanUtils.toString(type));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
