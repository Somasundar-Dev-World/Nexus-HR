INSERT INTO department (name, description) VALUES ('Engineering', 'Software & Hardware Engineering');
INSERT INTO department (name, description) VALUES ('Management', 'Corporate Management & Leadership');
INSERT INTO department (name, description) VALUES ('Sales', 'Outside and Inside Sales Team');

INSERT INTO employee (first_name, last_name, email, phone, department_id, job_title, status, role, created_at) VALUES ('Jane', 'Doe', 'jane.doe@example.com', '555-0100', 1, 'Software Engineer', 'Active', 'Member', CURRENT_TIMESTAMP);
INSERT INTO employee (first_name, last_name, email, phone, department_id, job_title, status, role, created_at) VALUES ('Michael', 'Scott', 'm.scott@example.com', '555-0101', 2, 'Regional Manager', 'Active', 'Admin', CURRENT_TIMESTAMP);
INSERT INTO employee (first_name, last_name, email, phone, department_id, job_title, status, role, created_at) VALUES ('Dwight', 'Schrute', 'dwight@example.com', '555-0102', 3, 'Asst. Regional Manager', 'On Leave', 'Member', CURRENT_TIMESTAMP);
