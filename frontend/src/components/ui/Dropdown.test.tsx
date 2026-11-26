import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { describe, expect, test } from 'vitest';
import { Dropdown } from './Dropdown';

function Proba() {
  const [v, setV] = useState('b');
  return (
    <Dropdown
      eticheta="Alege"
      valoare={v}
      onSchimba={setV}
      optiuni={[
        { valoare: 'a', eticheta: 'Alfa' },
        { valoare: 'b', eticheta: 'Beta' },
        { valoare: 'c', eticheta: 'Gama' },
      ]}
    />
  );
}

describe('Dropdown', () => {
  test('click deschide lista, alegerea o inchide si schimba valoarea', async () => {
    const user = userEvent.setup();
    render(<Proba />);
    const buton = screen.getByRole('combobox', { name: 'Alege' });
    expect(buton).toHaveTextContent('Beta');
    expect(screen.queryByRole('listbox')).toBeNull();

    await user.click(buton);
    expect(screen.getByRole('option', { name: 'Beta' })).toHaveAttribute('aria-selected', 'true');
    await user.click(screen.getByRole('option', { name: 'Gama' }));

    expect(buton).toHaveTextContent('Gama');
    expect(screen.queryByRole('listbox')).toBeNull();
  });

  test('tastatura: sageata deschide, sagetile muta, Enter alege, Escape inchide', async () => {
    const user = userEvent.setup();
    render(<Proba />);
    const buton = screen.getByRole('combobox', { name: 'Alege' });
    buton.focus();

    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('listbox')).toBeInTheDocument();
    await user.keyboard('{ArrowUp}{Enter}');
    expect(buton).toHaveTextContent('Alfa');

    await user.keyboard('{ArrowDown}{Escape}');
    expect(screen.queryByRole('listbox')).toBeNull();
    expect(buton).toHaveTextContent('Alfa');
  });
});
