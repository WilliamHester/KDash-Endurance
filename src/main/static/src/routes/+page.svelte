<script>
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import {
    LiveTelemetryServiceDefinition
  } from '$lib/grpc/live_telemetry_service';
  import {createChannel, createClient} from "nice-grpc-web";

  let sessions = [];
  let loading = true;
  let error = null;

  onMount(async () => {
    try {
      const channel = createChannel(window.location.origin + '/api');
      const client = createClient(LiveTelemetryServiceDefinition, channel);

      const response = await client.listSessions();
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

<div class="container">
  <h2>🏁 Session List</h2>
  <table class="session-table">
      <thead>
        <tr>
          <th>Session ID</th>
          <th>Sub Session ID</th>
          <th>Sim Session #</th>
          <th>Car #</th>
          <th>Track</th>
        </tr>
      </thead>
      <tbody>
        {#each sessions as session}
          <tr onclick={() => selectSession(session)} class="session-row">
            <td>{session.sessionId}</td>
            <td>{session.subSessionId}</td>
            <td>{session.simSessionNumber}</td>
            <td>{session.carNumber}</td>
            <td>{session.trackName}</td>
          </tr>
        {/each}
      </tbody>
    </table>
</div>

<style>
  .container {
    font-family: sans-serif;
    padding: 2rem;
    max-width: 1200px;
    margin: 0 auto;
  }

  .session-table {
    width: 100%;
    border-collapse: collapse;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    margin-top: 1rem;
  }

  th {
    border-bottom: 2px solid #ddd;
    padding: 12px;
    text-align: left;
    background-color: #fafafa;
    font-weight: 600;
    color: #333;
  }

  td {
    border-bottom: 1px solid #ddd;
    padding: 12px;
    color: #333;
  }

  .session-row {
    cursor: pointer;
    transition: background-color 0.2s;
  }

  .session-row:hover {
    background-color: #f0f0f0;
  }
</style>
