package com.opengamma.sesame;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.ParameterIssuerProviderInterface;
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

public interface IssuerProviderFn {

  Result<Pair<ParameterIssuerProviderInterface, CurveBuildingBlockBundle>> createBundle(Environment env,
                                                                                        FinancialSecurity security,
                                                                                        Result<FXMatrix> fxMatrix);
}
