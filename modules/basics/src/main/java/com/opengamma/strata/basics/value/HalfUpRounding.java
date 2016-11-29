/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.NoSuchElementException;
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

import com.opengamma.strata.collect.ArgChecker;

/**
 * Standard implementation of {@code Rounding} that uses the half-up convention.
 * <p>
 * This class implements {@link Rounding} to provide the ability to round a number.
 * Rounding follows the normal {@link RoundingMode#HALF_UP} convention.
 * For example, this could be used to round a price to the appropriate market convention.
 * <p>
 * Note that rounding a {@code double} is not straightforward as floating point
 * numbers are based on a binary representation, not a decimal one.
 * For example, the value 0.1 cannot be exactly represented in a {@code double}.
 */
@BeanDefinition(builderScope = "private")
public final class HalfUpRounding
    implements Rounding, ImmutableBean, Serializable {

  /**
   * Cache common roundings.
   * Roundings will be commonly used in trades, which are relatively long-lived,
   * so some limited caching makes sense.
   */
  private static final HalfUpRounding[] CACHE = new HalfUpRounding[16];
  static {
    for (int i = 0; i < 16; i++) {
      CACHE[i] = new HalfUpRounding(i, 0);
    }
  }

  /**
   * The number of decimal places to round to.
   * <p>
   * Rounding follows the normal {@link RoundingMode#HALF_UP} convention.
   * <p>
   * The value must be from 0 to 255 inclusive.
   */
  @PropertyDefinition
  private final int decimalPlaces;
  /**
   * The fraction of the smallest decimal place to round to.
   * <p>
   * If used, this allows the rounding point to be set as a fraction of the smallest decimal place.
   * For example, setting this field to 32 will round to the nearest 1/32nd of the last decimal place.
   * <p>
   * This will not be present if rounding is to an exact number of decimal places and there is no fraction.
   * The value must be from 2 to 256 inclusive, 0 is used to indicate no fractional part.
   */
  @PropertyDefinition
  private final int fraction;
  /**
   * The fraction, as a {@code BigDecimal}.
   * Not a Joda-Beans property.
   */
  private final transient BigDecimal fractionDecimal;
  /**
   * The hash code.
   * Uniquely identifies the state of the object.
   * Not a Joda-Beans property.
   */
  private final transient int uniqueHashCode;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that rounds to the specified number of decimal places.
   * <p>
   * This returns a convention that rounds to the specified number of decimal places.
   * Rounding follows the normal {@link RoundingMode#HALF_UP} convention.
   * 
   * @param decimalPlaces  the number of decimal places to round to, from 0 to 255 inclusive
   * @return the rounding convention
   * @throws IllegalArgumentException if the decimal places is invalid
   */
  public static HalfUpRounding ofDecimalPlaces(int decimalPlaces) {
    if (decimalPlaces >= 0 && decimalPlaces < 16) {
      return CACHE[decimalPlaces];
    }
    return new HalfUpRounding(decimalPlaces, 1);
  }

  /**
   * Obtains an instance from the number of decimal places and fraction.
   * <p>
   * This returns a convention that rounds to a fraction of the specified number of decimal places.
   * Rounding follows the normal {@link RoundingMode#HALF_UP} convention.
   * <p>
   * For example, to round to the nearest 1/32nd of the 4th decimal place, call
   * this method with the arguments 4 and 32.
   * 
   * @param decimalPlaces  the number of decimal places to round to, from 0 to 255 inclusive
   * @param fraction  the fraction of the last decimal place, such as 32 for 1/32, from 0 to 256 inclusive
   * @return the rounding convention
   * @throws IllegalArgumentException if the decimal places or fraction is invalid
   */
  public static HalfUpRounding ofFractionalDecimalPlaces(int decimalPlaces, int fraction) {
    return new HalfUpRounding(decimalPlaces, fraction);
  }

  //-------------------------------------------------------------------------
  // constructor
  @ImmutableConstructor
  private HalfUpRounding(
      int decimalPlaces,
      int fraction) {

    if (decimalPlaces < 0 || decimalPlaces > 255) {
      throw new IllegalArgumentException("Invalid decimal places, must be from 0 to 255 inclusive");
    }
    if (fraction < 0 || fraction > 256) {
      throw new IllegalArgumentException("Invalid fraction, must be from 0 to 256 inclusive");
    }
    this.decimalPlaces = ArgChecker.notNegative(decimalPlaces, "decimalPlaces");
    this.fraction = (fraction <= 1 ? 0 : fraction);
    this.fractionDecimal = (fraction <= 1 ? null : BigDecimal.valueOf(this.fraction));
    this.uniqueHashCode = (this.decimalPlaces << 16) + this.fraction;
  }

  // deserialize transient
  private Object readResolve() throws ObjectStreamException {
    return new HalfUpRounding(decimalPlaces, fraction);
  }

  //-------------------------------------------------------------------------
  @Override
  public double round(double value) {
    return Rounding.super.round(value);
  }

  @Override
  public BigDecimal round(BigDecimal value) {
    if (fraction > 1) {
      return value
          .multiply(fractionDecimal)
          .setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP)
          .divide(fractionDecimal);
    }
    return value.setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP);
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof HalfUpRounding) {
      // hash code is unique so can be used to compare
      return (uniqueHashCode == ((HalfUpRounding) obj).uniqueHashCode);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return uniqueHashCode;
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return "Round to " + (fraction > 1 ? "1/" + fraction + " of " : "") + decimalPlaces + "dp";
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code HalfUpRounding}.
   * @return the meta-bean, not null
   */
  public static HalfUpRounding.Meta meta() {
    return HalfUpRounding.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(HalfUpRounding.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public HalfUpRounding.Meta metaBean() {
    return HalfUpRounding.Meta.INSTANCE;
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
   * Gets the number of decimal places to round to.
   * <p>
   * Rounding follows the normal {@link RoundingMode#HALF_UP} convention.
   * <p>
   * The value must be from 0 to 255 inclusive.
   * @return the value of the property
   */
  public int getDecimalPlaces() {
    return decimalPlaces;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fraction of the smallest decimal place to round to.
   * <p>
   * If used, this allows the rounding point to be set as a fraction of the smallest decimal place.
   * For example, setting this field to 32 will round to the nearest 1/32nd of the last decimal place.
   * <p>
   * This will not be present if rounding is to an exact number of decimal places and there is no fraction.
   * The value must be from 2 to 256 inclusive, 0 is used to indicate no fractional part.
   * @return the value of the property
   */
  public int getFraction() {
    return fraction;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code HalfUpRounding}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code decimalPlaces} property.
     */
    private final MetaProperty<Integer> decimalPlaces = DirectMetaProperty.ofImmutable(
        this, "decimalPlaces", HalfUpRounding.class, Integer.TYPE);
    /**
     * The meta-property for the {@code fraction} property.
     */
    private final MetaProperty<Integer> fraction = DirectMetaProperty.ofImmutable(
        this, "fraction", HalfUpRounding.class, Integer.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "decimalPlaces",
        "fraction");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1477363453:  // decimalPlaces
          return decimalPlaces;
        case -1653751294:  // fraction
          return fraction;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends HalfUpRounding> builder() {
      return new HalfUpRounding.Builder();
    }

    @Override
    public Class<? extends HalfUpRounding> beanType() {
      return HalfUpRounding.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code decimalPlaces} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> decimalPlaces() {
      return decimalPlaces;
    }

    /**
     * The meta-property for the {@code fraction} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> fraction() {
      return fraction;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1477363453:  // decimalPlaces
          return ((HalfUpRounding) bean).getDecimalPlaces();
        case -1653751294:  // fraction
          return ((HalfUpRounding) bean).getFraction();
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
   * The bean-builder for {@code HalfUpRounding}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<HalfUpRounding> {

    private int decimalPlaces;
    private int fraction;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1477363453:  // decimalPlaces
          return decimalPlaces;
        case -1653751294:  // fraction
          return fraction;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1477363453:  // decimalPlaces
          this.decimalPlaces = (Integer) newValue;
          break;
        case -1653751294:  // fraction
          this.fraction = (Integer) newValue;
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
    public HalfUpRounding build() {
      return new HalfUpRounding(
          decimalPlaces,
          fraction);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("HalfUpRounding.Builder{");
      buf.append("decimalPlaces").append('=').append(JodaBeanUtils.toString(decimalPlaces)).append(',').append(' ');
      buf.append("fraction").append('=').append(JodaBeanUtils.toString(fraction));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
