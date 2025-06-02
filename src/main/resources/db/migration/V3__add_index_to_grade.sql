-- 복합 인덱스 추가: member_id + year + semester
CREATE INDEX IF NOT EXISTS idx_member_year_semester ON grade (id, year, semester);
