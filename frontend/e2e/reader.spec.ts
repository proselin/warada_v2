import { expect, test, type Page } from '@playwright/test';

const comic = {
  id: 7,
  title: 'One Piece',
  slug: 'one-piece-12345',
  author: 'Eiichiro Oda',
  description: 'A pirate adventure.',
  chapterCount: 2,
  status: 'ongoing',
  thumbImage: { filePath: '/covers/one-piece.jpg', originUrl: '' }
};

const chapters = [
  { id: 11, chapterNumber: '1', position: 1, crawlingStatus: 'FINISHED' },
  { id: 12, chapterNumber: '2', position: 2, crawlingStatus: 'INIT' }
];

async function mockGraphql(page: Page) {
  await page.route('**/graphql', async (route) => {
    const request = route.request();
    const body = request.postDataJSON() as { query?: string };
    const query = body.query || '';

    if (query.includes('Comics')) {
      await route.fulfill({ json: { data: { comics: [comic] } } });
    } else if (query.includes('Chapters')) {
      await route.fulfill({ json: { data: { chapters } } });
    } else if (query.includes('Comic(')) {
      await route.fulfill({ json: { data: { comic } } });
    } else if (query.includes('Chapter(')) {
      await route.fulfill({
        json: {
          data: {
            detailChapterById: {
              ...chapters[0],
              images: [
                { id: 101, position: 1, filePath: '/pages/one-piece-1.jpg', originUrl: '' },
                { id: 102, position: 2, filePath: '/pages/one-piece-2.jpg', originUrl: '' }
              ]
            }
          }
        }
      });
    } else {
      await route.continue();
    }
  });
}

test('browses comics and opens comic details', async ({ page }) => {
  await mockGraphql(page);

  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Find your next story.' })).toBeVisible();
  await expect(page.getByRole('link', { name: /One Piece/ })).toBeVisible();

  await page.getByRole('link', { name: /One Piece/ }).click();
  await expect(page.getByRole('heading', { name: 'One Piece' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Chapter 1' })).toBeVisible();
});

test('renders a chapter reader with page images', async ({ page }) => {
  await mockGraphql(page);

  await page.goto('/reader/11');
  await expect(page.getByRole('heading', { name: 'Chapter 1' })).toBeVisible();
  await expect(page.getByRole('img', { name: 'Page 1' })).toHaveAttribute('src', '/pages/one-piece-1.jpg');
  await expect(page.getByRole('img', { name: 'Page 2' })).toHaveAttribute('src', '/pages/one-piece-2.jpg');
});

test('validates the admin crawl form before sending a request', async ({ page }) => {
  await page.goto('/admin');
  await page.getByRole('button', { name: 'Start crawl' }).click();

  await expect(page.getByRole('alert')).toHaveText('Enter a comic slug or source id.');
  await expect(page.getByRole('textbox', { name: 'Comic slug or source id' })).toBeVisible();
});
