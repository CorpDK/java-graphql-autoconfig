-- src/main/resources/data.sql
INSERT INTO department (name) VALUES ('HR');
INSERT INTO department (name) VALUES ('IT');

INSERT INTO employee (name, department_id) VALUES ('Alice', 1);
INSERT INTO employee (name, department_id) VALUES ('Bob', 2);
