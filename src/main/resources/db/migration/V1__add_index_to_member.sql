CREATE INDEX IF NOT EXISTS idx_member_year_class_role
    ON member (year, class_id, role);
