package sn.ugb.service;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.util.Date;

/**
 * Service de signature numérique PAdES-B.
 * Mémoire Master 2 — UGB Saint-Louis 2025-2026.
 */
public class SignatureService {

    private static final Logger log =
            LoggerFactory.getLogger(SignatureService.class);

    /**
     * Signe un document PDF en PAdES-B.
     *
     * @param p12Path      chemin du fichier PKCS#12
     * @param password     mot de passe du PKCS#12
     * @param documentPath chemin du PDF à signer
     * @param outputPath   chemin du PDF signé en sortie
     */
    public void signDocument(
            String p12Path,
            String password,
            String documentPath,
            String outputPath) throws IOException {

        log.info("=== Début signature PAdES ===");

        // ── 1. Charger le document PDF ──────────────────────
        DSSDocument document =
                new FileDocument(new File(documentPath));
        log.info("Document chargé : {}", documentPath);

        // ── 2. Configurer les paramètres PAdES ──────────────
        PAdESSignatureParameters params =
                new PAdESSignatureParameters();
        params.setSignatureLevel(SignatureLevel.PAdES_BASELINE_B);
        params.setDigestAlgorithm(DigestAlgorithm.SHA256);
        params.setReason("Signature mémoire Master 2 UGB 2026");
        params.setLocation("Saint-Louis, Sénégal");
        params.bLevel().setSigningDate(new Date());

        // ── 3. Charger la clé depuis PKCS#12 ────────────────
        try (Pkcs12SignatureToken token = new Pkcs12SignatureToken(
                p12Path,
                new KeyStore.PasswordProtection(
                        password.toCharArray()))) {

            DSSPrivateKeyEntry privateKey =
                    token.getKeys().get(0);
            log.info("Clé chargée pour : {}",
                    privateKey.getCertificate()
                              .getCertificate()
                              .getSubjectX500Principal());

            params.setSigningCertificate(
                    privateKey.getCertificate());
            params.setCertificateChain(
                    privateKey.getCertificateChain());

            // ── 4. Calculer les données à signer ─────────────
            CommonCertificateVerifier verifier =
                    new CommonCertificateVerifier();
            PAdESService service = new PAdESService(verifier);

            ToBeSigned dataToSign =
                    service.getDataToSign(document, params);
            log.info("Phase 1 OK : hash calculé");

            // ── 5. Signer avec la clé privée ──────────────────
            SignatureValue signatureValue = token.sign(
                    dataToSign,
                    params.getDigestAlgorithm(),
                    privateKey);
            log.info("Phase 2 OK : signature calculée");

            // ── 6. Assembler le PDF signé ─────────────────────
            DSSDocument signedDoc = service.signDocument(
                    document, params, signatureValue);

            // ── 7. Sauvegarder ───────────────────────────────
            signedDoc.save(outputPath);
            log.info("=== PDF signé : {} ===", outputPath);

        } catch (Exception e) {
            log.error("Erreur signature : {}", e.getMessage());
            throw new IOException("Signature échouée : "
                    + e.getMessage(), e);
        }
    }
}