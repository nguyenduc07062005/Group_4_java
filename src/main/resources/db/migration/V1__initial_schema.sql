create table users (
    id bigint primary key auto_increment,
    username varchar(100) not null,
    password_hash varchar(255) not null,
    full_name varchar(150) not null,
    role varchar(50) not null,
    enabled boolean not null default true,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_users_username unique (username)
);

create table semesters (
    id bigint primary key auto_increment,
    code varchar(50) not null,
    name varchar(150) not null,
    start_date date not null,
    end_date date not null,
    archived boolean not null default false,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_semesters_code unique (code)
);

create table assignments (
    id bigint primary key auto_increment,
    semester_id bigint not null,
    title varchar(150) not null,
    description text null,
    grading_mode varchar(30) not null,
    total_score decimal(5,2) not null default 100.00,
    plagiarism_threshold decimal(5,2) not null default 80.00,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_assignments_semester
        foreign key (semester_id) references semesters(id)
);

create table problems (
    id bigint primary key auto_increment,
    assignment_id bigint not null,
    problem_order int not null,
    title varchar(150) not null,
    max_score decimal(5,2) not null default 0.00,
    input_mode varchar(30) not null default 'STDIN',
    output_comparison_mode varchar(30) not null default 'EXACT',
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_problems_assignment
        foreign key (assignment_id) references assignments(id),
    constraint uk_problems_assignment_order unique (assignment_id, problem_order)
);

create table test_cases (
    id bigint primary key auto_increment,
    problem_id bigint not null,
    case_order int not null,
    input_data text null,
    expected_output text null,
    sample boolean not null default false,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_test_cases_problem
        foreign key (problem_id) references problems(id),
    constraint uk_test_cases_problem_order unique (problem_id, case_order)
);

create index idx_assignments_semester_id on assignments(semester_id);
create index idx_problems_assignment_id on problems(assignment_id);
create index idx_test_cases_problem_id on test_cases(problem_id);
