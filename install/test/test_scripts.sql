select group_event_id as id, show_id as showId, channel_id as channelId, show_time as showTime, master_mdn as masterMdn, create_time as createTime
from GROUP_EVENT
where show_time between CURRENT_TIMESTAMP AND {fn TIMESTAMPADD(SQL_TSI_MINUTE, 15, CURRENT_TIMESTAMP)};



select group_event_id as id, show_id as showId, channel_id as channelId, show_time as showTime, 
show_name as showName, master_mdn as masterMdn, create_time as createTime
from GROUP_EVENT
where reminder_sent = 0 and
show_time between CURRENT_TIMESTAMP AND {fn TIMESTAMPADD(SQL_TSI_MINUTE, 10, CURRENT_TIMESTAMP)};



update group_event
set show_time = TIMESTAMP('20130717163318');

-----------------------------------------------------------------------


select g.group_event_id, g.mdn, u.name, u.channel_id, g.member_status, g.last_channel_id
from GROUP_MEMBER g, USERS u
where u.mdn = g.mdn and g.member_status = 'MASTER' or g.member_status = 'ACCEPTED'
order by g.GROUP_EVENT_ID, g.mdn;


select g.group_event_id, g.mdn, g.member_status, g.last_channel_id, u.name, u.channel_id
from GROUP_MEMBER g left outer join users u on g.mdn = u.mdn
where g.member_status = 'MASTER' or g.member_status = 'ACCEPTED'
order by g.GROUP_EVENT_ID, g.mdn;


update group_member
set member_status = 'ACCEPTED'
where member_status = 'INVITED';

delete from group_member;
delete from group_event;










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



select mdn, channel_id as channelId, name from madhack.users
where mdn = '9250000001';


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




-------------------------------------------------------
INSERT INTO USERS (MDN, "NAME", CHANNEL_ID)
VALUES ('9251000001', 'John Johnson', '001##10001');

INSERT INTO USERS (MDN, "NAME", CHANNEL_ID)
VALUES ('9251000002', 'James Johnson', '002##10001');

INSERT INTO USERS (MDN, "NAME", CHANNEL_ID)
VALUES ('9251000003', 'Mary Johnson', '003##10001');

INSERT INTO USERS (MDN, "NAME", CHANNEL_ID)
VALUES ('9252000001', 'David Miller', '001##10002');

INSERT INTO USERS (MDN, "NAME", CHANNEL_ID)
VALUES ('9252000002', 'Robert Miller', '002##10002');

INSERT INTO USERS (MDN, "NAME", CHANNEL_ID)
VALUES ('9252000003', 'Linda Miller', '003##10002');

INSERT INTO USERS (MDN, "NAME", CHANNEL_ID)
VALUES ('9253000001', 'Mark Lee', '001##10002');

INSERT INTO USERS (MDN, "NAME", CHANNEL_ID)
VALUES ('9253000002', 'Paul Lee', '002##10002');

INSERT INTO USERS (MDN, "NAME", CHANNEL_ID)
VALUES ('9253000003', 'Laura Lee', '003##10002');


alter table group_event
drop column reminder_sent;

alter table group_member
add column reminder_sent integer default 0;


--=====
--==
update users
set channel_id = '703##6718065'
where mdn = '9253248817';

update users
set channel_id = '200##000001'
where mdn = '9253248967';

----------------------------------------------------
-- hud 3
INSERT INTO USERS (MDN, "NAME", CHANNEL_ID)
VALUES ('9253248817', 'Dongliang Hu', '200##000001');
INSERT INTO USERS (MDN, "NAME", CHANNEL_ID)
VALUES ('9253248967', 'Fred Bierhaus', '200##000001');
INSERT INTO USERS (MDN, "NAME", CHANNEL_ID)
VALUES ('9084426933', 'Jeff Lin', '200##000001');




-- insert channels
INSERT INTO CHANNELS (CHANNEL_ID, CHANNEL_NAME)
VALUES('703##6718065', 'NBC');
INSERT INTO CHANNELS (CHANNEL_ID, CHANNEL_NAME)
VALUES('200##000001', 'CBS');




