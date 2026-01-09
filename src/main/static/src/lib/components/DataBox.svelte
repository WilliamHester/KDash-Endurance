<script>
  let { children, title, class: className = '' } = $props();
</script>

<div class="data-box-container {className}">
  <div class="box-title">{title}</div>
  {@render children?.()}
</div>

<style>
  .data-box-container {
    width: fit-content;
    border: #404040 1px solid;
    display: inline-block; /* Aligns them in rows naturally */
    vertical-align: top;
  }

  .box-title {
    background-color: #202020;
    padding: 4px;
    color: #ccc;
    text-align: center;
  }
</style>