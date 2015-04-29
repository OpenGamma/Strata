/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.future;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.id.LinkResolutionException;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.Resolvable;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.Product;
import com.opengamma.strata.finance.Security;
import com.opengamma.strata.finance.SecurityLink;
import com.opengamma.strata.finance.common.FutureOptionPremiumStyle;

/**
 * A futures option contract, based on an IBOR-like index.
 * <p>
 * An Ibor future option is a financial instrument that provides an option based on the future value of
 * an IBOR-like interest rate. The option is American, exercised at any point up to the exercise time.
 * It handles options with either daily margining or upfront premium.
 * <p>
 * An Ibor future option is also known as a <i>STIR future option</i> (Short Term Interest Rate).
 * This class represents the structure of a single option contract.
 */
@BeanDefinition
public class IborFutureOption
    implements Product, Resolvable<IborFutureOption>, ImmutableBean, Serializable {

  /**
   * Whether the option is put or call.
   * <p>
   * A call gives the owner the right, but not obligation, to buy the underlying at
   * an agreed price in the future. A put gives a similar option to sell.
   */
  @PropertyDefinition
  private final PutCall putCall;
  /**
   * The strike price, represented in decimal form.
   * <p>
   * This is the price at which the option applies and refers to the price of the underlying future.
   * This must be represented in decimal form, {@code (1.0 - decimalRate)}. 
   * As such, the common market price of 99.3 for a 0.7% rate must be input as 0.993.
   * The rate implied by the strike can take negative values.
   */
  @PropertyDefinition
  private final double strikePrice;
  /**
   * The expiration date of the option.  
   * <p>
   * The expiration date is related to the expiration time and time-zone.
   * The date must not be after last trade date of the underlying future. 
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate expirationDate;
  /**
   * The expiration time of the option.  
   * <p>
   * The expiration time is related to the expiration date and time-zone.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalTime expirationTime;
  /**
   * The time-zone of the expiration time.  
   * <p>
   * The expiration time-zone is related to the expiration date and time.
   */
  @PropertyDefinition(validate = "notNull")
  private final ZoneId expirationZone;
  /**
   * The style of the option premium.
   * <p>
   * The two options are daily margining and upfront premium.
   */
  @PropertyDefinition(validate = "notNull")
  private final FutureOptionPremiumStyle premiumStyle;
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
  /**
   * The link to the underlying future.
   * <p>
   * This property returns a link to the security via a {@link StandardId}.
   * See {@link #getUnderlying()} and {@link SecurityLink} for more details.
   */
  @PropertyDefinition(validate = "notNull")
  private final SecurityLink<IborFuture> underlyingLink;

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    if (underlyingLink.isResolved()) {
      LocalDate lastTradeDate = underlyingLink.resolve(null).getProduct().getLastTradeDate();
      ArgChecker.inOrderOrEqual(expirationDate, lastTradeDate, "expirationDate", "lastTradeDate");
    }
  }

  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.rounding(Rounding.none());
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the expiration date-time.
   * <p>
   * The option expires at this date and time.
   * <p>
   * The result is returned by combining the expiration date, time and time-zone.
   * 
   * @return the expiration date and time
   */
  public ZonedDateTime getExpirationDateTime() {
    return expirationDate.atTime(expirationTime).atZone(expirationZone);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the underlying Ibor future security that was traded, throwing an exception if not resolved.
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
  public Security<IborFuture> getUnderlyingSecurity() {
    return underlyingLink.resolvedTarget();
  }

  /**
   * Gets the underlying Ibor future that was traded, throwing an exception if not resolved.
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
  public IborFuture getUnderlying() {
    return getUnderlyingSecurity().getProduct();
  }

  //-------------------------------------------------------------------------
  @Override
  public IborFutureOption resolveLinks(LinkResolver resolver) {
    return resolver.resolveLinksIn(this, underlyingLink, resolved -> toBuilder().underlyingLink(resolved).build());
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborFutureOption}.
   * @return the meta-bean, not null
   */
  public static IborFutureOption.Meta meta() {
    return IborFutureOption.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IborFutureOption.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static IborFutureOption.Builder builder() {
    return new IborFutureOption.Builder();
  }

  /**
   * Restricted constructor.
   * @param builder  the builder to copy from, not null
   */
  protected IborFutureOption(IborFutureOption.Builder builder) {
    JodaBeanUtils.notNull(builder.expirationDate, "expirationDate");
    JodaBeanUtils.notNull(builder.expirationTime, "expirationTime");
    JodaBeanUtils.notNull(builder.expirationZone, "expirationZone");
    JodaBeanUtils.notNull(builder.premiumStyle, "premiumStyle");
    JodaBeanUtils.notNull(builder.rounding, "rounding");
    JodaBeanUtils.notNull(builder.underlyingLink, "underlyingLink");
    this.putCall = builder.putCall;
    this.strikePrice = builder.strikePrice;
    this.expirationDate = builder.expirationDate;
    this.expirationTime = builder.expirationTime;
    this.expirationZone = builder.expirationZone;
    this.premiumStyle = builder.premiumStyle;
    this.rounding = builder.rounding;
    this.underlyingLink = builder.underlyingLink;
    validate();
  }

  @Override
  public IborFutureOption.Meta metaBean() {
    return IborFutureOption.Meta.INSTANCE;
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
   * Gets whether the option is put or call.
   * <p>
   * A call gives the owner the right, but not obligation, to buy the underlying at
   * an agreed price in the future. A put gives a similar option to sell.
   * @return the value of the property
   */
  public PutCall getPutCall() {
    return putCall;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the strike price, represented in decimal form.
   * <p>
   * This is the price at which the option applies and refers to the price of the underlying future.
   * This must be represented in decimal form, {@code (1.0 - decimalRate)}.
   * As such, the common market price of 99.3 for a 0.7% rate must be input as 0.993.
   * The rate implied by the strike can take negative values.
   * @return the value of the property
   */
  public double getStrikePrice() {
    return strikePrice;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the expiration date of the option.
   * <p>
   * The expiration date is related to the expiration time and time-zone.
   * The date must not be after last trade date of the underlying future.
   * @return the value of the property, not null
   */
  public LocalDate getExpirationDate() {
    return expirationDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the expiration time of the option.
   * <p>
   * The expiration time is related to the expiration date and time-zone.
   * @return the value of the property, not null
   */
  public LocalTime getExpirationTime() {
    return expirationTime;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time-zone of the expiration time.
   * <p>
   * The expiration time-zone is related to the expiration date and time.
   * @return the value of the property, not null
   */
  public ZoneId getExpirationZone() {
    return expirationZone;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the style of the option premium.
   * <p>
   * The two options are daily margining and upfront premium.
   * @return the value of the property, not null
   */
  public FutureOptionPremiumStyle getPremiumStyle() {
    return premiumStyle;
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
   * Gets the link to the underlying future.
   * <p>
   * This property returns a link to the security via a {@link StandardId}.
   * See {@link #getUnderlying()} and {@link SecurityLink} for more details.
   * @return the value of the property, not null
   */
  public SecurityLink<IborFuture> getUnderlyingLink() {
    return underlyingLink;
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
      IborFutureOption other = (IborFutureOption) obj;
      return JodaBeanUtils.equal(getPutCall(), other.getPutCall()) &&
          JodaBeanUtils.equal(getStrikePrice(), other.getStrikePrice()) &&
          JodaBeanUtils.equal(getExpirationDate(), other.getExpirationDate()) &&
          JodaBeanUtils.equal(getExpirationTime(), other.getExpirationTime()) &&
          JodaBeanUtils.equal(getExpirationZone(), other.getExpirationZone()) &&
          JodaBeanUtils.equal(getPremiumStyle(), other.getPremiumStyle()) &&
          JodaBeanUtils.equal(getRounding(), other.getRounding()) &&
          JodaBeanUtils.equal(getUnderlyingLink(), other.getUnderlyingLink());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getPutCall());
    hash = hash * 31 + JodaBeanUtils.hashCode(getStrikePrice());
    hash = hash * 31 + JodaBeanUtils.hashCode(getExpirationDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getExpirationTime());
    hash = hash * 31 + JodaBeanUtils.hashCode(getExpirationZone());
    hash = hash * 31 + JodaBeanUtils.hashCode(getPremiumStyle());
    hash = hash * 31 + JodaBeanUtils.hashCode(getRounding());
    hash = hash * 31 + JodaBeanUtils.hashCode(getUnderlyingLink());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(288);
    buf.append("IborFutureOption{");
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  protected void toString(StringBuilder buf) {
    buf.append("putCall").append('=').append(JodaBeanUtils.toString(getPutCall())).append(',').append(' ');
    buf.append("strikePrice").append('=').append(JodaBeanUtils.toString(getStrikePrice())).append(',').append(' ');
    buf.append("expirationDate").append('=').append(JodaBeanUtils.toString(getExpirationDate())).append(',').append(' ');
    buf.append("expirationTime").append('=').append(JodaBeanUtils.toString(getExpirationTime())).append(',').append(' ');
    buf.append("expirationZone").append('=').append(JodaBeanUtils.toString(getExpirationZone())).append(',').append(' ');
    buf.append("premiumStyle").append('=').append(JodaBeanUtils.toString(getPremiumStyle())).append(',').append(' ');
    buf.append("rounding").append('=').append(JodaBeanUtils.toString(getRounding())).append(',').append(' ');
    buf.append("underlyingLink").append('=').append(JodaBeanUtils.toString(getUnderlyingLink())).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborFutureOption}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code putCall} property.
     */
    private final MetaProperty<PutCall> putCall = DirectMetaProperty.ofImmutable(
        this, "putCall", IborFutureOption.class, PutCall.class);
    /**
     * The meta-property for the {@code strikePrice} property.
     */
    private final MetaProperty<Double> strikePrice = DirectMetaProperty.ofImmutable(
        this, "strikePrice", IborFutureOption.class, Double.TYPE);
    /**
     * The meta-property for the {@code expirationDate} property.
     */
    private final MetaProperty<LocalDate> expirationDate = DirectMetaProperty.ofImmutable(
        this, "expirationDate", IborFutureOption.class, LocalDate.class);
    /**
     * The meta-property for the {@code expirationTime} property.
     */
    private final MetaProperty<LocalTime> expirationTime = DirectMetaProperty.ofImmutable(
        this, "expirationTime", IborFutureOption.class, LocalTime.class);
    /**
     * The meta-property for the {@code expirationZone} property.
     */
    private final MetaProperty<ZoneId> expirationZone = DirectMetaProperty.ofImmutable(
        this, "expirationZone", IborFutureOption.class, ZoneId.class);
    /**
     * The meta-property for the {@code premiumStyle} property.
     */
    private final MetaProperty<FutureOptionPremiumStyle> premiumStyle = DirectMetaProperty.ofImmutable(
        this, "premiumStyle", IborFutureOption.class, FutureOptionPremiumStyle.class);
    /**
     * The meta-property for the {@code rounding} property.
     */
    private final MetaProperty<Rounding> rounding = DirectMetaProperty.ofImmutable(
        this, "rounding", IborFutureOption.class, Rounding.class);
    /**
     * The meta-property for the {@code underlyingLink} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<SecurityLink<IborFuture>> underlyingLink = DirectMetaProperty.ofImmutable(
        this, "underlyingLink", IborFutureOption.class, (Class) SecurityLink.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "putCall",
        "strikePrice",
        "expirationDate",
        "expirationTime",
        "expirationZone",
        "premiumStyle",
        "rounding",
        "underlyingLink");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -219971059:  // putCall
          return putCall;
        case 50946231:  // strikePrice
          return strikePrice;
        case -668811523:  // expirationDate
          return expirationDate;
        case -668327396:  // expirationTime
          return expirationTime;
        case -668142853:  // expirationZone
          return expirationZone;
        case -1257652838:  // premiumStyle
          return premiumStyle;
        case -142444:  // rounding
          return rounding;
        case 1497199863:  // underlyingLink
          return underlyingLink;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public IborFutureOption.Builder builder() {
      return new IborFutureOption.Builder();
    }

    @Override
    public Class<? extends IborFutureOption> beanType() {
      return IborFutureOption.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code putCall} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<PutCall> putCall() {
      return putCall;
    }

    /**
     * The meta-property for the {@code strikePrice} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Double> strikePrice() {
      return strikePrice;
    }

    /**
     * The meta-property for the {@code expirationDate} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalDate> expirationDate() {
      return expirationDate;
    }

    /**
     * The meta-property for the {@code expirationTime} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalTime> expirationTime() {
      return expirationTime;
    }

    /**
     * The meta-property for the {@code expirationZone} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<ZoneId> expirationZone() {
      return expirationZone;
    }

    /**
     * The meta-property for the {@code premiumStyle} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<FutureOptionPremiumStyle> premiumStyle() {
      return premiumStyle;
    }

    /**
     * The meta-property for the {@code rounding} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Rounding> rounding() {
      return rounding;
    }

    /**
     * The meta-property for the {@code underlyingLink} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<SecurityLink<IborFuture>> underlyingLink() {
      return underlyingLink;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -219971059:  // putCall
          return ((IborFutureOption) bean).getPutCall();
        case 50946231:  // strikePrice
          return ((IborFutureOption) bean).getStrikePrice();
        case -668811523:  // expirationDate
          return ((IborFutureOption) bean).getExpirationDate();
        case -668327396:  // expirationTime
          return ((IborFutureOption) bean).getExpirationTime();
        case -668142853:  // expirationZone
          return ((IborFutureOption) bean).getExpirationZone();
        case -1257652838:  // premiumStyle
          return ((IborFutureOption) bean).getPremiumStyle();
        case -142444:  // rounding
          return ((IborFutureOption) bean).getRounding();
        case 1497199863:  // underlyingLink
          return ((IborFutureOption) bean).getUnderlyingLink();
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
   * The bean-builder for {@code IborFutureOption}.
   */
  public static class Builder extends DirectFieldsBeanBuilder<IborFutureOption> {

    private PutCall putCall;
    private double strikePrice;
    private LocalDate expirationDate;
    private LocalTime expirationTime;
    private ZoneId expirationZone;
    private FutureOptionPremiumStyle premiumStyle;
    private Rounding rounding;
    private SecurityLink<IborFuture> underlyingLink;

    /**
     * Restricted constructor.
     */
    protected Builder() {
      applyDefaults(this);
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    protected Builder(IborFutureOption beanToCopy) {
      this.putCall = beanToCopy.getPutCall();
      this.strikePrice = beanToCopy.getStrikePrice();
      this.expirationDate = beanToCopy.getExpirationDate();
      this.expirationTime = beanToCopy.getExpirationTime();
      this.expirationZone = beanToCopy.getExpirationZone();
      this.premiumStyle = beanToCopy.getPremiumStyle();
      this.rounding = beanToCopy.getRounding();
      this.underlyingLink = beanToCopy.getUnderlyingLink();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -219971059:  // putCall
          return putCall;
        case 50946231:  // strikePrice
          return strikePrice;
        case -668811523:  // expirationDate
          return expirationDate;
        case -668327396:  // expirationTime
          return expirationTime;
        case -668142853:  // expirationZone
          return expirationZone;
        case -1257652838:  // premiumStyle
          return premiumStyle;
        case -142444:  // rounding
          return rounding;
        case 1497199863:  // underlyingLink
          return underlyingLink;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -219971059:  // putCall
          this.putCall = (PutCall) newValue;
          break;
        case 50946231:  // strikePrice
          this.strikePrice = (Double) newValue;
          break;
        case -668811523:  // expirationDate
          this.expirationDate = (LocalDate) newValue;
          break;
        case -668327396:  // expirationTime
          this.expirationTime = (LocalTime) newValue;
          break;
        case -668142853:  // expirationZone
          this.expirationZone = (ZoneId) newValue;
          break;
        case -1257652838:  // premiumStyle
          this.premiumStyle = (FutureOptionPremiumStyle) newValue;
          break;
        case -142444:  // rounding
          this.rounding = (Rounding) newValue;
          break;
        case 1497199863:  // underlyingLink
          this.underlyingLink = (SecurityLink<IborFuture>) newValue;
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
    public IborFutureOption build() {
      return new IborFutureOption(this);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code putCall} property in the builder.
     * @param putCall  the new value
     * @return this, for chaining, not null
     */
    public Builder putCall(PutCall putCall) {
      this.putCall = putCall;
      return this;
    }

    /**
     * Sets the {@code strikePrice} property in the builder.
     * @param strikePrice  the new value
     * @return this, for chaining, not null
     */
    public Builder strikePrice(double strikePrice) {
      this.strikePrice = strikePrice;
      return this;
    }

    /**
     * Sets the {@code expirationDate} property in the builder.
     * @param expirationDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expirationDate(LocalDate expirationDate) {
      JodaBeanUtils.notNull(expirationDate, "expirationDate");
      this.expirationDate = expirationDate;
      return this;
    }

    /**
     * Sets the {@code expirationTime} property in the builder.
     * @param expirationTime  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expirationTime(LocalTime expirationTime) {
      JodaBeanUtils.notNull(expirationTime, "expirationTime");
      this.expirationTime = expirationTime;
      return this;
    }

    /**
     * Sets the {@code expirationZone} property in the builder.
     * @param expirationZone  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expirationZone(ZoneId expirationZone) {
      JodaBeanUtils.notNull(expirationZone, "expirationZone");
      this.expirationZone = expirationZone;
      return this;
    }

    /**
     * Sets the {@code premiumStyle} property in the builder.
     * @param premiumStyle  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder premiumStyle(FutureOptionPremiumStyle premiumStyle) {
      JodaBeanUtils.notNull(premiumStyle, "premiumStyle");
      this.premiumStyle = premiumStyle;
      return this;
    }

    /**
     * Sets the {@code rounding} property in the builder.
     * @param rounding  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder rounding(Rounding rounding) {
      JodaBeanUtils.notNull(rounding, "rounding");
      this.rounding = rounding;
      return this;
    }

    /**
     * Sets the {@code underlyingLink} property in the builder.
     * @param underlyingLink  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder underlyingLink(SecurityLink<IborFuture> underlyingLink) {
      JodaBeanUtils.notNull(underlyingLink, "underlyingLink");
      this.underlyingLink = underlyingLink;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(288);
      buf.append("IborFutureOption.Builder{");
      int len = buf.length();
      toString(buf);
      if (buf.length() > len) {
        buf.setLength(buf.length() - 2);
      }
      buf.append('}');
      return buf.toString();
    }

    protected void toString(StringBuilder buf) {
      buf.append("putCall").append('=').append(JodaBeanUtils.toString(putCall)).append(',').append(' ');
      buf.append("strikePrice").append('=').append(JodaBeanUtils.toString(strikePrice)).append(',').append(' ');
      buf.append("expirationDate").append('=').append(JodaBeanUtils.toString(expirationDate)).append(',').append(' ');
      buf.append("expirationTime").append('=').append(JodaBeanUtils.toString(expirationTime)).append(',').append(' ');
      buf.append("expirationZone").append('=').append(JodaBeanUtils.toString(expirationZone)).append(',').append(' ');
      buf.append("premiumStyle").append('=').append(JodaBeanUtils.toString(premiumStyle)).append(',').append(' ');
      buf.append("rounding").append('=').append(JodaBeanUtils.toString(rounding)).append(',').append(' ');
      buf.append("underlyingLink").append('=').append(JodaBeanUtils.toString(underlyingLink)).append(',').append(' ');
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
