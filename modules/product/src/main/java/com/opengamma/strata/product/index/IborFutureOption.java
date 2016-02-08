/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

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
import com.opengamma.strata.collect.id.LinkResolvable;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.Product;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityLink;
import com.opengamma.strata.product.common.FutureOptionPremiumStyle;

/**
 * A futures option contract, based on an Ibor index.
 * <p>
 * An Ibor future option is a financial instrument that provides an option based on the future value of
 * an Ibor index interest rate. The option is American, exercised at any point up to the exercise time.
 * It handles options with either daily margining or upfront premium.
 * <p>
 * An Ibor future option is also known as a <i>STIR future option</i> (Short Term Interest Rate).
 * This class represents the structure of a single option contract.
 */
@BeanDefinition
public class IborFutureOption
    implements Product, LinkResolvable<IborFutureOption>, ImmutableBean, Serializable {

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
   * The expiry date of the option.  
   * <p>
   * The expiry date is related to the expiry time and time-zone.
   * The date must not be after last trade date of the underlying future. 
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate expiryDate;
  /**
   * The expiry time of the option.  
   * <p>
   * The expiry time is related to the expiry date and time-zone.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalTime expiryTime;
  /**
   * The time-zone of the expiry time.  
   * <p>
   * The expiry time-zone is related to the expiry date and time.
   */
  @PropertyDefinition(validate = "notNull")
  private final ZoneId expiryZone;
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
      ArgChecker.inOrderOrEqual(expiryDate, lastTradeDate, "expiryDate", "lastTradeDate");
    }
  }

  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.rounding(Rounding.none());
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the expiry date-time.
   * <p>
   * The option expires at this date and time.
   * <p>
   * The result is returned by combining the expiry date, time and time-zone.
   * 
   * @return the expiry date and time
   */
  public ZonedDateTime getExpiry() {
    return expiryDate.atTime(expiryTime).atZone(expiryZone);
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
    JodaBeanUtils.notNull(builder.expiryDate, "expiryDate");
    JodaBeanUtils.notNull(builder.expiryTime, "expiryTime");
    JodaBeanUtils.notNull(builder.expiryZone, "expiryZone");
    JodaBeanUtils.notNull(builder.premiumStyle, "premiumStyle");
    JodaBeanUtils.notNull(builder.rounding, "rounding");
    JodaBeanUtils.notNull(builder.underlyingLink, "underlyingLink");
    this.putCall = builder.putCall;
    this.strikePrice = builder.strikePrice;
    this.expiryDate = builder.expiryDate;
    this.expiryTime = builder.expiryTime;
    this.expiryZone = builder.expiryZone;
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
   * Gets the expiry date of the option.
   * <p>
   * The expiry date is related to the expiry time and time-zone.
   * The date must not be after last trade date of the underlying future.
   * @return the value of the property, not null
   */
  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the expiry time of the option.
   * <p>
   * The expiry time is related to the expiry date and time-zone.
   * @return the value of the property, not null
   */
  public LocalTime getExpiryTime() {
    return expiryTime;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time-zone of the expiry time.
   * <p>
   * The expiry time-zone is related to the expiry date and time.
   * @return the value of the property, not null
   */
  public ZoneId getExpiryZone() {
    return expiryZone;
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
      return JodaBeanUtils.equal(putCall, other.putCall) &&
          JodaBeanUtils.equal(strikePrice, other.strikePrice) &&
          JodaBeanUtils.equal(expiryDate, other.expiryDate) &&
          JodaBeanUtils.equal(expiryTime, other.expiryTime) &&
          JodaBeanUtils.equal(expiryZone, other.expiryZone) &&
          JodaBeanUtils.equal(premiumStyle, other.premiumStyle) &&
          JodaBeanUtils.equal(rounding, other.rounding) &&
          JodaBeanUtils.equal(underlyingLink, other.underlyingLink);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(putCall);
    hash = hash * 31 + JodaBeanUtils.hashCode(strikePrice);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiryDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiryTime);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiryZone);
    hash = hash * 31 + JodaBeanUtils.hashCode(premiumStyle);
    hash = hash * 31 + JodaBeanUtils.hashCode(rounding);
    hash = hash * 31 + JodaBeanUtils.hashCode(underlyingLink);
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
    buf.append("putCall").append('=').append(JodaBeanUtils.toString(putCall)).append(',').append(' ');
    buf.append("strikePrice").append('=').append(JodaBeanUtils.toString(strikePrice)).append(',').append(' ');
    buf.append("expiryDate").append('=').append(JodaBeanUtils.toString(expiryDate)).append(',').append(' ');
    buf.append("expiryTime").append('=').append(JodaBeanUtils.toString(expiryTime)).append(',').append(' ');
    buf.append("expiryZone").append('=').append(JodaBeanUtils.toString(expiryZone)).append(',').append(' ');
    buf.append("premiumStyle").append('=').append(JodaBeanUtils.toString(premiumStyle)).append(',').append(' ');
    buf.append("rounding").append('=').append(JodaBeanUtils.toString(rounding)).append(',').append(' ');
    buf.append("underlyingLink").append('=').append(JodaBeanUtils.toString(underlyingLink)).append(',').append(' ');
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
     * The meta-property for the {@code expiryDate} property.
     */
    private final MetaProperty<LocalDate> expiryDate = DirectMetaProperty.ofImmutable(
        this, "expiryDate", IborFutureOption.class, LocalDate.class);
    /**
     * The meta-property for the {@code expiryTime} property.
     */
    private final MetaProperty<LocalTime> expiryTime = DirectMetaProperty.ofImmutable(
        this, "expiryTime", IborFutureOption.class, LocalTime.class);
    /**
     * The meta-property for the {@code expiryZone} property.
     */
    private final MetaProperty<ZoneId> expiryZone = DirectMetaProperty.ofImmutable(
        this, "expiryZone", IborFutureOption.class, ZoneId.class);
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
        "expiryDate",
        "expiryTime",
        "expiryZone",
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
        case -816738431:  // expiryDate
          return expiryDate;
        case -816254304:  // expiryTime
          return expiryTime;
        case -816069761:  // expiryZone
          return expiryZone;
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
     * The meta-property for the {@code expiryDate} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalDate> expiryDate() {
      return expiryDate;
    }

    /**
     * The meta-property for the {@code expiryTime} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalTime> expiryTime() {
      return expiryTime;
    }

    /**
     * The meta-property for the {@code expiryZone} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<ZoneId> expiryZone() {
      return expiryZone;
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
        case -816738431:  // expiryDate
          return ((IborFutureOption) bean).getExpiryDate();
        case -816254304:  // expiryTime
          return ((IborFutureOption) bean).getExpiryTime();
        case -816069761:  // expiryZone
          return ((IborFutureOption) bean).getExpiryZone();
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
    private LocalDate expiryDate;
    private LocalTime expiryTime;
    private ZoneId expiryZone;
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
      this.expiryDate = beanToCopy.getExpiryDate();
      this.expiryTime = beanToCopy.getExpiryTime();
      this.expiryZone = beanToCopy.getExpiryZone();
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
        case -816738431:  // expiryDate
          return expiryDate;
        case -816254304:  // expiryTime
          return expiryTime;
        case -816069761:  // expiryZone
          return expiryZone;
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
        case -816738431:  // expiryDate
          this.expiryDate = (LocalDate) newValue;
          break;
        case -816254304:  // expiryTime
          this.expiryTime = (LocalTime) newValue;
          break;
        case -816069761:  // expiryZone
          this.expiryZone = (ZoneId) newValue;
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
     * Sets whether the option is put or call.
     * <p>
     * A call gives the owner the right, but not obligation, to buy the underlying at
     * an agreed price in the future. A put gives a similar option to sell.
     * @param putCall  the new value
     * @return this, for chaining, not null
     */
    public Builder putCall(PutCall putCall) {
      this.putCall = putCall;
      return this;
    }

    /**
     * Sets the strike price, represented in decimal form.
     * <p>
     * This is the price at which the option applies and refers to the price of the underlying future.
     * This must be represented in decimal form, {@code (1.0 - decimalRate)}.
     * As such, the common market price of 99.3 for a 0.7% rate must be input as 0.993.
     * The rate implied by the strike can take negative values.
     * @param strikePrice  the new value
     * @return this, for chaining, not null
     */
    public Builder strikePrice(double strikePrice) {
      this.strikePrice = strikePrice;
      return this;
    }

    /**
     * Sets the expiry date of the option.
     * <p>
     * The expiry date is related to the expiry time and time-zone.
     * The date must not be after last trade date of the underlying future.
     * @param expiryDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expiryDate(LocalDate expiryDate) {
      JodaBeanUtils.notNull(expiryDate, "expiryDate");
      this.expiryDate = expiryDate;
      return this;
    }

    /**
     * Sets the expiry time of the option.
     * <p>
     * The expiry time is related to the expiry date and time-zone.
     * @param expiryTime  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expiryTime(LocalTime expiryTime) {
      JodaBeanUtils.notNull(expiryTime, "expiryTime");
      this.expiryTime = expiryTime;
      return this;
    }

    /**
     * Sets the time-zone of the expiry time.
     * <p>
     * The expiry time-zone is related to the expiry date and time.
     * @param expiryZone  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expiryZone(ZoneId expiryZone) {
      JodaBeanUtils.notNull(expiryZone, "expiryZone");
      this.expiryZone = expiryZone;
      return this;
    }

    /**
     * Sets the style of the option premium.
     * <p>
     * The two options are daily margining and upfront premium.
     * @param premiumStyle  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder premiumStyle(FutureOptionPremiumStyle premiumStyle) {
      JodaBeanUtils.notNull(premiumStyle, "premiumStyle");
      this.premiumStyle = premiumStyle;
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

    /**
     * Sets the link to the underlying future.
     * <p>
     * This property returns a link to the security via a {@link StandardId}.
     * See {@link #getUnderlying()} and {@link SecurityLink} for more details.
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
      buf.append("expiryDate").append('=').append(JodaBeanUtils.toString(expiryDate)).append(',').append(' ');
      buf.append("expiryTime").append('=').append(JodaBeanUtils.toString(expiryTime)).append(',').append(' ');
      buf.append("expiryZone").append('=').append(JodaBeanUtils.toString(expiryZone)).append(',').append(' ');
      buf.append("premiumStyle").append('=').append(JodaBeanUtils.toString(premiumStyle)).append(',').append(' ');
      buf.append("rounding").append('=').append(JodaBeanUtils.toString(rounding)).append(',').append(' ');
      buf.append("underlyingLink").append('=').append(JodaBeanUtils.toString(underlyingLink)).append(',').append(' ');
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
