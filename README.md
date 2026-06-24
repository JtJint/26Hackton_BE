# Interview Helper Backend

Spring Boot 기반 면접 준비 도우미 백엔드입니다. 프론트엔드가 추출한 이력서 텍스트, 녹음 음성 파일, MediaPipe 시선 지표를 받아 AI 서버와 연동하고 면접 질문/답변 피드백을 제공합니다.

## Stack

- Java 17
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Validation
- SpringDoc OpenAPI Swagger UI
- Gradle

## Architecture

```text
Frontend
  ├─ 이력서 파일에서 텍스트 추출
  ├─ 음성 녹음 파일 생성
  └─ MediaPipe로 눈/얼굴 특징값 계산

Spring Backend
  ├─ 프론트 요청 검증/저장
  ├─ AI 서버 /analyze-resume 호출
  ├─ AI 서버 /synthesize 호출
  ├─ AI 서버 /transcribe 호출
  └─ AI 서버 /feedback 호출

AI Server
  ├─ /analyze-resume: 이력서 기반 질문 생성
  ├─ /synthesize: 질문 텍스트를 면접관 음성으로 합성
  ├─ /transcribe: 음성 전사 + 발화 분석
  └─ /feedback: 답변 내용 + 발화 지표 + 눈 지표 기반 통합 피드백
```

현재 Spring 백엔드는 눈 특징값을 별도 영상 분석하지 않습니다. 프론트가 MediaPipe로 계산한 지표를 전달하고, Spring은 해당 값을 AI 서버 `/feedback` 요청에 포함합니다.

## Run Locally

```bash
./gradlew bootRun
```

프론트 빌드를 건너뛸 때:

```bash
./gradlew bootRun -PskipFrontend
```

기본 포트는 `8080`입니다.

```bash
curl http://localhost:8080/
```

정상 응답:

```json
{
  "status": "ok",
  "message": "interview-helper backend is running"
}
```

## Swagger

로컬 실행 후 접속:

```text
http://localhost:8080/swagger-ui/index.html
```

EC2 배포 후 접속 예시:

```text
http://<EC2_PUBLIC_IP>:8080/swagger-ui/index.html
```

## Environment

AI 서버 주소는 환경변수로 변경할 수 있습니다.

```bash
AI_SERVER_BASE_URL=http://13.125.255.148:8000
```

기본값:

```properties
ai.server.base-url=${AI_SERVER_BASE_URL:http://13.125.255.148:8000}
```

## Frontend Flow

### 1. 회원가입/로그인

회원가입:

```http
POST /api/auth/signup
Content-Type: application/json
```

```json
{
  "email": "min@example.com",
  "nickname": "민",
  "password": "password1234"
}
```

로그인:

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "min@example.com",
  "password": "password1234"
}
```

응답:

```json
{
  "userId": 1,
  "email": "min@example.com",
  "nickname": "민",
  "createdAt": "2026-06-24T22:30:00"
}
```

프론트는 로그인 응답의 `userId`를 이후 이력서 분석 요청에 포함합니다.

### 2. 이력서 분석

프론트는 파일 자체가 아니라, 파일에서 추출한 텍스트와 기본 면접 정보를 보냅니다.

```http
POST /api/resumes/analyze
Content-Type: application/json
```

```json
{
  "userId": 1,
  "jobRole": "백엔드 개발자",
  "careerLevel": "JUNIOR",
  "position": "BACKEND",
  "interviewType": "TECHNICAL",
  "interviewerType": "SOFT",
  "resumeText": "Java와 Spring Boot, MySQL을 사용해 주문 API와 재고 차감 로직을 구현했습니다."
}
```

주요 응답:

```json
{
  "resumeId": 1,
  "userId": 1,
  "summary": "신입 백엔드 개발자를 위한 면접 질문입니다.",
  "recommendedQuestionTopics": [
    "주문 API 구현 경험",
    "Spring Boot 사용 이유"
  ]
}
```

참고: `/api/resumes/text`는 이력서 텍스트 저장만 수행합니다. 질문 생성까지 이어갈 경우 `/api/resumes/analyze` 사용을 권장합니다.

### 3. 예상 질문 생성

```http
POST /api/interviews
Content-Type: application/json
```

```json
{
  "resumeId": 1,
  "questionCount": 5
}
```

응답:

```json
{
  "interviewId": 10,
  "resumeId": 1,
  "userId": 1,
  "questions": [
    {
      "questionId": 101,
      "order": 1,
      "type": "TECH",
      "content": "Spring Boot를 사용한 이유는 무엇인가요?",
      "intent": "기술 이해도와 실무 경험 확인",
      "category": "tech"
    }
  ]
}
```

질문 목록만 다시 조회할 때:

```http
GET /api/interviews/{interviewId}/questions
```

### 4. 질문 TTS 음성 생성

질문 또는 꼬리질문 텍스트를 면접관 음성 MP3 base64로 변환합니다.

```http
POST /api/speech/synthesize
Content-Type: application/json
```

```json
{
  "text": "Spring Boot를 사용한 이유는 무엇인가요?",
  "voice": "nova",
  "interviewerType": "SOFT",
  "instructions": "차분한 한국어 면접관처럼 말해 주세요."
}
```

응답:

```json
{
  "text": "Spring Boot를 사용한 이유는 무엇인가요?",
  "audioBase64": "base64-encoded-mp3",
  "contentType": "audio/mpeg",
  "format": "mp3",
  "voice": "nova"
}
```

프론트에서는 `audioBase64`를 디코딩해 `Blob URL` 또는 `<audio>`로 재생합니다.

### 5. 음성 답변 업로드

프론트는 음성 파일과 MediaPipe 눈 지표를 함께 보냅니다.

```http
POST /api/interviews/{interviewId}/answers/audio
Content-Type: multipart/form-data
```

FormData:

```text
questionId: 101
audio: answer.webm
screenFocusRatio: 0.74
gazeAwayCount: 8
headMovementScore: 82
```

프론트 예시:

```javascript
const formData = new FormData();
formData.append("questionId", String(questionId));
formData.append("audio", audioBlob, "answer.webm");
formData.append("screenFocusRatio", String(screenFocusRatio));
formData.append("gazeAwayCount", String(gazeAwayCount));
formData.append("headMovementScore", String(headMovementScore));

const response = await fetch(`${BASE_URL}/api/interviews/${interviewId}/answers/audio`, {
  method: "POST",
  body: formData
});
```

브라우저에서 `Content-Type`을 직접 지정하지 마세요. `multipart/form-data` boundary는 브라우저가 자동으로 넣어야 합니다.

Spring은 이 음성 파일을 AI 서버 `/transcribe`로 전달하고, 전사 텍스트와 발화 지표를 저장합니다.

응답:

```json
{
  "answerId": 1001,
  "questionId": 101,
  "saved": true,
  "transcript": "저는 생성자 주입을 사용했습니다.",
  "durationSeconds": 72,
  "speechAnalysis": {
    "wordsPerMinute": 128,
    "fillerWordCount": 3,
    "silenceSeconds": 0.0,
    "volumeStabilityScore": 75,
    "repetitionCount": 0,
    "selfCorrectionCount": 0,
    "longPauseCount": 0,
    "maxPauseSeconds": 0.0,
    "avgPauseSeconds": 0.0,
    "disfluencyScore": 0
  }
}
```

### 6. 실시간 꼬리질문 저장

프론트가 OpenAI Realtime API 등으로 생성한 꼬리질문을 새 `questionId`로 면접 질문 목록에 추가합니다. 이 API는 AI 서버를 호출하지 않고 대시보드 기록도 저장하지 않습니다.

```http
POST /api/interviews/{interviewId}/follow-up
Content-Type: application/json
```

요청:

```json
{
  "userId": 1,
  "parentQuestionId": 101,
  "parentAnswerId": 1001,
  "followUpQuestion": "Spring Boot를 선택한 구체적인 이유는 무엇인가요?",
  "gapCriterion": "기술 선택 이유 부족"
}
```

응답:

```json
{
  "parentQuestionId": 101,
  "parentAnswerId": 1001,
  "followUpQuestionId": 102,
  "followUpQuestion": "Spring Boot를 선택한 구체적인 이유와 그로 인해 얻은 이점은 무엇인가요?",
  "gapCriterion": "사용한 기술의 선택 이유가 있는가"
}
```

프론트는 `followUpQuestion`을 화면에 보여주고, 사용자가 답변하면 기존 답변 저장 API에 `questionId = followUpQuestionId`로 다시 저장하면 됩니다.

### 7. 피드백 생성

```http
POST /api/interviews/{interviewId}/feedback
Content-Type: application/json
```

특정 답변만 포함:

```json
{
  "userId": 1,
  "answerIds": [1001]
}
```

전체 답변 기준:

```json
{
  "userId": 1,
  "answerIds": []
}
```

응답:

```json
{
  "interviewId": 10,
  "totalScore": 78,
  "summary": "AI 서버가 답변 내용과 전달 지표를 종합해 피드백을 생성했습니다.",
  "contentFeedback": {
    "score": 80,
    "strength": "생성자 주입을 사용한 이유를 명확히 설명했습니다.",
    "improvement": "실제 프로젝트에서 어떤 문제를 해결했는지 사례를 추가하면 좋습니다."
  },
  "eyeFeedback": {
    "score": 74,
    "strength": "전체 답변 시간 중 화면 응시 비율이 비교적 안정적입니다.",
    "improvement": "시선 이탈이 8회 발생해 핵심 문장에서는 화면을 바라보는 연습이 필요합니다."
  },
  "speechFeedback": {
    "score": 81,
    "strength": "말 속도는 적절한 편입니다.",
    "improvement": "습관어가 일부 반복되어 문장 사이에 짧게 쉬는 연습이 좋습니다."
  },
  "recommendedAnswer": "저는 Spring Boot 프로젝트에서 생성자 주입을 사용했습니다...",
  "gapCriterion": "ROLE_CLARITY",
  "followUpQuestion": "해당 경험에서 본인이 직접 담당한 부분과 의사결정한 내용을 더 구체적으로 설명해 주세요."
}
```

생성된 결과 조회:

```http
GET /api/interviews/{interviewId}/feedback
```

피드백 생성 시 해당 면접이 로그인 사용자 `userId`와 연결되어 있으면 대시보드 기록에 자동 저장됩니다.

### 8. 대시보드 조회

```http
GET /api/dashboard/{userId}
```

응답:

```json
{
  "userId": 1,
  "practiceCount": 3,
  "averageScore": 74,
  "scoreTrend": 8,
  "weakestArea": "content",
  "weakestAreaLabel": "답변 내용",
  "recommendedPractice": "프로젝트 경험을 문제 상황, 본인 역할, 해결 과정, 결과 순서로 말하는 연습을 추천합니다.",
  "areaAverages": {
    "content": 65,
    "eye": 82,
    "speech": 75
  },
  "recentPractices": [
    {
      "resultId": 1,
      "interviewId": 10,
      "totalScore": 78,
      "contentScore": 70,
      "eyeScore": 82,
      "speechScore": 81,
      "summary": "AI 서버가 답변 내용과 전달 지표를 종합해 피드백을 생성했습니다.",
      "contentFeedback": {
        "score": 70,
        "strength": "답변에서 핵심 경험을 설명했습니다.",
        "improvement": "실제 프로젝트 사례와 결과를 더 구체적으로 말하면 좋습니다."
      },
      "eyeFeedback": {
        "score": 82,
        "strength": "화면 응시 비율이 안정적입니다.",
        "improvement": "핵심 문장에서 시선을 더 오래 유지해 보세요."
      },
      "speechFeedback": {
        "score": 81,
        "strength": "말 속도는 적절한 편입니다.",
        "improvement": "문장 사이에 짧은 쉼을 넣으면 더 안정적으로 들립니다."
      },
      "recommendedAnswer": "저는 Spring Boot 프로젝트에서...",
      "gapCriterion": "ROLE_CLARITY",
      "followUpQuestion": "해당 경험에서 본인이 직접 담당한 부분과 의사결정한 내용을 더 구체적으로 설명해 주세요.",
      "questionLogs": [
        {
          "mainQuestionId": 101,
          "mainQuestion": "Spring Boot를 사용한 이유는 무엇인가요?",
          "mainAnswer": "저는 주문 API에서 Spring Boot를 사용했습니다.",
          "followUpQuestionId": 102,
          "followUpQuestion": "Spring Boot를 선택한 구체적인 이유는 무엇인가요?",
          "followUpAnswer": "자동 설정과 트랜잭션 관리가 편해서 선택했습니다."
        }
      ],
      "createdAt": "2026-06-24T22:40:00"
    }
  ]
}
```

## AI Server Contract

### `/analyze-resume`

Spring이 AI 서버로 보내는 요청:

```json
{
  "resumeText": "이력서 전체 텍스트",
  "jobRole": "백엔드 개발자",
  "careerLevel": "JUNIOR",
  "position": "BACKEND",
  "interviewType": "TECHNICAL",
  "interviewerType": "SOFT",
  "numQuestions": 5
}
```

AI 서버 응답:

```json
{
  "summary": "신입 백엔드 개발자를 위한 면접 질문입니다.",
  "questions": [
    {
      "id": "q1",
      "text": "Spring Boot를 사용한 이유는 무엇인가요?",
      "intent": "기술 이해도와 실무 경험 확인",
      "category": "tech"
    }
  ]
}
```

### `/transcribe`

Spring이 AI 서버로 보내는 요청:

```text
multipart/form-data
audio: answer.webm
```

AI 서버 응답:

```json
{
  "transcript": "저는 생성자 주입을 사용했습니다.",
  "durationSeconds": 72,
  "speechAnalysis": {
    "wordsPerMinute": 128,
    "fillerWordCount": 3,
    "silenceSeconds": 0.0,
    "volumeStabilityScore": 75,
    "repetitionCount": 0,
    "selfCorrectionCount": 0,
    "longPauseCount": 0,
    "maxPauseSeconds": 0.0,
    "avgPauseSeconds": 0.0,
    "disfluencyScore": 0
  },
  "pace_status": "ideal",
  "filler_words": [
    {
      "word": "음",
      "count": 2
    }
  ]
}
```

### `/synthesize`

Spring이 AI 서버로 보내는 요청:

```json
{
  "text": "Spring Boot를 사용한 이유는 무엇인가요?",
  "voice": "nova",
  "interviewerType": "SOFT",
  "instructions": "차분한 한국어 면접관처럼 말해 주세요."
}
```

AI 서버 응답:

```json
{
  "text": "Spring Boot를 사용한 이유는 무엇인가요?",
  "audioBase64": "base64-encoded-mp3",
  "contentType": "audio/mpeg",
  "format": "mp3",
  "voice": "nova"
}
```

### `/feedback`

Spring이 AI 서버로 보내는 요청:

```json
{
  "question": "Spring Boot를 사용한 이유는 무엇인가요?",
  "question_intent": "기술 이해도와 실무 경험 확인",
  "answer_transcript": "저는 생성자 주입을 사용했습니다.",
  "metrics": {
    "pace_spm": 320,
    "pace_status": "ideal",
    "filler_total": 3,
    "screenFocusRatio": 0.74,
    "gazeAwayCount": 8,
    "headMovementScore": 82
  },
  "category": "tech",
  "interviewer_type": "SOFT",
  "position": "BACKEND",
  "experience": "JUNIOR"
}
```

AI 서버 응답:

```json
{
  "content_feedback": {
    "score": 80,
    "strength": "생성자 주입을 사용한 이유를 명확히 설명했습니다.",
    "improvement": "실제 프로젝트 사례를 추가하면 좋습니다."
  },
  "eye_feedback": {
    "score": 74,
    "strength": "전체 답변 시간 중 화면 응시 비율이 비교적 안정적입니다.",
    "improvement": "시선 이탈이 8회 발생해 핵심 문장에서는 화면을 바라보는 연습이 필요합니다."
  },
  "speech_feedback": {
    "score": 81,
    "strength": "말 속도는 적절한 편입니다.",
    "improvement": "습관어가 일부 반복되어 문장 사이에 짧게 쉬는 연습이 좋습니다."
  },
  "overall_score": 78,
  "recommended_answer": "저는 Spring Boot 프로젝트에서 생성자 주입을 사용했습니다...",
  "gap_criterion": "ROLE_CLARITY",
  "follow_up_question": "해당 경험에서 본인이 직접 담당한 부분과 의사결정한 내용을 더 구체적으로 설명해 주세요."
}
```

## Eye Analysis Policy

현재 구현은 프론트 MediaPipe 지표를 `/feedback`에 포함하는 방식입니다.

향후 AI 서버에 별도 `/analyze-eye`가 추가되면 다음 구조로 확장할 수 있습니다.

```text
Spring -> AI /transcribe: 음성 전사 + 발화 분석
Spring -> AI /analyze-eye: 눈 지표 기반 시선 분석
Spring -> AI /feedback: 내용 + 발화 분석 + 시선 분석 통합 피드백
```

눈 지표 기준:

- `screenFocusRatio`: 0~1 비율. 예: `0.74`는 74% 화면 응시
- `gazeAwayCount`: 시선 이탈 횟수
- `headMovementScore`: 고개 안정성 점수. 0~100

주의:

- 눈/얼굴 지표로 불안, 거짓말, 성격, 건강 상태를 추정하지 않습니다.
- 시선 지표는 면접 태도와 전달력 관점에서만 평가합니다.

## Enum Values

`careerLevel`

```text
JUNIOR
NEWCOMER
EXPERIENCED
```

`position`

```text
BACKEND
FRONTEND
FULLSTACK
AI
DEVOPS
MOBILE
ETC
```

`interviewType`

```text
TECHNICAL
PERSONALITY
COMPREHENSIVE
```

`interviewerType`

```text
SOFT
PRESSURE
TECH_DEEP
PERSONALITY
```

## Deploy To EC2

```bash
cd ~/26Hackton_BE
git pull origin main
./gradlew clean bootJar
pkill -f interview-helper
nohup java -jar build/libs/interview-helper-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
tail -f app.log
```

AI 서버 주소를 명시해서 실행할 때:

```bash
nohup env AI_SERVER_BASE_URL=http://13.125.255.148:8000 java -jar build/libs/interview-helper-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

MySQL을 사용할 때:

```bash
nohup env \
  AI_SERVER_BASE_URL=http://13.125.255.148:8000 \
  SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/interview_helper?serverTimezone=Asia/Seoul&characterEncoding=UTF-8' \
  SPRING_DATASOURCE_USERNAME='root' \
  SPRING_DATASOURCE_PASSWORD='비밀번호' \
  SPRING_JPA_HIBERNATE_DDL_AUTO=update \
  java -jar build/libs/interview-helper-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

환경변수를 지정하지 않으면 기본으로 H2 파일 DB(`./data/interview-helper`)를 사용합니다.

로그 확인:

```bash
tail -n 100 app.log
```

## Test

```bash
./gradlew test
```

## Error Response

공통 에러 응답:

```json
{
  "code": "AI_SERVER_REQUEST_FAILED",
  "message": "AI 서버 요청에 실패했습니다."
}
```

자주 보는 에러:

- `INVALID_JSON`: JSON 형식 또는 enum 값 오류
- `INVALID_MULTIPART_REQUEST`: `questionId` 또는 `audio` 파일 누락
- `AUDIO_FILE_EMPTY`: 빈 음성 파일 업로드
- `AI_SERVER_REQUEST_FAILED`: AI 서버 호출 실패
- `INTERNAL_SERVER_ERROR`: 처리 중 예상하지 못한 오류
