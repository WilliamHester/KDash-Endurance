<script>
  import {
    connected,
    sessionStore,
  } from "$lib/stores/session.js";
  import LeaderboardTable from "$lib/components/LeaderboardTable.svelte";

  $effect(() => {
    if ($connected) {
      sessionStore.startTelemetry([
        'CarIdxLapDistPct',
        'CarIdxDriverCarClassEstTime',
        'CarIdxLapCompleted',
      ]);
    }
  })
</script>

<LeaderboardTable></LeaderboardTable>