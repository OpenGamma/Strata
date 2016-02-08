/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.collect.id.LinkResolutionException;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.LinkResolvable;

/**
 * A trade that is directly based on a security.
 * <p>
 * A security trade is a {@link Trade} that contains a reference to a {@link Security}.
 * The security is held via a {@link SecurityLink}, which allows for the security to be
 * managed as reference data, separately from the trade.
 * <p>
 * Implementations of this interface must be immutable beans.
 * 
 * @param <P>  the type of the product
 */
public interface SecurityTrade<P extends Product>
    extends FinanceTrade, LinkResolvable<SecurityTrade<P>> {

  /**
   * Gets the link to the security that was traded.
   * <p>
   * A security link provides loose coupling between different parts of the object model.
   * For example, this allows the trade to be stored in a different database to the security.
   * <p>
   * A link can be in one of two states, resolvable and resolved.
   * In the resolvable state, the link contains the identifier and product type of the security.
   * In the resolved state, the link directly embeds the security.
   * <p>
   * When the link is in the resolvable state, the {@link #getSecurity()} AND {@link #getProduct()}
   * methods will throw an {@link IllegalStateException}.
   * <p>
   * To ensure that the link is resolved, call {@link #resolveLinks(LinkResolver)}.
   * 
   * @return the link to the security, which may be in either the resolvable or resolved state
   */
  public abstract SecurityLink<P> getSecurityLink();

  /**
   * Gets the security that was traded, throwing an exception if not resolved.
   * <p>
   * Returns the underlying security that was traded.
   * This is obtained from {@link #getSecurityLink()}.
   * <p>
   * The link has two states, resolvable and resolved.
   * The security can only be returned if the link is resolved.
   * If the link is in the resolvable state, this method will throw an {@link IllegalStateException}.
   * <p>
   * To ensure that the link is resolved, call {@link #resolveLinks(LinkResolver)}.
   * 
   * @return full details of the security
   * @throws IllegalStateException if the security link is not resolved
   */
  public default Security<P> getSecurity() {
    return getSecurityLink().resolvedTarget();
  }

  /**
   * Gets the underlying product that was agreed when the trade occurred, throwing an exception if not resolved.
   * <p>
   * Returns the underlying product that captures the contracted financial details of the trade.
   * This is obtained from {@link #getSecurityLink()}.
   * <p>
   * The link has two states, resolvable and resolved.
   * The product can only be returned if the link is resolved.
   * If the link is in the resolvable state, this method will throw an {@link IllegalStateException}.
   * <p>
   * To ensure that the link is resolved, call {@link #resolveLinks(LinkResolver)}.
   * 
   * @return the product
   * @throws IllegalStateException if the security link is not resolved
   */
  public default P getProduct() {
    return getSecurity().getProduct();
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a trade where all links have been resolved.
   * <p>
   * This method examines the trade, locates any links and resolves them.
   * The result is fully resolved with all data available for use.
   * Calling {@link #getSecurity()} or {@link #getProduct()} on the result will not throw an exception.
   * <p>
   * An exception is thrown if a link cannot be resolved.
   * 
   * @param resolver  the resolver to use
   * @return the fully resolved trade
   * @throws LinkResolutionException if a link cannot be resolved
   */
  @Override
  public abstract SecurityTrade<P> resolveLinks(LinkResolver resolver);

}
