CREATE INDEX IF NOT EXISTS idx_attendance_member_year_semester_month
    ON public.attendance (student_id, year, semester, (EXTRACT(MONTH FROM date)));

CREATE INDEX IF NOT EXISTS idx_attendance_member_year_semester
    ON public.attendance (student_id, year, semester);
