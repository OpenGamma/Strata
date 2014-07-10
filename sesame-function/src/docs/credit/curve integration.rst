=================
Curve Integration
=================

There are two primary ways of integrating curves for credit pricing into
OpenGamma. Either via snapshots or via function integration. Each mechanism is
discussed in detail below along with its particular benefits.


Curve snapshots
===============

Snapshots are a good choice if you intend to store your credit data in
OpenGamma. A single snapshot instance holds a set of curves indexed in a map.
The curves hold basic market data inputs, term structure and any other
parameters required for bootstrapping.

Available snapshot types are:

* ``com.opengamma.financial.analytics.isda.credit.CreditCurveDataSnapshot`` 
* ``com.opengamma.financial.analytics.isda.credit.YieldCurveDataSnapshot``

TODO: add CDX snapshots

For more information on the different types of snapshot, see here (TODO - link
to credit snapshot docs)

Once a snapshot is created and/or persisted, it needs to be referenced in the
function graph. The two functions designed for this purpose are:

* ``com.opengamma.sesame.credit.snapshot.SnapshotCreditCurveDataProviderFn``
* ``com.opengamma.sesame.credit.snapshot.SnapshotYieldCurveDataProviderFn``

Each takes a snapshot link as its constructor parameter which will be resolved
when a curve is required.

Curve data provider functions
=============================

The alternative to constructing a snapshot is to implement the curve data
provider interface. This is a good choice for when curve data will be loaded
directly from an external data source.

Function interfaces which can be implemented are:

* ``com.opengamma.sesame.credit.snapshot.CreditCurveDataProviderFn``
* ``com.opengamma.sesame.credit.snapshot.YieldCurveDataProviderFn``

Each has a single method which takes the key for the relevant curve type. The
implementation should consult the relevant data source and return a result as
appropriate.