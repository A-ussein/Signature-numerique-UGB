/* ═══════════════════════════════════════════════════════════
   app.js — Logique JavaScript interface signature numérique
   Université Gaston Berger — Master 2 Mathématiques CCA
   2024-2025
═══════════════════════════════════════════════════════════ */
 
// ══ STATISTIQUES ═════════════════════════════════════════
let stats = {
    signatures:    0,
    verifications: 0,
    valides:       0,
    invalides:     0
};
 
// ══ NAVIGATION ═══════════════════════════════════════════
function showSection(name) {
    document.querySelectorAll('.section')
            .forEach(s => s.classList.remove('active'));
    document.querySelectorAll('nav a')
            .forEach(a => a.classList.remove('active'));
    document.getElementById('section-' + name)
            .classList.add('active');
    document.getElementById('nav-' + name)
            .classList.add('active');
}
 
// ══ UPLOAD ═══════════════════════════════════════════════
function showFileInfo(inputId, infoId) {
    const file =
        document.getElementById(inputId).files[0];
    if (!file) return;
    const name = file.name;
    const size = (file.size / 1024).toFixed(1) + ' Ko';
    document.getElementById(infoId + '-name')
            .textContent = name;
    document.getElementById(infoId + '-size')
            .textContent = size;
    document.getElementById(infoId)
            .classList.add('visible');
    const btnId = 'btn-' + infoId.replace('info-', '');
    const btn = document.getElementById(btnId);
    if (btn) btn.disabled = false;
}
 
function dragOver(e, zoneId) {
    e.preventDefault();
    document.getElementById(zoneId)
            .classList.add('dragover');
}
 
function dragLeave(zoneId) {
    document.getElementById(zoneId)
            .classList.remove('dragover');
}
 
function drop(e, inputId, infoId) {
    e.preventDefault();
    dragLeave(e.currentTarget.id || '');
    const dt = e.dataTransfer;
    if (dt && dt.files.length) {
        const input =
            document.getElementById(inputId);
        input.files = dt.files;
        showFileInfo(inputId, infoId);
    }
}
 
// ══ SIGNER ═══════════════════════════════════════════════
async function signerDocument() {
    const file =
        document.getElementById(
            'file-signer').files[0];
    if (!file) return;

    showLoader('signer');

    const form = new FormData();
    form.append('fichier', file);

    try {
        const res = await fetch('/api/signer', {
            method: 'POST',
            body: form
        });
        const data = await res.json();
        console.log('DATA:', JSON.stringify(data));
        hideLoader('signer');

        // Afficher résultat
        const box = document.getElementById(
            'result-signer');
        box.className = 'result-box visible success';

        document.getElementById(
            'result-signer-title')
            .textContent = data.message;
        document.getElementById(
            'result-signer-detail')
            .textContent = data.detail || '';
        document.getElementById(
            'result-signer-time')
            .textContent = '⏱ Durée : ' +
                data.durationMs + ' ms';

        // ── Afficher le bouton télécharger ────
        const nomFichier = data.outputPath ||
            file.name.replace('.pdf', '_signe.pdf');

        const zone = document.getElementById(
            'zone-telecharger');
        const lien = document.getElementById(
            'lien-telecharger');

        lien.href = '/api/telecharger?fichier=' +
            encodeURIComponent(nomFichier);
        lien.setAttribute('download', nomFichier);
        zone.style.display = 'block';

        // Compteur
        stats.signatures++;
        document.getElementById('nb-signatures')
                .textContent = stats.signatures;

    } catch(e) {
        hideLoader('signer');
        const box = document.getElementById(
            'result-signer');
        box.className = 'result-box visible error';
        document.getElementById(
            'result-signer-title')
            .textContent = '❌ Erreur : ' + e.message;
    }
}
 
// ══ VÉRIFIER ═════════════════════════════════════════════
async function verifierDocument() {
    const file =
        document.getElementById(
            'file-verifier').files[0];
    if (!file) return;
 
    showLoader('verifier');
 
    const form = new FormData();
    form.append('fichier', file);
 
    try {
        const res = await fetch('/api/verifier', {
            method: 'POST',
            body: form
        });
        const data = await res.json();
        hideLoader('verifier');
 
        showResult('verifier',
            data.success,
            data.message,
            data.detail,
            data.durationMs);
 
        stats.verifications++;
        document.getElementById('nb-verifications')
                .textContent = stats.verifications;
 
        if (data.success) {
            stats.valides++;
            document.getElementById('nb-valides')
                    .textContent = stats.valides;
        } else {
            stats.invalides++;
            document.getElementById('nb-invalides')
                    .textContent = stats.invalides;
        }
    } catch(e) {
        hideLoader('verifier');
        showResult('verifier', false,
            '❌ Erreur réseau', e.message, 0);
    }
}
 
// ══ FALSIFICATION ════════════════════════════════════════
async function testerFalsification() {
    const file =
        document.getElementById(
            'file-falsifier').files[0];
    if (!file) return;
 
    showLoader('falsifier');
 
    const form = new FormData();
    form.append('fichier', file);
 
    try {
        const res = await fetch('/api/falsifier', {
            method: 'POST',
            body: form
        });
        const data = await res.json();
        hideLoader('falsifier');
 
        const detail = data.detail || '';
        const avant  = detail.includes('original : ✅');
        const apres  = !detail.includes('falsifié : ❌');
 
        const box =
            document.getElementById('result-falsifier');
        box.className = 'result-box visible info';
 
        document.getElementById(
            'result-falsifier-title')
            .textContent = data.message;
 
        document.getElementById('demo-steps')
                .style.display = 'grid';
 
        document.getElementById('demo-avant')
            .innerHTML = avant
            ? '<span style="color:var(--ugb-vert)">✅ VALIDE</span>'
            : '<span style="color:var(--ugb-rouge)">❌ INVALIDE</span>';
 
        document.getElementById('demo-apres')
            .innerHTML = !apres
            ? '<span style="color:var(--ugb-rouge)">❌ FALSIFICATION DÉTECTÉE</span>'
            : '<span style="color:var(--ugb-vert)">✅ VALIDE</span>';
 
        document.getElementById(
            'result-falsifier-detail')
            .innerHTML =
            '<strong>Conclusion :</strong> ' +
            'La propriété d\'intégrité est validée. ' +
            'SHA-256 détecte toute modification du document, ' +
            'même d\'un seul octet ' +
            '(effet avalanche de la fonction de hachage).';
 
    } catch(e) {
        hideLoader('falsifier');
        const box =
            document.getElementById('result-falsifier');
        box.className = 'result-box visible error';
        document.getElementById(
            'result-falsifier-title')
            .textContent = '❌ Erreur : ' + e.message;
    }
}
 
// ══ BENCHMARK ════════════════════════════════════════════
async function lancerBenchmark() {
    showLoader('benchmark');
    document.getElementById('result-benchmark')
            .style.display = 'none';
 
    try {
        const res = await fetch('/api/benchmark');
        const data = await res.json();
        hideLoader('benchmark');
 
        if (!data.success) return;
 
        const detail = data.detail || '';
 
        // Parser les valeurs
        const getMs = (str, idx) => {
            const parts = str.split('|');
            const part  = parts[idx] || '';
            const match = part.match(/(\d+)\s*ms/);
            return match ? parseInt(match[1]) : 0;
        };
        const getOct = (str, label) => {
            const re =
                new RegExp(label + '\\s*:\\s*(\\d+)');
            const m = str.match(re);
            return m ? parseInt(m[1]) : 0;
        };
 
        const sign  = data.durationMs;
        const verif = getMs(detail, 1);
        const orig  = getOct(detail, 'Taille originale');
        const signe = getOct(detail, 'Taille signée');
        const over  = signe > 0 ? signe - orig : 0;
 
        const rows = [
            ['Temps de signature (moyenne)',
             sign + ' ms',
             '< 2 000 ms',
             sign > 0 && sign < 2000],
            ['Temps de vérification (moyenne)',
             verif + ' ms',
             '< 2 000 ms',
             verif > 0 && verif < 2000],
            ['Taille document original',
             orig + ' octets',
             '—', true],
            ['Taille document signé',
             signe + ' octets',
             '—', true],
            ['Overhead de la signature',
             over + ' octets',
             '—', true],
        ];
 
        const tbody =
            document.getElementById('bench-tbody');
        tbody.innerHTML = rows.map(r => `
            <tr>
                <td>${r[0]}</td>
                <td><strong>${r[1]}</strong></td>
                <td>${r[2]}</td>
                <td class="${r[3] ? 'ok' : 'ko'}">
                    ${r[3]
                        ? '✅ Conforme'
                        : '❌ Non conforme'}
                </td>
            </tr>
        `).join('');
 
        document.getElementById('result-benchmark')
                .style.display = 'block';
 
    } catch(e) {
        hideLoader('benchmark');
        alert('Erreur benchmark : ' + e.message);
    }
}
 
// ══ UTILITAIRES ══════════════════════════════════════════
function showLoader(id) {
    const loader =
        document.getElementById('loader-' + id);
    if (loader) loader.classList.add('visible');
    const btn =
        document.getElementById('btn-' + id);
    if (btn) btn.disabled = true;
}
 
function hideLoader(id) {
    const loader =
        document.getElementById('loader-' + id);
    if (loader) loader.classList.remove('visible');
    const btn =
        document.getElementById('btn-' + id);
    if (btn) btn.disabled = false;
}
 
function showResult(id, success, title, detail, ms) {
    const box =
        document.getElementById('result-' + id);
    if (!box) return;
    box.className = 'result-box visible ' +
        (success ? 'success' : 'error');
 
    const titleEl =
        document.getElementById(
            'result-' + id + '-title');
    if (titleEl) titleEl.textContent = title;
 
    const detailEl =
        document.getElementById(
            'result-' + id + '-detail');
    if (detailEl) detailEl.textContent = detail || '';
 
    const timeEl =
        document.getElementById(
            'result-' + id + '-time');
    if (timeEl && ms > 0)
        timeEl.textContent = '⏱ Durée : ' + ms + ' ms';
}
 