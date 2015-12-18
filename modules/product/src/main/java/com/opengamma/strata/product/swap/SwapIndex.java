package com.opengamma.strata.product.swap;

import org.joda.convert.FromString;

import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.product.swap.type.FixedIborSwapTemplate;

public interface SwapIndex
    extends Index, Named {

  /**
   * Obtains a {@code SwapIndex} from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the index
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static SwapIndex of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of {@code SwapIndex} to be lookup up.
   * It also provides the complete set of available instances.
   * 
   * @return the extended enum helper
   */
  public static ExtendedEnum<SwapIndex> extendedEnum() {
    return SwapIndices.ENUM_LOOKUP;
  }

  //-----------------------------------------------------------------------

  /**
   * Gets template for creating Fixed-Ibor swap.
   * <p>
   * @return the template
   */
  public abstract FixedIborSwapTemplate getTemplate();

}
