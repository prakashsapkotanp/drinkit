# Drinkin' — Platform Architecture & Development Blueprint

**A LinkedIn-style social platform for drink experiences, reviews, and culture**
Stack: Kotlin end-to-end | Targets: Android + Web (KMP-ready for iOS later)

---

## 1. Product Summary

A feed-based social network where users post about their drink experiences — alcoholic and non-alcoholic (coffee, tea, cocktails, beer, wine, spirits, mocktails, etc.). Core interaction model mirrors LinkedIn: profile → feed → post → like/comment → follow/connect → notifications.

**Owner role:** You are the shareholder/product owner, not the hands-on coder. Development will be executed via AI coding agents (e.g. Google Jules) working in VS Code, with you reviewing tickets and architecture decisions.

---

## 2. Why Kotlin End-to-End Works Here

| Layer | Technology | Notes |
|---|---|---|
| Backend | **Kotlin + Spring Boot** | Mature, production-proven, huge ecosystem, easy for agents to generate idiomatic code against well-known patterns |
| Android | **Kotlin + Jetpack Compose** | Native Android UI |
| Web | **Kotlin Multiplatform + Compose Multiplatform (Web/Wasm)** | Share UI + business logic with Android |
| Shared Code | **Kotlin Multiplatform (KMP) module** | Models, validation, API client, view-models shared across Android & Web |
| iOS (future) | KMP already supports it | Add Compose Multiplatform iOS target later at low cost |

**Key benefit:** business logic (data models, API contracts, state management, validation rules) lives in one shared KMP module. You write it once; Android and Web both consume it. This roughly halves the frontend work for a solo-shareholder-run project.

---

## 3. High-Level Architecture

```
                         ┌─────────────────────────┐
                         │        Clients           │
                         │  Android (Compose)        │
                         │  Web (Compose/Wasm)        │
                         └────────────┬─────────────┘
                                      │ HTTPS / REST (+ WebSocket for live features)
                         ┌────────────▼─────────────┐
                         │      API Gateway           │
                         │ (Spring Cloud Gateway /    │
                         │  or simple Nginx reverse   │
                         │  proxy for MVP)            │
                         └────────────┬─────────────┘
                                      │
        ┌─────────────┬──────────────┼──────────────┬──────────────┐
        ▼             ▼              ▼              ▼              ▼
   ┌─────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐
   │  User    │  │   Feed/    │  │  Social    │  │  Media     │  │ Notifica-  │
   │  Service │  │   Post     │  │  Graph     │  │  Service   │  │ tion Svc   │
   │          │  │  Service   │  │  (Follow)  │  │ (uploads)  │  │            │
   └────┬────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘
        │              │              │              │              │
        └──────────────┴──────┬───────┴──────────────┴──────────────┘
                               ▼
                    ┌─────────────────────┐
                    │   PostgreSQL (core)  │
                    │   Redis (cache/feed) │
                    │   Elasticsearch      │
                    │   (search/discovery) │
                    │   Cloud Storage      │
                    │   (images/media)     │
                    └─────────────────────┘
```

**MVP recommendation:** Start as a **modular monolith** (single Spring Boot app, cleanly separated packages/modules per domain) rather than true microservices. As a solo shareholder directing agents, a monolith is far easier to build, deploy, debug, and reason about. Split into services later only if scale demands it.

---

## 4. Core Domain Modules

1. **User/Identity** — registration, auth (JWT/OAuth2), profile, bio, drink preferences
2. **Post/Feed** — create post, feed ranking, media attachment, drink tagging (category, type, brand)
3. **Social Graph** — follow/unfollow, connections, mutual follows
4. **Engagement** — likes, comments, shares/reposts
5. **Discovery/Search** — search users, posts, drink categories, trending tags
6. **Notifications** — new follower, like, comment, mention
7. **Media** — image upload, resizing, CDN delivery
8. **Moderation** — reporting, content flagging, admin review queue (important given alcohol content — age gating and community guidelines matter here)

---

## 5. Suggested Tech Stack

| Concern | Choice | Reasoning |
|---|---|---|
| Backend framework | Spring Boot (Kotlin) | Best Kotlin backend ecosystem, agents generate reliable code against it |
| API style | REST (JSON) for MVP; GraphQL optional later | REST is simpler for agents to implement correctly and for you to review |
| Database | PostgreSQL | Relational integrity for users/posts/follows; JSONB for flexible drink metadata |
| Cache / Feed store | Redis | Feed fan-out, session cache, rate limiting |
| Search | Elasticsearch or OpenSearch | Drink/user/post discovery |
| Media storage | Google Cloud Storage | You mentioned Google Cloud — natural fit |
| Auth | Spring Security + JWT, OAuth2 login (Google) | Standard, agent-friendly, secure |
| Hosting | Google Cloud Run (backend) + Firebase Hosting or Cloud Storage+CDN (Web/Wasm static output) | Serverless-friendly, low ops overhead for solo ownership |
| CI/CD | GitHub Actions | Free tier is enough at MVP stage; agents can maintain YAML pipelines |
| Mobile distribution | Google Play (Android) | iOS via KMP later if you expand |

---

## 6. Core Data Model (MVP)

```
User
 ├─ id, email, username, displayName, bio, avatarUrl
 ├─ createdAt, ageVerified (important for alcohol content)
 └─ drinkPreferences[] (coffee, wine, cocktails, tea, etc.)

Post
 ├─ id, authorId, text, mediaUrls[]
 ├─ drinkCategory (alcoholic / non-alcoholic), drinkType (wine, beer, coffee, tea, cocktail...)
 ├─ rating (optional, 1–5), tastingNotes (optional structured field)
 ├─ scenario (e.g. "with friends", "morning routine", "cold weather")
 ├─ createdAt, likeCount, commentCount

Follow
 ├─ followerId, followingId, createdAt

Like
 ├─ userId, postId, createdAt

Comment
 ├─ id, postId, authorId, text, createdAt

Notification
 ├─ id, userId, type (like/comment/follow), sourceUserId, refId, read, createdAt
```

This hybrid design (free-text posts + optional structured rating/tasting fields) covers both your "LinkedIn-style post" vision and room to grow into structured reviews later.

---

## 7. Repository Structure (Kotlin Multiplatform Monorepo)

```
drinkin/
├── backend/                  # Spring Boot Kotlin backend
│   ├── src/main/kotlin/...
│   └── build.gradle.kts
├── shared/                   # KMP shared module
│   ├── src/commonMain/kotlin/    # models, API client, business logic
│   ├── src/androidMain/kotlin/
│   └── src/jsMain (or wasmJsMain)/kotlin/
├── androidApp/                # Compose Android app
│   └── src/main/kotlin/...
├── webApp/                    # Compose Multiplatform Web (Wasm) app
│   └── src/wasmJsMain/kotlin/...
└── .github/workflows/         # CI/CD pipelines
```

This single-repo structure is easy for AI agents to navigate ticket-by-ticket, and easy for you to review as shareholder — one PR can touch shared logic + both frontends coherently.

---

## 8. Development Roadmap

### Phase 0 — Foundation (2–3 weeks)
- Set up monorepo, CI/CD, Gradle multiplatform config
- Backend skeleton: auth, user service, PostgreSQL schema + migrations (Flyway)
- Shared KMP module: core data models, API client (Ktor client)

### Phase 1 — MVP Core (4–6 weeks)
- User registration/login, profile creation, age verification
- Create/view posts with drink tagging
- Feed (reverse-chronological is fine for MVP — skip ranking algorithm initially)
- Follow/unfollow
- Likes + comments
- Android app (Compose) consuming shared module
- Web app (Compose/Wasm) consuming same shared module

### Phase 2 — Engagement & Discovery (3–4 weeks)
- Notifications
- Search (users, drinks, tags)
- Media upload (images with posts)
- Basic moderation/reporting tools

### Phase 3 — Growth Features (ongoing)
- Feed ranking algorithm (engagement-based)
- Trending drinks/tags
- Direct messaging
- Structured review mode (ratings, tasting notes, comparisons)
- iOS app via KMP (near-zero new backend work)

---

## 9. Important Domain-Specific Considerations

- **Age verification & legal compliance:** Since content covers alcohol, you'll need age-gating at signup and region-aware content policies (some countries restrict alcohol marketing/content). Worth a legal review before public launch.
- **Content moderation:** Community guidelines around responsible drinking messaging are worth establishing early — this also affects app store approval (Google Play has specific policies for alcohol-related content).
- **App store policy:** Google Play requires alcohol-content apps to have age-restriction settings configured in the Play Console.

---

## 10. How to Run This as a Solo Shareholder with AI Agents

- Break each roadmap item above into individual tickets (I can generate a full ticket backlog if useful)
- Keep the shared KMP module as the "source of truth" — review changes here most carefully, since bugs propagate to both frontends
- Use GitHub Actions to enforce: build passes + tests pass before any agent-authored PR merges
- Review architecture-level decisions yourself (schema changes, API contract changes); let agents handle implementation details within tickets

---

## Next Steps

Let me know if you'd like:
1. A detailed ticket backlog (Phase 0 and Phase 1 broken into individual dev tickets)
2. Database schema as actual SQL/Flyway migration files
3. API contract spec (OpenAPI/Swagger) for the backend
4. Gradle multiplatform project scaffold (starter code)