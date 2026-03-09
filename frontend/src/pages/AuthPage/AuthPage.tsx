import { z } from 'zod'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useNavigate } from 'react-router-dom'
import styles from './AuthPage.module.css'
import { Button } from '../../shared/ui/Button/Button'
import { Input } from '../../shared/ui/Input/Input'
import { Card } from '../../shared/ui/Card/Card'
import { authApi } from '../../shared/api/auth'
import { useAuth } from '../../shared/auth/AuthContext'

const schema = z.object({
  email: z.string().email('Нужен валидный email'),
  password: z.string().min(8, 'Минимум 8 символов'),
})

type FormValues = z.infer<typeof schema>

export function AuthPage() {
  const nav = useNavigate()
  const auth = useAuth()

  const loginForm = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '' },
  })

  const registerForm = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '' },
  })

  async function doLogin(values: FormValues) {
    const pair = await authApi.login(values.email, values.password)
    auth.loginWithTokens(pair)
    nav('/app', { replace: true })
  }

  async function doRegister(values: FormValues) {
    await authApi.register(values.email, values.password)
    const pair = await authApi.login(values.email, values.password)
    auth.loginWithTokens(pair)
    nav('/app', { replace: true })
  }

  return (
    <div className={styles.page}>
      <div className={styles.hero}>
        <div className={styles.brand}>Stylish</div>
        <div className={styles.subtitle}>
          Учебный UI для теста авторизации, гардероба и генерации лука.
        </div>
      </div>

      <div className={styles.grid}>
        <Card title="Вход">
          <form
            className={styles.form}
            onSubmit={loginForm.handleSubmit(async (v) => {
              loginForm.clearErrors()
              try {
                await doLogin(v)
              } catch (e: any) {
                loginForm.setError('email', { message: e?.message ?? 'Ошибка входа' })
              }
            })}
          >
            <Input
              label="Email"
              placeholder="you@example.com"
              {...loginForm.register('email')}
              error={loginForm.formState.errors.email?.message}
            />
            <Input
              label="Пароль"
              type="password"
              placeholder="********"
              {...loginForm.register('password')}
              error={loginForm.formState.errors.password?.message}
            />
            <Button type="submit" disabled={loginForm.formState.isSubmitting}>
              Войти
            </Button>
          </form>
        </Card>

        <Card title="Регистрация">
          <form
            className={styles.form}
            onSubmit={registerForm.handleSubmit(async (v) => {
              registerForm.clearErrors()
              try {
                await doRegister(v)
              } catch (e: any) {
                registerForm.setError('email', { message: e?.message ?? 'Ошибка регистрации' })
              }
            })}
          >
            <Input
              label="Email"
              placeholder="you@example.com"
              {...registerForm.register('email')}
              error={registerForm.formState.errors.email?.message}
            />
            <Input
              label="Пароль"
              type="password"
              placeholder="минимум 8 символов"
              {...registerForm.register('password')}
              error={registerForm.formState.errors.password?.message}
            />
            <Button type="submit" disabled={registerForm.formState.isSubmitting}>
              Создать аккаунт
            </Button>
          </form>
        </Card>
      </div>
    </div>
  )
}

