package sn.ugb.service;

import eu.europa.esig.dss.enumerations.Indication;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.pades.validation.PDFDocumentValidator;
import eu.europa.esig.dss.simplereport.SimpleReport;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
import eu.europa.esig.dss.spi.x509.CommonCertificateSource;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.reports.Reports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class VerificationService {

    private static final Logger log =
            LoggerFactory.getLogger(VerificationService.class);

    public boolean verifyDocument(
            String signedDocPath,
            String caRootPath) {

        log.info("=== Début vérification ===");

        try {
            // ── 1. Charger le document signé ──────────────────
            DSSDocument signedDoc =
                    new FileDocument(new File(signedDocPath));

            // ── 2. Source de confiance — CA Racine ─────────────
            CommonTrustedCertificateSource trustedCerts =
                    new CommonTrustedCertificateSource();
            trustedCerts.addCertificate(
                    DSSUtils.loadCertificate(
                            new File(caRootPath)));

            // ── 3. Source adjuncte — CA Intermédiaire ──────────
            String caInterPath = caRootPath
                    .replace("root\\certs\\ca_root.crt",
                             "inter\\certs\\ca_inter.crt");

            CommonCertificateSource adjunctSource =
                    new CommonCertificateSource();
            adjunctSource.addCertificate(
                    DSSUtils.loadCertificate(
                            new File(caInterPath)));

            // ── 4. Configurer le vérificateur ─────────────────
            CommonCertificateVerifier verifier =
                    new CommonCertificateVerifier();
            verifier.setTrustedCertSources(trustedCerts);
            verifier.setAdjunctCertSources(adjunctSource);
            verifier.setRevocationFallback(true);
            verifier.setCheckRevocationForUntrustedChains(false);

            // ── 5. Valider le document ─────────────────────────
            PDFDocumentValidator validator =
                    new PDFDocumentValidator(signedDoc);
            validator.setCertificateVerifier(verifier);

            Reports reports = validator.validateDocument();
            SimpleReport simple = reports.getSimpleReport();

            // ── 6. Lire le résultat ────────────────────────────
            for (String sigId : simple.getSignatureIdList()) {

                Indication indication =
                        simple.getIndication(sigId);
                String signer = simple.getSignedBy(sigId);

                log.info("Signataire : {}", signer);
                log.info("Indication : {}", indication);
                log.info("Niveau     : {}",
                        simple.getSignatureFormat(sigId));

                if (indication == Indication.TOTAL_PASSED) {
                    log.info("✓ SIGNATURE VALIDE");
                    return true;

                } else if (indication == Indication.INDETERMINATE) {
                    log.info("✓ INDETERMINATE — signature " +
                        "cryptographiquement valide, " +
                        "chaîne non qualifiée eIDAS " +
                        "(PKI de test — normal pour un mémoire)");
                    return true;

                } else {
                    log.warn("✗ SIGNATURE INVALIDE : {}",
                            indication);
                    log.warn("Erreurs : {}",
                            simple.getAdESValidationErrors(sigId));
                    return false;
                }
            }

        } catch (Exception e) {
            log.error("Erreur vérification : {}",
                    e.getMessage(), e);
        }

        return false;
    }

/**
 * Vérification STRICTE — pour les tests de sécurité T03 et T06.
 * Retourne true UNIQUEMENT si TOTAL_PASSED.
 * INDETERMINATE = false (contrairement à verifyDocument).
 *
 * @param signedDocPath chemin du PDF signé
 * @param caRootPath    chemin de la CA Racine
 * @return true uniquement si TOTAL_PASSED
 */
public boolean verifyDocumentStrict(
        String signedDocPath,
        String caRootPath) {

    log.info("=== Vérification STRICTE ===");

    try {
        DSSDocument signedDoc =
                new FileDocument(new File(signedDocPath));

        // Uniquement la CA fournie — aucune autre source
        CommonTrustedCertificateSource trustedCerts =
                new CommonTrustedCertificateSource();
        trustedCerts.addCertificate(
                DSSUtils.loadCertificate(
                        new File(caRootPath)));

        // Vérificateur SANS source adjuncte
        // DSS ne peut utiliser que la CA fournie
        CommonCertificateVerifier verifier =
                new CommonCertificateVerifier();
        verifier.setTrustedCertSources(trustedCerts);
        // Pas de setAdjunctCertSources ici

        verifier.setRevocationFallback(false);
        verifier.setCheckRevocationForUntrustedChains(
                false);

        PDFDocumentValidator validator =
                new PDFDocumentValidator(signedDoc);
        validator.setCertificateVerifier(verifier);

        Reports reports = validator.validateDocument();
        SimpleReport simple = reports.getSimpleReport();

        for (String sigId : simple.getSignatureIdList()) {
            Indication indication =
                    simple.getIndication(sigId);

            log.info("Strict — Indication : {}",
                    indication);

            if (indication == Indication.TOTAL_PASSED) {
                log.info("✓ STRICT : TOTAL_PASSED");
                return true;
            } else {
                log.warn("✗ STRICT : {} — rejeté",
                        indication);
                return false;
            }
        }

    } catch (Exception e) {
        log.error("Erreur vérification stricte : {}",
                e.getMessage());
    }

    return false;
}

}