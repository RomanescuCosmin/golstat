import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, getPieteZile } from '../api/client';
import { cotaPiata, meciPiete, pieteZile, ziPiete } from '../test/factories';
import { PietePage } from './PietePage';

vi.mock('../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/client')>()),
  getPieteZile: vi.fn(),
}));

const mockGet = vi.mocked(getPieteZile);

function randeaza() {
  return render(
    <MemoryRouter>
      <PietePage />
    </MemoryRouter>,
  );
}

/** Trei meciuri intr-o zi cu goluri + cornere, ca sa putem comuta intre piete. */
function dateComplete() {
  return pieteZile({
    zile: [
      ziPiete({
        data: '2026-07-21',
        meciuri: [
          meciPiete({
            fixtureId: 1,
            gazde: { id: 1, nume: 'Slaba', logo: null },
            oaspeti: { id: 2, nume: 'Modesta', logo: null },
            piete: [
              cotaPiata({ piata: 'GOLURI_PESTE', linie: 2.5, probabilitate: 0.35 }),
              cotaPiata({ piata: 'CORNERE_PESTE', linie: 7.5, probabilitate: 0.9 }),
            ],
          }),
          meciPiete({
            fixtureId: 2,
            gazde: { id: 3, nume: 'Puternica', logo: null },
            oaspeti: { id: 4, nume: 'Ofensiva', logo: null },
            piete: [
              cotaPiata({ piata: 'GOLURI_PESTE', linie: 2.5, probabilitate: 0.82 }),
              cotaPiata({ piata: 'GOLURI_PESTE', linie: 1.5, probabilitate: 0.95 }),
              cotaPiata({ piata: 'GG', linie: null, probabilitate: 0.7 }),
            ],
          }),
          meciPiete({
            fixtureId: 3,
            gazde: { id: 5, nume: 'Medie', logo: null },
            oaspeti: { id: 6, nume: 'Echilibrata', logo: null },
            piete: [cotaPiata({ piata: 'GOLURI_PESTE', linie: 2.5, probabilitate: 0.55 })],
          }),
        ],
      }),
    ],
  });
}

/** Trei meciuri din trei campionate diferite, pentru filtrul de campionat. */
function dateMultiLiga() {
  const meci = (id: number, ligaId: number, ligaNume: string, gazde: string, p: number) =>
    meciPiete({
      fixtureId: id,
      liga: { id: ligaId, nume: ligaNume, logo: null },
      gazde: { id: id * 10, nume: gazde, logo: null },
      oaspeti: { id: id * 10 + 1, nume: `Adversar ${id}`, logo: null },
      piete: [cotaPiata({ piata: 'GOLURI_PESTE', linie: 2.5, probabilitate: p })],
    });
  return pieteZile({
    zile: [
      ziPiete({
        data: '2026-07-21',
        meciuri: [
          meci(1, 39, 'Premier League', 'Arsenal', 0.9),
          meci(2, 140, 'La Liga', 'Barcelona', 0.86),
          meci(3, 283, 'Liga I', 'Dinamo', 0.4),
        ],
      }),
    ],
  });
}

/** Meniurile din bara de filtre se deschid inainte de alegere. */
async function deschide(titlu: string) {
  const chip = await screen.findByRole('button', { name: titlu });
  if (chip.getAttribute('aria-expanded') !== 'true') await userEvent.click(chip);
}

async function alegeTip(nume: string) {
  await deschide('Tip piață');
  await userEvent.click(screen.getByRole('tab', { name: nume }));
}

async function meniu(titlu: string) {
  await deschide(titlu);
  return screen.getByRole('listbox', { name: titlu });
}

async function alegeOptiune(titlu: string, valoare: string) {
  const optiuni = within(await meniu(titlu)).getAllByRole('option');
  await userEvent.click(optiuni.find((o) => o.dataset.valoare === valoare)!);
}

async function etichete(titlu: string) {
  const lista = within(await meniu(titlu)).getAllByRole('option').map((o) => o.firstElementChild!.textContent);
  await userEvent.keyboard('{Escape}');
  return lista;
}

const chip = (titlu: string) => screen.getByRole('button', { name: titlu });

async function seteazaPrag(valoare: number) {
  await deschide('Șansă minimă');
  fireEvent.change(screen.getByRole('slider', { name: 'Șansă minimă' }), { target: { value: String(valoare) } });
}

async function liga(nume: RegExp) {
  await deschide('Campionate');
  return screen.findByRole('button', { name: nume });
}

describe('PietePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('afiseaza implicit peste 2.5 goluri cu pragul la 30%', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();

    await screen.findByText('Puternica');
    expect(screen.getByRole('button', { name: 'Șansă minimă' })).toHaveTextContent('Șansă ≥ 30%');
    // toate cele 3 sunt >= 35% deci trec pragul implicit
    expect(screen.getByText(/3 meciuri peste 30%/)).toBeInTheDocument();
  });

  it('sorteaza descrescator in interiorul zilei', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    const { container } = randeaza();

    await screen.findByText('Puternica');
    const procente = Array.from(container.querySelectorAll('a'))
      .map((a) => a.textContent?.match(/(\d+)%/)?.[1])
      .filter(Boolean)
      .map(Number);
    expect(procente).toEqual([82, 55, 35]);
  });

  it('slider-ul de prag filtreaza fara sa refaca cererea', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();
    await screen.findByText('Puternica');

    // input[type=range] nu raspunde la type(); schimbam valoarea direct
    await seteazaPrag(60);

    await waitFor(() => expect(screen.queryByText('Slaba')).not.toBeInTheDocument());
    expect(screen.getByText('Puternica')).toBeInTheDocument();
    expect(screen.queryByText('Medie')).not.toBeInTheDocument();
    expect(mockGet).toHaveBeenCalledTimes(1);
  });

  it('schimbarea tipului schimba lista si optiunile din dropdown', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();
    await screen.findByText('Puternica');

    await alegeTip('Cornere');

    // liniile de goluri dispar din dropdown, apar cele de cornere
    const selector = await meniu('Piața');
    expect(within(selector).getByRole('option', { name: 'Peste 7.5' })).toBeInTheDocument();
    expect(within(selector).queryByRole('option', { name: 'Peste 2.5' })).not.toBeInTheDocument();
    // doar meciul 1 are cornere
    expect(screen.getByText('Slaba')).toBeInTheDocument();
    expect(screen.queryByText('Puternica')).not.toBeInTheDocument();
    expect(mockGet).toHaveBeenCalledTimes(1);
  });

  it('piata binara are doar directii in dropdown, fara linii', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();
    await screen.findByText('Puternica');

    await alegeTip('GG');

    const optiuni = await etichete('Piața');
    expect(optiuni).toEqual(['Ambele înscriu', 'Nu ambele']);
    expect(screen.getByText('Puternica')).toBeInTheDocument();
  });

  it('schimbarea liniei din dropdown schimba procentele', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();
    await screen.findByText('Puternica');

    await alegeOptiune('Piața', 'GOLURI_PESTE|1.5');

    expect(screen.getByText('95%')).toBeInTheDocument();
    expect(screen.getByText(/1 meci peste 30%/)).toBeInTheDocument();
  });

  it('fiecare rand arata piata aleasa, cu unitate', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();
    await screen.findByText('Puternica');

    // eticheta lunga tine minte ce inseamna procentul cand derulezi lista
    expect(screen.getAllByText('Peste 2.5 goluri')).toHaveLength(3);

    await alegeTip('Cornere');
    expect(screen.getByText('Peste 7.5 cornere')).toBeInTheDocument();
  });

  it('steluta marcheaza meciul si ramane apasata, fara sa navigheze', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();
    await screen.findByText('Puternica');

    const steluta = screen.getByRole('button', { name: /Salvează Puternica/ });
    expect(steluta).toHaveAttribute('aria-pressed', 'false');

    await userEvent.click(steluta);

    expect(screen.getByRole('button', { name: /Salvează Puternica/ })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    // lista ramane pe loc: marcajul nu e un filtru
    expect(screen.getByText(/3 meciuri peste 30%/)).toBeInTheDocument();
  });

  it('prag imposibil da stare goala, nu lista muta', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();
    await screen.findByText('Puternica');

    await seteazaPrag(100);

    expect(await screen.findByText('Niciun meci peste prag')).toBeInTheDocument();
    expect(screen.queryByText('Puternica')).not.toBeInTheDocument();
  });

  it('fereastra fara meciuri da stare goala', async () => {
    mockGet.mockResolvedValueOnce(pieteZile({ zile: [] }));
    randeaza();
    expect(await screen.findByText('Niciun meci peste prag')).toBeInTheDocument();
  });

  it('eroarea ofera reincercare care reface cererea', async () => {
    mockGet.mockRejectedValueOnce(new ApiError(503, 'Serviciu indisponibil', 'Baza e jos.'));
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();

    expect(await screen.findByText('Serviciu indisponibil')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /Încearcă din nou/i }));

    expect(await screen.findByText('Puternica')).toBeInTheDocument();
    expect(mockGet).toHaveBeenCalledTimes(2);
  });

  it('nu afiseaza NaN/undefined nici pe date la limita', async () => {
    mockGet.mockResolvedValueOnce(
      pieteZile({
        zile: [
          ziPiete({
            meciuri: [
              meciPiete({
                fixtureId: 9,
                gazde: { id: 9, nume: null, logo: null },
                oaspeti: { id: 10, nume: null, logo: null },
                piete: [cotaPiata({ probabilitate: 0.5, cota: 2, esantion: 1 })],
              }),
            ],
          }),
        ],
      }),
    );
    const { container } = randeaza();

    await screen.findByText(/1 meci peste 30%/);
    expect(container.textContent).not.toMatch(/NaN|undefined|Infinity/);
  });

  it('filtreaza pe campionat, cu selectii multiple, fara refetch', async () => {
    mockGet.mockResolvedValueOnce(dateMultiLiga());
    randeaza();
    await screen.findByText('Arsenal');

    // implicit: toate campionatele
    expect((await liga(/^Toate$/))).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByText(/3 meciuri peste 30%/)).toBeInTheDocument();

    await userEvent.click((await liga(/Premier League/)));
    expect(screen.getByText(/1 meci peste 30%/)).toBeInTheDocument();
    expect(screen.getByText('Arsenal')).toBeInTheDocument();
    expect(screen.queryByText('Barcelona')).not.toBeInTheDocument();

    // a doua selectie se ADAUGA
    await userEvent.click((await liga(/La Liga/)));
    expect(screen.getByText(/2 meciuri peste 30%/)).toBeInTheDocument();
    expect(screen.getByText('Arsenal')).toBeInTheDocument();
    expect(screen.getByText('Barcelona')).toBeInTheDocument();
    expect(screen.queryByText('Dinamo')).not.toBeInTheDocument();

    // deselectarea scoate campionatul
    await userEvent.click((await liga(/Premier League/)));
    expect(screen.getByText(/1 meci peste 30%/)).toBeInTheDocument();
    expect(screen.queryByText('Arsenal')).not.toBeInTheDocument();

    expect(mockGet).toHaveBeenCalledTimes(1);
  });

  it('„Toate" reseteaza selectia de campionate', async () => {
    mockGet.mockResolvedValueOnce(dateMultiLiga());
    randeaza();
    await screen.findByText('Arsenal');

    await userEvent.click((await liga(/Premier League/)));
    expect((await liga(/^Toate$/))).toHaveAttribute('aria-pressed', 'false');

    await userEvent.click((await liga(/^Toate$/)));
    expect(screen.getByText(/3 meciuri peste 30%/)).toBeInTheDocument();
    expect((await liga(/^Toate$/))).toHaveAttribute('aria-pressed', 'true');
  });

  it('selectia de campionat se pastreaza cand schimbi piata sau pragul', async () => {
    mockGet.mockResolvedValueOnce(dateMultiLiga());
    randeaza();
    await screen.findByText('Arsenal');

    await userEvent.click((await liga(/Premier League/)));
    await seteazaPrag(10);

    expect((await liga(/Premier League/))).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    expect(screen.queryByText('Barcelona')).not.toBeInTheDocument();
  });

  it('campionatul fara meciuri la pragul curent ramane in lista, cu 0', async () => {
    mockGet.mockResolvedValueOnce(dateMultiLiga());
    randeaza();
    await screen.findByText('Arsenal');

    await seteazaPrag(85);

    // Liga I (0.4) cade sub prag, dar pastila ramane — altfel lista ar sari sub deget
    const pastila = (await liga(/Liga I/));
    expect(pastila).toHaveTextContent('0');
  });

  it('selectand un campionat gol se ajunge la stare goala explicita', async () => {
    mockGet.mockResolvedValueOnce(dateMultiLiga());
    randeaza();
    await screen.findByText('Arsenal');

    await seteazaPrag(85);
    await userEvent.click((await liga(/Liga I/)));

    expect(await screen.findByText('Niciun meci peste prag')).toBeInTheDocument();
    expect(screen.getByText(/campionatele alese/)).toBeInTheDocument();
  });

  it('grupeaza pe zi cu antet propriu', async () => {
    mockGet.mockResolvedValueOnce(
      pieteZile({
        zile: [
          ziPiete({ data: '2026-07-21', meciuri: [meciPiete({ fixtureId: 1 })] }),
          ziPiete({ data: '2026-07-22', meciuri: [meciPiete({ fixtureId: 2 })] }),
        ],
      }),
    );
    const { container } = randeaza();

    await waitFor(() => expect(container.querySelectorAll('section')).toHaveLength(2));
    const sectiuni = container.querySelectorAll('section');
    expect(within(sectiuni[0] as HTMLElement).getByText(/21 iulie 2026/)).toBeInTheDocument();
    expect(within(sectiuni[1] as HTMLElement).getByText(/22 iulie 2026/)).toBeInTheDocument();
  });
});

describe('PietePage — perioada', () => {
  it('perioada aleasa de utilizator ajunge la server', async () => {
    mockGet.mockResolvedValue(pieteZile());
    randeaza();
    await userEvent.click(await screen.findByRole('button', { name: 'Alege perioada' }));
    const zile = screen
      .getAllByRole('button')
      .filter((b) => /^\d{4}-\d{2}-\d{2}$/.test(b.getAttribute('aria-label') ?? '') && !b.hasAttribute('disabled'));
    // aceeasi zi de doua ori = perioada de o zi (merge si in ultima zi a lunii)
    const zi = zile[0].getAttribute('aria-label')!;
    await userEvent.click(zile[0]);
    await userEvent.click(screen.getByRole('button', { name: zi }));
    await waitFor(() => expect(mockGet).toHaveBeenLastCalledWith(3, 'toate', { de: zi, pana: zi }, 'separat'));
  });

  it('comutatorul „Toate meciurile" reface cererea fara regula acasa/deplasare si se retine', async () => {
    localStorage.removeItem('golstat.filtruLocatie');
    mockGet.mockResolvedValue(pieteZile());
    randeaza();
    await waitFor(() => expect(mockGet).toHaveBeenLastCalledWith(3, 'toate', expect.anything(), 'separat'));

    await userEvent.click(screen.getByRole('button', { name: 'Toate meciurile' }));

    await waitFor(() => expect(mockGet).toHaveBeenLastCalledWith(3, 'toate', expect.anything(), 'toate'));
    expect(screen.getByRole('button', { name: 'Toate meciurile' })).toHaveAttribute('aria-pressed', 'true');
    expect(localStorage.getItem('golstat.filtruLocatie')).toBe('toate');
    localStorage.removeItem('golstat.filtruLocatie');
  });
});

/** Un meci cu toate liniile de cornere pe repriza 1 (aceleasi cifre ca la City – Fulham). */
function dateReprize() {
  const r1 = (linie: number, p: number) => cotaPiata({ piata: 'CORNERE_REPRIZA1_PESTE', linie, probabilitate: p });
  return pieteZile({
    zile: [
      ziPiete({
        data: '2026-07-21',
        meciuri: [
          meciPiete({
            fixtureId: 1,
            gazde: { id: 1, nume: 'City', logo: null },
            oaspeti: { id: 2, nume: 'Fulham', logo: null },
            piete: [
              r1(2.5, 0.875), r1(3.5, 0.734), r1(4.5, 0.558), r1(5.5, 0.383), r1(6.5, 0.237),
              cotaPiata({ piata: 'CORNERE_REPRIZA1_SUB', linie: 6.5, probabilitate: 0.763 }),
            ],
          }),
        ],
      }),
    ],
  });
}

describe('PietePage — cornere pe reprize', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('piata „sub" pe repriza se poate alege si arata procentul serverului', async () => {
    mockGet.mockResolvedValue(dateReprize());
    randeaza();
    await screen.findByRole('button', { name: 'Tip piață' });
    await alegeTip('Cornere pe reprize');
    await alegeOptiune('Piața', 'CORNERE_REPRIZA1_SUB|6.5');

    expect(screen.getByText('76%')).toBeInTheDocument();
    expect(screen.getByText('Repriza 1 sub 6.5 cornere')).toBeInTheDocument();
  });

  it('gol in prima / a doua: peste 0.5 si 1.5 pe fiecare repriza', async () => {
    const g = (piata: 'GOLURI_REPRIZA1_PESTE' | 'GOLURI_REPRIZA2_PESTE', linie: number, p: number) =>
      cotaPiata({ piata, linie, probabilitate: p });
    mockGet.mockResolvedValue(
      pieteZile({
        zile: [ziPiete({ data: '2026-07-21', meciuri: [meciPiete({ fixtureId: 1, piete: [
          g('GOLURI_REPRIZA1_PESTE', 0.5, 0.71), g('GOLURI_REPRIZA1_PESTE', 1.5, 0.33),
          g('GOLURI_REPRIZA2_PESTE', 0.5, 0.82), g('GOLURI_REPRIZA2_PESTE', 1.5, 0.47),
        ] })] })],
      }),
    );
    randeaza();
    await screen.findByRole('button', { name: 'Tip piață' });
    await alegeTip('Gol în prima / a doua');

    expect(screen.getByText('71%')).toBeInTheDocument();
    expect(screen.getByText('Prima repriză peste 0.5 goluri')).toBeInTheDocument();

    await alegeOptiune('Piața', 'GOLURI_REPRIZA2_PESTE|1.5');
    expect(screen.getByText('47%')).toBeInTheDocument();
    expect(screen.getByText('A doua repriză peste 1.5 goluri')).toBeInTheDocument();
  });

  it('interval: implicit peste 2.5 si sub 6.5 = 64%, nu media (81%)', async () => {
    mockGet.mockResolvedValue(dateReprize());
    randeaza();
    await alegeTip('Interval cornere pe repriză');

    expect(chip('Peste')).toHaveTextContent('Peste 2.5');
    expect(chip('Sub')).toHaveTextContent('Sub 6.5');
    expect(screen.getByText('64%')).toBeInTheDocument();
    expect(screen.queryByText('81%')).not.toBeInTheDocument();
    expect(screen.getByText('Repriza 1: peste 2.5 și sub 6.5 cornere')).toBeInTheDocument();
  });

  it('interval: schimbi limitele si procentul se recalculeaza; „sub" ofera doar linii peste „peste"', async () => {
    mockGet.mockResolvedValue(dateReprize());
    randeaza();
    await alegeTip('Interval cornere pe repriză');

    await alegeOptiune('Peste', '3.5');
    await alegeOptiune('Sub', '5.5');

    // 0.734 − 0.383 = 35.1%
    expect(screen.getByText('35%')).toBeInTheDocument();
    expect(screen.getByText('Repriza 1: peste 3.5 și sub 5.5 cornere')).toBeInTheDocument();
    const optiuniSub = await etichete('Sub');
    expect(optiuniSub).toEqual(['4.5', '5.5', '6.5']);
    const optiuniPeste = await etichete('Peste');
    expect(optiuniPeste).toEqual(['2.5', '3.5', '4.5']);
    expect(mockGet).toHaveBeenCalledTimes(1);
  });

  it('interval: schimbarea reprizei pastreaza limitele alese', async () => {
    mockGet.mockResolvedValue(dateReprize());
    randeaza();
    await alegeTip('Interval cornere pe repriză');
    await alegeOptiune('Sub', '5.5');
    await alegeOptiune('Piața', 'CORNERE_REPRIZA2_INTERVAL|');

    expect(chip('Sub')).toHaveTextContent('Sub 5.5');
    // meciul n-are date pe repriza 2 → stare goala, nu procent fals
    expect(screen.queryByText('City')).not.toBeInTheDocument();
  });

  it('la „Înapoi" (re-montare) pastreaza piata, intervalul si pragul', async () => {
    mockGet.mockResolvedValue(dateReprize());
    const { unmount } = randeaza();
    await alegeTip('Interval cornere pe repriză');
    await alegeOptiune('Peste', '3.5');
    await seteazaPrag(20);
    unmount();

    randeaza();
    await screen.findByText('City');
    expect(screen.getByRole('button', { name: 'Tip piață' })).toHaveTextContent('Interval cornere pe repriză');
    expect(chip('Peste')).toHaveTextContent('Peste 3.5');
    expect(chip('Sub')).toHaveTextContent('Sub 6.5');
    expect(screen.getByRole('button', { name: 'Șansă minimă' })).toHaveTextContent('Șansă ≥ 20%');
  });
});

describe('PietePage — bara compacta de filtre', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('chip-urile arata alegerea curenta; meniurile sunt inchise implicit', async () => {
    mockGet.mockResolvedValueOnce(dateMultiLiga());
    randeaza();
    await screen.findByText('Arsenal');

    expect(screen.getByRole('button', { name: 'Tip piață' })).toHaveTextContent('Goluri');
    expect(screen.getByRole('button', { name: 'Șansă minimă' })).toHaveTextContent('Șansă ≥ 30%');
    expect(screen.getByRole('button', { name: 'Campionate' })).toHaveTextContent('Toate campionatele');
    expect(screen.queryByRole('slider')).not.toBeInTheDocument();
    expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
  });

  it('alegerea tipului inchide meniul si schimba eticheta', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();
    await screen.findByText('Puternica');

    await alegeTip('Cornere');
    expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Tip piață' })).toHaveTextContent('Cornere');
  });

  it('Escape si click in afara inchid meniul', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();
    await screen.findByText('Puternica');

    await deschide('Șansă minimă');
    expect(screen.getByRole('slider')).toBeInTheDocument();
    await userEvent.keyboard('{Escape}');
    expect(screen.queryByRole('slider')).not.toBeInTheDocument();

    await deschide('Șansă minimă');
    await userEvent.click(screen.getByText('Puternica'));
    expect(screen.queryByRole('slider')).not.toBeInTheDocument();
  });

  it('pragurile rapide seteaza pragul dintr-un click', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();
    await screen.findByText('Puternica');

    await deschide('Șansă minimă');
    await userEvent.click(screen.getByRole('button', { name: '80%' }));
    expect(screen.getByRole('button', { name: 'Șansă minimă' })).toHaveTextContent('Șansă ≥ 80%');
    expect(screen.getByText('Puternica')).toBeInTheDocument();
    expect(screen.queryByText('Medie')).not.toBeInTheDocument();
  });

  it('campionatele alese apar pe chip (unul = numele, mai multe = numarul)', async () => {
    mockGet.mockResolvedValueOnce(dateMultiLiga());
    randeaza();
    await screen.findByText('Arsenal');

    await userEvent.click(await liga(/Premier League/));
    expect(screen.getByRole('button', { name: 'Campionate' })).toHaveTextContent('Premier League');
    await userEvent.click(await liga(/La Liga/));
    expect(screen.getByRole('button', { name: 'Campionate' })).toHaveTextContent('2 campionate');
  });

  it('interval goluri: implicit 1–3, alegi „de la” / „până la” în numere întregi', async () => {
    const g = (linie: number, p: number) => cotaPiata({ piata: 'GOLURI_PESTE', linie, probabilitate: p });
    mockGet.mockResolvedValue(pieteZile({ zile: [ziPiete({ data: '2026-07-21', meciuri: [meciPiete({
      fixtureId: 1, gazde: { id: 1, nume: 'City', logo: null }, oaspeti: { id: 2, nume: 'Fulham', logo: null },
      piete: [g(0.5, 0.92), g(1.5, 0.75), g(2.5, 0.52), g(3.5, 0.3), g(4.5, 0.14), g(5.5, 0.05), g(6.5, 0.02)],
    })] })] }));
    randeaza();
    await alegeTip('Interval goluri');

    expect(chip('De la')).toHaveTextContent('De la 1');
    expect(chip('Până la')).toHaveTextContent('Până la 3');
    expect(screen.getByText('62%')).toBeInTheDocument();
    expect(screen.getByText('Între 1 și 3 goluri')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Piața' })).toBeNull();

    await alegeOptiune('De la', '2.5');
    await alegeOptiune('Până la', '6.5');
    // 3–6 goluri = 0.52 − 0.02
    expect(screen.getByText('50%')).toBeInTheDocument();
    expect(screen.getByText('Între 3 și 6 goluri')).toBeInTheDocument();
  });
});

describe('PietePage — interval orar', () => {
  it('„În 3 ore" păstrează doar meciurile care încep în următoarele 3 ore; „Oricând" le readuce', async () => {
    const user = userEvent.setup();
    const peste = (ore: number) => new Date(Date.now() + ore * 60 * 60 * 1000).toISOString();
    const meci = (id: number, nume: string, ore: number) =>
      meciPiete({
        fixtureId: id,
        kickoff: peste(ore),
        gazde: { id: id * 10, nume, logo: null },
        oaspeti: { id: id * 10 + 1, nume: `Adversar ${id}`, logo: null },
        piete: [cotaPiata({ piata: 'GOLURI_PESTE', linie: 2.5, probabilitate: 0.8 })],
      });
    sessionStorage.clear();
    mockGet.mockResolvedValue(
      pieteZile({ zile: [ziPiete({ meciuri: [meci(1, 'Curand', 1), meci(2, 'Peste4', 4), meci(3, 'Peste7', 7)] })] }),
    );
    randeaza();
    await screen.findByText('Curand');

    const grup = screen.getByRole('group', { name: 'Interval orar' });
    await user.click(within(grup).getByRole('button', { name: 'În 3 ore' }));
    expect(screen.getByText('Curand')).toBeInTheDocument();
    expect(screen.queryByText('Peste4')).toBeNull();
    expect(screen.queryByText('Peste7')).toBeNull();

    await user.click(within(grup).getByRole('button', { name: 'În 5 ore' }));
    expect(screen.getByText('Peste4')).toBeInTheDocument();
    expect(screen.queryByText('Peste7')).toBeNull();

    await user.click(within(grup).getByRole('button', { name: 'În 8 ore' }));
    expect(screen.getByText('Peste7')).toBeInTheDocument();

    await user.click(within(grup).getByRole('button', { name: 'Oricând' }));
    expect(within(grup).getByRole('button', { name: 'Oricând' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByText('Peste7')).toBeInTheDocument();
  });
});
