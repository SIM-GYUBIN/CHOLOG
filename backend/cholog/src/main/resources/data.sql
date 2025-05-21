INSERT INTO user (created_at, email, nickname, password, user_type)
VALUES (NOW(), 'test1@email.com', 'testman1', 'testpassword', 'GENERAL');

INSERT INTO user (created_at, email, nickname, password, user_type)
VALUES (NOW(), 'test2@email.com', 'testman2', 'testpassword', 'GENERAL');

INSERT INTO project (created_at, name, project_token)
VALUES (NOW(), 'project1', '123123');

INSERT INTO project (created_at, name, project_token)
VALUES (NOW(), 'project2', '456456');

INSERT INTO project_user (is_creator, project_id, user_id)
VALUES (true, 1, 1);

INSERT INTO project_user (is_creator, project_id, user_id)
VALUES (false, 1, 2);

INSERT INTO project_user (is_creator, project_id, user_id)
VALUES (true, 2, 2);

INSERT INTO project_user (is_creator, project_id, user_id)
VALUES (false, 2, 1);