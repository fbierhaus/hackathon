CREATE TABLE settings('_id' INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL ,'key'  VARCHAR(25) NOT NULL ,'value'  VARCHAR(255) ,UNIQUE ('key') ON CONFLICT REPLACE )
CREATE TABLE linked_devices ('id' VARCHAR,'name' VARCHAR,'time' INTEGER, UNIQUE ('id','name','time' ) ON CONFLICT REPLACE )
CREATE TABLE fwd_address ('autoForwardUsedAddr' VARCHAR)
CREATE TABLE reply_address ('autoReplyUsedMsgs' VARCHAR)
CREATE TABLE sync_items ('_id' INTEGER PRIMARY KEY AUTOINCREMENT  NOT NULL,'luid' INTEGER,'type' INTEGER,'action' INTEGER ,'priority' INTEGER ,'retry_count' INTEGER , UNIQUE ('luid','type','priority','action' ) ON CONFLICT REPLACE )
CREATE  TABLE syncstatus ('min_uid' INTEGER,'min_mod_seq' INTEGER,'max_uid' INTEGER,'max_mod_seq' INTEGER,'syncmode' INTEGER DEFAULT -1,'procesed_maxmodseq' INTEGER DEFAULT -1)
CREATE TABLE mapping ('uid' INTEGER DEFAULT 0,'luid' INTEGER DEFAULT 0,'thread_id' INTEGER DEFAULT 0,'msg_type' INTEGER, 'msg_id' VARCHAR,'vma_checksum' INTEGER,'vma_time' INTEGER,'smsc_checksum' INTEGER,'smsc_time' INTEGER,'source' INTEGER,'deleted' INTEGER, UNIQUE ('uid','luid','msg_id','msg_type' ) ON CONFLICT REPLACE )
CREATE TABLE events ('_id' INTEGER PRIMARY KEY AUTOINCREMENT  NOT NULL,'luid' INTEGER,'msg_type' INTEGER,'msg_action' INTEGER , UNIQUE ('luid','msg_type','msg_action' ) ON CONFLICT REPLACE )

CREATE INDEX VMAI_LUID ON mapping( luid );
CREATE INDEX VMAI_UID ON mapping( uid );
CREATE INDEX VMAI_VMA_CHECKSUM ON mapping( vma_checksum );
CREATE INDEX VMAI_SMSC_CHECKSUM ON mapping( smsc_checksum );
CREATE INDEX VMAPI_MSGID ON mapping( msg_id );
