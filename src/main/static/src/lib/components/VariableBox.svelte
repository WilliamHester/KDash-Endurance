<script>
  let { children, title } = $props();
</script>

<div class="variable-box-container">
  <div class="variable-box-title">{title}</div>
  <div class="variable-box-contents">
    <span class="variable-text">
      {@render children?.()}
    </span>
  </div>
</div>

<style>
  .variable-box-container {
    width: fit-content;
    border: #404040 1px solid;
    display: inline-block; /* Aligns them in rows naturally */
    vertical-align: top;
  }

  .variable-box-title {
    background-color: #202020;
    padding: 4px;
    color: #888;
    text-align: center;
  }

  .variable-box-contents {
    display: flex;
    justify-content: center;
    align-items: center;
  }

  .variable-text {
    font-size: 1.2rem;
    padding: 12px 24px;
    text-align: center;
    display: block;
    font-family: Monospaced, sans-serif;
  }
</style>
