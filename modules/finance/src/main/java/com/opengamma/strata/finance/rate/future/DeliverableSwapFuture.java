/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.future;

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

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.id.LinkResolutionException;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.Resolvable;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.Product;
import com.opengamma.strata.finance.Security;
import com.opengamma.strata.finance.SecurityLink;
import com.opengamma.strata.finance.rate.swap.ExpandedSwapLeg;
import com.opengamma.strata.finance.rate.swap.NotionalExchange;
import com.opengamma.strata.finance.rate.swap.NotionalPaymentPeriod;
import com.opengamma.strata.finance.rate.swap.PaymentEvent;
import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapLeg;

/**
 * A deliverable swap futures contract.
 * <p>
 * A deliverable swap future is a financial instrument that physically settles an interest rate swap on a future date. 
 * The futures product is margined on a daily basis. 
 * This class represents the structure of a single futures contract.
 */
@BeanDefinition
public final class DeliverableSwapFuture
    implements Product, Resolvable<DeliverableSwapFuture>, ImmutableBean, Serializable {

  /**
   * The link to the underlying swap.
   * <p>
   * This property returns a link to the security via a {@link StandardId}.
   * See {@link #getUnderlying()} and {@link SecurityLink} for more details.
   */
  @PropertyDefinition(validate = "notNull")
  private final SecurityLink<Swap> underlyingLink;
  /**
   * The delivery date. 
   * <p>
   * The underlying swap is delivered on this date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate deliveryDate;
  /**
   * The last date of trading.
   * <p>
   * This date must be before the delivery date of the underlying swap. 
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate lastTradeDate;
  /**
   * The notional of the futures. 
   * <p>
   * This is also called face value or contract value.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double notional;
  /**
   * The definition of how to round the option price, defaulted to no rounding.
   * <p>
   * The price is represented in decimal form, not percentage form.
   * As such, the decimal places expressed by the rounding refers to this decimal form.
   * For example, the common market price of 99.7125 is represented as 0.997125 which
   * has 6 decimal places.
   */
  @PropertyDefinition(validate = "notNull")
  private final Rounding rounding;

  @ImmutableValidator
  private void validate() {
    if (underlyingLink.isResolved()) {
      Swap swap = getUnderlying();
      ArgChecker.inOrderOrEqual(deliveryDate, swap.getStartDate(), "deliveryDate", "startDate");
      ArgChecker.isFalse(swap.isCrossCurrency(), "underlying swap must not be cross currency");
      for (SwapLeg swapLeg : swap.getLegs()) {
        ExpandedSwapLeg expandedSwapLeg = swapLeg.expand();
        for (PaymentEvent event : expandedSwapLeg.getPaymentEvents()) {
          ArgChecker.isTrue(event instanceof NotionalExchange, "PaymentEvent must be NotionalExchange");
          NotionalExchange notioanlEvent = (NotionalExchange) event;
          ArgChecker.isTrue(Math.abs(notioanlEvent.getPaymentAmount().getAmount()) == 1d,
              "notional of underlying swap must be unity");
        }
        for (PaymentPeriod period : expandedSwapLeg.getPaymentPeriods()) {
          ArgChecker.isTrue(period instanceof NotionalPaymentPeriod, "PaymentPeriod must be NotionalPaymentPeriod");
          NotionalPaymentPeriod notioanlPeriod = (NotionalPaymentPeriod) period;
          ArgChecker.isTrue(Math.abs(notioanlPeriod.getNotionalAmount().getAmount()) == 1d,
              "notional of underlying swap must be unity");
        }
      }
    }
    ArgChecker.inOrderOrEqual(lastTradeDate, deliveryDate, "lastTradeDate", "deliveryDate");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the underlying swap security that was traded, throwing an exception if not resolved.
   * <p>
   * This method accesses the security via the {@link #getUnderlyingLink() underlyingLink} property.
   * The link has two states, resolvable and resolved.
   * <p>
   * In the resolved state, the security is known and available for use.
   * The security object will be directly embedded in the link held within this trade.
   * <p>
   * In the resolvable state, only the identifier and type of the security are known.
   * These act as a pointer to the security, and as such the security is not directly available.
   * The link must be resolved before use.
   * This can be achieved by calling {@link #resolveLinks(LinkResolver)} on this trade.
   * If the trade has not been resolved, then this method will throw a {@link LinkResolutionException}.
   * 
   * @return full details of the security
   * @throws LinkResolutionException if the security is not resolved
   */
  public Security<Swap> getUnderlyingSecurity() {
    return underlyingLink.resolvedTarget();
  }

  /**
   * Gets the underlying swap that was traded, throwing an exception if not resolved.
   * <p>
   * Returns the underlying product that captures the contracted financial details of the trade.
   * This method accesses the security via the {@link #getUnderlyingLink() underlyingLink} property.
   * The link has two states, resolvable and resolved.
   * <p>
   * In the resolved state, the security is known and available for use.
   * The security object will be directly embedded in the link held within this trade.
   * <p>
   * In the resolvable state, only the identifier and type of the security are known.
   * These act as a pointer to the security, and as such the security is not directly available.
   * The link must be resolved before use.
   * This can be achieved by calling {@link #resolveLinks(LinkResolver)} on this trade.
   * If the trade has not been resolved, then this method will throw a {@link LinkResolutionException}.
   * 
   * @return the product underlying the option
   * @throws LinkResolutionException if the security is not resolved
   */
  public Swap getUnderlying() {
    return getUnderlyingSecurity().getProduct();
  }

  /**
   * Gets the currency of the underlying swap.
   * <p>
   * The underlying swap must have a single currency.
   * 
   * @return the currency of the swap
   */
  public Currency getCurrency() {
    return getUnderlying().getReceiveLeg().get().getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public DeliverableSwapFuture resolveLinks(LinkResolver resolver) {
    return resolver.resolveLinksIn(this, underlyingLink, resolved -> toBuilder().underlyingLink(resolved).build());
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code DeliverableSwapFuture}.
   * @return the meta-bean, not null
   */
  public static DeliverableSwapFuture.Meta meta() {
    return DeliverableSwapFuture.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(DeliverableSwapFuture.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static DeliverableSwapFuture.Builder builder() {
    return new DeliverableSwapFuture.Builder();
  }

  private DeliverableSwapFuture(
      SecurityLink<Swap> underlyingLink,
      LocalDate deliveryDate,
      LocalDate lastTradeDate,
      double notional,
      Rounding rounding) {
    JodaBeanUtils.notNull(underlyingLink, "underlyingLink");
    JodaBeanUtils.notNull(deliveryDate, "deliveryDate");
    JodaBeanUtils.notNull(lastTradeDate, "lastTradeDate");
    ArgChecker.notNegative(notional, "notional");
    JodaBeanUtils.notNull(rounding, "rounding");
    this.underlyingLink = underlyingLink;
    this.deliveryDate = deliveryDate;
    this.lastTradeDate = lastTradeDate;
    this.notional = notional;
    this.rounding = rounding;
    validate();
  }

  @Override
  public DeliverableSwapFuture.Meta metaBean() {
    return DeliverableSwapFuture.Meta.INSTANCE;
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
   * Gets the link to the underlying swap.
   * <p>
   * This property returns a link to the security via a {@link StandardId}.
   * See {@link #getUnderlying()} and {@link SecurityLink} for more details.
   * @return the value of the property, not null
   */
  public SecurityLink<Swap> getUnderlyingLink() {
    return underlyingLink;
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
   * Gets the definition of how to round the option price, defaulted to no rounding.
   * <p>
   * The price is represented in decimal form, not percentage form.
   * As such, the decimal places expressed by the rounding refers to this decimal form.
   * For example, the common market price of 99.7125 is represented as 0.997125 which
   * has 6 decimal places.
   * @return the value of the property, not null
   */
  public Rounding getRounding() {
    return rounding;
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
      DeliverableSwapFuture other = (DeliverableSwapFuture) obj;
      return JodaBeanUtils.equal(getUnderlyingLink(), other.getUnderlyingLink()) &&
          JodaBeanUtils.equal(getDeliveryDate(), other.getDeliveryDate()) &&
          JodaBeanUtils.equal(getLastTradeDate(), other.getLastTradeDate()) &&
          JodaBeanUtils.equal(getNotional(), other.getNotional()) &&
          JodaBeanUtils.equal(getRounding(), other.getRounding());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getUnderlyingLink());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDeliveryDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getLastTradeDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getNotional());
    hash = hash * 31 + JodaBeanUtils.hashCode(getRounding());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("DeliverableSwapFuture{");
    buf.append("underlyingLink").append('=').append(getUnderlyingLink()).append(',').append(' ');
    buf.append("deliveryDate").append('=').append(getDeliveryDate()).append(',').append(' ');
    buf.append("lastTradeDate").append('=').append(getLastTradeDate()).append(',').append(' ');
    buf.append("notional").append('=').append(getNotional()).append(',').append(' ');
    buf.append("rounding").append('=').append(JodaBeanUtils.toString(getRounding()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code DeliverableSwapFuture}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code underlyingLink} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<SecurityLink<Swap>> underlyingLink = DirectMetaProperty.ofImmutable(
        this, "underlyingLink", DeliverableSwapFuture.class, (Class) SecurityLink.class);
    /**
     * The meta-property for the {@code deliveryDate} property.
     */
    private final MetaProperty<LocalDate> deliveryDate = DirectMetaProperty.ofImmutable(
        this, "deliveryDate", DeliverableSwapFuture.class, LocalDate.class);
    /**
     * The meta-property for the {@code lastTradeDate} property.
     */
    private final MetaProperty<LocalDate> lastTradeDate = DirectMetaProperty.ofImmutable(
        this, "lastTradeDate", DeliverableSwapFuture.class, LocalDate.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", DeliverableSwapFuture.class, Double.TYPE);
    /**
     * The meta-property for the {@code rounding} property.
     */
    private final MetaProperty<Rounding> rounding = DirectMetaProperty.ofImmutable(
        this, "rounding", DeliverableSwapFuture.class, Rounding.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "underlyingLink",
        "deliveryDate",
        "lastTradeDate",
        "notional",
        "rounding");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1497199863:  // underlyingLink
          return underlyingLink;
        case 681469378:  // deliveryDate
          return deliveryDate;
        case -1041950404:  // lastTradeDate
          return lastTradeDate;
        case 1585636160:  // notional
          return notional;
        case -142444:  // rounding
          return rounding;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public DeliverableSwapFuture.Builder builder() {
      return new DeliverableSwapFuture.Builder();
    }

    @Override
    public Class<? extends DeliverableSwapFuture> beanType() {
      return DeliverableSwapFuture.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code underlyingLink} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SecurityLink<Swap>> underlyingLink() {
      return underlyingLink;
    }

    /**
     * The meta-property for the {@code deliveryDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> deliveryDate() {
      return deliveryDate;
    }

    /**
     * The meta-property for the {@code lastTradeDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> lastTradeDate() {
      return lastTradeDate;
    }

    /**
     * The meta-property for the {@code notional} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> notional() {
      return notional;
    }

    /**
     * The meta-property for the {@code rounding} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Rounding> rounding() {
      return rounding;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1497199863:  // underlyingLink
          return ((DeliverableSwapFuture) bean).getUnderlyingLink();
        case 681469378:  // deliveryDate
          return ((DeliverableSwapFuture) bean).getDeliveryDate();
        case -1041950404:  // lastTradeDate
          return ((DeliverableSwapFuture) bean).getLastTradeDate();
        case 1585636160:  // notional
          return ((DeliverableSwapFuture) bean).getNotional();
        case -142444:  // rounding
          return ((DeliverableSwapFuture) bean).getRounding();
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
   * The bean-builder for {@code DeliverableSwapFuture}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<DeliverableSwapFuture> {

    private SecurityLink<Swap> underlyingLink;
    private LocalDate deliveryDate;
    private LocalDate lastTradeDate;
    private double notional;
    private Rounding rounding;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(DeliverableSwapFuture beanToCopy) {
      this.underlyingLink = beanToCopy.getUnderlyingLink();
      this.deliveryDate = beanToCopy.getDeliveryDate();
      this.lastTradeDate = beanToCopy.getLastTradeDate();
      this.notional = beanToCopy.getNotional();
      this.rounding = beanToCopy.getRounding();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1497199863:  // underlyingLink
          return underlyingLink;
        case 681469378:  // deliveryDate
          return deliveryDate;
        case -1041950404:  // lastTradeDate
          return lastTradeDate;
        case 1585636160:  // notional
          return notional;
        case -142444:  // rounding
          return rounding;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1497199863:  // underlyingLink
          this.underlyingLink = (SecurityLink<Swap>) newValue;
          break;
        case 681469378:  // deliveryDate
          this.deliveryDate = (LocalDate) newValue;
          break;
        case -1041950404:  // lastTradeDate
          this.lastTradeDate = (LocalDate) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (Double) newValue;
          break;
        case -142444:  // rounding
          this.rounding = (Rounding) newValue;
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
    public DeliverableSwapFuture build() {
      return new DeliverableSwapFuture(
          underlyingLink,
          deliveryDate,
          lastTradeDate,
          notional,
          rounding);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the link to the underlying swap.
     * <p>
     * This property returns a link to the security via a {@link StandardId}.
     * See {@link #getUnderlying()} and {@link SecurityLink} for more details.
     * @param underlyingLink  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder underlyingLink(SecurityLink<Swap> underlyingLink) {
      JodaBeanUtils.notNull(underlyingLink, "underlyingLink");
      this.underlyingLink = underlyingLink;
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
     * Sets the notional of the futures.
     * <p>
     * This is also called face value or contract value.
     * @param notional  the new value
     * @return this, for chaining, not null
     */
    public Builder notional(double notional) {
      ArgChecker.notNegative(notional, "notional");
      this.notional = notional;
      return this;
    }

    /**
     * Sets the definition of how to round the option price, defaulted to no rounding.
     * <p>
     * The price is represented in decimal form, not percentage form.
     * As such, the decimal places expressed by the rounding refers to this decimal form.
     * For example, the common market price of 99.7125 is represented as 0.997125 which
     * has 6 decimal places.
     * @param rounding  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder rounding(Rounding rounding) {
      JodaBeanUtils.notNull(rounding, "rounding");
      this.rounding = rounding;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("DeliverableSwapFuture.Builder{");
      buf.append("underlyingLink").append('=').append(JodaBeanUtils.toString(underlyingLink)).append(',').append(' ');
      buf.append("deliveryDate").append('=').append(JodaBeanUtils.toString(deliveryDate)).append(',').append(' ');
      buf.append("lastTradeDate").append('=').append(JodaBeanUtils.toString(lastTradeDate)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("rounding").append('=').append(JodaBeanUtils.toString(rounding));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
