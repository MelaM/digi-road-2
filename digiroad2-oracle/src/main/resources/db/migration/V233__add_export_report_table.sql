CREATE TABLE export_report (
  ID NUMBER(38,0),
  CONTENT CLOB,
  FILE_NAME VARCHAR2(512),
  EXPORTED_ASSETS VARCHAR2(2048),
  MUNICIPALITIES VARCHAR2(2048),
  STATUS NUMBER(3,0) DEFAULT 1 NOT NULL ENABLE,
  CREATED_BY VARCHAR2(256),
  CREATED_DATE DATE DEFAULT sysdate NOT NULL ENABLE,

  CONSTRAINT export_report_pk PRIMARY KEY (ID)
);

CREATE INDEX export_report_created_by_idx ON export_report (created_by);

/* Obstacles Asset = 220
   Change the name from esterakennelma to Esterakennelma */
UPDATE ASSET_TYPE
SET name = TRIM(INITCAP(name))
WHERE ID = 220;