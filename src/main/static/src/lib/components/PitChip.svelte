<script>
  import { formatNumberAsDuration } from '$lib/formatters';

  export let entry; // The LapEntry object

  $: inLap = entry.pitIn;
  $: outLap = entry.pitOut;
  $: pitTime = entry.pitTime;

  $: label = (() => {
    if (outLap && inLap) return 'OUT/IN';
    if (outLap) return 'OUT';
    if (inLap) return 'IN';
    return null;
  })();
</script>

{#if label || pitTime > 0}
    <div class="pit-container">
        {#if label}
            <span class="pit-label" class:has-time={pitTime > 0}>{label}</span>
        {/if}

        {#if pitTime > 0}
      <span class="pit-time" class:has-label={!!label}>
        {formatNumberAsDuration(pitTime, true)}
      </span>
        {/if}
    </div>
{/if}

<style>
    .pit-container {
        display: inline-flex;
        align-items: center;
        justify-content: flex-end;
        font-size: 10px;
        color: white;
    }

    .pit-label {
        background-color: #ee7200;
        border: 1px solid #ee7200;
        padding: 2px 4px;
        border-radius: 4px;
        font-weight: bold;
    }

    /* If we have both label and time, round corners appropriately to join them */
    .pit-label.has-time {
        border-top-right-radius: 0;
        border-bottom-right-radius: 0;
        border-right: none;
    }

    .pit-time {
        border: 1px solid #ee7200;
        padding: 2px 4px;
        border-radius: 4px;
    }

    .pit-time.has-label {
        border-top-left-radius: 0;
        border-bottom-left-radius: 0;
    }
</style>
