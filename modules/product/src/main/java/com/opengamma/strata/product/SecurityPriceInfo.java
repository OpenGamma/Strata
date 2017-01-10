/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.DerivedProperty;
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

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Defines the meaning of the security price.
 * <p>
 * This provides information about the price of a security.
 * <p>
 * Tick size is the minimum movement in the price of the security.
 * Tick value is the monetary value of the minimum movement in the price of the security.
 * Contract size is the quantity of the underlying present in each derivative contract.
 */
@BeanDefinition(builderScope = "private")
public final class SecurityPriceInfo
    implements ImmutableBean, Serializable {

  /**
   * The size of each tick.
   * <p>
   * Tick size is the minimum movement in the price of the security.
   * For example, the price might move up or down in units of 0.01
   * It must be a positive decimal number.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero")
  private final double tickSize;
  /**
   * The monetary value of one tick.
   * <p>
   * Tick value is the monetary value of the minimum movement in the price of the security.
   * When the price changes by one tick, this amount is gained or lost.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyAmount tickValue;
  /**
   * The size of each contract.
   * <p>
   * Contract size is the quantity of the underlying present in each derivative contract.
   * For example, an equity option typically consists of 100 shares.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero")
  private final double contractSize;
  /**
   * Multiplier to apply to the price.
   */
  private final transient double tradeUnitValue;  // derived, not a property

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the tick size and tick value.
   * <p>
   * The contract size will be set to 1.
   * 
   * @param tickSize  the size of each tick, not negative or zero
   * @param tickValue  the value of each tick
   * @return the security price information
   */
  public static SecurityPriceInfo of(double tickSize, CurrencyAmount tickValue) {
    return new SecurityPriceInfo(tickSize, tickValue, 1);
  }

  /**
   * Obtains an instance from the tick size, tick value and contract size.
   * 
   * @param tickSize  the size of each tick, not negative or zero
   * @param tickValue  the value of each tick
   * @param contractSize  the contract size
   * @return the security price information
   */
  public static SecurityPriceInfo of(double tickSize, CurrencyAmount tickValue, double contractSize) {
    return new SecurityPriceInfo(tickSize, tickValue, contractSize);
  }

  /**
   * Obtains an instance from the currency and the value of a single tradeable unit.
   *
   * @param currency  the currency in which the security is traded
   * @param tradeUnitValue  the value of a single tradeable unit of the security
   * @return the security price information
   */
  public static SecurityPriceInfo of(Currency currency, double tradeUnitValue) {
    return new SecurityPriceInfo(1, CurrencyAmount.of(currency, 1), tradeUnitValue);
  }

  /**
   * Obtains an instance from the currency.
   * <p>
   * This sets the tick size and tick value to the minor unit of the currency.
   * For example, for USD this will set the tick size to 0.01 and the tick value to $0.01.
   * This typically matches the conventions of equities and bonds.
   * 
   * @param currency  the currency to derive the price information from
   * @return the security price information
   */
  public static SecurityPriceInfo ofCurrencyMinorUnit(Currency currency) {
    int digits = currency.getMinorUnitDigits();
    double unitAmount = Math.pow(10, -digits);
    return new SecurityPriceInfo(unitAmount, CurrencyAmount.of(currency, unitAmount), 1);
  }

  @ImmutableConstructor
  private SecurityPriceInfo(
      double tickSize,
      CurrencyAmount tickValue,
      double contractSize) {
    ArgChecker.notNegativeOrZero(tickSize, "tickSize");
    JodaBeanUtils.notNull(tickValue, "tickValue");
    ArgChecker.notNegativeOrZero(contractSize, "contractSize");
    this.tickSize = tickSize;
    this.tickValue = tickValue;
    this.contractSize = contractSize;
    this.tradeUnitValue = (tickValue.getAmount() * contractSize) / tickSize;
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new SecurityPriceInfo(tickSize, tickValue, contractSize);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency that the security is traded in.
   * <p>
   * The currency is derived from the tick value.
   * 
   * @return the currency
   */
  @DerivedProperty
  public Currency getCurrency() {
    return tickValue.getCurrency();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the monetary value of the specified quantity and price.
   * <p>
   * This calculates a monetary value using the stored price information.
   * For equities, this is the premium that will be paid.
   * For bonds, this will be the premium if the price specified is the <i>dirty</i> price.
   * For margined ETDs, the profit or loss per day is the monetary difference
   * between two calls to this method with the price on each day.
   * <p>
   * This returns {@link #calculateMonetaryValue(double, double)} as a {@link CurrencyAmount}.
   *
   * @param quantity  the quantity, such as the number of shares or number of future contracts
   * @param price  the price, typically from the market
   * @return the monetary value combining the tick size, tick value, contract size, quantity and price.
   */
  public CurrencyAmount calculateMonetaryAmount(double quantity, double price) {
    return CurrencyAmount.of(tickValue.getCurrency(), calculateMonetaryValue(quantity, price));
  }

  /**
   * Calculates the monetary value of the specified quantity and price.
   * <p>
   * This calculates a monetary value using the stored price information.
   * For equities, this is the premium that will be paid.
   * For bonds, this will be the premium if the price specified is the <i>dirty</i> price.
   * For margined ETDs, the profit or loss per day is the monetary difference
   * between two calls to this method with the price on each day.
   *
   * @param quantity  the quantity, such as the number of shares or number of future contracts
   * @param price  the price, typically from the market
   * @return the monetary value combining the tick size, tick value, contract size, quantity and price.
   */
  public double calculateMonetaryValue(double quantity, double price) {
    return price * quantity * tradeUnitValue;
  }

  /**
   * Returns the value of a single tradeable unit of the security.
   * <p>
   * The monetary value of a position in a security is
   * <pre>tradeUnitValue * price * quantity</pre>
   * <p>
   * This value is normally derived from the tick size, tick value and contract size:
   * <pre>tradeUnitValue = tickValue * contractSize / tickSize</pre>
   *
   * @return the value of a single tradeable unit of the security
   */
  public double getTradeUnitValue() {
    return tradeUnitValue;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SecurityPriceInfo}.
   * @return the meta-bean, not null
   */
  public static SecurityPriceInfo.Meta meta() {
    return SecurityPriceInfo.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SecurityPriceInfo.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public SecurityPriceInfo.Meta metaBean() {
    return SecurityPriceInfo.Meta.INSTANCE;
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
   * Gets the size of each tick.
   * <p>
   * Tick size is the minimum movement in the price of the security.
   * For example, the price might move up or down in units of 0.01
   * It must be a positive decimal number.
   * @return the value of the property
   */
  public double getTickSize() {
    return tickSize;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the monetary value of one tick.
   * <p>
   * Tick value is the monetary value of the minimum movement in the price of the security.
   * When the price changes by one tick, this amount is gained or lost.
   * @return the value of the property, not null
   */
  public CurrencyAmount getTickValue() {
    return tickValue;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the size of each contract.
   * <p>
   * Contract size is the quantity of the underlying present in each derivative contract.
   * For example, an equity option typically consists of 100 shares.
   * @return the value of the property
   */
  public double getContractSize() {
    return contractSize;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SecurityPriceInfo other = (SecurityPriceInfo) obj;
      return JodaBeanUtils.equal(tickSize, other.tickSize) &&
          JodaBeanUtils.equal(tickValue, other.tickValue) &&
          JodaBeanUtils.equal(contractSize, other.contractSize);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(tickSize);
    hash = hash * 31 + JodaBeanUtils.hashCode(tickValue);
    hash = hash * 31 + JodaBeanUtils.hashCode(contractSize);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("SecurityPriceInfo{");
    buf.append("tickSize").append('=').append(tickSize).append(',').append(' ');
    buf.append("tickValue").append('=').append(tickValue).append(',').append(' ');
    buf.append("contractSize").append('=').append(contractSize).append(',').append(' ');
    buf.append("currency").append('=').append(JodaBeanUtils.toString(getCurrency()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SecurityPriceInfo}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code tickSize} property.
     */
    private final MetaProperty<Double> tickSize = DirectMetaProperty.ofImmutable(
        this, "tickSize", SecurityPriceInfo.class, Double.TYPE);
    /**
     * The meta-property for the {@code tickValue} property.
     */
    private final MetaProperty<CurrencyAmount> tickValue = DirectMetaProperty.ofImmutable(
        this, "tickValue", SecurityPriceInfo.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code contractSize} property.
     */
    private final MetaProperty<Double> contractSize = DirectMetaProperty.ofImmutable(
        this, "contractSize", SecurityPriceInfo.class, Double.TYPE);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofDerived(
        this, "currency", SecurityPriceInfo.class, Currency.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "tickSize",
        "tickValue",
        "contractSize",
        "currency");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1936822078:  // tickSize
          return tickSize;
        case -85538348:  // tickValue
          return tickValue;
        case -1402368973:  // contractSize
          return contractSize;
        case 575402001:  // currency
          return currency;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends SecurityPriceInfo> builder() {
      return new SecurityPriceInfo.Builder();
    }

    @Override
    public Class<? extends SecurityPriceInfo> beanType() {
      return SecurityPriceInfo.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code tickSize} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> tickSize() {
      return tickSize;
    }

    /**
     * The meta-property for the {@code tickValue} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyAmount> tickValue() {
      return tickValue;
    }

    /**
     * The meta-property for the {@code contractSize} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> contractSize() {
      return contractSize;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1936822078:  // tickSize
          return ((SecurityPriceInfo) bean).getTickSize();
        case -85538348:  // tickValue
          return ((SecurityPriceInfo) bean).getTickValue();
        case -1402368973:  // contractSize
          return ((SecurityPriceInfo) bean).getContractSize();
        case 575402001:  // currency
          return ((SecurityPriceInfo) bean).getCurrency();
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
   * The bean-builder for {@code SecurityPriceInfo}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<SecurityPriceInfo> {

    private double tickSize;
    private CurrencyAmount tickValue;
    private double contractSize;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1936822078:  // tickSize
          return tickSize;
        case -85538348:  // tickValue
          return tickValue;
        case -1402368973:  // contractSize
          return contractSize;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1936822078:  // tickSize
          this.tickSize = (Double) newValue;
          break;
        case -85538348:  // tickValue
          this.tickValue = (CurrencyAmount) newValue;
          break;
        case -1402368973:  // contractSize
          this.contractSize = (Double) newValue;
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
    public SecurityPriceInfo build() {
      return new SecurityPriceInfo(
          tickSize,
          tickValue,
          contractSize);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("SecurityPriceInfo.Builder{");
      buf.append("tickSize").append('=').append(JodaBeanUtils.toString(tickSize)).append(',').append(' ');
      buf.append("tickValue").append('=').append(JodaBeanUtils.toString(tickValue)).append(',').append(' ');
      buf.append("contractSize").append('=').append(JodaBeanUtils.toString(contractSize));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
