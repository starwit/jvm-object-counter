CREATE TABLE gclogsclassid (
    id INTEGER IDENTITY PRIMARY KEY,
    identifier VARCHAR(255) NOT NULL
);

CREATE TABLE gclogscount (
    id INTEGER IDENTITY PRIMARY KEY,
    count INT NOT NULL,
    memsize INT NOT NULL,
    measuretime TIMESTAMP,
    classid INT,
    FOREIGN KEY (classid) REFERENCES gclogsclassid(id)
);