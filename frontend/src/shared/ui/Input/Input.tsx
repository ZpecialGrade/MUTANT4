import React from 'react'
import styles from './Input.module.css'

type Props = React.InputHTMLAttributes<HTMLInputElement> & {
  label?: string
  hint?: string
  error?: string
}

export function Input({ label, hint, error, className, ...rest }: Props) {
  return (
    <label className={[styles.wrap, className].filter(Boolean).join(' ')}>
      {label ? <div className={styles.label}>{label}</div> : null}
      <input className={[styles.input, error ? styles.inputError : ''].join(' ')} {...rest} />
      {error ? <div className={styles.error}>{error}</div> : hint ? <div className={styles.hint}>{hint}</div> : null}
    </label>
  )
}

