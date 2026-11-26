export type LinkEmail = { tip: 'confirmare' | 'resetare'; token: string } | null;

const MODEL = /^#(confirmare|resetare)=([A-Za-z0-9_-]{20,64})$/;

/**
 * Linkurile din email poarta tokenul in fragment (`/#resetare=...`). Il citim o singura data, la pornire
 * (inainte de render, din cauza StrictMode), si il scoatem imediat din bara de adrese si din istoric.
 */
export function citesteSiCurataLinkEmail(loc: Location = window.location, istoric: History = window.history): LinkEmail {
  const fragment = loc.hash;
  if (!fragment.startsWith('#confirmare=') && !fragment.startsWith('#resetare=')) return null;
  istoric.replaceState(istoric.state, '', loc.pathname + loc.search);
  const potrivire = MODEL.exec(fragment);
  return potrivire ? { tip: potrivire[1] as 'confirmare' | 'resetare', token: potrivire[2] } : null;
}
