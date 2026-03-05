# warada_v2 🎉

> ⚠️ **WARNING:** This project is a hobby side project. It is NOT published, NOT production-ready, and NOT recommended for anything serious. If you found this somehow, congratulations — you are either a very curious person or very, very bored. Either way, welcome! 🎊

## What even is this? 🤔

**warada_v2** is a comic/manga crawler and management system. Basically, it's what happens when a developer thinks *"I should build my own manga reader instead of just using one that already exists."*

Built on [Quarkus](https://quarkus.io/) — the Supersonic Subatomic Java Framework — because if you're gonna over-engineer a hobby project, you might as well do it with style and blazing-fast startup times.

### Tech Stack (a.k.a. Things I Wanted to Learn)

- 🚀 **Quarkus** — Java, but make it fast (and cool)
- 🐘 **PostgreSQL** — Because SQLite felt too easy
- 🧬 **Hibernate ORM + Panache** — Less boilerplate, more time to procrastinate
- 🌐 **GraphQL + REST** — Two APIs because one is never enough
- 🐳 **Docker** — Containerizing things I could just run locally
- 📦 **Gradle** — Multi-module build because flat is boring
- 📊 **Prometheus + Micrometer** — Monitoring a hobby project nobody uses

### Project Structure (a.k.a. "The Over-Architecture")

```text
warada_v2/
├── application/        # The main entry point. Where everything starts (and hopefully doesn't crash)
├── db-entities/        # JPA entities — Comic, Chapter, Image (the holy trinity)
├── repositories/       # Data access layer. Ask nicely and the DB will answer
├── services/           # Business logic. Currently mostly empty stubs, shh 🤫
├── resources/          # REST & GraphQL endpoints. Actually useful endpoints go here
├── commons/            # Shared utilities. Currently a museum of good intentions
└── configurations/     # Config classes. Because magic constants are a war crime
```

## Running this Masterpiece 🖥️

### Prerequisites

- Java 21+ (the future is now, old man)
- Docker (for PostgreSQL — yes, we containerized the database for a hobby project)
- A willingness to watch Gradle download half the internet

### Step 1: Start the Database (the Real MVP)

```shell
docker compose -f docker.compose.yaml up -d
```

This fires up a PostgreSQL instance.  
Username: `postgres`  
Password: `postgres`  

Super secure. Don't deploy this anywhere.

### Step 2: Run in Dev Mode (with Live Reload Magic ✨)

```shell
./gradlew quarkusDev
```

Your app is now running at <http://localhost:8080>.  
A fancy Dev UI awaits you at <http://localhost:8080/q/dev/>.

Want to poke at the GraphQL API?  
It's at <http://localhost:8080/graphql> with a playground UI available in dev mode.

### Step 3: Actually Use It

Hit the REST crawl endpoint:

```http
GET http://localhost:8080/api/v1/crawl
```

*(Spoiler: it currently just says hi. The real crawling is "coming soon™")*

Query some comics via GraphQL:

```graphql
query {
  comics {
    title
    author
    status
  }
}
```

*(Spoiler #2: returns hardcoded demo data. Real data is also "coming soon™")*

## Building for Production (Why Though?) 📦

Package it up:

```shell
./gradlew build
```

Run the jar:

```shell
java -jar build/quarkus-app/quarkus-run.jar
```

If you're feeling adventurous and want an über-jar:

```shell
./gradlew build -Dquarkus.package.jar.type=uber-jar
java -jar build/*-runner.jar
```

## Native Executable (Maximum Overkill) ⚡

Want to compile to a native binary?

```shell
./gradlew build -Dquarkus.native.enabled=true
```

No GraalVM? No problem, use Docker:

```shell
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
```

Then run it:

```shell
./build/warada_v2-1.0-SNAPSHOT-runner
```

Starts in milliseconds. Uses almost no memory. Perfect for a project that runs on exactly zero servers.

## Current Status 🚦

| Feature | Status |
|--------|--------|
| Database entities (Comic, Chapter, Image) | ✅ Done |
| Repositories | ✅ Done |
| GraphQL API | ✅ Kinda works (returns hello + demo data) |
| REST API | ✅ Returns a greeting |
| Services (actual business logic) | 🚧 Coming soon™ |
| Actual crawling | 🚧 Coming soon™ |
| Being published | ❌ Not until I change my mind |

## FAQ ❓

**Q: Can I use this in production?**  
A: lol no

**Q: When will it be finished?**  
A: When I feel like it. It's a hobby project.

**Q: Why "warada_v2"? What happened to v1?**  
A: We don't talk about v1.

**Q: Should I star this repo?**  
A: Absolutely yes, it will make me feel validated 🌟

---

*Built with ☕ caffeine, 🎵 lofi music, and the eternal developer delusion that "this weekend project" won't take six months.*
