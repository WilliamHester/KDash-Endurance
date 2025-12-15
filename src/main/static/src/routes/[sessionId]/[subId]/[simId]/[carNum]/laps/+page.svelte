<script>
  import { laps } from '$lib/stores/session';
  import LapLogTable from '$lib/components/LapLogTable.svelte';
</script>

<div class="page-layout">
    <header>
        <h2>Lap Records</h2>
        <span class="count">Total Laps: {$laps.length}</span>
    </header>

    {#if $laps.length === 0}
        <div class="empty-state">No laps recorded yet.</div>
    {:else}
        <LapLogTable entries={$laps} />
    {/if}
</div>

<style>
    .page-layout {
        padding: 0 10px;
    }

    header {
        display: flex;
        justify-content: space-between;
        align-items: baseline;
        margin-bottom: 1rem;
        border-bottom: 1px solid #333;
        padding-bottom: 10px;
    }

    h2 {
        margin: 0;
        font-family: 'Electrolize', sans-serif;
    }

    .count {
        color: #666;
        font-family: monospace;
    }

    .empty-state {
        padding: 2rem;
        text-align: center;
        color: #666;
        font-style: italic;
    }
</style>
