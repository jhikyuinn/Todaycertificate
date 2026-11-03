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

/** 풀기 전 문제. 정답·해설은 없다. */
export type QuestionView = {
  id: number;
  number: number;
  subjectNumber: number;
  subjectName: string;
  content: string;
  choices: string[];
  bookmarked: boolean;
};

export type Stats = { solvedCount: number; correctRate: number; mostChosenWrongNo: number | null };

export type SolveResult = { correct: boolean; answerNo: number; explanation: string | null; stats: Stats | null };

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
  selectedNo: number | null;
  answerNo: number;
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
