# Git 컨벤션

> 커밋·브랜치·이슈·PR 규칙이에요. TEAM-HASHI/HASHI-SERVER의 규칙을 이 저장소에 맞게 옮겼어요.

---

## 1. 커밋 메시지

```bash
git commit -m "<type>(<scope>): <subject> (#issue)"
```

- **type**: 커밋 종류 (필수)
- **scope**: 바꾼 영역 (선택, 아래 표)
- **subject**: 짧은 설명 (필수)
- body: 무엇을, 왜 바꿨는지 (선택)

### Type

| Type | 설명 | 예시 |
| --- | --- | --- |
| `feat` | 새 기능 | `feat(post): 나의 공고 삭제 API 추가 (#12)` |
| `fix` | 버그 수정 | `fix(participation): 마지막 자리 동시 신청 시 409 반환 (#34)` |
| `docs` | 문서 | `docs(readme): 로컬 실행 방법 추가` |
| `refactor` | 동작은 그대로, 구조만 정리 | `refactor(post): 상태 계산을 함수로 분리` |
| `perf` | 성능 | `perf(participation): 목록 조회 쿼리 줄이기` |
| `test` | 테스트 | `test(post): 마지막 자리 경합 시험 추가` |
| `build` | 빌드·의존성 | `build(deps): Spring Boot 3.5.15로 올림` |
| `ci` | CI·배포 설정 | `ci(github): 운영 배포를 SSM으로` |
| `chore` | 기타(소스 변경 없음) | `chore: .gitignore 정리` |
| `revert` | 되돌리기 | `revert: fix(participation) 되돌림` |

### 작성 규칙

- subject는 50자 이내, 마침표 없이, "추가함" 말고 "추가"처럼 씁니다.
- body에는 무엇을, 왜 바꿨는지 씁니다. 어떻게는 코드에 있어요.
- 이슈 번호는 subject 끝 `(#123)`에 붙입니다.
- 커밋 메시지에 `Co-Authored-By` 줄을 넣지 않습니다.

```text
✅ fix(participation): 마지막 자리 동시 신청 시 409 반환 (#34)
❌ 수정함   ❌ Update code   ❌ feat: 작업   ❌ 버그수정.
```

### 커밋 단위

1. scope 하나에 커밋 하나. 한 영역 안에서 끝냅니다.
2. type 하나로 설명되면 적당합니다. 두 개가 필요하면 나눕니다.
3. 커밋마다 빌드와 테스트가 통과해야 합니다.
4. 여러 영역에 걸치면 불리는 쪽을 먼저, 부르는 쪽을 나중에 커밋합니다.

---

## 2. Scope

| scope | 범위 |
| --- | --- |
| `auth` | 로그인·토큰·OAuth (global/auth) |
| `member` | 회원·프로필 |
| `preference` | 온보딩 취향·알레르기 |
| `post` | 공고 |
| `participation` | 공고 참여·대기실 준비 |
| `invitation` | 개별 초대 |
| `plans` | 확정 밥약(내 밥약) |
| `article` | 피드 게시글·댓글·좋아요 |
| `meal` | 식사 상태 |
| `friend` | 친구 |
| `notification` | 알림함·동네소식 |
| `push` | 웹 푸시 토큰·발송 |
| `challenge` | 챌린지 |
| `coupon` | 쿠폰 |
| `recommendation` | 메뉴 추천(AI) |
| `referral` | 추천인 코드 |
| `subscription` | 공고 알림 구독 |
| `event` | 도메인 이벤트·메시지 큐 (global/event) |
| `config` | 설정·보안·CORS (global/config) |
| `error` | 오류 응답 (global/error) |

---

## 3. 브랜치

```text
feat/#{이슈번호}/{기능명}     예: feat/#12/announcement-cancel
fix/#{이슈번호}/{버그명}      예: fix/#34/last-seat-race
docs/#{이슈번호}/{주제}       예: docs/#56/git-convention
chore/#{이슈번호}/{주제}      예: chore/#78/gitignore
```

- 소문자로 쓰고, 이슈 번호를 꼭 넣습니다. `feat/login`처럼 번호가 없으면 안 됩니다.
- 한 브랜치에는 한 가지 일만 담습니다.
- 작업 브랜치는 `main`에 바로 병합하지 않습니다. 지금은 통합 PR의 브랜치(`fix/three-friend-flow`)로 PR을 보내고, 통합 PR이 `main`에 병합됩니다.
- 병합한 브랜치는 지웁니다.

---

## 4. 이슈 / PR

- 이슈 제목: `[Type] 작업 내용` (예: `[Bug] 마지막 자리 동시 신청 시 두 명이 모두 참여됨`)
- PR 제목: `[Type] 작업 내용 (#이슈번호)` (커밋의 `type(scope): subject`와 달라요)
- PR 템플릿의 모든 항목을 채웁니다.
- 리뷰 승인 1명 이상, CI 통과 뒤 병합합니다.
- `Closes #N`은 기본 브랜치에 병합될 때만 이슈를 닫습니다. 통합 브랜치로 가는 PR은 `Refs #N`으로 쓰고, `Closes` 줄은 통합 PR에 모읍니다.

템플릿 파일은 `.github/`에 있어요(`ISSUE_TEMPLATE/issue-form.yml`, `pull_request_template.md`).
