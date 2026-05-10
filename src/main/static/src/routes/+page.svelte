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
