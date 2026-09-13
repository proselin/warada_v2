import { config } from '$lib/config';

export type CrawlProgressEvent = {
  type?: string;
  status?: string;
  message?: string;
  chapter?: string;
};

export async function crawlComic(slugNId: string): Promise<string> {
  const response = await fetch(`${config.crawlApiUrl}/api/v1/crawl/nettruyen/comic`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ slugNId })
  });
  if (!response.ok) throw new Error(`Crawl request failed with status ${response.status}`);
  return response.text();
}

export async function retryChapters(slug: string): Promise<string> {
  const response = await fetch(`${config.crawlApiUrl}/api/v1/crawl/nettruyen/comic/${encodeURIComponent(slug)}/retry`, {
    method: 'POST'
  });
  if (!response.ok) throw new Error(`Retry request failed with status ${response.status}`);
  return response.text();
}

export function subscribeToProgress(slug: string, onEvent: (event: CrawlProgressEvent) => void, onError: () => void) {
  const source = new EventSource(`${config.crawlApiUrl}/api/v1/crawl/progress/${encodeURIComponent(slug)}`);
  source.onmessage = (message) => {
    try {
      onEvent(JSON.parse(message.data) as CrawlProgressEvent);
    } catch {
      onEvent({ message: message.data });
    }
  };
  source.onerror = onError;
  return () => source.close();
}
