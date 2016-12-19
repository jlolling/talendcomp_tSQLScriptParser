
create table bicontrol.process_config (
   process_config_id integer not null,
   name varchar(255) not null,
   description text,
   start_task_id integer,
   default_end_task_id integer,
   max_parallel integer,
   last_modified_at timestamp,
   author varchar(255),
   preferred_pse integer,
   constraint pk_process_config primary key (process_config_id));
;


create table bicontrol.process_flag (
   process_config_id integer not null,
   flag_key varchar(255) not null,
   flag_value varchar(512),
   last_modified timestamp,
   constraint pk_process_flag primary key (process_config_id,flag_key),
   constraint fk_process_flag_process foreign key (process_config_id) references bicontrol.process_config(process_config_id));
;


create table bicontrol.process_instance (
   process_instance_id integer not null,
   name varchar(255),
   process_config_id integer,
   pid integer,
   process_steering_engine_id integer,
   start_time timestamp,
   stop_time timestamp,
   status integer,
   keep_alive timestamp,
   constraint pk_process_instances primary key (process_instance_id),
   constraint fk_process_instances_process foreign key (process_config_id) references bicontrol.process_config(process_config_id),
   constraint fk_process_instances_engines foreign key (process_steering_engine_id) references bicontrol.process_steering_engine(process_steering_engine_id));
;


create table bicontrol.process_instance_command (
   process_command_id integer not null,
   process_instance_id integer not null,
   command_line varchar(1024) not null,
   created_at timestamp not null,
   created_from varchar(255) not null,
   done_at timestamp,
   constraint pk_process_instance_commands primary key (process_command_id),
   constraint fk_pk_process_inst_comm_pi foreign key (process_instance_id) references bicontrol.process_instance(process_instance_id));
;


create table bicontrol.process_steering_engine (
   process_steering_engine_id integer not null,
   name varchar(255),
   status integer,
   started_at timestamp,
   stopped_at timestamp,
   constraint pk_process_steering_engines primary key (process_steering_engine_id));
;


create table bicontrol.process_trace (
   process_trace_id integer not null,
   process_instance_id integer,
   task_config_id integer,
   next_task_id integer,
   task_return_code varchar(64),
   start_time timestamp not null,
   stop_time timestamp,
   status integer,
   current_action varchar(512),
   progress_info text,
   constraint pk_process_trace primary key (process_trace_id),
   constraint fk_process_trace_instance foreign key (process_instance_id) references bicontrol.process_instance(process_instance_id));
;


create table bicontrol.process_trace_measure (
   process_trace_id integer not null,
   measure_attr_key varchar(255) not null,
   measure_attr_value text,
   measured_at timestamp,
   constraint pk_process_trace_measure primary key (process_trace_id,measure_attr_key),
   constraint fk_process_trace_measure_trace foreign key (process_trace_id) references bicontrol.process_trace(process_trace_id));
;


create table bicontrol.process_trace_task_attribute (
   process_trace_id integer not null,
   task_attr_key varchar(255) not null,
   task_attr_value text,
   last_modified_at timestamp,
   constraint pk_process_trace_attribute primary key (process_trace_id,task_attr_key),
   constraint fk_process_trace_attrib_trace foreign key (process_trace_id) references bicontrol.process_trace(process_trace_id));
;


create table bicontrol.task_config (
   task_config_id integer not null,
   process_config_id integer not null,
   name varchar(255) not null,
   classname varchar(1024) not null,
   technicalname varchar(255) not null,
   description text,
   last_modified_at timestamp,
   author varchar(255),
   constraint pk_conf_tasks primary key (task_config_id),
   constraint fk_task_config_pcid foreign key (process_config_id) references bicontrol.process_config(process_config_id));
;


create table bicontrol.task_attribute_config (
   task_config_id integer not null,
   attr_key varchar(64) not null,
   attr_value_index integer,
   attr_value text,
   attr_type varchar(32),
   last_modified_at timestamp,
   author varchar(255),
   constraint pk_task_attributes primary key (task_config_id,attr_key),
   constraint fk_task_attrib_task_id foreign key (task_config_id) references bicontrol.task_config(task_config_id));
;


create table bicontrol.task_flow_config (
   process_config_id integer not null,
   task_config_id integer not null,
   task_return_code varchar(255) not null,
   next_task_id integer not null,
   stop_before bpchar(1),
   is_end_task bpchar(1),
   constraint pk_task_flow_config primary key (process_config_id,task_config_id,task_return_code),
   constraint fk_task_flow_conf_procid foreign key (process_config_id) references bicontrol.process_config(process_config_id));
;


create table bicontrol.trigger_config (
   trigger_config_id integer not null,
   process_config_id integer,
   is_active integer,
   status integer,
   name varchar(255) not null,
   classname varchar(255) not null,
   description varchar(1024),
   last_modified_at timestamp,
   author varchar(255),
   constraint pk_trigger_config primary key (trigger_config_id));
;


create table bicontrol.trigger_attributes_config (
   trigger_config_id integer not null,
   attr_key varchar(255) not null,
   attr_value_index integer,
   attr_value text,
   last_modified_at timestamp,
   author varchar(255),
   constraint pk_trigger_attribute_config primary key (trigger_config_id,attr_key),
   constraint fk_trigger_attrib_trig_conf_id foreign key (trigger_config_id) references bicontrol.trigger_config(trigger_config_id));
;


