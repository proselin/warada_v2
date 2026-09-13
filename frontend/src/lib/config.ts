import { env } from '$env/dynamic/public';

export const config = {
  graphqlUrl: env.PUBLIC_GRAPHQL_URL || 'http://localhost:8080/graphql',
  crawlApiUrl: env.PUBLIC_CRAWL_API_URL || 'http://localhost:8081'
};
