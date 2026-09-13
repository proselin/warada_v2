<script lang="ts">
  import '../app.css';
</script>

<svelte:head>
  <meta name="theme-color" content="#312e81" />
</svelte:head>

<a class="skip-link" href="#main-content">Skip to content</a>
<header class="site-header">
  <a class="brand" href="/">Warada<span>Reader</span></a>
  <nav aria-label="Primary navigation">
    <a href="/">Browse</a>
    <a href="/admin">Admin</a>
  </nav>
</header>
<main id="main-content">
  {@render children()}
</main>
