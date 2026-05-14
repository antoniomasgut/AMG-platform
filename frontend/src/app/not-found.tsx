import Link from 'next/link';

export default function NotFound() {
  return (
    <div className="min-h-screen bg-[#0d0d1a] flex items-center justify-center px-4">
      <div className="text-center max-w-md">
        <div className="font-mono text-[120px] font-black leading-none text-accent opacity-20 select-none">
          404
        </div>
        <h1 className="text-2xl font-bold text-ink-0 -mt-4 mb-3">
          Pàgina no trobada
        </h1>
        <p className="text-ink-3 mb-8">
          La pàgina que cerques no existeix o ha estat moguda.
        </p>
        <div className="flex gap-3 justify-center">
          <Link
            href="/portal"
            className="px-5 py-2.5 bg-[#FF6B00] text-black font-semibold text-sm rounded hover:bg-[#FF9A3C] transition"
          >
            Anar al portal
          </Link>
          <Link
            href="/login"
            className="px-5 py-2.5 border border-[rgba(255,107,0,0.3)] text-accent-light font-semibold text-sm rounded hover:bg-accent-subtle transition"
          >
            Iniciar sessió
          </Link>
        </div>
      </div>
    </div>
  );
}
