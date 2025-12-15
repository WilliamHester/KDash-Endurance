<script>
	import favicon from '$lib/assets/favicon.svg';

	let { children } = $props();
</script>

<svelte:head>
  <title>Stintlytics</title>
	<link rel="icon" href={favicon} />
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link rel="stylesheet" href="https://fonts.googleapis.com/css?family=Electrolize">
  <link href="https://fonts.googleapis.com/css2?family=Azeret+Mono:wght@300&family=Roboto:wght@400&display=swap" rel="stylesheet">
</svelte:head>

{@render children()}
