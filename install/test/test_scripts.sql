select group_event_id as id, show_id as showId, channel_id as channelId, show_time as showTime, master_mdn as masterMdn, create_time as createTime
from GROUP_EVENT
where show_time between CURRENT_TIMESTAMP AND {fn TIMESTAMPADD(SQL_TSI_MINUTE, 15, CURRENT_TIMESTAMP)};

select mdn from group_member
where group_event_id = 1 and MEMBER_STATUS = 'ACCEPTED';


select * from group_event;

-- add an group event
INSERT INTO GROUP_EVENT (
	GROUP_EVENT_ID, 
	SHOW_ID,
	CHANNEL_ID,
	SHOW_TIME,
	SHOW_NAME,
	MASTER_MDN
) VALUES (
	NEXT VALUE FOR GROUP_EVENT_PK_SEQ,
	'show1',
	'channel1',
	{fn TIMESTAMPADD(SQL_TSI_MINUTE, 10, CURRENT_TIMESTAMP)},
	'super bowl',
	'9250000001'
);

INSERT INTO GROUP_MEMBER (
	GROUP_EVENT_ID,
	MDN,
	MEMBER_STATUS,
	MEMBER_NAME
) VALUES (
	?, ?, 'INVITED', ?
);

select * from group_member;

-- add group members
INSERT INTO GROUP_MEMBER (GROUP_EVENT_ID, MDN, MEMBER_STATUS)
VALUES (1, '9250000001', 'MASTER');
INSERT INTO GROUP_MEMBER (GROUP_EVENT_ID, MDN, MEMBER_STATUS)
VALUES (1, '9250001001', 'INVITED');
INSERT INTO GROUP_MEMBER (GROUP_EVENT_ID, MDN, MEMBER_STATUS)
VALUES (1, '9250001002', 'ACCEPTED');
INSERT INTO GROUP_MEMBER (GROUP_EVENT_ID, MDN, MEMBER_STATUS)
VALUES (1, '9250001003', 'ACCEPTED');
INSERT INTO GROUP_MEMBER (GROUP_EVENT_ID, MDN, MEMBER_STATUS)
VALUES (1, '9250001004', 'DECLINED');
	


update group_event
set show_name = 'super bowl'
where group_event_id = 1;


alter table GROUP_MEMBER
add column device_id varchar(1024) default null;

alter table GROUP_MEMBER
add column member_name varchar(512) default '' not null;


update GROUP_MEMBER
set MEMBER_STATUS = 'ACCEPTED'
where GROUP_EVENT_ID = 1 and MDN = '1111111111';

alter table GROUP_EVENT
add column REMINDER_SENT integer default 0;




select g.group_event_id, g.mdn, u.name, g.last_channel_id from GROUP_MEMBER g, USERS u
where u.mdn = g.mdn and g.member_status = 'MASTER' or g.member_status = 'ACCEPTED'
order by g.GROUP_EVENT_ID, g.mdn;


--------------------------------------------------------
-- 2013-07-17

alter table GROUP_MEMBER
add column LAST_CHANNEL_ID varchar(512) default NULL;

CREATE TABLE CHANNELS (
	CHANNEL_ID VARCHAR(512) PRIMARY KEY NOT NULL,
	CHANNEL_NAME VARCHAR(512) NOT NULL,
	CHANNEL_DESC VARCHAR(1024) DEFAULT NULL
);


INSERT INTO CHANNELS (CHANNEL_ID,CHANNEL_NAME)
VALUES('0001', 'CBS');

alter table GROUP_MEMBER
DROP COLUMN MEMBER_NAME;

ALTER TABLE GROUP_MEMBER
DROP COLUMN DEVICE_ID;

CREATE TABLE USERS (
	MDN VARCHAR(128) NOT NULL,
	CHANNEL_ID VARCHAR(512) NOT NULL,
	"NAME" VARCHAR(512) NOT NULL
);


CREATE INDEX IDX1_USERS ON USERS(MDN);



--========================================
-- Fred

CREATE TABLE FLING (
	FLING_ID INTEGER PRIMARY KEY NOT NULL,
	CONTENT_TYPE VARCHAR(128) NOT NULL,
	FILE_PATH VARCHAR(256) NOT NULL,
	CREATE_TIME TIMESTAMP DEFAULT CURRENT TIMESTAMP NOT NULL
);


-------------------------------------------------
-- hud 2013-07-17 2
drop index IDX1_USERS;

CREATE UNIQUE INDEX IDX1_USERS ON USERS(MDN);

delete from channels;

INSERT INTO CHANNELS (CHANNEL_ID, CHANNEL_NAME)
VALUES('001##10001', 'channel_1');

INSERT INTO CHANNELS (CHANNEL_ID, CHANNEL_NAME)
VALUES('002##10001', 'channel_2');

INSERT INTO CHANNELS (CHANNEL_ID, CHANNEL_NAME)
VALUES('003##10001', 'channel_2');

INSERT INTO CHANNELS (CHANNEL_ID, CHANNEL_NAME)
VALUES('004##10001', 'channel_2');

----------------------------

INSERT INTO USERS (MDN, "NAME", CHANNEL_ID)
VALUES ('9250000001', 'John Smith', '001##10001');

INSERT INTO USERS (MDN, "NAME", CHANNEL_ID)
VALUES ('9250000002', 'Joe Smith', '002##10001');

INSERT INTO USERS (MDN, "NAME", CHANNEL_ID)
VALUES ('9250000003', 'Marry Smith', '003##10001');

INSERT INTO USERS (MDN, "NAME", CHANNEL_ID)
VALUES ('9250000004', 'Alice Lee', '004##10001');





