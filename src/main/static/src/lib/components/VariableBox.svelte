<script>
  import DataBox from "$lib/components/DataBox.svelte";

  let { children, title } = $props();
</script>

<DataBox {title}>
  <div class="variable-box-contents">
    <span class="variable-text text-xl">
      {@render children?.()}
    </span>
  </div>
</DataBox>

<style>
  .variable-box-contents {
    display: flex;
    justify-content: center;
    align-items: center;
  }

  .variable-text {
    padding: 12px 24px;
    text-align: center;
    display: block;
  }
</style>
