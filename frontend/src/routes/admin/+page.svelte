<script lang="ts">
  import { crawlComic, subscribeToProgress, type CrawlProgressEvent } from '$api/crawl';

  let slugNId = '';
  let progress: CrawlProgressEvent[] = [];
  let error = '';
  let running = false;
  let closeStream: (() => void) | undefined;

  async function startCrawl() {
    if (!slugNId.trim()) {
      error = 'Enter a comic slug or source id.';
      return;
    }
    error = '';
    progress = [];
    running = true;
    closeStream?.();
    closeStream = subscribeToProgress(slugNId.trim(), (event) => progress = [...progress, event], () => {
      error = 'Progress stream disconnected.';
      running = false;
    });
    try {
      await crawlComic(slugNId.trim());
    } catch (cause) {
      error = cause instanceof Error ? cause.message : 'Unable to start crawl';
      running = false;
      closeStream?.();
    }
  }
</script>

<svelte:head><title>Admin crawl | Warada Reader</title></svelte:head>
<section class="page-heading">
  <p class="eyebrow">Operations</p>
  <h1>Crawl a comic.</h1>
  <p class="lede">Start a NetTruyen crawl and monitor progress without leaving the reader.</p>
</section>
<form class="crawl-form" onsubmit={(event) => { event.preventDefault(); startCrawl(); }}>
  <label for="slug">Comic slug or source id</label>
  <input id="slug" bind:value={slugNId} placeholder="one-piece-12345" autocomplete="off" />
  <button class="button primary" disabled={running}>{running ? 'Crawling…' : 'Start crawl'}</button>
</form>
{#if error}<p class="status error" role="alert">{error}</p>{/if}
<section aria-live="polite" aria-label="Crawl progress">
  {#each progress as event}
    <p class="progress-event"><strong>{event.status || event.type || 'Update'}</strong> {event.message || event.chapter || ''}</p>
  {/each}
</section>
