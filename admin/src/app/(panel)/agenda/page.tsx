import { QueuePage, type QueueSearch } from "@/components/QueuePage";

export default async function AgendaPage({ searchParams }: { searchParams: Promise<QueueSearch> }) {
  const search = await searchParams;
  return (
    <QueuePage
      basePath="/agenda"
      kind="events"
      title="Agenda & jadwal"
      lead="Jadwal pertunjukan. Jadwal yang dibatalkan dicatat sebagai hari tutup tempatnya."
      search={search}
    />
  );
}
