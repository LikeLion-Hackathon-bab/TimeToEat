# 웹 푸시(FCM HTTP v1)

알림(`NotificationCommandService.notify`)이 저장되면 수신자의 등록 토큰(`push_token` 표, `POST /api/v1/push/tokens`)마다
FCM HTTP v1 으로 웹 푸시를 보낸다. 푸시는 최선 노력 발송이다. 실패해도 알림은 저장돼 있어 앱 알림함에서 보인다.

| 구성 요소 | 파일 |
|---|---|
| 발송기 선택 | `infra/push/PushSenderConfig.java` |
| FCM 발송기 | `infra/push/FcmHttpV1PushSender.java` |
| 서비스 계정 읽기 | `infra/push/FcmServiceAccount.java` |
| 자격 증명 없을 때(로그만) | `infra/push/LoggingPushSender.java` |

## 동작

- `FCM_SERVICE_ACCOUNT_JSON` 이 비어 있으면 `LoggingPushSender` 를 쓴다. 기동 로그에 `웹 푸시: FCM_SERVICE_ACCOUNT_JSON 없음` 이 찍히고, 발송 대신 `PUSH(dry-run)` 로그만 남는다.
- 값이 있으면 `FcmHttpV1PushSender` 를 쓴다. 기동 로그는 `웹 푸시: FCM HTTP v1 project=<project_id>` 이다. 값이 있는데 파일이 없거나 JSON 이 깨져 있으면 **기동이 실패**한다. 운영에서 푸시가 꺼진 줄 모른 채 배포되는 일을 막으려는 것이다.
- 서비스 계정 키로 RS256 JWT 를 서명해 `https://oauth2.googleapis.com/token` 에서 access token(scope `https://www.googleapis.com/auth/firebase.messaging`)을 받는다. 받은 토큰은 만료 1분 전까지 다시 쓴다.
- 토큰마다 `POST https://fcm.googleapis.com/v1/projects/{project_id}/messages:send` 를 한 번씩 호출한다. 본문은 `notification{title,body}` 와 `data{url}` 이다. `FCM_LINK_BASE_URL` 이 https 주소이면 `webpush.fcm_options.link=<base><url>` 도 붙인다.
- 응답 처리:

| 응답 | 처리 |
|---|---|
| 2xx | 성공 |
| 404 + `errorCode: UNREGISTERED` | 해당 `push_token` 행 삭제 |
| 400 `INVALID_ARGUMENT` 이고 등록 토큰이 원인(`message.token` 필드 위반 또는 "registration token" 메시지) | 해당 행 삭제 |
| 그 밖의 400·404 (본문 형식 오류, 잘못된 project_id) | 로그만 남긴다. 정상 토큰을 지우지 않는다 |
| 401 | 로그를 남기고 캐시한 access token 을 버린다. 다음 발송 때 새로 받는다 |
| 429, 5xx | 재시도하지 않고 로그만 남긴다 |

- 연결 3초, 응답 5초 제한. 로그에는 토큰 값 대신 `push_token.id` 를 남긴다.

## 설정(환경변수)

| 변수 | 기본 | 설명 |
|---|---|---|
| `FCM_SERVICE_ACCOUNT_JSON` | 비어 있음 | 서비스 계정 JSON **파일 경로(권장)**. base64 로 인코딩한 JSON 도 받는다 |
| `FCM_LINK_BASE_URL` | 비어 있음 | 알림을 눌렀을 때 열 Client 주소(예 `https://<client 도메인>`). 비우면 `data.url` 만 보낸다 |
| `FCM_TOKEN_URI` | 비어 있음 | 시험용. 비우면 JSON 의 `token_uri` 를 쓴다 |
| `FCM_API_BASE_URL` | `https://fcm.googleapis.com` | 시험용 |

## 자격 증명 넣기(운영)

자격 증명은 사용자가 직접 만든다. 저장소에는 두지 않는다.

1. Firebase 프로젝트 `timetoeat-6d2b7` → 프로젝트 설정 → 서비스 계정 → "새 비공개 키 생성"으로 JSON 을 받는다.
2. 그 JSON 을 Parameter Store `/babmuckdang/fcm_service_account`(SecureString)에 넣는다.
   ```bash
   aws ssm put-parameter --name /babmuckdang/fcm_service_account --type SecureString \
     --value file://timetoeat-6d2b7-firebase-adminsdk.json --overwrite
   ```
   내려받은 JSON 파일은 넣은 뒤 지운다.
3. 서버(배포 스크립트)가 파라미터를 파일로 쓴다. 권한은 600 으로 둔다.
   ```bash
   install -d -m 700 /opt/babmuckdang/secrets
   aws ssm get-parameter --name /babmuckdang/fcm_service_account --with-decryption \
     --query Parameter.Value --output text > /opt/babmuckdang/secrets/fcm-service-account.json
   chmod 600 /opt/babmuckdang/secrets/fcm-service-account.json
   ```
4. 컨테이너에 읽기 전용으로 붙이고 경로를 넘긴다(`docker-compose.prod.yml` 의 `application`).
   ```yaml
   volumes:
     - /opt/babmuckdang/secrets/fcm-service-account.json:/run/secrets/fcm-service-account.json:ro
   environment:
     FCM_SERVICE_ACCOUNT_JSON: /run/secrets/fcm-service-account.json
     FCM_LINK_BASE_URL: https://<client 도메인>
   ```
   호스트 파일이 없는데 볼륨을 붙이면 Docker 가 그 자리에 빈 디렉터리를 만든다. 3번을 먼저 한다.
   파일을 붙일 수 없는 환경이면 `.env` 에 `FCM_SERVICE_ACCOUNT_JSON=$(base64 -w0 fcm-service-account.json)` 으로 넣어도 된다.
5. 재기동한 뒤 로그에 `웹 푸시: FCM HTTP v1 project=timetoeat-6d2b7` 가 찍히는지 본다.

Client 쪽 설정(VITE_FIREBASE_*, VAPID 키)은 BabMukDang-Client 의 `.env.example` 에 있다. 서비스 계정과 Client 웹 앱 설정은 같은 Firebase 프로젝트여야 한다. 다른 프로젝트에서 발급한 등록 토큰으로 보내면 FCM 이 거부한다.

## 시험

`src/test/java/com/example/timetoeat/infra/push/FcmHttpV1PushSenderTest.java` 는 `com.sun.net.httpserver` 모의 서버로 토큰 엔드포인트와 FCM 을 대신한다. 실제 Google 을 호출하지 않는다.
다음을 확인한다. 서명한 JWT(iss·scope·aud), 요청 본문, access token 재사용, UNREGISTERED·무효 토큰 삭제, 429·5xx 를 재시도하지 않는지, 토큰과 무관한 오류에서 토큰을 지우지 않는지, 설정 유무에 따라 발송기가 바뀌는지.

```bash
bash gradlew --max-workers=1 test --tests 'com.example.timetoeat.infra.push.*'
```
