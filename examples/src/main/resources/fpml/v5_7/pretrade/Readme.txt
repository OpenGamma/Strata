Copyright © 2014 International Swaps and Derivatives Association. All rights reserved.
--------------------------------------------------------------------------------------

FpML 5.x - Readme file
===========================================

There are two sets of documentation for each view published in FpML:
* confirmation - contains the confirmation view of FpML
* reporting - contains the reporting view of FpML
* recordkeeping - contains the recordkeeping view of FpML
* transparency- contains the transparency view of FpMLFpML
* pretrade - contains the pretrade view of FpML
* legal - contains the legal view of FpML

For each of these views, there are schema files and example sub-directories.

The schema files are labelled fpml-XXX-MajorVersion-MinorVersion.xsd (e.g. fpml-main-5-6.xsd), for better readability the version number is omitted from the schema names below.

Confirmation Schemas:


* example-extension.xsd - File containing an example of an extension from the 5.x confirmation view schemas.
* fpml-asset.xsd - Underlyer definitions plus some types used by them (e.g. ones relating to commissions or dividend payouts).
* fpml-bond-option.xsd - Bond and Convertible Bond Options Product Definitions.
* fpml-business-events.xsd - Content of the Business events components
* fpml-cd.xsd - Credit derivative product definitions.
* fpml-clearing-processes.xsd - Clearing processes messages.
* fpml-com.xsd - Commodity product definitions.
* fpml-confirmation-processes.xsd - Confirmation process messages.
* fpml-correlation-swap.xsd - Correlation Swap Product Definitions.
* fpml-dividend-swaps.xsd - Dividend Swap Product Definitions.
* fpml-doc.xsd - Trade and contract definitions and definitions relating to validation.
* fpml-enum.xsd - Shared enumeration definitions. These definitions list the values that enumerated types may take.
* fpml-eqd.xsd - Equity Option and Equity Forward Product Definitions.
* fpml-eq-shared.xsd - Definitions shared by types with Equity Underlyers.
* fpml-fx.xsd - Foreign Exchange Product Definitions.
* fpml-generic-5-3 - Used in Transparency reporting to define a product that represents an OTC derivative transaction whose economics are not fully described using an FpML schema. In other views, generic products are present for convenience to support internal messaging and workflows that are cross-product. Generic products are not full trade representations as such they are not intended to be used for confirming trades.
* fpml-ird.xsd - Interest rate derivative product definitions.
* fpml-main.xsd - Root definitions.
* fpml-mktenv.xsd – Definitions of market environment data structures such as yield curves, volatility matrices, and the like.
* fpml-msg.xsd - High level definitions related to messaging.
* fpml-option-shared.xsd - Shared option definitions used for defining the common features of options.
* fpml-return-swaps.xsd - Return Swaps Product Definitions.
* fpml-riskdef.xsd – Definitions of valuation and sensitivity results. They include detailed definitions of sensitivity calculations and are intended to be used by sophisticated users.
* fpml-shared.xsd - Shared definitions used widely throughout the specification. These include items such as base types, shared financial structures, etc.
* fpml-standard-5-3 - Used in Transparency reporting to define a product that represents a standardized OTC derivative transaction whose economics do not need to be fully described using an FpML schema because they are implied by the product ID. In other views, standard products are present for convenience to support internal messaging and workflows that are cross-product. Standard products are not full trade representations as such they are not intended to be used for confirming trades.
* fpml-valuation.xsd – Valuation result sets and related definitions.
* fpml-variance-swap.xsd - Variance Swap Product Definitions.
* xmldsig-core-schema.xsd - W3C Digital Signature Schema
  
  Plus, the Confirmation view directory contains subdirectories for each group of FpML examples, namely:
	
	
	confirmation/business-processes/
	 * allocation
	 * clearing
	 * collateral
	 * confirmation
	 * consent
	 * execution-advice
	 * execution-notification
	 * option-exercise-expiry
	 * product-definition
	 * service-notification
	 * trade-change-advice
	 * trade-info-update

	confirmation/products/
	 * bond-options
	 * commodity-derivatives
	 * correlation-swaps
	 * credit-derivatives
	 * dividend-swaps
	 * equit-forwards
	 * equity-options
	 * equity-swaps
	 * fx-derivatives
	 * inflation-swaps
	 * interest-rate-derivatives
	 * securities
	 * variance-swaps

	confirmation/validation/
	 * invalid-testcases


Pretrade Schemas:


* example-extension.xsd - File containing an example of an extension from the 5.x confirmation view schemas.
* fpml-asset.xsd - Underlyer definitions plus some types used by them (e.g. ones relating to commissions or dividend payouts).
* fpml-bond-option.xsd - Bond and Convertible Bond Options Product Definitions.
* fpml-business-events.xsd - File containing a Trade - (Content of the Business events components)
* fpml-cd.xsd - Credit derivative (swap) product definitions.
* fpml-clearing-processes.xsd - Clearing processes messages.
* fpml-doc.xsd - Trade and contract definitions and definitions relating to validation.
* fpml-enum.xsd - Shared enumeration definitions. These definitions list the values that enumerated types may take.
* fpml-fx.xsd - Foreign Exchange (NDF) Product Definitions.
* fpml-ird.xsd - Interest rate (swap) derivative product definitions.
* fpml-main.xsd - Root definitions.
* fpml-msg.xsd - High level definitions related to messaging.
* fpml-option-shared.xsd - Shared option definitions used for defining the common features of options.
* fpml-pretrade-processes.xsd - Pretrade process messages.
* fpml-shared.xsd - Shared definitions used widely throughout the specification. These include items such as base types, shared financial structures, etc.
* xmldsig-core-schema.xsd - W3C Digital Signature Schema


  Plus, the Pretrade view directory contains subdirectories for each group of FpML examples, namely:
	pretrade/
	 * clearing
	 * limit-check

	pretrade/products/
	 * credit-derivatives
	 * interest-rate-derivatives	


Reporting Schemas:


* example-extension.xsd - File containing an example of an extension from the 5.x confirmation view schemas.
* fpml-asset.xsd - Underlyer definitions plus some types used by them (e.g. ones relating to commissions or dividend payouts).
* fpml-bond-option.xsd - Bond and Convertible Bond Options Product Definitions.
* fpml-business-events.xsd - Content of the Business events components
* fpml-cd.xsd - Credit derivative product definitions.
* fpml-collateral-processes.xsd - Collateral process messages.
* fpml-com.xsd - Commodity product definitions.
* fpml-correlation-swap.xsd - Correlation Swap Product Definitions.
* fpml-credit-event-notification.xsd - Credit event notification components.
* fpml-dividend-swaps.xsd - Dividend Swap Product Definitions.
* fpml-doc.xsd - Trade and contract definitions and definitions relating to validation.
* fpml-enum.xsd - Shared enumeration definitions. These definitions list the values that enumerated types may take.
* fpml-eqd.xsd - Equity Option and Equity Forward Product Definitions.
* fpml-eq-shared.xsd - Definitions shared by types with Equity Underlyers.
* fpml-fx.xsd - Foreign Exchange Product Definitions.	
* fpml-generic.xsd – Used in Transparency reporting to define a product that represents an OTC derivative transaction whose economics are not fully described using an FpML schema. In other views, generic products are present for convenience to support internal messaging and workflows that are cross-product. Generic products are not full trade representations as such they are not intended to be used for confirming trades.
* fpml-ird.xsd - Interest rate derivative product definitions.
* fpml-main.xsd - Root definitions.
* fpml-mktenv.xsd – Definitions of market environment data structures such as yield curves, volatility matrices, and the like.
* fpml-msg.xsd - Definitions related to messaging and workflow.
* fpml-option-shared.xsd - Shared option definitions used for defining the common features of options.
* fpml-reconciliation.xsd - Cash flow matching and Portfolio Reconciliation messaging components    
* fpml-recordkeeping-processes.xsd - Recordkeeping process messages (for internal use only).
* fpml-reporting.xsd – Messages used for requesting and providing valuation reporting results.
* fpml-return-swaps.xsd - Return Swaps Product Definitions.
* fpml-riskdef.xsd – Definitions of valuation and sensitivity results. They include detailed definitions of sensitivity calculations and are intended to be used by sophisticated users.
* fpml-shared.xsd - Shared definitions used widely throughout the specification. These include items such as base types, shared financial structures, etc.
* fpml-standard.xsd - Used in Transparency reporting to define a product that represents a standardized OTC derivative transaction whose economics do not need to be fully described using an FpML schema because they are implied by the product ID. In other views, standard products are present for convenience to support internal messaging and workflows that are cross-product. Standard products are not full trade representations as such they are not intended to be used for confirming trades.
* fpml-valuation.xsd – Valuation result sets and related definitions.
* fpml-valuation-reporting – Valuation Reporting result sets and related definitions.
* fpml-variance-swap.xsd - Variance Swap Product Definitions.
* xmldsig-core-schema.xsd - W3C Digital Signature Schema


  Plus, the Reporting view directory contains subdirectories for each group of FpML examples, namely:
	
	
	reporting/
	 * cash flow matching
	 * collateral
	 * credit-event-notice
	 * entity-reporting
	 * exposure
	 * portfolio-reconciliation
	 * position-and-activity-reporting
	 * recordkeeping
	 * reset-reporting
	 * securities
	 * valuation

	reporting/validation/
	 * invalid-testcases



Recordkeeping Schemas:


* example-extension.xsd - File containing an example of an extension from the 5.x confirmation view schemas.
* fpml-asset.xsd - Underlyer definitions plus some types used by them (e.g. ones relating to commissions or dividend payouts).
* fpml-bond-option.xsd - Bond and Convertible Bond Options Product Definitions.
* fpml-business-events.xsd - Content of the Business events components
* fpml-cd.xsd - Credit derivative product definitions.
* fpml-com.xsd - Commodity product definitions.
* fpml-correlation-swap.xsd - Correlation Swap Product Definitions.
* fpml-credit-event-notification.xsd - Credit event notification components.
* fpml-dividend-swaps.xsd - Dividend Swap Product Definitions.
* fpml-doc.xsd - Trade and contract definitions and definitions relating to validation.
* fpml-enum.xsd - Shared enumeration definitions. These definitions list the values that enumerated types may take.
* fpml-eqd.xsd - Equity Option and Equity Forward Product Definitions.
* fpml-eq-shared.xsd - Definitions shared by types with Equity Underlyers.
* fpml-fx.xsd - Foreign Exchange Product Definitions.
* fpml-generic.xsd – Used in Transparency reporting to define a product that represents an OTC derivative transaction whose economics are not fully described using an FpML schema. In other views, generic products are present for convenience to support internal messaging and workflows that are cross-product. Generic products are not full trade representations as such they are not intended to be used for confirming trades.
* fpml-ird.xsd - Interest rate derivative product definitions.
* fpml-main.xsd - Root definitions.
* fpml-mktenv.xsd – Definitions of market environment data structures such as yield curves, volatility matrices, and the like.
* fpml-msg.xsd - High level definitions related to messaging.
* fpml-option-shared.xsd - Shared option definitions used for defining the common features of options.
* fpml-recordkeeping -processes.xsd - Recordkeeping process messages.
* fpml-return-swaps.xsd - Return Swaps Product Definitions.
* fpml-riskdef.xsd – Definitions of valuation and sensitivity results. They include detailed definitions of sensitivity calculations and are intended to be used by sophisticated users.
* fpml-shared.xsd - Shared definitions used widely throughout the specification. These include items such as base types, shared financial structures, etc.
* fpml-standard.xsd - Used in Transparency reporting to define a product that represents a standardized OTC derivative transaction whose economics do not need to be fully described using an FpML schema because they are implied by the product ID. In other views, standard products are present for convenience to support internal messaging and workflows that are cross-product. Standard products are not full trade representations as such they are not intended to be used for confirming trades.
* fpml-valuation.xsd – Valuation result sets and related definitions.
* fpml-valuation-reporting – Valuation Reporting result sets and related definitions.
* fpml-variance-swap.xsd - Variance Swap Product Definitions.
* xmldsig-core-schema.xsd - W3C Digital Signature Schema


  Plus, the Recordkeeping view directory contains subdirectories for each group of FpML examples, namely:
	
	
	recordkeeping/
	 * events
	 * products



Transparency - Schemas:


* example-extension.xsd - File containing an example of an extension from the 5.x confirmation view schemas.
* fpml-asset.xsd - Underlyer definitions plus some types used by them (e.g. ones relating to commissions or dividend payouts).
* fpml-bond-option.xsd - Bond and Convertible Bond Options Product Definitions.
* fpml-business-events.xsd - Content of the Business events components
* fpml-cd.xsd - Credit derivative product definitions.
* fpml-com.xsd - Commodity product definitions. 
* fpml-correlation-swap.xsd - Correlation Swap Product Definitions.
* fpml-credit-event-notification.xsd - Credit event notification components (for internal use only).
* fpml-dividend-swaps.xsd - Dividend Swap Product Definitions.
* fpml-doc.xsd - Trade and contract definitions and definitions relating to validation.
* fpml-enum.xsd - Shared enumeration definitions. These definitions list the values that enumerated types may take.
* fpml-eqd.xsd - Equity Option and Equity Forward Product Definitions.
* fpml-eq-shared.xsd - Definitions shared by types with Equity Underlyers.
* fpml-fx.xsd - Foreign Exchange Product Definitions.
* fpml-generic.xsd – Used in Transparency reporting to define a product that represents an OTC derivative transaction whose economics are not fully described using an FpML schema. In other views, generic products are present for convenience to support internal messaging and workflows that are cross-product. Generic products are not full trade representations as such they are not intended to be used for confirming trades.
* fpml-ird.xsd - Interest rate derivative product definitions.
* fpml-main.xsd - Root definitions.
* fpml-msg.xsd - High level definitions related to messaging.
* fpml-option-shared.xsd - Shared option definitions used for defining the common features of options.
* fpml-return-swaps.xsd - Return Swaps Product Definitions.
* fpml-shared.xsd - Shared definitions used widely throughout the specification. These include items such as base types, shared financial structures, etc.
* fpml-standard.xsd - Used in Transparency reporting to define a product that represents a standardized OTC derivative transaction whose economics do not need to be fully described using an FpML schema because they are implied by the product ID. In other views, standard products are present for convenience to support internal messaging and workflows that are cross-product. Standard products are not full trade representations as such they are not intended to be used for confirming trades.
* fpml-transparency-processes.xsd - Transparency process messages.
* fpml-variance-swap.xsd - Variance Swap Product Definitions.
* xmldsig-core-schema.xsd - W3C Digital Signature Schema

  Plus, the Transparency view directory contains subdirectories for each group of FpML examples, namely:
	
	transparency/generated-products/
	 * bond-options
	 * commodity-derivatives
	 * correlation-swaps
	 * credit-derivatives
	 * dividend-swaps
	 * equit-forwards
	 * equity-options
	 * equity-swaps
	 * fx-derivatives
	 * inflation-swaps
	 * interest-rate-derivatives
	 * securities
	 * variance-swaps
	
	transparency/
	 * messages
	 * products


Legal Schemas:
* fpml-asset.xsd - Underlyer definitions plus some types used by them (e.g. ones relating to commissions or dividend payouts).
* fpml-doc.xsd - Trade and contract definitions and definitions relating to validation.
* fpml-enum.xsd - Shared enumeration definitions. These definitions list the values that enumerated types may take.
* fpml-legal.xsd - Legal Definitions.
* fpml-main.xsd - Root definitions.
* fpml-shared.xsd - Shared definitions used widely throughout the specification. These include items such as base types, shared financial structures, etc.

  Plus, the Legal view directory contains subdirectories for each group of FpML examples, namely:

	legal
	 * legal-document


Each subdirectory contains a number of example files.  These files are named as follows:
YY_exNN_long_description.xml, 
where
YY is the subset identifier and
NN is an integer number.

The examples have a schemaLocation attribute that references their parent 
directory.  In other words, the FpML schema must be present in the parent 
directory of the example file for the example file to validate using the 
schemaLocation attribute.

The schemaLocation attribute previously referenced the examples' own directory, 
and extra copies of the schema files were placed in each example directory.
This simplified validation of the examples and helped certain tools to work
properly, but caused some users confusion.

The examples have been validated using Xerces J v 2.4.1.