import { useState, type FormEvent } from 'react'
import './App.css'

type User = {
  id: number
  firstName: string
  lastName: string
  email: string
}

type Account = {
  id: number
  accountNumber: string
  balance: number
  currency: 'MAD' | 'EUR'
  status: 'ACTIVE' | 'BLOCKED'
  userId: number
  createdAt: string
}

type Transaction = {
  id: number
  type: 'DEPOSIT' | 'WITHDRAWAL'
  amount: number
  balanceAfter: number
  accountId: number
  createdAt: string
}

type ApiProblem = {
  title?: string
  detail?: string
  errors?: Record<string, string>
}

function App() {
  // =====================================================
  // USER
  // =====================================================

  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [email, setEmail] = useState('')

  const [currentUser, setCurrentUser] = useState<User | null>(null)
  const [existingUserId, setExistingUserId] = useState('')

  const [fieldErrors, setFieldErrors] =
    useState<Record<string, string>>({})

  // =====================================================
  // ACCOUNT
  // =====================================================

  const [accounts, setAccounts] = useState<Account[]>([])
  const [currency, setCurrency] =
    useState<'MAD' | 'EUR'>('MAD')

  const [amounts, setAmounts] =
    useState<Record<number, string>>({})

  // =====================================================
  // TRANSACTIONS
  // =====================================================

  const [transactions, setTransactions] =
    useState<Transaction[]>([])

  const [historyAccountId, setHistoryAccountId] =
    useState<number | null>(null)

  // =====================================================
  // GLOBAL MESSAGE
  // =====================================================

  const [message, setMessage] = useState('')
  const [isError, setIsError] = useState(false)


  // =====================================================
  // HELPERS
  // =====================================================

  async function getApiError(
    response: Response
  ): Promise<ApiProblem> {

    try {
      return await response.json()
    } catch {
      return {
        title: 'Erreur',
        detail: 'Une erreur inattendue est survenue.'
      }
    }
  }


  function showSuccess(text: string) {
    setMessage(text)
    setIsError(false)
  }


  function showError(text: string) {
    setMessage(text)
    setIsError(true)
  }


  // =====================================================
  // CREATE USER
  // =====================================================

  async function createUser(event: FormEvent) {
    event.preventDefault()

    setMessage('')
    setFieldErrors({})

    try {
      const response = await fetch('/api/users', {
        method: 'POST',

        headers: {
          'Content-Type': 'application/json'
        },

        body: JSON.stringify({
          firstName,
          lastName,
          email
        })
      })

      if (!response.ok) {
        const problem = await getApiError(response)

        setFieldErrors(problem.errors ?? {})

        showError(
          problem.detail ??
          problem.title ??
          'Impossible de créer utilisateur.'
        )

        return
      }

      const user: User = await response.json()

      setCurrentUser(user)
      setExistingUserId(String(user.id))

      setFirstName('')
      setLastName('')
      setEmail('')
      setAccounts([])
      setTransactions([])
      setHistoryAccountId(null)

      showSuccess(
        `Utilisateur ${user.firstName} créé avec succès !`
      )

      await loadAccounts(user.id)

    } catch {
      showError(
        'Impossible de communiquer avec le serveur BankFlow.'
      )
    }
  }


  // =====================================================
  // LOAD EXISTING USER
  // =====================================================

  async function loadExistingUser() {

    if (!existingUserId) {
      showError('Renseigne un identifiant utilisateur.')
      return
    }

    try {
      const response = await fetch(
        `/api/users/${existingUserId}`
      )

      if (!response.ok) {
        const problem = await getApiError(response)

        showError(
          problem.detail ??
          'Utilisateur introuvable.'
        )

        return
      }

      const user: User = await response.json()

      setCurrentUser(user)
      setTransactions([])
      setHistoryAccountId(null)

      await loadAccounts(user.id)

      showSuccess(
        `Utilisateur ${user.firstName} chargé.`
      )

    } catch {
      showError(
        'Impossible de communiquer avec le serveur BankFlow.'
      )
    }
  }


  // =====================================================
  // LOAD ACCOUNTS
  // =====================================================

  async function loadAccounts(userId: number) {

    const response = await fetch(
      `/api/accounts/user/${userId}`
    )

    if (!response.ok) {
      const problem = await getApiError(response)

      throw new Error(
        problem.detail ??
        'Impossible de charger les comptes.'
      )
    }

    const data: Account[] = await response.json()

    setAccounts(data)
  }


  // =====================================================
  // CREATE ACCOUNT
  // =====================================================

  async function createAccount() {

    if (!currentUser) {
      showError(
        'Sélectionne ou crée d’abord un utilisateur.'
      )
      return
    }

    try {
      const response = await fetch('/api/accounts', {
        method: 'POST',

        headers: {
          'Content-Type': 'application/json'
        },

        body: JSON.stringify({
          userId: currentUser.id,
          currency
        })
      })

      if (!response.ok) {
        const problem = await getApiError(response)

        showError(
          problem.detail ??
          problem.title ??
          'Impossible de créer le compte.'
        )

        return
      }

      const account: Account =
        await response.json()

      await loadAccounts(currentUser.id)

      showSuccess(
        `Compte ${account.currency} créé avec succès.`
      )

    } catch {
      showError(
        'Impossible de communiquer avec le serveur BankFlow.'
      )
    }
  }


  // =====================================================
  // UPDATE AMOUNT
  // =====================================================

  function updateAmount(
    accountId: number,
    value: string
  ) {

    setAmounts(previous => ({
      ...previous,
      [accountId]: value
    }))
  }


  // =====================================================
  // DEPOSIT
  // =====================================================

  async function deposit(account: Account) {

    const amount = amounts[account.id]

    if (!amount) {
      showError('Renseigne un montant.')
      return
    }

    try {
      const response = await fetch(
        `/api/accounts/${account.id}/deposit`,
        {
          method: 'POST',

          headers: {
            'Content-Type': 'application/json'
          },

          body: JSON.stringify({
            amount: Number(amount)
          })
        }
      )

      if (!response.ok) {
        const problem = await getApiError(response)

        showError(
          problem.errors?.amount ??
          problem.detail ??
          problem.title ??
          'Dépôt impossible.'
        )

        return
      }

      const updatedAccount: Account =
        await response.json()

      setAmounts(previous => ({
        ...previous,
        [account.id]: ''
      }))

      if (currentUser) {
        await loadAccounts(currentUser.id)
      }

      if (historyAccountId === account.id) {
        await loadTransactions(account.id)
      }

      showSuccess(
        `Dépôt effectué. Nouveau solde : ` +
        `${updatedAccount.balance.toFixed(2)} ` +
        `${updatedAccount.currency}`
      )

    } catch {
      showError(
        'Impossible de communiquer avec le serveur BankFlow.'
      )
    }
  }


  // =====================================================
  // WITHDRAW
  // =====================================================

  async function withdraw(account: Account) {

    const amount = amounts[account.id]

    if (!amount) {
      showError('Renseigne un montant.')
      return
    }

    try {
      const response = await fetch(
        `/api/accounts/${account.id}/withdraw`,
        {
          method: 'POST',

          headers: {
            'Content-Type': 'application/json'
          },

          body: JSON.stringify({
            amount: Number(amount)
          })
        }
      )

      if (!response.ok) {
        const problem = await getApiError(response)

        showError(
          problem.errors?.amount ??
          problem.detail ??
          problem.title ??
          'Retrait impossible.'
        )

        return
      }

      const updatedAccount: Account =
        await response.json()

      setAmounts(previous => ({
        ...previous,
        [account.id]: ''
      }))

      if (currentUser) {
        await loadAccounts(currentUser.id)
      }

      if (historyAccountId === account.id) {
        await loadTransactions(account.id)
      }

      showSuccess(
        `Retrait effectué. Nouveau solde : ` +
        `${updatedAccount.balance.toFixed(2)} ` +
        `${updatedAccount.currency}`
      )

    } catch {
      showError(
        'Impossible de communiquer avec le serveur BankFlow.'
      )
    }
  }


  // =====================================================
  // TRANSACTION HISTORY
  // =====================================================

  async function loadTransactions(
    accountId: number
  ) {

    try {
      const response = await fetch(
        `/api/accounts/${accountId}/transactions`
      )

      if (!response.ok) {
        const problem = await getApiError(response)

        showError(
          problem.detail ??
          'Impossible de charger historique.'
        )

        return
      }

      const data: Transaction[] =
        await response.json()

      setTransactions(data)
      setHistoryAccountId(accountId)

    } catch {
      showError(
        'Impossible de communiquer avec le serveur BankFlow.'
      )
    }
  }


  // =====================================================
  // UI
  // =====================================================

  return (
    <main className="app">

      <header className="header">
        <div>
          <h1>BankFlow</h1>
          <p>Banking QA Automation Demo</p>
        </div>
      </header>


      {message && (
        <div
          id="message"
          className={
            isError
              ? 'message error'
              : 'message success'
          }
        >
          {message}
        </div>
      )}


      {/* ============================================= */}
      {/* USER CREATION */}
      {/* ============================================= */}

      <section className="panel">

        <h2>Créer un utilisateur</h2>

        <form
          onSubmit={createUser}
          noValidate
          className="form"
        >

          <div className="field">
            <label htmlFor="firstName">
              Prénom
            </label>

            <input
              id="firstName"
              value={firstName}
              onChange={event =>
                setFirstName(event.target.value)
              }
            />

            {fieldErrors.firstName && (
              <span
                id="firstName-error"
                className="field-error"
              >
                {fieldErrors.firstName}
              </span>
            )}
          </div>


          <div className="field">
            <label htmlFor="lastName">
              Nom
            </label>

            <input
              id="lastName"
              value={lastName}
              onChange={event =>
                setLastName(event.target.value)
              }
            />
          </div>


          <div className="field">
            <label htmlFor="email">
              Email
            </label>

            <input
              id="email"
              value={email}
              onChange={event =>
                setEmail(event.target.value)
              }
            />

            {fieldErrors.email && (
              <span
                id="email-error"
                className="field-error"
              >
                {fieldErrors.email}
              </span>
            )}
          </div>


          <button
            id="createUserButton"
            type="submit"
          >
            Créer utilisateur
          </button>

        </form>

      </section>


      {/* ============================================= */}
      {/* LOAD USER */}
      {/* ============================================= */}

      <section className="panel">

        <h2>Utilisateur existant</h2>

        <div className="inline-form">

          <input
            id="existingUserId"
            type="number"
            min="1"
            placeholder="ID utilisateur"
            value={existingUserId}
            onChange={event =>
              setExistingUserId(event.target.value)
            }
          />

          <button
            id="loadUserButton"
            type="button"
            onClick={loadExistingUser}
          >
            Charger
          </button>

        </div>

      </section>


      {/* ============================================= */}
      {/* ACTIVE USER */}
      {/* ============================================= */}

      {currentUser && (

        <>

          <section
            id="activeUser"
            className="panel user-panel"
          >

            <h2>Utilisateur actif</h2>

            <strong>
              {currentUser.firstName}{' '}
              {currentUser.lastName}
            </strong>

            <span>{currentUser.email}</span>

            <span>
              ID : {currentUser.id}
            </span>

          </section>


          {/* ========================================= */}
          {/* CREATE ACCOUNT */}
          {/* ========================================= */}

          <section className="panel">

            <h2>Créer un compte bancaire</h2>

            <div className="inline-form">

              <select
                id="accountCurrency"
                value={currency}
                onChange={event =>
                  setCurrency(
                    event.target.value as
                      'MAD' | 'EUR'
                  )
                }
              >

                <option value="MAD">
                  MAD
                </option>

                <option value="EUR">
                  EUR
                </option>

              </select>

              <button
                id="createAccountButton"
                type="button"
                onClick={createAccount}
              >
                Créer le compte
              </button>

            </div>

          </section>


          {/* ========================================= */}
          {/* ACCOUNTS */}
          {/* ========================================= */}

          <section className="panel">

            <h2>Mes comptes</h2>

            <div
              id="accountsList"
              className="accounts-grid"
            >

              {accounts.length === 0 && (
                <p>Aucun compte bancaire.</p>
              )}


              {accounts.map(account => (

                <article
                  key={account.id}
                  className="account-card"
                  data-testid="account-card"
                >

                  <div className="account-header">

                    <div>
                      <span className="currency">
                        {account.currency}
                      </span>

                      <span
                        className={
                          account.status === 'ACTIVE'
                            ? 'status active'
                            : 'status blocked'
                        }
                      >
                        {account.status}
                      </span>
                    </div>

                    <strong className="balance">
                      {Number(account.balance)
                        .toFixed(2)}
                      {' '}
                      {account.currency}
                    </strong>

                  </div>


                  <p className="account-number">
                    {account.accountNumber}
                  </p>


                  <input
                    id={`amount-${account.id}`}
                    type="number"
                    min="0"
                    step="0.01"
                    placeholder="Montant"
                    value={amounts[account.id] ?? ''}
                    onChange={event =>
                      updateAmount(
                        account.id,
                        event.target.value
                      )
                    }
                  />


                  <div className="account-actions">

                    <button
                      data-testid="deposit-button"
                      type="button"
                      onClick={() =>
                        deposit(account)
                      }
                    >
                      Déposer
                    </button>

                    <button
                      data-testid="withdraw-button"
                      type="button"
                      onClick={() =>
                        withdraw(account)
                      }
                    >
                      Retirer
                    </button>

                    <button
                      data-testid="history-button"
                      type="button"
                      className="secondary"
                      onClick={() =>
                        loadTransactions(
                          account.id
                        )
                      }
                    >
                      Historique
                    </button>

                  </div>

                </article>

              ))}

            </div>

          </section>


          {/* ========================================= */}
          {/* HISTORY */}
          {/* ========================================= */}

          {historyAccountId !== null && (

            <section
              id="transaction-history"
              className="panel"
            >

              <h2>Historique des transactions</h2>

              {transactions.length === 0 ? (

                <p>Aucune transaction.</p>

              ) : (

                <div className="transactions">

                  {transactions.map(transaction => (

                    <div
                      key={transaction.id}
                      className="transaction"
                      data-testid="transaction-row"
                    >

                      <div>

                        <strong>
                          {transaction.type}
                        </strong>

                        <span>
                          Solde après opération :
                          {' '}
                          {Number(
                            transaction.balanceAfter
                          ).toFixed(2)}
                        </span>

                      </div>


                      <span
                        className={
                          transaction.type ===
                          'DEPOSIT'
                            ? 'transaction-amount deposit'
                            : 'transaction-amount withdrawal'
                        }
                      >

                        {transaction.type ===
                        'DEPOSIT'
                          ? '+'
                          : '-'}

                        {Number(
                          transaction.amount
                        ).toFixed(2)}

                      </span>

                    </div>

                  ))}

                </div>

              )}

            </section>

          )}

        </>

      )}

    </main>
  )
}

export default App