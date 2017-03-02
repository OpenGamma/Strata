/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.dsf;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
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

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.SecuritizedProduct;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapLegType;

/**
 * A deliverable swap futures contract.
 * <p>
 * A deliverable swap future is a financial instrument that physically settles
 * an interest rate swap on a future date.
 * The delivered swap is cleared by a central counterparty.
 * The last future price before delivery is quoted in term of the underlying swap present value.
 * The futures product is margined on a daily basis.
 * 
 * <h4>Price</h4>
 * The price of a DSF is based on the present value (NPV) of the underlying swap on the delivery date.
 * For example, a price of 100.182 represents a present value of $100,182.00, if the notional is $100,000.
 * This price can also be viewed as a percentage present value - {@code (100 + percentPv)}, or 0.182% in this example.
 * <p>
 * Strata uses <i>decimal prices</i> for DSFs in the trade model, pricers and market data.
 * The decimal price is based on the decimal multiplier equivalent to the implied percentage.
 * Thus the market price of 100.182 is represented in Strata by 1.00182.
 */
@BeanDefinition(constructorScope = "package")
public final class Dsf
    implements SecuritizedProduct, Resolvable<ResolvedDsf>, ImmutableBean, Serializable {

  /**
   * The security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final SecurityId securityId;
  /**
   * The notional of the futures.
   * <p>
   * This is also called face value or contract value.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero")
  private final double notional;
  /**
   * The last date of trading.
   * <p>
   * This date must be before the delivery date of the underlying swap.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate lastTradeDate;
  /**
   * The delivery date.
   * <p>
   * The underlying swap is delivered on this date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate deliveryDate;
  /**
   * The underlying swap.
   * <p>
   * The delivery date of the future is typically the first accrual date of the underlying swap.
   * The swap should be a receiver swap of notional 1.
   */
  @PropertyDefinition(validate = "notNull")
  private final Swap underlyingSwap;

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderOrEqual(
        deliveryDate, underlyingSwap.getStartDate().getUnadjusted(), "deliveryDate", "underlyingSwap.startDate.unadjusted");
    ArgChecker.isFalse(underlyingSwap.isCrossCurrency(), "Underlying swap must not be cross currency");
    for (SwapLeg swapLeg : underlyingSwap.getLegs()) {
      if (swapLeg.getType().equals(SwapLegType.FIXED)) {
        ArgChecker.isTrue(swapLeg.getPayReceive().isReceive(), "Underlying swap must receive the fixed leg");
      }
      if (swapLeg instanceof RateCalculationSwapLeg) {
        RateCalculationSwapLeg leg = (RateCalculationSwapLeg) swapLeg;
        ArgChecker.isTrue(Math.abs(leg.getNotionalSchedule().getAmount().getInitialValue()) == 1d,
            "Underlying swap must have a notional of 1");
      }
    }
    ArgChecker.inOrderOrEqual(lastTradeDate, deliveryDate, "lastTradeDate", "deliveryDate");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the currency of the underlying swap.
   * <p>
   * The underlying swap must have a single currency.
   * 
   * @return the currency of the swap
   */
  @Override
  public Currency getCurrency() {
    return underlyingSwap.getReceiveLeg().get().getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public ResolvedDsf resolve(ReferenceData refData) {
    ResolvedSwap resolvedSwap = underlyingSwap.resolve(refData);
    return new ResolvedDsf(securityId, notional, deliveryDate, lastTradeDate, resolvedSwap);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code Dsf}.
   * @return the meta-bean, not null
   */
  public static Dsf.Meta meta() {
    return Dsf.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(Dsf.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static Dsf.Builder builder() {
    return new Dsf.Builder();
  }

  /**
   * Creates an instance.
   * @param securityId  the value of the property, not null
   * @param notional  the value of the property
   * @param lastTradeDate  the value of the property, not null
   * @param deliveryDate  the value of the property, not null
   * @param underlyingSwap  the value of the property, not null
   */
  Dsf(
      SecurityId securityId,
      double notional,
      LocalDate lastTradeDate,
      LocalDate deliveryDate,
      Swap underlyingSwap) {
    JodaBeanUtils.notNull(securityId, "securityId");
    ArgChecker.notNegativeOrZero(notional, "notional");
    JodaBeanUtils.notNull(lastTradeDate, "lastTradeDate");
    JodaBeanUtils.notNull(deliveryDate, "deliveryDate");
    JodaBeanUtils.notNull(underlyingSwap, "underlyingSwap");
    this.securityId = securityId;
    this.notional = notional;
    this.lastTradeDate = lastTradeDate;
    this.deliveryDate = deliveryDate;
    this.underlyingSwap = underlyingSwap;
    validate();
  }

  @Override
  public Dsf.Meta metaBean() {
    return Dsf.Meta.INSTANCE;
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
   * Gets the security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   * @return the value of the property, not null
   */
  @Override
  public SecurityId getSecurityId() {
    return securityId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional of the futures.
   * <p>
   * This is also called face value or contract value.
   * @return the value of the property
   */
  public double getNotional() {
    return notional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the last date of trading.
   * <p>
   * This date must be before the delivery date of the underlying swap.
   * @return the value of the property, not null
   */
  public LocalDate getLastTradeDate() {
    return lastTradeDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the delivery date.
   * <p>
   * The underlying swap is delivered on this date.
   * @return the value of the property, not null
   */
  public LocalDate getDeliveryDate() {
    return deliveryDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying swap.
   * <p>
   * The delivery date of the future is typically the first accrual date of the underlying swap.
   * The swap should be a receiver swap of notional 1.
   * @return the value of the property, not null
   */
  public Swap getUnderlyingSwap() {
    return underlyingSwap;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      Dsf other = (Dsf) obj;
      return JodaBeanUtils.equal(securityId, other.securityId) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(lastTradeDate, other.lastTradeDate) &&
          JodaBeanUtils.equal(deliveryDate, other.deliveryDate) &&
          JodaBeanUtils.equal(underlyingSwap, other.underlyingSwap);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(securityId);
    hash = hash * 31 + JodaBeanUtils.hashCode(notional);
    hash = hash * 31 + JodaBeanUtils.hashCode(lastTradeDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(deliveryDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(underlyingSwap);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("Dsf{");
    buf.append("securityId").append('=').append(securityId).append(',').append(' ');
    buf.append("notional").append('=').append(notional).append(',').append(' ');
    buf.append("lastTradeDate").append('=').append(lastTradeDate).append(',').append(' ');
    buf.append("deliveryDate").append('=').append(deliveryDate).append(',').append(' ');
    buf.append("underlyingSwap").append('=').append(JodaBeanUtils.toString(underlyingSwap));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code Dsf}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code securityId} property.
     */
    private final MetaProperty<SecurityId> securityId = DirectMetaProperty.ofImmutable(
        this, "securityId", Dsf.class, SecurityId.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", Dsf.class, Double.TYPE);
    /**
     * The meta-property for the {@code lastTradeDate} property.
     */
    private final MetaProperty<LocalDate> lastTradeDate = DirectMetaProperty.ofImmutable(
        this, "lastTradeDate", Dsf.class, LocalDate.class);
    /**
     * The meta-property for the {@code deliveryDate} property.
     */
    private final MetaProperty<LocalDate> deliveryDate = DirectMetaProperty.ofImmutable(
        this, "deliveryDate", Dsf.class, LocalDate.class);
    /**
     * The meta-property for the {@code underlyingSwap} property.
     */
    private final MetaProperty<Swap> underlyingSwap = DirectMetaProperty.ofImmutable(
        this, "underlyingSwap", Dsf.class, Swap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "securityId",
        "notional",
        "lastTradeDate",
        "deliveryDate",
        "underlyingSwap");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          return securityId;
        case 1585636160:  // notional
          return notional;
        case -1041950404:  // lastTradeDate
          return lastTradeDate;
        case 681469378:  // deliveryDate
          return deliveryDate;
        case 1497421456:  // underlyingSwap
          return underlyingSwap;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public Dsf.Builder builder() {
      return new Dsf.Builder();
    }

    @Override
    public Class<? extends Dsf> beanType() {
      return Dsf.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code securityId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SecurityId> securityId() {
      return securityId;
    }

    /**
     * The meta-property for the {@code notional} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> notional() {
      return notional;
    }

    /**
     * The meta-property for the {@code lastTradeDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> lastTradeDate() {
      return lastTradeDate;
    }

    /**
     * The meta-property for the {@code deliveryDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> deliveryDate() {
      return deliveryDate;
    }

    /**
     * The meta-property for the {@code underlyingSwap} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Swap> underlyingSwap() {
      return underlyingSwap;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          return ((Dsf) bean).getSecurityId();
        case 1585636160:  // notional
          return ((Dsf) bean).getNotional();
        case -1041950404:  // lastTradeDate
          return ((Dsf) bean).getLastTradeDate();
        case 681469378:  // deliveryDate
          return ((Dsf) bean).getDeliveryDate();
        case 1497421456:  // underlyingSwap
          return ((Dsf) bean).getUnderlyingSwap();
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
   * The bean-builder for {@code Dsf}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<Dsf> {

    private SecurityId securityId;
    private double notional;
    private LocalDate lastTradeDate;
    private LocalDate deliveryDate;
    private Swap underlyingSwap;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(Dsf beanToCopy) {
      this.securityId = beanToCopy.getSecurityId();
      this.notional = beanToCopy.getNotional();
      this.lastTradeDate = beanToCopy.getLastTradeDate();
      this.deliveryDate = beanToCopy.getDeliveryDate();
      this.underlyingSwap = beanToCopy.getUnderlyingSwap();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          return securityId;
        case 1585636160:  // notional
          return notional;
        case -1041950404:  // lastTradeDate
          return lastTradeDate;
        case 681469378:  // deliveryDate
          return deliveryDate;
        case 1497421456:  // underlyingSwap
          return underlyingSwap;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          this.securityId = (SecurityId) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (Double) newValue;
          break;
        case -1041950404:  // lastTradeDate
          this.lastTradeDate = (LocalDate) newValue;
          break;
        case 681469378:  // deliveryDate
          this.deliveryDate = (LocalDate) newValue;
          break;
        case 1497421456:  // underlyingSwap
          this.underlyingSwap = (Swap) newValue;
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
    public Dsf build() {
      return new Dsf(
          securityId,
          notional,
          lastTradeDate,
          deliveryDate,
          underlyingSwap);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the security identifier.
     * <p>
     * This identifier uniquely identifies the security within the system.
     * @param securityId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder securityId(SecurityId securityId) {
      JodaBeanUtils.notNull(securityId, "securityId");
      this.securityId = securityId;
      return this;
    }

    /**
     * Sets the notional of the futures.
     * <p>
     * This is also called face value or contract value.
     * @param notional  the new value
     * @return this, for chaining, not null
     */
    public Builder notional(double notional) {
      ArgChecker.notNegativeOrZero(notional, "notional");
      this.notional = notional;
      return this;
    }

    /**
     * Sets the last date of trading.
     * <p>
     * This date must be before the delivery date of the underlying swap.
     * @param lastTradeDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder lastTradeDate(LocalDate lastTradeDate) {
      JodaBeanUtils.notNull(lastTradeDate, "lastTradeDate");
      this.lastTradeDate = lastTradeDate;
      return this;
    }

    /**
     * Sets the delivery date.
     * <p>
     * The underlying swap is delivered on this date.
     * @param deliveryDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder deliveryDate(LocalDate deliveryDate) {
      JodaBeanUtils.notNull(deliveryDate, "deliveryDate");
      this.deliveryDate = deliveryDate;
      return this;
    }

    /**
     * Sets the underlying swap.
     * <p>
     * The delivery date of the future is typically the first accrual date of the underlying swap.
     * The swap should be a receiver swap of notional 1.
     * @param underlyingSwap  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder underlyingSwap(Swap underlyingSwap) {
      JodaBeanUtils.notNull(underlyingSwap, "underlyingSwap");
      this.underlyingSwap = underlyingSwap;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("Dsf.Builder{");
      buf.append("securityId").append('=').append(JodaBeanUtils.toString(securityId)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("lastTradeDate").append('=').append(JodaBeanUtils.toString(lastTradeDate)).append(',').append(' ');
      buf.append("deliveryDate").append('=').append(JodaBeanUtils.toString(deliveryDate)).append(',').append(' ');
      buf.append("underlyingSwap").append('=').append(JodaBeanUtils.toString(underlyingSwap));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
