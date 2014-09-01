======================
Bond Model Integration
======================

Analytic results are are provided via the ``BondCalculator``

Multicurve
==========

The ``DiscountingBondCalculator`` is the discounting calculator implementation for ``BondCalculator``.

*Creation of the calculator is dependent on the*:

* ``IssuerProviderFn`` -  to provide the multicurve bundle for curves by issuer.
* ``BondAndBondFutureTradeConverter`` - to convert the trade/security into the analytics object.
* ``CurveDefinitionFn`` - to obtain cure definitions by curve name.
* ``Environment`` - to provide the valuation time and market data
* ``BondTrade`` - containing the ``BondSecurity`` or the subtypes ``MunicipalBondSecurity``, ``InflationBondSecurity``, ``GovernmentBondSecurity`` or ``CorporateBondSecurity``
* ``MarketDataFn`` - to provide the market price

Outputs
-------

* Present value, in the form of a ``MultipleCurrencyAmount``
* PV01, in the form of a ``ReferenceAmount``
* Bucketed PV01, in the form of ``BucketedCurveSensitivities``
* Z-Spread, in the form of a ``Double``
* Market Clean Price, in the form of a ``Double``
* Yield To Maturity, in the form of a ``Double``
