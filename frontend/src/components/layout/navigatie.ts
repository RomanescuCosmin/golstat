import type { ComponentType, SVGProps } from 'react';
import { matchPath } from 'react-router-dom';
import {
  IconCalendar,
  IconChart,
  IconDiamond,
  IconGear,
  IconLive,
  IconMeciuri,
  IconShield,
  IconStar,
  IconStiri,
  IconTrophy,
  IconUser,
} from '../ui/icons';

type Icon = ComponentType<SVGProps<SVGSVGElement>>;

export interface SectiuneNav {
  eticheta: string;
  icon: Icon;
  to: string;
  end?: boolean;
  /** Rutele care tin de sectiune (pentru starea activa si titlul din header). */
  rute: string[];
}

/** Sursa unica a navigatiei: sidebar-ul de desktop, bara de jos si meniul „Mai mult". */
export const SECTIUNI: SectiuneNav[] = [
  { eticheta: 'Meciuri', icon: IconMeciuri, to: '/', end: true, rute: ['/', '/meci/:fixtureId', '/meci/:fixtureId/centru'] },
  { eticheta: 'Live', icon: IconLive, to: '/live', rute: ['/live'] },
  { eticheta: 'Program', icon: IconCalendar, to: '/program', rute: ['/program'] },
  { eticheta: 'Competiții', icon: IconTrophy, to: '/competitie/39', rute: ['/competitie/:leagueId'] },
  { eticheta: 'Statistici', icon: IconChart, to: '/statistici', rute: ['/statistici'] },
  { eticheta: 'Piețe', icon: IconDiamond, to: '/piete', rute: ['/piete'] },
  { eticheta: 'Echipe', icon: IconShield, to: '/echipe', rute: ['/echipe', '/echipa/:teamId'] },
  { eticheta: 'Jucători', icon: IconUser, to: '/jucatori', rute: ['/jucatori', '/jucator/:playerId'] },
  { eticheta: 'Știri', icon: IconStiri, to: '/stiri', rute: ['/stiri'] },
  { eticheta: 'Favorite', icon: IconStar, to: '/favorite', rute: ['/favorite'] },
  { eticheta: 'Setări', icon: IconGear, to: '/setari', rute: ['/setari'] },
];

/** Sectiunile cu loc fix in bara de jos de pe mobil; restul stau in meniul „Mai mult". */
export const IN_BARA_JOS = ['Meciuri', 'Live', 'Piețe', 'Favorite'];

/** Titlurile paginilor de detaliu (au buton „inapoi" pe mobil). */
const DETALII: { ruta: string; titlu: string }[] = [
  { ruta: '/meci/:fixtureId/centru', titlu: 'Centru meci' },
  { ruta: '/meci/:fixtureId', titlu: 'Previzualizare' },
  { ruta: '/echipa/:teamId', titlu: 'Echipă' },
  { ruta: '/jucator/:playerId', titlu: 'Jucător' },
];

export function sectiuneActiva(pathname: string): SectiuneNav | undefined {
  return SECTIUNI.find((s) => s.rute.some((r) => matchPath({ path: r, end: true }, pathname)));
}

export interface AntetRuta {
  titlu: string;
  /** Pagina e un detaliu (meci / echipa / jucator) → header-ul mobil arata „inapoi". */
  detaliu: boolean;
}

export function antetRuta(pathname: string): AntetRuta {
  const detaliu = DETALII.find((d) => matchPath({ path: d.ruta, end: true }, pathname));
  if (detaliu) return { titlu: detaliu.titlu, detaliu: true };
  if (matchPath({ path: '/profil', end: true }, pathname)) return { titlu: 'Profil', detaliu: false };
  return { titlu: sectiuneActiva(pathname)?.eticheta ?? 'golstat', detaliu: false };
}
