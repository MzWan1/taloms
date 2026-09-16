package za.co.taloms.company.application.service;

import za.co.taloms.company.application.dto.PorVerificationResponse;

/**
 * Proof-of-residence verification for approved external companies.
 *
 * Implemented separately from any document-returning feature: this contract
 * returns verification information only. If a business requirement later needs
 * the actual stamped PoR document, it must be added as its own explicit,
 * separately-authorized capability.
 */
public interface ProofOfResidenceService {

    /**
     * Verifies whether a valid proof of residence exists for the given full SA ID
     * number. Callers must already be authenticated and authorized.
     */
    PorVerificationResponse verify(String idNumber);
}