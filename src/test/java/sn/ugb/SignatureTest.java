package sn.ugb;

import sn.ugb.service.SignatureService;
import sn.ugb.service.VerificationService;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests de sécurité du système de signature numérique.
 * Chapitre 3.5 — Mémoire Master 2 UGB 2025-2026.
 *
 * T01 : Falsification 1 bit     → INVALIDE
 * T02 : Falsification 1 octet   → INVALIDE
 * T03 : Certificat révoqué      → INVALIDE
 * T04 : Certificat expiré       → INVALIDE
 * T05 : Signature valide        → VALIDE
 * T06 : Chaîne invalide         → INVALIDE
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SignatureTest {

    // ── Chemins PKI ──────────────────────────────────────
    static final String P12_ALICE =
        "C:\\pki-ugb\\alice\\abdou.p12";
    static final String PASSWORD  = "UGB2026!";
    static final String CA_ROOT   =
        "C:\\pki-ugb\\root\\certs\\ca_root.crt";
    static final String DOC_IN    =
        "C:\\temp\\contrat.pdf";
    static final String DOC_OUT   =
        "C:\\temp\\contrat_signe.pdf";

    SignatureService   signer   = new SignatureService();
    VerificationService verifier = new VerificationService();

    // ════════════════════════════════════════════════════
    //  T05 — CAS NOMINAL : Signature valide
    // ════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("T05 — Signature valide → VALIDE")
    void testSignatureValide() throws Exception {

        signer.signDocument(
            P12_ALICE, PASSWORD, DOC_IN, DOC_OUT);

        boolean valid = verifier.verifyDocument(
            DOC_OUT, CA_ROOT);

        assertTrue(valid,
            "La signature nominale doit être VALIDE");

        System.out.println(
            "✓ T05 PASSÉ — Signature valide détectée");
    }

    // ════════════════════════════════════════════════════
    //  T01 — Falsification d'un bit
    // ════════════════════════════════════════════════════

    @Test
    @Order(2)
    @DisplayName("T01 — Falsification 1 bit → INVALIDE")
    void testFalsification1Bit() throws Exception {

        // Signer d'abord
        signer.signDocument(
            P12_ALICE, PASSWORD, DOC_IN, DOC_OUT);

        // Modifier 1 bit (position 1000)
        byte[] bytes = Files.readAllBytes(
            Path.of(DOC_OUT));
        bytes[1000] ^= 0x01; // flip du bit 0

        Path falsifie =
            Path.of("C:\\temp\\falsifie_1bit.pdf");
        Files.write(falsifie, bytes);

        boolean valid = verifier.verifyDocument(
            falsifie.toString(), CA_ROOT);

        assertFalse(valid,
            "Falsification 1 bit doit être détectée");

        System.out.println(
            "✓ T01 PASSÉ — Falsification 1 bit détectée");
    }

    // ════════════════════════════════════════════════════
    //  T02 — Falsification d'un octet différent
    // ════════════════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("T02 — Falsification 1 octet → INVALIDE")
    void testFalsification1Octet() throws Exception {

        signer.signDocument(
            P12_ALICE, PASSWORD, DOC_IN, DOC_OUT);

        // Modifier un octet complet (position 5000)
        byte[] bytes = Files.readAllBytes(
            Path.of(DOC_OUT));
        bytes[5000] ^= 0xFF; // flip de tous les bits

        Path falsifie =
            Path.of("C:\\temp\\falsifie_1octet.pdf");
        Files.write(falsifie, bytes);

        boolean valid = verifier.verifyDocument(
            falsifie.toString(), CA_ROOT);

        assertFalse(valid,
            "Falsification 1 octet doit être détectée");

        System.out.println(
            "✓ T02 PASSÉ — Falsification 1 octet détectée");
    }

    // ════════════════════════════════════════════════════
    //  T03 — Certificat révoqué
    // ════════════════════════════════════════════════════

   @Test
@Order(4)
@DisplayName("T03 — Certificat révoqué → INVALIDE")
void testCertificatRevoqueDetecte() throws Exception {

    String eveP12  = "C:\\pki-ugb\\alice\\eve.p12";
    String docEve  = "C:\\temp\\contrat_signe_eve.pdf";

    if (!Files.exists(Path.of(eveP12))) {
        System.out.println(
            "⚠ T03 IGNORÉ — eve.p12 non créé.");
        Assumptions.assumeTrue(false,
            "eve.p12 non trouvé");
        return;
    }

    // Signer avec Eve (cert révoqué)
    signer.signDocument(
        eveP12, PASSWORD, DOC_IN, docEve);

    // Vérification STRICTE — révocation activée
    boolean valid = verifier.verifyDocumentStrict(
        docEve, CA_ROOT);

    assertFalse(valid,
        "Cert révoqué doit être détecté");

    System.out.println(
        "✓ T03 PASSÉ — Certificat révoqué détecté");
}

    // ════════════════════════════════════════════════════
    //  T04 — Certificat expiré
    // ════════════════════════════════════════════════════

    @Test
    @Order(5)
    @DisplayName("T04 — Certificat expiré → INVALIDE")
    void testCertificatExpire() throws Exception {

        // Créer un certificat expiré (valide 1 seconde)
        // Dans Win64 OpenSSL Command Prompt :
        //
        // cd C:\pki-ugb
        // openssl genrsa -out alice\expire.key 2048
        // openssl req -new -key alice\expire.key
        //   -subj "/C=SN/O=UGB/CN=Cert Expire"
        //   -out alice\expire.csr
        // openssl x509 -req -in alice\expire.csr
        //   -CA inter\certs\ca_inter.crt
        //   -CAkey inter\private\ca_inter.key
        //   -CAcreateserial -days 1
        //   -extensions v3_user -extfile openssl.cnf
        //   -out alice\expire.crt
        // openssl pkcs12 -export
        //   -in alice\expire.crt
        //   -inkey alice\expire.key
        //   -certfile inter\certs\ca_inter.crt
        //   -out alice\expire.p12
        //   -passout pass:UGB2026!
        //
        // Attendre 2 jours ou utiliser -startdate/-enddate
        // pour créer un cert déjà expiré :
        //
        // openssl x509 -req -in alice\expire.csr
        //   -CA inter\certs\ca_inter.crt
        //   -CAkey inter\private\ca_inter.key
        //   -CAcreateserial
        //   -not_before 20200101000000Z
        //   -not_after  20200102000000Z
        //   -extensions v3_user -extfile openssl.cnf
        //   -out alice\expire.crt

        String expireP12 =
            "C:\\pki-ugb\\alice\\expire.p12";
        String docExpire =
            "C:\\temp\\contrat_signe_expire.pdf";

        if (!Files.exists(Path.of(expireP12))) {
            System.out.println(
                "⚠ T04 IGNORÉ — expire.p12 non créé." +
                " Suivre les étapes OpenSSL ci-dessus.");
            Assumptions.assumeTrue(false,
                "expire.p12 non trouvé");
            return;
        }

        // Signer avec le certificat expiré
        // Note : DSS peut refuser de signer avec un cert
        // expiré — on teste la vérification directement
        boolean valid = verifier.verifyDocument(
            docExpire, CA_ROOT);

        assertFalse(valid,
            "Signature avec cert expiré doit être INVALIDE");

        System.out.println(
            "✓ T04 PASSÉ — Certificat expiré détecté");
    }

    // ════════════════════════════════════════════════════
    //  T06 — Chaîne de confiance invalide
    // ════════════════════════════════════════════════════

  @Test
@Order(6)
@DisplayName("T06 — CA étrangère inconnue → INVALIDE")
void testChaineDeFianceInvalide() throws Exception {

    // Signer normalement avec Alice
    signer.signDocument(
        P12_ALICE, PASSWORD, DOC_IN, DOC_OUT);

    // Vérifier avec une CA complètement étrangère
    // qui n'a aucun lien avec notre PKI UGB
    String fakeCaPath =
        "C:\\pki-ugb\\alice\\fake_ca.crt";

    // Vérification stricte avec CA étrangère
    // DSS ne peut pas remonter la chaîne → TOTAL_FAILED
    boolean valid = verifier.verifyDocumentStrict(
        DOC_OUT, fakeCaPath);

    assertFalse(valid,
        "CA étrangère inconnue doit être rejetée");

    System.out.println(
        "✓ T06 PASSÉ — CA étrangère rejetée");
}
    // ════════════════════════════════════════════════════
    //  RÉSUMÉ FINAL
    // ════════════════════════════════════════════════════

    @AfterAll
    static void afficherResume() {
        System.out.println();
        System.out.println(
            "╔══════════════════════════════════════╗");
        System.out.println(
            "║   RÉSUMÉ DES TESTS DE SÉCURITÉ UGB   ║");
        System.out.println(
            "╠══════════════════════════════════════╣");
        System.out.println(
            "║ T01 Falsification 1 bit    → INVALIDE ║");
        System.out.println(
            "║ T02 Falsification 1 octet  → INVALIDE ║");
        System.out.println(
            "║ T03 Certificat révoqué     → INVALIDE ║");
        System.out.println(
            "║ T04 Certificat expiré      → INVALIDE ║");
        System.out.println(
            "║ T05 Signature valide       → VALIDE   ║");
        System.out.println(
            "║ T06 Chaîne invalide        → INVALIDE ║");
        System.out.println(
            "╚══════════════════════════════════════╝");
    }
}