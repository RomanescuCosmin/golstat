import { useAuth, type ModAutentificare, type StareModal } from '../../auth/AuthProvider';
import { Modal } from '../ui/Modal';
import { FormularAutentificare } from './FormularAutentificare';
import { FormularConfirmareEmail } from './FormularConfirmareEmail';
import { FormularInregistrare } from './FormularInregistrare';
import { FormularParolaNoua } from './FormularParolaNoua';
import { FormularParolaUitata } from './FormularParolaUitata';

const TITLURI: Record<ModAutentificare, string> = {
  autentificare: 'Intră în cont',
  inregistrare: 'Creează un cont',
  'parola-uitata': 'Parolă uitată',
  resetare: 'Alege o parolă nouă',
  confirmare: 'Confirmă adresa de email',
  'schimbare-parola': 'Schimbă parola',
};

const COMUTATOARE: { mod: ModAutentificare; eticheta: string }[] = [
  { mod: 'autentificare', eticheta: 'Autentificare' },
  { mod: 'inregistrare', eticheta: 'Cont nou' },
];

export function AutentificareModal() {
  const { modal, deschideModal, inchideModal } = useAuth();
  if (!modal) return null;

  const cuComutator = modal.mod === 'autentificare' || modal.mod === 'inregistrare';
  return (
    <Modal deschis titlu={TITLURI[modal.mod]} onInchide={inchideModal} cheieContinut={modal.mod}>
      {cuComutator && (
        <div className="mb-5 grid grid-cols-2 gap-1 rounded-full bg-ink2/10 p-1">
          {COMUTATOARE.map(({ mod, eticheta }) => (
            <button
              key={mod}
              type="button"
              aria-pressed={modal.mod === mod}
              onClick={() => deschideModal(mod)}
              className={`h-9 rounded-full text-sm font-semibold transition ${
                modal.mod === mod ? 'bg-primary text-white shadow-card' : 'text-ink2 hover:text-ink'
              }`}
            >
              {eticheta}
            </button>
          ))}
        </div>
      )}
      <Continut key={modal.mod} modal={modal} />
    </Modal>
  );
}

function Continut({ modal }: { modal: StareModal }) {
  switch (modal.mod) {
    case 'autentificare':
      return <FormularAutentificare />;
    case 'inregistrare':
      return <FormularInregistrare />;
    case 'parola-uitata':
      return <FormularParolaUitata />;
    case 'resetare':
      return <FormularParolaNoua varianta="resetare" token={modal.token ?? ''} />;
    case 'confirmare':
      return <FormularConfirmareEmail token={modal.token ?? ''} />;
    case 'schimbare-parola':
      return <FormularParolaNoua varianta="schimbare" />;
  }
}
