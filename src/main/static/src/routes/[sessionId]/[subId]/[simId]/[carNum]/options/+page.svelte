<script>
  import options from '$lib/stores/options';
  import { Input } from "$lib/components/ui/input";
  import { Label } from "$lib/components/ui/label";
</script>

<div class="flex w-full max-w-sm flex-col gap-1.5">
  <Label for="target-stint-length">Target Stint Length</Label>
  <Input type="number" id="target-stint-length" bind:value={$options.targetStintLength} />

  <Label for="stop-and-go">Stop and Go Seconds Lost</Label>
  <Input type="number" id="stop-and-go" step="any" bind:value={$options.stopAndGoSeconds} />

  <Label for="fuel-margin">Fuel Margin</Label>
  <Input type="number" id="fuel-margin" step="any" bind:value={$options.fuelMargin} />

  <Label for="fuel-fill-rate">Fuel Fill Rate</Label>
  <Input type="number" id="fuel-fill-rate" step="any" bind:value={$options.fuelFillRate} />
</div>

<style>
</style>
