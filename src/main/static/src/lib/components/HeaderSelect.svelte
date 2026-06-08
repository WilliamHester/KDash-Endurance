<script>
  import * as Select from "$lib/components/ui/select/index.js";

  let { value = $bindable(), options = [], class: className = "" } = $props();
</script>

<Select.Root type="single" bind:value={value}>
  <Select.Trigger class={className}>
    {value}
  </Select.Trigger>
  <Select.Content>
    {#each options as option}
      <Select.Item value={option}>{option}</Select.Item>
    {/each}
  </Select.Content>
</Select.Root>
