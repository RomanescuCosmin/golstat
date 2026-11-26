import { fireEvent, render, screen } from '@testing-library/react';
import { useState } from 'react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { Modal } from './Modal';

function Demo({ onInchide = () => {} }: { onInchide?: () => void }) {
  const [deschis, setDeschis] = useState(false);
  return (
    <>
      <button onClick={() => setDeschis(true)}>Deschide</button>
      <Modal
        deschis={deschis}
        titlu="Autentificare"
        onInchide={() => {
          onInchide();
          setDeschis(false);
        }}
      >
        <input aria-label="Email" />
        <button>Trimite</button>
      </Modal>
    </>
  );
}

describe('Modal', () => {
  beforeEach(() => {
    document.body.innerHTML = '<div id="root"></div>';
  });

  function randeaza(onInchide?: () => void) {
    render(<Demo onInchide={onInchide} />, { container: document.getElementById('root')! });
    screen.getByText('Deschide').focus();
    fireEvent.click(screen.getByText('Deschide'));
  }

  test('aria: dialog modal etichetat de titlu, primul camp in focus', () => {
    randeaza();

    const dialog = screen.getByRole('dialog', { name: 'Autentificare' });
    expect(dialog).toHaveAttribute('aria-modal', 'true');
    expect(screen.getByLabelText('Email')).toHaveFocus();
  });

  test('restul paginii e inert si scroll-ul blocat cat timp e deschis', () => {
    randeaza();

    expect(document.getElementById('root')).toHaveAttribute('inert');
    expect(document.body.style.overflow).toBe('hidden');

    fireEvent.keyDown(document, { key: 'Escape' });
    expect(document.getElementById('root')).not.toHaveAttribute('inert');
    expect(document.body.style.overflow).toBe('');
  });

  test('Escape inchide si focusul revine la buton', () => {
    const onInchide = vi.fn();
    randeaza(onInchide);

    fireEvent.keyDown(document, { key: 'Escape' });

    expect(onInchide).toHaveBeenCalledOnce();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.getByText('Deschide')).toHaveFocus();
  });

  test('Tab pe ultimul element revine la primul (focus tinut inauntru)', () => {
    randeaza();
    screen.getByText('Trimite').focus();

    fireEvent.keyDown(document, { key: 'Tab' });

    expect(screen.getByRole('button', { name: 'Închide' })).toHaveFocus();
  });

  test('Shift+Tab pe primul element sare la ultimul', () => {
    randeaza();
    screen.getByRole('button', { name: 'Închide' }).focus();

    fireEvent.keyDown(document, { key: 'Tab', shiftKey: true });

    expect(screen.getByText('Trimite')).toHaveFocus();
  });

  test('click pe fundal inchide; click in panou nu', () => {
    const onInchide = vi.fn();
    randeaza(onInchide);

    const fundal = screen.getByTestId('modal-fundal');
    fireEvent.mouseDown(screen.getByLabelText('Email'));
    fireEvent.click(fundal);
    expect(onInchide).not.toHaveBeenCalled();

    fireEvent.mouseDown(fundal);
    fireEvent.click(fundal);
    expect(onInchide).toHaveBeenCalledOnce();
  });

  test('butonul Închide', () => {
    const onInchide = vi.fn();
    randeaza(onInchide);

    fireEvent.click(screen.getByRole('button', { name: 'Închide' }));

    expect(onInchide).toHaveBeenCalledOnce();
  });
});
