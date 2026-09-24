
import { useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'

type UserForm = {
  firstName: string
  lastName: string
  email: string
}

type ApiError = {
  title?: string
  detail?: string
  errors?: Record<string, string>
}

function App() {

  // Données du formulaire
  const [form, setForm] = useState<UserForm>({
    firstName: '',
    lastName: '',
    email: '',
  })

  // Messages et erreurs
  const [message, setMessage] = useState('')
  const [fieldErrors, setFieldErrors] = useState<
    Record<string, string>
  >({})

  const [loading, setLoading] = useState(false)

  // Mise à jour des champs
  function handleChange(
    event: React.ChangeEvent<HTMLInputElement>
  ) {
    const { name, value } = event.target

    setForm(previous => ({
      ...previous,
      [name]: value,
    }))
  }

  // Création de l'utilisateur
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {

    event.preventDefault()

    setMessage('')
    setFieldErrors({})
    setLoading(true)

    try {

      const response = await fetch('/api/users', {
        method: 'POST',

        headers: {
          'Content-Type': 'application/json',
        },

        body: JSON.stringify(form),
      })

      if (response.ok) {

        const user = await response.json()

        setMessage(
          `Utilisateur ${user.firstName} créé avec succès !`
        )

        // Réinitialiser le formulaire
        setForm({
          firstName: '',
          lastName: '',
          email: '',
        })

      } else {

        const error: ApiError = await response.json()

        setMessage(
          error.detail || error.title || 'Une erreur est survenue.'
        )

        setFieldErrors(error.errors || {})
      }

    } catch {

      setMessage(
        'Impossible de communiquer avec le serveur BankFlow.'
      )

    } finally {

      setLoading(false)

    }
  }

  return (
    <div className="app">

      <header className="header">
        <h1>BankFlow</h1>
        <p>Gestion des utilisateurs</p>
      </header>

      <main className="container">

        <h2>Créer un utilisateur</h2>

        <p className="description">
          Renseignez les informations pour créer
          un nouvel utilisateur.
        </p>

        <form
          id="userForm"
          onSubmit={handleSubmit}
          noValidate
        >

          <div className="form-group">

            <label htmlFor="firstName">
              Prénom
            </label>

            <input
              id="firstName"
              name="firstName"
              type="text"
              value={form.firstName}
              onChange={handleChange}
              placeholder="Votre prénom"
            />

            {fieldErrors.firstName && (
  <p
    id="firstName-error"
    className="field-error"
  >
    {fieldErrors.firstName}
  </p>
)}

          </div>

          <div className="form-group">

            <label htmlFor="lastName">
              Nom
            </label>

            <input
              id="lastName"
              name="lastName"
              type="text"
              value={form.lastName}
              onChange={handleChange}
              placeholder="Votre nom"
            />

            {fieldErrors.lastName && (
              <p className="field-error">
                {fieldErrors.lastName}
              </p>
            )}

          </div>

          <div className="form-group">

            <label htmlFor="email">
              Email
            </label>

            <input
              id="email"
              name="email"
              type="email"
              value={form.email}
              onChange={handleChange}
              placeholder="exemple@email.com"
            />

            {fieldErrors.email && (
              <p id="email-error" className="field-error">
                {fieldErrors.email}
              </p>
            )}

          </div>

          <button
            id="createUserButton"
            type="submit"
            disabled={loading}
          >
            {loading
              ? 'Enregistrement...'
              : 'Créer un utilisateur'}
          </button>

        </form>

        {message && (
          <p
            id="message"
            role="status"
            aria-live="polite"
            className="message"
          >
            {message}
          </p>
        )}

      </main>

    </div>
  )
}

export default App