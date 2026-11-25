import { QueuePage, type QueueSearch } from "@/components/QueuePage";
import { getSession } from "@/lib/data";

export default async function AntreanPage({ searchParams }: { searchParams: Promise<QueueSearch> }) {
  const [search, { staff }] = await Promise.all([searchParams, getSession()]);
  const region = staff?.regionName ?? "Semua wilayah";
  return (
    <QueuePage
      basePath="/"
      title="Antrean verifikasi"
      lead={`${region} · data yang tampil ke wisatawan hanya yang berstatus Terbit.`}
      search={search}
    />
  );
}
