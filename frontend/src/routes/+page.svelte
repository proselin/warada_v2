<script lang="ts">
  import { onMount } from 'svelte';
  import { getComics, type Comic } from '$api/graphql';

  let comics: Comic[] = [];
  let loading = true;
  let error = '';

  async function loadComics() {
    loading = true;
    error = '';
    try {
      comics = (await getComics({ page: 0, size: 20, sortBy: 'title', sortOrder: 'asc' })).comics;
    } catch (cause) {
      error = cause instanceof Error ? cause.message : 'Unable to load comics';
    } finally {
      loading = false;
    }
  }

  onMount(loadComics);
</script>

<svelte:head>
  <title>Browse comics | Warada Reader</title>
  <meta name="description" content="Browse your Warada comic library." />
</svelte:head>

<section class="page-heading">
  <p class="eyebrow">Your reading library</p>
  <h1>Find your next story.</h1>
  <p class="lede">Browse comics collected by Warada and continue reading without the noise.</p>
</section>

{#if loading}
  <div class="status" aria-busy="true">Loading comics…</div>
{:else if error}
  <div class="status error" role="alert">{error}<button class="button secondary" onclick={loadComics}>Try again</button></div>
{:else if comics.length === 0}
  <div class="status">No comics are available yet.</div>
{:else}
  <section class="comic-grid" aria-label="Comic library">
    {#each comics as comic}
      <a class="comic-card" href={`/comics/${comic.id}`}>
        <div class="cover">
          {#if comic.thumbImage?.filePath || comic.thumbImage?.originUrl}
            <img src={comic.thumbImage.filePath || comic.thumbImage.originUrl} alt="" loading="lazy" />
          {:else}
            <span aria-hidden="true">No cover</span>
          {/if}
        </div>
        <div class="comic-card-body">
          <h2>{comic.title}</h2>
          <p>{comic.author || 'Unknown author'}</p>
        </div>
      </a>
    {/each}
  </section>
{/if}
