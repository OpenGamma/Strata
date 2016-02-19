/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.node;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.ImmutablePreBuild;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.DatedCurveParameterMetadata;
import com.opengamma.strata.market.curve.meta.SimpleCurveNodeMetadata;
import com.opengamma.strata.market.curve.meta.TenorCurveNodeMetadata;
import com.opengamma.strata.product.deposit.ExpandedIborFixingDeposit;
import com.opengamma.strata.product.deposit.IborFixingDepositTrade;
import com.opengamma.strata.product.deposit.type.IborFixingDepositTemplate;

/**
 * A curve node whose instrument is an Ibor fixing deposit.
 */
@BeanDefinition
public final class IborFixingDepositCurveNode
    implements CurveNode, ImmutableBean, Serializable {

  /**
   * The template for the Ibor fixing deposit associated with this node.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborFixingDepositTemplate template;
  /**
   * The key identifying the market data value which provides the rate.
   */
  @PropertyDefinition(validate = "notNull")
  private final ObservableKey rateKey;
  /**
   * The additional spread added to the rate.
   */
  @PropertyDefinition
  private final double additionalSpread;
  /**
   * The label to use for the node, defaulted.
   * <p>
   * When building, this will default based on the deposit period if not specified.
   */
  @PropertyDefinition(validate = "notEmpty", overrideGet = true)
  private final String label;
  /**
   * The method by which the date of the node is calculated, defaulted to 'End'.
   */
  @PropertyDefinition
  private final CurveNodeDate date;

  //-------------------------------------------------------------------------
  /**
   * Returns a curve node for an Ibor deposit using the specified template and rate key.
   * <p>
   * A suitable default label will be created.
   *
   * @param template  the template used for building the instrument for the node
   * @param rateKey  the key identifying the market rate used when building the instrument for the node
   * @return a node whose instrument is built from the template using a market rate
   */
  public static IborFixingDepositCurveNode of(IborFixingDepositTemplate template, ObservableKey rateKey) {
    return of(template, rateKey, 0d);
  }

  /**
   * Returns a curve node for an Ibor deposit using the specified template, rate key and spread.
   * <p>
   * A suitable default label will be created.
   *
   * @param template  the template defining the node instrument
   * @param rateKey  the key identifying the market data providing the rate for the node instrument
   * @param additionalSpread  the additional spread amount added to the rate
   * @return a node whose instrument is built from the template using a market rate
   */
  public static IborFixingDepositCurveNode of(
      IborFixingDepositTemplate template,
      ObservableKey rateKey,
      double additionalSpread) {

    return builder()
        .template(template)
        .rateKey(rateKey)
        .additionalSpread(additionalSpread)
        .build();
  }

  /**
   * Returns a curve node for an Ibor deposit using the specified template, rate key, spread and label.
   *
   * @param template  the template defining the node instrument
   * @param rateKey  the key identifying the market data providing the rate for the node instrument
   * @param additionalSpread  the additional spread amount added to the rate
   * @param label  the label to use for the node, if null or empty an appropriate default label will be used
   * @return a node whose instrument is built from the template using a market rate
   */
  public static IborFixingDepositCurveNode of(
      IborFixingDepositTemplate template,
      ObservableKey rateKey,
      double additionalSpread,
      String label) {

    return new IborFixingDepositCurveNode(template, rateKey, additionalSpread, label, CurveNodeDate.END);
  }

  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.date = CurveNodeDate.END;
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.label == null && builder.template != null) {
      builder.label = Tenor.of(builder.template.getDepositPeriod()).toString();
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<ObservableKey> requirements() {
    return ImmutableSet.of(rateKey);
  }

  @Override
  public DatedCurveParameterMetadata metadata(LocalDate valuationDate) {
    LocalDate nodeDate = date.calculate(
        () -> calculateEnd(valuationDate),
        () -> calculateLastFixingDate(valuationDate));
    if (date.isFixed()) {
      return SimpleCurveNodeMetadata.of(nodeDate, label);
    }
    Tenor tenor = Tenor.of(template.getDepositPeriod());
    return TenorCurveNodeMetadata.of(nodeDate, tenor, label);
  }

  // calculate the end date
  private LocalDate calculateEnd(LocalDate valuationDate) {
    ExpandedIborFixingDeposit deposit = template.createTrade(valuationDate, BuySell.BUY, 0d, 0d).getProduct().expand();
    return deposit.getEndDate();
  }

  // calculate the last fixing date
  private LocalDate calculateLastFixingDate(LocalDate valuationDate) {
    ExpandedIborFixingDeposit deposit = template.createTrade(valuationDate, BuySell.BUY, 0d, 0d).getProduct().expand();
    return deposit.getFloatingRate().getFixingDate();
  }

  @Override
  public IborFixingDepositTrade trade(LocalDate valuationDate, MarketData marketData) {
    double fixedRate = marketData.getValue(rateKey) + additionalSpread;
    return template.createTrade(valuationDate, BuySell.BUY, 1d, fixedRate);
  }

  @Override
  public double initialGuess(LocalDate valuationDate, MarketData marketData, ValueType valueType) {
    if (ValueType.ZERO_RATE.equals(valueType) || ValueType.FORWARD_RATE.equals(valueType)) {
      return marketData.getValue(rateKey);
    }
    if (ValueType.DISCOUNT_FACTOR.equals(valueType)) {
      double approximateMaturity = template.getDepositPeriod().toTotalMonths() / 12.0d;
      return Math.exp(-approximateMaturity * marketData.getValue(rateKey));
    }
    return 0d;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this node with the specified date.
   * 
   * @param date  the date to use
   * @return the node based on this node with the specified date
   */
  public IborFixingDepositCurveNode withDate(CurveNodeDate date) {
    return new IborFixingDepositCurveNode(template, rateKey, additionalSpread, label, date);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborFixingDepositCurveNode}.
   * @return the meta-bean, not null
   */
  public static IborFixingDepositCurveNode.Meta meta() {
    return IborFixingDepositCurveNode.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IborFixingDepositCurveNode.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static IborFixingDepositCurveNode.Builder builder() {
    return new IborFixingDepositCurveNode.Builder();
  }

  private IborFixingDepositCurveNode(
      IborFixingDepositTemplate template,
      ObservableKey rateKey,
      double additionalSpread,
      String label,
      CurveNodeDate date) {
    JodaBeanUtils.notNull(template, "template");
    JodaBeanUtils.notNull(rateKey, "rateKey");
    JodaBeanUtils.notEmpty(label, "label");
    this.template = template;
    this.rateKey = rateKey;
    this.additionalSpread = additionalSpread;
    this.label = label;
    this.date = date;
  }

  @Override
  public IborFixingDepositCurveNode.Meta metaBean() {
    return IborFixingDepositCurveNode.Meta.INSTANCE;
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
   * Gets the template for the Ibor fixing deposit associated with this node.
   * @return the value of the property, not null
   */
  public IborFixingDepositTemplate getTemplate() {
    return template;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the key identifying the market data value which provides the rate.
   * @return the value of the property, not null
   */
  public ObservableKey getRateKey() {
    return rateKey;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the additional spread added to the rate.
   * @return the value of the property
   */
  public double getAdditionalSpread() {
    return additionalSpread;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the label to use for the node, defaulted.
   * <p>
   * When building, this will default based on the deposit period if not specified.
   * @return the value of the property, not empty
   */
  @Override
  public String getLabel() {
    return label;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the method by which the date of the node is calculated, defaulted to 'End'.
   * @return the value of the property
   */
  public CurveNodeDate getDate() {
    return date;
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
      IborFixingDepositCurveNode other = (IborFixingDepositCurveNode) obj;
      return JodaBeanUtils.equal(template, other.template) &&
          JodaBeanUtils.equal(rateKey, other.rateKey) &&
          JodaBeanUtils.equal(additionalSpread, other.additionalSpread) &&
          JodaBeanUtils.equal(label, other.label) &&
          JodaBeanUtils.equal(date, other.date);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(template);
    hash = hash * 31 + JodaBeanUtils.hashCode(rateKey);
    hash = hash * 31 + JodaBeanUtils.hashCode(additionalSpread);
    hash = hash * 31 + JodaBeanUtils.hashCode(label);
    hash = hash * 31 + JodaBeanUtils.hashCode(date);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("IborFixingDepositCurveNode{");
    buf.append("template").append('=').append(template).append(',').append(' ');
    buf.append("rateKey").append('=').append(rateKey).append(',').append(' ');
    buf.append("additionalSpread").append('=').append(additionalSpread).append(',').append(' ');
    buf.append("label").append('=').append(label).append(',').append(' ');
    buf.append("date").append('=').append(JodaBeanUtils.toString(date));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborFixingDepositCurveNode}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code template} property.
     */
    private final MetaProperty<IborFixingDepositTemplate> template = DirectMetaProperty.ofImmutable(
        this, "template", IborFixingDepositCurveNode.class, IborFixingDepositTemplate.class);
    /**
     * The meta-property for the {@code rateKey} property.
     */
    private final MetaProperty<ObservableKey> rateKey = DirectMetaProperty.ofImmutable(
        this, "rateKey", IborFixingDepositCurveNode.class, ObservableKey.class);
    /**
     * The meta-property for the {@code additionalSpread} property.
     */
    private final MetaProperty<Double> additionalSpread = DirectMetaProperty.ofImmutable(
        this, "additionalSpread", IborFixingDepositCurveNode.class, Double.TYPE);
    /**
     * The meta-property for the {@code label} property.
     */
    private final MetaProperty<String> label = DirectMetaProperty.ofImmutable(
        this, "label", IborFixingDepositCurveNode.class, String.class);
    /**
     * The meta-property for the {@code date} property.
     */
    private final MetaProperty<CurveNodeDate> date = DirectMetaProperty.ofImmutable(
        this, "date", IborFixingDepositCurveNode.class, CurveNodeDate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "template",
        "rateKey",
        "additionalSpread",
        "label",
        "date");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          return template;
        case 983444831:  // rateKey
          return rateKey;
        case 291232890:  // additionalSpread
          return additionalSpread;
        case 102727412:  // label
          return label;
        case 3076014:  // date
          return date;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public IborFixingDepositCurveNode.Builder builder() {
      return new IborFixingDepositCurveNode.Builder();
    }

    @Override
    public Class<? extends IborFixingDepositCurveNode> beanType() {
      return IborFixingDepositCurveNode.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code template} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborFixingDepositTemplate> template() {
      return template;
    }

    /**
     * The meta-property for the {@code rateKey} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ObservableKey> rateKey() {
      return rateKey;
    }

    /**
     * The meta-property for the {@code additionalSpread} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> additionalSpread() {
      return additionalSpread;
    }

    /**
     * The meta-property for the {@code label} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> label() {
      return label;
    }

    /**
     * The meta-property for the {@code date} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveNodeDate> date() {
      return date;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          return ((IborFixingDepositCurveNode) bean).getTemplate();
        case 983444831:  // rateKey
          return ((IborFixingDepositCurveNode) bean).getRateKey();
        case 291232890:  // additionalSpread
          return ((IborFixingDepositCurveNode) bean).getAdditionalSpread();
        case 102727412:  // label
          return ((IborFixingDepositCurveNode) bean).getLabel();
        case 3076014:  // date
          return ((IborFixingDepositCurveNode) bean).getDate();
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
   * The bean-builder for {@code IborFixingDepositCurveNode}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<IborFixingDepositCurveNode> {

    private IborFixingDepositTemplate template;
    private ObservableKey rateKey;
    private double additionalSpread;
    private String label;
    private CurveNodeDate date;

    /**
     * Restricted constructor.
     */
    private Builder() {
      applyDefaults(this);
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(IborFixingDepositCurveNode beanToCopy) {
      this.template = beanToCopy.getTemplate();
      this.rateKey = beanToCopy.getRateKey();
      this.additionalSpread = beanToCopy.getAdditionalSpread();
      this.label = beanToCopy.getLabel();
      this.date = beanToCopy.getDate();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          return template;
        case 983444831:  // rateKey
          return rateKey;
        case 291232890:  // additionalSpread
          return additionalSpread;
        case 102727412:  // label
          return label;
        case 3076014:  // date
          return date;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          this.template = (IborFixingDepositTemplate) newValue;
          break;
        case 983444831:  // rateKey
          this.rateKey = (ObservableKey) newValue;
          break;
        case 291232890:  // additionalSpread
          this.additionalSpread = (Double) newValue;
          break;
        case 102727412:  // label
          this.label = (String) newValue;
          break;
        case 3076014:  // date
          this.date = (CurveNodeDate) newValue;
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
    public IborFixingDepositCurveNode build() {
      preBuild(this);
      return new IborFixingDepositCurveNode(
          template,
          rateKey,
          additionalSpread,
          label,
          date);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the template for the Ibor fixing deposit associated with this node.
     * @param template  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder template(IborFixingDepositTemplate template) {
      JodaBeanUtils.notNull(template, "template");
      this.template = template;
      return this;
    }

    /**
     * Sets the key identifying the market data value which provides the rate.
     * @param rateKey  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder rateKey(ObservableKey rateKey) {
      JodaBeanUtils.notNull(rateKey, "rateKey");
      this.rateKey = rateKey;
      return this;
    }

    /**
     * Sets the additional spread added to the rate.
     * @param additionalSpread  the new value
     * @return this, for chaining, not null
     */
    public Builder additionalSpread(double additionalSpread) {
      this.additionalSpread = additionalSpread;
      return this;
    }

    /**
     * Sets the label to use for the node, defaulted.
     * <p>
     * When building, this will default based on the deposit period if not specified.
     * @param label  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder label(String label) {
      JodaBeanUtils.notEmpty(label, "label");
      this.label = label;
      return this;
    }

    /**
     * Sets the method by which the date of the node is calculated, defaulted to 'End'.
     * @param date  the new value
     * @return this, for chaining, not null
     */
    public Builder date(CurveNodeDate date) {
      this.date = date;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("IborFixingDepositCurveNode.Builder{");
      buf.append("template").append('=').append(JodaBeanUtils.toString(template)).append(',').append(' ');
      buf.append("rateKey").append('=').append(JodaBeanUtils.toString(rateKey)).append(',').append(' ');
      buf.append("additionalSpread").append('=').append(JodaBeanUtils.toString(additionalSpread)).append(',').append(' ');
      buf.append("label").append('=').append(JodaBeanUtils.toString(label)).append(',').append(' ');
      buf.append("date").append('=').append(JodaBeanUtils.toString(date));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
