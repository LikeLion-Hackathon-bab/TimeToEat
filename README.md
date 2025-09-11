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
