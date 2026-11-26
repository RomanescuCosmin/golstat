import { describe, expect, test } from 'vitest';
import { antetRuta, sectiuneActiva } from './navigatie';

describe('navigatie', () => {
  test('sectiunea activa acopera si rutele de detaliu', () => {
    expect(sectiuneActiva('/')?.eticheta).toBe('Meciuri');
    expect(sectiuneActiva('/echipa/33')?.eticheta).toBe('Echipe');
    expect(sectiuneActiva('/jucator/909')?.eticheta).toBe('Jucători');
    expect(sectiuneActiva('/competitie/283')?.eticheta).toBe('Competiții');
    expect(sectiuneActiva('/piete')?.eticheta).toBe('Piețe');
  });

  test('antetul: detaliile au „inapoi", sectiunile au titlul lor', () => {
    expect(antetRuta('/meci/10/centru')).toEqual({ titlu: 'Centru meci', detaliu: true });
    expect(antetRuta('/meci/10')).toEqual({ titlu: 'Previzualizare', detaliu: true });
    expect(antetRuta('/live')).toEqual({ titlu: 'Live', detaliu: false });
    expect(antetRuta('/profil')).toEqual({ titlu: 'Profil', detaliu: false });
    expect(antetRuta('/necunoscut').titlu).toBe('golstat');
  });
});
