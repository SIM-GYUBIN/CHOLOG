INSERT INTO user (created_at, email, nickname, password)
VALUES (NOW(), 'test1@email.com', 'testman1', 'testpassword');

INSERT INTO user (created_at, email, nickname, password)
VALUES (NOW(), 'test2@email.com', 'testman2', 'testpassword');

INSERT INTO project (created_at, name, jira_token, mm_url, project_token)
VALUES (NOW(), 'project1', '123', 'testurl1', '123123');

INSERT INTO project (created_at, name, jira_token, mm_url, project_token)
VALUES (NOW(), 'project2', '456', 'testurl2', '456456');

INSERT INTO project_user (is_creator, project_id, user_id)
VALUES (true, 1, 1);

INSERT INTO project_user (is_creator, project_id, user_id)
VALUES (false, 1, 2);

INSERT INTO project_user (is_creator, project_id, user_id)
VALUES (true, 2, 2);

INSERT INTO project_user (is_creator, project_id, user_id)
VALUES (false, 2, 1);