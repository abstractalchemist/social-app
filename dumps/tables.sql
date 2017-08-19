drop database if exists test;
create database test;
use test;

drop table if exists t_user;
drop table if exists t_wall;
drop table if exists t_tag;
drop table if exists t_tag_user;

create table t_user ( id int unsigned auto_increment primary key,
		      name varchar(100),
       	     	      email varchar(100) unique key,
		      password varchar(100),
		      salt varchar(1000)) ENGINE=INNODB;

create table t_wall ( id int unsigned auto_increment primary key,
       	     	      user_id int unsigned references t_user.id,
		      comment longtext,
		      at date ) ENGINE=INNODB;

create table t_tag ( id int unsigned auto_increment primary key,
       	     	    tag varchar(100) ) ENGINE=INNODB;

create table t_tag_user ( tag_id int unsigned references t_tag.id, 
       	     		  user_id int unsigned references t_tag_user ) ENGINE=INNODB;
		    
