# Discord Password Rotator

App Android locale per generare una password casuale di 32 caratteri e facilitare il cambio password di Discord.

## Sicurezza
- Nessun permesso INTERNET.
- Nessun accesso a token o API Discord.
- Password generata con SecureRandom.
- Nessun salvataggio su disco.
- Screenshot e anteprima Recenti bloccati con FLAG_SECURE.
- Clipboard marcata come sensibile.
- Cancellazione automatica della clipboard dopo circa 5 minuti mentre il processo dell'app resta attivo.

## Flusso con Samsung Pass
1. Genera una password.
2. Premi "Copia e apri Discord".
3. Usa Samsung Pass per la password attuale.
4. Incolla la nuova password.
5. Conferma Discord/MFA.
6. Accetta l'eventuale aggiornamento proposto da Samsung Pass.
7. Torna nell'app e cancella password e appunti.

## Nota
Il deep-link diretto alla schermata di cambio password di Discord non è un'API pubblica stabile. L'app include un fallback alla schermata Account.
