"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

export function NavLink({ href, children, count }: { href: string; children: React.ReactNode; count?: number }) {
  const pathname = usePathname();
  const current = href === "/" ? pathname === "/" : pathname.startsWith(href);
  return (
    <Link href={href} className="nav-link" aria-current={current ? "page" : undefined}>
      {children}
      {count && count > 0 ? (
        <span className="nav-count" aria-label={`${count} baru`}>
          {count}
        </span>
      ) : null}
    </Link>
  );
}
