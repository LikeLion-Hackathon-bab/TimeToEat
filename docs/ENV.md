# TimeToEat(Spring) 환경변수

이 문서는 코드에서 실제로 읽는 값만 적는다. 기준 커밋은 `fix/three-friend-flow`(PR #107)이다.
실제 비밀값은 저장소, 이슈, 채팅에 쓰지 않는다. 운영 값은 서버 `/opt/babmuckdang/.env`(chmod 600)와 AWS SSM Parameter Store에만 둔다.

## 값이 들어가는 경로

- `application.yml`이 `spring.profiles.active: prod`를 켜고 `optional:file:.env[.properties]`를 읽는다. 그래서 작업 디렉터리에 `.env`가 있으면 그 값을 쓰고, 없으면 OS 환경변수만 쓴다.
- Docker Compose에서는 `env_file: .env`로 넘긴다. MySQL, RabbitMQ 컨테이너도 같은 `.env`를 읽으므로 이름이 겹치는 값(`MYSQL_*`, `RABBITMQ_DEFAULT_*`)은 한 번만 적는다.
- `.env`는 Java properties 형식으로 읽힌다. 값에 따옴표를 붙이면 따옴표까지 값이 된다.
- 표의 "속성" 열은 yml에 연결된 이름이다. yml에 `${...}` 연결이 없는 속성은 OS 환경변수 이름(점과 `-`를 `_`로, 대문자. 예: `app.ai.api-key` → `APP_AI_API_KEY`)으로 덮어쓴다.
- **주의**: Spring이 `.env` 파일을 직접 읽을 때는 그 줄이 OS 환경변수가 아니라 속성이 된다. 그래서 아래 두 종류는 `.env` 파일에 적어도 적용되지 않는다.
  - `System.getenv`로 읽는 `CORS_ALLOWED_ORIGINS`, `AUTH_COOKIE_DOMAIN`
  - yml 연결이 없는 `APP_AI_API_KEY`, `APP_REFERRAL_BASE_URL`, `APP_REFERRAL_TTL_DAYS`, `JWT_REFRESH_REUSE_GRACE_MS`, `SPRING_PROFILES_ACTIVE`
  
  Docker Compose `env_file`은 진짜 환경변수로 넘기므로 운영에서는 문제가 없다. 로컬에서는 `set -a; . ./.env; set +a` 뒤에 `./gradlew bootRun`을 실행해 모두 환경변수로 넘긴다(README "로컬 실행").
- 견본은 저장소 루트의 [`.env.example`](../.env.example)이다.

## 필수

비어 있으면 기동이 실패하거나 핵심 흐름이 멈추는 값이다.

| 이름 | 속성 / 읽는 곳 | 설명 | 예시(가짜 값) |
|---|---|---|---|
| `DB_URL` | `spring.datasource.url` | MySQL 8 JDBC 주소 | `jdbc:mysql://mysql:3306/timetoeat` |
| `DB_USERNAME` | `spring.datasource.username` | DB 계정 | `timetoeat` |
| `DB_PASSWORD` | `spring.datasource.password` | DB 비밀번호 | `change-me` |
| `RABBITMQ_DEFAULT_USER` | `spring.rabbitmq.username` | RabbitMQ 계정. RabbitMQ 컨테이너도 이 값으로 계정을 만든다 | `timetoeat` |
| `RABBITMQ_DEFAULT_PASS` | `spring.rabbitmq.password` | RabbitMQ 비밀번호 | `change-me` |
| `JWT_SECRET` | `jwt.secret` | HMAC 서명 키. UTF-8 32바이트 이상이어야 한다(`Keys.hmacShaKeyFor`). **Socket과 같은 값** | `openssl rand -base64 48`로 만든 값 |
| `ROOM_RESULT_KEY` | `MeetingController`, `MatchingPostService` | Spring↔Socket 서버 간 호출의 `X-Room-Key`. **Socket과 같은 값**. 비면 대기실 생성과 결과 저장이 403이 된다. 브라우저(Client)에는 넣지 않는다 | `openssl rand -hex 32`로 만든 값 |
| `KAKAO_CLIENT_ID` | `spring.security.oauth2.client.registration.kakao.client-id` | 카카오 REST API 키 | (카카오 콘솔) |
| `KAKAO_CLIENT_SECRET` | `...kakao.client-secret` | 카카오 Client Secret(`client_secret_post`) | (카카오 콘솔) |
| `CLIENT_REDIRECT_PATH` | `...kakao.redirect-uri` | 카카오 Redirect URI. 카카오 콘솔에 등록한 값과 글자까지 같아야 한다. 운영은 Vercel 주소로 고정한다(아래 "운영 값") | `http://localhost:8080/login/oauth2/code/kakao` |

## 연결 주소

| 이름 | 속성 / 읽는 곳 | 기본값 | 설명 |
|---|---|---|---|
| `REDIS_HOST` | `spring.data.redis.host` | `redis` | Redis 호스트 |
| `REDIS_PORT` | `spring.data.redis.port` | `6379` | |
| `RABBITMQ_HOST` | `spring.rabbitmq.host` | `rabbitmq` | |
| `RABBITMQ_PORT` | `spring.rabbitmq.port` | `5672` | |
| `SOCKET_BASE_URL` | `matching.socket-base-url` | `http://socket:3000` | Spring이 Socket 방 초기화·나가기 API를 부를 주소. 내부망 주소를 쓴다. 공인 주소나 평문 외부 HTTP로 `X-Room-Key`를 내보내지 않는다 |
| `FOOD_AI_BASE_URL` | `app.ai.food-base-url` | `http://food-ai:8000` | 음식 인식 AI |
| `RECO_AI_BASE_URL` | `app.ai.reco-base-url` | `http://recommend-ai:8010` | 메뉴 추천 AI |
| `TASTE_BASE_URL` | `app.ai.taste-base-url` | `http://taste-service:8020` | 취향 서비스 |

## Client 주소, 쿠키, CORS

| 이름 | 읽는 곳 | 기본값 | 설명 |
|---|---|---|---|
| `CLIENT_LOGIN_REDIRECT` | `LoginSuccessHandler` | `https://bab-muk-dang-client.vercel.app/` | 로그인 성공 뒤 돌아갈 Client 주소 |
| `CORS_ALLOWED_ORIGINS` | `SecurityConfig` (`System.getenv`) | (빈 값) | 쉼표로 구분한 출처 목록. 기본 목록 `https://bab-muk-dang-client.vercel.app`에 더해진다. 끝 슬래시는 지운다. `*`는 무시한다(자격증명 CORS). 로컬 개발이면 `http://localhost:3001`을 넣는다 |
| `AUTH_COOKIE_DOMAIN` | `CookieUtil` (`System.getenv`) | (빈 값) | 비우면 호스트 전용 쿠키다. Client와 API가 같은 상위 도메인을 쓸 때만 넣는다. `.vercel.app`은 Public Suffix라 브라우저가 거부하므로 넣으면 안 된다 |
| `APP_REFERRAL_BASE_URL` | `app.referral.base-url` | (빈 값) | 추천 링크 주소. 우선순위는 이 값 → `CLIENT_BASE_URL` + `/invite` → `CLIENT_LOGIN_REDIRECT`의 origin + `/invite` → 상대 경로 `/invite` |
| `CLIENT_BASE_URL` | `ReferralCommandService` | (빈 값) | 위 우선순위의 두 번째 |
| `APP_REFERRAL_TTL_DAYS` | `app.referral.ttl-days` | `7` | 추천 코드 유효 기간(일) |

쿠키는 항상 `Secure; HttpOnly; SameSite=None; Path=/`로 나간다. 로컬 `http://localhost`는 브라우저가 보안 출처로 취급해서 그대로 동작한다.

## 선택

| 이름 | 속성 / 읽는 곳 | 기본값 | 설명 |
|---|---|---|---|
| `KAKAO_SCOPE` | `...kakao.scope` | `profile_nickname,profile_image` | 이메일 동의를 연 카카오 앱이면 `,account_email`을 더한다. 열지 않은 앱에서 요청하면 KOE205 |
| `AI_OUTBOUND_KEY` | `app.ai.outbound-key` | (빈 값) | Spring → AI 호출에 붙이는 `X-AI-KEY`. 비면 AI 호출을 끈다(기동은 된다. 추천은 503 등으로 실패) |
| `AI_HTTP_TIMEOUT_MILLIS` | `app.ai.timeout-millis` | `3000` | AI 호출 제한 시간 |
| `APP_AI_API_KEY` | `app.ai.api-key` | (빈 값) | AI 서버 → Spring(`/api/v1/ai/**`) 호출을 확인하는 `X-AI-KEY`. 비면 그 호출을 모두 403으로 거부한다 |
| `JWT_REFRESH_REUSE_GRACE_MS` | `jwt.refresh-reuse-grace-ms` | `30000` | 회전 직후 옛 refresh 토큰을 다시 받아 주는 유예(동시 탭) |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `spring.jpa.hibernate.ddl-auto` | `validate` | 운영은 `validate`. 빈 DB를 처음 만들 때만 `create`로 한 번 띄운다([README의 DB 절](../README.md#db-스크립트-적용-순서)) |
| `SPRING_PROFILES_ACTIVE` | | `prod` | 지금 프로필은 `prod` 하나뿐이다 |

토큰 만료(access 1시간, refresh 7일)는 `application.yml`의 `jwt.*-expiration`에 고정돼 있다. 바꾸려면 `JWT_ACCESS_TOKEN_EXPIRATION`, `JWT_REFRESH_TOKEN_EXPIRATION`(밀리초)을 OS 환경변수로 덮어쓴다.

## 같은 `.env`를 쓰는 컨테이너 값

Spring 코드는 읽지 않지만 같은 compose `.env`에 둔다.

| 이름 | 쓰는 곳 | 설명 |
|---|---|---|
| `MYSQL_ROOT_PASSWORD` | mysql 컨테이너, `bootstrap.sh` | root 비밀번호 |
| `MYSQL_DATABASE` | mysql 컨테이너 | `DB_URL`의 DB 이름과 같게 |
| `MYSQL_USER` / `MYSQL_PASSWORD` | mysql 컨테이너 | `DB_USERNAME` / `DB_PASSWORD`와 같게 |

운영 서버의 `.env`는 Socket과 함께 쓰므로 Socket 값(`SPRING_API_SERVER`, `CLIENT_ORIGIN`, `REDIS_URL`, `S3_*`, `CLOUDFRONT_DOMAIN`, `KAKAO_REST_KEY` 등)도 같은 파일에 있다. 목록은 BabMukDang-Socket README에 있다.

## 운영 값 (새 EC2, 2026-11)

비밀이 아닌 공개 설정만 적는다. 서버 `bootstrap.sh`가 이 값을 `.env`에 채운다.

| 이름 | 운영 값 |
|---|---|
| `CLIENT_REDIRECT_PATH` | `https://babmuckdang.vercel.app/login/oauth2/code/kakao` |
| `CLIENT_LOGIN_REDIRECT` | `https://babmuckdang.vercel.app` |
| `CORS_ALLOWED_ORIGINS` | `https://babmuckdang.vercel.app` |
| `AUTH_COOKIE_DOMAIN` | (빈 값) |
| `APP_REFERRAL_BASE_URL` | `https://babmuckdang.vercel.app/invite` |
| `SOCKET_BASE_URL` | `http://socket:3000` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `validate` |
| `KAKAO_SCOPE` | `profile_nickname,profile_image,account_email` |

브라우저는 API 주소(`api.<IP>.sslip.io`)를 직접 부르지 않는다. Vercel rewrite가 `/api`, `/oauth2`, `/login/oauth2`를 같은 출처로 넘긴다. 그래서 redirect URI와 쿠키 호스트가 모두 `babmuckdang.vercel.app`이다. 새 API 경로를 이 세 접두사 밖에 만들면 Client `vercel.json`에도 rewrite를 더해야 한다.

카카오 키(`KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`)는 Parameter Store `/babmuckdang/*`에서 읽는다. DB, JWT, `ROOM_RESULT_KEY`, RabbitMQ, AI 키는 서버에서 만들고 서버 `.env`에만 둔다.
