/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.observable;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;

import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.TypedMetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.data.ObservableSource;

/**
 * An identifier used to access a market quote.
 * <p>
 * A quote ID identifies a piece of data in an external data provider.
 * <p>
 * Where possible, applications should use higher level IDs, instead of this class.
 * Higher level market data keys allow the system to associate the market data with metadata when
 * applying scenario definitions. If quote IDs are used directly, the system has no way to
 * perturb the market data using higher level rules that rely on metadata.
 * <p>
 * The {@link StandardId} in a quote ID is typically the ID from an underlying data provider (e.g.
 * Bloomberg or Reuters). However the field name is a generic name which is mapped to the field name
 * in the underlying provider by the market data system.
 * <p>
 * The reason for this difference is the different sources of the ID and field name data. The ID is typically
 * taken from an object which is provided to any calculations, for example a security linked to the
 * trade. The calculation rarely has to make up an ID for an object it doesn't have access to.
 * <p>
 * In contrast, calculations will often have to reference field names that aren't part their arguments. For
 * example, if a calculation requires the last closing price of a security, it could take the ID from
 * the security, but it needs a way to specify the field name containing the last closing price.
 * <p>
 * If the field name were specific to the market data provider, the calculation would have to be aware
 * of the source of its market data. However, if it uses a generic field name from {@code FieldNames}
 * the market data source can change without affecting the calculation.
 *
 * @see FieldName
 */
@BeanDefinition(style = "light", cacheHashCode = true)
public final class QuoteId
    implements ObservableId, ImmutableBean, Serializable {

  /**
   * The identifier of the data.
   * This is typically an identifier from an external data provider.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final StandardId standardId;
  /**
   * The field name in the market data record that contains the market data item.
   * The most common field name is {@linkplain FieldName#MARKET_VALUE market value}.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final FieldName fieldName;
  /**
   * The source of observable market data.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ObservableSource observableSource;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance used to obtain an observable value.
   * <p>
   * The field name containing the data is {@link FieldName#MARKET_VALUE} and the market
   * data source is {@link ObservableSource#NONE}.
   *
   * @param standardId  the standard identifier of the data in the underlying data provider
   * @return the identifier
   */
  public static QuoteId of(StandardId standardId) {
    return new QuoteId(standardId, FieldName.MARKET_VALUE, ObservableSource.NONE);
  }

  /**
   * Obtains an instance used to obtain an observable value.
   * <p>
   * The market data source is {@link ObservableSource#NONE}.
   *
   * @param standardId  the standard identifier of the data in the underlying data provider
   * @param fieldName  the name of the field in the market data record holding the data
   * @return the identifier
   */
  public static QuoteId of(StandardId standardId, FieldName fieldName) {
    return new QuoteId(standardId, fieldName, ObservableSource.NONE);
  }

  /**
   * Obtains an instance used to obtain an observable value,
   * specifying the source of observable market data.
   *
   * @param standardId  the standard identifier of the data in the underlying data provider
   * @param fieldName  the name of the field in the market data record holding the data
   * @param obsSource  the source of observable market data
   * @return the identifier
   */
  public static QuoteId of(StandardId standardId, FieldName fieldName, ObservableSource obsSource) {
    return new QuoteId(standardId, fieldName, obsSource);
  }

  //-------------------------------------------------------------------------
  @Override
  public QuoteId withObservableSource(ObservableSource obsSource) {
    return new QuoteId(standardId, fieldName, obsSource);
  }

  @Override
  public String toString() {
    return new StringBuilder(32)
        .append("QuoteId:")
        .append(standardId)
        .append('/')
        .append(fieldName)
        .append(observableSource.equals(ObservableSource.NONE) ? "" : "/" + observableSource)
        .toString();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code QuoteId}.
   */
  private static final TypedMetaBean<QuoteId> META_BEAN =
      LightMetaBean.of(
          QuoteId.class,
          MethodHandles.lookup(),
          new String[] {
              "standardId",
              "fieldName",
              "observableSource"},
          new Object[0]);

  /**
   * The meta-bean for {@code QuoteId}.
   * @return the meta-bean, not null
   */
  public static TypedMetaBean<QuoteId> meta() {
    return META_BEAN;
  }

  static {
    MetaBean.register(META_BEAN);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * The cached hash code, using the racy single-check idiom.
   */
  private transient int cacheHashCode;

  private QuoteId(
      StandardId standardId,
      FieldName fieldName,
      ObservableSource observableSource) {
    JodaBeanUtils.notNull(standardId, "standardId");
    JodaBeanUtils.notNull(fieldName, "fieldName");
    JodaBeanUtils.notNull(observableSource, "observableSource");
    this.standardId = standardId;
    this.fieldName = fieldName;
    this.observableSource = observableSource;
  }

  @Override
  public TypedMetaBean<QuoteId> metaBean() {
    return META_BEAN;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the identifier of the data.
   * This is typically an identifier from an external data provider.
   * @return the value of the property, not null
   */
  @Override
  public StandardId getStandardId() {
    return standardId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the field name in the market data record that contains the market data item.
   * The most common field name is {@linkplain FieldName#MARKET_VALUE market value}.
   * @return the value of the property, not null
   */
  @Override
  public FieldName getFieldName() {
    return fieldName;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the source of observable market data.
   * @return the value of the property, not null
   */
  @Override
  public ObservableSource getObservableSource() {
    return observableSource;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      QuoteId other = (QuoteId) obj;
      return JodaBeanUtils.equal(standardId, other.standardId) &&
          JodaBeanUtils.equal(fieldName, other.fieldName) &&
          JodaBeanUtils.equal(observableSource, other.observableSource);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = cacheHashCode;
    if (hash == 0) {
      hash = getClass().hashCode();
      hash = hash * 31 + JodaBeanUtils.hashCode(standardId);
      hash = hash * 31 + JodaBeanUtils.hashCode(fieldName);
      hash = hash * 31 + JodaBeanUtils.hashCode(observableSource);
      cacheHashCode = hash;
    }
    return hash;
  }

  //-------------------------- AUTOGENERATED END --------------------------
}
