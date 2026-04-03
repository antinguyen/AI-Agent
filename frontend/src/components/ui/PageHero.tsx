import type { ReactNode } from 'react'

interface PageHeroProps {
  eyebrow: string
  title: string
  description: string
  aside?: ReactNode
  icon?: ReactNode
}

export default function PageHero({ eyebrow, title, description, aside, icon }: PageHeroProps) {
  return (
    <section className="surface-float rounded-[28px] p-6 md:p-7 enter-up">
      <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div className="flex items-start gap-3">
          {icon}
          <div>
            <p className="text-xs uppercase tracking-[0.24em] text-teal-700 font-bold">{eyebrow}</p>
            <h2 className="mt-2 text-3xl font-bold tracking-tight text-gray-900">{title}</h2>
            <p className="mt-2 text-sm text-gray-600">{description}</p>
          </div>
        </div>
        {aside}
      </div>
    </section>
  )
}