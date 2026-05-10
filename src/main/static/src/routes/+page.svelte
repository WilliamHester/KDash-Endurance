<script>
  import { onMount, createRawSnippet } from 'svelte';
  import { goto } from '$app/navigation';
  import {
    LiveTelemetryServiceDefinition
  } from '$lib/grpc/live_telemetry_service';
  import {createChannel, createClient} from "nice-grpc-web";
  import DataTable from '$lib/components/DataTable.svelte';
  import { renderSnippet } from "$lib/components/ui/data-table/index.js";

  const columns = [
    {
      accessorKey: "sessionCreated",
      header: "Date Created",
      cell: ({ row }) => {
        const dateSnippet = createRawSnippet((getDate) => {
          const { date } = getDate();
          return {
            render: () => `<div>${date ? date.toLocaleString() : ''}</div>`
          };
        });
        return renderSnippet(dateSnippet, {
          date: row.original.sessionCreated,
        });
      },
    },
    {
      accessorKey: "mostRecentDriver",
      header: "Most Recent Driver",
    },
    {
      accessorKey: "trackName",
      header: "Track",
    },
    {
      accessorKey: "carNumber",
      header: "Car #",
    },
    {
      accessorKey: "sessionName",
      header: "Session",
    },
    {
      accessorKey: 'sessionMetadataTimestamp',
      header: '',
      cell: ({ row }) => {
        const liveSnippet = createRawSnippet((getTimestamp) => {
          const { timestamp } = getTimestamp();
          if (!timestamp || Date.now() - timestamp > 60_000) {
            return { render: () => `<div></div>` };
          }

          return {
            render: () => `<div class="inline-flex items-center gap-1.5 bg-red-600 text-white px-2 py-0.5 rounded text-xs font-bold uppercase tracking-wide w-fit">
              Live
              <span class="relative flex h-2 w-2">
                <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-white opacity-75"></span>
                <span class="relative inline-flex rounded-full h-2 w-2 bg-white"></span>
              </span>
            </div>`
          };
        });
        return renderSnippet(liveSnippet, {
          timestamp: row.original.sessionMetadataTimestamp,
        });
      }
    }
  ];

  let sessions = $state([]);
  let loading = $state(true);
  let error = $state(null);

  onMount(async () => {
    try {
      const channel = createChannel(window.location.origin + '/api');
      const client = createClient(LiveTelemetryServiceDefinition, channel);

      const response = await client.listSessions({});
      sessions = response.sessions;

      loading = false;
    } catch (e) {
      console.error("gRPC Error:", e);
      error = e;
      loading = false;
    }
  });

  function selectSession(s) {
    goto(`/${s.sessionId}/${s.subSessionId}/${s.simSessionNumber}/${s.carNumber}`);
  }
</script>

<div class="container mx-auto py-10 max-w-300">
  <h2 class="text-2xl font-bold mb-4">Session List</h2>
  {#if loading}
    <p class="text-sm text-muted-foreground">Loading sessions...</p>
  {:else if error}
    <p class="text-sm text-destructive">Error loading sessions.</p>
  {:else}
    <DataTable data={sessions} {columns} onRowClick={selectSession} />
  {/if}
</div>

<style>
  * {
    font-family: sans-serif;
  }
</style>
