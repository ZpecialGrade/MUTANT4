import React from 'react'
import styles from './Card.module.css'

export function Card({
  title,
  right,
  children,
  className,
}: {
  title?: string
  right?: React.ReactNode
  children: React.ReactNode
  className?: string
}) {
  return (
    <section className={[styles.card, className].filter(Boolean).join(' ')}>
      {title ? (
        <header className={styles.header}>
          <div className={styles.title}>{title}</div>
          <div className={styles.right}>{right}</div>
        </header>
      ) : null}
      <div className={styles.body}>{children}</div>
    </section>
  )
}

