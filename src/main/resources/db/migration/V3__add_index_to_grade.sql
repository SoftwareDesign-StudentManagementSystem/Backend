CREATE INDEX IF NOT EXISTS idx_grade_member_year_semester
    ON public.grade (student_id, year, semester);
