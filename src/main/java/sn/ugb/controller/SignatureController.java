package sn.ugb.controller;

import sn.ugb.model.OperationResult;
import sn.ugb.service.SignatureService;
import sn.ugb.service.VerificationService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Controller
public class SignatureController {

    // Chemins PKI
   static final String P12 =
    "C:\\pki-ugb\\alice\\abdou.p12";
    static final String PASS   = "UGB2026!";
    static final String CA     =
        "C:\\pki-ugb\\root\\certs\\ca_root.crt";
    static final String UPLOAD =
        "C:\\temp\\uploads\\";

    SignatureService    signer   =
        new SignatureService();
    VerificationService verifier =
        new VerificationService();

    // ── Page principale ───────────────────────────
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("titre",
            "Système de Signature Numérique — UGB");
        return "index";
    }

    // ── API : Signer un document ──────────────────
@PostMapping("/api/signer")
@ResponseBody
public ResponseEntity<OperationResult> signer(
        @RequestParam("fichier")
        MultipartFile fichier) {
    try {
        // Créer dossier upload
        Files.createDirectories(
            Path.of(UPLOAD));

        // Nom des fichiers
        String nomOriginal =
            fichier.getOriginalFilename();
        String nomSigne = nomOriginal
            .replace(".pdf", "_signe.pdf");

        // Chemins complets
        String docIn  = UPLOAD + nomOriginal;
        String docOut = UPLOAD + nomSigne;

        // Sauvegarder le fichier uploadé
        fichier.transferTo(new File(docIn));

        // ── Signer ───────────────────────────
        long debut = System.currentTimeMillis();
        signer.signDocument(
            P12, PASS, docIn, docOut);
        long duree =
            System.currentTimeMillis() - debut;

        // ── Résultat ─────────────────────────
        OperationResult result =
            new OperationResult(
                true,
                "✅ Document signé avec succès",
                "Signataire : Abdou Fatim DIOP | " +
                "Algorithme : RSA + SHA-256 | " +
                "Format : PAdES-B",
                duree
            );

        // NOM SEUL — pas le chemin complet
        result.setOutputPath(nomSigne);

        return ResponseEntity.ok(result);

    } catch (Exception e) {
        return ResponseEntity.ok(
            new OperationResult(
                false,
                "❌ Erreur de signature",
                e.getMessage(), 0));
    }
}
// ── API : Télécharger le document signé ───────
@GetMapping("/api/telecharger")
public ResponseEntity<org.springframework.core.io
        .Resource> telecharger(
        @RequestParam("fichier")
        String nomFichier) {
    try {
        Path filePath =
            Path.of(UPLOAD + nomFichier);

        if (!Files.exists(filePath)) {
            return ResponseEntity
                .notFound().build();
        }

        org.springframework.core.io.Resource
            resource =
            new org.springframework.core.io
                .FileSystemResource(
                    filePath.toFile());

        return ResponseEntity.ok()
            .header("Content-Disposition",
                "attachment; filename=\"" +
                nomFichier + "\"")
            .header("Content-Type",
                "application/pdf")
            .body(resource);

    } catch (Exception e) {
        return ResponseEntity
            .internalServerError().build();
    }
}
    // ── API : Vérifier une signature ──────────────
    @PostMapping("/api/verifier")
    @ResponseBody
    public ResponseEntity<OperationResult> verifier(
            @RequestParam("fichier")
            MultipartFile fichier) {
        try {
            Files.createDirectories(
                Path.of(UPLOAD));

            String nom = fichier.getOriginalFilename();
            String docPath = UPLOAD + nom;
            fichier.transferTo(new File(docPath));

            long debut = System.currentTimeMillis();
            boolean valid = verifier.verifyDocument(
                docPath, CA);
            long duree =
                System.currentTimeMillis() - debut;

            return ResponseEntity.ok(
                new OperationResult(
                    valid,
                    valid
                    ? "✅ Signature VALIDE"
                    : "❌ Signature INVALIDE",
                    valid
                    ? "Authenticité ✓ | " +
                      "Intégrité ✓ | " +
                      "Non-répudiation ✓"
                    : "Le document a été modifié " +
                      "ou le certificat est invalide",
                    duree
                ));

        } catch (Exception e) {
            return ResponseEntity.ok(
                new OperationResult(
                    false,
                    "❌ Erreur de vérification",
                    e.getMessage(), 0));
        }
    }

    // ── API : Tester la falsification ─────────────
    @PostMapping("/api/falsifier")
    @ResponseBody
    public ResponseEntity<OperationResult> falsifier(
            @RequestParam("fichier")
            MultipartFile fichier) {
        try {
            Files.createDirectories(
                Path.of(UPLOAD));

            String nom = fichier.getOriginalFilename();
            String docIn  = UPLOAD + nom;
            String docOut = UPLOAD +
                "demo_signe.pdf";
            String docFalsifie = UPLOAD +
                "demo_falsifie.pdf";

            fichier.transferTo(new File(docIn));

            // Signer
            signer.signDocument(
                P12, PASS, docIn, docOut);

            // Vérifier original
            boolean avant = verifier.verifyDocument(
                docOut, CA);

            // Falsifier
            byte[] bytes =
                Files.readAllBytes(Path.of(docOut));
            bytes[1000] ^= 0xFF;
            Files.write(
                Path.of(docFalsifie), bytes);

            // Vérifier falsifié
            boolean apres = verifier.verifyDocument(
                docFalsifie, CA);

            return ResponseEntity.ok(
                new OperationResult(
                    true,
                    "🔬 Démonstration terminée",
                    "Document original : " +
                    (avant ? "✅ VALIDE" :
                             "❌ INVALIDE") +
                    " | Document falsifié : " +
                    (apres ? "✅ VALIDE" :
                             "❌ DÉTECTÉ"),
                    0
                ));

        } catch (Exception e) {
            return ResponseEntity.ok(
                new OperationResult(
                    false,
                    "❌ Erreur",
                    e.getMessage(), 0));
        }
    }

    // ── API : Benchmark ───────────────────────────
    @GetMapping("/api/benchmark")
    @ResponseBody
    public ResponseEntity<OperationResult> benchmark()
            throws Exception {

        String docIn  = "C:\\temp\\contrat.pdf";
        String docOut = "C:\\temp\\bench.pdf";
        int N = 5;

        // Benchmark signature
        long totalSign = 0;
        for (int i = 0; i < N; i++) {
            long t = System.currentTimeMillis();
            signer.signDocument(
                P12, PASS, docIn, docOut);
            totalSign +=
                System.currentTimeMillis() - t;
        }

        // Benchmark vérification
        long totalVerif = 0;
        for (int i = 0; i < N; i++) {
            long t = System.currentTimeMillis();
            verifier.verifyDocument(docOut, CA);
            totalVerif +=
                System.currentTimeMillis() - t;
        }

        long moySign  = totalSign  / N;
        long moyVerif = totalVerif / N;
        long tailleAvant =
            Files.size(Path.of(docIn));
        long tailleApres =
            Files.size(Path.of(docOut));

        return ResponseEntity.ok(
            new OperationResult(
                true,
                "📊 Benchmark terminé (" + N +
                " répétitions)",
                "Signature : " + moySign +
                " ms | Vérification : " +
                moyVerif + " ms | " +
                "Taille originale : " +
                tailleAvant + " octets | " +
                "Taille signée : " +
                tailleApres + " octets | " +
                "Overhead : " +
                (tailleApres - tailleAvant) +
                " octets",
                moySign
            ));
    }
}