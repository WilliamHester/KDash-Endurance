<script>
  import * as Command from "$lib/components/ui/command/index.js";
  import {Button} from "$lib/components/ui/button/index.js";
  import {
    driversList,
    staticSessionInfo,
  } from "$lib/stores/session.js";
  import {
    selectedCars,
  } from "$lib/stores/drivers.svelte.js";

  import {Checkbox} from "$lib/components/ui/checkbox/index.js";

  let open = $state(false);

  const toggleCarId = (carId) => {
    if (selectedCars.has(carId)) {
      selectedCars.delete(carId);
    } else {
      selectedCars.add(carId);
    }
  };

  const carType = $derived(staticSessionInfo == null || staticSessionInfo.isTeamEvent ? "team" : "driver");
</script>

<Button onclick={() => open = !open}>Select {carType}s to watch</Button>

<Command.Dialog bind:open>
  <Command.Input placeholder={`Enter a ${carType} name...`}/>
  <Command.List>
    <Command.Group heading={`${carType}s`} class="capitalize">
      {#each $driversList as driver}
        <Command.Item
          onSelect={() => toggleCarId(driver.carId)}
          class="flex items-center justify-between py-0"
        >
          <div class="flex items-center gap-3">
            <Checkbox checked={selectedCars.has(driver.carId)} />
            <span class={selectedCars.has(driver.carId) ? "font-medium text-primary" : ""}>
              #{driver.carNumber} {driver.teamName}
            </span>
          </div>
        </Command.Item>
      {/each}
    </Command.Group>
  </Command.List>
</Command.Dialog>
