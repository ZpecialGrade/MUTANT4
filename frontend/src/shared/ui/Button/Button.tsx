import React from 'react'
import styles from './Button.module.css'

type Props = React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'ghost' | 'danger'
}

export function Button({ variant = 'primary', className, ...rest }: Props) {
  const cls = [styles.button, styles[variant], className].filter(Boolean).join(' ')
  return <button className={cls} {...rest} />
}

