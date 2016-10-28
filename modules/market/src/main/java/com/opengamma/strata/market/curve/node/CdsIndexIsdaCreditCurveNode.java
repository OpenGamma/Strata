/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.node;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.Set;

import org.joda.beans.Bean;
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

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.market.curve.IsdaCreditCurveNode;
import com.opengamma.strata.market.param.DatedParameterMetadata;
import com.opengamma.strata.market.param.LabelDateParameterMetadata;
import com.opengamma.strata.market.param.TenorDateParameterMetadata;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.credit.Cds;
import com.opengamma.strata.product.credit.CdsIndex;
import com.opengamma.strata.product.credit.CdsIndexCalibrationTrade;
import com.opengamma.strata.product.credit.CdsIndexTrade;
import com.opengamma.strata.product.credit.CdsQuote;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.credit.type.CdsQuoteConvention;
import com.opengamma.strata.product.credit.type.CdsTemplate;
import com.opengamma.strata.product.credit.type.TenorCdsTemplate;

/**
 * An ISDA compliant curve node whose instrument is a CDS index.
 * <p>
 * The trade produced by the node will be a protection payer (BUY) for a positive quantity
 * and a protection receiver (SELL) for a negative quantity.
 */
@BeanDefinition
public final class CdsIndexIsdaCreditCurveNode
    implements IsdaCreditCurveNode, ImmutableBean, Serializable {

  /**
   * The template for the single names associated with this node.
   */
  @PropertyDefinition(validate = "notNull")
  private final CdsTemplate template;
  /**
   * The label to use for the node.
   * <p>
   * When building, this will default based on {@code template} if not specified.
   */
  @PropertyDefinition(validate = "notEmpty", overrideGet = true)
  private final String label;
  /**
   * The identifier of the market data value that provides the quoted value.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ObservableId observableId;
  /**
   * The CDS index identifier.
   * <p>
   * This identifier is used for referring this CDS index product.
   */
  @PropertyDefinition(validate = "notNull")
  private final StandardId cdsIndexId;
  /**
   * The legal entity identifiers.
   * <p>
   * This identifiers are used for the reference legal entities of the CDS index.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<StandardId> referenceEntityIds;
  /**
   * The market quote convention.
   * <p>
   * The CDS index is quoted in par spread, points upfront or quoted spread.
   * See {@link CdsQuoteConvention} for detail.
   */
  @PropertyDefinition(validate = "notNull")
  private final CdsQuoteConvention quoteConvention;
  /**
   * The fixed coupon rate.
   * <p>
   * This must be represented in decimal form.
   */
  @PropertyDefinition(get = "optional")
  private final Double fixedRate;

  //-------------------------------------------------------------------------
  @Override
  public LocalDate getNodeDate(LocalDate tradeDate, ReferenceData refData) {
    CdsTrade trade = template.createTrade(cdsIndexId, tradeDate, BuySell.BUY, 1, 1, refData);
    return trade.getProduct().resolve(refData).getProtectionEndDate();
  }

  @Override
  public DatedParameterMetadata metadata(LocalDate nodeDate) {
    return template instanceof TenorCdsTemplate
        ? TenorDateParameterMetadata.of(nodeDate, ((TenorCdsTemplate) template).getTenor(), label)
        : LabelDateParameterMetadata.of(nodeDate, label);
  }

  /**
   * Creates a trade representing the CDS index at the node.
   * <p>
   * This uses the observed market data to build the CDS index trade that the node represents.
   * The resulting trade is not resolved.
   * The notional of the trade is taken from the 'quantity' variable.
   * The quantity is signed and will affect whether the trade is Buy or Sell.
   * The valuation date is defined by the market data.
   *
   * @param quantity  the quantity or notional of the trade
   * @param marketData  the market data required to build a trade for the instrument, including the valuation date
   * @param refData  the reference data, used to resolve the trade dates
   * @return a trade representing the instrument at the node
   */
  public CdsIndexCalibrationTrade trade(double quantity, MarketData marketData, ReferenceData refData) {
    BuySell buySell = quantity > 0 ? BuySell.BUY : BuySell.SELL;
    LocalDate valuationDate = marketData.getValuationDate();
    double quoteValue = marketData.getValue(observableId);
    CdsQuote quote = CdsQuote.of(quoteConvention, quoteValue);
    double notional = Math.abs(quantity);
    CdsTrade cdsTrade = null;
    if (quoteConvention.equals(CdsQuoteConvention.PAR_SPREAD)) {
      cdsTrade = template.createTrade(cdsIndexId, valuationDate, buySell, notional, quoteValue, refData);
    } else {
      double coupon = getFixedRate().getAsDouble(); // always success
      cdsTrade = template.createTrade(cdsIndexId, valuationDate, buySell, notional, coupon, refData);
    }
    Cds cdsProduct = cdsTrade.getProduct();
    CdsIndexTrade cdsIndex = CdsIndexTrade.builder()
        .info(cdsTrade.getInfo())
        .product(CdsIndex.builder()
            .buySell(cdsProduct.getBuySell())
            .currency(cdsProduct.getCurrency())
            .notional(cdsProduct.getNotional())
            .cdsIndexId(cdsIndexId)
            .referenceEntityIds(referenceEntityIds)
            .dayCount(cdsProduct.getDayCount())
            .accrualSchedule(cdsProduct.getAccrualSchedule())
            .fixedRate(cdsProduct.getFixedRate())
            .paymentOnDefault(cdsProduct.getPaymentOnDefault())
            .protectionStart(cdsProduct.getProtectionStart())
            .settlementDateOffset(cdsProduct.getSettlementDateOffset())
            .stepinDateOffset(cdsProduct.getSettlementDateOffset())
            .build())
        .build();
    return CdsIndexCalibrationTrade.of(cdsIndex, quote);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CdsIndexIsdaCreditCurveNode}.
   * @return the meta-bean, not null
   */
  public static CdsIndexIsdaCreditCurveNode.Meta meta() {
    return CdsIndexIsdaCreditCurveNode.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CdsIndexIsdaCreditCurveNode.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static CdsIndexIsdaCreditCurveNode.Builder builder() {
    return new CdsIndexIsdaCreditCurveNode.Builder();
  }

  private CdsIndexIsdaCreditCurveNode(
      CdsTemplate template,
      String label,
      ObservableId observableId,
      StandardId cdsIndexId,
      List<StandardId> referenceEntityIds,
      CdsQuoteConvention quoteConvention,
      Double fixedRate) {
    JodaBeanUtils.notNull(template, "template");
    JodaBeanUtils.notEmpty(label, "label");
    JodaBeanUtils.notNull(observableId, "observableId");
    JodaBeanUtils.notNull(cdsIndexId, "cdsIndexId");
    JodaBeanUtils.notNull(referenceEntityIds, "referenceEntityIds");
    JodaBeanUtils.notNull(quoteConvention, "quoteConvention");
    this.template = template;
    this.label = label;
    this.observableId = observableId;
    this.cdsIndexId = cdsIndexId;
    this.referenceEntityIds = ImmutableList.copyOf(referenceEntityIds);
    this.quoteConvention = quoteConvention;
    this.fixedRate = fixedRate;
  }

  @Override
  public CdsIndexIsdaCreditCurveNode.Meta metaBean() {
    return CdsIndexIsdaCreditCurveNode.Meta.INSTANCE;
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
   * Gets the template for the single names associated with this node.
   * @return the value of the property, not null
   */
  public CdsTemplate getTemplate() {
    return template;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the label to use for the node.
   * <p>
   * When building, this will default based on {@code template} if not specified.
   * @return the value of the property, not empty
   */
  @Override
  public String getLabel() {
    return label;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the identifier of the market data value that provides the quoted value.
   * @return the value of the property, not null
   */
  @Override
  public ObservableId getObservableId() {
    return observableId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the CDS index identifier.
   * <p>
   * This identifier is used for referring this CDS index product.
   * @return the value of the property, not null
   */
  public StandardId getCdsIndexId() {
    return cdsIndexId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the legal entity identifiers.
   * <p>
   * This identifiers are used for the reference legal entities of the CDS index.
   * @return the value of the property, not null
   */
  public ImmutableList<StandardId> getReferenceEntityIds() {
    return referenceEntityIds;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market quote convention.
   * <p>
   * The CDS index is quoted in par spread, points upfront or quoted spread.
   * See {@link CdsQuoteConvention} for detail.
   * @return the value of the property, not null
   */
  public CdsQuoteConvention getQuoteConvention() {
    return quoteConvention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fixed coupon rate.
   * <p>
   * This must be represented in decimal form.
   * @return the optional value of the property, not null
   */
  public OptionalDouble getFixedRate() {
    return fixedRate != null ? OptionalDouble.of(fixedRate) : OptionalDouble.empty();
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
      CdsIndexIsdaCreditCurveNode other = (CdsIndexIsdaCreditCurveNode) obj;
      return JodaBeanUtils.equal(template, other.template) &&
          JodaBeanUtils.equal(label, other.label) &&
          JodaBeanUtils.equal(observableId, other.observableId) &&
          JodaBeanUtils.equal(cdsIndexId, other.cdsIndexId) &&
          JodaBeanUtils.equal(referenceEntityIds, other.referenceEntityIds) &&
          JodaBeanUtils.equal(quoteConvention, other.quoteConvention) &&
          JodaBeanUtils.equal(fixedRate, other.fixedRate);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(template);
    hash = hash * 31 + JodaBeanUtils.hashCode(label);
    hash = hash * 31 + JodaBeanUtils.hashCode(observableId);
    hash = hash * 31 + JodaBeanUtils.hashCode(cdsIndexId);
    hash = hash * 31 + JodaBeanUtils.hashCode(referenceEntityIds);
    hash = hash * 31 + JodaBeanUtils.hashCode(quoteConvention);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixedRate);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("CdsIndexIsdaCreditCurveNode{");
    buf.append("template").append('=').append(template).append(',').append(' ');
    buf.append("label").append('=').append(label).append(',').append(' ');
    buf.append("observableId").append('=').append(observableId).append(',').append(' ');
    buf.append("cdsIndexId").append('=').append(cdsIndexId).append(',').append(' ');
    buf.append("referenceEntityIds").append('=').append(referenceEntityIds).append(',').append(' ');
    buf.append("quoteConvention").append('=').append(quoteConvention).append(',').append(' ');
    buf.append("fixedRate").append('=').append(JodaBeanUtils.toString(fixedRate));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CdsIndexIsdaCreditCurveNode}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code template} property.
     */
    private final MetaProperty<CdsTemplate> template = DirectMetaProperty.ofImmutable(
        this, "template", CdsIndexIsdaCreditCurveNode.class, CdsTemplate.class);
    /**
     * The meta-property for the {@code label} property.
     */
    private final MetaProperty<String> label = DirectMetaProperty.ofImmutable(
        this, "label", CdsIndexIsdaCreditCurveNode.class, String.class);
    /**
     * The meta-property for the {@code observableId} property.
     */
    private final MetaProperty<ObservableId> observableId = DirectMetaProperty.ofImmutable(
        this, "observableId", CdsIndexIsdaCreditCurveNode.class, ObservableId.class);
    /**
     * The meta-property for the {@code cdsIndexId} property.
     */
    private final MetaProperty<StandardId> cdsIndexId = DirectMetaProperty.ofImmutable(
        this, "cdsIndexId", CdsIndexIsdaCreditCurveNode.class, StandardId.class);
    /**
     * The meta-property for the {@code referenceEntityIds} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<StandardId>> referenceEntityIds = DirectMetaProperty.ofImmutable(
        this, "referenceEntityIds", CdsIndexIsdaCreditCurveNode.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code quoteConvention} property.
     */
    private final MetaProperty<CdsQuoteConvention> quoteConvention = DirectMetaProperty.ofImmutable(
        this, "quoteConvention", CdsIndexIsdaCreditCurveNode.class, CdsQuoteConvention.class);
    /**
     * The meta-property for the {@code fixedRate} property.
     */
    private final MetaProperty<Double> fixedRate = DirectMetaProperty.ofImmutable(
        this, "fixedRate", CdsIndexIsdaCreditCurveNode.class, Double.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "template",
        "label",
        "observableId",
        "cdsIndexId",
        "referenceEntityIds",
        "quoteConvention",
        "fixedRate");

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
        case 102727412:  // label
          return label;
        case -518800962:  // observableId
          return observableId;
        case -464117509:  // cdsIndexId
          return cdsIndexId;
        case -315789110:  // referenceEntityIds
          return referenceEntityIds;
        case 2049149709:  // quoteConvention
          return quoteConvention;
        case 747425396:  // fixedRate
          return fixedRate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public CdsIndexIsdaCreditCurveNode.Builder builder() {
      return new CdsIndexIsdaCreditCurveNode.Builder();
    }

    @Override
    public Class<? extends CdsIndexIsdaCreditCurveNode> beanType() {
      return CdsIndexIsdaCreditCurveNode.class;
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
    public MetaProperty<CdsTemplate> template() {
      return template;
    }

    /**
     * The meta-property for the {@code label} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> label() {
      return label;
    }

    /**
     * The meta-property for the {@code observableId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ObservableId> observableId() {
      return observableId;
    }

    /**
     * The meta-property for the {@code cdsIndexId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StandardId> cdsIndexId() {
      return cdsIndexId;
    }

    /**
     * The meta-property for the {@code referenceEntityIds} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<StandardId>> referenceEntityIds() {
      return referenceEntityIds;
    }

    /**
     * The meta-property for the {@code quoteConvention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CdsQuoteConvention> quoteConvention() {
      return quoteConvention;
    }

    /**
     * The meta-property for the {@code fixedRate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> fixedRate() {
      return fixedRate;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          return ((CdsIndexIsdaCreditCurveNode) bean).getTemplate();
        case 102727412:  // label
          return ((CdsIndexIsdaCreditCurveNode) bean).getLabel();
        case -518800962:  // observableId
          return ((CdsIndexIsdaCreditCurveNode) bean).getObservableId();
        case -464117509:  // cdsIndexId
          return ((CdsIndexIsdaCreditCurveNode) bean).getCdsIndexId();
        case -315789110:  // referenceEntityIds
          return ((CdsIndexIsdaCreditCurveNode) bean).getReferenceEntityIds();
        case 2049149709:  // quoteConvention
          return ((CdsIndexIsdaCreditCurveNode) bean).getQuoteConvention();
        case 747425396:  // fixedRate
          return ((CdsIndexIsdaCreditCurveNode) bean).fixedRate;
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
   * The bean-builder for {@code CdsIndexIsdaCreditCurveNode}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<CdsIndexIsdaCreditCurveNode> {

    private CdsTemplate template;
    private String label;
    private ObservableId observableId;
    private StandardId cdsIndexId;
    private List<StandardId> referenceEntityIds = ImmutableList.of();
    private CdsQuoteConvention quoteConvention;
    private Double fixedRate;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(CdsIndexIsdaCreditCurveNode beanToCopy) {
      this.template = beanToCopy.getTemplate();
      this.label = beanToCopy.getLabel();
      this.observableId = beanToCopy.getObservableId();
      this.cdsIndexId = beanToCopy.getCdsIndexId();
      this.referenceEntityIds = beanToCopy.getReferenceEntityIds();
      this.quoteConvention = beanToCopy.getQuoteConvention();
      this.fixedRate = beanToCopy.fixedRate;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          return template;
        case 102727412:  // label
          return label;
        case -518800962:  // observableId
          return observableId;
        case -464117509:  // cdsIndexId
          return cdsIndexId;
        case -315789110:  // referenceEntityIds
          return referenceEntityIds;
        case 2049149709:  // quoteConvention
          return quoteConvention;
        case 747425396:  // fixedRate
          return fixedRate;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          this.template = (CdsTemplate) newValue;
          break;
        case 102727412:  // label
          this.label = (String) newValue;
          break;
        case -518800962:  // observableId
          this.observableId = (ObservableId) newValue;
          break;
        case -464117509:  // cdsIndexId
          this.cdsIndexId = (StandardId) newValue;
          break;
        case -315789110:  // referenceEntityIds
          this.referenceEntityIds = (List<StandardId>) newValue;
          break;
        case 2049149709:  // quoteConvention
          this.quoteConvention = (CdsQuoteConvention) newValue;
          break;
        case 747425396:  // fixedRate
          this.fixedRate = (Double) newValue;
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
    public CdsIndexIsdaCreditCurveNode build() {
      return new CdsIndexIsdaCreditCurveNode(
          template,
          label,
          observableId,
          cdsIndexId,
          referenceEntityIds,
          quoteConvention,
          fixedRate);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the template for the single names associated with this node.
     * @param template  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder template(CdsTemplate template) {
      JodaBeanUtils.notNull(template, "template");
      this.template = template;
      return this;
    }

    /**
     * Sets the label to use for the node.
     * <p>
     * When building, this will default based on {@code template} if not specified.
     * @param label  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder label(String label) {
      JodaBeanUtils.notEmpty(label, "label");
      this.label = label;
      return this;
    }

    /**
     * Sets the identifier of the market data value that provides the quoted value.
     * @param observableId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder observableId(ObservableId observableId) {
      JodaBeanUtils.notNull(observableId, "observableId");
      this.observableId = observableId;
      return this;
    }

    /**
     * Sets the CDS index identifier.
     * <p>
     * This identifier is used for referring this CDS index product.
     * @param cdsIndexId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder cdsIndexId(StandardId cdsIndexId) {
      JodaBeanUtils.notNull(cdsIndexId, "cdsIndexId");
      this.cdsIndexId = cdsIndexId;
      return this;
    }

    /**
     * Sets the legal entity identifiers.
     * <p>
     * This identifiers are used for the reference legal entities of the CDS index.
     * @param referenceEntityIds  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder referenceEntityIds(List<StandardId> referenceEntityIds) {
      JodaBeanUtils.notNull(referenceEntityIds, "referenceEntityIds");
      this.referenceEntityIds = referenceEntityIds;
      return this;
    }

    /**
     * Sets the {@code referenceEntityIds} property in the builder
     * from an array of objects.
     * @param referenceEntityIds  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder referenceEntityIds(StandardId... referenceEntityIds) {
      return referenceEntityIds(ImmutableList.copyOf(referenceEntityIds));
    }

    /**
     * Sets the market quote convention.
     * <p>
     * The CDS index is quoted in par spread, points upfront or quoted spread.
     * See {@link CdsQuoteConvention} for detail.
     * @param quoteConvention  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder quoteConvention(CdsQuoteConvention quoteConvention) {
      JodaBeanUtils.notNull(quoteConvention, "quoteConvention");
      this.quoteConvention = quoteConvention;
      return this;
    }

    /**
     * Sets the fixed coupon rate.
     * <p>
     * This must be represented in decimal form.
     * @param fixedRate  the new value
     * @return this, for chaining, not null
     */
    public Builder fixedRate(Double fixedRate) {
      this.fixedRate = fixedRate;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("CdsIndexIsdaCreditCurveNode.Builder{");
      buf.append("template").append('=').append(JodaBeanUtils.toString(template)).append(',').append(' ');
      buf.append("label").append('=').append(JodaBeanUtils.toString(label)).append(',').append(' ');
      buf.append("observableId").append('=').append(JodaBeanUtils.toString(observableId)).append(',').append(' ');
      buf.append("cdsIndexId").append('=').append(JodaBeanUtils.toString(cdsIndexId)).append(',').append(' ');
      buf.append("referenceEntityIds").append('=').append(JodaBeanUtils.toString(referenceEntityIds)).append(',').append(' ');
      buf.append("quoteConvention").append('=').append(JodaBeanUtils.toString(quoteConvention)).append(',').append(' ');
      buf.append("fixedRate").append('=').append(JodaBeanUtils.toString(fixedRate));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
