<script lang="ts">
  export let data;
</script>

<svelte:head>
  <title>Chapter {data.chapter.chapterNumber || data.chapter.position} | Warada Reader</title>
</svelte:head>

<div class="reader-heading">
  <a class="back-link" href="/">← Library</a>
  <h1>Chapter {data.chapter.chapterNumber || data.chapter.position}</h1>
</div>
{#if !data.chapter.images?.length}
  <div class="status">This chapter has no images yet.</div>
{:else}
  <section class="reader" aria-label="Chapter pages">
    {#each data.chapter.images as image, index}
      <figure>
        <img src={image.filePath || image.originUrl} alt={`Page ${index + 1}`} loading={index === 0 ? 'eager' : 'lazy'} />
      </figure>
    {/each}
  </section>
{/if}
