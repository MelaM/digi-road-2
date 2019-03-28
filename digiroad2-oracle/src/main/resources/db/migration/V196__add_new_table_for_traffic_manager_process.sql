CREATE TABLE	TRAFFIC_SIGN_MANAGER
(
	TRAFFIC_SIGN_ID NUMBER NOT NULL,
	LINEAR_ASSET_TYPE_ID NUMBER NOT NULL,
	VALUE VARCHAR2(256),
	CONSTRAINT FK_M_TRAFFIC_SIGN_ID FOREIGN KEY (TRAFFIC_SIGN_ID) REFERENCES ASSET (ID),
	CONSTRAINT FK_LINEAR_ASSET_TYPE_ID FOREIGN KEY (LINEAR_ASSET_TYPE_ID) REFERENCES ASSET_TYPE (ID),
	CONSTRAINT ASSET_ID_LINEAR_ASSET_TYPE UNIQUE (TRAFFIC_SIGN_ID, LINEAR_ASSET_TYPE_ID)
);