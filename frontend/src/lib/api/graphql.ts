import { config } from '$lib/config';

export type PageRequest = {
  page?: number;
  size?: number;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
};

export type Comic = {
  id: number;
  title: string;
  slug: string;
  author?: string;
  description?: string;
  chapterCount?: number;
  status?: string;
  thumbImage?: { filePath?: string; originUrl?: string };
};

export type Chapter = {
  id: number;
  chapterNumber?: string;
  position?: number;
  crawlingStatus?: string;
  images?: Array<{ id: number; filePath?: string; originUrl?: string; position?: number }>;
};

async function request<T>(query: string, variables: Record<string, unknown> = {}): Promise<T> {
  const response = await fetch(config.graphqlUrl, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ query, variables })
  });

  if (!response.ok) {
    throw new Error(`GraphQL request failed with status ${response.status}`);
  }

  const payload = (await response.json()) as { data?: T; errors?: Array<{ message: string }> };
  if (payload.errors?.length) {
    throw new Error(payload.errors.map((error) => error.message).join(', '));
  }
  if (!payload.data) {
    throw new Error('GraphQL response did not include data');
  }
  return payload.data;
}

export function getComics(pageRequest: PageRequest = {}) {
  return request<{ comics: Comic[] }>(
    `query Comics($pageRequest: PageRequest) {
      comics(pageRequest: $pageRequest) {
        id title slug author description chapterCount status
        thumbImage { filePath originUrl }
      }
    }`,
    { pageRequest }
  );
}

export function getComic(id: number) {
  return request<{ comic: Comic }>(
    `query Comic($id: Long!) {
      comic(id: $id) {
        id title slug author description chapterCount status
        thumbImage { filePath originUrl }
      }
    }`,
    { id }
  );
}

export function getChapters(comicId: number, pageRequest: PageRequest = {}) {
  return request<{ chapters: Chapter[] }>(
    `query Chapters($comicId: Long!, $pageRequest: PageRequest) {
      chapters(comicId: $comicId, pageRequest: $pageRequest) {
        id chapterNumber position crawlingStatus
      }
    }`,
    { comicId, pageRequest }
  );
}

export function getChapter(id: number) {
  return request<{ detailChapterById: Chapter }>(
    `query Chapter($id: Long!) {
      detailChapterById(id: $id) {
        id chapterNumber position crawlingStatus
        images { id filePath originUrl position }
      }
    }`,
    { id }
  );
}
