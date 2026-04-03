import { History, Rocket, Wrench, MonitorSmartphone, ShieldCheck } from 'lucide-react'
import PageHero from '../components/ui/PageHero'
import { CURRENT_RELEASE, RELEASE_NOTES } from '../lib/releaseNotes'

export default function ReleaseNotesPage() {
  return (
    <div className="space-y-5">
      <PageHero
        eyebrow="Release Management"
        title="Release Notes"
        description="Theo doi thay doi theo tung phien ban ngay tren UI de kiem soat rollout va impact."
        icon={<History className="mt-1 text-teal-700" size={22} />}
        aside={
          <div className="rounded-2xl bg-teal-50 px-4 py-2 text-right">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-teal-700">Ban hien tai</p>
            <p className="mt-1 text-lg font-bold text-teal-900">{CURRENT_RELEASE.version}</p>
            <p className="text-xs text-teal-700">{CURRENT_RELEASE.date}</p>
          </div>
        }
      />

      <section className="grid gap-4">
        {RELEASE_NOTES.map((release) => (
          <article key={release.version} className="surface-float rounded-3xl p-5 md:p-6">
            <div className="flex flex-wrap items-start justify-between gap-3 border-b border-stone-200 pb-4">
              <div>
                <h3 className="text-2xl font-bold tracking-tight text-gray-900">{release.version}</h3>
                <p className="mt-1 text-sm text-gray-600">{release.summary}</p>
              </div>
              <span className="rounded-xl bg-stone-100 px-3 py-1 text-xs font-semibold text-gray-700">{release.date}</span>
            </div>

            <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
              <ReleaseBlock title="Highlights" icon={<Rocket size={16} />} items={release.highlights} tone="teal" />
              <ReleaseBlock title="Backend" icon={<Wrench size={16} />} items={release.backend} tone="sky" />
              <ReleaseBlock title="Frontend" icon={<MonitorSmartphone size={16} />} items={release.frontend} tone="amber" />
              <ReleaseBlock title="Quality" icon={<ShieldCheck size={16} />} items={release.quality} tone="emerald" />
            </div>
          </article>
        ))}
      </section>
    </div>
  )
}

type Tone = 'teal' | 'sky' | 'amber' | 'emerald'

function ReleaseBlock({ title, icon, items, tone }: { title: string; icon: React.ReactNode; items: string[]; tone: Tone }) {
  const classes = {
    teal: 'bg-teal-50 text-teal-800 border-teal-100',
    sky: 'bg-sky-50 text-sky-800 border-sky-100',
    amber: 'bg-amber-50 text-amber-800 border-amber-100',
    emerald: 'bg-emerald-50 text-emerald-800 border-emerald-100',
  }[tone]

  return (
    <div className={`rounded-2xl border p-4 ${classes}`}>
      <div className="mb-3 flex items-center gap-2 text-sm font-semibold">
        {icon}
        <span>{title}</span>
      </div>
      <ul className="space-y-2 text-sm">
        {items.map((item) => (
          <li key={item} className="leading-relaxed">- {item}</li>
        ))}
      </ul>
    </div>
  )
}
