/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trade;

import java.math.BigDecimal;
import java.util.Map;

import org.threeten.bp.LocalDate;
import org.threeten.bp.OffsetTime;

import com.opengamma.core.position.Counterparty;
import com.opengamma.core.position.Trade;
import com.opengamma.core.security.Security;
import com.opengamma.core.security.SecurityLink;
import com.opengamma.id.UniqueId;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;

/**
 * Base class that wraps a trade object.
 * @param <S> instance of Security
 */
public abstract class TradeWrapper<S extends Security> implements Trade {
  
  private final Trade _trade;
  
  private final Class<S> _clazz;
  
  /**
   * Base trade wrapper constructor that wraps a trade in an explicit instrument type.
   * @param trade the trade containing the instrument, not null.
   * @param clazz the type of instrument, not null.
   */
  public TradeWrapper(Trade trade, Class<S> clazz) {
    _trade = ArgumentChecker.notNull(trade, "trade");
    _clazz = ArgumentChecker.notNull(clazz, "clazz");
    ArgumentChecker.isTrue(trade.getSecurity().getClass().isAssignableFrom(clazz), trade.getSecurity() + " is not a " + clazz);
  }

  @Override
  public UniqueId getUniqueId() {
    return _trade.getUniqueId();
  }

  @Override
  public BigDecimal getQuantity() {
    return _trade.getQuantity();
  }

  @Override
  public SecurityLink getSecurityLink() {
    return _trade.getSecurityLink();
  }

  @Override
  public S getSecurity() {
    return _clazz.cast(_trade.getSecurity());
  }

  @Override
  public Map<String, String> getAttributes() {
    return _trade.getAttributes();
  }

  @Override
  public void setAttributes(Map<String, String> attributes) {
    _trade.setAttributes(attributes);
  }

  @Override
  public void addAttribute(String key, String value) {
    _trade.addAttribute(key, value);
  }

  @Override
  public Counterparty getCounterparty() {
    return _trade.getCounterparty();
  }

  @Override
  public LocalDate getTradeDate() {
    return _trade.getTradeDate();
  }

  @Override
  public OffsetTime getTradeTime() {
    return _trade.getTradeTime();
  }

  @Override
  public Double getPremium() {
    return _trade.getPremium();
  }

  @Override
  public Currency getPremiumCurrency() {
    return _trade.getPremiumCurrency();
  }

  @Override
  public LocalDate getPremiumDate() {
    return _trade.getPremiumDate();
  }

  @Override
  public OffsetTime getPremiumTime() {
    return _trade.getPremiumTime();
  }

}
