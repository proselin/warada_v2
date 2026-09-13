import { getChapter } from '$api/graphql';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ params }) => {
  const id = Number(params.id);
  if (!Number.isSafeInteger(id)) throw new Error('Invalid chapter id');
  return { chapter: (await getChapter(id)).detailChapterById };
};
