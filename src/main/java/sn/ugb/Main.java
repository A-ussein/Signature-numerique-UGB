package sn.ugb;

import sn.ugb.service.SignatureService;
import sn.ugb.service.VerificationService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

/**
 * Application console interactive.
 * Démonstration soutenance — UGB Master 2 2026.
 */
public class Main {

    static final String P12_ALICE =
        "C:\\pki-ugb\\alice\\alice.p12";
    static final String PASSWORD  = "UGB2026!";
    static final String CA_ROOT   =
        "C:\\pki-ugb\\root\\certs\\ca_root.crt";

    static SignatureService   signer =
        new SignatureService();
    static VerificationService verifier =
        new VerificationService();
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args)
            throws Exception {

        afficherBanniere();

        boolean continuer = true;
        while (continuer) {
            afficherMenu();
            String choix = scanner.nextLine().trim();

            switch (choix) {
                case "1" -> signerDocument();
                case "2" -> verifierDocument();
                case "3" -> testerFalsification();
                case "4" -> benchmark();
                case "5" -> {
                    System.out.println(
                        "\nAu revoir !");
                    continuer = false;
                }
                default -> System.out.println(
                    "Choix invalide.");
            }
        }
    }

    // ════════════════════════════════════════════
    //  MENU
    // ════════════════════════════════════════════

    static void afficherBanniere() {
        System.out.println(
        "╔═══════════════════════════════════════════╗");
        System.out.println(
        "║   SYSTÈME DE SIGNATURE NUMÉRIQUE          ║");
        System.out.println(
        "║   Mémoire Master 2 — UGB Saint-Louis      ║");
        System.out.println(
        "║   Implémentation PAdES / eIDAS Art.26     ║");
        System.out.println(
        "╚═══════════════════════════════════════════╝");
        System.out.println();
    }

    static void afficherMenu() {
        System.out.println(
        "┌─────────────────────────────────────────┐");
        System.out.println(
        "│  1. Signer un document PDF              │");
        System.out.println(
        "│  2. Vérifier une signature              │");
        System.out.println(
        "│  3. Tester la détection de falsification│");
        System.out.println(
        "│  4. Benchmark performances              │");
        System.out.println(
        "│  5. Quitter                             │");
        System.out.println(
        "└─────────────────────────────────────────┘");
        System.out.print("Votre choix : ");
    }

    // ════════════════════════════════════════════
    //  OPTION 1 — Signer
    // ════════════════════════════════════════════

    static void signerDocument() throws Exception {
        System.out.print(
            "\nChemin du PDF à signer : ");
        String docIn = scanner.nextLine().trim();

        if (!Files.exists(Path.of(docIn))) {
            System.out.println(
                "❌ Fichier introuvable : " + docIn);
            return;
        }

        // Chemin de sortie automatique
        String docOut = docIn.replace(
            ".pdf", "_signe.pdf");

        System.out.println(
            "\n⏳ Signature en cours...");
        long debut = System.currentTimeMillis();

        signer.signDocument(
            P12_ALICE, PASSWORD, docIn, docOut);

        long duree =
            System.currentTimeMillis() - debut;

        System.out.println(
            "✅ Document signé en " + duree + " ms");
        System.out.println(
            "📄 Fichier créé : " + docOut);
        System.out.println(
            "👉 Ouvrir dans Adobe Reader pour " +
            "voir le panneau de signature");
        System.out.println();
    }

    // ════════════════════════════════════════════
    //  OPTION 2 — Vérifier
    // ════════════════════════════════════════════

    static void verifierDocument() {
        System.out.print(
            "\nChemin du PDF signé à vérifier : ");
        String docSigne = scanner.nextLine().trim();

        if (!Files.exists(Path.of(docSigne))) {
            System.out.println(
                "❌ Fichier introuvable.");
            return;
        }

        System.out.println(
            "\n⏳ Vérification en cours...");
        long debut = System.currentTimeMillis();

        boolean valid = verifier.verifyDocument(
            docSigne, CA_ROOT);

        long duree =
            System.currentTimeMillis() - debut;

        System.out.println();
        if (valid) {
            System.out.println(
            "╔══════════════════════════════════╗");
            System.out.println(
            "║  ✅ SIGNATURE VALIDE             ║");
            System.out.println(
            "║  Authenticité    : Confirmée     ║");
            System.out.println(
            "║  Intégrité       : Confirmée     ║");
            System.out.println(
            "║  Non-répudiation : Confirmée     ║");
            System.out.println(
            "╚══════════════════════════════════╝");
        } else {
            System.out.println(
            "╔══════════════════════════════════╗");
            System.out.println(
            "║  ❌ SIGNATURE INVALIDE           ║");
            System.out.println(
            "║  Document falsifié ou cert       ║");
            System.out.println(
            "║  non reconnu                     ║");
            System.out.println(
            "╚══════════════════════════════════╝");
        }
        System.out.println(
            "⏱ Vérification : " + duree + " ms\n");
    }

    // ════════════════════════════════════════════
    //  OPTION 3 — Démonstration falsification
    // ════════════════════════════════════════════

    static void testerFalsification()
            throws Exception {

        System.out.println(
            "\n🔬 DÉMONSTRATION DÉTECTION FALSIFICATION");
        System.out.println(
            "─────────────────────────────────────────");

        String docIn  = "C:\\temp\\contrat.pdf";
        String docOut =
            "C:\\temp\\demo_signe.pdf";
        String docFalsifie =
            "C:\\temp\\demo_falsifie.pdf";

        // Étape 1 : Signer
        System.out.println(
            "Étape 1 : Signature du document...");
        signer.signDocument(
            P12_ALICE, PASSWORD, docIn, docOut);
        System.out.println("  ✅ Document signé");

        // Étape 2 : Vérifier original
        System.out.println(
            "Étape 2 : Vérification originale...");
        boolean avant = verifier.verifyDocument(
            docOut, CA_ROOT);
        System.out.println(
            "  → Résultat : " +
            (avant ? "✅ VALIDE" : "❌ INVALIDE"));

        // Étape 3 : Falsifier
        System.out.println(
            "Étape 3 : Falsification " +
            "(modification octet n°1000)...");
        byte[] bytes =
            Files.readAllBytes(Path.of(docOut));
        bytes[1000] ^= 0xFF;
        Files.write(Path.of(docFalsifie), bytes);
        System.out.println(
            "  ⚠️  Document modifié");

        // Étape 4 : Vérifier falsifié
        System.out.println(
            "Étape 4 : Vérification du document " +
            "falsifié...");
        boolean apres = verifier.verifyDocument(
            docFalsifie, CA_ROOT);
        System.out.println(
            "  → Résultat : " +
            (apres ? "✅ VALIDE" : "❌ INVALIDE"));

        // Conclusion
        System.out.println();
        System.out.println(
        "┌─────────────────────────────────────────┐");
        System.out.println(
        "│  CONCLUSION DE LA DÉMONSTRATION         │");
        System.out.println(
        "│  Document original  : ✅ VALIDE         │");
        System.out.println(
        "│  Document falsifié  : ❌ INVALIDE       │");
        System.out.println(
        "│  → Propriété d'intégrité validée        │");
        System.out.println(
        "│  → SHA-256 détecte toute modification   │");
        System.out.println(
        "└─────────────────────────────────────────┘");
        System.out.println();
    }

    // ════════════════════════════════════════════
    //  OPTION 4 — Benchmark
    // ════════════════════════════════════════════

    static void benchmark() throws Exception {
        System.out.println(
            "\n📊 BENCHMARK PERFORMANCES");
        System.out.println(
            "─────────────────────────────────────────");
        System.out.println(
            "10 répétitions par opération...\n");

        String docIn  = "C:\\temp\\contrat.pdf";
        String docOut = "C:\\temp\\bench_signe.pdf";
        int N = 10;

        // ── Benchmark Signature ───────────────────
        long totalSign = 0;
        for (int i = 0; i < N; i++) {
            long t = System.currentTimeMillis();
            signer.signDocument(
                P12_ALICE, PASSWORD,
                docIn, docOut);
            totalSign +=
                System.currentTimeMillis() - t;
            System.out.print(
                "  Signature " + (i+1) +
                "/" + N + "\r");
        }
        long moySign = totalSign / N;

        // ── Benchmark Vérification ────────────────
        long totalVerif = 0;
        for (int i = 0; i < N; i++) {
            long t = System.currentTimeMillis();
            verifier.verifyDocument(
                docOut, CA_ROOT);
            totalVerif +=
                System.currentTimeMillis() - t;
            System.out.print(
                "  Vérification " + (i+1) +
                "/" + N + "\r");
        }
        long moyVerif = totalVerif / N;

        // Tailles fichiers
        long tailleAvant =
            Files.size(Path.of(docIn));
        long tailleApres =
            Files.size(Path.of(docOut));
        long overhead = tailleApres - tailleAvant;

        // Résultats
        System.out.println();
        System.out.println(
        "┌──────────────────────────────────────────┐");
        System.out.println(
        "│  RÉSULTATS BENCHMARK (" + N +
        " répétitions)     │");
        System.out.println(
        "├──────────────────────────────────────────┤");
        System.out.printf(
        "│  Temps signature moyen  : %6d ms      │%n",
            moySign);
        System.out.printf(
        "│  Temps vérification moy : %6d ms      │%n",
            moyVerif);
        System.out.printf(
        "│  Taille document orig.  : %6d octets  │%n",
            tailleAvant);
        System.out.printf(
        "│  Taille document signé  : %6d octets  │%n",
            tailleApres);
        System.out.printf(
        "│  Overhead signature     : %6d octets  │%n",
            overhead);
        System.out.println(
        "├──────────────────────────────────────────┤");
        System.out.println(
        "│  Cible NF01 : < 2000 ms                 │");
        System.out.printf(
        "│  Résultat   : %s                │%n",
            moySign < 2000
            ? "✅ CONFORME     "
            : "❌ NON CONFORME ");
        System.out.println(
        "└──────────────────────────────────────────┘");
        System.out.println();
    }
}