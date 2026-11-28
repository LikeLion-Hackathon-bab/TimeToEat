# 시연 로그인 (카카오 계정 없이 시연)

심사·시연 때 카카오 계정 없이 시연 회원 5명으로 로그인한다. **기본은 꺼짐**이고, 꺼져 있으면 `/api/v1/auth/demo-login`은 404다.

| 계정 번호 | 화면 이름 | 내부 이메일(화면에 안 보임) |
|---|---|---|
| 1 | 김민준 | qa_demo1@users.noreply.babmuckdang |
| 2 | 최서연 | qa_demo2@users.noreply.babmuckdang |
| 3 | 박지윤 | qa_demo3@users.noreply.babmuckdang |
| 4 | 강태민 | qa_demo4@users.noreply.babmuckdang |
| 5 | 이소율 | qa_demo5@users.noreply.babmuckdang |

## 켜기 (Spring)

| 환경변수 | 값 | 설명 |
|---|---|---|
| `DEMO_LOGIN_ENABLED` | `true` | 켤 때만. 그 밖의 값이면 꺼짐 |
| `DEMO_LOGIN_SECRET` | 32자 이상 임의 문자열 | 시연 코드. 짧거나 없으면 **기동 실패**(잘못 켠 채로 뜨지 않게) |
| `DEMO_LOGIN_UNTIL` | 선택. ISO 시각 예 `2026-12-05T23:59:00+09:00` | 지나면 자동으로 404(꺼진 것과 같음) |

- 비밀값 만들기: `openssl rand -base64 36`
- 켜고 기동하면 시연 회원 5명이 없을 때만 만들어진다(여러 번 기동해도 중복 없음). 로그에 `시연 로그인이 켜져 있습니다` WARN이 남는다.
- Client(Vercel)는 `VITE_DEMO_LOGIN=true`로 빌드해야 `/login`에 "시연 계정으로 시작" 버튼이 보인다. 시연 코드는 화면에서 입력한다(빌드에 넣지 않는다).

## 요청

`POST /api/v1/auth/demo-login` 본문 `{"secret":"<DEMO_LOGIN_SECRET>","account":1}` (1~5)

| 결과 | 응답 |
|---|---|
| 성공 | 200 `{"code":"OK","data":{"accessToken":"...","signupRequired":true}}` + `refreshToken` HttpOnly 쿠키(카카오 로그인과 같음). `signupRequired`가 true면 온보딩으로 |
| 시연 코드 틀림 | 403 `DEMO_403` |
| 계정 번호가 1~5 밖 | 400 `DEMO_400` |
| 같은 IP 1분 10회 초과 | 429 `DEMO_429` |
| 꺼짐·`DEMO_LOGIN_UNTIL` 지남 | 404 |

## 시연이 끝나면 끄기

1. 서버 환경변수에서 `DEMO_LOGIN_ENABLED`를 지우거나 `false`로 바꾸고 Spring을 다시 띄운다(또는 처음부터 `DEMO_LOGIN_UNTIL`을 시연 종료 시각으로).
2. Vercel에서 `VITE_DEMO_LOGIN`을 지우고 다시 배포한다.
3. 시연 회원 데이터는 남는다. 지우려면 `qa_demo%@users.noreply.babmuckdang` 회원과 그 게시물·약속을 정리한다.

## 보안 고려

- 시연 회원은 코드에 고정된 이메일 5개뿐이고, 소셜 계정이 연결된 회원(실제 사용자)은 어떤 경우에도 고를 수 없다. 요청 본문으로 회원 id·이메일을 받지 않는다.
- 시연 코드는 상수 시간 비교(SHA-256 후 `MessageDigest.isEqual`), 32자 이상 강제, IP별 분당 10회 제한. 시도는 계정 번호·IP만 WARN으로 남긴다(코드 값은 남기지 않음).
- 켜진 동안에는 시연 코드를 아는 사람 누구나 시연 회원으로 들어올 수 있다. 시연 코드는 심사 자리에서만 알려 주고, 끝나면 바로 끈다. 시연 코드를 Client 번들·저장소·스크린샷에 넣지 않는다.
- IP 제한은 서버 메모리라 서버 1대 기준이다. 여러 대로 늘리면 Redis 기반 제한으로 바꾼다.
