INSERT INTO sub_teams (team_name, sub_team_name, leader_user_id)
VALUES
  ('미디어사역팀', '청바지TV', NULL),
  ('미디어사역팀', '조이힐그램', NULL)
ON CONFLICT DO NOTHING;
INSERT INTO sub_team_members (sub_team_id, user_id)
SELECT
  (SELECT id FROM sub_teams WHERE team_name = '미디어사역팀' AND sub_team_name = '청바지TV'),
  id
FROM users
WHERE name IN ('김세진', '김예현', '김주현', '신주안', '최찬혁', '채승아', '홍성민')
ON CONFLICT DO NOTHING;
INSERT INTO sub_team_members (sub_team_id, user_id)
SELECT
  (SELECT id FROM sub_teams WHERE team_name = '미디어사역팀' AND sub_team_name = '조이힐그램'),
  id
FROM users
WHERE name IN ('이경헌', '정이듬', '윤하영', '김다혜', '채승찬', '신주안', '박예성')
ON CONFLICT DO NOTHING;