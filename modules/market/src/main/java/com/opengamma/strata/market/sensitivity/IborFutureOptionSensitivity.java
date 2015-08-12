/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;

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

import com.google.common.collect.ComparisonChain;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.index.IborIndex;

/**
 * Point sensitivity to an implied volatility for a Ibor future option model.
 * <p>
 * Holds the sensitivity to a specific volatility point.
 */
@BeanDefinition(builderScope = "private")
public final class IborFutureOptionSensitivity
    implements PointSensitivity, PointSensitivityBuilder, ImmutableBean, Serializable {

  /**
   * The index on which the underlying future fixes.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborIndex index;
  /**
   * The expiration date-time of the option.
   */
  @PropertyDefinition(validate = "notNull")
  private final ZonedDateTime expiration;
  /**
   * The underlying future last trading or fixing date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate fixingDate;
  /**
   * The option strike price.
   */
  @PropertyDefinition
  private final double strikePrice;
  /**
   * The underlying future price.
   */
  @PropertyDefinition
  private final double futurePrice;
  /**
   * The currency of the sensitivity.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * The value of the sensitivity.
   */
  @PropertyDefinition(overrideGet = true)
  private final double sensitivity;

  //-------------------------------------------------------------------------
  /**
   * Obtains an {@code IborFutureOptionSensitivity} from the key and value.
   * <p>
   * The currency is defaulted from the index.
   * 
   * @param index  the index of the curve
   * @param expirationDate  the expiration date of the option
   * @param fixingDate  the fixing date of the underlying future
   * @param strikePrice  the strike price of the option
   * @param futurePrice  the price of the underlying future
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static IborFutureOptionSensitivity of(
      IborIndex index,
      ZonedDateTime expirationDate,
      LocalDate fixingDate,
      double strikePrice,
      double futurePrice,
      double sensitivity) {

    return new IborFutureOptionSensitivity(
        index, expirationDate, fixingDate, strikePrice, futurePrice, index.getCurrency(), sensitivity);
  }

  /**
   * Obtains an {@code IborFutureOptionSensitivity} from the key, sensitivity currency and value.
   * 
   * @param index  the index of the curve
   * @param expirationDate  the expiration date of the option
   * @param fixingDate  the fixing date of the underlying future
   * @param strikePrice  the strike price of the option
   * @param futurePrice  the price of the underlying future
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static IborFutureOptionSensitivity of(
      IborIndex index,
      ZonedDateTime expirationDate,
      LocalDate fixingDate,
      double strikePrice,
      double futurePrice,
      Currency sensitivityCurrency,
      double sensitivity) {

    return new IborFutureOptionSensitivity(
        index, expirationDate, fixingDate, strikePrice, futurePrice, sensitivityCurrency, sensitivity);
  }

  //-------------------------------------------------------------------------
  @Override
  public IborFutureOptionSensitivity withCurrency(Currency currency) {
    if (this.currency.equals(currency)) {
      return this;
    }
    return new IborFutureOptionSensitivity(
        index, expiration, fixingDate, strikePrice, futurePrice, currency, sensitivity);
  }

  @Override
  public IborFutureOptionSensitivity withSensitivity(double sensitivity) {
    return new IborFutureOptionSensitivity(
        index, expiration, fixingDate, strikePrice, futurePrice, currency, sensitivity);
  }

  @Override
  public int compareKey(PointSensitivity other) {
    if (other instanceof IborFutureOptionSensitivity) {
      IborFutureOptionSensitivity otherOption = (IborFutureOptionSensitivity) other;
      return ComparisonChain.start()
          .compare(index.toString(), otherOption.index.toString())
          .compare(expiration, otherOption.expiration)
          .compare(fixingDate, otherOption.fixingDate)
          .compare(strikePrice, otherOption.strikePrice)
          .compare(futurePrice, otherOption.futurePrice)
          .compare(currency, otherOption.currency)
          .result();
    }
    return getClass().getSimpleName().compareTo(other.getClass().getSimpleName());
  }

  @Override
  public IborFutureOptionSensitivity convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    return (IborFutureOptionSensitivity) PointSensitivity.super.convertedTo(resultCurrency, rateProvider);
  }

  //-------------------------------------------------------------------------
  @Override
  public IborFutureOptionSensitivity multipliedBy(double factor) {
    return new IborFutureOptionSensitivity(
        index, expiration, fixingDate, strikePrice, futurePrice, currency, sensitivity * factor);
  }

  @Override
  public IborFutureOptionSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new IborFutureOptionSensitivity(
        index, expiration, fixingDate, strikePrice, futurePrice, currency, operator.applyAsDouble(sensitivity));
  }

  @Override
  public IborFutureOptionSensitivity normalize() {
    return this;
  }

  @Override
  public MutablePointSensitivities buildInto(MutablePointSensitivities combination) {
    return combination.add(this);
  }

  @Override
  public IborFutureOptionSensitivity cloned() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborFutureOptionSensitivity}.
   * @return the meta-bean, not null
   */
  public static IborFutureOptionSensitivity.Meta meta() {
    return IborFutureOptionSensitivity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IborFutureOptionSensitivity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private IborFutureOptionSensitivity(
      IborIndex index,
      ZonedDateTime expiration,
      LocalDate fixingDate,
      double strikePrice,
      double futurePrice,
      Currency currency,
      double sensitivity) {
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(expiration, "expiration");
    JodaBeanUtils.notNull(fixingDate, "fixingDate");
    JodaBeanUtils.notNull(currency, "currency");
    this.index = index;
    this.expiration = expiration;
    this.fixingDate = fixingDate;
    this.strikePrice = strikePrice;
    this.futurePrice = futurePrice;
    this.currency = currency;
    this.sensitivity = sensitivity;
  }

  @Override
  public IborFutureOptionSensitivity.Meta metaBean() {
    return IborFutureOptionSensitivity.Meta.INSTANCE;
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
   * Gets the index on which the underlying future fixes.
   * @return the value of the property, not null
   */
  public IborIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the expiration date-time of the option.
   * @return the value of the property, not null
   */
  public ZonedDateTime getExpiration() {
    return expiration;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying future last trading or fixing date.
   * @return the value of the property, not null
   */
  public LocalDate getFixingDate() {
    return fixingDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the option strike price.
   * @return the value of the property
   */
  public double getStrikePrice() {
    return strikePrice;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying future price.
   * @return the value of the property
   */
  public double getFuturePrice() {
    return futurePrice;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the sensitivity.
   * @return the value of the property, not null
   */
  @Override
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the value of the sensitivity.
   * @return the value of the property
   */
  @Override
  public double getSensitivity() {
    return sensitivity;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      IborFutureOptionSensitivity other = (IborFutureOptionSensitivity) obj;
      return JodaBeanUtils.equal(getIndex(), other.getIndex()) &&
          JodaBeanUtils.equal(getExpiration(), other.getExpiration()) &&
          JodaBeanUtils.equal(getFixingDate(), other.getFixingDate()) &&
          JodaBeanUtils.equal(getStrikePrice(), other.getStrikePrice()) &&
          JodaBeanUtils.equal(getFuturePrice(), other.getFuturePrice()) &&
          JodaBeanUtils.equal(getCurrency(), other.getCurrency()) &&
          JodaBeanUtils.equal(getSensitivity(), other.getSensitivity());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getIndex());
    hash = hash * 31 + JodaBeanUtils.hashCode(getExpiration());
    hash = hash * 31 + JodaBeanUtils.hashCode(getFixingDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getStrikePrice());
    hash = hash * 31 + JodaBeanUtils.hashCode(getFuturePrice());
    hash = hash * 31 + JodaBeanUtils.hashCode(getCurrency());
    hash = hash * 31 + JodaBeanUtils.hashCode(getSensitivity());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("IborFutureOptionSensitivity{");
    buf.append("index").append('=').append(getIndex()).append(',').append(' ');
    buf.append("expiration").append('=').append(getExpiration()).append(',').append(' ');
    buf.append("fixingDate").append('=').append(getFixingDate()).append(',').append(' ');
    buf.append("strikePrice").append('=').append(getStrikePrice()).append(',').append(' ');
    buf.append("futurePrice").append('=').append(getFuturePrice()).append(',').append(' ');
    buf.append("currency").append('=').append(getCurrency()).append(',').append(' ');
    buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(getSensitivity()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborFutureOptionSensitivity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<IborIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", IborFutureOptionSensitivity.class, IborIndex.class);
    /**
     * The meta-property for the {@code expiration} property.
     */
    private final MetaProperty<ZonedDateTime> expiration = DirectMetaProperty.ofImmutable(
        this, "expiration", IborFutureOptionSensitivity.class, ZonedDateTime.class);
    /**
     * The meta-property for the {@code fixingDate} property.
     */
    private final MetaProperty<LocalDate> fixingDate = DirectMetaProperty.ofImmutable(
        this, "fixingDate", IborFutureOptionSensitivity.class, LocalDate.class);
    /**
     * The meta-property for the {@code strikePrice} property.
     */
    private final MetaProperty<Double> strikePrice = DirectMetaProperty.ofImmutable(
        this, "strikePrice", IborFutureOptionSensitivity.class, Double.TYPE);
    /**
     * The meta-property for the {@code futurePrice} property.
     */
    private final MetaProperty<Double> futurePrice = DirectMetaProperty.ofImmutable(
        this, "futurePrice", IborFutureOptionSensitivity.class, Double.TYPE);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", IborFutureOptionSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code sensitivity} property.
     */
    private final MetaProperty<Double> sensitivity = DirectMetaProperty.ofImmutable(
        this, "sensitivity", IborFutureOptionSensitivity.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "expiration",
        "fixingDate",
        "strikePrice",
        "futurePrice",
        "currency",
        "sensitivity");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case -837465425:  // expiration
          return expiration;
        case 1255202043:  // fixingDate
          return fixingDate;
        case 50946231:  // strikePrice
          return strikePrice;
        case -518499002:  // futurePrice
          return futurePrice;
        case 575402001:  // currency
          return currency;
        case 564403871:  // sensitivity
          return sensitivity;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends IborFutureOptionSensitivity> builder() {
      return new IborFutureOptionSensitivity.Builder();
    }

    @Override
    public Class<? extends IborFutureOptionSensitivity> beanType() {
      return IborFutureOptionSensitivity.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code expiration} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ZonedDateTime> expiration() {
      return expiration;
    }

    /**
     * The meta-property for the {@code fixingDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> fixingDate() {
      return fixingDate;
    }

    /**
     * The meta-property for the {@code strikePrice} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> strikePrice() {
      return strikePrice;
    }

    /**
     * The meta-property for the {@code futurePrice} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> futurePrice() {
      return futurePrice;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code sensitivity} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> sensitivity() {
      return sensitivity;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return ((IborFutureOptionSensitivity) bean).getIndex();
        case -837465425:  // expiration
          return ((IborFutureOptionSensitivity) bean).getExpiration();
        case 1255202043:  // fixingDate
          return ((IborFutureOptionSensitivity) bean).getFixingDate();
        case 50946231:  // strikePrice
          return ((IborFutureOptionSensitivity) bean).getStrikePrice();
        case -518499002:  // futurePrice
          return ((IborFutureOptionSensitivity) bean).getFuturePrice();
        case 575402001:  // currency
          return ((IborFutureOptionSensitivity) bean).getCurrency();
        case 564403871:  // sensitivity
          return ((IborFutureOptionSensitivity) bean).getSensitivity();
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
   * The bean-builder for {@code IborFutureOptionSensitivity}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<IborFutureOptionSensitivity> {

    private IborIndex index;
    private ZonedDateTime expiration;
    private LocalDate fixingDate;
    private double strikePrice;
    private double futurePrice;
    private Currency currency;
    private double sensitivity;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case -837465425:  // expiration
          return expiration;
        case 1255202043:  // fixingDate
          return fixingDate;
        case 50946231:  // strikePrice
          return strikePrice;
        case -518499002:  // futurePrice
          return futurePrice;
        case 575402001:  // currency
          return currency;
        case 564403871:  // sensitivity
          return sensitivity;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          this.index = (IborIndex) newValue;
          break;
        case -837465425:  // expiration
          this.expiration = (ZonedDateTime) newValue;
          break;
        case 1255202043:  // fixingDate
          this.fixingDate = (LocalDate) newValue;
          break;
        case 50946231:  // strikePrice
          this.strikePrice = (Double) newValue;
          break;
        case -518499002:  // futurePrice
          this.futurePrice = (Double) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 564403871:  // sensitivity
          this.sensitivity = (Double) newValue;
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
    public IborFutureOptionSensitivity build() {
      return new IborFutureOptionSensitivity(
          index,
          expiration,
          fixingDate,
          strikePrice,
          futurePrice,
          currency,
          sensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("IborFutureOptionSensitivity.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("expiration").append('=').append(JodaBeanUtils.toString(expiration)).append(',').append(' ');
      buf.append("fixingDate").append('=').append(JodaBeanUtils.toString(fixingDate)).append(',').append(' ');
      buf.append("strikePrice").append('=').append(JodaBeanUtils.toString(strikePrice)).append(',').append(' ');
      buf.append("futurePrice").append('=').append(JodaBeanUtils.toString(futurePrice)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
