<script lang="ts">
  export let data;
</script>

<svelte:head>
  <title>{data.comic.title} | Warada Reader</title>
</svelte:head>

<a class="back-link" href="/">← Back to library</a>
<article class="detail">
  <div class="detail-cover cover">
    {#if data.comic.thumbImage?.filePath || data.comic.thumbImage?.originUrl}
      <img src={data.comic.thumbImage.filePath || data.comic.thumbImage.originUrl} alt="" />
    {:else}
      <span aria-hidden="true">No cover</span>
    {/if}
  </div>
  <div>
    <p class="eyebrow">Comic detail</p>
    <h1>{data.comic.title}</h1>
    <p class="muted">{data.comic.author || 'Unknown author'}</p>
    <p>{data.comic.description || 'No description available.'}</p>
    <h2>Chapters</h2>
    {#if data.chapters.length === 0}
      <p class="status">No chapters are available yet.</p>
    {:else}
      <ol class="chapter-list">
        {#each data.chapters as chapter}
          <li><a href={`/reader/${chapter.id}`}>Chapter {chapter.chapterNumber || chapter.position}</a><span>{chapter.crawlingStatus}</span></li>
        {/each}
      </ol>
    {/if}
  </div>
</article>
