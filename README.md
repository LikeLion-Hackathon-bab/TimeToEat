# 🍚 밥먹댕(TimeToEat) 🍴 – 멋사대학 13기 해커톤 (전체 참가 247팀 중 백분위 약 16%로 본선 진출)

> **대학가 혼밥/불규칙 식사 문제를 지역 상권과 연결해 해결하는 소셜 미식·약속 플랫폼**  
> 사진 한 장으로 **AI 음식 인식**, **취향 온보딩**, **친구 식사상태 공유**, **실시간 약속/리워드**까지 한 흐름으로 연결합니다.

---
<img width="819" height="462" alt="image" src="https://github.com/user-attachments/assets/c904eafb-fc97-4eee-be22-fd9052331326" />
<img width="818" height="460" alt="image" src="https://github.com/user-attachments/assets/a9d19138-3701-473d-b95d-54ebb968a738" />
<img width="822" height="462" alt="image" src="https://github.com/user-attachments/assets/cef90b8c-1e9d-4849-b471-6c1b84288be8" />
<img width="814" height="461" alt="image" src="https://github.com/user-attachments/assets/21ff88ff-4845-4a60-8d4d-3e6ecc0b7a00" />
<img width="813" height="457" alt="image" src="https://github.com/user-attachments/assets/6c94579a-bc93-48a9-8a41-b07987769d66" />
<img width="815" height="459" alt="image" src="https://github.com/user-attachments/assets/2fb7c23d-fdd7-40b6-a8fb-dce9d9797899" />
<img width="813" height="456" alt="image" src="https://github.com/user-attachments/assets/2905d1bc-f7e9-4314-a5a3-b6839875208b" />
<img width="816" height="450" alt="image" src="https://github.com/user-attachments/assets/e01502ba-ce96-4e5f-874f-1d1b440fb428" />
---

### 1) 서비스 소개
**밥먹댕**은 친구들과 **식사상태(배고픔/식사완료) 공유**, **사진 기반 AI 음식 인식**으로 섭취 기록을 자동화하고, **취향 온보딩/추천**으로 **같이 먹기 좋은 메뉴와 가게를 제안**합니다. 더불어 **오프피크 쿠폰/챌린지**로 지역 상권과 사용자를 연결해 **건강한 루틴**과 **지역경제 활성화**를 동시에 지향합니다.

### 2) 핵심 기능 및 해결 방식
- **AI 음식 인식**: 사진 업로드 → 인퍼런스(백엔드 저장/집계), **최근 N일 조회 코드 API**로 중복 메뉴를 사용자가 다시 먹고 싶은지 조사 후 추천 
- **취향 온보딩/요약**: 선호·비선호·알레르기 저장/조회  
- **친구 식사상태 & 피드**: 24h 홈 피드, 댓글/좋아요, **HUNGRY 필터**  
- **실시간 약속**: 웹소켓(별도 컨테이너)과 연동  
- **리워드/쿠폰/추천인**: 챌린지 보상, 쿠폰 사용, 추천 코드 발급/교환  
- **보안/운영**: Kakao OAuth2 + JWT, Docker + MySQL/Redis/RabbitMQ, GitHub Actions CI/CD

### 3) AI 활용과 실행 전략
- **역할**: (1) 음식 분류·기록 자동화, (2) 취향 기반 추천, (3) **최근 N일 조회**로 식단 다양화  
- **설계**: `WebClient` 타임아웃/재시도, `X-AI-KEY` 검증, 실패 시 메시지큐 기반 보강  
- **실행**: (단기) 캠퍼스 상권 오프피크 쿠폰 제휴 → (중기) 추천·가게 매칭 정교화 → (장기) 건강 루틴/로열티

### 4) 아키텍처 & 코드 스타일
- **레이어 분리**: Controller → Service(**Command/Query**) → Repository → Entity/DTO  
- **DTO 규칙**: 요청 DTO = Bean Validation + `toEntity()` / 응답 DTO = `from(Entity)`  
- **응답 래퍼**: `ApiResponse<T>` / 도메인별 `ErrorCode`  
- **JPA Auditing**: `BaseTimeEntity` 상속으로 `createdAt/updatedAt` 자동 관리

-------
## 🧑 역할
### 최강                                                                                                                                                     
                                                                                                                                                                                   
서버 아키텍처 설계 및 구축                                                                                                                                                       
- 프로젝트 구조 설계                                                                                                                                      
- Docker Compose 기반 배포 환경 구축                                                                                                                                             
- GitHub Actions CI/CD 파이프라인 구성                                                                                                                                           
                                                                                                                                                                                 
카카오 OAuth2 로그인 및 JWT 회원관리 구현                                                                                                                                        
- 카카오 OAuth2 로그인 연동 (global/auth/oauth2/)                                                                                                                                
- JWT Access/Refresh Token 발급 및 검증                                                                                                                                          
- Security Filter Chain 구성                                                                                                                                                     
                                                                                                                                                                                 
공고(Post) 도메인 - 밥약속 모집 기능 구현                                                                                                                                        
- Post CRUD API 및 상태 관리                                                                                                                                       
- 공고 이벤트 처리                                                                                                                                  
- 매칭 서비스 구현                                                                                                                                                               
                                                                                                                                                                                 
초대(Invitation) 도메인 구현                                                                                                                                                     
- 초대 보내기/수락/거절 기능                                                                                                                                                     
- WebSocket 알림 연동
                                                                                                                                                            
참여(Participation) / 계획(Plans) 도메인 구현

<br>

### 김성휘

피드(Article) 및 좋아요 기능 구현                                                                                                                                                
  - 게시글 CRUD API                                                                                                                                                                
  - 홈 피드 조회 (24시간 이내)                                                                                                                                                     
  - 좋아요 토글 기능                                                                                                                                                               
                                                                                                                                                                                   
  댓글 기능 구현                                                                                                                                                                   
  - 댓글/대댓글 작성                                                                                                                                                               
  - 권한 검증 및 Cascade 삭제                                                                                                                                                      
                                                                                                                                                                                   
  AI 연동 기능 구현                                                                                                                                                                
  - AI 사진 추론 게이트웨이 (infra/ai/)                                                                                                                                            
  - 재시도 메커니즘                                                                                                                                                                
                                                                                                                                                                                   
  프로필 관리 및 온보딩 기능 구현                                                                                                                                                  
  - 프로필 조회/수정 API                                                                                                                                                           
  - 온보딩 통합 (프로필 + 선호도)                                                                                                                                                  
                                                                                                                                                                                   
  음식 선호도(Preference) 기능 구현                                                                                                                                                
  - 선호/비선호/알레르기 저장                                                                                                                                                      
  - 음식 코드 관리

                      
  챌린지(Challenge) / 쿠폰(Coupon) / 추천 코드(Referral) 기능 구현

-------
## 🛠 개발과 운영

밥먹댕은 저장소 세 개로 나뉜다.

| 저장소 | 역할 | 기술 |
|---|---|---|
| **TimeToEat** (이 저장소) | REST API, 카카오 로그인, 공고·초대·밥약·피드·쿠폰 | Spring Boot 3.5, Java 17, MySQL 8, Redis 7, RabbitMQ 3 |
| BabMukDang-Socket | 대기실 실시간 진행(Socket.IO), 사진 업로드 presign, 식당·메뉴 검색 | NestJS 11, Node 22 |
| BabMukDang-Client | 웹 앱(PWA) | React, Vite, Vercel |

```
브라우저 ─(같은 출처: /api, /oauth2, /login/oauth2)→ Vercel rewrite ─→ Spring(application:8080)
브라우저 ─(wss, Bearer)──────────────────────────────────────────────→ Socket(socket:3000)
Spring ─(X-Room-Key, 내부망 http://socket:3000)→ Socket   /   Socket ─(X-Room-Key, http://application:8080/api/v1)→ Spring
```

### 로컬 실행

필요한 것: JDK 17, Docker.

```bash
# 1) MySQL, Redis, RabbitMQ (로컬 전용 가짜 비밀번호)
docker run -d --name tte-mysql -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=change-me-root -e MYSQL_DATABASE=timetoeat \
  -e MYSQL_USER=timetoeat -e MYSQL_PASSWORD=change-me mysql:8.0
docker run -d --name tte-redis -p 6379:6379 redis:7-alpine
docker run -d --name tte-rabbit -p 5672:5672 \
  -e RABBITMQ_DEFAULT_USER=timetoeat -e RABBITMQ_DEFAULT_PASS=change-me rabbitmq:3-alpine

# 2) 환경변수
cp .env.example .env        # 값 채우기: docs/ENV.md

# 3) 빈 DB에 스키마를 처음 만들 때만 create로 한 번 띄운다(아래 "DB 스크립트 적용 순서")
set -a; . ./.env; set +a
SPRING_JPA_HIBERNATE_DDL_AUTO=create ./gradlew bootRun   # 기동 로그 "Started" 확인 후 Ctrl+C

# 4) 평소 실행
set -a; . ./.env; set +a
./gradlew bootRun           # http://localhost:8080, Swagger: /swagger-ui/index.html
```

- `.env`를 `set -a`로 환경변수로 넘기는 이유: `CORS_ALLOWED_ORIGINS`, `AUTH_COOKIE_DOMAIN` 등 일부 값은 OS 환경변수로만 읽힌다([docs/ENV.md](docs/ENV.md)).
- 카카오 로그인은 본인 카카오 앱(무료)으로 시험한다. 카카오 콘솔 Redirect URI에 `http://localhost:8080/login/oauth2/code/kakao`를 등록하고 `CLIENT_REDIRECT_PATH`에 같은 값을 넣는다. 로그인 뒤에는 `CLIENT_LOGIN_REDIRECT`(로컬 Client `http://localhost:3001/`)로 돌아간다.
- 대기실까지 띄우려면 BabMukDang-Socket을 `PORT=3000`으로 같이 띄우고 `JWT_SECRET`, `ROOM_RESULT_KEY`를 두 서버에 같은 값으로 넣는다.

### 환경변수

전체 표, 기본값, 운영 값은 [docs/ENV.md](docs/ENV.md)에 있다. 견본은 [.env.example](.env.example)이다. 자주 틀리는 값만 여기 적는다.

| 이름 | 핵심 |
|---|---|
| `JWT_SECRET` | 32바이트 이상. **Socket과 같은 값** |
| `ROOM_RESULT_KEY` | Spring↔Socket 서버 간 키. **Socket과 같은 값**. Client에는 넣지 않는다 |
| `SOCKET_BASE_URL` | Spring이 Socket을 부르는 내부 주소. 기본 `http://socket:3000` |
| `CLIENT_REDIRECT_PATH` | 카카오 콘솔 Redirect URI와 같은 값. 운영은 `https://babmuckdang.vercel.app/login/oauth2/code/kakao` |
| `CLIENT_LOGIN_REDIRECT` | 로그인 후 돌아갈 Client 홈 URL |
| `CORS_ALLOWED_ORIGINS` | 운영 Client 주소(쉼표 구분). 와일드카드 불가 |
| `AUTH_COOKIE_DOMAIN` | 기본 빈 값(호스트 전용 쿠키). Client와 API가 같은 상위 도메인을 쓸 때만 넣는다 |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | 기본 `validate`. 빈 검증용 DB에서만 `create` |

### DB 스크립트 적용 순서

운영 프로필은 `ddl-auto=validate`로 시작 시 스키마를 검사한다. 엔티티와 DB가 다르면 기동하지 않는다. `db/*.sql`은 Hibernate가 만들지 않거나 고치지 않는 부분이다.

| 파일 | 내용 |
|---|---|
| `db/confirmed_meeting.sql` | `confirmed_meeting`, `confirmed_invitation_meeting` 테이블과 외래 키(확정 식당) |
| `db/notification.sql` | `notification`, `push_token` 테이블(알림함, 웹 푸시) |
| `db/invitation_cancel.sql` | `invitation.status` ENUM에 `CANCELLED` 추가. 옛 DB 전용 |

**기존 DB**(이 브랜치 이전 서버가 쓰던 DB): 서버를 새 버전으로 바꾸기 전에 아래 순서로 한 번씩 적용한다.

```bash
mysql -u<user> -p <db> < db/confirmed_meeting.sql
mysql -u<user> -p <db> < db/notification.sql
mysql -u<user> -p <db> < db/invitation_cancel.sql
```

**새 빈 DB**:
1. `SPRING_JPA_HIBERNATE_DDL_AUTO=create`로 한 번 띄워 테이블을 만들고 내린다.
2. `db/confirmed_meeting.sql`, `db/notification.sql`을 적용한다. Hibernate가 이미 만든 테이블은 `ERROR 1050/1061`(이미 있음)이 나는데 무시해도 된다(`mysql --force`).
3. `invitation_cancel.sql`은 적용하지 않는다. 새 DB에는 `CANCELLED`가 이미 들어 있다.
4. Hibernate `create`는 `confirmed_meeting.post_id → post.id`, `confirmed_invitation_meeting.invitation_id → invitation.id` 외래 키를 만들지 않는다. 옛 DB와 맞추려면 두 키를 `ALTER TABLE ... ADD CONSTRAINT`로 더한다(이미 있으면 `ERROR 1826`).
5. `validate`로 다시 띄운다.

운영 서버는 `/opt/babmuckdang/bootstrap.sh`가 1–5를 자동으로 하고, 적용한 파일을 `db/.applied-sql`에 기록한다. 새 `db/*.sql`을 추가하면 배포 스크립트는 **자동 적용하지 않고 경고만** 낸다. 운영 DB에 직접 적용한 뒤 `.applied-sql`에 이름을 더한다.

### 시험

```bash
./gradlew build -x test     # CI(ci.yml)와 같은 확인
./gradlew test              # 단위 시험. TimeToEatApplicationTests는 DB·Redis·RabbitMQ가 떠 있어야 한다
QA_IT=1 ./gradlew test      # 실제 MySQL·Redis·RabbitMQ에 붙는 통합 시험(qa 패키지)도 실행
```

### 배포 흐름

| 단계 | 내용 |
|---|---|
| PR | `ci.yml`이 PR마다 `./gradlew clean build -x test`를 돌린다 |
| 병합 | `main`에 push되면 `cd.yml`이 실행된다 |
| 인증 | GitHub OIDC로 AWS 역할을 받는다(장기 키 없음). 저장소 Variables: `AWS_ROLE_ARN`, `AWS_REGION`, `EC2_INSTANCE_ID` |
| 배포 | SSM Run Command로 EC2에서 `/opt/babmuckdang/deploy.sh application <전체 SHA>`를 실행한다. 서버가 그 커밋을 받아 이미지를 빌드하고, 직전 이미지를 `:prev`로 남긴다. 헬스 체크가 실패하면 되돌린다 |
| 구성 | EC2 한 대에서 Docker Compose로 application, socket, mysql, redis, rabbitmq, food-ai, recommend-ai, caddy(자동 TLS)를 띄운다. 호스트에 여는 포트는 caddy 80/443뿐이다 |
| DB | 새 `db/*.sql`은 자동 적용하지 않는다(위 절) |

- 저장소의 `docker-compose.prod.yml`, `nginx/`는 해커톤 때(`babmuckdang.site`, Docker Hub 이미지) 구성이다. 지금 운영 서버는 이 파일을 쓰지 않는다. 도메인은 만료됐다.
- 브라우저는 API 주소를 직접 부르지 않는다. Client의 Vercel rewrite가 `/api`, `/oauth2`, `/login/oauth2`를 Spring으로 넘긴다. 이 세 접두사 밖에 새 경로를 만들면 Client `vercel.json`도 고쳐야 한다.
- 운영 비밀(DB, JWT, `ROOM_RESULT_KEY`, 카카오 키 등)은 서버 `.env`와 SSM Parameter Store에만 둔다. 저장소, 이슈, 채팅에 쓰지 않는다.
