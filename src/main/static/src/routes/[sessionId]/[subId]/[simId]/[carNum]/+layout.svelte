<script>
  import { page } from '$app/state';
  import { sessionStore } from '$lib/stores/session';
  let { children } = $props();

  $effect(() => {
    const { sessionId, subId, simId, carNum } = page.params;
    if (sessionId) {
      sessionStore.connect(sessionId, subId, simId, carNum);
    }
    return () => sessionStore.disconnect();
  });

  let currentPath = $derived(page.url.pathname);
  let basePath = $derived(`/${page.params.sessionId}/${page.params.subId}/${page.params.simId}/${page.params.carNum}`);
</script>

<div class="app-column">
  <header class="header">
    <div class="wrapper">
      <a class="website-name" href="/">Stintlytics</a>
    </div>
  </header>

  <div class="app-row">
    <nav class="hamburgerMenu">
      <a 
        class="menu-item" 
        class:active={currentPath === basePath} 
        href="{basePath}">
        Race Overview
      </a>
      
      <a 
        class="menu-item" 
        class:active={currentPath.includes('/laps')} 
        href="{basePath}/laps">
        Lap Records
      </a>
      
      <a 
        class="menu-item" 
        class:active={currentPath.includes('/otherlaps')} 
        href="{basePath}/otherlaps">
        Other Cars' Laps
      </a>

      <a
        class="menu-item"
        class:active={currentPath.includes('/telemetry')}
        href="{basePath}/telemetry">
        Telemetry Charts
      </a>

      <a
        class="menu-item"
        class:active={currentPath.includes('/options')}
        href="{basePath}/options">
        Options
      </a>
    </nav>

    <div class="content">
      {@render children?.()}
    </div>
  </div>
</div>

<style>
  :global(body) {
    margin: 0;
    background-color: #000;
    color: #f0f0f0;
    font-family: Roboto, -apple-system, BlinkMacSystemFont, 'Helvetica Neue', sans-serif;
    font-size: 0.7em;
    height: 100vh;
    overflow: hidden;
  }

  .app-column {
    display: flex;
    flex-direction: column;
    height: 100vh;
  }

  .header {
    display: flex;
    align-items: center;
    height: 48px;
    padding: 0 48px;
    background-color: #181818;
    border-bottom: 1px solid #333;
  }

  .website-name {
    font-family: 'Electrolize', sans-serif;
    font-size: 1.2rem;
    color: inherit;
    text-decoration: none;
  }

  .app-row {
    display: flex;
    flex: 1;
    overflow: hidden; /* Contain scroll to content area */
  }

  .hamburgerMenu {
    display: flex;
    flex-direction: column;
    width: 150px;
    background-color: #101010;
    font-family: 'Electrolize', sans-serif;
    padding-top: 10px;
    border-right: 1px solid #333;
  }

  .menu-item {
    color: #888;
    text-decoration: none;
    font-size: 0.8rem;
    margin: 4px 8px;
    padding: 10px 16px;
    border-radius: 5px;
    transition: all 0.2s;
  }

  .menu-item:hover {
    background-color: #252525;
    color: #fff;
  }

  .menu-item.active {
    background-color: #3a3a3a;
    color: #fff;
    font-weight: bold;
  }

  .content {
    flex: 1;
    overflow-y: auto;
    padding: 12px;
  }
</style>
