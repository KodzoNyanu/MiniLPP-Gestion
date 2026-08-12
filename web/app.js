// MiniLPP — interface web.
// Volontairement sans framework : fetch, DOM, modules ES. C'est ce que la
// mention « bonne connaissance en JavaScript / html » recouvre en pratique.

const chf = new Intl.NumberFormat('fr-CH', {
  style: 'currency', currency: 'CHF', maximumFractionDigits: 0
});
const chfPrecis = new Intl.NumberFormat('fr-CH', {
  style: 'currency', currency: 'CHF', minimumFractionDigits: 2
});

const $ = (sel) => document.querySelector(sel);
const corpsAssures = $('#tableau-assures tbody');
const etat = $('#etat');

let assures = [];
let triCourant = { colonne: 'nom', ascendant: true };
let assureSelectionne = null;

// --- chargement de la liste --------------------------------------------------

async function chargerAssures(filtre = '') {
  etat.textContent = 'Chargement…';
  try {
    const reponse = await fetch(`/api/assures?nom=${encodeURIComponent(filtre)}`);
    if (!reponse.ok) throw new Error(`HTTP ${reponse.status}`);
    assures = await reponse.json();
    afficherAssures();
    etat.textContent = `${assures.length} assuré(s)`;
  } catch (erreur) {
    etat.textContent = `Erreur : ${erreur.message}. La base MariaDB est-elle démarrée ?`;
  }
}

function afficherAssures() {
  const { colonne, ascendant } = triCourant;
  const tries = [...assures].sort((a, b) => {
    const x = a[colonne], y = b[colonne];
    const cmp = typeof x === 'string' ? x.localeCompare(y, 'fr') : Number(x) - Number(y);
    return ascendant ? cmp : -cmp;
  });

  corpsAssures.replaceChildren();
  for (const a of tries) {
    const tr = document.createElement('tr');
    tr.dataset.id = a.id;
    // textContent et non innerHTML : aucune donnée saisie ne doit pouvoir
    // devenir du HTML exécutable (XSS).
    for (const [valeur, num] of [
      [a.nom, false], [a.prenom, false], [a.age, true],
      [`${a.tauxActivite} %`, true],
      [chf.format(a.salaireAnnuelBrut), true],
      [chf.format(a.salaireCoordonne), true]
    ]) {
      const td = document.createElement('td');
      td.textContent = valeur;
      if (num) td.className = 'num';
      tr.append(td);
    }
    corpsAssures.append(tr);
  }
}

// --- projection ---------------------------------------------------------------

async function chargerProjection(id, ageRetraite) {
  const reponse = await fetch(`/api/assures/${id}/projection?ageRetraite=${ageRetraite}`);
  if (!reponse.ok) throw new Error(`HTTP ${reponse.status}`);
  const p = await reponse.json();

  $('#panneau-projection').hidden = false;
  $('#nom-assure').textContent = p.assure;
  $('#avoir-final').textContent = chf.format(p.avoirALaRetraite);
  $('#rente-annuelle').textContent = chf.format(p.renteAnnuelle);
  $('#rente-mensuelle').textContent = chfPrecis.format(p.renteMensuelle);

  const corps = $('#tableau-projection tbody');
  corps.replaceChildren();
  for (const l of p.lignes) {
    const tr = document.createElement('tr');
    for (const [valeur, num] of [
      [l.annee, false], [l.age, true],
      [chf.format(l.avoirInitial), true], [chf.format(l.bonification), true],
      [chf.format(l.interet), true], [chf.format(l.avoirFinal), true]
    ]) {
      const td = document.createElement('td');
      td.textContent = valeur;
      if (num) td.className = 'num';
      tr.append(td);
    }
    corps.append(tr);
  }

  dessinerGraphique(p.lignes);
}

// Graphique SVG écrit à la main : exercice E5.3, aucune librairie.
function dessinerGraphique(lignes) {
  const largeur = 800, hauteur = 260, marge = { h: 55, b: 28, t: 10, d: 10 };
  const max = Math.max(...lignes.map((l) => Number(l.avoirFinal)), 1);
  const x = (i) => marge.h + (i / Math.max(lignes.length - 1, 1)) * (largeur - marge.h - marge.d);
  const y = (v) => hauteur - marge.b - (v / max) * (hauteur - marge.b - marge.t);

  const points = lignes.map((l, i) => `${x(i).toFixed(1)},${y(Number(l.avoirFinal)).toFixed(1)}`);
  const aire = `M ${marge.h},${hauteur - marge.b} L ${points.join(' L ')} L ${x(lignes.length - 1)},${hauteur - marge.b} Z`;

  const graduations = [0, 0.5, 1].map((f) => {
    const valeur = max * f;
    return `<line x1="${marge.h}" y1="${y(valeur)}" x2="${largeur - marge.d}" y2="${y(valeur)}"
                  stroke="#e2e2dd"/>
            <text x="${marge.h - 6}" y="${y(valeur) + 4}" text-anchor="end"
                  font-size="10" fill="#6b6b66">${chf.format(valeur)}</text>`;
  }).join('');

  const etiquettesX = lignes
    .filter((_, i) => i % Math.ceil(lignes.length / 8) === 0 || i === lignes.length - 1)
    .map((l) => {
      const i = lignes.indexOf(l);
      return `<text x="${x(i)}" y="${hauteur - 8}" text-anchor="middle"
                    font-size="10" fill="#6b6b66">${l.annee}</text>`;
    }).join('');

  $('#graphique').innerHTML = `
    <svg viewBox="0 0 ${largeur} ${hauteur}" role="img"
         aria-label="Projection de l'avoir de vieillesse">
      ${graduations}
      <path d="${aire}" fill="#1f5f4f" fill-opacity="0.12"/>
      <polyline points="${points.join(' ')}" fill="none" stroke="#1f5f4f" stroke-width="2"/>
      ${etiquettesX}
    </svg>`;
}

// --- événements ----------------------------------------------------------------

let minuteur;
$('#recherche').addEventListener('input', (e) => {
  clearTimeout(minuteur);                       // anti-rebond : une requête par pause de frappe
  minuteur = setTimeout(() => chargerAssures(e.target.value), 250);
});

// Délégation d'événement : un seul écouteur pour toutes les lignes, présentes et futures.
corpsAssures.addEventListener('click', (e) => {
  const tr = e.target.closest('tr');
  if (!tr) return;
  corpsAssures.querySelectorAll('tr').forEach((l) => l.classList.remove('selection'));
  tr.classList.add('selection');
  assureSelectionne = tr.dataset.id;
  chargerProjection(assureSelectionne, $('#age-retraite').value)
    .catch((err) => { etat.textContent = `Erreur projection : ${err.message}`; });
});

$('#age-retraite').addEventListener('change', () => {
  if (assureSelectionne) {
    chargerProjection(assureSelectionne, $('#age-retraite').value)
      .catch((err) => { etat.textContent = `Erreur projection : ${err.message}`; });
  }
});

document.querySelectorAll('#tableau-assures th[data-tri]').forEach((th) => {
  th.addEventListener('click', () => {
    const colonne = th.dataset.tri;
    triCourant = {
      colonne,
      ascendant: triCourant.colonne === colonne ? !triCourant.ascendant : true
    };
    afficherAssures();
  });
});

chargerAssures();
