// 서버 응답 타입. docs/API.md와 같은 모양이다.

export type Role = 'USER' | 'ADMIN';

export type Member = { id: number; loginId: string; name: string; role: Role };

export type Target = {
  certificateId: number;
  certificateName: string;
  year: number;
  round: number;
  examDate: string;
  dDay: number;
};

export type Me = Member & { target: Target | null };

export type CertificateItem = { id: number; name: string; description: string | null };

export type Schedule = {
  id: number;
  year: number;
  round: number;
  registrationStart: string;
  registrationEnd: string;
  examDate: string;
};

export type ExamSummary = { id: number; year: number; round: number; questionCount: number };

export type CertificateDetail = {
  id: number;
  name: string;
  description: string | null;
  timeLimitMinutes: number;
  failScore: number;
  passAverage: number;
  subjects: { number: number; name: string }[];
  schedules: Schedule[];
  exams: ExamSummary[];
};

export type QuestionType = 'MULTIPLE_CHOICE' | 'SHORT_ANSWER';

/** 풀기 전 문제. 정답·해설은 없다. 주관식은 choices가 빈 배열이다. */
export type QuestionView = {
  id: number;
  number: number;
  type: QuestionType;
  subjectNumber: number;
  subjectName: string;
  content: string;
  choices: string[];
  bookmarked: boolean;
};

export type Stats = { solvedCount: number; correctRate: number; mostChosenWrongNo: number | null };

/** 객관식은 answerNo, 주관식은 correctAnswerText(허용 답을 |로 이은 원문)만 채워진다. */
export type SolveResult = {
  correct: boolean;
  answerNo: number | null;
  correctAnswerText: string | null;
  explanation: string | null;
  stats: Stats | null;
};

export type StartResult = {
  attemptId: number;
  startedAt: string;
  timeLimitMinutes: number;
  questions: QuestionView[];
};

export type SubjectScore = { number: number; name: string; score: number; failed: boolean };

export type QuestionResult = {
  questionId: number;
  number: number;
  type: QuestionType;
  selectedNo: number | null;
  answerText: string | null; // 주관식 내 답
  answerNo: number | null;
  correctAnswerText: string | null;
  correct: boolean;
  explanation: string | null;
  stats: Stats | null;
};

export type AttemptResult = {
  attemptId: number;
  averageScore: number;
  passed: boolean;
  subjects: SubjectScore[];
  questions: QuestionResult[];
};

export type NoteItem = { certificateName: string; examYear: number; examRound: number; question: QuestionView };

export type SubjectReport = {
  subjectNumber: number;
  subjectName: string;
  solvedCount: number;
  correctRate: number;
  atRisk: boolean;
};

export type UploadResult = { examId: number; created: number; updated: number; deleted: number };

/** 점수 이력 한 줄 (#32). 제출할 때 저장한 점수다. */
export type HistoryItem = {
  attemptId: number;
  examYear: number;
  examRound: number;
  submittedAt: string;
  averageScore: number;
  passed: boolean;
};
