import React from 'react'
import styles from './Select.module.css'

type Option = { value: string; label: string }

type Props = Omit<React.SelectHTMLAttributes<HTMLSelectElement>, 'value' | 'onChange'> & {
  label?: string
  hint?: string
  error?: string
  value: string
  options: Option[]
  onChange: (v: string) => void
}

export function Select({ label, hint, error, value, options, onChange, className, ...rest }: Props) {
  return (
    <label className={[styles.wrap, className].filter(Boolean).join(' ')}>
      {label ? <div className={styles.label}>{label}</div> : null}
      <select
        className={[styles.select, error ? styles.selectError : ''].join(' ')}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        {...rest}
      >
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      {error ? <div className={styles.error}>{error}</div> : hint ? <div className={styles.hint}>{hint}</div> : null}
    </label>
  )
}

