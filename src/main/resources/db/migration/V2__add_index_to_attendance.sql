-- 인덱스1: member_id, year, semester, month
CREATE INDEX IF NOT EXISTS idx_attendance_month_lookup
    ON attendance (id, year, semester, EXTRACT(MONTH FROM date));

-- 인덱스2: member_id, year, semester
CREATE INDEX IF NOT EXISTS idx_attendance_member_year_semester
    ON attendance (id, year, semester);
