import { getComic, getChapters } from '$api/graphql';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ params }) => {
  const id = Number(params.id);
  if (!Number.isSafeInteger(id)) throw new Error('Invalid comic id');
  const [{ comic }, { chapters }] = await Promise.all([getComic(id), getChapters(id, { page: 0, size: 100 })]);
  return { comic, chapters };
};
