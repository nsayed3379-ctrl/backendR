-- NID (personal ID) document verification has been removed in favor of the
-- existing PHONE claim-verification path (see claim.VerificationMethod).
-- Historical audit_log rows referencing entity_type = 'NID_VERIFICATION'
-- are left in place as a record of past admin actions.

DROP TABLE IF EXISTS nid_verification;
